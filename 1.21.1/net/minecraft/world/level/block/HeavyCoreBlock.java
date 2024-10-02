package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HeavyCoreBlock extends Block implements SimpleWaterloggedBlock
{
    public static final MapCodec<HeavyCoreBlock> CODEC = simpleCodec(HeavyCoreBlock::new);
    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);

    public HeavyCoreBlock(BlockBehaviour.Properties p_329842_)
    {
        super(p_329842_);
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public MapCodec<HeavyCoreBlock> codec()
    {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_329416_)
    {
        p_329416_.add(BlockStateProperties.WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_328906_, Direction p_334684_, BlockState p_330097_, LevelAccessor p_336091_, BlockPos p_330469_, BlockPos p_331798_
    )
    {
        if (p_328906_.getValue(BlockStateProperties.WATERLOGGED))
        {
            p_336091_.scheduleTick(p_330469_, Fluids.WATER, Fluids.WATER.getTickDelay(p_336091_));
        }

        return super.updateShape(p_328906_, p_334684_, p_330097_, p_336091_, p_330469_, p_331798_);
    }

    @Override
    protected FluidState getFluidState(BlockState p_331726_)
    {
        return p_331726_.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_331726_);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_336134_)
    {
        FluidState fluidstate = p_336134_.getLevel().getFluidState(p_336134_.getClickedPos());
        return this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(fluidstate.is(Fluids.WATER)));
    }

    @Override
    protected VoxelShape getShape(BlockState p_328321_, BlockGetter p_329138_, BlockPos p_335327_, CollisionContext p_331838_)
    {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState p_335703_, PathComputationType p_334121_)
    {
        return false;
    }
}
