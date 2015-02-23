package com.bryankrosenbaum.stepintime.alarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bryankrosenbaum.stepintime.R;
import com.bryankrosenbaum.stepintime.model.ActivityStep;
import com.bryankrosenbaum.stepintime.model.ActivityStepWrapper;
import com.bryankrosenbaum.stepintime.model.ScheduledEvent;
import com.bryankrosenbaum.stepintime.rest.FitbitRequestInterceptor;
import com.bryankrosenbaum.stepintime.rest.FitbitService;
import com.bryankrosenbaum.stepintime.signpostretrofit.RetrofitHttpOAuthConsumer;
import com.bryankrosenbaum.stepintime.signpostretrofit.SigningOkClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.DecimalFormat;
import java.util.Calendar;

import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

public class EventReminderAlarmBroadcastReceiver extends BroadcastReceiver {

    private String TAG = EventReminderAlarmBroadcastReceiver.class.getSimpleName();

    private String OAUTH_CONSUMER_KEY;
    private String OAUTH_CONSUMER_SECRET;
    private SharedPreferences prefs;
    private FitbitService fitbitService;

    private Uri soundUri;


    public EventReminderAlarmBroadcastReceiver() {
        //Define sound URI
        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        final String time = extras.getString(context.getString(R.string.intent_extra_time));
        final String stepCountScheduled = extras.getString(context.getString(R.string.intent_extra_step_count_scheduled));

        final Context contextFinal = context;

        Log.d(TAG, "Received Broadcast (" + time + ", " + stepCountScheduled + ")");

        if (fitbitService == null) {
            initializeFitbitService(context);
        }

        fitbitService.getStepCountToday(new retrofit.Callback<ActivityStepWrapper>() {
            @Override
            public void success(ActivityStepWrapper activityStepWrapper, Response response) {
                ActivityStep[] activitySteps = activityStepWrapper.getActivitiesSteps();
                if (activitySteps.length == 0) {
                    Log.w(TAG, "getStepCountToday returned empty");
                    createErrorNotification(contextFinal, contextFinal.getString(R.string.fitbit_webservice_no_records));
                }
                else if (activitySteps.length > 1) {
                    Log.w(TAG, "getStepCountToday returned " + activitySteps.length + " results");
                    // TODO: REMOVE NOTIFICATION
                    createErrorNotification(contextFinal, contextFinal.getString(R.string.fitbit_webservice_too_many_records));
                }
                // expected: activitySteps.length == 1
                else {
                    Log.d(TAG, "getStepCountToday returned 1 result for " + activitySteps[0].getDateTime() + " : " + activitySteps[0].getValue());
                    int stepCountActual = activitySteps[0].getValue();

                    if (stepCountScheduled == null) {
                        Log.e(TAG, "stepCountScheduled is null, so cannot show reminder");
                    }
                    else {
                        Log.d(TAG, "step count actual=" + stepCountActual + ", scheduled = " + stepCountScheduled);
                        createNotification(contextFinal, time, stepCountScheduled, Integer.toString(stepCountActual));
                    }
                    /*else if (Integer.parseInt(stepCountScheduled) > stepCountActual) {
                        Log.d(TAG, "step count actual is less than scheduled, so show reminder");
                        createNotification(contextFinal, time, stepCountScheduled, Integer.toString(stepCountActual));
                    }
                    else {
                        Log.d(TAG, "step count actual is greater than scheduled, so do not show reminder");
                        createErrorNotification(contextFinal, "Good job: " + Integer.toString(stepCountActual) + " actual vs " + stepCountScheduled + " scheduled");
                    }*/
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "error retreiving step counts: " + error);
                createErrorNotification(contextFinal, contextFinal.getString(R.string.fitbit_webservice_error));
            }
        });
    }

    public void setAlarm(Context context, ScheduledEvent scheduledEvent) {
        setAlarm(context, scheduledEvent.getHour(), scheduledEvent.getMinute(), scheduledEvent.getStepCount());
    }

