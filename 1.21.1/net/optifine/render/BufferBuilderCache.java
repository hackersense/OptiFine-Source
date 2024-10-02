package net.optifine.render;

import java.util.BitSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.optifine.shaders.SVertexBuilder;
import org.joml.Vector3f;

public class BufferBuilderCache
{
    private TextureAtlasSprite[] quadSprites = new TextureAtlasSprite[16];
    private MultiTextureBuilder multiTextureBuilder = new MultiTextureBuilder();
    private SVertexBuilder sVertexBuilder = new SVertexBuilder();
    private RenderEnv renderEnv = new RenderEnv(null, null);
    private BitSet animatedSprites = new BitSet();
    protected Vector3f tempVec3f = new Vector3f();
    protected float[] tempFloat4 = new float[4];
    protected int[] tempInt4 = new int[4];
    protected Vector3f midBlock = new Vector3f();

    public TextureAtlasSprite[] getQuadSprites()
    {
        return this.quadSprites;
    }

    public void setQuadSprites(TextureAtlasSprite[] quadSprites)
    {
        if (this.quadSprites == null || quadSprites == null || this.quadSprites.length < quadSprites.length)
        {
            this.quadSprites = quadSprites;
        }
    }

    public MultiTextureBuilder getMultiTextureBuilder()
    {
        return this.multiTextureBuilder;
    }

    public SVertexBuilder getSVertexBuilder()
    {
        return this.sVertexBuilder;
    }

    public RenderEnv getRenderEnv()
    {
        return this.renderEnv;
    }

    public BitSet getAnimatedSprites()
    {
        return this.animatedSprites;
    }

    public Vector3f getTempVec3f()
    {
        return this.tempVec3f;
    }

    public float[] getTempFloat4()
    {
        return this.tempFloat4;
    }

    public int[] getTempInt4()
    {
        return this.tempInt4;
    }

    public Vector3f getMidBlock()
    {
        return this.midBlock;
    }
}
