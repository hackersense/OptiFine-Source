package net.optifine.util;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.optifine.reflect.Reflector;

public class PotionUtils
{
    public static MobEffect getPotion(ResourceLocation loc)
    {
        return !BuiltInRegistries.MOB_EFFECT.containsKey(loc) ? null : BuiltInRegistries.MOB_EFFECT.get(loc);
    }

    public static MobEffect getPotion(int potionID)
    {
        return BuiltInRegistries.MOB_EFFECT.byId(potionID);
    }

    public static int getId(MobEffect potionIn)
    {
        return BuiltInRegistries.MOB_EFFECT.getId(potionIn);
    }

    public static String getPotionBaseName(Potion p)
    {
        return p == null ? null : (String)Reflector.Potion_baseName.getValue(p);
    }

    public static Potion getPotion(ItemStack itemStack)
    {
        PotionContents potioncontents = itemStack.get(DataComponents.POTION_CONTENTS);

        if (potioncontents == null)
        {
            return null;
        }
        else
        {
            Optional<Holder<Potion>> optional = potioncontents.potion();

            if (optional.isEmpty())
            {
                return null;
            }
            else
            {
                Holder<Potion> holder = optional.get();
                return !holder.isBound() ? null : holder.value();
            }
        }
    }
}
