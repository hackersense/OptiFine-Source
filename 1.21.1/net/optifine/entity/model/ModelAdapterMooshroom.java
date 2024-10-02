package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.MushroomCowRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.MushroomCow;

public class ModelAdapterMooshroom extends ModelAdapterQuadruped
{
    public ModelAdapterMooshroom()
    {
        super(EntityType.MOOSHROOM, "mooshroom", 0.7F);
    }

    @Override
    public Model makeModel()
    {
        return new CowModel(bakeModelLayer(ModelLayers.MOOSHROOM));
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        MushroomCowRenderer mushroomcowrenderer = new MushroomCowRenderer(entityrenderdispatcher.getContext());
        mushroomcowrenderer.model = (CowModel<MushroomCow>)modelBase;
        mushroomcowrenderer.shadowRadius = shadowSize;
        return mushroomcowrenderer;
    }
}
