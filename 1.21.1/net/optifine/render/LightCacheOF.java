package net.optifine.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.optifine.override.ChunkCacheOF;

public class LightCacheOF
{
    public static final float getBrightness(BlockState blockStateIn, BlockAndTintGetter worldIn, BlockPos blockPosIn)
    {
        float f = getAoLightRaw(blockStateIn, worldIn, blockPosIn);
        return ModelBlockRenderer.fixAoLightValue(f);
    }

    private static float getAoLightRaw(BlockState blockStateIn, BlockAndTintGetter worldIn, BlockPos blockPosIn)
    {
        if (blockStateIn.getBlock() == Blocks.MOVING_PISTON)
        {
            return 1.0F;
        }
        else
        {
            return blockStateIn.getBlock() == Blocks.SCAFFOLDING ? 1.0F : blockStateIn.getShadeBrightness(worldIn, blockPosIn);
        }
    }

    public static final int getPackedLight(BlockState blockStateIn, BlockAndTintGetter worldIn, BlockPos blockPosIn)
    {
        return worldIn instanceof ChunkCacheOF chunkcacheof
               ? chunkcacheof.getCombinedLight(blockStateIn, worldIn, blockPosIn)
               : getPackedLightRaw(worldIn, blockStateIn, blockPosIn);
    }

    public static int getPackedLightRaw(BlockAndTintGetter worldIn, BlockState blockStateIn, BlockPos blockPosIn)
    {
        return LevelRenderer.getLightColor(worldIn, blockStateIn, blockPosIn);
    }
}
