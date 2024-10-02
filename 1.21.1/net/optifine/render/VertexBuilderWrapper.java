package net.optifine.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Vector3f;

public abstract class VertexBuilderWrapper implements VertexConsumer
{
    private VertexConsumer vertexBuilder;

    public VertexBuilderWrapper(VertexConsumer vertexBuilder)
    {
        this.vertexBuilder = vertexBuilder;
    }

    public VertexConsumer getVertexBuilder()
    {
        return this.vertexBuilder;
    }

    @Override
    public void putSprite(TextureAtlasSprite sprite)
    {
        this.vertexBuilder.putSprite(sprite);
    }

    @Override
    public void setSprite(TextureAtlasSprite sprite)
    {
        this.vertexBuilder.setSprite(sprite);
    }

    @Override
    public boolean isMultiTexture()
    {
        return this.vertexBuilder.isMultiTexture();
    }

    @Override
    public RenderType getRenderType()
    {
        return this.vertexBuilder.getRenderType();
    }

    @Override
    public Vector3f getTempVec3f()
    {
        return this.vertexBuilder.getTempVec3f();
    }

    @Override
    public float[] getTempFloat4(float f1, float f2, float f3, float f4)
    {
        return this.vertexBuilder.getTempFloat4(f1, f2, f3, f4);
    }

    @Override
    public int[] getTempInt4(int i1, int i2, int i3, int i4)
    {
        return this.vertexBuilder.getTempInt4(i1, i2, i3, i4);
    }

    @Override
    public MultiBufferSource.BufferSource getRenderTypeBuffer()
    {
        return this.vertexBuilder.getRenderTypeBuffer();
    }

    @Override
    public void setQuadVertexPositions(VertexPosition[] vps)
    {
        this.vertexBuilder.setQuadVertexPositions(vps);
    }

    @Override
    public void setMidBlock(float mbx, float mby, float mbz)
    {
        this.vertexBuilder.setMidBlock(mbx, mby, mbz);
    }

    @Override
    public Vector3f getMidBlock()
    {
        return this.vertexBuilder.getMidBlock();
    }

    @Override
    public int getVertexCount()
    {
        return this.vertexBuilder.getVertexCount();
    }
}
