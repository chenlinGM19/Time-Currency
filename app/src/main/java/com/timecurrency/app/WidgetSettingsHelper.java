package com.timecurrency.app;

import android.content.Context;
import android.content.SharedPreferences;

public class WidgetSettingsHelper {
    private static final String PREFS_NAME = "com.timecurrency.app.WidgetPrefs";
    private static final String PREF_PREFIX_KEY = "appwidget_";

    // Background Type: 0 = Color, 1 = Image
    public static void saveBackgroundType(Context context, int appWidgetId, int type) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "_bg_type", type);
        prefs.apply();
    }

    public static int loadBackgroundType(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "_bg_type", 0); // Default Color
    }

    // X Offset (dp)
    public static void saveXOffset(Context context, int appWidgetId, int xOffset) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "_offset_x", xOffset);
        prefs.apply();
    }

    public static int loadXOffset(Context context, int appWidgetId) {
        return context.getSharedPreferences(PREFS_NAME, 0).getInt(PREF_PREFIX_KEY + appWidgetId + "_offset_x", 0);
    }

    // Y Offset (dp)
    public static void saveYOffset(Context context, int appWidgetId, int yOffset) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "_offset_y", yOffset);
        prefs.apply();
    }

    public static int loadYOffset(Context context, int appWidgetId) {
        return context.getSharedPreferences(PREFS_NAME, 0).getInt(PREF_PREFIX_KEY + appWidgetId + "_offset_y", 0);
    }

    public static void saveStyle(Context context, int appWidgetId, int style) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "_style", style);
        prefs.apply();
    }

    public static int loadStyle(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "_style", 0);
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
    
    public static void saveCornerRadius(Context context, int appWidgetId, int radius) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "_radius", radius);
        prefs.apply();
    }

    public static int loadCornerRadius(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "_radius", 16); 
    }

    public static void deletePrefs(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_bg_type");
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_offset_x");
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_offset_y");
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_style");
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_alpha");
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_image");
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_radius");
        prefs.apply();
    }
}