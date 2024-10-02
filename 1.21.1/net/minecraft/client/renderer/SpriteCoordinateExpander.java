package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.optifine.SmartAnimations;
import net.optifine.render.VertexBuilderWrapper;

public class SpriteCoordinateExpander extends VertexBuilderWrapper implements VertexConsumer
{
    private final VertexConsumer delegate;
    private final TextureAtlasSprite sprite;

    public SpriteCoordinateExpander(VertexConsumer p_110798_, TextureAtlasSprite p_110799_)
    {
        super(p_110798_);

        if (SmartAnimations.isActive())
        {
            SmartAnimations.spriteRendered(p_110799_);
        }

        this.delegate = p_110798_;
        this.sprite = p_110799_;
    }

    @Override
    public VertexConsumer addVertex(float p_342932_, float p_342886_, float p_342696_)
    {
        this.delegate.addVertex(p_342932_, p_342886_, p_342696_);
        return this;
    }

    @Override
    public VertexConsumer setColor(int p_344589_, int p_342555_, int p_344320_, int p_345258_)
    {
        this.delegate.setColor(p_344589_, p_342555_, p_344320_, p_345258_);
        return this;
    }

    @Override
    public VertexConsumer setUv(float p_343856_, float p_344420_)
    {
        this.delegate.setUv(this.sprite.getU(p_343856_), this.sprite.getV(p_344420_));
        return this;
    }

    @Override
    public VertexConsumer setUv1(int p_343784_, int p_344827_)
    {
        this.delegate.setUv1(p_343784_, p_344827_);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int p_345257_, int p_344124_)
    {
        this.delegate.setUv2(p_345257_, p_344124_);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float p_342779_, float p_342534_, float p_344783_)
    {
        this.delegate.setNormal(p_342779_, p_342534_, p_344783_);
        return this;
    }

    @Override
    public void addVertex(
        float p_342812_,
        float p_344058_,
        float p_343304_,
        int p_343913_,
        float p_344339_,
        float p_343349_,
        int p_344262_,
        int p_345265_,
        float p_344296_,
        float p_345357_,
        float p_343817_
    )
    {
        if (this.delegate.isMultiTexture())
        {
            this.delegate.putSprite(this.sprite);
            this.delegate.addVertex(p_342812_, p_344058_, p_343304_, p_343913_, p_344339_, p_343349_, p_344262_, p_345265_, p_344296_, p_345357_, p_343817_);
        }
        else
        {
            this.delegate
            .addVertex(
                p_342812_,
                p_344058_,
                p_343304_,
                p_343913_,
                this.sprite.getU(p_344339_),
                this.sprite.getV(p_343349_),
                p_344262_,
                p_345265_,
                p_344296_,
                p_345357_,
                p_343817_
            );
        }
    }

    @Override
    public boolean canAddVertexFast()
    {
        return this.delegate.canAddVertexFast();
    }

    @Override
    public void addVertexFast(float x, float y, float z, int color, float texU, float texV, int overlayUV, int lightmapUV, int normals)
    {
        if (this.delegate.isMultiTexture())
        {
            this.delegate.putSprite(this.sprite);
            this.delegate.addVertexFast(x, y, z, color, texU, texV, overlayUV, lightmapUV, normals);
        }
        else
        {
            float f = this.sprite.getU(texU);
            float f1 = this.sprite.getV(texV);
            this.delegate.addVertexFast(x, y, z, color, f, f1, overlayUV, lightmapUV, normals);
        }
    }
}
