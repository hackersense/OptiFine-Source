package net.minecraft.client.gui.font.providers;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.resources.ResourceLocation;

public record ProviderReferenceDefinition(ResourceLocation id) implements GlyphProviderDefinition
{
    public static final MapCodec<ProviderReferenceDefinition> CODEC = RecordCodecBuilder.mapCodec(
        p_286521_ -> p_286521_.group(ResourceLocation.CODEC.fieldOf("id").forGetter(ProviderReferenceDefinition::id))
        .apply(p_286521_, ProviderReferenceDefinition::new)
    );

    @Override
    public GlyphProviderType type()
    {
        return GlyphProviderType.REFERENCE;
    }

    @Override
    public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack()
    {
        return Either.right(new GlyphProviderDefinition.Reference(this.id));
    }
}
