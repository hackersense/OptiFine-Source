package net.minecraft.client.renderer.texture;

public interface SpriteTicker extends AutoCloseable
{
    void tickAndUpload(int p_248847_, int p_250486_);

    @Override
    void close();

default TextureAtlasSprite getSprite()
    {
        return null;
    }

default void setSprite(TextureAtlasSprite sprite)
    {
    }
}
