package com.mojang.realmsclient.util;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

public enum LevelType
{
    DEFAULT(0, WorldPresets.NORMAL),
    FLAT(1, WorldPresets.FLAT),
    LARGE_BIOMES(2, WorldPresets.LARGE_BIOMES),
    AMPLIFIED(3, WorldPresets.AMPLIFIED);

    private final int index;
    private final Component name;

    private LevelType(final int p_239483_, final ResourceKey<WorldPreset> p_239484_)
    {
        this.index = p_239483_;
        this.name = Component.translatable(p_239484_.location().toLanguageKey("generator"));
    }

    public Component getName()
    {
        return this.name;
    }

    public int getDtoIndex()
    {
        return this.index;
    }
}
