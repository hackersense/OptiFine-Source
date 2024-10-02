package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;

public class ModelAdapterHusk extends ModelAdapterBiped
{
    public ModelAdapterHusk()
    {
        super(EntityType.HUSK, "husk", 0.5F);
    }

    @Override
    public Model makeModel()
    {
        return new ZombieModel(bakeModelLayer(ModelLayers.HUSK));
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        HuskRenderer huskrenderer = new HuskRenderer(entityrenderdispatcher.getContext());
        huskrenderer.model = (ZombieModel<Zombie>)modelBase;
        huskrenderer.shadowRadius = shadowSize;
        return huskrenderer;
    }
}
