package net.minecraft.client.gui.components;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CommonButtons
{
    public static SpriteIconButton language(int p_299277_, Button.OnPress p_299778_, boolean p_301098_)
    {
        return SpriteIconButton.builder(Component.translatable("options.language"), p_299778_, p_301098_)
               .width(p_299277_)
               .sprite(ResourceLocation.withDefaultNamespace("icon/language"), 15, 15)
               .build();
    }

    public static SpriteIconButton accessibility(int p_300710_, Button.OnPress p_298571_, boolean p_299983_)
    {
        Component component = p_299983_ ? Component.translatable("options.accessibility") : Component.translatable("accessibility.onboarding.accessibility.button");
        return SpriteIconButton.builder(component, p_298571_, p_299983_)
               .width(p_300710_)
               .sprite(ResourceLocation.withDefaultNamespace("icon/accessibility"), 15, 15)
               .build();
    }
}
