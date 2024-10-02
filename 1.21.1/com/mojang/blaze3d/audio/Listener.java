package com.mojang.blaze3d.audio;

import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;

public class Listener
{
    private float gain = 1.0F;
    private ListenerTransform transform = ListenerTransform.INITIAL;

    public void setTransform(ListenerTransform p_312167_)
    {
        this.transform = p_312167_;
        Vec3 vec3 = p_312167_.position();
        Vec3 vec31 = p_312167_.forward();
        Vec3 vec32 = p_312167_.up();
        AL10.alListener3f(4100, (float)vec3.x, (float)vec3.y, (float)vec3.z);
        AL10.alListenerfv(
            4111,
            new float[]
            {
                (float)vec31.x, (float)vec31.y, (float)vec31.z, (float)vec32.x(), (float)vec32.y(), (float)vec32.z()
            }
        );
    }

    public void setGain(float p_83738_)
    {
        AL10.alListenerf(4106, p_83738_);
        this.gain = p_83738_;
    }

    public float getGain()
    {
        return this.gain;
    }

    public void reset()
    {
        this.setTransform(ListenerTransform.INITIAL);
    }

    public ListenerTransform getTransform()
    {
        return this.transform;
    }
}
