package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterHead extends ModelAdapter
{
    private ModelLayerLocation modelLayer;
    private SkullBlock.Types skullBlockType;

    public ModelAdapterHead(String name, ModelLayerLocation modelLayer, SkullBlock.Types skullBlockType)
    {
        super(BlockEntityType.SKULL, name, 0.0F);
        this.modelLayer = modelLayer;
        this.skullBlockType = skullBlockType;
    }

    @Override
    public Model makeModel()
    {
        return new SkullModel(bakeModelLayer(this.modelLayer));
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
        BlockEntityRenderDispatcher blockentityrenderdispatcher = Config.getMinecraft().getBlockEntityRenderDispatcher();
        BlockEntityRenderer blockentityrenderer = rendererCache.get(
                    BlockEntityType.SKULL, index, () -> new SkullBlockRenderer(blockentityrenderdispatcher.getContext())
                );

        if (!(blockentityrenderer instanceof SkullBlockRenderer))
        {
            return null;
        }
        else
        {
            Map<SkullBlock.Type, SkullModelBase> map = SkullBlockRenderer.models;

            if (map == null)
            {
                Config.warn("Field not found: SkullBlockRenderer.models");
                return null;
            }
            else
            {
                map.put(this.skullBlockType, (SkullModelBase)modelBase);
                return blockentityrenderer;
            }
        }
    }
}
