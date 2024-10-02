package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HoglinModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ZoglinRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zoglin;

public class ModelAdapterZoglin extends ModelAdapterHoglin
{
    public ModelAdapterZoglin()
    {
        super(EntityType.ZOGLIN, "zoglin", 0.7F);
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        ZoglinRenderer zoglinrenderer = new ZoglinRenderer(entityrenderdispatcher.getContext());
        zoglinrenderer.model = (HoglinModel<Zoglin>)modelBase;
        zoglinrenderer.shadowRadius = shadowSize;
        return zoglinrenderer;
    }
}
