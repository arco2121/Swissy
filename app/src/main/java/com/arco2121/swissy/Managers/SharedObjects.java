package com.arco2121.swissy.Managers;

import androidx.annotation.NonNull;

import java.util.HashMap;

public class SharedObjects {
    static final HashMap<String, Object> sharedObjects = new HashMap<>();

    public static Object addObj(String key, Object io) {
        sharedObjects.put(key, io);
        return io;
    }
    public static Object getObj(String io) {
        return sharedObjects.get(io);
    }
    public static String asString() {
        return sharedObjects.keySet().toString();
    }
}
