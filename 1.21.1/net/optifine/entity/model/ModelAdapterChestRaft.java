package net.optifine.entity.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ChestRaftModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.optifine.Config;
import net.optifine.util.ArrayUtils;

public class ModelAdapterChestRaft extends ModelAdapterRaft
{
    public ModelAdapterChestRaft()
    {
        super(EntityType.CHEST_BOAT, "chest_raft", 0.5F);
    }

    @Override
    public Model makeModel()
    {
        return new ChestRaftModel(bakeModelLayer(ModelLayers.createChestBoatModelName(Boat.Type.BAMBOO)));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart)
    {
        if (!(model instanceof ChestRaftModel chestraftmodel))
        {
            return null;
        }
        else
        {
            ImmutableList<ModelPart> immutablelist = chestraftmodel.parts();

            if (immutablelist != null)
            {
                if (modelPart.equals("chest_base"))
                {
                    return ModelRendererUtils.getModelRenderer(immutablelist, 3);
                }

                if (modelPart.equals("chest_lid"))
                {
                    return ModelRendererUtils.getModelRenderer(immutablelist, 4);
                }

                if (modelPart.equals("chest_knob"))
                {
                    return ModelRendererUtils.getModelRenderer(immutablelist, 5);
                }
            }

            return super.getModelRenderer(chestraftmodel, modelPart);
        }
    }

    @Override
    public String[] getModelRendererNames()
    {
        String[] astring = super.getModelRendererNames();
        return (String[])ArrayUtils.addObjectsToArray(astring, new String[] {"chest_base", "chest_lid", "chest_knob"});
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        BoatRenderer boatrenderer = new BoatRenderer(entityrenderdispatcher.getContext(), true);
        EntityRenderer entityrenderer = rendererCache.get(EntityType.CHEST_BOAT, index, () -> boatrenderer);

        if (!(entityrenderer instanceof BoatRenderer boatrenderer1))
        {
            Config.warn("Not a BoatRender: " + entityrenderer);
            return null;
        }
        else
        {
            return ModelAdapterBoat.makeEntityRender(modelBase, shadowSize, boatrenderer1);
        }
    }
}
