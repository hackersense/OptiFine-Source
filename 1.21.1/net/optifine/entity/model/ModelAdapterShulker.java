package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ShulkerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.optifine.reflect.Reflector;

public class ModelAdapterShulker extends ModelAdapter
{
    public ModelAdapterShulker()
    {
        super(EntityType.SHULKER, "shulker", 0.0F);
    }

    @Override
    public Model makeModel()
    {
        return new ShulkerModel(bakeModelLayer(ModelLayers.SHULKER));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart)
    {
        if (!(model instanceof ShulkerModel shulkermodel))
        {
            return null;
        }
        else if (modelPart.equals("base"))
        {
            return (ModelPart)Reflector.ModelShulker_ModelRenderers.getValue(shulkermodel, 0);
        }
        else if (modelPart.equals("lid"))
        {
            return (ModelPart)Reflector.ModelShulker_ModelRenderers.getValue(shulkermodel, 1);
        }
        else
        {
            return modelPart.equals("head") ? (ModelPart)Reflector.ModelShulker_ModelRenderers.getValue(shulkermodel, 2) : null;
        }
    }

    @Override
    public String[] getModelRendererNames()
    {
        return new String[] {"base", "lid", "head"};
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        ShulkerRenderer shulkerrenderer = new ShulkerRenderer(entityrenderdispatcher.getContext());
        shulkerrenderer.model = (ShulkerModel<Shulker>)modelBase;
        shulkerrenderer.shadowRadius = shadowSize;
        return shulkerrenderer;
    }
}
