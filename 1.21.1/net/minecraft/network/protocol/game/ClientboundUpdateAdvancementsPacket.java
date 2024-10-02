package net.minecraft.network.protocol.game;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceLocation;

public class ClientboundUpdateAdvancementsPacket implements Packet<ClientGamePacketListener>
{
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundUpdateAdvancementsPacket> STREAM_CODEC = Packet.codec(
                ClientboundUpdateAdvancementsPacket::write, ClientboundUpdateAdvancementsPacket::new
            );
    private final boolean reset;
    private final List<AdvancementHolder> added;
    private final Set<ResourceLocation> removed;
    private final Map<ResourceLocation, AdvancementProgress> progress;

    public ClientboundUpdateAdvancementsPacket(
        boolean p_133560_, Collection<AdvancementHolder> p_133561_, Set<ResourceLocation> p_133562_, Map<ResourceLocation, AdvancementProgress> p_133563_
    )
    {
        this.reset = p_133560_;
        this.added = List.copyOf(p_133561_);
        this.removed = Set.copyOf(p_133562_);
        this.progress = Map.copyOf(p_133563_);
    }

    private ClientboundUpdateAdvancementsPacket(RegistryFriendlyByteBuf p_329261_)
    {
        this.reset = p_329261_.readBoolean();
        this.added = AdvancementHolder.LIST_STREAM_CODEC.decode(p_329261_);
        this.removed = p_329261_.readCollection(Sets::newLinkedHashSetWithExpectedSize, FriendlyByteBuf::readResourceLocation);
        this.progress = p_329261_.readMap(FriendlyByteBuf::readResourceLocation, AdvancementProgress::fromNetwork);
    }

    private void write(RegistryFriendlyByteBuf p_328856_)
    {
        p_328856_.writeBoolean(this.reset);
        AdvancementHolder.LIST_STREAM_CODEC.encode(p_328856_, this.added);
        p_328856_.writeCollection(this.removed, FriendlyByteBuf::writeResourceLocation);
        p_328856_.writeMap(this.progress, FriendlyByteBuf::writeResourceLocation, (p_179444_, p_179445_) -> p_179445_.serializeToNetwork(p_179444_));
    }

    @Override
    public PacketType<ClientboundUpdateAdvancementsPacket> type()
    {
        return GamePacketTypes.CLIENTBOUND_UPDATE_ADVANCEMENTS;
    }

    public void handle(ClientGamePacketListener p_133569_)
    {
        p_133569_.handleUpdateAdvancementsPacket(this);
    }

    public List<AdvancementHolder> getAdded()
    {
        return this.added;
    }

    public Set<ResourceLocation> getRemoved()
    {
        return this.removed;
    }

    public Map<ResourceLocation, AdvancementProgress> getProgress()
    {
        return this.progress;
    }

    public boolean shouldReset()
    {
        return this.reset;
    }
}
