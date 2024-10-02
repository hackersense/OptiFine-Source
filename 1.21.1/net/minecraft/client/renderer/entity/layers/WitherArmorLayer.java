package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.wither.WitherBoss;

public class WitherArmorLayer extends EnergySwirlLayer<WitherBoss, WitherBossModel<WitherBoss>>
{
    private static final ResourceLocation WITHER_ARMOR_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/wither/wither_armor.png");
    public WitherBossModel<WitherBoss> model;
    public ResourceLocation customTextureLocation;

    public WitherArmorLayer(RenderLayerParent<WitherBoss, WitherBossModel<WitherBoss>> p_174554_, EntityModelSet p_174555_)
    {
        super(p_174554_);
        this.model = new WitherBossModel<>(p_174555_.bakeLayer(ModelLayers.WITHER_ARMOR));
    }

    @Override
    protected float xOffset(float p_117702_)
    {
        return Mth.cos(p_117702_ * 0.02F) * 3.0F;
    }

    @Override
    protected ResourceLocation getTextureLocation()
    {
        return this.customTextureLocation != null ? this.customTextureLocation : WITHER_ARMOR_LOCATION;
    }

    @Override
    protected EntityModel<WitherBoss> model()
    {
        return this.model;
    }
}
