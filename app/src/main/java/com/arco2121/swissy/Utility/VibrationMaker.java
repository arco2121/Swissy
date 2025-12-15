package com.arco2121.swissy.Utility;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

public class VibrationMaker {

    public enum Vibration {
        High, Low, Medium, Long, Short, ReverseHigh
    }

    static public void vibrate(View v, Vibration vibe) {
        switch (vibe) {
            case High : if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                break;
            }
            case ReverseHigh : if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                v.performHapticFeedback(HapticFeedbackConstants.REJECT);
                break;
            }
            case Short : v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                break;
            case Low : v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                break;
            case Medium : v.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE);
                break;
            case Long : v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                break;
        }
    }
}
