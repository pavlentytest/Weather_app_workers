package org.asdtm.goodweather;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import org.asdtm.goodweather.utils.LanguageUtil;
import org.asdtm.goodweather.utils.PreferenceUtil;
import org.asdtm.goodweather.utils.PreferenceUtil.Theme;

public class GoodWeatherApp extends Application {

    private static final String TAG = "GoodWeatherApp";

    private static Theme sTheme = Theme.light;

    @Override
    public void onCreate() {
        super.onCreate();
        sTheme = PreferenceUtil.getTheme(this);
        Log.d("RRRRRRR","onCreate() app");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LanguageUtil.setLanguage(this, PreferenceUtil.getLanguage(this));
    }

    public void reloadTheme() {
        sTheme = PreferenceUtil.getTheme(this);
    }

    public void applyTheme(Activity activity) {
        activity.setTheme(getThemeResId());
    }

    public static int getThemeResId() {
        switch (sTheme) {
            case light:
                return R.style.AppThemeLight;
            case dark:
                return R.style.AppThemeDark;
            default:
                return R.style.AppThemeLight;
        }
    }
}
