package com.bryankrosenbaum.stepintime;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
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
import com.bryankrosenbaum.stepintime.rest.FitbitRequestInterceptor;
import com.bryankrosenbaum.stepintime.rest.FitbitService;
import com.bryankrosenbaum.stepintime.signpostretrofit.RetrofitHttpOAuthConsumer;
import com.bryankrosenbaum.stepintime.signpostretrofit.SigningOkClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;


public class ConnectFitbitActivity extends Activity {

    private String TAG = ConnectFitbitActivity.class.getSimpleName();

    private TextView txtStatus;

    private String OAUTH_CONSUMER_KEY;
    private String OAUTH_CONSUMER_SECRET;
    private String OAUTH_CALLBACK_SCHEME;
    private String OAUTH_CALLBACK_URL;

    private OAuthConsumer mConsumer;
    private OAuthProvider mProvider;
    private SharedPreferences prefs;

    private FitbitService fitbitService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_fitbit);

        txtStatus = (TextView) findViewById(R.id.txtStatus);

        OAUTH_CONSUMER_KEY = getString(R.string.oauth_consumer_key);
        OAUTH_CONSUMER_SECRET = getString(R.string.oauth_consumer_secret);
        OAUTH_CALLBACK_SCHEME = getString(R.string.oauth_callback_scheme);
        OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME + "://" + getString(R.string.oauth_callback_host);

        mConsumer = new CommonsHttpOAuthConsumer(OAUTH_CONSUMER_KEY, OAUTH_CONSUMER_SECRET);
        mProvider = new DefaultOAuthProvider(
                getString(R.string.oauth_uri_request_token),
                getString(R.string.oauth_uri_access_token),
                getString(R.string.oauth_uri_authorize));

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
        if (uri != null && uri.getScheme().equals(OAUTH_CALLBACK_SCHEME)) {
            Log.d(TAG, "callback: " + uri.getPath());

            String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
            Log.d(TAG, "verifier: " + verifier);

            new RetrieveAccessTokenTask(this).execute(verifier);
        }
    }

    public void onClickAuthorizeApp (View v) {
        new OAuthAuthorizeTask().execute();
    }

    public void onClickTestProfile(View v) {

        if (fitbitService == null) {
            initializeFitbitService();
        }

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

        if (fitbitService == null) {
            initializeFitbitService();
        }

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

    // Responsible for starting the FitBit authorization
    class OAuthAuthorizeTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String authUrl;
            String message = null;
            try {
                Log.d(TAG, "callback url: " + OAUTH_CALLBACK_URL);
                authUrl = mProvider.retrieveRequestToken(mConsumer, OAUTH_CALLBACK_URL);
                Log.d(TAG, "authUrl: " + authUrl);

                // open browser for auth; add flags so back button doesn't open up browser
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
                startActivity(intent);
            } catch (OAuthMessageSignerException e) {
                message = "OAuthMessageSignerException";
                e.printStackTrace();
            } catch (OAuthNotAuthorizedException e) {
                message = "OAuthNotAuthorizedException";
                e.printStackTrace();
            } catch (OAuthExpectationFailedException e) {
                message = "OAuthExpectationFailedException";
                e.printStackTrace();
            } catch (OAuthCommunicationException e) {
                message = "OAuthCommunicationException";
                e.printStackTrace();
            }

            return message;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                Toast.makeText(ConnectFitbitActivity.this, result, Toast.LENGTH_LONG).show();
            }
        }
    }

    // Responsible for retrieving access tokens from FitBit on callback
    class RetrieveAccessTokenTask extends AsyncTask<String, Void, String> {

        public ConnectFitbitActivity myActivity = null;

        public RetrieveAccessTokenTask(ConnectFitbitActivity myActivity) {
            this.myActivity = myActivity;
        }

        @Override
        protected String doInBackground(String... params) {
            String message = null;
            String verifier = params[0];
            try {
                // Get the token
                Log.d(TAG, "mConsumer: " + mConsumer);
                Log.d(TAG, "mProvider: " + mProvider);

                mProvider.retrieveAccessToken(mConsumer, verifier);
                String token = mConsumer.getToken();
                String tokenSecret = mConsumer.getTokenSecret();
                mConsumer.setTokenWithSecret(token, tokenSecret);

                Log.d(TAG, String.format("verifier: %s, token: %s, tokenSecret: %s", verifier, token, tokenSecret));

                // Store token in preferences
                prefs.edit().putString(getString(R.string.oauth_token_name), token)
                        .putString(getString(R.string.oauth_token_secret_name), tokenSecret).commit();



            } catch (OAuthMessageSignerException e) {
                message = "OAuthMessageSignerException";
                e.printStackTrace();
            } catch (OAuthNotAuthorizedException e) {
                message = "OAuthNotAuthorizedException";
                e.printStackTrace();
            } catch (OAuthExpectationFailedException e) {
                message = "OAuthExpectationFailedException";
                e.printStackTrace();
            } catch (OAuthCommunicationException e) {
                message = "OAuthCommunicationException";
                e.printStackTrace();
            }
            return message;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                Toast.makeText(ConnectFitbitActivity.this, result, Toast.LENGTH_LONG).show();
            }
        }
    }

    // Request Fitbit data from API
    class MakeRequest extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {
            String message = null;
            String[] responseXml = new String[params.length];

            for (int i = 0; i < params.length; i++) {
                String requestUrl = params[i];
                responseXml[i] = null;

                DefaultHttpClient httpclient = new DefaultHttpClient();
                HttpGet request = new HttpGet(requestUrl);
                try {
                    mConsumer.sign(request);
                    HttpResponse response = httpclient.execute(request);
                    Log.i(TAG, "Statusline : " + response.getStatusLine());
                    InputStream data = response.getEntity().getContent();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(data));
                    String responseLine;
                    StringBuilder responseBuilder = new StringBuilder();

                    while ((responseLine = bufferedReader.readLine()) != null) {
                        responseBuilder.append(responseLine);
                    }
                    responseXml[i] = responseBuilder.toString();

                } catch (OAuthMessageSignerException e) {
                    message = "OAuthMessageSignerException";
                    e.printStackTrace();
                } catch (OAuthExpectationFailedException e) {
                    message = "OAuthExpectationFailedException";
                    e.printStackTrace();
                } catch (OAuthCommunicationException e) {
                    message = "OAuthCommunicationException";
                    e.printStackTrace();
                } catch (ClientProtocolException e) {
                    message = "ClientProtocolException";
                    e.printStackTrace();
                } catch (IOException e) {
                    message = "IOException";
                    e.printStackTrace();
                }
            }
            return responseXml;
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            Log.d(TAG, result[0]);
            Toast.makeText(ConnectFitbitActivity.this, "Successful response: " + result[0], Toast.LENGTH_LONG).show();
        }
    }

    private void updateStatusText() {
        // Read the preferences to see if we have tokens
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String token = prefs.getString(getString(R.string.oauth_token_name), null);
        String tokenSecret = prefs.getString(getString(R.string.oauth_token_secret_name), null);
        if (token != null && tokenSecret != null) {
            mConsumer.setTokenWithSecret(token, tokenSecret); // We have tokens, use them
            txtStatus.setText(getString(R.string.oauth_success_message));
        }
        else {
            txtStatus.setText(getString(R.string.oauth_not_configured_message));
        }
    }

    private void initializeFitbitService() {

        FitbitRequestInterceptor fitbitRequestInterceptor = new FitbitRequestInterceptor();
        Gson gsonConv = new GsonBuilder().create();

        String token = mConsumer.getToken();
        String tokenSecret = mConsumer.getTokenSecret();

        RetrofitHttpOAuthConsumer oAuthConsumer = new RetrofitHttpOAuthConsumer(OAUTH_CONSUMER_KEY, OAUTH_CONSUMER_SECRET);
        oAuthConsumer.setTokenWithSecret(token, tokenSecret);
        OkClient okClient = new SigningOkClient(oAuthConsumer);
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(getString(R.string.fitbit_base_uri))
                .setRequestInterceptor(fitbitRequestInterceptor)
                .setConverter(new GsonConverter(gsonConv))
                .setClient(okClient)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        fitbitService = restAdapter.create(FitbitService.class);
    }
}
