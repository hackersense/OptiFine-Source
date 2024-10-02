package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;

public class ModelAdapterVillager extends ModelAdapter
{
    public ModelAdapterVillager()
    {
        super(EntityType.VILLAGER, "villager", 0.5F);
    }

    protected ModelAdapterVillager(EntityType type, String name, float shadowSize)
    {
        super(type, name, shadowSize);
    }

    @Override
    public Model makeModel()
    {
        return new VillagerModel(bakeModelLayer(ModelLayers.VILLAGER));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart)
    {
        if (!(model instanceof VillagerModel villagermodel))
        {
            return null;
        }
        else if (modelPart.equals("head"))
        {
            return villagermodel.root().getChildModelDeep("head");
        }
        else if (modelPart.equals("headwear"))
        {
            return villagermodel.root().getChildModelDeep("hat");
        }
        else if (modelPart.equals("headwear2"))
        {
            return villagermodel.root().getChildModelDeep("hat_rim");
        }
        else if (modelPart.equals("body"))
        {
            return villagermodel.root().getChildModelDeep("body");
        }
        else if (modelPart.equals("bodywear"))
        {
            return villagermodel.root().getChildModelDeep("jacket");
        }
        else if (modelPart.equals("arms"))
        {
            return villagermodel.root().getChildModelDeep("arms");
        }
        else if (modelPart.equals("right_leg"))
        {
            return villagermodel.root().getChildModelDeep("right_leg");
        }
        else if (modelPart.equals("left_leg"))
        {
            return villagermodel.root().getChildModelDeep("left_leg");
        }
        else if (modelPart.equals("nose"))
        {
            return villagermodel.root().getChildModelDeep("nose");
        }
        else
        {
            return modelPart.equals("root") ? villagermodel.root() : null;
        }
    }

    @Override
    public String[] getModelRendererNames()
    {
        return new String[] {"head", "headwear", "headwear2", "body", "bodywear", "arms", "right_leg", "left_leg", "nose", "root"};
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        ReloadableResourceManager reloadableresourcemanager = (ReloadableResourceManager)Minecraft.getInstance().getResourceManager();
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        VillagerRenderer villagerrenderer = new VillagerRenderer(entityrenderdispatcher.getContext());
        villagerrenderer.model = (VillagerModel<Villager>)modelBase;
        villagerrenderer.shadowRadius = shadowSize;
        return villagerrenderer;
    }
}
