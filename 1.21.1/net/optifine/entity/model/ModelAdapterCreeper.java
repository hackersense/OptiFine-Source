package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;

public class ModelAdapterCreeper extends ModelAdapter
{
    public ModelAdapterCreeper()
    {
        super(EntityType.CREEPER, "creeper", 0.5F);
    }

    public ModelAdapterCreeper(EntityType entityType, String name, float shadowSize)
    {
        super(entityType, name, shadowSize);
    }

    @Override
    public Model makeModel()
    {
        return new CreeperModel(bakeModelLayer(ModelLayers.CREEPER));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart)
    {
        if (!(model instanceof CreeperModel creepermodel))
        {
            return null;
        }
        else if (modelPart.equals("head"))
        {
            return creepermodel.root().getChildModelDeep("head");
        }
        else if (modelPart.equals("body"))
        {
            return creepermodel.root().getChildModelDeep("body");
        }
        else if (modelPart.equals("leg1"))
        {
            return creepermodel.root().getChildModelDeep("right_hind_leg");
        }
        else if (modelPart.equals("leg2"))
        {
            return creepermodel.root().getChildModelDeep("left_hind_leg");
        }
        else if (modelPart.equals("leg3"))
        {
            return creepermodel.root().getChildModelDeep("right_front_leg");
        }
        else if (modelPart.equals("leg4"))
        {
            return creepermodel.root().getChildModelDeep("left_front_leg");
        }
        else
        {
            return modelPart.equals("root") ? creepermodel.root() : null;
        }
    }

    @Override
    public String[] getModelRendererNames()
    {
        return new String[] {"head", "body", "leg1", "leg2", "leg3", "leg4", "root"};
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        CreeperRenderer creeperrenderer = new CreeperRenderer(entityrenderdispatcher.getContext());
        creeperrenderer.model = (CreeperModel<Creeper>)modelBase;
        creeperrenderer.shadowRadius = shadowSize;
        return creeperrenderer;
    }
}
