package com.bryankrosenbaum.stepintime.model;

import com.google.gson.annotations.SerializedName;

public class AccessTokenWrapper {
    @SerializedName("activities-steps")
    private AccessToken accessToken;

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }
}
