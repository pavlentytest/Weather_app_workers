package org.asdtm.goodweather.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;

import org.asdtm.goodweather.ConnectionDetector;
import org.asdtm.goodweather.MainActivity;
import org.asdtm.goodweather.R;
import org.asdtm.goodweather.WeatherJSONParser;
import org.asdtm.goodweather.WeatherRequest;
import org.asdtm.goodweather.model.Weather;
import org.asdtm.goodweather.utils.AppPreference;
import org.asdtm.goodweather.utils.Constants;
import org.asdtm.goodweather.utils.LanguageUtil;
import org.asdtm.goodweather.utils.PreferenceUtil;
import org.asdtm.goodweather.utils.Utils;
import org.json.JSONException;

import java.io.IOException;
import java.util.Locale;

public class NotificationService extends IntentService {

    private static final String TAG = "NotificationsService";
    public static final String NOTIFICATION_CHANNEL_ID = NotificationService.class.getSimpleName();

    public NotificationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ConnectionDetector checkNetwork = new ConnectionDetector(this);
        if (!checkNetwork.isNetworkAvailableAndConnected()) {
            return;
        }

        SharedPreferences preferences = getSharedPreferences(Constants.APP_SETTINGS_NAME, 0);
        String latitude = preferences.getString(Constants.APP_SETTINGS_LATITUDE, "51.51");
        String longitude = preferences.getString(Constants.APP_SETTINGS_LONGITUDE, "-0.13");
        String locale = LanguageUtil.getLanguageName(PreferenceUtil.getLanguage(this));
        String units = AppPreference.getTemperatureUnit(this);

        Weather weather;
        try {
            String weatherRaw = new WeatherRequest().getItems(latitude, longitude, units, locale);
            weather = WeatherJSONParser.getWeather(weatherRaw);

            AppPreference.saveLastUpdateTimeMillis(this);
            AppPreference.saveWeather(this, weather);
            weatherNotification(weather);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error get weather", e);
        }
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, NotificationService.class);
    }

    public static void setNotificationServiceAlarm(Context context,
                                                   boolean isNotificationEnable) {
        Log.d("RRRR","setNotification!!!");
        Intent intent = NotificationService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent,
                                                               PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        String intervalPref = AppPreference.getInterval(context);
        Log.d("RRRR","intervalPref" + intervalPref);

        long intervalMillis = Utils.intervalMillisForAlarm(intervalPref);

        if (isNotificationEnable) {

            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                             SystemClock.elapsedRealtime() + intervalMillis,
                                             intervalMillis,
                                             pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private void weatherNotification(Weather weather) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent launchIntent = PendingIntent.getActivity(this, 0, intent, 0);

        String temperatureScale = Utils.getTemperatureScale(this);
        String speedScale = Utils.getSpeedScale(this);

        String temperature = String.format(Locale.getDefault(), "%.1f",
                                           weather.temperature.getTemp());

        String wind = getString(R.string.wind_label, String.format(Locale.getDefault(),
                                                                   "%.1f",
                                                                   weather.wind.getSpeed()),
                                speedScale);
        String humidity = getString(R.string.humidity_label,
                                    String.valueOf(weather.currentCondition.getHumidity()),
                                    getString(R.string.percent_sign));
        String pressure = getString(R.string.pressure_label,
                                    String.format(Locale.getDefault(), "%.1f",
                                                  weather.currentCondition.getPressure()),
                                    getString(R.string.pressure_measurement));
        String cloudiness = getString(R.string.pressure_label,
                                      String.valueOf(weather.cloud.getClouds()),
                                      getString(R.string.percent_sign));

        StringBuilder notificationText = new StringBuilder(wind)
                .append("  ")
                .append(humidity)
                .append("  ")
                .append(pressure)
                .append("  ")
                .append(cloudiness);

        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_ID+"_name",
                NotificationManager.IMPORTANCE_HIGH);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(temperature
                        + temperatureScale
                        + "  "
                        + weather.currentWeather.getDescription())
                .setContentText(notificationText)
                .setOngoing(true)
                .setContentIntent(launchIntent)
                .setSmallIcon(R.drawable.small_icon)
                .setTicker(temperature
                        + temperatureScale
                        + "  "
                        + weather.location.getCityName()
                        + ", "
                        + weather.location.getCountryCode())
                .setCategory(Notification.CATEGORY_SERVICE)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());

    }

}
