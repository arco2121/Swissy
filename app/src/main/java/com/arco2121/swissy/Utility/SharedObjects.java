package com.arco2121.swissy.Utility;

import android.graphics.drawable.Drawable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedObjects {
    static final HashMap<String, Object[]> sharedObjects = new HashMap<>();

    public static Object addObj(String key, Object io, int drawableResorce) {
        Object[] entry = { io, drawableResorce };
        sharedObjects.put(key, entry);
        return io;
    }
    public static Object[] getObj(String io) {
        return sharedObjects.get(io);
    }
    public static String asString() {
        return sharedObjects.keySet().toString();
    }
    public static List<String> asList() {
        return new ArrayList<>(sharedObjects.keySet());
    }
    public static String[] asArray() {
        return sharedObjects.keySet().toArray(new String[0]);
    }
}
