package com.bryankrosenbaum.stepintime.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.bryankrosenbaum.stepintime.model.ScheduledEvent;

public class StepinTimeDataSource {

    private String TAG = StepinTimeDataSource.class.getSimpleName();

    protected StepinTimeSQLiteHelper dbHelper;

    private SQLiteDatabase database;

    private String[] allColumns = {
            StepinTimeSQLiteHelper.COLUMN_ID,
            StepinTimeSQLiteHelper.COLUMN_HOUR,
            StepinTimeSQLiteHelper.COLUMN_MINUTE,
            StepinTimeSQLiteHelper.COLUMN_STEP_COUNT,
            StepinTimeSQLiteHelper.COLUMN_IS_ENABLED
    };

    /**
     * Initialize data source
     * @param context
     */
    public StepinTimeDataSource(Context context) {
        Log.d(TAG, "initialize data source");

        dbHelper = new StepinTimeSQLiteHelper(context);
    }

    /**
     * Open database connection
     */
    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    /**
     * Close database connection
     */
    public void close() {
        dbHelper.close();
    }

    public ScheduleEventItem createItem(ScheduledEvent scheduledEvent) {
        return createItem(scheduledEvent.getHour(), scheduledEvent.getMinute(), scheduledEvent.getStepCount());
    }

    /**
     * Insert item into database
     * @param hour
     * @param minute
     * @param stepCount
     * @return
     */
    public ScheduleEventItem createItem(int hour, int minute, int stepCount) {
        // open db connection
        this.open();

        ContentValues values = new ContentValues();

        values.put(StepinTimeSQLiteHelper.COLUMN_HOUR, hour);
        values.put(StepinTimeSQLiteHelper.COLUMN_MINUTE, minute);
        values.put(StepinTimeSQLiteHelper.COLUMN_STEP_COUNT, stepCount);
        values.put(StepinTimeSQLiteHelper.COLUMN_IS_ENABLED, 1); // default to "on"

        long insertId = database.insert(StepinTimeSQLiteHelper.TABLE_SIT_SCHEDULE, "NullColHackThrowError", values);
        Cursor cursor = database.query(StepinTimeSQLiteHelper.TABLE_SIT_SCHEDULE,
                allColumns, StepinTimeSQLiteHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        ScheduleEventItem newItem = cursorToItem(cursor);
        cursor.close();

        // close db connection
        this.close();

        return newItem;
    }

    public ScheduleEventItem updateItemEnabled(int eventId, boolean isEnabled) {
        ScheduleEventItem eventItem = fetchEvent(eventId);
        eventItem.setEnabled(isEnabled);
        return updateItem(eventItem);
    }

    /**
     * Update item from database
     *
     * @param item ScheduleEventItem that will be deleted
     */
    public ScheduleEventItem updateItem(ScheduleEventItem item) {
        // open db connection
        this.open();

        ContentValues values = new ContentValues();

        values.put(StepinTimeSQLiteHelper.COLUMN_HOUR, item.getHour());
        values.put(StepinTimeSQLiteHelper.COLUMN_MINUTE, item.getMinute());
        values.put(StepinTimeSQLiteHelper.COLUMN_STEP_COUNT, item.getStepCount());
        values.put(StepinTimeSQLiteHelper.COLUMN_IS_ENABLED, item.isEnabled());

        long _id = item.getId();
        Log.d(TAG, "item updated with id: " + _id);
        database.update(StepinTimeSQLiteHelper.TABLE_SIT_SCHEDULE, values, StepinTimeSQLiteHelper.COLUMN_ID
                + " = " + _id, null);

        // close db connection
        this.close();

        return fetchEvent(_id);
    }

    /**
     * Create a ScheduleEventItem from the cursor parameter
     *
     * @param cursor Cursor that contains the item content
     * @return ScheduleEventItem object created from the cursor
     */
    private ScheduleEventItem cursorToItem(Cursor cursor) {
        long _id = cursor.getLong(cursor.getColumnIndex(StepinTimeSQLiteHelper.COLUMN_ID));
        int hour = cursor.getInt(cursor.getColumnIndex(StepinTimeSQLiteHelper.COLUMN_HOUR));
        int minute = cursor.getInt(cursor.getColumnIndex(StepinTimeSQLiteHelper.COLUMN_MINUTE));
        int stepCount = cursor.getInt(cursor.getColumnIndex(StepinTimeSQLiteHelper.COLUMN_STEP_COUNT));
        boolean isEnabled = (cursor.getInt(cursor.getColumnIndex(StepinTimeSQLiteHelper.COLUMN_IS_ENABLED)) == 1) ? true : false;
        ScheduleEventItem scheduleEventItem = new ScheduleEventItem(_id, hour, minute, stepCount, isEnabled);
        return scheduleEventItem;
    }

    /**
     * Get scheduled event from the database
     *
     * @param eventId event id in the database of the record to update
     * @return ScheduleEventItem of the record
     */
    public ScheduleEventItem fetchEvent(long eventId) {
        // open connection
        this.open();

        Cursor cursor = database.query(StepinTimeSQLiteHelper.TABLE_SIT_SCHEDULE, allColumns,
                StepinTimeSQLiteHelper.COLUMN_ID + " = " + eventId, null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }

        // close db connection
        this.close();

        return cursorToItem(cursor);
    }

    /**
     * Get all scheduled events from the database
     * @return cursor containing all of the records sorted by time
     */
    public Cursor fetchEvents() {
        // open connection
        this.open();

        String orderBy = StepinTimeSQLiteHelper.COLUMN_HOUR + ", " + StepinTimeSQLiteHelper.COLUMN_MINUTE;
        Cursor cursor = database.query(StepinTimeSQLiteHelper.TABLE_SIT_SCHEDULE, allColumns, null, null, null, null, orderBy);

        if (cursor != null) {
            cursor.moveToFirst();
        }

        Log.d(TAG, "fetched " + cursor.getCount() + " items from database");

        // close db connection
        this.close();

        return cursor;
    }

    /**
     * Delete item from database
     *
     * @param eventId eventId that will be deleted
     */
    public void deleteItem(int eventId) {
        // open db connection
        this.open();

        long _id = (long)eventId;
        Log.d(TAG, "item deleted with id: " + _id);
        database.delete(StepinTimeSQLiteHelper.TABLE_SIT_SCHEDULE, StepinTimeSQLiteHelper.COLUMN_ID
                + " = " + _id, null);

        // close db connection
        this.close();
    }

    /**
     * Delete item from database
     *
     * @param item ScheduleEventItem that will be deleted
     */
    public void deleteItem(ScheduleEventItem item) {
        // open db connection
        this.open();

        long _id = item.getId();
        Log.d(TAG, "item deleted with id: " + _id);
        database.delete(StepinTimeSQLiteHelper.TABLE_SIT_SCHEDULE, StepinTimeSQLiteHelper.COLUMN_ID
                + " = " + _id, null);

        // close db connection
        this.close();
    }

    /**
     * Delete all items from the database
     */
    public void deleteAll() {
        // open db connection
        this.open();

        database.delete(StepinTimeSQLiteHelper.TABLE_SIT_SCHEDULE, null, null);

        Log.d(TAG, "deleted all items from database");

        // close db connection
        this.close();
    }
}