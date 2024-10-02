package net.optifine.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public class ComponentUtils
{
    public static String getTranslationKey(Component comp)
    {
        if (comp == null)
        {
            return null;
        }
        else
        {
            return !(comp.getContents() instanceof TranslatableContents translatablecontents) ? null : translatablecontents.getKey();
        }
    }
}
