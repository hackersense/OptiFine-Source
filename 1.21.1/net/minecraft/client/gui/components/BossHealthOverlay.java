package net.minecraft.client.gui.components;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraftforge.eventbus.api.Event;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.reflect.Reflector;

public class BossHealthOverlay
{
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final ResourceLocation[] BAR_BACKGROUND_SPRITES = new ResourceLocation[]
    {
        ResourceLocation.withDefaultNamespace("boss_bar/pink_background"),
        ResourceLocation.withDefaultNamespace("boss_bar/blue_background"),
        ResourceLocation.withDefaultNamespace("boss_bar/red_background"),
        ResourceLocation.withDefaultNamespace("boss_bar/green_background"),
        ResourceLocation.withDefaultNamespace("boss_bar/yellow_background"),
        ResourceLocation.withDefaultNamespace("boss_bar/purple_background"),
        ResourceLocation.withDefaultNamespace("boss_bar/white_background")
    };
    private static final ResourceLocation[] BAR_PROGRESS_SPRITES = new ResourceLocation[]
    {
        ResourceLocation.withDefaultNamespace("boss_bar/pink_progress"),
        ResourceLocation.withDefaultNamespace("boss_bar/blue_progress"),
        ResourceLocation.withDefaultNamespace("boss_bar/red_progress"),
        ResourceLocation.withDefaultNamespace("boss_bar/green_progress"),
        ResourceLocation.withDefaultNamespace("boss_bar/yellow_progress"),
        ResourceLocation.withDefaultNamespace("boss_bar/purple_progress"),
        ResourceLocation.withDefaultNamespace("boss_bar/white_progress")
    };
    private static final ResourceLocation[] OVERLAY_BACKGROUND_SPRITES = new ResourceLocation[]
    {
        ResourceLocation.withDefaultNamespace("boss_bar/notched_6_background"),
        ResourceLocation.withDefaultNamespace("boss_bar/notched_10_background"),
        ResourceLocation.withDefaultNamespace("boss_bar/notched_12_background"),
        ResourceLocation.withDefaultNamespace("boss_bar/notched_20_background")
    };
    private static final ResourceLocation[] OVERLAY_PROGRESS_SPRITES = new ResourceLocation[]
    {
        ResourceLocation.withDefaultNamespace("boss_bar/notched_6_progress"),
        ResourceLocation.withDefaultNamespace("boss_bar/notched_10_progress"),
        ResourceLocation.withDefaultNamespace("boss_bar/notched_12_progress"),
        ResourceLocation.withDefaultNamespace("boss_bar/notched_20_progress")
    };
    private final Minecraft minecraft;
    final Map<UUID, LerpingBossEvent> events = Maps.newLinkedHashMap();

    public BossHealthOverlay(Minecraft p_93702_)
    {
        this.minecraft = p_93702_;
    }

    public void render(GuiGraphics p_283175_)
    {
        if (!this.events.isEmpty())
        {
            this.minecraft.getProfiler().push("bossHealth");
            int i = p_283175_.guiWidth();
            int j = 12;

            for (LerpingBossEvent lerpingbossevent : this.events.values())
            {
                int k = i / 2 - 91;
                boolean flag = true;
                int l = 19;

                if (Reflector.ForgeHooksClient_onCustomizeBossEventProgress.exists())
                {
                    Event event = (Event)Reflector.ForgeHooksClient_onCustomizeBossEventProgress
                                  .call(p_283175_, this.minecraft.getWindow(), lerpingbossevent, k, j, 10 + 9);
                    flag = !event.isCanceled();
                    l = Reflector.callInt(event, Reflector.CustomizeGuiOverlayEvent_BossEventProgress_getIncrement);
                }

                if (flag)
                {
                    this.drawBar(p_283175_, k, j, lerpingbossevent);
                    Component component = lerpingbossevent.getName();
                    int i1 = this.minecraft.font.width(component);
                    int j1 = i / 2 - i1 / 2;
                    int k1 = j - 9;
                    int l1 = 16777215;

                    if (Config.isCustomColors())
                    {
                        l1 = CustomColors.getBossTextColor(l1);
                    }

                    p_283175_.drawString(this.minecraft.font, component, j1, k1, l1);
                }

                j += l;

                if (j >= p_283175_.guiHeight() / 3)
                {
                    break;
                }
            }

            this.minecraft.getProfiler().pop();
        }
    }

