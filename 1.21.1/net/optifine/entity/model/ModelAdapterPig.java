package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;

public class ModelAdapterPig extends ModelAdapterQuadruped
{
    public ModelAdapterPig()
    {
        super(EntityType.PIG, "pig", 0.7F);
    }

    @Override
    public Model makeModel()
    {
        return new PigModel(bakeModelLayer(ModelLayers.PIG));
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        PigRenderer pigrenderer = new PigRenderer(entityrenderdispatcher.getContext());
        pigrenderer.model = (PigModel<Pig>)modelBase;
        pigrenderer.shadowRadius = shadowSize;
        return pigrenderer;
    }
}
