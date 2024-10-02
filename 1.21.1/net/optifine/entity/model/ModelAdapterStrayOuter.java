package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.StrayRenderer;
import net.minecraft.client.renderer.entity.layers.SkeletonClothingLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.optifine.Config;

public class ModelAdapterStrayOuter extends ModelAdapterStray
{
    public ModelAdapterStrayOuter()
    {
        super(EntityType.STRAY, "stray_outer", 0.7F);
    }

    @Override
    public Model makeModel()
    {
        return new SkeletonModel(bakeModelLayer(ModelLayers.STRAY_OUTER_LAYER));
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        StrayRenderer strayrenderer = new StrayRenderer(entityrenderdispatcher.getContext());
        strayrenderer.model = new SkeletonModel(bakeModelLayer(ModelLayers.STRAY_OUTER_LAYER));
        strayrenderer.shadowRadius = 0.7F;
        EntityRenderer entityrenderer = rendererCache.get(EntityType.STRAY, index, () -> strayrenderer);

        if (!(entityrenderer instanceof StrayRenderer strayrenderer1))
        {
            Config.warn("Not a SkeletonModelRenderer: " + entityrenderer);
            return null;
        }
        else
        {
            ResourceLocation resourcelocation = new ResourceLocation("textures/entity/skeleton/stray_overlay.png");
            SkeletonClothingLayer skeletonclothinglayer = new SkeletonClothingLayer<>(
                strayrenderer1, entityrenderdispatcher.getContext().getModelSet(), ModelLayers.STRAY_OUTER_LAYER, resourcelocation
            );
            skeletonclothinglayer.layerModel = (SkeletonModel) modelBase;
            strayrenderer1.removeLayers(SkeletonClothingLayer.class);
            strayrenderer1.addLayer(skeletonclothinglayer);
            return strayrenderer1;
        }
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, ResourceLocation textureLocation)
    {
        StrayRenderer strayrenderer = (StrayRenderer)er;

        for (SkeletonClothingLayer skeletonclothinglayer : strayrenderer.getLayers(SkeletonClothingLayer.class))
        {
            skeletonclothinglayer.clothesLocation = textureLocation;
        }

        return true;
    }
}
