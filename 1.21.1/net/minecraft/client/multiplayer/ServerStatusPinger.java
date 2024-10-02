package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import org.slf4j.Logger;

public class ServerStatusPinger
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component CANT_CONNECT_MESSAGE = Component.translatable("multiplayer.status.cannot_connect").withColor(-65536);
    private final List<Connection> connections = Collections.synchronizedList(Lists.newArrayList());

    public void pingServer(final ServerData p_105460_, final Runnable p_105461_, final Runnable p_335024_) throws UnknownHostException
    {
        final ServerAddress serveraddress = ServerAddress.parseString(p_105460_.ip);
        Optional<InetSocketAddress> optional = ServerNameResolver.DEFAULT.resolveAddress(serveraddress).map(ResolvedServerAddress::asInetSocketAddress);

        if (optional.isEmpty())
        {
            this.onPingFailed(ConnectScreen.UNKNOWN_HOST_MESSAGE, p_105460_);
        }
        else
        {
            final InetSocketAddress inetsocketaddress = optional.get();
            final Connection connection = Connection.connectToServer(inetsocketaddress, false, null);
            this.connections.add(connection);
            p_105460_.motd = Component.translatable("multiplayer.status.pinging");
            p_105460_.playerList = Collections.emptyList();
            ClientStatusPacketListener clientstatuspacketlistener = new ClientStatusPacketListener()
            {
                private boolean success;
                private boolean receivedPing;
                private long pingStart;
                @Override
                public void handleStatusResponse(ClientboundStatusResponsePacket p_105489_)
                {
                    if (this.receivedPing)
                    {
                        connection.disconnect(Component.translatable("multiplayer.status.unrequested"));
                    }
                    else
                    {
                        this.receivedPing = true;
                        ServerStatus serverstatus = p_105489_.status();
                        p_105460_.motd = serverstatus.description();
                        serverstatus.version().ifPresentOrElse(p_273307_ ->
                        {
                            p_105460_.version = Component.literal(p_273307_.name());
                            p_105460_.protocol = p_273307_.protocol();
                        }, () ->
                        {
                            p_105460_.version = Component.translatable("multiplayer.status.old");
                            p_105460_.protocol = 0;
                        });
                        serverstatus.players().ifPresentOrElse(p_273230_ ->
                        {
                            p_105460_.status = ServerStatusPinger.formatPlayerCount(p_273230_.online(), p_273230_.max());
                            p_105460_.players = p_273230_;

                            if (!p_273230_.sample().isEmpty())
                            {
                                List<Component> list = new ArrayList<>(p_273230_.sample().size());

                                for (GameProfile gameprofile : p_273230_.sample())
                                {
                                    list.add(Component.literal(gameprofile.getName()));
                                }

                                if (p_273230_.sample().size() < p_273230_.online())
                                {
                                    list.add(Component.translatable("multiplayer.status.and_more", p_273230_.online() - p_273230_.sample().size()));
                                }

                                p_105460_.playerList = list;
                            }
                            else {
                                p_105460_.playerList = List.of();
                            }
                        }, () -> p_105460_.status = Component.translatable("multiplayer.status.unknown").withStyle(ChatFormatting.DARK_GRAY));
                        serverstatus.favicon().ifPresent(p_272704_ ->
                        {
                            if (!Arrays.equals(p_272704_.iconBytes(), p_105460_.getIconBytes()))
                            {
                                p_105460_.setIconBytes(ServerData.validateIcon(p_272704_.iconBytes()));
                                p_105461_.run();
                            }
                        });
                        this.pingStart = Util.getMillis();
                        connection.send(new ServerboundPingRequestPacket(this.pingStart));
                        this.success = true;
                    }
                }
                @Override
                public void handlePongResponse(ClientboundPongResponsePacket p_329322_)
                {
                    long i = this.pingStart;
                    long j = Util.getMillis();
                    p_105460_.ping = j - i;
                    connection.disconnect(Component.translatable("multiplayer.status.finished"));
                    p_335024_.run();
                }
                @Override
                public void onDisconnect(DisconnectionDetails p_343233_)
                {
                    if (!this.success)
                    {
                        ServerStatusPinger.this.onPingFailed(p_343233_.reason(), p_105460_);
                        ServerStatusPinger.this.pingLegacyServer(inetsocketaddress, serveraddress, p_105460_);
                    }
                }
                @Override
                public boolean isAcceptingMessages()
                {
                    return connection.isConnected();
                }
            };

            try
            {
                connection.initiateServerboundStatusConnection(serveraddress.getHost(), serveraddress.getPort(), clientstatuspacketlistener);
                connection.send(ServerboundStatusRequestPacket.INSTANCE);
            }
            catch (Throwable throwable)
            {
                LOGGER.error("Failed to ping server {}", serveraddress, throwable);
            }
        }
    }

    void onPingFailed(Component p_171815_, ServerData p_171816_)
    {
        LOGGER.error("Can't ping {}: {}", p_171816_.ip, p_171815_.getString());
        p_171816_.motd = CANT_CONNECT_MESSAGE;
        p_171816_.status = CommonComponents.EMPTY;
    }

    void pingLegacyServer(InetSocketAddress p_171812_, final ServerAddress p_300887_, final ServerData p_171813_)
    {
        new Bootstrap().group(Connection.NETWORK_WORKER_GROUP.get()).handler(new ChannelInitializer<Channel>()
        {
            @Override
            protected void initChannel(Channel p_105498_)
            {
                try
                {
                    p_105498_.config().setOption(ChannelOption.TCP_NODELAY, true);
                }
                catch (ChannelException channelexception)
                {
                }

                p_105498_.pipeline().addLast(new LegacyServerPinger(p_300887_, (p_325482_, p_325483_, p_325484_, p_325485_, p_325486_) ->
                {
                    p_171813_.setState(ServerData.State.INCOMPATIBLE);
                    p_171813_.version = Component.literal(p_325483_);
                    p_171813_.motd = Component.literal(p_325484_);
                    p_171813_.status = ServerStatusPinger.formatPlayerCount(p_325485_, p_325486_);
                    p_171813_.players = new ServerStatus.Players(p_325486_, p_325485_, List.of());
                }));
            }
        }).channel(NioSocketChannel.class).connect(p_171812_.getAddress(), p_171812_.getPort());
    }

    public static Component formatPlayerCount(int p_105467_, int p_105468_)
    {
        Component component = Component.literal(Integer.toString(p_105467_)).withStyle(ChatFormatting.GRAY);
        Component component1 = Component.literal(Integer.toString(p_105468_)).withStyle(ChatFormatting.GRAY);
        return Component.translatable("multiplayer.status.player_count", component, component1).withStyle(ChatFormatting.DARK_GRAY);
    }

    public void tick()
    {
        synchronized (this.connections)
        {
            Iterator<Connection> iterator = this.connections.iterator();

            while (iterator.hasNext())
            {
                Connection connection = iterator.next();

                if (connection.isConnected())
                {
                    connection.tick();
                }
                else
                {
                    iterator.remove();
                    connection.handleDisconnection();
                }
            }
        }
    }

    public void removeAll()
    {
        synchronized (this.connections)
        {
            Iterator<Connection> iterator = this.connections.iterator();

            while (iterator.hasNext())
            {
                Connection connection = iterator.next();

                if (connection.isConnected())
                {
                    iterator.remove();
                    connection.disconnect(Component.translatable("multiplayer.status.cancelled"));
                }
            }
        }
    }
}
