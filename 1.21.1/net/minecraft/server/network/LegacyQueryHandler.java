package net.minecraft.server.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.SocketAddress;
import java.util.Locale;
import net.minecraft.server.ServerInfo;
import org.slf4j.Logger;

public class LegacyQueryHandler extends ChannelInboundHandlerAdapter
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ServerInfo server;

    public LegacyQueryHandler(ServerInfo p_298392_)
    {
        this.server = p_298392_;
    }

    @Override
    public void channelRead(ChannelHandlerContext p_9686_, Object p_9687_)
    {
        ByteBuf bytebuf = (ByteBuf)p_9687_;
        bytebuf.markReaderIndex();
        boolean flag = true;

        try
        {
            try
            {
                if (bytebuf.readUnsignedByte() != 254)
                {
                    return;
                }

                SocketAddress socketaddress = p_9686_.channel().remoteAddress();
                int i = bytebuf.readableBytes();

                if (i == 0)
                {
                    LOGGER.debug("Ping: (<1.3.x) from {}", socketaddress);
                    String s = createVersion0Response(this.server);
                    sendFlushAndClose(p_9686_, createLegacyDisconnectPacket(p_9686_.alloc(), s));
                }
                else
                {
                    if (bytebuf.readUnsignedByte() != 1)
                    {
                        return;
                    }

                    if (bytebuf.isReadable())
                    {
                        if (!readCustomPayloadPacket(bytebuf))
                        {
                            return;
                        }

                        LOGGER.debug("Ping: (1.6) from {}", socketaddress);
                    }
                    else
                    {
                        LOGGER.debug("Ping: (1.4-1.5.x) from {}", socketaddress);
                    }

                    String s1 = createVersion1Response(this.server);
                    sendFlushAndClose(p_9686_, createLegacyDisconnectPacket(p_9686_.alloc(), s1));
                }

                bytebuf.release();
                flag = false;
            }
            catch (RuntimeException runtimeexception)
            {
            }
        }
        finally
        {
            if (flag)
            {
                bytebuf.resetReaderIndex();
                p_9686_.channel().pipeline().remove(this);
                p_9686_.fireChannelRead(p_9687_);
            }
        }
    }

    private static boolean readCustomPayloadPacket(ByteBuf p_297429_)
    {
        short short1 = p_297429_.readUnsignedByte();

        if (short1 != 250)
        {
            return false;
        }
        else
        {
            String s = LegacyProtocolUtils.readLegacyString(p_297429_);

            if (!"MC|PingHost".equals(s))
            {
                return false;
            }
            else
            {
                int i = p_297429_.readUnsignedShort();

                if (p_297429_.readableBytes() != i)
                {
                    return false;
                }
                else
                {
                    short short2 = p_297429_.readUnsignedByte();

                    if (short2 < 73)
                    {
                        return false;
                    }
                    else
                    {
                        String s1 = LegacyProtocolUtils.readLegacyString(p_297429_);
                        int j = p_297429_.readInt();
                        return j <= 65535;
                    }
                }
            }
        }
    }

    private static String createVersion0Response(ServerInfo p_300881_)
    {
        return String.format(Locale.ROOT, "%s\u00a7%d\u00a7%d", p_300881_.getMotd(), p_300881_.getPlayerCount(), p_300881_.getMaxPlayers());
    }

    private static String createVersion1Response(ServerInfo p_297753_)
    {
        return String.format(
                   Locale.ROOT,
                   "\u00a71\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d",
                   127,
                   p_297753_.getServerVersion(),
                   p_297753_.getMotd(),
                   p_297753_.getPlayerCount(),
                   p_297753_.getMaxPlayers()
               );
    }

    private static void sendFlushAndClose(ChannelHandlerContext p_9681_, ByteBuf p_9682_)
    {
        p_9681_.pipeline().firstContext().writeAndFlush(p_9682_).addListener(ChannelFutureListener.CLOSE);
    }

    private static ByteBuf createLegacyDisconnectPacket(ByteBufAllocator p_298175_, String p_298389_)
    {
        ByteBuf bytebuf = p_298175_.buffer();
        bytebuf.writeByte(255);
        LegacyProtocolUtils.writeLegacyString(bytebuf, p_298389_);
        return bytebuf;
    }
}
