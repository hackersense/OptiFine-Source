package net.optifine.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class GuiButtonOF extends Button
{
    public final int id;

    public GuiButtonOF(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, Button.OnPress pressable, Button.CreateNarration narrationIn)
    {
        super(x, y, widthIn, heightIn, Component.literal(buttonText), pressable, narrationIn);
        this.id = buttonId;
    }

    public GuiButtonOF(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText)
    {
        this(buttonId, x, y, widthIn, heightIn, buttonText, btn ->
        {
        }, DEFAULT_NARRATION);
    }

    public GuiButtonOF(int buttonId, int x, int y, String buttonText)
    {
        this(buttonId, x, y, 200, 20, buttonText, btn ->
        {
        }, DEFAULT_NARRATION);
    }

    public void setMessage(String messageIn)
    {
        super.setMessage(Component.literal(messageIn));
    }

    public static void blit(GuiGraphics graphicsIn, ResourceLocation locationIn, int x, int y, int rectX, int rectY, int width, int height)
    {
        graphicsIn.blit(locationIn, x, y, rectX, rectY, width, height);
    }

    public void blit(
        GuiGraphics graphicsIn,
        ResourceLocation locationIn,
        int x,
        int y,
        int width,
        int height,
        float rectX,
        float rectY,
        int rectWidth,
        int rectHeight,
        int texWidth,
        int texHeight
    )
    {
        graphicsIn.blit(locationIn, x, y, width, height, rectX, rectY, rectWidth, rectHeight, texWidth, texHeight);
    }
}
