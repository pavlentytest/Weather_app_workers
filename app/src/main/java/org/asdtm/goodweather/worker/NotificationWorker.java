package org.asdtm.goodweather.worker;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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
import java.util.concurrent.TimeUnit;

public class NotificationWorker extends Worker {

    private static final String TAG = "NotificationsService";
    public static final String NOTIFICATION_CHANNEL_ID = NotificationWorker.class.getSimpleName();
    private Context context;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        ConnectionDetector checkNetwork = new ConnectionDetector(context);
        if (!checkNetwork.isNetworkAvailableAndConnected()) {
            Result.failure();
        }

        SharedPreferences preferences = getApplicationContext().getSharedPreferences(Constants.APP_SETTINGS_NAME, 0);
        String latitude = preferences.getString(Constants.APP_SETTINGS_LATITUDE, "51.51");
        String longitude = preferences.getString(Constants.APP_SETTINGS_LONGITUDE, "-0.13");
        String locale = LanguageUtil.getLanguageName(PreferenceUtil.getLanguage(context));
        String units = AppPreference.getTemperatureUnit(context);

        Weather weather;
        try {
            String weatherRaw = new WeatherRequest().getItems(latitude, longitude, units, locale);
            weather = WeatherJSONParser.getWeather(weatherRaw);

            AppPreference.saveLastUpdateTimeMillis(context);
            AppPreference.saveWeather(context, weather);
          // Log.d("RRRR",requestResult);
           // return parseWeather(requestResult);

            return weatherNotification(weather);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error get weather", e);
            return Result.failure();
        }
    }

    private Result weatherNotification(Weather weather) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent launchIntent = PendingIntent.getActivity(context, 0, intent, 0);

        String temperatureScale = Utils.getTemperatureScale(context);
        String speedScale = Utils.getSpeedScale(context);

        String temperature = String.format(Locale.getDefault(), "%.1f",
                weather.temperature.getTemp());

        String wind = context.getString(R.string.wind_label, String.format(Locale.getDefault(),
                "%.1f",
                weather.wind.getSpeed()),
                speedScale);
        String humidity = context.getString(R.string.humidity_label,
                String.valueOf(weather.currentCondition.getHumidity()),
                context.getString(R.string.percent_sign));
        String pressure = context.getString(R.string.pressure_label,
                String.format(Locale.getDefault(), "%.1f",
                        weather.currentCondition.getPressure()),
                context.getString(R.string.pressure_measurement));
        String cloudiness = context.getString(R.string.pressure_label,
                String.valueOf(weather.cloud.getClouds()),
                context.getString(R.string.percent_sign));

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

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
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

        return Result.success();
    }
}
