package com.bryankrosenbaum.stepintime.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bryankrosenbaum.stepintime.R;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * https://futurestud.io/blog/oauth-2-on-android-with-retrofit/
 */
public class AccessToken {

    private static String TAG = AccessToken.class.getSimpleName();

    private static AccessToken savedInstance;

    @SerializedName("access_token")
    private String access_token;

    @SerializedName("expires_in")
    private int expires_in;

    @SerializedName("expiration_dtm")
    private Date expiration_dtm;

    @SerializedName("refresh_token")
    private String refresh_token;

    @SerializedName("scope")
    private String scope;

    @SerializedName("token_type")
    private String token_type;

    @SerializedName("user_id")
    private String user_id;

    private SharedPreferences prefs;

    public AccessToken() {
        this.access_token = null;
        this.expires_in = 0;
        this.expiration_dtm = null;
        this.refresh_token = null;
        this.scope = null;
        this.token_type = null;
        this.user_id = null;
    }

    public AccessToken(Context context) {
        this.restoreFromPreferences(context);
    }

    public String getAccessToken() {
        return this.access_token;
    }

    public int getExpiresIn() {
        return this.expires_in;
    }

    public String getRefreshToken() { return this.refresh_token; }

    public String getScope() {
        return this.scope;
    }

    public String getTokenType() { return this.token_type; }

    public String getUserId() {
        return this.user_id;
    }

    public Date getExpirationDtm() { return this.getExpirationDtm(); }

    public boolean isTokenExpired() {
        Date now = new Date();
        if (now.getTime() > this.expiration_dtm.getTime()) {
            Log.d(TAG, "token has expired");
            return true;
        }
        else {
            Log.d(TAG, "token has not expired");
            return false;
        }
    }

    public void expireToken(Context context) {
        Log.i(TAG, "manually saving expired token");
        this.expires_in = 0;
        this.saveToPreferences(context);
    }

    public void saveToPreferences(Context context) {
        this.expiration_dtm = new Date(new Date().getTime() + (this.expires_in * 1000));
        Log.d(TAG, "token expiration in " + this.expires_in + " seconds at: " + this.expiration_dtm.toString());

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .putString(context.getString(R.string.pref_name_access_token), this.access_token)
                .putInt(context.getString(R.string.pref_name_expires_in), this.expires_in)
                .putString(context.getString(R.string.pref_name_refresh_token), this.refresh_token)
                .putString(context.getString(R.string.pref_name_scope), this.scope)
                .putString(context.getString(R.string.pref_name_token_type), this.token_type)
                .putString(context.getString(R.string.pref_name_user_id), this.user_id)
                .putLong(context.getString(R.string.pref_name_expiration_dtm_timeinmillis), this.expiration_dtm.getTime())
                .commit();
    }

    public void restoreFromPreferences(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.access_token = prefs.getString(context.getString(R.string.pref_name_access_token), null);
        this.expires_in = prefs.getInt(context.getString(R.string.pref_name_expires_in), -1);
        this.refresh_token = prefs.getString(context.getString(R.string.pref_name_refresh_token), null);
        this.scope = prefs.getString(context.getString(R.string.pref_name_scope), null);
        this.token_type = prefs.getString(context.getString(R.string.pref_name_token_type), null);
        this.user_id = prefs.getString(context.getString(R.string.pref_name_user_id), null);
        this.expiration_dtm = new Date(prefs.getLong(context.getString(R.string.pref_name_expiration_dtm_timeinmillis), 0));
    }

    public static AccessToken getInstance(Context context) {
        if (savedInstance == null) {
            savedInstance = new AccessToken(context);
        }
        return savedInstance;
    }

    public static void saveNewAccessToken(AccessToken newAccessToken, Context context) {
        savedInstance = newAccessToken;
        savedInstance.saveToPreferences(context);
    }
}