package net.minecraft.world.level.chunk;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

class LevelChunkSection$1BlockCounter implements PalettedContainer.CountConsumer<BlockState>
{
    public int nonEmptyBlockCount;
    public int tickingBlockCount;
    public int tickingFluidCount;

    LevelChunkSection$1BlockCounter(final LevelChunkSection p_204442_)
    {
    }

    public void accept(BlockState p_204444_, int p_204445_)
    {
        FluidState fluidstate = p_204444_.getFluidState();

        if (!p_204444_.isAir())
        {
            this.nonEmptyBlockCount += p_204445_;

            if (p_204444_.isRandomlyTicking())
            {
                this.tickingBlockCount += p_204445_;
            }
        }

        if (!fluidstate.isEmpty())
        {
            this.nonEmptyBlockCount += p_204445_;

            if (fluidstate.isRandomlyTicking())
            {
                this.tickingFluidCount += p_204445_;
            }
        }
    }
}
