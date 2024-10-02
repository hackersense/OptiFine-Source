package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.SnifferModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.sniffer.Sniffer;

public class SnifferRenderer extends MobRenderer<Sniffer, SnifferModel<Sniffer>>
{
    private static final ResourceLocation SNIFFER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/sniffer/sniffer.png");

    public SnifferRenderer(EntityRendererProvider.Context p_272933_)
    {
        super(p_272933_, new SnifferModel<>(p_272933_.bakeLayer(ModelLayers.SNIFFER)), 1.1F);
    }

    public ResourceLocation getTextureLocation(Sniffer p_273552_)
    {
        return SNIFFER_LOCATION;
    }
}
