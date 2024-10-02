package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CarpetBlock extends Block
{
    public static final MapCodec<CarpetBlock> CODEC = simpleCodec(CarpetBlock::new);
    protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    @Override
    public MapCodec <? extends CarpetBlock > codec()
    {
        return CODEC;
    }

    public CarpetBlock(BlockBehaviour.Properties p_152915_)
    {
        super(p_152915_);
    }

    @Override
    protected VoxelShape getShape(BlockState p_152917_, BlockGetter p_152918_, BlockPos p_152919_, CollisionContext p_152920_)
    {
        return SHAPE;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_152926_, Direction p_152927_, BlockState p_152928_, LevelAccessor p_152929_, BlockPos p_152930_, BlockPos p_152931_
    )
    {
        return !p_152926_.canSurvive(p_152929_, p_152930_)
               ? Blocks.AIR.defaultBlockState()
               : super.updateShape(p_152926_, p_152927_, p_152928_, p_152929_, p_152930_, p_152931_);
    }

    @Override
    protected boolean canSurvive(BlockState p_152922_, LevelReader p_152923_, BlockPos p_152924_)
    {
        return !p_152923_.isEmptyBlock(p_152924_.below());
    }
}
