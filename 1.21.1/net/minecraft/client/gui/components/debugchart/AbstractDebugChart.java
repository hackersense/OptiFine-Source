package net.minecraft.client.gui.components.debugchart;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.debugchart.SampleStorage;

public abstract class AbstractDebugChart
{
    protected static final int COLOR_GREY = 14737632;
    protected static final int CHART_HEIGHT = 60;
    protected static final int LINE_WIDTH = 1;
    protected final Font font;
    protected final SampleStorage sampleStorage;

    protected AbstractDebugChart(Font p_297994_, SampleStorage p_333599_)
    {
        this.font = p_297994_;
        this.sampleStorage = p_333599_;
    }

    public int getWidth(int p_300792_)
    {
        return Math.min(this.sampleStorage.capacity() + 2, p_300792_);
    }

    public void drawChart(GuiGraphics p_300681_, int p_298472_, int p_298870_)
    {
        if (this instanceof TpsDebugChart)
        {
            Minecraft minecraft = Minecraft.getInstance();
            int i = (int)(512.0 / minecraft.getWindow().getGuiScale());
            p_298472_ = Math.max(p_298472_, i);
            p_298870_ = minecraft.getWindow().getGuiScaledWidth() - p_298472_;
        }

        int j2 = p_300681_.guiHeight();
        p_300681_.fill(RenderType.guiOverlay(), p_298472_, j2 - 60, p_298472_ + p_298870_, j2, -1873784752);
        long k2 = 0L;
        long j = 2147483647L;
        long k = -2147483648L;
        int l = Math.max(0, this.sampleStorage.capacity() - (p_298870_ - 2));
        int i1 = this.sampleStorage.size() - l;

        for (int j1 = 0; j1 < i1; j1++)
        {
            int k1 = p_298472_ + j1 + 1;
            int l1 = l + j1;
            long i2 = this.getValueForAggregation(l1);
            j = Math.min(j, i2);
            k = Math.max(k, i2);
            k2 += i2;
            this.drawDimensions(p_300681_, j2, k1, l1);
        }

        p_300681_.hLine(RenderType.guiOverlay(), p_298472_, p_298472_ + p_298870_ - 1, j2 - 60, -1);
        p_300681_.hLine(RenderType.guiOverlay(), p_298472_, p_298472_ + p_298870_ - 1, j2 - 1, -1);
        p_300681_.vLine(RenderType.guiOverlay(), p_298472_, j2 - 60, j2, -1);
        p_300681_.vLine(RenderType.guiOverlay(), p_298472_ + p_298870_ - 1, j2 - 60, j2, -1);

        if (i1 > 0)
        {
            String s = this.toDisplayString((double)j) + " min";
            String s1 = this.toDisplayString((double)k2 / (double)i1) + " avg";
            String s2 = this.toDisplayString((double)k) + " max";
            p_300681_.drawString(this.font, s, p_298472_ + 2, j2 - 60 - 9, 14737632);
            p_300681_.drawCenteredString(this.font, s1, p_298472_ + p_298870_ / 2, j2 - 60 - 9, 14737632);
            p_300681_.drawString(this.font, s2, p_298472_ + p_298870_ - this.font.width(s2) - 2, j2 - 60 - 9, 14737632);
        }

        this.renderAdditionalLinesAndLabels(p_300681_, p_298472_, p_298870_, j2);
    }

    protected void drawDimensions(GuiGraphics p_332509_, int p_335817_, int p_329430_, int p_328589_)
    {
        this.drawMainDimension(p_332509_, p_335817_, p_329430_, p_328589_);
        this.drawAdditionalDimensions(p_332509_, p_335817_, p_329430_, p_328589_);
    }

    protected void drawMainDimension(GuiGraphics p_336289_, int p_328284_, int p_335372_, int p_331181_)
    {
        long i = this.sampleStorage.get(p_331181_);
        int j = this.getSampleHeight((double)i);
        int k = this.getSampleColor(i);
        p_336289_.fill(RenderType.guiOverlay(), p_335372_, p_328284_ - j, p_335372_ + 1, p_328284_, k);
    }

    protected void drawAdditionalDimensions(GuiGraphics p_332338_, int p_333190_, int p_332312_, int p_328542_)
    {
    }

    protected long getValueForAggregation(int p_335854_)
    {
        return this.sampleStorage.get(p_335854_);
    }

    protected void renderAdditionalLinesAndLabels(GuiGraphics p_300007_, int p_299062_, int p_300355_, int p_297248_)
    {
    }

    protected void drawStringWithShade(GuiGraphics p_300760_, String p_299957_, int p_301259_, int p_298524_)
    {
        p_300760_.fill(RenderType.guiOverlay(), p_301259_, p_298524_, p_301259_ + this.font.width(p_299957_) + 1, p_298524_ + 9, -1873784752);
        p_300760_.drawString(this.font, p_299957_, p_301259_ + 1, p_298524_ + 1, 14737632, false);
    }

    protected abstract String toDisplayString(double p_299846_);

    protected abstract int getSampleHeight(double p_298917_);

    protected abstract int getSampleColor(long p_301058_);

    protected int getSampleColor(double p_300651_, double p_300082_, int p_298618_, double p_299706_, int p_300095_, double p_298068_, int p_299403_)
    {
        p_300651_ = Mth.clamp(p_300651_, p_300082_, p_298068_);
        return p_300651_ < p_299706_
               ? FastColor.ARGB32.lerp((float)((p_300651_ - p_300082_) / (p_299706_ - p_300082_)), p_298618_, p_300095_)
               : FastColor.ARGB32.lerp((float)((p_300651_ - p_299706_) / (p_298068_ - p_299706_)), p_300095_, p_299403_);
    }
}
