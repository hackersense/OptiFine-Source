package net.minecraft.client;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;

public record GuiMessage(int addedTime, Component content, @Nullable MessageSignature signature, @Nullable GuiMessageTag tag)
{
    @Nullable
    public GuiMessageTag.Icon icon()
    {
        return this.tag != null ? this.tag.icon() : null;
    }
    public static record Line(int addedTime, FormattedCharSequence content, @Nullable GuiMessageTag tag, boolean endOfEntry)
    {
    }
}
