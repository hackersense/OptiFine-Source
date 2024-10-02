package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.SpectralArrowRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterSpectralArrow extends ModelAdapterArrow
{
    public ModelAdapterSpectralArrow()
    {
        super(EntityType.SPECTRAL_ARROW, "spectral_arrow", 0.0F);
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index)
    {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        SpectralArrowRenderer spectralarrowrenderer = new SpectralArrowRenderer(entityrenderdispatcher.getContext());
        spectralarrowrenderer.model = (ArrowModel)modelBase;
        spectralarrowrenderer.shadowRadius = shadowSize;
        return spectralarrowrenderer;
    }
}
