package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Drowned;

public class DrownedOuterLayer<T extends Drowned> extends RenderLayer<T, DrownedModel<T>>
{
    private static final ResourceLocation DROWNED_OUTER_LAYER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer.png");
    public DrownedModel<T> model;
    public ResourceLocation customTextureLocation;

    public DrownedOuterLayer(RenderLayerParent<T, DrownedModel<T>> p_174490_, EntityModelSet p_174491_)
    {
        super(p_174490_);
        this.model = new DrownedModel<>(p_174491_.bakeLayer(ModelLayers.DROWNED_OUTER_LAYER));
    }

    public void render(
        PoseStack p_116924_,
        MultiBufferSource p_116925_,
        int p_116926_,
        T p_116927_,
        float p_116928_,
        float p_116929_,
        float p_116930_,
        float p_116931_,
        float p_116932_,
        float p_116933_
    )
    {
        ResourceLocation resourcelocation = this.customTextureLocation != null ? this.customTextureLocation : DROWNED_OUTER_LAYER_LOCATION;
        coloredCutoutModelCopyLayerRender(
            this.getParentModel(),
            this.model,
            resourcelocation,
            p_116924_,
            p_116925_,
            p_116926_,
            p_116927_,
            p_116928_,
            p_116929_,
            p_116931_,
            p_116932_,
            p_116933_,
            p_116930_,
            -1
        );
    }
}
