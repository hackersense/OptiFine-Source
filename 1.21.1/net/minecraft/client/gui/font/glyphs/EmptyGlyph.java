package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class EmptyGlyph extends BakedGlyph
{
    public static final EmptyGlyph INSTANCE = new EmptyGlyph();

    public EmptyGlyph()
    {
        super(GlyphRenderTypes.createForColorTexture(ResourceLocation.withDefaultNamespace("")), 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    @Override
    public void render(
        boolean p_95278_,
        float p_95279_,
        float p_95280_,
        Matrix4f p_253794_,
        VertexConsumer p_95282_,
        float p_95283_,
        float p_95284_,
        float p_95285_,
        float p_95286_,
        int p_95287_
    )
    {
    }
}
