package net.minecraft.client.renderer.texture.atlas;

import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public interface SpriteSource
{
    FileToIdConverter TEXTURE_ID_CONVERTER = new FileToIdConverter("textures", ".png");

    void run(ResourceManager p_261770_, SpriteSource.Output p_261757_);

    SpriteSourceType type();

    public interface Output
    {
    default void add(ResourceLocation p_261841_, Resource p_261651_)
        {
            this.add(p_261841_, p_296307_ -> p_296307_.loadSprite(p_261841_, p_261651_));
        }

        void add(ResourceLocation p_261821_, SpriteSource.SpriteSupplier p_261760_);

        void removeAll(Predicate<ResourceLocation> p_261532_);
    }

    public interface SpriteSupplier extends Function<SpriteResourceLoader, SpriteContents>
    {
    default void discard()
        {
        }
    }
}
