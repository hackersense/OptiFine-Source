package net.minecraft.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Checkbox extends AbstractButton
{
    private static final ResourceLocation CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/checkbox_selected_highlighted");
    private static final ResourceLocation CHECKBOX_SELECTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/checkbox_selected");
    private static final ResourceLocation CHECKBOX_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/checkbox_highlighted");
    private static final ResourceLocation CHECKBOX_SPRITE = ResourceLocation.withDefaultNamespace("widget/checkbox");
    private static final int TEXT_COLOR = 14737632;
    private static final int SPACING = 4;
    private static final int BOX_PADDING = 8;
    private boolean selected;
    private final Checkbox.OnValueChange onValueChange;
    private final MultiLineTextWidget textWidget;

    Checkbox(int p_93826_, int p_93827_, int p_342287_, Component p_93830_, Font p_312622_, boolean p_93831_, Checkbox.OnValueChange p_309427_)
    {
        super(p_93826_, p_93827_, 0, 0, p_93830_);
        this.width = this.getAdjustedWidth(p_342287_, p_93830_, p_312622_);
        this.textWidget = new MultiLineTextWidget(p_93830_, p_312622_).setMaxWidth(this.width).setColor(14737632);
        this.height = this.getAdjustedHeight(p_312622_);
        this.selected = p_93831_;
        this.onValueChange = p_309427_;
    }

    private int getAdjustedWidth(int p_342252_, Component p_343945_, Font p_344442_)
    {
        return Math.min(getDefaultWidth(p_343945_, p_344442_), p_342252_);
    }

    private int getAdjustedHeight(Font p_344662_)
    {
        return Math.max(getBoxSize(p_344662_), this.textWidget.getHeight());
    }

    static int getDefaultWidth(Component p_343910_, Font p_343985_)
    {
        return getBoxSize(p_343985_) + 4 + p_343985_.width(p_343910_);
    }

    public static Checkbox.Builder builder(Component p_309446_, Font p_309998_)
    {
        return new Checkbox.Builder(p_309446_, p_309998_);
    }

    public static int getBoxSize(Font p_310239_)
    {
        return 9 + 8;
    }

    @Override
    public void onPress()
    {
        this.selected = !this.selected;
        this.onValueChange.onValueChange(this, this.selected);
    }

    public boolean selected()
    {
        return this.selected;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_260253_)
    {
        p_260253_.add(NarratedElementType.TITLE, this.createNarrationMessage());

        if (this.active)
        {
            if (this.isFocused())
            {
                p_260253_.add(NarratedElementType.USAGE, Component.translatable("narration.checkbox.usage.focused"));
            }
            else
            {
                p_260253_.add(NarratedElementType.USAGE, Component.translatable("narration.checkbox.usage.hovered"));
            }
        }
    }

    @Override
    public void renderWidget(GuiGraphics p_283124_, int p_282925_, int p_282705_, float p_282612_)
    {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.enableDepthTest();
        Font font = minecraft.font;
        p_283124_.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        ResourceLocation resourcelocation;

        if (this.selected)
        {
            resourcelocation = this.isFocused() ? CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE : CHECKBOX_SELECTED_SPRITE;
        }
        else
        {
            resourcelocation = this.isFocused() ? CHECKBOX_HIGHLIGHTED_SPRITE : CHECKBOX_SPRITE;
        }

        int i = getBoxSize(font);
        p_283124_.blitSprite(resourcelocation, this.getX(), this.getY(), i, i);
        int j = this.getX() + i + 4;
        int k = this.getY() + i / 2 - this.textWidget.getHeight() / 2;
        this.textWidget.setPosition(j, k);
        this.textWidget.renderWidget(p_283124_, p_282925_, p_282705_, p_282612_);
    }

    public static class Builder
    {
        private final Component message;
        private final Font font;
        private int maxWidth;
        private int x = 0;
        private int y = 0;
        private Checkbox.OnValueChange onValueChange = Checkbox.OnValueChange.NOP;
        private boolean selected = false;
        @Nullable
        private OptionInstance<Boolean> option = null;
        @Nullable
        private Tooltip tooltip = null;

        Builder(Component p_312515_, Font p_311430_)
        {
            this.message = p_312515_;
            this.font = p_311430_;
            this.maxWidth = Checkbox.getDefaultWidth(p_312515_, p_311430_);
        }

        public Checkbox.Builder pos(int p_313014_, int p_311548_)
        {
            this.x = p_313014_;
            this.y = p_311548_;
            return this;
        }

        public Checkbox.Builder onValueChange(Checkbox.OnValueChange p_312502_)
        {
            this.onValueChange = p_312502_;
            return this;
        }

        public Checkbox.Builder selected(boolean p_310957_)
        {
            this.selected = p_310957_;
            this.option = null;
            return this;
        }

        public Checkbox.Builder selected(OptionInstance<Boolean> p_310610_)
        {
            this.option = p_310610_;
            this.selected = p_310610_.get();
            return this;
        }

        public Checkbox.Builder tooltip(Tooltip p_309712_)
        {
            this.tooltip = p_309712_;
            return this;
        }

        public Checkbox.Builder maxWidth(int p_343638_)
        {
            this.maxWidth = p_343638_;
            return this;
        }

        public Checkbox build()
        {
            Checkbox.OnValueChange checkbox$onvaluechange = this.option == null ? this.onValueChange : (p_311135_, p_313032_) ->
            {
                this.option.set(p_313032_);
                this.onValueChange.onValueChange(p_311135_, p_313032_);
            };
            Checkbox checkbox = new Checkbox(
                this.x, this.y, this.maxWidth, this.message, this.font, this.selected, checkbox$onvaluechange
            );
            checkbox.setTooltip(this.tooltip);
            return checkbox;
        }
    }

    public interface OnValueChange
    {
        Checkbox.OnValueChange NOP = (p_310417_, p_311975_) ->
        {
        };

        void onValueChange(Checkbox p_309925_, boolean p_310656_);
    }
}
