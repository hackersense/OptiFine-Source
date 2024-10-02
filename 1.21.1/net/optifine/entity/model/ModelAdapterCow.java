package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;

public class ModelAdapterCow extends ModelAdapterQuadruped
{
    public ModelAdapterCow()
    {
        super(EntityType.COW, "cow", 0.7F);
    }

    @Override
    public Model makeModel()
    {
        return new CowModel(bakeModelLayer(ModelLayers.COW));
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        CowRenderer cowrenderer = new CowRenderer(entityrenderdispatcher.getContext());
        cowrenderer.model = (CowModel<Cow>)modelBase;
        cowrenderer.shadowRadius = shadowSize;
        return cowrenderer;
    }
}
