package net.minecraft.client.renderer;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.optifine.Config;
import net.optifine.render.ClearVertexBuffersTask;
import net.optifine.render.VboRegion;

public class ViewArea
{
    protected final LevelRenderer levelRenderer;
    protected final Level level;
    protected int sectionGridSizeY;
    protected int sectionGridSizeX;
    protected int sectionGridSizeZ;
    private int viewDistance;
    public SectionRenderDispatcher.RenderSection[] sections;
    private Map<ChunkPos, VboRegion[]> mapVboRegions = new HashMap<>();
    private int lastCleanIndex = 0;

    public ViewArea(SectionRenderDispatcher p_298339_, Level p_110846_, int p_110847_, LevelRenderer p_110848_)
    {
        this.levelRenderer = p_110848_;
        this.level = p_110846_;
        this.setViewDistance(p_110847_);
        this.createSections(p_298339_);
    }

    protected void createSections(SectionRenderDispatcher p_299921_)
    {
        if (!Minecraft.getInstance().isSameThread())
        {
            throw new IllegalStateException("createSections called from wrong thread: " + Thread.currentThread().getName());
        }
        else
        {
            int i = this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ;
            this.sections = new SectionRenderDispatcher.RenderSection[i];
            int j = this.level.getMinBuildHeight();

            for (int k = 0; k < this.sectionGridSizeX; k++)
            {
                for (int l = 0; l < this.sectionGridSizeY; l++)
                {
                    for (int i1 = 0; i1 < this.sectionGridSizeZ; i1++)
                    {
                        int j1 = this.getSectionIndex(k, l, i1);
                        this.sections[j1] = p_299921_.new RenderSection(j1, k * 16, this.level.getMinBuildHeight() + l * 16, i1 * 16);
                        this.sections[j1].setOrigin(k * 16, l * 16 + j, i1 * 16);

                        if (Config.isVbo() && Config.isRenderRegions())
                        {
                            this.updateVboRegion(this.sections[j1]);
                        }
                    }
                }
            }

            for (int k1 = 0; k1 < this.sections.length; k1++)
            {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.sections[k1];

                for (int l1 = 0; l1 < Direction.VALUES.length; l1++)
                {
                    Direction direction = Direction.VALUES[l1];
                    BlockPos blockpos = sectionrenderdispatcher$rendersection1.getRelativeOrigin(direction);
                    SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.getRenderSectionAt(blockpos);
                    sectionrenderdispatcher$rendersection1.setRenderChunkNeighbour(direction, sectionrenderdispatcher$rendersection);
                }
            }
        }
    }

    public void releaseAllBuffers()
    {
        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.sections)
        {
            sectionrenderdispatcher$rendersection.releaseBuffers();
        }

