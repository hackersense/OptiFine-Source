package net.optifine.util;

import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.optifine.reflect.Reflector;

public class ItemUtils
{
    private static CompoundTag EMPTY_TAG = new CompoundTag();

    public static Item getItem(ResourceLocation loc)
    {
        return !BuiltInRegistries.ITEM.containsKey(loc) ? null : BuiltInRegistries.ITEM.get(loc);
    }

    public static int getId(Item item)
    {
        return BuiltInRegistries.ITEM.getId(item);
    }

    public static CompoundTag getTag(ItemStack itemStack)
    {
        if (itemStack == null)
        {
            return EMPTY_TAG;
        }
        else
        {
            PatchedDataComponentMap patcheddatacomponentmap = (PatchedDataComponentMap)Reflector.ItemStack_components.getValue(itemStack);
            return patcheddatacomponentmap == null ? EMPTY_TAG : patcheddatacomponentmap.getTag();
        }
    }
}
