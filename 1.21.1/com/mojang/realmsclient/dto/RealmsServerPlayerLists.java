package com.mojang.realmsclient.dto;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public class RealmsServerPlayerLists extends ValueObject
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public Map<Long, List<ProfileResult>> servers = Map.of();

    public static RealmsServerPlayerLists parse(String p_87597_)
    {
        RealmsServerPlayerLists realmsserverplayerlists = new RealmsServerPlayerLists();
        Builder<Long, List<ProfileResult>> builder = ImmutableMap.builder();

        try
        {
            JsonObject jsonobject = GsonHelper.parse(p_87597_);

            if (GsonHelper.isArrayNode(jsonobject, "lists"))
            {
                for (JsonElement jsonelement : jsonobject.getAsJsonArray("lists"))
                {
                    JsonObject jsonobject1 = jsonelement.getAsJsonObject();
                    String s = JsonUtils.getStringOr("playerList", jsonobject1, null);
                    List<ProfileResult> list;

                    if (s != null)
                    {
                        JsonElement jsonelement1 = JsonParser.parseString(s);

                        if (jsonelement1.isJsonArray())
                        {
                            list = parsePlayers(jsonelement1.getAsJsonArray());
                        }
                        else
                        {
                            list = Lists.newArrayList();
                        }
                    }
                    else
                    {
                        list = Lists.newArrayList();
                    }

                    builder.put(JsonUtils.getLongOr("serverId", jsonobject1, -1L), list);
                }
            }
        }
        catch (Exception exception)
        {
            LOGGER.error("Could not parse RealmsServerPlayerLists: {}", exception.getMessage());
        }

        realmsserverplayerlists.servers = builder.build();
        return realmsserverplayerlists;
    }

    private static List<ProfileResult> parsePlayers(JsonArray p_342185_)
    {
        List<ProfileResult> list = new ArrayList<>(p_342185_.size());
        MinecraftSessionService minecraftsessionservice = Minecraft.getInstance().getMinecraftSessionService();

        for (JsonElement jsonelement : p_342185_)
        {
            if (jsonelement.isJsonObject())
            {
                UUID uuid = JsonUtils.getUuidOr("playerId", jsonelement.getAsJsonObject(), null);

                if (uuid != null && !Minecraft.getInstance().isLocalPlayer(uuid))
                {
                    try
                    {
                        ProfileResult profileresult = minecraftsessionservice.fetchProfile(uuid, false);

                        if (profileresult != null)
                        {
                            list.add(profileresult);
                        }
                    }
                    catch (Exception exception)
                    {
                        LOGGER.error("Could not get name for {}", uuid, exception);
                    }
                }
            }
        }

        return list;
    }

    public List<ProfileResult> getProfileResultsFor(long p_343284_)
    {
        List<ProfileResult> list = this.servers.get(p_343284_);
        return list != null ? list : List.of();
    }
}
