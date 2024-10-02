package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.optifine.Config;

public class ModelAdapterBell extends ModelAdapter
{
    public ModelAdapterBell()
    {
        super(BlockEntityType.BELL, "bell", 0.0F);
    }

    @Override
    public Model makeModel()
    {
        return new BellModel();
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart)
    {
        if (!(model instanceof BellModel bellmodel))
        {
            return null;
        }
        else
        {
            return modelPart.equals("body") ? bellmodel.bellBody : null;
        }
    }

    @Override
    public String[] getModelRendererNames()
    {
        return new String[] {"body"};
    }

    @Override
    public IEntityRenderer makeEntityRender(Model model, float shadowSize, RendererCache rendererCache, int index)
    {
        BlockEntityRenderDispatcher blockentityrenderdispatcher = Config.getMinecraft().getBlockEntityRenderDispatcher();
        BlockEntityRenderer blockentityrenderer = rendererCache.get(
                    BlockEntityType.BELL, index, () -> new BellRenderer(blockentityrenderdispatcher.getContext())
                );

        if (!(blockentityrenderer instanceof BellRenderer))
        {
            return null;
        }
        else if (!(model instanceof BellModel bellmodel))
        {
            Config.warn("Not a bell model: " + model);
            return null;
        }
        else
        {
            return bellmodel.updateRenderer(blockentityrenderer);
        }
    }
}
