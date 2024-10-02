package net.minecraft.world.level.levelgen.flat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.item.Item;

public record FlatLevelGeneratorPreset(Holder<Item> displayItem, FlatLevelGeneratorSettings settings)
{
    public static final Codec<FlatLevelGeneratorPreset> DIRECT_CODEC = RecordCodecBuilder.create(
                p_259010_ -> p_259010_.group(
                    RegistryFixedCodec.create(Registries.ITEM).fieldOf("display").forGetter(p_226258_ -> p_226258_.displayItem),
                    FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(p_226255_ -> p_226255_.settings)
                )
                .apply(p_259010_, FlatLevelGeneratorPreset::new)
            );
    public static final Codec<Holder<FlatLevelGeneratorPreset>> CODEC = RegistryFileCodec.create(Registries.FLAT_LEVEL_GENERATOR_PRESET, DIRECT_CODEC);
}
