package net.minecraft.network.protocol.game;

import com.google.common.base.MoreObjects;
import com.mojang.authlib.GameProfile;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public class ClientboundPlayerInfoUpdatePacket implements Packet<ClientGamePacketListener>
{
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPlayerInfoUpdatePacket> STREAM_CODEC = Packet.codec(
                ClientboundPlayerInfoUpdatePacket::write, ClientboundPlayerInfoUpdatePacket::new
            );
    private final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions;
    private final List<ClientboundPlayerInfoUpdatePacket.Entry> entries;

    public ClientboundPlayerInfoUpdatePacket(EnumSet<ClientboundPlayerInfoUpdatePacket.Action> p_251739_, Collection<ServerPlayer> p_251579_)
    {
        this.actions = p_251739_;
        this.entries = p_251579_.stream().map(ClientboundPlayerInfoUpdatePacket.Entry::new).toList();
    }

    public ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action p_251648_, ServerPlayer p_252273_)
    {
        this.actions = EnumSet.of(p_251648_);
        this.entries = List.of(new ClientboundPlayerInfoUpdatePacket.Entry(p_252273_));
    }

    public static ClientboundPlayerInfoUpdatePacket createPlayerInitializing(Collection<ServerPlayer> p_252314_)
    {
        EnumSet<ClientboundPlayerInfoUpdatePacket.Action> enumset = EnumSet.of(
                    ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                    ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
                );
        return new ClientboundPlayerInfoUpdatePacket(enumset, p_252314_);
    }

    private ClientboundPlayerInfoUpdatePacket(RegistryFriendlyByteBuf p_330420_)
    {
        this.actions = p_330420_.readEnumSet(ClientboundPlayerInfoUpdatePacket.Action.class);
        this.entries = p_330420_.readList(
                             p_326100_ ->
        {
            ClientboundPlayerInfoUpdatePacket.EntryBuilder clientboundplayerinfoupdatepacket$entrybuilder = new ClientboundPlayerInfoUpdatePacket.EntryBuilder(
                p_326100_.readUUID()
            );

            for (ClientboundPlayerInfoUpdatePacket.Action clientboundplayerinfoupdatepacket$action : this.actions)
            {
                clientboundplayerinfoupdatepacket$action.reader
                .read(clientboundplayerinfoupdatepacket$entrybuilder, (RegistryFriendlyByteBuf)p_326100_);
            }

            return clientboundplayerinfoupdatepacket$entrybuilder.build();
        }
                         );
    }

    private void write(RegistryFriendlyByteBuf p_332405_)
    {
        p_332405_.writeEnumSet(this.actions, ClientboundPlayerInfoUpdatePacket.Action.class);
        p_332405_.writeCollection(this.entries, (p_326101_, p_326102_) ->
        {
            p_326101_.writeUUID(p_326102_.profileId());

            for (ClientboundPlayerInfoUpdatePacket.Action clientboundplayerinfoupdatepacket$action : this.actions)
            {
                clientboundplayerinfoupdatepacket$action.writer.write((RegistryFriendlyByteBuf)p_326101_, p_326102_);
            }
        });
    }

    @Override
    public PacketType<ClientboundPlayerInfoUpdatePacket> type()
    {
        return GamePacketTypes.CLIENTBOUND_PLAYER_INFO_UPDATE;
    }

    public void handle(ClientGamePacketListener p_249935_)
    {
        p_249935_.handlePlayerInfoUpdate(this);
    }

    public EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions()
    {
        return this.actions;
    }

    public List<ClientboundPlayerInfoUpdatePacket.Entry> entries()
    {
        return this.entries;
    }

    public List<ClientboundPlayerInfoUpdatePacket.Entry> newEntries()
    {
        return this.actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER) ? this.entries : List.of();
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this).add("actions", this.actions).add("entries", this.entries).toString();
    }

    public static enum Action
    {
        ADD_PLAYER((p_326115_, p_326116_) -> {
            GameProfile gameprofile = new GameProfile(p_326115_.profileId, p_326116_.readUtf(16));
            gameprofile.getProperties().putAll(ByteBufCodecs.GAME_PROFILE_PROPERTIES.decode(p_326116_));
            p_326115_.profile = gameprofile;
        }, (p_326113_, p_326114_) -> {
            GameProfile gameprofile = Objects.requireNonNull(p_326114_.profile());
            p_326113_.writeUtf(gameprofile.getName(), 16);
            ByteBufCodecs.GAME_PROFILE_PROPERTIES.encode(p_326113_, gameprofile.getProperties());
        }),
        INITIALIZE_CHAT(
            (p_326119_, p_326120_) -> p_326119_.chatSession = p_326120_.readNullable(RemoteChatSession.Data::read),
            (p_326121_, p_326122_) -> p_326121_.writeNullable(p_326122_.chatSession, RemoteChatSession.Data::write)
        ),
        UPDATE_GAME_MODE(
            (p_326123_, p_326124_) -> p_326123_.gameMode = GameType.byId(p_326124_.readVarInt()),
            (p_326125_, p_326126_) -> p_326125_.writeVarInt(p_326126_.gameMode().getId())
        ),
        UPDATE_LISTED(
            (p_326107_, p_326108_) -> p_326107_.listed = p_326108_.readBoolean(), (p_326105_, p_326106_) -> p_326105_.writeBoolean(p_326106_.listed())
        ),
        UPDATE_LATENCY(
            (p_326109_, p_326110_) -> p_326109_.latency = p_326110_.readVarInt(), (p_326117_, p_326118_) -> p_326117_.writeVarInt(p_326118_.latency())
        ),
        UPDATE_DISPLAY_NAME(
            (p_326103_, p_326104_) -> p_326103_.displayName = FriendlyByteBuf.readNullable(p_326104_, ComponentSerialization.TRUSTED_STREAM_CODEC),
            (p_326111_, p_326112_) -> FriendlyByteBuf.writeNullable(p_326111_, p_326112_.displayName(), ComponentSerialization.TRUSTED_STREAM_CODEC)
        );

        final ClientboundPlayerInfoUpdatePacket.Action.Reader reader;
        final ClientboundPlayerInfoUpdatePacket.Action.Writer writer;

        private Action(final ClientboundPlayerInfoUpdatePacket.Action.Reader p_249392_, final ClientboundPlayerInfoUpdatePacket.Action.Writer p_250487_)
        {
            this.reader = p_249392_;
            this.writer = p_250487_;
        }

        public interface Reader {
            void read(ClientboundPlayerInfoUpdatePacket.EntryBuilder p_251859_, RegistryFriendlyByteBuf p_332411_);
        }

        public interface Writer {
            void write(RegistryFriendlyByteBuf p_330677_, ClientboundPlayerInfoUpdatePacket.Entry p_249783_);
        }
    }

    public static record Entry(
        UUID profileId,
        @Nullable GameProfile profile,
        boolean listed,
        int latency,
        GameType gameMode,
        @Nullable Component displayName,
        @Nullable RemoteChatSession.Data chatSession
    )
    {
        Entry(ServerPlayer p_252094_)
        {
            this(
                p_252094_.getUUID(),
                p_252094_.getGameProfile(),
                true,
                p_252094_.connection.latency(),
                p_252094_.gameMode.getGameModeForPlayer(),
                p_252094_.getTabListDisplayName(),
                Optionull.map(p_252094_.getChatSession(), RemoteChatSession::asData)
            );
        }
    }

    static class EntryBuilder
    {
        final UUID profileId;
        @Nullable
        GameProfile profile;
        boolean listed;
        int latency;
        GameType gameMode = GameType.DEFAULT_MODE;
        @Nullable
        Component displayName;
        @Nullable
        RemoteChatSession.Data chatSession;

        EntryBuilder(UUID p_251670_)
        {
            this.profileId = p_251670_;
        }

        ClientboundPlayerInfoUpdatePacket.Entry build()
        {
            return new ClientboundPlayerInfoUpdatePacket.Entry(
                       this.profileId, this.profile, this.listed, this.latency, this.gameMode, this.displayName, this.chatSession
                   );
        }
    }
}
