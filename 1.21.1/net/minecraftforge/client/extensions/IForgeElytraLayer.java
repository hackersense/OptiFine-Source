package net.minecraftforge.client.extensions;

import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public interface IForgeElytraLayer<T>
{
default boolean shouldRender(ItemStack stack, T entity)
    {
        return stack.getItem() == Items.ELYTRA;
    }

default ResourceLocation getElytraTexture(ItemStack stack, T entity)
    {
        return ElytraLayer.WINGS_LOCATION;
    }
}
