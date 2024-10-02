package net.optifine.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.optifine.util.ArrayUtils;

public class SpriteRenderData
{
    private TextureAtlasSprite sprite;
    private int[] positions;
    private int[] counts;

    public SpriteRenderData(TextureAtlasSprite sprite, int[] positions, int[] counts)
    {
        this.sprite = sprite;
        this.positions = positions;
        this.counts = counts;

        if (positions.length != counts.length)
        {
            throw new IllegalArgumentException(positions.length + " != " + counts.length);
        }
    }

    public TextureAtlasSprite getSprite()
    {
        return this.sprite;
    }

    public int[] getPositions()
    {
        return this.positions;
    }

    public int[] getCounts()
    {
        return this.counts;
    }

    @Override
    public String toString()
    {
        return this.sprite.getName()
               + ", positions: ["
               + ArrayUtils.arrayToString(this.positions)
               + "], counts: ["
               + ArrayUtils.arrayToString(this.counts)
               + "]";
    }
}
