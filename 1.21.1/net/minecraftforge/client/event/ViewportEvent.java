package net.minecraftforge.client.event;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.eventbus.api.Event;

public abstract class ViewportEvent extends Event
{
    public ViewportEvent(GameRenderer renderer, Camera camera, double partialTick)
    {
    }

    public GameRenderer getRenderer()
    {
        return null;
    }

    public Camera getCamera()
    {
        return null;
    }

    public double getPartialTick()
    {
        return 0.0;
    }

    public static class ComputeCameraAngles extends ViewportEvent
    {
        public ComputeCameraAngles(GameRenderer renderer, Camera camera, double renderPartialTicks, float yaw, float pitch, float roll)
        {
            super(renderer, camera, renderPartialTicks);
        }

        public float getYaw()
        {
            return 0.0F;
        }

        public void setYaw(float yaw)
        {
        }

        public float getPitch()
        {
            return 0.0F;
        }

        public void setPitch(float pitch)
        {
        }

        public float getRoll()
        {
            return 0.0F;
        }

        public void setRoll(float roll)
        {
        }
    }

    public static class ComputeFogColor extends ViewportEvent
    {
        public ComputeFogColor(Camera camera, float partialTicks, float red, float green, float blue)
        {
            super(null, camera, (double)partialTicks);
        }

        public float getRed()
        {
            return 0.0F;
        }

        public void setRed(float red)
        {
        }

        public float getGreen()
        {
            return 0.0F;
        }

        public void setGreen(float green)
        {
        }

        public float getBlue()
        {
            return 0.0F;
        }

        public void setBlue(float blue)
        {
        }
    }

    public static class ComputeFov extends ViewportEvent
    {
        public ComputeFov(GameRenderer renderer, Camera camera, double renderPartialTicks, double fov, boolean usedConfiguredFov)
        {
            super(renderer, camera, renderPartialTicks);
        }

        public double getFOV()
        {
            return 0.0;
        }

        public void setFOV(double fov)
        {
        }

        public boolean usedConfiguredFov()
        {
            return false;
        }
    }

    public static class RenderFog extends ViewportEvent
    {
        public RenderFog(
            FogRenderer.FogMode mode, FogType type, Camera camera, float partialTicks, float nearPlaneDistance, float farPlaneDistance, FogShape fogShape
        )
        {
            super(null, camera, (double)partialTicks);
        }

        public FogRenderer.FogMode getMode()
        {
            return null;
        }

        public FogType getType()
        {
            return null;
        }

        public float getFarPlaneDistance()
        {
            return 0.0F;
        }

        public float getNearPlaneDistance()
        {
            return 0.0F;
        }

        public FogShape getFogShape()
        {
            return null;
        }

        public void setFarPlaneDistance(float distance)
        {
        }

        public void setNearPlaneDistance(float distance)
        {
        }

        public void setFogShape(FogShape shape)
        {
        }

        public void scaleFarPlaneDistance(float factor)
        {
        }

        public void scaleNearPlaneDistance(float factor)
        {
        }
    }
}
