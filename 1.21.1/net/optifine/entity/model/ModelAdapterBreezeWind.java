package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BreezeModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.BreezeRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.layers.BreezeWindLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.optifine.Config;

public class ModelAdapterBreezeWind extends ModelAdapterBreeze
{
    public ModelAdapterBreezeWind()
    {
        super(EntityType.BREEZE, "breeze_wind", 0.0F);
    }

    @Override
    public Model makeModel()
    {
        return new BreezeModel(BreezeModel.createBodyLayer(128, 128).bakeRoot());
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        BreezeRenderer breezerenderer = new BreezeRenderer(entityrenderdispatcher.getContext());
        breezerenderer.model = new BreezeModel<>(bakeModelLayer(ModelLayers.BREEZE));
        breezerenderer.shadowRadius = 0.0F;
        EntityRenderer entityrenderer = rendererCache.get(EntityType.BREEZE, index, () -> breezerenderer);

        if (!(entityrenderer instanceof BreezeRenderer breezerenderer1))
        {
            Config.warn("Not a RenderBreeze: " + entityrenderer);
            return null;
        }
        else
        {
            ResourceLocation resourcelocation = modelBase.locationTextureCustom != null
                                                ? modelBase.locationTextureCustom
                                                : new ResourceLocation("textures/entity/breeze/breeze_wind.png");
            BreezeWindLayer breezewindlayer = new BreezeWindLayer(entityrenderdispatcher.getContext(), breezerenderer1);
            breezewindlayer.setModel((BreezeModel<Breeze>)modelBase);
            breezewindlayer.setTextureLocation(resourcelocation);
            breezerenderer1.removeLayers(BreezeWindLayer.class);
            breezerenderer1.addLayer(breezewindlayer);
            return breezerenderer1;
        }
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, ResourceLocation textureLocation)
    {
        return true;
    }
}
