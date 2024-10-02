package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ColorableHierarchicalModel;
import net.minecraft.client.model.TropicalFishModelA;
import net.minecraft.client.model.TropicalFishModelB;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.TropicalFishPatternLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.TropicalFish;

public class TropicalFishRenderer extends MobRenderer<TropicalFish, ColorableHierarchicalModel<TropicalFish>>
{
    private final ColorableHierarchicalModel<TropicalFish> modelA = this.getModel();
    private final ColorableHierarchicalModel<TropicalFish> modelB;
    private static final ResourceLocation MODEL_A_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fish/tropical_a.png");
    private static final ResourceLocation MODEL_B_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fish/tropical_b.png");

    public TropicalFishRenderer(EntityRendererProvider.Context p_174428_)
    {
        super(p_174428_, new TropicalFishModelA<>(p_174428_.bakeLayer(ModelLayers.TROPICAL_FISH_SMALL)), 0.15F);
        this.modelB = new TropicalFishModelB<>(p_174428_.bakeLayer(ModelLayers.TROPICAL_FISH_LARGE));
        this.addLayer(new TropicalFishPatternLayer(this, p_174428_.getModelSet()));
    }

    public ResourceLocation getTextureLocation(TropicalFish p_116217_)
    {

        return switch (p_116217_.getVariant().base())
        {
            case SMALL -> MODEL_A_TEXTURE;

            case LARGE -> MODEL_B_TEXTURE;
        };
    }

    public void render(TropicalFish p_116219_, float p_116220_, float p_116221_, PoseStack p_116222_, MultiBufferSource p_116223_, int p_116224_)
    {

        ColorableHierarchicalModel<TropicalFish> colorablehierarchicalmodel = switch (p_116219_.getVariant().base())
        {
            case SMALL -> this.modelA;

            case LARGE -> this.modelB;
        };

        this.model = colorablehierarchicalmodel;

        colorablehierarchicalmodel.setColor(p_116219_.getBaseColor().getTextureDiffuseColor());

        super.render(p_116219_, p_116220_, p_116221_, p_116222_, p_116223_, p_116224_);

        colorablehierarchicalmodel.setColor(-1);
    }

    protected void setupRotations(TropicalFish p_331912_, PoseStack p_116205_, float p_116206_, float p_116207_, float p_116208_, float p_334850_)
    {
        super.setupRotations(p_331912_, p_116205_, p_116206_, p_116207_, p_116208_, p_334850_);
        float f = 4.3F * Mth.sin(0.6F * p_116206_);
        p_116205_.mulPose(Axis.YP.rotationDegrees(f));

        if (!p_331912_.isInWater())
        {
            p_116205_.translate(0.2F, 0.1F, 0.0F);
            p_116205_.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }
}
