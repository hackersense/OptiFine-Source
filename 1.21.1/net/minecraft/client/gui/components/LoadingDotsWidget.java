package net.minecraft.client.gui.components;

import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public class LoadingDotsWidget extends AbstractWidget
{
    private final Font font;

    public LoadingDotsWidget(Font p_299146_, Component p_300579_)
    {
        super(0, 0, p_299146_.width(p_300579_), 9 * 3, p_300579_);
        this.font = p_299146_;
    }

    @Override
    protected void renderWidget(GuiGraphics p_300747_, int p_298491_, int p_299148_, float p_300011_)
    {
        int i = this.getX() + this.getWidth() / 2;
        int j = this.getY() + this.getHeight() / 2;
        Component component = this.getMessage();
        p_300747_.drawString(this.font, component, i - this.font.width(component) / 2, j - 9, -1, false);
        String s = LoadingDotsText.get(Util.getMillis());
        p_300747_.drawString(this.font, s, i - this.font.width(s) / 2, j + 9, -8355712, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_300971_)
    {
    }

    @Override
    public void playDownSound(SoundManager p_310640_)
    {
    }

    @Override
    public boolean isActive()
    {
        return false;
    }

    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent p_312633_)
    {
        return null;
    }
}
