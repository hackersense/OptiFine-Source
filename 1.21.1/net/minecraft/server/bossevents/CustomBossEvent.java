package net.minecraft.server.bossevents;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

public class CustomBossEvent extends ServerBossEvent
{
    private final ResourceLocation id;
    private final Set<UUID> players = Sets.newHashSet();
    private int value;
    private int max = 100;

    public CustomBossEvent(ResourceLocation p_136261_, Component p_136262_)
    {
        super(p_136262_, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
        this.id = p_136261_;
        this.setProgress(0.0F);
    }

    public ResourceLocation getTextId()
    {
        return this.id;
    }

    @Override
    public void addPlayer(ServerPlayer p_136267_)
    {
        super.addPlayer(p_136267_);
        this.players.add(p_136267_.getUUID());
    }

    public void addOfflinePlayer(UUID p_136271_)
    {
        this.players.add(p_136271_);
    }

    @Override
    public void removePlayer(ServerPlayer p_136281_)
    {
        super.removePlayer(p_136281_);
        this.players.remove(p_136281_.getUUID());
    }

    @Override
    public void removeAllPlayers()
    {
        super.removeAllPlayers();
        this.players.clear();
    }

    public int getValue()
    {
        return this.value;
    }

    public int getMax()
    {
        return this.max;
    }

    public void setValue(int p_136265_)
    {
        this.value = p_136265_;
        this.setProgress(Mth.clamp((float)p_136265_ / (float)this.max, 0.0F, 1.0F));
    }

    public void setMax(int p_136279_)
    {
        this.max = p_136279_;
        this.setProgress(Mth.clamp((float)this.value / (float)p_136279_, 0.0F, 1.0F));
    }

    public final Component getDisplayName()
    {
        return ComponentUtils.wrapInSquareBrackets(this.getName())
               .withStyle(
                   p_326211_ -> p_326211_.withColor(this.getColor().getFormatting())
                   .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(this.getTextId().toString())))
                   .withInsertion(this.getTextId().toString())
               );
    }

    public boolean setPlayers(Collection<ServerPlayer> p_136269_)
    {
        Set<UUID> set = Sets.newHashSet();
        Set<ServerPlayer> set1 = Sets.newHashSet();

        for (UUID uuid : this.players)
        {
            boolean flag = false;

            for (ServerPlayer serverplayer : p_136269_)
            {
                if (serverplayer.getUUID().equals(uuid))
                {
                    flag = true;
                    break;
                }
            }

            if (!flag)
            {
                set.add(uuid);
            }
        }

        for (ServerPlayer serverplayer1 : p_136269_)
        {
            boolean flag1 = false;

            for (UUID uuid2 : this.players)
            {
                if (serverplayer1.getUUID().equals(uuid2))
                {
                    flag1 = true;
                    break;
                }
            }

            if (!flag1)
            {
                set1.add(serverplayer1);
            }
        }

        for (UUID uuid1 : set)
        {
            for (ServerPlayer serverplayer3 : this.getPlayers())
            {
                if (serverplayer3.getUUID().equals(uuid1))
                {
                    this.removePlayer(serverplayer3);
                    break;
                }
            }

            this.players.remove(uuid1);
        }

        for (ServerPlayer serverplayer2 : set1)
        {
            this.addPlayer(serverplayer2);
        }

        return !set.isEmpty() || !set1.isEmpty();
    }

    public CompoundTag save(HolderLookup.Provider p_328456_)
    {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("Name", Component.Serializer.toJson(this.name, p_328456_));
        compoundtag.putBoolean("Visible", this.isVisible());
        compoundtag.putInt("Value", this.value);
        compoundtag.putInt("Max", this.max);
        compoundtag.putString("Color", this.getColor().getName());
        compoundtag.putString("Overlay", this.getOverlay().getName());
        compoundtag.putBoolean("DarkenScreen", this.shouldDarkenScreen());
        compoundtag.putBoolean("PlayBossMusic", this.shouldPlayBossMusic());
        compoundtag.putBoolean("CreateWorldFog", this.shouldCreateWorldFog());
        ListTag listtag = new ListTag();

        for (UUID uuid : this.players)
        {
            listtag.add(NbtUtils.createUUID(uuid));
        }

        compoundtag.put("Players", listtag);
        return compoundtag;
    }

    public static CustomBossEvent load(CompoundTag p_136273_, ResourceLocation p_136274_, HolderLookup.Provider p_327949_)
    {
        CustomBossEvent custombossevent = new CustomBossEvent(p_136274_, Component.Serializer.fromJson(p_136273_.getString("Name"), p_327949_));
        custombossevent.setVisible(p_136273_.getBoolean("Visible"));
        custombossevent.setValue(p_136273_.getInt("Value"));
        custombossevent.setMax(p_136273_.getInt("Max"));
        custombossevent.setColor(BossEvent.BossBarColor.byName(p_136273_.getString("Color")));
        custombossevent.setOverlay(BossEvent.BossBarOverlay.byName(p_136273_.getString("Overlay")));
        custombossevent.setDarkenScreen(p_136273_.getBoolean("DarkenScreen"));
        custombossevent.setPlayBossMusic(p_136273_.getBoolean("PlayBossMusic"));
        custombossevent.setCreateWorldFog(p_136273_.getBoolean("CreateWorldFog"));

        for (Tag tag : p_136273_.getList("Players", 11))
        {
            custombossevent.addOfflinePlayer(NbtUtils.loadUUID(tag));
        }

        return custombossevent;
    }

    public void onPlayerConnect(ServerPlayer p_136284_)
    {
        if (this.players.contains(p_136284_.getUUID()))
        {
            this.addPlayer(p_136284_);
        }
    }

    public void onPlayerDisconnect(ServerPlayer p_136287_)
    {
        super.removePlayer(p_136287_);
    }
}
