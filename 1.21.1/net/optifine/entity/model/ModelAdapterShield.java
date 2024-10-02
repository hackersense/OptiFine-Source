package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.optifine.reflect.Reflector;
import net.optifine.util.Either;

public class ModelAdapterShield extends ModelAdapter
{
    public ModelAdapterShield()
    {
        super((Either<EntityType, BlockEntityType>)null, "shield", 0.0F, null);
    }

    @Override
    public Model makeModel()
    {
        return new ShieldModel(bakeModelLayer(ModelLayers.SHIELD));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart)
    {
        if (!(model instanceof ShieldModel shieldmodel))
        {
            return null;
        }
        else if (modelPart.equals("plate"))
        {
            return (ModelPart)Reflector.ModelShield_ModelRenderers.getValue(shieldmodel, 1);
        }
        else if (modelPart.equals("handle"))
        {
            return (ModelPart)Reflector.ModelShield_ModelRenderers.getValue(shieldmodel, 2);
        }
        else
        {
            return modelPart.equals("root") ? (ModelPart)Reflector.ModelShield_ModelRenderers.getValue(shieldmodel, 0) : null;
        }
    }

    @Override
    public String[] getModelRendererNames()
    {
        return new String[] {"plate", "handle", "root"};
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        IEntityRenderer ientityrenderer = new VirtualEntityRenderer(modelBase);
        return ientityrenderer;
    }
}
