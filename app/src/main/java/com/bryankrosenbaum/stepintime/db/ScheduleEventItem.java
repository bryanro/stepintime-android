package com.bryankrosenbaum.stepintime.db;

public class ScheduleEventItem {

    private long _id;
    private int hour;
    private int minute;
    private int stepCount;
    private boolean isEnabled;

    public ScheduleEventItem(long _id, int hour, int minute, int stepCount) {
        init(_id, hour, minute, stepCount, true);
    }

    public ScheduleEventItem(long _id, int hour, int minute, int stepCount, boolean isEnabled) {
        init(_id, hour, minute, stepCount, isEnabled);
    }

    /**
     * Helper function for the multiple constructors that creates a new LaterListItem object
     *
     * @param _id ID
     * @param hour Integer value from 0 to 23 indicating the hour
     * @param minute Integer value from 0 to 59 indicating the minute
     * @param stepCount Integer value indicating the step count for the specified time
     * @param isEnabled boolean indicating whether this event reminder is on or off
     */
    private void init(long _id, int hour, int minute, int stepCount, boolean isEnabled) {
        this.setId(_id);
        this.setHour(hour);
        this.setMinute(minute);
        this.setStepCount(stepCount);
        this.setEnabled(true);
    }

    public long getId() {
        return _id;
    }

    public void setId(long _id) {
        this._id = _id;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}
