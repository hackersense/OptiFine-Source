package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.realms.DisconnectedRealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerLinks;
import org.slf4j.Logger;

public abstract class ClientCommonPacketListenerImpl implements ClientCommonPacketListener
{
    private static final Component GENERIC_DISCONNECT_MESSAGE = Component.translatable("disconnect.lost");
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final Minecraft minecraft;
    protected final Connection connection;
    @Nullable
    protected final ServerData serverData;
    @Nullable
    protected String serverBrand;
    protected final WorldSessionTelemetryManager telemetryManager;
    @Nullable
    protected final Screen postDisconnectScreen;
    protected boolean isTransferring;
    @Deprecated(
        forRemoval = true
    )
    protected final boolean strictErrorHandling;
    private final List<ClientCommonPacketListenerImpl.DeferredPacket> deferredPackets = new ArrayList<>();
    protected final Map<ResourceLocation, byte[]> serverCookies;
    protected Map<String, String> customReportDetails;
    protected ServerLinks serverLinks;

    protected ClientCommonPacketListenerImpl(Minecraft p_300051_, Connection p_300688_, CommonListenerCookie p_300429_)
    {
        this.minecraft = p_300051_;
        this.connection = p_300688_;
        this.serverData = p_300429_.serverData();
        this.serverBrand = p_300429_.serverBrand();
        this.telemetryManager = p_300429_.telemetryManager();
        this.postDisconnectScreen = p_300429_.postDisconnectScreen();
        this.serverCookies = p_300429_.serverCookies();
        this.strictErrorHandling = p_300429_.strictErrorHandling();
        this.customReportDetails = p_300429_.customReportDetails();
        this.serverLinks = p_300429_.serverLinks();
    }

    @Override
    public void onPacketError(Packet p_333124_, Exception p_332903_)
    {
        LOGGER.error("Failed to handle packet {}", p_333124_, p_332903_);
        Optional<Path> optional = this.storeDisconnectionReport(p_333124_, p_332903_);
        Optional<URI> optional1 = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT).map(ServerLinks.Entry::link);

