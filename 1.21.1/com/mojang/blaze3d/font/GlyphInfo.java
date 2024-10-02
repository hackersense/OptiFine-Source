package com.mojang.blaze3d.font;

import java.util.function.Function;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;

public interface GlyphInfo
{
    float getAdvance();

default float getAdvance(boolean p_83828_)
    {
        return this.getAdvance() + (p_83828_ ? this.getBoldOffset() : 0.0F);
    }

default float getBoldOffset()
    {
        return 1.0F;
    }

default float getShadowOffset()
    {
        return 1.0F;
    }

    BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> p_231088_);

    public interface SpaceGlyphInfo extends GlyphInfo
    {
        @Override

    default BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> p_231090_)
        {
            return EmptyGlyph.INSTANCE;
        }
    }
}