    public void setAlarm(Context context, int hour, int minute, int stepCount)
    {
        String time = formatTime(context, hour, minute);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent eventReminderIntent = new Intent(context, EventReminderAlarmBroadcastReceiver.class);
        eventReminderIntent.putExtra(context.getString(R.string.intent_extra_time), time);
        eventReminderIntent.putExtra(context.getString(R.string.intent_extra_step_count_scheduled), "" + stepCount);
        PendingIntent eventReminderPendingIntent = PendingIntent.getBroadcast(context, getRequestCode(hour, minute), eventReminderIntent, 0);

        // Set the alarm
        Calendar calendarAlarmTime = Calendar.getInstance();
        calendarAlarmTime.setTimeInMillis(System.currentTimeMillis());

        long currentTime = calendarAlarmTime.getTimeInMillis();

        calendarAlarmTime.set(Calendar.HOUR_OF_DAY, hour);
        calendarAlarmTime.set(Calendar.MINUTE, minute);
        calendarAlarmTime.set(Calendar.SECOND, 0);

        // if the scheduled time has already passed today, schedule starting tomorrow
        if (currentTime > calendarAlarmTime.getTimeInMillis()) {
            Log.d(TAG, "schedule time has already passed, so schedule starting tomorrow");
            calendarAlarmTime.add(Calendar.DATE, 1);
        }

        // repeat daily
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendarAlarmTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, eventReminderPendingIntent);

        Log.d(TAG, "alarm set for " + hour + ":" + minute + " with stepCount = " + stepCount);
    }

    public void cancelAlarm(Context context, ScheduledEvent scheduledEvent) {
        cancelAlarm(context, scheduledEvent.getHour(), scheduledEvent.getMinute());
    }

    public void cancelAlarm(Context context, int hour, int minute)
    {
        Intent eventReminderIntent = new Intent(context, EventReminderAlarmBroadcastReceiver.class);
        PendingIntent eventReminderPendingIntent = PendingIntent.getBroadcast(context, getRequestCode(hour, minute), eventReminderIntent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(eventReminderPendingIntent);
    }

    private int getRequestCode(int hour, int minute) {
        return (hour * 100) + minute;
    }

    private void createNotification(Context context, String time, String stepCountScheduled, String stepCountActual) {

        int smallIcon;
        int stepCountOver = Integer.parseInt(stepCountActual) - Integer.parseInt(stepCountScheduled);
        if (stepCountOver < 0) {
            smallIcon = R.drawable.ic_stat_stepintime_walk;
        }
        else {
            smallIcon = R.drawable.ic_stat_stepintime_smiley;
        }

        DecimalFormat formatter = new DecimalFormat("#,###");

        String notificationText = time + " - " + formatter.format((double)Math.abs(stepCountOver)) + " steps " + (stepCountOver >= 0 ? "ahead of" : "behind") + " schedule";

        Notification notification = new Notification.Builder(context)
                .setSmallIcon(smallIcon)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(notificationText)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .build();

        int uniqueId = 1;
        try {
            uniqueId = Integer.parseInt(time.replaceAll("[^0-9]", ""));
        }
        catch (NumberFormatException numberFormatEx) {
            Log.e(TAG, "NumberFormatException trying to parse time from extra (" + time + "): " + numberFormatEx);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(uniqueId, notification);
    }

    private void createErrorNotification(Context context, String errorMessage) {
        Notification.Builder notificationBuilder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_stepintime)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(errorMessage);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());
    }

    private void initializeFitbitService(Context context) {

        OAUTH_CONSUMER_KEY = context.getString(R.string.oauth_consumer_key);
        OAUTH_CONSUMER_SECRET = context.getString(R.string.oauth_consumer_secret);

        FitbitRequestInterceptor fitbitRequestInterceptor = new FitbitRequestInterceptor();
        Gson gsonConv = new GsonBuilder().create();

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String token = prefs.getString(context.getString(R.string.oauth_token_name), null);
        String tokenSecret = prefs.getString(context.getString(R.string.oauth_token_secret_name), null);

        RetrofitHttpOAuthConsumer oAuthConsumer = new RetrofitHttpOAuthConsumer(OAUTH_CONSUMER_KEY, OAUTH_CONSUMER_SECRET);
        oAuthConsumer.setTokenWithSecret(token, tokenSecret);
        OkClient okClient = new SigningOkClient(oAuthConsumer);
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(context.getString(R.string.fitbit_base_uri))
                .setRequestInterceptor(fitbitRequestInterceptor)
                .setConverter(new GsonConverter(gsonConv))
                .setClient(okClient)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        fitbitService = restAdapter.create(FitbitService.class);
    }

    private String formatTime(Context context, int hour, int minute) {
        String time = "";
        if (hour == 0) {
            time = "12:";
        }
        else if (hour <= 12) {
            time = hour + ":";
        }
        else {
            time = (hour - 12) + ":";
        }
        if (minute < 10) {
            time += "0";
        }
        time += minute;
        if (hour < 12) {
            time += context.getString(R.string.time_am);
        }
        else {
            time += context.getString(R.string.time_pm);
        }
        return time;
    }
}