        this.deleteVboRegions();
    }

    private int getSectionIndex(int p_297902_, int p_298060_, int p_297930_)
    {
        return (p_297930_ * this.sectionGridSizeY + p_298060_) * this.sectionGridSizeX + p_297902_;
    }

    protected void setViewDistance(int p_110854_)
    {
        int i = p_110854_ * 2 + 1;
        this.sectionGridSizeX = i;
        this.sectionGridSizeY = this.level.getSectionsCount();
        this.sectionGridSizeZ = i;
        this.viewDistance = p_110854_;
    }

    public int getViewDistance()
    {
        return this.viewDistance;
    }

    public LevelHeightAccessor getLevelHeightAccessor()
    {
        return this.level;
    }

    public void repositionCamera(double p_110851_, double p_110852_)
    {
        int i = Mth.ceil(p_110851_);
        int j = Mth.ceil(p_110852_);

        for (int k = 0; k < this.sectionGridSizeX; k++)
        {
            int l = this.sectionGridSizeX * 16;
            int i1 = i - 7 - l / 2;
            int j1 = i1 + Math.floorMod(k * 16 - i1, l);

            for (int k1 = 0; k1 < this.sectionGridSizeZ; k1++)
            {
                int l1 = this.sectionGridSizeZ * 16;
                int i2 = j - 7 - l1 / 2;
                int j2 = i2 + Math.floorMod(k1 * 16 - i2, l1);

                for (int k2 = 0; k2 < this.sectionGridSizeY; k2++)
                {
                    int l2 = this.level.getMinBuildHeight() + k2 * 16;
                    SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.sections[this.getSectionIndex(k, k2, k1)];
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getOrigin();

                    if (j1 != blockpos.getX() || l2 != blockpos.getY() || j2 != blockpos.getZ())
                    {
                        sectionrenderdispatcher$rendersection.setOrigin(j1, l2, j2);
                    }
                }
            }
        }
    }

    public void setDirty(int p_110860_, int p_110861_, int p_110862_, boolean p_110863_)
    {
        int i = Math.floorMod(p_110860_, this.sectionGridSizeX);
        int j = Math.floorMod(p_110861_ - this.level.getMinSection(), this.sectionGridSizeY);
        int k = Math.floorMod(p_110862_, this.sectionGridSizeZ);
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.sections[this.getSectionIndex(i, j, k)];
        sectionrenderdispatcher$rendersection.setDirty(p_110863_);
    }

    @Nullable
    public SectionRenderDispatcher.RenderSection getRenderSectionAt(BlockPos p_299271_)
    {
        int i = p_299271_.getY() - this.level.getMinBuildHeight() >> 4;

        if (i >= 0 && i < this.sectionGridSizeY)
        {
            int j = Mth.positiveModulo(p_299271_.getX() >> 4, this.sectionGridSizeX);
            int k = Mth.positiveModulo(p_299271_.getZ() >> 4, this.sectionGridSizeZ);
            return this.sections[this.getSectionIndex(j, i, k)];
        }
        else
        {
            return null;
        }
    }

    private void updateVboRegion(SectionRenderDispatcher.RenderSection renderChunk)
    {
        BlockPos blockpos = renderChunk.getOrigin();
        int i = blockpos.getX() >> 8 << 8;
        int j = blockpos.getZ() >> 8 << 8;
        ChunkPos chunkpos = new ChunkPos(i, j);
        RenderType[] arendertype = RenderType.CHUNK_RENDER_TYPES;
        VboRegion[] avboregion = this.mapVboRegions.get(chunkpos);

        if (avboregion == null)
        {
            avboregion = new VboRegion[arendertype.length];

            for (int k = 0; k < arendertype.length; k++)
            {
                if (!arendertype[k].isNeedsSorting())
                {
                    avboregion[k] = new VboRegion(arendertype[k]);
                }
            }

            this.mapVboRegions.put(chunkpos, avboregion);
        }

        for (int l = 0; l < arendertype.length; l++)
        {
            RenderType rendertype = arendertype[l];
            VboRegion vboregion = avboregion[l];
            renderChunk.getBuffer(rendertype).setVboRegion(vboregion);
        }
    }

    public void deleteVboRegions()
    {
        for (ChunkPos chunkpos : this.mapVboRegions.keySet())
        {
            VboRegion[] avboregion = this.mapVboRegions.get(chunkpos);

            for (int i = 0; i < avboregion.length; i++)
            {
                VboRegion vboregion = avboregion[i];

                if (vboregion != null)
                {
                    vboregion.deleteGlBuffers();
                }

                avboregion[i] = null;
            }
        }

        this.mapVboRegions.clear();
    }

    public int getHighestUsedChunkIndex(int chunkX, int minChunkIndex, int chunkZ)
    {
        chunkX = Mth.positiveModulo(chunkX, this.sectionGridSizeX);
        minChunkIndex = Mth.clamp(minChunkIndex, 0, this.sectionGridSizeY);
        chunkZ = Mth.positiveModulo(chunkZ, this.sectionGridSizeZ);

        for (int i = this.sectionGridSizeY - 1; i >= minChunkIndex; i--)
        {
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.sections[this.getSectionIndex(chunkX, i, chunkZ)];

            if (!sectionrenderdispatcher$rendersection.getCompiled().hasNoRenderableLayers())
            {
                return i;
            }
        }

        return -1;
    }

    public void clearUnusedVbos()
    {
        int i = Config.limit(Config.getFpsAverage(), 1, 1000);
        int j = Config.limit(this.sections.length / (10 * i), 3, 100);
        int k = Config.limit(j / 3, 1, 3);
        int l = 0;
        int i1 = Config.limit(this.lastCleanIndex, 0, this.sections.length - 1);

        for (int j1 = Math.min(i1 + j, this.sections.length); i1 < j1 && l < k; i1++)
        {
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.sections[i1];
            ClearVertexBuffersTask clearvertexbufferstask = ClearVertexBuffersTask.make(
                        sectionrenderdispatcher$rendersection.getCompiled().getLayersUsed(), sectionrenderdispatcher$rendersection
                    );

            if (clearvertexbufferstask != null)
            {
                Minecraft.getInstance().levelRenderer.getSectionRenderDispatcher().addUploadTask(clearvertexbufferstask);
                l++;
            }
        }

        if (i1 >= this.sections.length)
        {
            i1 = 0;
        }

        this.lastCleanIndex = i1;
    }
}
