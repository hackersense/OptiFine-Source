package net.optifine.config;

public class OptionValueInt
{
    private int value;
    private String resourceKey;

    public OptionValueInt(int value, String resourceKey)
    {
        this.value = value;
        this.resourceKey = resourceKey;
    }

    public int getValue()
    {
        return this.value;
    }

    public String getResourceKey()
    {
        return this.resourceKey;
    }

    public boolean isOff()
    {
        return this.value == Option.OFF.value;
    }

    public boolean isCompact()
    {
        return this.value == Option.COMPACT.value;
    }

    public boolean isFull()
    {
        return this.value == Option.FULL.value;
    }

    @Override
    public String toString()
    {
        return this.value + ", " + this.resourceKey;
    }
}
