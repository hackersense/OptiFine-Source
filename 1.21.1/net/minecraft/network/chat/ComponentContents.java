package net.minecraft.network.chat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;

public interface ComponentContents
{
default <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> p_237130_, Style p_237131_)
    {
        return Optional.empty();
    }

default <T> Optional<T> visit(FormattedText.ContentConsumer<T> p_237129_)
    {
        return Optional.empty();
    }

default MutableComponent resolve(@Nullable CommandSourceStack p_237126_, @Nullable Entity p_237127_, int p_237128_) throws CommandSyntaxException
        {
            return MutableComponent.create(this);
        }

    ComponentContents.Type<?> type();

    public static record Type<T extends ComponentContents>(MapCodec<T> codec, String id) implements StringRepresentable
    {
        @Override
        public String getSerializedName()
        {
            return this.id;
        }
    }
}
