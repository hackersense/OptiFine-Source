package net.minecraft.client.resources.language;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import net.optifine.Lang;
import org.slf4j.Logger;

public class ClientLanguage extends Language
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<String, String> storage;
    private final boolean defaultRightToLeft;

    private ClientLanguage(Map<String, String> p_118914_, boolean p_118915_)
    {
        this.storage = p_118914_;
        this.defaultRightToLeft = p_118915_;
    }

    public static ClientLanguage loadFrom(ResourceManager p_265765_, List<String> p_265743_, boolean p_265470_)
    {
        Map<String, String> map = Maps.newHashMap();

        for (String s : p_265743_)
        {
            String s1 = String.format(Locale.ROOT, "lang/%s.json", s);

            for (String s2 : p_265765_.getNamespaces())
            {
                try
                {
                    ResourceLocation resourcelocation = ResourceLocation.fromNamespaceAndPath(s2, s1);
                    appendFrom(s, p_265765_.getResourceStack(resourcelocation), map);
                    Lang.loadResources(p_265765_, s, map);
                }
                catch (Exception exception1)
                {
                    LOGGER.warn("Skipped language file: {}:{} ({})", s2, s1, exception1.toString());
                }
            }
        }

        return new ClientLanguage(ImmutableMap.copyOf(map), p_265470_);
    }

    private static void appendFrom(String p_235036_, List<Resource> p_235037_, Map<String, String> p_235038_)
    {
        for (Resource resource : p_235037_)
        {
            try (InputStream inputstream = resource.open())
            {
                Language.loadFromJson(inputstream, p_235038_::put);
            }
            catch (IOException ioexception1)
            {
                LOGGER.warn("Failed to load translations for {} from pack {}", p_235036_, resource.sourcePackId(), ioexception1);
            }
        }
    }

    @Override
    public String getOrDefault(String p_118920_, String p_265273_)
    {
        return this.storage.getOrDefault(p_118920_, p_265273_);
    }

    @Override
    public boolean has(String p_118928_)
    {
        return this.storage.containsKey(p_118928_);
    }

    @Override
    public boolean isDefaultRightToLeft()
    {
        return this.defaultRightToLeft;
    }

    @Override
    public FormattedCharSequence getVisualOrder(FormattedText p_118925_)
    {
        return FormattedBidiReorder.reorder(p_118925_, this.defaultRightToLeft);
    }

    public Map<String, String> getLanguageData()
    {
        return this.storage;
    }
}
