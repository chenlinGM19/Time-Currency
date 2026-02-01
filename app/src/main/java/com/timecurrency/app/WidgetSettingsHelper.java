package com.timecurrency.app;

import android.content.Context;
import android.content.SharedPreferences;

public class WidgetSettingsHelper {
    private static final String PREFS_NAME = "com.timecurrency.app.WidgetPrefs";
    private static final String PREF_PREFIX_KEY = "appwidget_";

    public static void saveStyle(Context context, int appWidgetId, int style) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "_style", style);
        prefs.apply();
    }

    public static int loadStyle(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "_style", 0); // 0 is Classic
    }

    public static void saveTransparency(Context context, int appWidgetId, int alpha) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "_alpha", alpha);
        prefs.apply();
    }

    public static int loadTransparency(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "_alpha", 255);
    }
    
    public static void saveImagePath(Context context, int appWidgetId, String path) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId + "_image", path);
        prefs.apply();
    }

    public static String loadImagePath(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + appWidgetId + "_image", null);
    }

    public static void deletePrefs(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_style");
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_alpha");
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_image");
        prefs.apply();
    }
}