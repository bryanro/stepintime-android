package com.bryankrosenbaum.stepintime.model;

public class ScheduledEvent {
    private int hour;
    private int minute;
    private int stepCount;

    public ScheduledEvent() {
    }

    public ScheduledEvent(int hour, int minute, int stepCount) {
        this.hour = hour;
        this.minute = minute;
        this.stepCount = stepCount;
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
}
