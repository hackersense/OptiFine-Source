package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PolarBearModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.PolarBearRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.PolarBear;

public class ModelAdapterPolarBear extends ModelAdapterQuadruped
{
    public ModelAdapterPolarBear()
    {
        super(EntityType.POLAR_BEAR, "polar_bear", 0.7F);
    }

    @Override
    public Model makeModel()
    {
        return new PolarBearModel(bakeModelLayer(ModelLayers.POLAR_BEAR));
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        PolarBearRenderer polarbearrenderer = new PolarBearRenderer(entityrenderdispatcher.getContext());
        polarbearrenderer.model = (PolarBearModel<PolarBear>)modelBase;
        polarbearrenderer.shadowRadius = shadowSize;
        return polarbearrenderer;
    }
}
