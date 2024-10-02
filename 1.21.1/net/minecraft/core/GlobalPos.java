package net.minecraft.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record GlobalPos(ResourceKey<Level> dimension, BlockPos pos)
{
    public static final MapCodec<GlobalPos> MAP_CODEC = RecordCodecBuilder.mapCodec(
                p_122642_ -> p_122642_.group(
                    Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(GlobalPos::dimension), BlockPos.CODEC.fieldOf("pos").forGetter(GlobalPos::pos)
                )
                .apply(p_122642_, GlobalPos::of)
            );
    public static final Codec<GlobalPos> CODEC = MAP_CODEC.codec();
    public static final StreamCodec<ByteBuf, GlobalPos> STREAM_CODEC = StreamCodec.composite(
                ResourceKey.streamCodec(Registries.DIMENSION), GlobalPos::dimension, BlockPos.STREAM_CODEC, GlobalPos::pos, GlobalPos::of
            );
    public static GlobalPos of(ResourceKey<Level> p_122644_, BlockPos p_122645_)
    {
        return new GlobalPos(p_122644_, p_122645_);
    }
    @Override
    public String toString()
    {
        return this.dimension + " " + this.pos;
    }
}
