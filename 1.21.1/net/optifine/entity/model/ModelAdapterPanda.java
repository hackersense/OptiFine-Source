package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PandaModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.PandaRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Panda;

public class ModelAdapterPanda extends ModelAdapterQuadruped
{
    public ModelAdapterPanda()
    {
        super(EntityType.PANDA, "panda", 0.9F);
    }

    @Override
    public Model makeModel()
    {
        return new PandaModel(bakeModelLayer(ModelLayers.PANDA));
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        PandaRenderer pandarenderer = new PandaRenderer(entityrenderdispatcher.getContext());
        pandarenderer.model = (PandaModel<Panda>)modelBase;
        pandarenderer.shadowRadius = shadowSize;
        return pandarenderer;
    }
}
