package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.optifine.Config;

public class ModelAdapterEnderChest extends ModelAdapter
{
    public ModelAdapterEnderChest()
    {
        super(BlockEntityType.ENDER_CHEST, "ender_chest", 0.0F);
    }

    @Override
    public Model makeModel()
    {
        return new ChestModel();
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart)
    {
        if (!(model instanceof ChestModel chestmodel))
        {
            return null;
        }
        else if (modelPart.equals("lid"))
        {
            return chestmodel.lid;
        }
        else if (modelPart.equals("base"))
        {
            return chestmodel.base;
        }
        else
        {
            return modelPart.equals("knob") ? chestmodel.knob : null;
        }
    }

    @Override
    public String[] getModelRendererNames()
    {
        return new String[] {"lid", "base", "knob"};
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        BlockEntityRenderDispatcher blockentityrenderdispatcher = Config.getMinecraft().getBlockEntityRenderDispatcher();
        BlockEntityRenderer blockentityrenderer = rendererCache.get(
                    BlockEntityType.ENDER_CHEST, index, () -> new ChestRenderer(blockentityrenderdispatcher.getContext())
                );

        if (!(blockentityrenderer instanceof ChestRenderer))
        {
            return null;
        }
        else if (!(modelBase instanceof ChestModel chestmodel))
        {
            Config.warn("Not a chest model: " + modelBase);
            return null;
        }
        else
        {
            return chestmodel.updateRenderer(blockentityrenderer);
        }
    }
}
