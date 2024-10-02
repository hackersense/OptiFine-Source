package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.optifine.Config;

public class ModelAdapterSlimeOuter extends ModelAdapter
{
    public ModelAdapterSlimeOuter()
    {
        super(EntityType.SLIME, "slime_outer", 0.25F);
    }

    @Override
    public Model makeModel()
    {
        return new SlimeModel(bakeModelLayer(ModelLayers.SLIME_OUTER));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart)
    {
        if (!(model instanceof SlimeModel slimemodel))
        {
            return null;
        }
        else
        {
            return modelPart.equals("body") ? slimemodel.root().getChildModelDeep("cube") : null;
        }
    }

    @Override
    public String[] getModelRendererNames()
    {
        return new String[] {"body"};
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        SlimeRenderer slimerenderer = new SlimeRenderer(entityrenderdispatcher.getContext());
        slimerenderer.model = new SlimeModel<>(bakeModelLayer(ModelLayers.SLIME_OUTER));
        slimerenderer.shadowRadius = 0.25F;
        EntityRenderer entityrenderer = rendererCache.get(EntityType.SLIME, index, () -> slimerenderer);

        if (!(entityrenderer instanceof SlimeRenderer slimerenderer1))
        {
            Config.warn("Not a SlimeRenderer: " + entityrenderer);
            return null;
        }
        else
        {
            SlimeOuterLayer slimeouterlayer = new SlimeOuterLayer<>(slimerenderer1, entityrenderdispatcher.getContext().getModelSet());
            slimeouterlayer.model = (SlimeModel)modelBase;
            slimerenderer1.removeLayers(SlimeOuterLayer.class);
            slimerenderer1.addLayer(slimeouterlayer);
            return slimerenderer1;
        }
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, ResourceLocation textureLocation)
    {
        SlimeRenderer slimerenderer = (SlimeRenderer)er;

        for (SlimeOuterLayer slimeouterlayer : slimerenderer.getLayers(SlimeOuterLayer.class))
        {
            slimeouterlayer.customTextureLocation = textureLocation;
        }

        return true;
    }
}
