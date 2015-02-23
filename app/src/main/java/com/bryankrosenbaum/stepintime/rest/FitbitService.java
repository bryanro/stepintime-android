package com.bryankrosenbaum.stepintime.rest;

import com.bryankrosenbaum.stepintime.model.ActivityStepWrapper;
import com.bryankrosenbaum.stepintime.model.UserWrapper;

import retrofit.Callback;
import retrofit.http.GET;

public interface FitbitService {

    /**
     * Get profile
     *
     * @param cb
     */
    @GET("/1/user/-/profile.json")
    void getProfile(Callback<UserWrapper> cb);

    /**
     * Get today's step count
     */
    @GET("/1/user/-/activities/steps/date/today/1d.json")
    void getStepCountToday(Callback<ActivityStepWrapper> cb);
}
