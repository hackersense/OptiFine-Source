package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.dragon.DragonHeadModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.level.block.SkullBlock;
import net.optifine.reflect.Reflector;

public class ModelAdapterHeadDragon extends ModelAdapterHead
{
    public ModelAdapterHeadDragon()
    {
        super("head_dragon", null, SkullBlock.Types.DRAGON);
    }

    @Override
    public Model makeModel()
    {
        return new DragonHeadModel(bakeModelLayer(ModelLayers.DRAGON_SKULL));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart)
    {
        if (!(model instanceof DragonHeadModel dragonheadmodel))
        {
            return null;
        }
        else if (modelPart.equals("head"))
        {
            return (ModelPart)Reflector.getFieldValue(dragonheadmodel, Reflector.ModelDragonHead_head);
        }
        else
        {
            return modelPart.equals("jaw") ? (ModelPart)Reflector.getFieldValue(dragonheadmodel, Reflector.ModelDragonHead_jaw) : null;
        }
    }

    @Override
    public String[] getModelRendererNames()
    {
        return new String[] {"head", "jaw"};
    }
}
