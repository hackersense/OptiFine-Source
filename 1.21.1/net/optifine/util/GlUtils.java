package net.optifine.util;

import com.mojang.blaze3d.platform.GlStateManager;
import net.optifine.render.GlBlendState;
import net.optifine.render.GlCullState;
import net.optifine.render.GlDepthState;

public class GlUtils
{
    private static GlBlendState blendState = new GlBlendState();
    private static GlDepthState depthState = new GlDepthState();
    private static GlCullState cullState = new GlCullState();

    public static GlBlendState getBlendState()
    {
        GlStateManager.getBlendState(blendState);
        return blendState;
    }

    public static GlDepthState getDepthState()
    {
        GlStateManager.getDepthState(depthState);
        return depthState;
    }

    public static GlCullState getCullState()
    {
        GlStateManager.getCullState(cullState);
        return cullState;
    }
}
