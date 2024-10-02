package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.network.Filterable;

public record WritableBookContent(List<Filterable<String>> pages) implements BookContent<String, WritableBookContent>
{
    public static final WritableBookContent EMPTY = new WritableBookContent(List.of());
    public static final int PAGE_EDIT_LENGTH = 1024;
    public static final int MAX_PAGES = 100;
    private static final Codec<Filterable<String>> PAGE_CODEC = Filterable.codec(Codec.string(0, 1024));
    public static final Codec<List<Filterable<String>>> PAGES_CODEC = PAGE_CODEC.sizeLimitedListOf(100);
    public static final Codec<WritableBookContent> CODEC = RecordCodecBuilder.create(
        p_327725_ -> p_327725_.group(PAGES_CODEC.optionalFieldOf("pages", List.of()).forGetter(WritableBookContent::pages))
        .apply(p_327725_, WritableBookContent::new)
    );
    public static final StreamCodec<ByteBuf, WritableBookContent> STREAM_CODEC = Filterable.streamCodec(ByteBufCodecs.stringUtf8(1024))
    .apply(ByteBufCodecs.list(100))
    .map(WritableBookContent::new, WritableBookContent::pages);

    public WritableBookContent(List<Filterable<String>> pages)
    {
        if (pages.size() > 100)
        {
            throw new IllegalArgumentException("Got " + pages.size() + " pages, but maximum is 100");
        }
        else
        {
            this.pages = pages;
        }
    }

    public Stream<String> getPages(boolean p_333617_)
    {
        return this.pages.stream().map(p_334234_ -> p_334234_.get(p_333617_));
    }

    public WritableBookContent withReplacedPages(List<Filterable<String>> p_334830_)
    {
        return new WritableBookContent(p_334830_);
    }

    @Override
    public List<Filterable<String>> pages()
    {
        return this.pages;
    }
}
