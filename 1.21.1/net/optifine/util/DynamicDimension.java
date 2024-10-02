package net.optifine.util;

import java.awt.Dimension;

public class DynamicDimension
{
    private boolean relative = false;
    private float width;
    private float height;

    public DynamicDimension(boolean relative, float width, float height)
    {
        this.relative = relative;
        this.width = width;
        this.height = height;
    }

    public boolean isRelative()
    {
        return this.relative;
    }

    public float getWidth()
    {
        return this.width;
    }

    public float getHeight()
    {
        return this.height;
    }

    public Dimension getDimension(int baseWidth, int baseHeight)
    {
        return this.relative
               ? new Dimension((int)(this.width * (float)baseWidth), (int)(this.height * (float)baseHeight))
               : new Dimension((int)this.width, (int)this.height);
    }

    @Override
    public int hashCode()
    {
        int i = this.relative ? 1 : 0;
        i = i * 37 + (int)this.width;
        return i * 37 + (int)this.height;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof DynamicDimension dynamicdimension))
        {
            return false;
        }
        else if (this.relative != dynamicdimension.relative)
        {
            return false;
        }
        else
        {
            return this.width != dynamicdimension.width ? false : this.height == dynamicdimension.height;
        }
    }
}
