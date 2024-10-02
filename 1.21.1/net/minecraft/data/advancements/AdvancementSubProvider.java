package net.minecraft.data.advancements;

import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

public interface AdvancementSubProvider
{
    void generate(HolderLookup.Provider p_255901_, Consumer<AdvancementHolder> p_250888_);

    static AdvancementHolder createPlaceholder(String p_312736_)
    {
        return Advancement.Builder.advancement().build(ResourceLocation.parse(p_312736_));
    }
}
