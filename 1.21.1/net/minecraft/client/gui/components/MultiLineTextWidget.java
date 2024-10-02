package net.minecraft.client.gui.components;

import java.util.OptionalInt;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.SingleKeyCache;

public class MultiLineTextWidget extends AbstractStringWidget
{
    private OptionalInt maxWidth = OptionalInt.empty();
    private OptionalInt maxRows = OptionalInt.empty();
    private final SingleKeyCache<MultiLineTextWidget.CacheKey, MultiLineLabel> cache;
    private boolean centered = false;

    public MultiLineTextWidget(Component p_270532_, Font p_270639_)
    {
        this(0, 0, p_270532_, p_270639_);
    }

    public MultiLineTextWidget(int p_270325_, int p_270355_, Component p_270069_, Font p_270673_)
    {
        super(p_270325_, p_270355_, 0, 0, p_270069_, p_270673_);
        this.cache = Util.singleKeyCache(
                             p_340776_ -> p_340776_.maxRows.isPresent()
                             ? MultiLineLabel.create(p_270673_, p_340776_.maxWidth, p_340776_.maxRows.getAsInt(), p_340776_.message)
                             : MultiLineLabel.create(p_270673_, p_340776_.message, p_340776_.maxWidth)
                         );
        this.active = false;
    }

    public MultiLineTextWidget setColor(int p_270378_)
    {
        super.setColor(p_270378_);
        return this;
    }

    public MultiLineTextWidget setMaxWidth(int p_270776_)
    {
        this.maxWidth = OptionalInt.of(p_270776_);
        return this;
    }

    public MultiLineTextWidget setMaxRows(int p_270085_)
    {
        this.maxRows = OptionalInt.of(p_270085_);
        return this;
    }

    public MultiLineTextWidget setCentered(boolean p_270493_)
    {
        this.centered = p_270493_;
        return this;
    }

    @Override
    public int getWidth()
    {
        return this.cache.getValue(this.getFreshCacheKey()).getWidth();
    }

    @Override
    public int getHeight()
    {
        return this.cache.getValue(this.getFreshCacheKey()).getLineCount() * 9;
    }

    @Override
    public void renderWidget(GuiGraphics p_282535_, int p_261774_, int p_261640_, float p_261514_)
    {
        MultiLineLabel multilinelabel = this.cache.getValue(this.getFreshCacheKey());
        int i = this.getX();
        int j = this.getY();
        int k = 9;
        int l = this.getColor();

        if (this.centered)
        {
            multilinelabel.renderCentered(p_282535_, i + this.getWidth() / 2, j, k, l);
        }
        else
        {
            multilinelabel.renderLeftAligned(p_282535_, i, j, k, l);
        }
    }

    private MultiLineTextWidget.CacheKey getFreshCacheKey()
    {
        return new MultiLineTextWidget.CacheKey(this.getMessage(), this.maxWidth.orElse(Integer.MAX_VALUE), this.maxRows);
    }

    static record CacheKey(Component message, int maxWidth, OptionalInt maxRows)
    {
    }
}
