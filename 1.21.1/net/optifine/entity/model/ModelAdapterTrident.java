package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.world.entity.EntityType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterTrident extends ModelAdapter
{
    public ModelAdapterTrident()
    {
        super(EntityType.TRIDENT, "trident", 0.0F);
    }

    @Override
    public Model makeModel()
    {
        return new TridentModel(bakeModelLayer(ModelLayers.TRIDENT));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart)
    {
        if (!(model instanceof TridentModel tridentmodel))
        {
            return null;
        }
        else
        {
            ModelPart modelpart = (ModelPart)Reflector.ModelTrident_root.getValue(tridentmodel);

            if (modelpart != null)
            {
                if (modelPart.equals("body"))
                {
                    return modelpart.getChildModelDeep("pole");
                }

                if (modelPart.equals("base"))
                {
                    return modelpart.getChildModelDeep("base");
                }

                if (modelPart.equals("left_spike"))
                {
                    return modelpart.getChildModelDeep("left_spike");
                }

                if (modelPart.equals("middle_spike"))
                {
                    return modelpart.getChildModelDeep("middle_spike");
                }

                if (modelPart.equals("right_spike"))
                {
                    return modelpart.getChildModelDeep("right_spike");
                }
            }

            return modelPart.equals("root") ? modelpart : null;
        }
    }

    @Override
    public String[] getModelRendererNames()
    {
        return new String[] {"body", "base", "left_spike", "middle_spike", "right_spike", "root"};
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        ThrownTridentRenderer throwntridentrenderer = new ThrownTridentRenderer(entityrenderdispatcher.getContext());

        if (!Reflector.RenderTrident_modelTrident.exists())
        {
            Config.warn("Field not found: RenderTrident.modelTrident");
            return null;
        }
        else
        {
            Reflector.setFieldValue(throwntridentrenderer, Reflector.RenderTrident_modelTrident, modelBase);
            throwntridentrenderer.shadowRadius = shadowSize;
            return throwntridentrenderer;
        }
    }
}
