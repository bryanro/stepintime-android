package com.bryankrosenbaum.stepintime.model;

import com.google.gson.annotations.SerializedName;

public class ActivityStepWrapper {
    @SerializedName("activities-steps")
    private ActivityStep[] activitiesSteps;

    public ActivityStep[] getActivitiesSteps() {
        return activitiesSteps;
    }

    public void setActivitiesSteps(ActivityStep[] activitiesSteps) {
        this.activitiesSteps = activitiesSteps;
    }
}
