package net.optifine.config;

import net.minecraft.client.Options;

public interface IPersitentOption
{
    String getSaveKey();

    void loadValue(Options var1, String var2);

    String getSaveText(Options var1);
}
