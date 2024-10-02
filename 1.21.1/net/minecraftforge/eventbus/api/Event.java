package net.minecraftforge.eventbus.api;

public class Event
{
    public boolean isCanceled()
    {
        return false;
    }

    public Event.Result getResult()
    {
        return null;
    }

    public static enum Result
    {
        DENY,
        DEFAULT,
        ALLOW;
    }
}
