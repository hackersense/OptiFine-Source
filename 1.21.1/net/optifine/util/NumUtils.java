package net.optifine.util;

public class NumUtils
{
    public static int limit(int val, int min, int max)
    {
        if (val < min)
        {
            return min;
        }
        else
        {
            return val > max ? max : val;
        }
    }

    public static float limit(float val, float min, float max)
    {
        if (val < min)
        {
            return min;
        }
        else
        {
            return val > max ? max : val;
        }
    }

    public static double limit(double val, double min, double max)
    {
        if (val < min)
        {
            return min;
        }
        else
        {
            return val > max ? max : val;
        }
    }

    public static int mod(int x, int y)
    {
        int i = x % y;

        if (i < 0)
        {
            i += y;
        }

        return i;
    }
}
