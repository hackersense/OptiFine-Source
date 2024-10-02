package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.WitherSkullRenderer;
import net.minecraft.world.entity.EntityType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterWitherSkull extends ModelAdapter
{
    public ModelAdapterWitherSkull()
    {
        super(EntityType.WITHER_SKULL, "wither_skull", 0.0F);
    }

    @Override
    public Model makeModel()
    {
        return new SkullModel(bakeModelLayer(ModelLayers.WITHER_SKULL));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart)
    {
        if (!(model instanceof SkullModel skullmodel))
        {
            return null;
        }
        else if (modelPart.equals("head"))
        {
            return (ModelPart)Reflector.ModelSkull_renderers.getValue(skullmodel, 1);
        }
        else
        {
            return modelPart.equals("root") ? (ModelPart)Reflector.ModelSkull_renderers.getValue(skullmodel, 0) : null;
        }
    }

    @Override
    public String[] getModelRendererNames()
    {
        return new String[] {"head", "root"};
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        WitherSkullRenderer witherskullrenderer = new WitherSkullRenderer(entityrenderdispatcher.getContext());

        if (!Reflector.RenderWitherSkull_model.exists())
        {
            Config.warn("Field not found: RenderWitherSkull_model");
            return null;
        }
        else
        {
            Reflector.setFieldValue(witherskullrenderer, Reflector.RenderWitherSkull_model, modelBase);
            witherskullrenderer.shadowRadius = shadowSize;
            return witherskullrenderer;
        }
    }
}
