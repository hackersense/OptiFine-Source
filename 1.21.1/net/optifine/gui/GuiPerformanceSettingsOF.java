package net.optifine.gui;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.optifine.config.Option;

public class GuiPerformanceSettingsOF extends GuiScreenOF
{
    private Screen prevScreen;
    private Options settings;
    private TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderOptions());

    public GuiPerformanceSettingsOF(Screen guiscreen, Options gamesettings)
    {
        super(Component.literal(I18n.get("of.options.performanceTitle")));
        this.prevScreen = guiscreen;
        this.settings = gamesettings;
    }

    @Override
    public void init()
    {
        this.clearWidgets();
        OptionInstance[] aoptioninstance = new OptionInstance[]
        {
            Option.RENDER_REGIONS,
            Option.FAST_RENDER,
            Option.SMART_ANIMATIONS,
            Option.FAST_MATH,
            Option.SMOOTH_FPS,
            Option.SMOOTH_WORLD,
            Option.CHUNK_UPDATES,
            Option.CHUNK_UPDATES_DYNAMIC,
            Option.LAZY_CHUNK_LOADING,
            this.settings.PRIORITIZE_CHUNK_UPDATES
        };

        for (int i = 0; i < aoptioninstance.length; i++)
        {
            OptionInstance optioninstance = aoptioninstance[i];
            int j = this.width / 2 - 155 + i % 2 * 160;
            int k = this.height / 6 + 21 * (i / 2) - 12;
            AbstractWidget abstractwidget = this.addRenderableWidget(optioninstance.createButton(this.minecraft.options, j, k, 150));
            abstractwidget.setTooltip(null);
        }

        this.addRenderableWidget(new GuiButtonOF(200, this.width / 2 - 100, this.height / 6 + 168 + 11, I18n.get("gui.done")));
    }

    @Override
    protected void actionPerformed(AbstractWidget guiElement)
    {
        if (guiElement instanceof GuiButtonOF guibuttonof)
        {
            if (guibuttonof.active)
            {
                if (guibuttonof.id == 200)
                {
                    this.minecraft.options.save();
                    this.minecraft.setScreen(this.prevScreen);
                }
            }
        }
    }

    @Override
    public void removed()
    {
        this.minecraft.options.save();
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphicsIn, int x, int y, float partialTicks)
    {
        super.render(graphicsIn, x, y, partialTicks);
        drawCenteredString(graphicsIn, this.fontRenderer, this.title, this.width / 2, 15, 16777215);
        this.tooltipManager.drawTooltips(graphicsIn, x, y, this.getButtonList());
    }
}
