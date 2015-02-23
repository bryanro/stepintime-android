package com.bryankrosenbaum.stepintime.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.widget.Toast;

import com.bryankrosenbaum.stepintime.db.StepinTimeDataSource;
import com.bryankrosenbaum.stepintime.db.StepinTimeSQLiteHelper;
import com.bryankrosenbaum.stepintime.model.ScheduledEvent;

public class StartOnBootupBroadcastReceiver extends BroadcastReceiver {

    private String TAG = StartOnBootupBroadcastReceiver.class.getSimpleName();

    private StepinTimeDataSource scheduleDataSource;

    public StartOnBootupBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            Log.d(TAG, "Boot Completed");

            // pull from to database
            try {
                scheduleDataSource = new StepinTimeDataSource(context);
                Cursor cursor = scheduleDataSource.fetchEvents();
                cursor.moveToFirst();
                int alarmCount = 0;
                while (!cursor.isAfterLast()) {
                    int hour = cursor.getInt(cursor.getColumnIndex(StepinTimeSQLiteHelper.COLUMN_HOUR));
                    int minute = cursor.getInt(cursor.getColumnIndex(StepinTimeSQLiteHelper.COLUMN_MINUTE));
                    int stepCount = cursor.getInt(cursor.getColumnIndex(StepinTimeSQLiteHelper.COLUMN_STEP_COUNT));
                    boolean isEnabled = (cursor.getInt(cursor.getColumnIndex(StepinTimeSQLiteHelper.COLUMN_IS_ENABLED)) == 1) ? true : false;

                    if (isEnabled) {
                        // save alarm
                        ScheduledEvent scheduledEvent = new ScheduledEvent(hour, minute, stepCount);
                        EventReminderAlarmBroadcastReceiver eventReminderAlarm = new EventReminderAlarmBroadcastReceiver();
                        eventReminderAlarm.setAlarm(context, scheduledEvent);
                        Log.d(TAG, "alarm added: " + hour + ":" + minute + " for " + stepCount + " steps");
                    }
                    else {
                        Log.i(TAG, "alarm not added because isEnabled is false: " + hour + ":" + minute + " for " + stepCount + " steps");
                    }
                    cursor.moveToNext();
                }
                Toast.makeText(context, alarmCount + " items added to alarm", Toast.LENGTH_LONG).show();
            }
            catch (SQLiteException sqlEx) {
                Log.e(TAG, "exception attempting to open database connection: " + sqlEx.getMessage());
                Toast.makeText(context, "Exception thrown opening database connection. Try again.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
