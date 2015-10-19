package com.bryankrosenbaum.stepintime;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bryankrosenbaum.stepintime.alarm.EventReminderAlarmBroadcastReceiver;
import com.bryankrosenbaum.stepintime.model.ActivityStep;
import com.bryankrosenbaum.stepintime.model.ActivityStepWrapper;
import com.bryankrosenbaum.stepintime.model.UserWrapper;
import com.bryankrosenbaum.stepintime.model.AccessToken;
import com.bryankrosenbaum.stepintime.rest.ServiceGenerator;
import com.bryankrosenbaum.stepintime.rest.FitbitService;

import java.util.Calendar;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;


public class ConnectFitbitActivity extends Activity {

    private String TAG = ConnectFitbitActivity.class.getSimpleName();

    private TextView txtStatus;

    private String OAUTH2_CLIENT_ID;
    private String OAUTH_CONSUMER_KEY;
    private String OAUTH_CONSUMER_SECRET;
    private String OAUTH_CALLBACK_AUTH_SCHEME;
    private String OAUTH_CALLBACK_TOKEN_SCHEME;
    private String OAUTH_CALLBACK_AUTH_URI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_fitbit);

        txtStatus = (TextView) findViewById(R.id.txtStatus);

        OAUTH2_CLIENT_ID = getString(R.string.oauth2_client_id);
        OAUTH_CONSUMER_KEY = getString(R.string.oauth_consumer_key);
        OAUTH_CONSUMER_SECRET = getString(R.string.oauth_consumer_secret);
        OAUTH_CALLBACK_AUTH_SCHEME = getString(R.string.oauth_callback_auth_scheme);
        OAUTH_CALLBACK_AUTH_URI = OAUTH_CALLBACK_AUTH_SCHEME + "://" + getString(R.string.oauth_callback_host);

        updateStatusText();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connect_fitbit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Check if this is a callback from OAuth
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "intent: " + intent);

        Uri uri = intent.getData();
        if (uri != null && uri.getScheme().equals(OAUTH_CALLBACK_AUTH_SCHEME)) {
            String code = uri.getQueryParameter("code");
            Log.d(TAG, "code: " + code);
            if (code != null) {
                // async task to retrieve the access token
                new OauthRetrieveAccessTokenTask().execute(code);
            } else if (uri.getQueryParameter("error") != null) {
                Log.e(TAG, "error with oauth callback not returning code");
            }
        }
    }

    public void onClickAuthorizeApp (View v) {
        new OauthOpenAuthorizationPageTask().execute();
    }

    public void onClickRefreshAuth (View v) {
        new OauthRefreshTokenTask().execute();
    }

    public void onClickExpireToken (View v) {
        AccessToken.getInstance(this).expireToken(this);
    }

    public void onClickTestProfile(View v) {
        FitbitService fitbitService = ServiceGenerator.createService(FitbitService.class, getString(R.string.fitbit_base_uri), AccessToken.getInstance(this));

        fitbitService.getProfile(new Callback<UserWrapper>() {
            @Override
            public void success(UserWrapper user, Response response) {
                Toast.makeText(ConnectFitbitActivity.this, "SUCCESS!  Hello " + user.getUser().getDisplayName(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "error retreiving profile: " + error);
                Toast.makeText(ConnectFitbitActivity.this, "ERROR!  Try authorizing again.", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void onClickTestStepCount (View v) {
        FitbitService fitbitService = ServiceGenerator.createService(FitbitService.class, getString(R.string.fitbit_base_uri), AccessToken.getInstance(this));

        fitbitService.getStepCountToday(new Callback<ActivityStepWrapper>() {
            @Override
            public void success(ActivityStepWrapper activityStepWrapper, Response response) {
                ActivityStep[] activitySteps = activityStepWrapper.getActivitiesSteps();
                if (activitySteps.length == 0) {
                    Log.w(TAG, "getStepCountToday returned empty");
                    Toast.makeText(ConnectFitbitActivity.this, "SUCCESS! but 0 results returned", Toast.LENGTH_LONG).show();
                }
                else if (activitySteps.length == 1) {
                    Log.d(TAG, "getStepCountToday returned 1 result for " + activitySteps[0].getDateTime() + " : " + activitySteps[0].getValue());
                    Toast.makeText(ConnectFitbitActivity.this, "SUCCESS! " + activitySteps[0].getDateTime() + " : " + activitySteps[0].getValue(), Toast.LENGTH_LONG).show();
                }
                else {
                    Log.w(TAG, "getStepCountToday returned " + activitySteps.length + " results");
                    Toast.makeText(ConnectFitbitActivity.this, "SUCCESS! " + activitySteps.length + " results returned", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "error retreiving step counts: " + error);
                Toast.makeText(ConnectFitbitActivity.this, "ERROR!  Try authorizing again.", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void onClickTestAlarm (View v) {
        EventReminderAlarmBroadcastReceiver eventReminderAlarm = new EventReminderAlarmBroadcastReceiver();

        Calendar calendarAlarmTime = Calendar.getInstance();
        // set trigger for next minute
        calendarAlarmTime.setTimeInMillis(System.currentTimeMillis());
        eventReminderAlarm.setAlarm(getApplicationContext(), calendarAlarmTime.get(Calendar.HOUR_OF_DAY), calendarAlarmTime.get(Calendar.MINUTE) + 2, 1000);

        Toast.makeText(this, "Alert should show up in the next 1-2 minutes around " + calendarAlarmTime.get(Calendar.HOUR_OF_DAY) + ":" + (calendarAlarmTime.get(Calendar.MINUTE) + 2), Toast.LENGTH_LONG).show();

        Log.d(TAG, "alarm set");
    }

    public void onClickTestNotification (View v) {
        EventReminderAlarmBroadcastReceiver eventReminderAlarm = new EventReminderAlarmBroadcastReceiver();
        Intent eventReminderIntent = new Intent(getApplicationContext(), EventReminderAlarmBroadcastReceiver.class);
        eventReminderIntent.putExtra(getString(R.string.intent_extra_time), "12:00PM");
        eventReminderIntent.putExtra(getString(R.string.intent_extra_step_count_scheduled), "5678");
        sendBroadcast(eventReminderIntent);
    }

    /**
     * First step of oAuth2 - open the Fitbit authorization page where user logs in and accepts
     */
    private class OauthOpenAuthorizationPageTask extends AsyncTask<Void, Void, String> {

        /**
         * Open a browser to the Fitbit authorization webpage
         * @param params
         * @return
         */
        @Override
        protected String doInBackground(Void... params) {
            String authUrl = "https://www.fitbit.com/oauth2/authorize?response_type=code&client_id=" + OAUTH2_CLIENT_ID
                    + "&redirect_uri=" + OAUTH_CALLBACK_AUTH_URI
                    + "&scope=activity%20profile"; // scope=activity profile (delimited by space)
            Log.d(TAG, "authUrl: " + authUrl);

            // open browser for auth
            // add flags so back button doesn't open up browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            startActivity(intent);

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                Toast.makeText(ConnectFitbitActivity.this, result, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Second step of oAuth2 - retrieve the access token using the code from the first oAuth2 step
     */
    class OauthRetrieveAccessTokenTask extends AsyncTask<String, Void, String> {

        public OauthRetrieveAccessTokenTask() {
        }

        /**
         * Call the getAccessToken web service
         * @param params array of params where params[0] = the "code" returned from the first oAuth2 step
         * @return null
         */
        @Override
        protected String doInBackground(String... params) {
            try {
                FitbitService fitbitService = ServiceGenerator.createService(FitbitService.class, getString(R.string.fitbit_base_uri), OAUTH2_CLIENT_ID, OAUTH_CONSUMER_SECRET);
                AccessToken newAccessToken = fitbitService.getAccessToken(params[0], "authorization_code", OAUTH2_CLIENT_ID, OAUTH_CALLBACK_AUTH_URI);
                Log.d(TAG, "successfully retrieved access token, saving to preferences");
                AccessToken.saveNewAccessToken(newAccessToken, ConnectFitbitActivity.this);
            }
            catch (RetrofitError retrofitErr) {
                String reason = retrofitErr.getResponse().getReason();
                int status = retrofitErr.getResponse().getStatus();
                String message = retrofitErr.getMessage();
                String body = (new String(((TypedByteArray)retrofitErr.getResponse().getBody()).getBytes()));
                Log.e(TAG, "exception getting access token:"
                        + "\nstatus: " + status
                        + "\nreason: " + reason
                        + "\nmessage: " + message
                        + "\nbody: " + body);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                Toast.makeText(ConnectFitbitActivity.this, result, Toast.LENGTH_LONG).show();
            }
            updateStatusText();
        }
    }

    /**
     * refresh the oauth2 token
     */
    class OauthRefreshTokenTask extends AsyncTask<Void, Void, String> {

        public OauthRefreshTokenTask() {
        }

        /**
         *
         * @param params
         * @return
         */
        @Override
        protected String doInBackground(Void... params) {
            try {
                FitbitService fitbitService = ServiceGenerator.createService(FitbitService.class, getString(R.string.fitbit_base_uri), OAUTH2_CLIENT_ID, OAUTH_CONSUMER_SECRET);
                Log.d(TAG, "refresh token: " + AccessToken.getInstance(ConnectFitbitActivity.this).getRefreshToken());
                AccessToken newAccessToken = fitbitService.refreshToken("refresh_token", AccessToken.getInstance(ConnectFitbitActivity.this).getRefreshToken());
                Log.d(TAG, "successfully retrieved refreshed access token, saving to preferences");
                AccessToken.saveNewAccessToken(newAccessToken, ConnectFitbitActivity.this);
                updateStatusText();
            }
            catch (RetrofitError retrofitErr) {
                String reason = retrofitErr.getResponse().getReason();
                int status = retrofitErr.getResponse().getStatus();
                String message = retrofitErr.getMessage();
                String body = (new String(((TypedByteArray)retrofitErr.getResponse().getBody()).getBytes()));
                Log.e(TAG, "exception refreshing access token:"
                        + "\nstatus: " + status
                        + "\nreason: " + reason
                        + "\nmessage: " + message
                        + "\nbody: " + body);
            }
            catch (Exception ex) {
                Log.e(TAG, "exception: " + ex.toString());
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                Toast.makeText(ConnectFitbitActivity.this, result, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateStatusText() {
        // Read the preferences to see if we have tokens
        if (AccessToken.getInstance(this).getAccessToken() != null) {
            txtStatus.setText(getString(R.string.oauth_success_message));
        }
        else {
            txtStatus.setText(getString(R.string.oauth_not_configured_message));
        }
    }

    private void initializeFitbitService() {

    }
}
