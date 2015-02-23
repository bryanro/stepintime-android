package com.bryankrosenbaum.stepintime;

import android.app.Activity;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.bryankrosenbaum.stepintime.alarm.EventReminderAlarmBroadcastReceiver;
import com.bryankrosenbaum.stepintime.db.StepinTimeDataSource;
import com.bryankrosenbaum.stepintime.model.ScheduledEvent;

import java.util.ArrayList;


public class AddEventActivity extends Activity {

    private final String TAG = AddEventActivity.class.getSimpleName();

    private NumberPicker npickerStepCount;
    private TimePicker timepickTime;

    private StepinTimeDataSource scheduleDataSource;

    private String[] displayedValuesArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        npickerStepCount = (NumberPicker) findViewById(R.id.npickerStepCount);
        timepickTime = (TimePicker) findViewById(R.id.timepickTime);

        setNumberPickerValues();
        timepickTime.setCurrentHour(12);
        timepickTime.setCurrentMinute(0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_event, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClickAddNewItem (View view) {

        int stepCount = Integer.parseInt(displayedValuesArray[npickerStepCount.getValue()]);
        int hour = timepickTime.getCurrentHour();
        int minute = timepickTime.getCurrentMinute();

        Log.d(TAG, "creating new event for H=" + hour + ", M=" + minute + ", Steps=" + stepCount);

        ScheduledEvent scheduledEvent = new ScheduledEvent(hour, minute, stepCount);

        saveNewItemToDatabase(scheduledEvent);
    }

    private void setNumberPickerValues() {
        int min = 0;
        int max = 20000;
        int step = 500;

        ArrayList<String> valueSet = new ArrayList<String>();

        for (int i = min; i <= max; i += step) {
            valueSet.add(String.valueOf(i));
        }

        displayedValuesArray = valueSet.toArray(new String[valueSet.size()]);

        npickerStepCount.setMinValue(0);
        npickerStepCount.setMaxValue(displayedValuesArray.length - 1);
        npickerStepCount.setDisplayedValues(displayedValuesArray);
        npickerStepCount.setValue(5);
    }

    private void saveNewItemToDatabase(ScheduledEvent scheduledEvent) {
        // save to database
        try {
            scheduleDataSource = new StepinTimeDataSource(this);
            scheduleDataSource.createItem(scheduledEvent);

            saveRepeatingAlarm(scheduledEvent);

            // close this view
            finish();
        }
        catch (SQLiteException sqlEx) {
            Log.e(TAG, "exception attempting to open database connection: " + sqlEx.getMessage());
            Toast.makeText(this, "Exception thrown opening database connection. Try again.", Toast.LENGTH_LONG).show();
        }
    }

    private void saveRepeatingAlarm(ScheduledEvent scheduledEvent) {
        EventReminderAlarmBroadcastReceiver eventReminderAlarm = new EventReminderAlarmBroadcastReceiver();
        eventReminderAlarm.setAlarm(getApplicationContext(), scheduledEvent);
    }
}
