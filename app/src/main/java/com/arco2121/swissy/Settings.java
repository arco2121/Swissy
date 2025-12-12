package com.arco2121.swissy;

import static com.arco2121.swissy.Utility.SharedObjects.animateButton;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.arco2121.swissy.Managers.PermissionManager;
import com.arco2121.swissy.Managers.SettingsManager;


public class Settings extends AppCompatActivity {
    private PermissionManager permissionManager;
    private SharedPreferences see;
    private SharedPreferences.Editor write;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        permissionManager = new PermissionManager(this);
        see = SettingsManager.getPropreties(this);
        write = SettingsManager.getEditor(this);
        float scale = (float) getResources().getInteger(R.integer.scaleMinus) / 100;
        long duration = getResources().getInteger(R.integer.icon_duration);
        TextView tema = findViewById(R.id.settingsTheme);
        TextView permessi = findViewById(R.id.openSettings);
        tema.setOnTouchListener((v, event) -> animateButton(v, event, scale, scale, duration, () -> {
            int now = see.getInt("theme", 0);
            if(now + 1 < 3) now = now + 1; else now = 0;
            write.putInt("theme", now);
            write.apply();
            String mode = "";
            switch (now) {
                case 0 : AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ); mode = "Auto"; break;
                case 1 : AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO
                ); mode = "Light"; break;
                case 2 : AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_YES
                ); mode = "Dark";
            }
            tema.setText(mode);
        }, true));
        permessi.setOnTouchListener((v, event) -> animateButton(v, event, scale, scale, duration, () -> {
            permissionManager.openSettings(this);
        }, true));
    }

}
