package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.optifine.reflect.Reflector;

public class ModelAdapterWolf extends ModelAdapter
{
    public ModelAdapterWolf()
    {
        super(EntityType.WOLF, "wolf", 0.5F);
    }

    protected ModelAdapterWolf(EntityType entityType, String name, float shadowSize)
    {
        super(entityType, name, shadowSize);
    }

    @Override
    public Model makeModel()
    {
        return new WolfModel(bakeModelLayer(ModelLayers.WOLF));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart)
    {
        if (!(model instanceof WolfModel wolfmodel))
        {
            return null;
        }
        else if (modelPart.equals("head"))
        {
            return (ModelPart)Reflector.ModelWolf_ModelRenderers.getValue(wolfmodel, 0);
        }
        else if (modelPart.equals("body"))
        {
            return (ModelPart)Reflector.ModelWolf_ModelRenderers.getValue(wolfmodel, 2);
        }
        else if (modelPart.equals("leg1"))
        {
            return (ModelPart)Reflector.ModelWolf_ModelRenderers.getValue(wolfmodel, 3);
        }
        else if (modelPart.equals("leg2"))
        {
            return (ModelPart)Reflector.ModelWolf_ModelRenderers.getValue(wolfmodel, 4);
        }
        else if (modelPart.equals("leg3"))
        {
            return (ModelPart)Reflector.ModelWolf_ModelRenderers.getValue(wolfmodel, 5);
        }
        else if (modelPart.equals("leg4"))
        {
            return (ModelPart)Reflector.ModelWolf_ModelRenderers.getValue(wolfmodel, 6);
        }
        else if (modelPart.equals("tail"))
        {
            return (ModelPart)Reflector.ModelWolf_ModelRenderers.getValue(wolfmodel, 7);
        }
        else
        {
            return modelPart.equals("mane") ? (ModelPart)Reflector.ModelWolf_ModelRenderers.getValue(wolfmodel, 9) : null;
        }
    }

    @Override
    public String[] getModelRendererNames()
    {
        return new String[] {"head", "body", "leg1", "leg2", "leg3", "leg4", "tail", "mane"};
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        WolfRenderer wolfrenderer = new WolfRenderer(entityrenderdispatcher.getContext());
        wolfrenderer.model = (WolfModel<Wolf>)modelBase;
        wolfrenderer.shadowRadius = shadowSize;
        return wolfrenderer;
    }
}
