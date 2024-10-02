package net.optifine.override;

import java.util.Arrays;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.optifine.BlockPosM;
import net.optifine.render.LightCacheOF;
import net.optifine.render.RenderEnv;
import net.optifine.util.ArrayCache;

public class ChunkCacheOF implements BlockAndTintGetter
{
    private final RenderChunkRegion chunkCache;
    private final int posX;
    private final int posY;
    private final int posZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final int sizeXZ;
    private int[] combinedLights;
    private BlockState[] blockStates;
    private Biome[] biomes;
    private final int arraySize;
    private RenderEnv renderEnv;
    private static final ArrayCache cacheCombinedLights = new ArrayCache(int.class, 16);
    private static final ArrayCache cacheBlockStates = new ArrayCache(BlockState.class, 16);
    private static final ArrayCache cacheBiomes = new ArrayCache(Biome.class, 16);

    public ChunkCacheOF(RenderChunkRegion chunkCache, SectionPos sectionPos)
    {
        this.chunkCache = chunkCache;
        int i = sectionPos.getX() - 1;
        int j = sectionPos.getY() - 1;
        int k = sectionPos.getZ() - 1;
        int l = sectionPos.getX() + 1;
        int i1 = sectionPos.getY() + 1;
        int j1 = sectionPos.getZ() + 1;
        this.sizeX = l - i + 1 << 4;
        this.sizeY = i1 - j + 1 << 4;
        this.sizeZ = j1 - k + 1 << 4;
        this.sizeXZ = this.sizeX * this.sizeZ;
        this.arraySize = this.sizeX * this.sizeY * this.sizeZ;
        this.posX = i << 4;
        this.posY = j << 4;
        this.posZ = k << 4;
    }

    public int getPositionIndex(BlockPos pos)
    {
        int i = pos.getX() - this.posX;

        if (i >= 0 && i < this.sizeX)
        {
            int j = pos.getY() - this.posY;

            if (j >= 0 && j < this.sizeY)
            {
                int k = pos.getZ() - this.posZ;
                return k >= 0 && k < this.sizeZ ? j * this.sizeXZ + k * this.sizeX + i : -1;
            }
            else
            {
                return -1;
            }
        }
        else
        {
            return -1;
        }
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos)
    {
        return this.chunkCache.getBrightness(type, pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        int i = this.getPositionIndex(pos);

        if (i >= 0 && i < this.arraySize && this.blockStates != null)
        {
            BlockState blockstate = this.blockStates[i];

            if (blockstate == null)
            {
                blockstate = this.chunkCache.getBlockState(pos);
                this.blockStates[i] = blockstate;
            }

            return blockstate;
        }
        else
        {
            return this.chunkCache.getBlockState(pos);
        }
    }

    public void renderStart()
    {
        if (this.combinedLights == null)
        {
            this.combinedLights = (int[])cacheCombinedLights.allocate(this.arraySize);
        }

        if (this.blockStates == null)
        {
            this.blockStates = (BlockState[])cacheBlockStates.allocate(this.arraySize);
        }

        if (this.biomes == null)
        {
            this.biomes = (Biome[])cacheBiomes.allocate(this.arraySize);
        }

        Arrays.fill(this.combinedLights, -1);
        Arrays.fill(this.blockStates, null);
        Arrays.fill(this.biomes, null);
        this.loadBlockStates();
    }

    private void loadBlockStates()
    {
        if (this.sizeX == 48 && this.sizeY == 48 && this.sizeZ == 48)
        {
            LevelChunk levelchunk = this.chunkCache.getLevelChunk(SectionPos.blockToSectionCoord(this.posX) + 1, SectionPos.blockToSectionCoord(this.posZ) + 1);
            BlockPosM blockposm = new BlockPosM();

            for (int i = 16; i < 32; i++)
            {
                int j = i * this.sizeXZ;

                for (int k = 16; k < 32; k++)
                {
                    int l = k * this.sizeX;

                    for (int i1 = 16; i1 < 32; i1++)
                    {
                        blockposm.setXyz(this.posX + i1, this.posY + i, this.posZ + k);
                        int j1 = j + l + i1;
                        BlockState blockstate = levelchunk.getBlockState(blockposm);
                        this.blockStates[j1] = blockstate;
                    }
                }
            }
        }
    }

    public void renderFinish()
    {
        cacheCombinedLights.free(this.combinedLights);
        this.combinedLights = null;
        cacheBlockStates.free(this.blockStates);
        this.blockStates = null;
        cacheBiomes.free(this.biomes);
        this.biomes = null;
    }

    public int getCombinedLight(BlockState blockStateIn, BlockAndTintGetter worldIn, BlockPos blockPosIn)
    {
        int i = this.getPositionIndex(blockPosIn);

        if (i >= 0 && i < this.combinedLights.length && this.combinedLights != null)
        {
            int j = this.combinedLights[i];

            if (j == -1)
            {
                j = LightCacheOF.getPackedLightRaw(worldIn, blockStateIn, blockPosIn);
                this.combinedLights[i] = j;
            }

            return j;
        }
        else
        {
            return LightCacheOF.getPackedLightRaw(worldIn, blockStateIn, blockPosIn);
        }
    }

    public Biome getBiome(BlockPos pos)
    {
        int i = this.getPositionIndex(pos);

        if (i >= 0 && i < this.arraySize && this.biomes != null)
        {
            Biome biome = this.biomes[i];

            if (biome == null)
            {
                biome = this.chunkCache.getBiome(pos);
                this.biomes[i] = biome;
            }

            return biome;
        }
        else
        {
            return this.chunkCache.getBiome(pos);
        }
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos)
    {
        return this.chunkCache.getBlockEntity(pos);
    }

    @Override
    public boolean canSeeSky(BlockPos pos)
    {
        return this.chunkCache.canSeeSky(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos)
    {
        return this.getBlockState(pos).getFluidState();
    }

    @Override
    public int getBlockTint(BlockPos blockPosIn, ColorResolver colorResolverIn)
    {
        return this.chunkCache.getBlockTint(blockPosIn, colorResolverIn);
    }

    @Override
    public LevelLightEngine getLightEngine()
    {
        return this.chunkCache.getLightEngine();
    }

    public RenderEnv getRenderEnv()
    {
        return this.renderEnv;
    }

    public void setRenderEnv(RenderEnv renderEnv)
    {
        this.renderEnv = renderEnv;
    }

    @Override
    public float getShade(Direction directionIn, boolean shadeIn)
    {
        return this.chunkCache.getShade(directionIn, shadeIn);
    }

    @Override
    public int getHeight()
    {
        return this.chunkCache.getHeight();
    }

    @Override
    public int getMinBuildHeight()
    {
        return this.chunkCache.getMinBuildHeight();
    }
}
