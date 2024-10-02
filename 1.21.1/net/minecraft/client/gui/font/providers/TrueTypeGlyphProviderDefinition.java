package net.minecraft.client.gui.font.providers;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

public record TrueTypeGlyphProviderDefinition(
    ResourceLocation location, float size, float oversample, TrueTypeGlyphProviderDefinition.Shift shift, String skip
) implements GlyphProviderDefinition
{
    private static final Codec<String> SKIP_LIST_CODEC = Codec.withAlternative(Codec.STRING, Codec.STRING.listOf(), p_286852_ -> String.join("", p_286852_));
    public static final MapCodec<TrueTypeGlyphProviderDefinition> CODEC = RecordCodecBuilder.mapCodec(
        p_286284_ -> p_286284_.group(
            ResourceLocation.CODEC.fieldOf("file").forGetter(TrueTypeGlyphProviderDefinition::location),
            Codec.FLOAT.optionalFieldOf("size", Float.valueOf(11.0F)).forGetter(TrueTypeGlyphProviderDefinition::size),
            Codec.FLOAT.optionalFieldOf("oversample", Float.valueOf(1.0F)).forGetter(TrueTypeGlyphProviderDefinition::oversample),
            TrueTypeGlyphProviderDefinition.Shift.CODEC
            .optionalFieldOf("shift", TrueTypeGlyphProviderDefinition.Shift.NONE)
            .forGetter(TrueTypeGlyphProviderDefinition::shift),
            SKIP_LIST_CODEC.optionalFieldOf("skip", "").forGetter(TrueTypeGlyphProviderDefinition::skip)
        )
        .apply(p_286284_, TrueTypeGlyphProviderDefinition::new)
    );

    @Override
    public GlyphProviderType type()
    {
        return GlyphProviderType.TTF;
    }

    @Override
    public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack()
    {
        return Either.left(this::load);
    }

    private GlyphProvider load(ResourceManager p_286229_) throws IOException {
        FT_Face ft_face = null;
        ByteBuffer bytebuffer = null;

        try {
            TrueTypeGlyphProvider truetypeglyphprovider;

            try (InputStream inputstream = p_286229_.open(this.location.withPrefix("font/")))
            {
                bytebuffer = TextureUtil.readResource(inputstream);
                bytebuffer.flip();

                synchronized (FreeTypeUtil.LIBRARY_LOCK)
                {
                    try (MemoryStack memorystack = MemoryStack.stackPush())
                    {
                        PointerBuffer pointerbuffer = memorystack.mallocPointer(1);
                        FreeTypeUtil.assertError(FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), bytebuffer, 0L, pointerbuffer), "Initializing font face");
                        ft_face = FT_Face.create(pointerbuffer.get());
                    }

                    String s = FreeType.FT_Get_Font_Format(ft_face);

                    if (!"TrueType".equals(s))
                    {
                        throw new IOException("Font is not in TTF format, was " + s);
                    }

                    FreeTypeUtil.assertError(FreeType.FT_Select_Charmap(ft_face, FreeType.FT_ENCODING_UNICODE), "Find unicode charmap");
                    truetypeglyphprovider = new TrueTypeGlyphProvider(
                        bytebuffer, ft_face, this.size, this.oversample, this.shift.x, this.shift.y, this.skip
                    );
                }
            }

            return truetypeglyphprovider;
        }
        catch (Exception exception)
        {
            synchronized (FreeTypeUtil.LIBRARY_LOCK)
            {
                if (ft_face != null)
                {
                    FreeType.FT_Done_Face(ft_face);
                }
            }

            MemoryUtil.memFree(bytebuffer);
            throw exception;
        }
    }

    public static record Shift(float x, float y)
    {
        public static final TrueTypeGlyphProviderDefinition.Shift NONE = new TrueTypeGlyphProviderDefinition.Shift(0.0F, 0.0F);
        public static final Codec<TrueTypeGlyphProviderDefinition.Shift> CODEC = Codec.floatRange(-512.0F, 512.0F)
                .listOf()
                .comapFlatMap(
                    p_325361_ -> Util.fixedSize((List<Float>)p_325361_, 2)
                    .map(p_286746_ -> new TrueTypeGlyphProviderDefinition.Shift(p_286746_.get(0), p_286746_.get(1))),
                    p_286274_ -> List.of(p_286274_.x, p_286274_.y)
                );
    }
}
