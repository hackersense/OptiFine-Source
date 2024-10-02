package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class RealmsNotification
{
    static final Logger LOGGER = LogUtils.getLogger();
    private static final String NOTIFICATION_UUID = "notificationUuid";
    private static final String DISMISSABLE = "dismissable";
    private static final String SEEN = "seen";
    private static final String TYPE = "type";
    private static final String VISIT_URL = "visitUrl";
    private static final String INFO_POPUP = "infoPopup";
    static final Component BUTTON_TEXT_FALLBACK = Component.translatable("mco.notification.visitUrl.buttonText.default");
    final UUID uuid;
    final boolean dismissable;
    final boolean seen;
    final String type;

    RealmsNotification(UUID p_275316_, boolean p_275303_, boolean p_275497_, String p_275401_)
    {
        this.uuid = p_275316_;
        this.dismissable = p_275303_;
        this.seen = p_275497_;
        this.type = p_275401_;
    }

    public boolean seen()
    {
        return this.seen;
    }

    public boolean dismissable()
    {
        return this.dismissable;
    }

    public UUID uuid()
    {
        return this.uuid;
    }

    public static List<RealmsNotification> parseList(String p_275464_)
    {
        List<RealmsNotification> list = new ArrayList<>();

        try
        {
            for (JsonElement jsonelement : JsonParser.parseString(p_275464_).getAsJsonObject().get("notifications").getAsJsonArray())
            {
                list.add(parse(jsonelement.getAsJsonObject()));
            }
        }
        catch (Exception exception)
        {
            LOGGER.error("Could not parse list of RealmsNotifications", (Throwable)exception);
        }

        return list;
    }

    private static RealmsNotification parse(JsonObject p_275549_)
    {
        UUID uuid = JsonUtils.getUuidOr("notificationUuid", p_275549_, null);

        if (uuid == null)
        {
            throw new IllegalStateException("Missing required property notificationUuid");
        }
        else
        {
            boolean flag = JsonUtils.getBooleanOr("dismissable", p_275549_, true);
            boolean flag1 = JsonUtils.getBooleanOr("seen", p_275549_, false);
            String s = JsonUtils.getRequiredString("type", p_275549_);
            RealmsNotification realmsnotification = new RealmsNotification(uuid, flag, flag1, s);

            return (RealmsNotification)(switch (s)
        {
            case "visitUrl" -> RealmsNotification.VisitUrl.parse(realmsnotification, p_275549_);

                case "infoPopup" -> RealmsNotification.InfoPopup.parse(realmsnotification, p_275549_);

                default -> realmsnotification;
            });
        }
    }

    public static class InfoPopup extends RealmsNotification
    {
        private static final String TITLE = "title";
        private static final String MESSAGE = "message";
        private static final String IMAGE = "image";
        private static final String URL_BUTTON = "urlButton";
        private final RealmsText title;
        private final RealmsText message;
        private final ResourceLocation image;
        @Nullable
        private final RealmsNotification.UrlButton urlButton;

        private InfoPopup(
            RealmsNotification p_311557_,
            RealmsText p_310281_,
            RealmsText p_312392_,
            ResourceLocation p_312062_,
            @Nullable RealmsNotification.UrlButton p_309599_
        )
        {
            super(p_311557_.uuid, p_311557_.dismissable, p_311557_.seen, p_311557_.type);
            this.title = p_310281_;
            this.message = p_312392_;
            this.image = p_312062_;
            this.urlButton = p_309599_;
        }

        public static RealmsNotification.InfoPopup parse(RealmsNotification p_311623_, JsonObject p_309477_)
        {
            RealmsText realmstext = JsonUtils.getRequired("title", p_309477_, RealmsText::parse);
            RealmsText realmstext1 = JsonUtils.getRequired("message", p_309477_, RealmsText::parse);
            ResourceLocation resourcelocation = ResourceLocation.parse(JsonUtils.getRequiredString("image", p_309477_));
            RealmsNotification.UrlButton realmsnotification$urlbutton = JsonUtils.getOptional("urlButton", p_309477_, RealmsNotification.UrlButton::parse);
            return new RealmsNotification.InfoPopup(p_311623_, realmstext, realmstext1, resourcelocation, realmsnotification$urlbutton);
        }

        @Nullable
        public PopupScreen buildScreen(Screen p_312232_, Consumer<UUID> p_310624_)
        {
            Component component = this.title.createComponent();

            if (component == null)
            {
                RealmsNotification.LOGGER.warn("Realms info popup had title with no available translation: {}", this.title);
                return null;
            }
            else
            {
                PopupScreen.Builder popupscreen$builder = new PopupScreen.Builder(p_312232_, component)
                .setImage(this.image)
                .setMessage(this.message.createComponent(CommonComponents.EMPTY));

                if (this.urlButton != null)
                {
                    popupscreen$builder.addButton(this.urlButton.urlText.createComponent(RealmsNotification.BUTTON_TEXT_FALLBACK), p_310971_ ->
                    {
                        Minecraft minecraft = Minecraft.getInstance();
                        minecraft.setScreen(new ConfirmLinkScreen(p_311599_ -> {
                            if (p_311599_)
                            {
                                Util.getPlatform().openUri(this.urlButton.url);
                                minecraft.setScreen(p_312232_);
                            }
                            else {
                                minecraft.setScreen(p_310971_);
                            }
                        }, this.urlButton.url, true));
                        p_310624_.accept(this.uuid());
                    });
                }

                popupscreen$builder.addButton(CommonComponents.GUI_OK, p_311029_ ->
                {
                    p_311029_.onClose();
                    p_310624_.accept(this.uuid());
                });
                popupscreen$builder.onClose(() -> p_310624_.accept(this.uuid()));
                return popupscreen$builder.build();
            }
        }
    }

    static record UrlButton(String url, RealmsText urlText)
    {
        private static final String URL = "url";
        private static final String URL_TEXT = "urlText";
        public static RealmsNotification.UrlButton parse(JsonObject p_310888_)
        {
            String s = JsonUtils.getRequiredString("url", p_310888_);
            RealmsText realmstext = JsonUtils.getRequired("urlText", p_310888_, RealmsText::parse);
            return new RealmsNotification.UrlButton(s, realmstext);
        }
    }

    public static class VisitUrl extends RealmsNotification
    {
        private static final String URL = "url";
        private static final String BUTTON_TEXT = "buttonText";
        private static final String MESSAGE = "message";
        private final String url;
        private final RealmsText buttonText;
        private final RealmsText message;

        private VisitUrl(RealmsNotification p_275564_, String p_275312_, RealmsText p_275433_, RealmsText p_275541_)
        {
            super(p_275564_.uuid, p_275564_.dismissable, p_275564_.seen, p_275564_.type);
            this.url = p_275312_;
            this.buttonText = p_275433_;
            this.message = p_275541_;
        }

        public static RealmsNotification.VisitUrl parse(RealmsNotification p_275651_, JsonObject p_275278_)
        {
            String s = JsonUtils.getRequiredString("url", p_275278_);
            RealmsText realmstext = JsonUtils.getRequired("buttonText", p_275278_, RealmsText::parse);
            RealmsText realmstext1 = JsonUtils.getRequired("message", p_275278_, RealmsText::parse);
            return new RealmsNotification.VisitUrl(p_275651_, s, realmstext, realmstext1);
        }

        public Component getMessage()
        {
            return this.message.createComponent(Component.translatable("mco.notification.visitUrl.message.default"));
        }

        public Button buildOpenLinkButton(Screen p_275412_)
        {
            Component component = this.buttonText.createComponent(RealmsNotification.BUTTON_TEXT_FALLBACK);
            return Button.builder(component, ConfirmLinkScreen.confirmLink(p_275412_, this.url)).build();
        }
    }
}
