package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public interface EnchantmentLocationBasedEffect
{
    Codec<EnchantmentLocationBasedEffect> CODEC = BuiltInRegistries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE
            .byNameCodec()
            .dispatch(EnchantmentLocationBasedEffect::codec, Function.identity());

    static MapCodec <? extends EnchantmentLocationBasedEffect > bootstrap(Registry < MapCodec <? extends EnchantmentLocationBasedEffect >> p_344274_)
    {
        Registry.register(p_344274_, "all_of", AllOf.LocationBasedEffects.CODEC);
        Registry.register(p_344274_, "apply_mob_effect", ApplyMobEffect.CODEC);
        Registry.register(p_344274_, "attribute", EnchantmentAttributeEffect.CODEC);
        Registry.register(p_344274_, "damage_entity", DamageEntity.CODEC);
        Registry.register(p_344274_, "damage_item", DamageItem.CODEC);
        Registry.register(p_344274_, "explode", ExplodeEffect.CODEC);
        Registry.register(p_344274_, "ignite", Ignite.CODEC);
        Registry.register(p_344274_, "play_sound", PlaySoundEffect.CODEC);
        Registry.register(p_344274_, "replace_block", ReplaceBlock.CODEC);
        Registry.register(p_344274_, "replace_disk", ReplaceDisk.CODEC);
        Registry.register(p_344274_, "run_function", RunFunction.CODEC);
        Registry.register(p_344274_, "set_block_properties", SetBlockProperties.CODEC);
        Registry.register(p_344274_, "spawn_particles", SpawnParticlesEffect.CODEC);
        return Registry.register(p_344274_, "summon_entity", SummonEntityEffect.CODEC);
    }

    void onChangedBlock(ServerLevel p_342313_, int p_345031_, EnchantedItemInUse p_345418_, Entity p_344951_, Vec3 p_344517_, boolean p_345369_);

default void onDeactivated(EnchantedItemInUse p_343068_, Entity p_344744_, Vec3 p_342973_, int p_342439_)
    {
    }

    MapCodec <? extends EnchantmentLocationBasedEffect > codec();
}
