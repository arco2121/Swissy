package com.arco2121.swissy;

import static com.arco2121.swissy.Utility.SharedObjects.animateButton;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.arco2121.swissy.Managers.PermissionManager;
import com.arco2121.swissy.Managers.SettingsManager;


public class Settings extends AppCompatActivity {
    private PermissionManager permissionManager;
    private SharedPreferences see;
    private SharedPreferences.Editor write;

    @SuppressLint("ClickableViewAccessibility")
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
        TextView openProject = findViewById(R.id.openLink);
        TextView energy = findViewById(R.id.settingsSafe);
        TextView vibration = findViewById(R.id.settingsVibra);
        ImageView back = findViewById(R.id.settingsBack);
        int themeV = see.getInt("theme", 0);
        tema.setText(themeV == 0 ? "Auto" : themeV == 1 ? "Light" : "Dark");
        boolean enV = see.getBoolean("energysafer", false);
        energy.setText(enV ? "On" : "Off");
        boolean enVi = see.getBoolean("vibration", true);
        vibration.setText(enVi ? "On" : "Off");
        back.setOnTouchListener((v, event) -> animateButton(v, event, scale, scale, duration, this::finish, true));
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
        energy.setOnTouchListener((v, event) -> animateButton(v, event, scale, scale, duration, () -> {
            boolean enDD = see.getBoolean("energysafer", false);
            enDD = !enDD;
            write.putBoolean("energysafer", enDD);
            write.apply();
            String mode = enDD ? "On" : "Off";
            energy.setText(mode);
            try {
                Thread.sleep(200);
                Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (intent != null) {
                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );
                    startActivity(intent);
                    Runtime.getRuntime().exit(0);
                }
            } catch (InterruptedException ignored) { }
        }, true));
        vibration.setOnTouchListener((v, event) -> animateButton(v, event, scale, scale, duration, () -> {
            boolean en = see.getBoolean("vibration", false);
            en = !en;
            write.putBoolean("vibration", en);
            write.apply();
            String mode = en ? "On" : "Off";
            vibration.setText(mode);
        }, true));
        openProject.setOnTouchListener((v, event) -> animateButton(v, event, scale, scale, duration, () -> {
            String url = getString(R.string.projectLink);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }, true));
    }
}
