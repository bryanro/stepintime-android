package com.bryankrosenbaum.stepintime.adapter;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Switch;
import android.widget.TextView;

import com.bryankrosenbaum.stepintime.R;
import com.bryankrosenbaum.stepintime.db.StepinTimeSQLiteHelper;
import com.bryankrosenbaum.stepintime.model.ScheduledEvent;

import java.util.ArrayList;

public class EventAdapter extends CursorAdapter {

    private ArrayList<ScheduledEvent> entries;
    private String TAG = EventAdapter.class.getSimpleName();

    public EventAdapter(Activity context, Cursor c) {
        super(context, c);
    }

    /**
     * Tell adapters how each item will look when the view is created for the first time
     * @param context
     * @param cursor
     * @param viewGroup
     * @return
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View retView = inflater.inflate(R.layout.list_events, viewGroup, false);
        return retView;
    }

    /**
     * Take data from the cursor and put it into the view
     * @param view
     * @param context
     * @param cursor
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // take data from cursor and put it in the view

        TextView txtEventId = (TextView) view.findViewById(R.id.txtEventId);
        TextView txtTime = (TextView) view.findViewById(R.id.txtTime);
        TextView txtStepCount = (TextView) view.findViewById(R.id.txtStepCount);
        Switch switchOnOff = (Switch) view.findViewById(R.id.switchOnOff);

        long eventId = cursor.getInt(cursor.getColumnIndex(StepinTimeSQLiteHelper.COLUMN_ID));
        int hour = cursor.getInt(cursor.getColumnIndex(StepinTimeSQLiteHelper.COLUMN_HOUR));
        int minute = cursor.getInt(cursor.getColumnIndex(StepinTimeSQLiteHelper.COLUMN_MINUTE));
        int stepCount = cursor.getInt(cursor.getColumnIndex(StepinTimeSQLiteHelper.COLUMN_STEP_COUNT));
        boolean isEnabled = (cursor.getInt(cursor.getColumnIndex(StepinTimeSQLiteHelper.COLUMN_IS_ENABLED)) == 1) ? true : false;


        String timeString = "";
        String amPm = "";
        if (hour == 0) {
            timeString += "12";
            amPm = "AM";
        }
        else if (hour == 12) {
            timeString = "12";
            amPm = "PM";
        }
        else if (hour > 0 && hour < 12) {
            if (hour < 10) {
                timeString += " ";
            }
            timeString += hour;
            amPm = "AM";
        }
        else if (hour > 12 && hour < 24) {
            if ((hour - 12) < 10) {
                timeString += " ";
            }
            timeString += (hour - 12);
            amPm = "PM";
        }
        else {
            Log.e(TAG, "hour out of bounds: " + hour);
            timeString = "ERR";
        }

        String minuteString = "" + minute;
        if (minute < 10) {
            minuteString = "0" + minute;
        }

        timeString += ":" + minuteString + " " + amPm;

        txtEventId.setText("" + eventId);
        txtTime.setText(timeString);
        txtStepCount.setText("" + stepCount);
        switchOnOff.setChecked(isEnabled);
    }
}
