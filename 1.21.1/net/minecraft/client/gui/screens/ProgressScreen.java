package net.minecraft.client.gui.screens;

import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProgressListener;
import net.optifine.CustomLoadingScreen;
import net.optifine.CustomLoadingScreens;

public class ProgressScreen extends Screen implements ProgressListener
{
    @Nullable
    private Component header;
    @Nullable
    private Component stage;
    private int progress;
    private boolean stop;
    private final boolean clearScreenAfterStop;
    private CustomLoadingScreen customLoadingScreen;

    public ProgressScreen(boolean p_169364_)
    {
        super(GameNarrator.NO_TITLE);
        this.clearScreenAfterStop = p_169364_;
        this.customLoadingScreen = CustomLoadingScreens.getCustomLoadingScreen();
    }

    @Override
    public boolean shouldCloseOnEsc()
    {
        return false;
    }

    @Override
    protected boolean shouldNarrateNavigation()
    {
        return false;
    }

    @Override
    public void progressStartNoAbort(Component p_96520_)
    {
        this.progressStart(p_96520_);
    }

    @Override
    public void progressStart(Component p_96523_)
    {
        this.header = p_96523_;
        this.progressStage(Component.translatable("menu.working"));
    }

    @Override
    public void progressStage(Component p_96525_)
    {
        this.stage = p_96525_;
        this.progressStagePercentage(0);
    }

    @Override
    public void progressStagePercentage(int p_96513_)
    {
        this.progress = p_96513_;
    }

    @Override
    public void stop()
    {
        this.stop = true;
    }

    @Override
    public void render(GuiGraphics p_283582_, int p_96516_, int p_96517_, float p_96518_)
    {
        if (this.stop)
        {
            if (this.clearScreenAfterStop)
            {
                this.minecraft.setScreen(null);
            }
        }
        else
        {
            super.render(p_283582_, p_96516_, p_96517_, p_96518_);

            if (this.progress > 0)
            {
                if (this.header != null)
                {
                    p_283582_.drawCenteredString(this.font, this.header, this.width / 2, 70, 16777215);
                }

                if (this.stage != null && this.progress != 0)
                {
                    p_283582_.drawCenteredString(
                        this.font, Component.empty().append(this.stage).append(" " + this.progress + "%"), this.width / 2, 90, 16777215
                    );
                }
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphicsIn, int mouseX, int mouseY, float partialTicks)
    {
        if (this.customLoadingScreen != null && this.minecraft.level == null)
        {
            this.customLoadingScreen.drawBackground(this.width, this.height);
        }
        else
        {
            super.renderBackground(graphicsIn, mouseX, mouseY, partialTicks);
        }
    }
}
