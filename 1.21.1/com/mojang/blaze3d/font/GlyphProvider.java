package com.mojang.blaze3d.font;

import it.unimi.dsi.fastutil.ints.IntSet;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.FontOption;

public interface GlyphProvider extends AutoCloseable
{
    float BASELINE = 7.0F;

    @Override

default void close()
    {
    }

    @Nullable

default GlyphInfo getGlyph(int p_231091_)
    {
        return null;
    }

    IntSet getSupportedGlyphs();

    public static record Conditional(GlyphProvider provider, FontOption.Filter filter) implements AutoCloseable
    {
        @Override
        public void close()
        {
            this.provider.close();
        }
    }
}