    private void drawBar(GuiGraphics p_283672_, int p_283570_, int p_283306_, BossEvent p_283156_)
    {
        this.drawBar(p_283672_, p_283570_, p_283306_, p_283156_, 182, BAR_BACKGROUND_SPRITES, OVERLAY_BACKGROUND_SPRITES);
        int i = Mth.lerpDiscrete(p_283156_.getProgress(), 0, 182);

        if (i > 0)
        {
            this.drawBar(p_283672_, p_283570_, p_283306_, p_283156_, i, BAR_PROGRESS_SPRITES, OVERLAY_PROGRESS_SPRITES);
        }
    }

    private void drawBar(
        GuiGraphics p_281657_, int p_283675_, int p_282498_, BossEvent p_281288_, int p_283619_, ResourceLocation[] p_298746_, ResourceLocation[] p_298698_
    )
    {
        RenderSystem.enableBlend();
        p_281657_.blitSprite(p_298746_[p_281288_.getColor().ordinal()], 182, 5, 0, 0, p_283675_, p_282498_, p_283619_, 5);

        if (p_281288_.getOverlay() != BossEvent.BossBarOverlay.PROGRESS)
        {
            p_281657_.blitSprite(p_298698_[p_281288_.getOverlay().ordinal() - 1], 182, 5, 0, 0, p_283675_, p_282498_, p_283619_, 5);
        }

        RenderSystem.disableBlend();
    }

    public void update(ClientboundBossEventPacket p_93712_)
    {
        p_93712_.dispatch(
            new ClientboundBossEventPacket.Handler()
        {
            @Override
            public void add(
                UUID p_168824_,
                Component p_168825_,
                float p_168826_,
                BossEvent.BossBarColor p_168827_,
                BossEvent.BossBarOverlay p_168828_,
                boolean p_168829_,
                boolean p_168830_,
                boolean p_168831_
            )
            {
                BossHealthOverlay.this.events
                .put(p_168824_, new LerpingBossEvent(p_168824_, p_168825_, p_168826_, p_168827_, p_168828_, p_168829_, p_168830_, p_168831_));
            }
            @Override
            public void remove(UUID p_168812_)
            {
                BossHealthOverlay.this.events.remove(p_168812_);
            }
            @Override
            public void updateProgress(UUID p_168814_, float p_168815_)
            {
                BossHealthOverlay.this.events.get(p_168814_).setProgress(p_168815_);
            }
            @Override
            public void updateName(UUID p_168821_, Component p_168822_)
            {
                BossHealthOverlay.this.events.get(p_168821_).setName(p_168822_);
            }
            @Override
            public void updateStyle(UUID p_168817_, BossEvent.BossBarColor p_168818_, BossEvent.BossBarOverlay p_168819_)
            {
                LerpingBossEvent lerpingbossevent = BossHealthOverlay.this.events.get(p_168817_);
                lerpingbossevent.setColor(p_168818_);
                lerpingbossevent.setOverlay(p_168819_);
            }
            @Override
            public void updateProperties(UUID p_168833_, boolean p_168834_, boolean p_168835_, boolean p_168836_)
            {
                LerpingBossEvent lerpingbossevent = BossHealthOverlay.this.events.get(p_168833_);
                lerpingbossevent.setDarkenScreen(p_168834_);
                lerpingbossevent.setPlayBossMusic(p_168835_);
                lerpingbossevent.setCreateWorldFog(p_168836_);
            }
        }
        );
    }

    public void reset()
    {
        this.events.clear();
    }

    public boolean shouldPlayMusic()
    {
        if (!this.events.isEmpty())
        {
            for (BossEvent bossevent : this.events.values())
            {
                if (bossevent.shouldPlayBossMusic())
                {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean shouldDarkenScreen()
    {
        if (!this.events.isEmpty())
        {
            for (BossEvent bossevent : this.events.values())
            {
                if (bossevent.shouldDarkenScreen())
                {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean shouldCreateWorldFog()
    {
        if (!this.events.isEmpty())
        {
            for (BossEvent bossevent : this.events.values())
            {
                if (bossevent.shouldCreateWorldFog())
                {
                    return true;
                }
            }
        }

        return false;
    }

    public String getBossName()
    {
        if (!this.events.isEmpty())
        {
            for (BossEvent bossevent : this.events.values())
            {
                Component component = bossevent.getName();

                if (component != null && component.getContents() instanceof TranslatableContents translatablecontents)
                {
                    return translatablecontents.getKey();
                }
            }
        }

        return null;
    }
}