        if (this.strictErrorHandling)
        {
            this.connection.disconnect(new DisconnectionDetails(Component.translatable("disconnect.packetError"), optional, optional1));
        }
    }

    @Override
    public DisconnectionDetails createDisconnectionInfo(Component p_342124_, Throwable p_344768_)
    {
        Optional<Path> optional = this.storeDisconnectionReport(null, p_344768_);
        Optional<URI> optional1 = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT).map(ServerLinks.Entry::link);
        return new DisconnectionDetails(p_342124_, optional, optional1);
    }

    private Optional<Path> storeDisconnectionReport(@Nullable Packet p_344412_, Throwable p_344707_)
    {
        CrashReport crashreport = CrashReport.forThrowable(p_344707_, "Packet handling error");
        PacketUtils.fillCrashReport(crashreport, this, p_344412_);
        Path path = this.minecraft.gameDirectory.toPath().resolve("debug");
        Path path1 = path.resolve("disconnect-" + Util.getFilenameFormattedDateTime() + "-client.txt");
        Optional<ServerLinks.Entry> optional = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT);
        List<String> list = optional.<List<String>>map(p_340889_ -> List.of("Server bug reporting link: " + p_340889_.link())).orElse(List.of());
        return crashreport.saveToFile(path1, ReportType.NETWORK_PROTOCOL_ERROR, list) ? Optional.of(path1) : Optional.empty();
    }

    @Override
    public boolean shouldHandleMessage(Packet<?> p_332498_)
    {
        return ClientCommonPacketListener.super.shouldHandleMessage(p_332498_)
               ? true
               : this.isTransferring && (p_332498_ instanceof ClientboundStoreCookiePacket || p_332498_ instanceof ClientboundTransferPacket);
    }

    @Override
    public void handleKeepAlive(ClientboundKeepAlivePacket p_301155_)
    {
        this.sendWhen(new ServerboundKeepAlivePacket(p_301155_.getId()), () -> !RenderSystem.isFrozenAtPollEvents(), Duration.ofMinutes(1L));
    }

    @Override
    public void handlePing(ClientboundPingPacket p_300922_)
    {
        PacketUtils.ensureRunningOnSameThread(p_300922_, this, this.minecraft);
        this.send(new ServerboundPongPacket(p_300922_.getId()));
    }

    @Override
    public void handleCustomPayload(ClientboundCustomPayloadPacket p_298103_)
    {
        CustomPacketPayload custompacketpayload = p_298103_.payload();

        if (!(custompacketpayload instanceof DiscardedPayload))
        {
            PacketUtils.ensureRunningOnSameThread(p_298103_, this, this.minecraft);

            if (custompacketpayload instanceof BrandPayload brandpayload)
            {
                this.serverBrand = brandpayload.brand();
                this.telemetryManager.onServerBrandReceived(brandpayload.brand());
            }
            else
            {
                this.handleCustomPayload(custompacketpayload);
            }
        }
    }

    protected abstract void handleCustomPayload(CustomPacketPayload p_297976_);

    @Override
    public void handleResourcePackPush(ClientboundResourcePackPushPacket p_310071_)
    {
        PacketUtils.ensureRunningOnSameThread(p_310071_, this, this.minecraft);
        UUID uuid = p_310071_.id();
        URL url = parseResourcePackUrl(p_310071_.url());

        if (url == null)
        {
            this.connection.send(new ServerboundResourcePackPacket(uuid, ServerboundResourcePackPacket.Action.INVALID_URL));
        }
        else
        {
            String s = p_310071_.hash();
            boolean flag = p_310071_.required();
            ServerData.ServerPackStatus serverdata$serverpackstatus = this.serverData != null ? this.serverData.getResourcePackStatus() : ServerData.ServerPackStatus.PROMPT;

            if (serverdata$serverpackstatus != ServerData.ServerPackStatus.PROMPT
                    && (!flag || serverdata$serverpackstatus != ServerData.ServerPackStatus.DISABLED))
            {
                this.minecraft.getDownloadedPackSource().pushPack(uuid, url, s);
            }
            else
            {
                this.minecraft.setScreen(this.addOrUpdatePackPrompt(uuid, url, s, flag, p_310071_.prompt().orElse(null)));
            }
        }
    }

    @Override
    public void handleResourcePackPop(ClientboundResourcePackPopPacket p_311803_)
    {
        PacketUtils.ensureRunningOnSameThread(p_311803_, this, this.minecraft);
        p_311803_.id().ifPresentOrElse(p_308277_ -> this.minecraft.getDownloadedPackSource().popPack(p_308277_), () -> this.minecraft.getDownloadedPackSource().popAll());
    }

    static Component preparePackPrompt(Component p_299226_, @Nullable Component p_298885_)
    {
        return (Component)(p_298885_ == null ? p_299226_ : Component.translatable("multiplayer.texturePrompt.serverPrompt", p_299226_, p_298885_));
    }

    @Nullable
    private static URL parseResourcePackUrl(String p_298850_)
    {
        try
        {
            URL url = new URL(p_298850_);
            String s = url.getProtocol();
            return !"http".equals(s) && !"https".equals(s) ? null : url;
        }
        catch (MalformedURLException malformedurlexception)
        {
            return null;
        }
    }

    @Override
    public void handleRequestCookie(ClientboundCookieRequestPacket p_328943_)
    {
        PacketUtils.ensureRunningOnSameThread(p_328943_, this, this.minecraft);
        this.connection.send(new ServerboundCookieResponsePacket(p_328943_.key(), this.serverCookies.get(p_328943_.key())));
    }

    @Override
    public void handleStoreCookie(ClientboundStoreCookiePacket p_333290_)
    {
        PacketUtils.ensureRunningOnSameThread(p_333290_, this, this.minecraft);
        this.serverCookies.put(p_333290_.key(), p_333290_.payload());
    }

    @Override
    public void handleCustomReportDetails(ClientboundCustomReportDetailsPacket p_342751_)
    {
        PacketUtils.ensureRunningOnSameThread(p_342751_, this, this.minecraft);
        this.customReportDetails = p_342751_.details();
    }

    @Override
    public void handleServerLinks(ClientboundServerLinksPacket p_342144_)
    {
        PacketUtils.ensureRunningOnSameThread(p_342144_, this, this.minecraft);
        List<ServerLinks.UntrustedEntry> list = p_342144_.links();
        Builder<ServerLinks.Entry> builder = ImmutableList.builderWithExpectedSize(list.size());

        for (ServerLinks.UntrustedEntry serverlinks$untrustedentry : list)
        {
            try
            {
                URI uri = Util.parseAndValidateUntrustedUri(serverlinks$untrustedentry.link());
                builder.add(new ServerLinks.Entry(serverlinks$untrustedentry.type(), uri));
            }
            catch (Exception exception)
            {
                LOGGER.warn(
                    "Received invalid link for type {}:{}", serverlinks$untrustedentry.type(), serverlinks$untrustedentry.link(), exception
                );
            }
        }

        this.serverLinks = new ServerLinks(builder.build());
    }

    @Override
    public void handleTransfer(ClientboundTransferPacket p_332424_)
    {
        this.isTransferring = true;
        PacketUtils.ensureRunningOnSameThread(p_332424_, this, this.minecraft);

        if (this.serverData == null)
        {
            throw new IllegalStateException("Cannot transfer to server from singleplayer");
        }
        else
        {
            this.connection.disconnect(Component.translatable("disconnect.transfer"));
            this.connection.setReadOnly();
            this.connection.handleDisconnection();
            ServerAddress serveraddress = new ServerAddress(p_332424_.host(), p_332424_.port());
            ConnectScreen.startConnecting(
                Objects.requireNonNullElseGet(this.postDisconnectScreen, TitleScreen::new),
                this.minecraft,
                serveraddress,
                this.serverData,
                false,
                new TransferState(this.serverCookies)
            );
        }
    }

    @Override
    public void handleDisconnect(ClientboundDisconnectPacket p_298016_)
    {
        this.connection.disconnect(p_298016_.reason());
    }

    protected void sendDeferredPackets()
    {
        Iterator<ClientCommonPacketListenerImpl.DeferredPacket> iterator = this.deferredPackets.iterator();

        while (iterator.hasNext())
        {
            ClientCommonPacketListenerImpl.DeferredPacket clientcommonpacketlistenerimpl$deferredpacket = iterator.next();

            if (clientcommonpacketlistenerimpl$deferredpacket.sendCondition().getAsBoolean())
            {
                this.send(clientcommonpacketlistenerimpl$deferredpacket.packet);
                iterator.remove();
            }
            else if (clientcommonpacketlistenerimpl$deferredpacket.expirationTime() <= Util.getMillis())
            {
                iterator.remove();
            }
        }
    }

    public void send(Packet<?> p_300175_)
    {
        this.connection.send(p_300175_);
    }

    @Override
    public void onDisconnect(DisconnectionDetails p_344141_)
    {
        this.telemetryManager.onDisconnect();
        this.minecraft.disconnect(this.createDisconnectScreen(p_344141_), this.isTransferring);
        LOGGER.warn("Client disconnected with reason: {}", p_344141_.reason().getString());
    }

    @Override
    public void fillListenerSpecificCrashDetails(CrashReport p_342520_, CrashReportCategory p_309761_)
    {
        p_309761_.setDetail("Server type", () -> this.serverData != null ? this.serverData.type().toString() : "<none>");
        p_309761_.setDetail("Server brand", () -> this.serverBrand);

        if (!this.customReportDetails.isEmpty())
        {
            CrashReportCategory crashreportcategory = p_342520_.addCategory("Custom Server Details");
            this.customReportDetails.forEach(crashreportcategory::setDetail);
        }
    }

    protected Screen createDisconnectScreen(DisconnectionDetails p_342895_)
    {
        Screen screen = Objects.requireNonNullElseGet(this.postDisconnectScreen, () -> new JoinMultiplayerScreen(new TitleScreen()));
        return (Screen)(this.serverData != null && this.serverData.isRealm()
                        ? new DisconnectedRealmsScreen(screen, GENERIC_DISCONNECT_MESSAGE, p_342895_.reason())
                        : new DisconnectedScreen(screen, GENERIC_DISCONNECT_MESSAGE, p_342895_));
    }

    @Nullable
    public String serverBrand()
    {
        return this.serverBrand;
    }

    private void sendWhen(Packet <? extends ServerboundPacketListener > p_300852_, BooleanSupplier p_299754_, Duration p_299011_)
    {
        if (p_299754_.getAsBoolean())
        {
            this.send(p_300852_);
        }
        else
        {
            this.deferredPackets.add(new ClientCommonPacketListenerImpl.DeferredPacket(p_300852_, p_299754_, Util.getMillis() + p_299011_.toMillis()));
        }
    }

    private Screen addOrUpdatePackPrompt(UUID p_313077_, URL p_312880_, String p_309420_, boolean p_312218_, @Nullable Component p_309535_)
    {
        Screen screen = this.minecraft.screen;
        return screen instanceof ClientCommonPacketListenerImpl.PackConfirmScreen clientcommonpacketlistenerimpl$packconfirmscreen
               ? clientcommonpacketlistenerimpl$packconfirmscreen.update(this.minecraft, p_313077_, p_312880_, p_309420_, p_312218_, p_309535_)
               : new ClientCommonPacketListenerImpl.PackConfirmScreen(
                   this.minecraft,
                   screen,
                   List.of(new ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest(p_313077_, p_312880_, p_309420_)),
                   p_312218_,
                   p_309535_
               );
    }

    static record DeferredPacket(Packet <? extends ServerboundPacketListener > packet, BooleanSupplier sendCondition, long expirationTime)
    {
    }

    class PackConfirmScreen extends ConfirmScreen
    {
        private final List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> requests;
        @Nullable
        private final Screen parentScreen;

        PackConfirmScreen(
            final Minecraft p_309743_,
            @Nullable final Screen p_312679_,
            final List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> p_312458_,
            final boolean p_313140_,
            @Nullable final Component p_312901_
        )
        {
            super(
                p_309396_ ->
            {
                p_309743_.setScreen(p_312679_);
                DownloadedPackSource downloadedpacksource = p_309743_.getDownloadedPackSource();

                if (p_309396_)
                {
                    if (ClientCommonPacketListenerImpl.this.serverData != null)
                    {
                        ClientCommonPacketListenerImpl.this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
                    }

                    downloadedpacksource.allowServerPacks();
                }
                else {
                    downloadedpacksource.rejectServerPacks();

                    if (p_313140_)
                    {
                        ClientCommonPacketListenerImpl.this.connection.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
                    }
                    else if (ClientCommonPacketListenerImpl.this.serverData != null)
                    {
                        ClientCommonPacketListenerImpl.this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
                    }
                }

                for (ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest : p_312458_)
                {
                    downloadedpacksource.pushPack(
                        clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest.id,
                        clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest.url,
                        clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest.hash
                    );
                }

                if (ClientCommonPacketListenerImpl.this.serverData != null)
                {
                    ServerList.saveSingleServer(ClientCommonPacketListenerImpl.this.serverData);
                }
            },
            p_313140_ ? Component.translatable("multiplayer.requiredTexturePrompt.line1") : Component.translatable("multiplayer.texturePrompt.line1"),
            ClientCommonPacketListenerImpl.preparePackPrompt(
                p_313140_
                ? Component.translatable("multiplayer.requiredTexturePrompt.line2").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                : Component.translatable("multiplayer.texturePrompt.line2"),
                p_312901_
            ),
            p_313140_ ? CommonComponents.GUI_PROCEED : CommonComponents.GUI_YES,
            p_313140_ ? CommonComponents.GUI_DISCONNECT : CommonComponents.GUI_NO
            );
            this.requests = p_312458_;
            this.parentScreen = p_312679_;
        }

        public ClientCommonPacketListenerImpl.PackConfirmScreen update(
            Minecraft p_312486_, UUID p_311436_, URL p_309404_, String p_312909_, boolean p_312985_, @Nullable Component p_309496_
        )
        {
            List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> list = ImmutableList.<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest>builderWithExpectedSize(
                        this.requests.size() + 1
                    )
                    .addAll(this.requests)
                    .add(new ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest(p_311436_, p_309404_, p_312909_))
                    .build();
            return ClientCommonPacketListenerImpl.this.new PackConfirmScreen(p_312486_, this.parentScreen, list, p_312985_, p_309496_);
        }

        static record PendingRequest(UUID id, URL url, String hash)
        {
        }
    }
}
