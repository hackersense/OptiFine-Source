package net.optifine.render;

public class GlDepthState
{
    private boolean enabled;
    private boolean mask;
    private int func;

    public GlDepthState()
    {
        this.enabled = false;
        this.mask = true;
        this.func = 513;
    }

    public GlDepthState(boolean enabled, boolean mask, int func)
    {
        this.enabled = enabled;
        this.mask = mask;
        this.func = func;
    }

    public void setState(boolean enabled, boolean mask, int func)
    {
        this.enabled = enabled;
        this.mask = mask;
        this.func = func;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isMask()
    {
        return this.mask;
    }

    public void setMask(boolean mask)
    {
        this.mask = mask;
    }

    public int getFunc()
    {
        return this.func;
    }

    public void setFunc(int func)
    {
        this.func = func;
    }

    @Override
    public String toString()
    {
        return "enabled: " + this.enabled + ", mask: " + this.mask + ", func: " + this.func;
    }
}
