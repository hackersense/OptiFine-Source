package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PolarBearModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.PolarBear;

public class PolarBearRenderer extends MobRenderer<PolarBear, PolarBearModel<PolarBear>>
{
    private static final ResourceLocation BEAR_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/bear/polarbear.png");

    public PolarBearRenderer(EntityRendererProvider.Context p_174356_)
    {
        super(p_174356_, new PolarBearModel<>(p_174356_.bakeLayer(ModelLayers.POLAR_BEAR)), 0.9F);
    }

    public ResourceLocation getTextureLocation(PolarBear p_115732_)
    {
        return BEAR_LOCATION;
    }

    protected void scale(PolarBear p_115734_, PoseStack p_115735_, float p_115736_)
    {
        p_115735_.scale(1.2F, 1.2F, 1.2F);
        super.scale(p_115734_, p_115735_, p_115736_);
    }
}
