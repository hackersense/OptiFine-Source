package net.optifine.gui;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.optifine.Lang;
import net.optifine.config.Option;

public class GuiQuickInfoOF extends GuiScreenOF
{
    private Screen prevScreen;
    private TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderOptions());

    public GuiQuickInfoOF(Screen guiscreen)
    {
        super(Component.translatable("of.options.quickInfoTitle"));
        this.prevScreen = guiscreen;
    }

    @Override
    public void init()
    {
        this.clearWidgets();
        OptionInstance[] aoptioninstance = new OptionInstance[]
        {
            Option.QUICK_INFO,
            Option.QUICK_INFO_FPS,
            Option.QUICK_INFO_CHUNKS,
            Option.QUICK_INFO_ENTITIES,
            Option.QUICK_INFO_PARTICLES,
            Option.QUICK_INFO_UPDATES,
            Option.QUICK_INFO_GPU,
            Option.QUICK_INFO_POS,
            Option.QUICK_INFO_BIOME,
            Option.QUICK_INFO_FACING,
            Option.QUICK_INFO_LIGHT,
            Option.QUICK_INFO_MEMORY,
            Option.QUICK_INFO_NATIVE_MEMORY,
            Option.QUICK_INFO_TARGET_BLOCK,
            Option.QUICK_INFO_TARGET_FLUID,
            Option.QUICK_INFO_TARGET_ENTITY,
            Option.QUICK_INFO_LABELS,
            Option.QUICK_INFO_BACKGROUND
        };

        for (int i = 0; i < aoptioninstance.length; i++)
        {
            OptionInstance optioninstance = aoptioninstance[i];

            if (optioninstance != null)
            {
                int j = this.width / 2 - 155 + i % 2 * 160;
                int k = this.height / 6 + 21 * (i / 2) - 12;
                AbstractWidget abstractwidget = this.addRenderableWidget(optioninstance.createButton(this.minecraft.options, j, k, 150));
                abstractwidget.setTooltip(null);
            }
        }

        this.addRenderableWidget(new GuiButtonOF(210, this.width / 2 - 155, this.height / 6 + 168 + 11, 70, 20, Lang.get("of.options.animation.allOn")));
        this.addRenderableWidget(new GuiButtonOF(211, this.width / 2 - 155 + 80, this.height / 6 + 168 + 11, 70, 20, Lang.get("of.options.animation.allOff")));
        this.addRenderableWidget(new GuiScreenButtonOF(200, this.width / 2 + 5, this.height / 6 + 168 + 11, I18n.get("gui.done")));
        this.updateSubOptions();
    }

    @Override
    protected void actionPerformed(AbstractWidget guiElement)
    {
        this.updateSubOptions();

        if (guiElement instanceof GuiButtonOF guibuttonof)
        {
            if (guibuttonof.active)
            {
                if (guibuttonof.id == 200)
                {
                    this.minecraft.options.save();
                    this.minecraft.setScreen(this.prevScreen);
                }

                if (guibuttonof.id == 210)
                {
                    this.minecraft.options.setAllQuickInfos(true);
                }

                if (guibuttonof.id == 211)
                {
                    this.minecraft.options.setAllQuickInfos(false);
                }

                this.minecraft.resizeDisplay();
            }
        }
    }

    private void updateSubOptions()
    {
        boolean flag = this.settings.ofQuickInfo;

        for (AbstractWidget abstractwidget : this.getButtonList())
        {
            if (abstractwidget instanceof IOptionControl)
            {
                IOptionControl ioptioncontrol = (IOptionControl)abstractwidget;

                if (ioptioncontrol.getControlOption() != Option.QUICK_INFO)
                {
                    abstractwidget.active = flag;
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
        drawCenteredString(graphicsIn, this.minecraft.font, this.title, this.width / 2, 15, 16777215);
        this.tooltipManager.drawTooltips(graphicsIn, x, y, this.getButtonList());
    }
}
