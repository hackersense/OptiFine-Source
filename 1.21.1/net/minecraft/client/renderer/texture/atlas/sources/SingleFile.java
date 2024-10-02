package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

public class SingleFile implements SpriteSource
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<SingleFile> CODEC = RecordCodecBuilder.mapCodec(
                instanceIn -> instanceIn.group(
                    ResourceLocation.CODEC.fieldOf("resource").forGetter(fileIn -> fileIn.resourceId),
                    ResourceLocation.CODEC.optionalFieldOf("sprite").forGetter(fileIn -> fileIn.spriteId)
                )
                .apply(instanceIn, SingleFile::new)
            );
    private final ResourceLocation resourceId;
    private final Optional<ResourceLocation> spriteId;

    public SingleFile(ResourceLocation p_261658_, Optional<ResourceLocation> p_261712_)
    {
        this.resourceId = p_261658_;
        this.spriteId = p_261712_;
    }

    @Override
    public void run(ResourceManager p_261920_, SpriteSource.Output p_261578_)
    {
        ResourceLocation resourcelocation = TEXTURE_ID_CONVERTER.idToFile(this.resourceId);

        if (TextureAtlas.isAbsoluteLocation(this.resourceId))
        {
            resourcelocation = new ResourceLocation(this.resourceId.getNamespace(), this.resourceId.getPath() + ".png");
        }

        Optional<Resource> optional = p_261920_.getResource(resourcelocation);

        if (optional.isPresent())
        {
            p_261578_.add(this.spriteId.orElse(this.resourceId), optional.get());
        }
        else
        {
            LOGGER.warn("Missing sprite: {}", resourcelocation);
        }
    }

    @Override
    public SpriteSourceType type()
    {
        return SpriteSources.SINGLE_FILE;
    }
}
