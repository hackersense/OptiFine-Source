package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;

public class ModelAdapterWanderingTrader extends ModelAdapterVillager
{
    public ModelAdapterWanderingTrader()
    {
        super(EntityType.WANDERING_TRADER, "wandering_trader", 0.5F);
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        WanderingTraderRenderer wanderingtraderrenderer = new WanderingTraderRenderer(entityrenderdispatcher.getContext());
        wanderingtraderrenderer.model = (VillagerModel<WanderingTrader>)modelBase;
        wanderingtraderrenderer.shadowRadius = shadowSize;
        return wanderingtraderrenderer;
    }
}
