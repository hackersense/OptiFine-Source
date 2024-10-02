package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.joml.Matrix4f;

public interface ClientTooltipComponent
{
    static ClientTooltipComponent create(FormattedCharSequence p_169949_)
    {
        return new ClientTextTooltip(p_169949_);
    }

    static ClientTooltipComponent create(TooltipComponent p_169951_)
    {
        if (p_169951_ instanceof BundleTooltip bundletooltip)
        {
            return new ClientBundleTooltip(bundletooltip.contents());
        }
        else if (p_169951_ instanceof ClientActivePlayersTooltip.ActivePlayersTooltip clientactiveplayerstooltip$activeplayerstooltip)
        {
            return new ClientActivePlayersTooltip(clientactiveplayerstooltip$activeplayerstooltip);
        }
        else
        {
            throw new IllegalArgumentException("Unknown TooltipComponent");
        }
    }

    int getHeight();

    int getWidth(Font p_169952_);

default void renderText(Font p_169953_, int p_169954_, int p_169955_, Matrix4f p_253692_, MultiBufferSource.BufferSource p_169957_)
    {
    }

default void renderImage(Font p_194048_, int p_194049_, int p_194050_, GuiGraphics p_283459_)
    {
    }
}
