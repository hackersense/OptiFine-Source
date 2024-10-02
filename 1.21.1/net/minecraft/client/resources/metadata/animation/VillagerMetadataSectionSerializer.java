package net.minecraft.client.resources.metadata.animation;

import com.google.gson.JsonObject;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;

public class VillagerMetadataSectionSerializer implements MetadataSectionSerializer<VillagerMetaDataSection>
{
    public VillagerMetaDataSection fromJson(JsonObject p_119095_)
    {
        return new VillagerMetaDataSection(VillagerMetaDataSection.Hat.getByName(GsonHelper.getAsString(p_119095_, "hat", "none")));
    }

    @Override
    public String getMetadataSectionName()
    {
        return "villager";
    }
}
