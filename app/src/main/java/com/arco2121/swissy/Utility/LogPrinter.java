package com.arco2121.swissy.Utility;

import android.content.Context;
import android.widget.Toast;

public class LogPrinter {
    public static void printToast(Context context, String message) {
        int duration = message.length() < 10 ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
        Toast.makeText(context, message, duration).show();
    }
}