package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.MushroomCowMushroomLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.MushroomCow;

public class MushroomCowRenderer extends MobRenderer<MushroomCow, CowModel<MushroomCow>>
{
    private static final Map<MushroomCow.MushroomType, ResourceLocation> TEXTURES = Util.make(Maps.newHashMap(), p_340940_ ->
    {
        p_340940_.put(MushroomCow.MushroomType.BROWN, ResourceLocation.withDefaultNamespace("textures/entity/cow/brown_mooshroom.png"));
        p_340940_.put(MushroomCow.MushroomType.RED, ResourceLocation.withDefaultNamespace("textures/entity/cow/red_mooshroom.png"));
    });

    public MushroomCowRenderer(EntityRendererProvider.Context p_174324_)
    {
        super(p_174324_, new CowModel<>(p_174324_.bakeLayer(ModelLayers.MOOSHROOM)), 0.7F);
        this.addLayer(new MushroomCowMushroomLayer<>(this, p_174324_.getBlockRenderDispatcher()));
    }

    public ResourceLocation getTextureLocation(MushroomCow p_115514_)
    {
        return TEXTURES.get(p_115514_.getVariant());
    }
}
