package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.BreezeModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.BreezeRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.breeze.Breeze;

public class BreezeEyesLayer extends RenderLayer<Breeze, BreezeModel<Breeze>>
{
    private RenderType BREEZE_EYES = RenderType.breezeEyes(ResourceLocation.withDefaultNamespace("textures/entity/breeze/breeze_eyes.png"));
    private BreezeModel<Breeze> customModel;

    public BreezeEyesLayer(RenderLayerParent<Breeze, BreezeModel<Breeze>> p_310165_)
    {
        super(p_310165_);
    }

    public void render(
        PoseStack p_312911_,
        MultiBufferSource p_312666_,
        int p_311532_,
        Breeze p_311391_,
        float p_311193_,
        float p_309423_,
        float p_310215_,
        float p_311406_,
        float p_311840_,
        float p_312197_
    )
    {
        VertexConsumer vertexconsumer = p_312666_.getBuffer(this.BREEZE_EYES);
        BreezeModel<Breeze> breezemodel = this.getEntityModel();
        BreezeRenderer.enable(breezemodel, breezemodel.head(), breezemodel.eyes())
        .renderToBuffer(p_312911_, vertexconsumer, p_311532_, OverlayTexture.NO_OVERLAY);
    }

    public void setModel(BreezeModel<Breeze> model)
    {
        this.customModel = model;
    }

    public void setTextureLocation(ResourceLocation textureLocation)
    {
        this.BREEZE_EYES = RenderType.breezeEyes(textureLocation);
    }

    public BreezeModel<Breeze> getEntityModel()
    {
        return this.customModel != null ? this.customModel : (BreezeModel)super.getParentModel();
    }
}
