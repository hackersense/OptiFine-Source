package net.optifine.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.optifine.Config;

public class StaticMap
{
    private static final Map<String, Object> MAP = Collections.synchronizedMap(new HashMap<>());

    public static boolean contains(String key)
    {
        return MAP.containsKey(key);
    }

    public static boolean contains(String key, Object value)
    {
        if (!MAP.containsKey(key))
        {
            return false;
        }
        else
        {
            Object object = MAP.get(key);
            return Config.equals(object, value);
        }
    }

    public static Object get(String key)
    {
        return MAP.get(key);
    }

    public static void put(String key, Object val)
    {
        MAP.put(key, val);
    }

    public static void remove(String key)
    {
        MAP.remove(key);
    }

    public static int getInt(String key, int def)
    {
        return !(MAP.get(key) instanceof Integer integer) ? def : integer;
    }

    public static int putInt(String key, int val)
    {
        int i = getInt(key, 0);
        Integer integer = val;
        MAP.put(key, integer);
        return i;
    }

    public static long getLong(String key, long def)
    {
        return !(MAP.get(key) instanceof Long olong) ? def : olong;
    }

    public static void putLong(String key, long val)
    {
        Long olong = val;
        MAP.put(key, olong);
    }

    public static long putLong(String key, long val, long def)
    {
        long i = getLong(key, def);
        Long olong = val;
        MAP.put(key, olong);
        return i;
    }

    public static long addLong(String key, long val, long def)
    {
        long i = getLong(key, def);
        i += val;
        putLong(key, i);
        return i;
    }
}
