package net.optifine;

import net.minecraft.client.Minecraft;

public class McDebugInfo
{
    private Minecraft minecraft = Minecraft.getInstance();
    private String mcDebug = this.minecraft.fpsString;

    public boolean isChanged()
    {
        if (this.mcDebug == this.minecraft.fpsString)
        {
            return false;
        }
        else
        {
            this.mcDebug = this.minecraft.fpsString;
            return true;
        }
    }
}
