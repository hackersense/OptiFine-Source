package net.optifine.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.optifine.util.ArrayUtils;

public class MultiTextureData
{
    private SpriteRenderData[] spriteRenderDatas;
    private int vertexCount;
    private RenderType blockLayer;
    private TextureAtlasSprite[] quadSprites;
    private SpriteRenderData[] spriteRenderDatasSorted;

    public MultiTextureData(SpriteRenderData[] spriteRenderDatas)
    {
        this.spriteRenderDatas = spriteRenderDatas;
    }

    public SpriteRenderData[] getSpriteRenderDatas()
    {
        return this.spriteRenderDatas;
    }

    public void setResortParameters(int vertexCountIn, RenderType blockLayerIn, TextureAtlasSprite[] quadSpritesIn)
    {
        this.vertexCount = vertexCountIn;
        this.blockLayer = blockLayerIn;
        this.quadSprites = quadSpritesIn;
    }

    public void prepareSort(MultiTextureBuilder multiTextureBuilder, int[] quadOrdering)
    {
        this.spriteRenderDatasSorted = multiTextureBuilder.buildRenderDatas(this.vertexCount, this.blockLayer, this.quadSprites, quadOrdering);
    }

    public void applySort()
    {
        if (this.spriteRenderDatasSorted != null)
        {
            this.spriteRenderDatas = this.spriteRenderDatasSorted;
            this.spriteRenderDatasSorted = null;
        }
    }

    @Override
    public String toString()
    {
        return ArrayUtils.arrayToString((Object[])this.spriteRenderDatas);
    }
}
