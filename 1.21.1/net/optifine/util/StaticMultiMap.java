package net.optifine.util;

import java.util.HashMap;
import java.util.Map;
import net.optifine.Config;

public class StaticMultiMap
{
    public static Map<String, Object> getMap(String mapName)
    {
        Object object = StaticMap.get(mapName);

        if (object == null)
        {
            object = new HashMap();
            StaticMap.put(mapName, object);
        }

        if (!(object instanceof Map))
        {
            throw new IllegalArgumentException("Not a map: " + object);
        }
        else
        {
            return (Map<String, Object>)object;
        }
    }

    public static void put(String mapName, String key, Object value)
    {
        Map<String, Object> map = getMap(mapName);
        map.put(key, value);
    }

    public static Object get(String mapName, String key)
    {
        Map<String, Object> map = getMap(mapName);
        return map.get(key);
    }

    public static boolean contains(String mapName, String key)
    {
        Map<String, Object> map = getMap(mapName);
        return map.containsKey(key);
    }

    public static boolean containsValue(String mapName, Object val)
    {
        Map<String, Object> map = getMap(mapName);
        return map.containsValue(val);
    }

    public static boolean contains(String mapName, String key, Object value)
    {
        Object object = get(mapName, key);
        return Config.equals(object, value);
    }
}
