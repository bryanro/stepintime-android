package com.bryankrosenbaum.stepintime;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.bryankrosenbaum.stepintime.adapter.EventAdapter;
import com.bryankrosenbaum.stepintime.alarm.EventReminderAlarmBroadcastReceiver;
import com.bryankrosenbaum.stepintime.db.ScheduleEventItem;
import com.bryankrosenbaum.stepintime.db.StepinTimeDataSource;

public class MainActivity extends Activity {

    private final String TAG = MainActivity.class.getSimpleName();

    private StepinTimeDataSource scheduleDataSource;
    private Cursor cursor;
    private CursorAdapter cursorAdapter;
    private SharedPreferences prefs;

    private EventReminderAlarmBroadcastReceiver eventReminderAlarm;
    private ActionMode actionMode;

    private ListView listReminderEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listReminderEvents = (ListView) findViewById(R.id.listReminderEvents);
        eventReminderAlarm = new EventReminderAlarmBroadcastReceiver();

        scheduleDataSource = new StepinTimeDataSource(this);

        // if the token isn't set, automatically navigate to the settings page
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, ConnectFitbitActivity.class));
        }
        else if (id == R.id.action_add_item) {
            startActivity(new Intent(MainActivity.this, AddEventActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String token = prefs.getString(getString(R.string.pref_name_access_token), null);
        String tokenSecret = prefs.getString(getString(R.string.pref_name_refresh_token), null);
        if (token == null || tokenSecret == null) {
            startActivity(new Intent(MainActivity.this, ConnectFitbitActivity.class));
        }
        else {
            displaySchedule();
            hookupScheduleEvents();
        }
    }

    private void displaySchedule() {
        cursor = scheduleDataSource.fetchEvents();
        if (cursorAdapter == null) {
            cursorAdapter = new EventAdapter(this, cursor);
        }
        else {
            cursorAdapter.changeCursor(cursor);
        }

        listReminderEvents.setAdapter(cursorAdapter);
    }

    private void hookupScheduleEvents() {
        listReminderEvents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView txtEventId = (TextView) view.findViewById(R.id.txtEventId);
                Switch switchOnOff = (Switch) view.findViewById(R.id.switchOnOff);
                switchOnOff.setChecked(!switchOnOff.isChecked());
                try {
                    int eventId = Integer.parseInt(txtEventId.getText().toString());

                    Log.d(TAG, "clicked on list item id=" + eventId + ", switch isChecked=" + switchOnOff.isChecked());

                    ScheduleEventItem eventItem = scheduleDataSource.updateItemEnabled(eventId, switchOnOff.isChecked());

                    if (switchOnOff.isChecked()) {
                        Log.d(TAG, "reminder id=" + eventId + " is enabled, so call setAlarm");
                        eventReminderAlarm.setAlarm(getApplicationContext(), eventItem.getHour(), eventItem.getMinute(), eventItem.getStepCount());
                    }
                    else {
                        Log.d(TAG, "reminder id=" + eventId + " is disabled, so call cancelAlarm");
                        eventReminderAlarm.cancelAlarm(getApplicationContext(), eventItem.getHour(), eventItem.getMinute());
                    }
                }
                catch (NumberFormatException numberFormatEx) {
                    Log.e(TAG, "NumberFormatException when trying to call Integer.parseInt('" + txtEventId.getText().toString() + "') in setOnItemClickListener");
                }
            }
        });

        listReminderEvents.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                TextView txtEventId = (TextView) view.findViewById(R.id.txtEventId);

                try {
                    final long eventId = Long.parseLong(txtEventId.getText().toString());
                    final ScheduleEventItem scheduleEventItem = scheduleDataSource.fetchEvent(eventId);

                    ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

                        @Override
                        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                            // Inflate a menu resource providing context menu items
                            getMenuInflater().inflate(R.menu.menu_context_longpress, menu);
                            return true;
                        }

                        @Override
                        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                            return false;
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.contextmenu_delete:
                                    // cancel the alarm
                                    eventReminderAlarm.cancelAlarm(getApplicationContext(), scheduleEventItem.getHour(), scheduleEventItem.getMinute());
                                    // delete from database
                                    scheduleDataSource.deleteItem(scheduleEventItem);
                                    // refresh the page
                                    displaySchedule();

                                    // close the action mode menu
                                    actionMode.finish();
                                    return true;
                            }
                            return false;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode mode) {
                            actionMode = null;
                        }
                    };

                    actionMode = startActionMode(actionModeCallback);
                }
                catch (NumberFormatException numberFormatEx) {
                    Log.e(TAG, "NumberFormatException when trying to call Integer.parseInt('" + txtEventId.getText().toString() + "') in setOnItemLongClickListener");
                }

                return true;
            }
        });
    }
}
