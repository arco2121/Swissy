package com.arco2121.swissy.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SharedObjects {
    static final HashMap<String, Object[]> sharedObjects = new HashMap<>();

    public static Object addObj(String key, Object io, Object[] drawableResorces) {
        Object[] entry = { io, drawableResorces };
        sharedObjects.put(key, entry);
        return io;
    }
    public static Object[] getObj(String io) {
        return sharedObjects.get(io);
    }
    public static int findIndex(String ele) { return asList().indexOf(ele); }
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
