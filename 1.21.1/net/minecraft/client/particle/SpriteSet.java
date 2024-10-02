package net.minecraft.client.particle;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

public interface SpriteSet
{
    TextureAtlasSprite get(int p_107966_, int p_107967_);

    TextureAtlasSprite get(RandomSource p_234102_);
}
