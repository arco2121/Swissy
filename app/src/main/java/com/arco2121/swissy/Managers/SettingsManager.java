package com.arco2121.swissy.Managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

public class SettingsManager {

    public static SharedPreferences getPropreties(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public static void LoadPropreties(Context ctx) {
        int t = getPropreties(ctx).getInt("theme", 0);
        switch (t) {
            case 0 : AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ); break;
            case 1 : AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
            ); break;
            case 2 : AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
            );
        }
    }

    public static SharedPreferences.Editor getEditor(Context ctx) {
        SharedPreferences temp = getPropreties(ctx);
        SharedPreferences.Editor editorSettings = temp.edit();
        //Settings di base di questa App
        if(!temp.contains("theme")) {
            editorSettings.putInt("theme", 0);
            editorSettings.apply();
        }
        if(!temp.contains("energysafer")) {
            editorSettings.putBoolean("energysafer", false);
            editorSettings.apply();
        }
        if(!temp.contains("vibration")) {
            editorSettings.putBoolean("vibration", true);
            editorSettings.apply();
        }
        int now = temp.getInt("theme", 0);
        switch (now) {
            case 0 : AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ); break;
            case 1 : AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
            ); break;
            case 2 : AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
            );
        }
        return editorSettings;
    }
}
