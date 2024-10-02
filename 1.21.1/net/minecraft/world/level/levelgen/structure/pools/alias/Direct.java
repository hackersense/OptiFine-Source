package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

record Direct(ResourceKey<StructureTemplatePool> alias, ResourceKey<StructureTemplatePool> target) implements PoolAliasBinding
{
    static MapCodec<Direct> CODEC = RecordCodecBuilder.mapCodec(
        p_311220_ -> p_311220_.group(
            ResourceKey.codec(Registries.TEMPLATE_POOL).fieldOf("alias").forGetter(Direct::alias),
            ResourceKey.codec(Registries.TEMPLATE_POOL).fieldOf("target").forGetter(Direct::target)
        )
        .apply(p_311220_, Direct::new)
    );

    @Override
    public void forEachResolved(RandomSource p_312348_, BiConsumer<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> p_310565_)
    {
        p_310565_.accept(this.alias, this.target);
    }

    @Override
    public Stream<ResourceKey<StructureTemplatePool>> allTargets()
    {
        return Stream.of(this.target);
    }

    @Override
    public MapCodec<Direct> codec()
    {
        return CODEC;
    }
}
