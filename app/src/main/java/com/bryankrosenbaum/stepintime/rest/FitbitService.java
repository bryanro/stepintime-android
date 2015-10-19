package com.bryankrosenbaum.stepintime.rest;

import com.bryankrosenbaum.stepintime.model.AccessTokenWrapper;
import com.bryankrosenbaum.stepintime.model.ActivityStepWrapper;
import com.bryankrosenbaum.stepintime.model.UserWrapper;
import com.bryankrosenbaum.stepintime.model.AccessToken;

import retrofit.Callback;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;

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

    @FormUrlEncoded
    @POST("/oauth2/token")
    AccessToken getAccessToken(@Field("code") String code,
                               @Field("grant_type") String grantType,
                               @Field("client_id") String clientId,
                               @Field("redirect_uri") String redirectUri);

    @FormUrlEncoded
    @POST("/oauth2/token")
    AccessToken refreshToken(@Field("grant_type") String grantType,
                             @Field("refresh_token") String refreshToken);

    @FormUrlEncoded
    @POST("/oauth2/token")
    void refreshToken(@Field("grant_type") String grantType,
                             @Field("refresh_token") String refreshToken,
                             Callback<AccessToken> cb);
}
