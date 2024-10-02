package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BreezeModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.BreezeRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.breeze.Breeze;

public class ModelAdapterBreeze extends ModelAdapter
{
    private static Map<String, String> mapParts = makeMapParts();

    public ModelAdapterBreeze()
    {
        super(EntityType.BREEZE, "breeze", 0.8F);
    }

    protected ModelAdapterBreeze(EntityType entityType, String name, float shadowSize)
    {
        super(entityType, name, shadowSize);
    }

    @Override
    public Model makeModel()
    {
        return new BreezeModel(bakeModelLayer(ModelLayers.BREEZE));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart)
    {
        if (!(model instanceof BreezeModel breezemodel))
        {
            return null;
        }
        else if (modelPart.equals("root"))
        {
            return breezemodel.root();
        }
        else if (mapParts.containsKey(modelPart))
        {
            String s = mapParts.get(modelPart);
            return breezemodel.root().getChildModelDeep(s);
        }
        else
        {
            return null;
        }
    }

    @Override
    public String[] getModelRendererNames()
    {
        return mapParts.keySet().toArray(new String[0]);
    }

    private static Map<String, String> makeMapParts()
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "body");
        map.put("rods", "rods");
        map.put("head", "head");
        map.put("wind_body", "wind_body");
        map.put("wind_middle", "wind_mid");
        map.put("wind_bottom", "wind_bottom");
        map.put("wind_top", "wind_top");
        map.put("root", "root");
        return map;
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        BreezeRenderer breezerenderer = new BreezeRenderer(entityrenderdispatcher.getContext());
        breezerenderer.model = (BreezeModel<Breeze>)modelBase;
        breezerenderer.shadowRadius = shadowSize;
        return breezerenderer;
    }
}
