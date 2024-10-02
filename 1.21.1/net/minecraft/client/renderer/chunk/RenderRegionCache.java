package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public class RenderRegionCache
{
    private final Long2ObjectMap<RenderRegionCache.ChunkInfo> chunkInfoCache = new Long2ObjectOpenHashMap<>();
    private AtomicInteger countCompileTasks = new AtomicInteger();

    @Nullable
    public RenderChunkRegion createRegion(Level p_200466_, SectionPos p_343190_)
    {
        RenderRegionCache.ChunkInfo renderregioncache$chunkinfo = this.getChunkInfo(p_200466_, p_343190_.x(), p_343190_.z());

        if (renderregioncache$chunkinfo.chunk().isSectionEmpty(p_343190_.y()))
        {
            return null;
        }
        else
        {
            int i = p_343190_.x() - 1;
            int j = p_343190_.z() - 1;
            int k = p_343190_.x() + 1;
            int l = p_343190_.z() + 1;
            RenderChunk[] arenderchunk = new RenderChunk[9];

            for (int i1 = j; i1 <= l; i1++)
            {
                for (int j1 = i; j1 <= k; j1++)
                {
                    int k1 = RenderChunkRegion.index(i, j, j1, i1);
                    RenderRegionCache.ChunkInfo renderregioncache$chunkinfo1 = j1 == p_343190_.x() && i1 == p_343190_.z()
                            ? renderregioncache$chunkinfo
                            : this.getChunkInfo(p_200466_, j1, i1);
                    arenderchunk[k1] = renderregioncache$chunkinfo1.renderChunk();
                }
            }

            return new RenderChunkRegion(p_200466_, i, j, arenderchunk, p_343190_);
        }
    }

    private RenderRegionCache.ChunkInfo getChunkInfo(Level p_344202_, int p_345234_, int p_342074_)
    {
        return this.chunkInfoCache
               .computeIfAbsent(
                   ChunkPos.asLong(p_345234_, p_342074_),
                   longPosIn -> new RenderRegionCache.ChunkInfo(p_344202_.getChunk(ChunkPos.getX(longPosIn), ChunkPos.getZ(longPosIn)))
               );
    }

    public void compileStarted()
    {
        this.countCompileTasks.incrementAndGet();
    }

    public void compileFinished()
    {
        int i = this.countCompileTasks.decrementAndGet();

        if (i <= 0)
        {
            this.finish();
        }
    }

    public int getCountCompileTasks()
    {
        return this.countCompileTasks.get();
    }

    private void finish()
    {
        for (RenderRegionCache.ChunkInfo renderregioncache$chunkinfo : this.chunkInfoCache.values())
        {
            if (renderregioncache$chunkinfo.renderChunk != null)
            {
                renderregioncache$chunkinfo.renderChunk.finish();
                renderregioncache$chunkinfo.renderChunk = null;
            }
        }
    }

    static final class ChunkInfo
    {
        private final LevelChunk chunk;
        @Nullable
        private RenderChunk renderChunk;

        ChunkInfo(LevelChunk p_200479_)
        {
            this.chunk = p_200479_;
        }

        public LevelChunk chunk()
        {
            return this.chunk;
        }

        public RenderChunk renderChunk()
        {
            if (this.renderChunk == null)
            {
                this.renderChunk = new RenderChunk(this.chunk);
            }

            return this.renderChunk;
        }
    }
}
