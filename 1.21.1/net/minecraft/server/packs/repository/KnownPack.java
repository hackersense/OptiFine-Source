package net.minecraft.server.packs.repository;

import io.netty.buffer.ByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record KnownPack(String namespace, String id, String version)
{
    public static final StreamCodec<ByteBuf, KnownPack> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                KnownPack::namespace,
                ByteBufCodecs.STRING_UTF8,
                KnownPack::id,
                ByteBufCodecs.STRING_UTF8,
                KnownPack::version,
                KnownPack::new
            );
    public static final String VANILLA_NAMESPACE = "minecraft";
    public static KnownPack vanilla(String p_332942_)
    {
        return new KnownPack("minecraft", p_332942_, SharedConstants.getCurrentVersion().getId());
    }
    public boolean isVanilla()
    {
        return this.namespace.equals("minecraft");
    }
    @Override
    public String toString()
    {
        return this.namespace + ":" + this.id + ":" + this.version;
    }
}
