package net.minecraft.client.resources.model;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.IForgeModelBaker;

public interface ModelBaker extends IForgeModelBaker
{
    UnbakedModel getModel(ResourceLocation p_252194_);

    @Nullable
    BakedModel bake(ResourceLocation p_250776_, ModelState p_251280_);
}
