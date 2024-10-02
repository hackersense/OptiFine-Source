package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ConduitBlock extends BaseEntityBlock implements SimpleWaterloggedBlock
{
    public static final MapCodec<ConduitBlock> CODEC = simpleCodec(ConduitBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final int SIZE = 3;
    protected static final VoxelShape SHAPE = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);

    @Override
    public MapCodec<ConduitBlock> codec()
    {
        return CODEC;
    }

    public ConduitBlock(BlockBehaviour.Properties p_52094_)
    {
        super(p_52094_);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, Boolean.valueOf(true)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_52118_)
    {
        p_52118_.add(WATERLOGGED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_153098_, BlockState p_153099_)
    {
        return new ConduitBlockEntity(p_153098_, p_153099_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_153094_, BlockState p_153095_, BlockEntityType<T> p_153096_)
    {
        return createTickerHelper(p_153096_, BlockEntityType.CONDUIT, p_153094_.isClientSide ? ConduitBlockEntity::clientTick : ConduitBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState p_52120_)
    {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected FluidState getFluidState(BlockState p_52127_)
    {
        return p_52127_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_52127_);
    }

    @Override
    protected BlockState updateShape(BlockState p_52111_, Direction p_52112_, BlockState p_52113_, LevelAccessor p_52114_, BlockPos p_52115_, BlockPos p_52116_)
    {
        if (p_52111_.getValue(WATERLOGGED))
        {
            p_52114_.scheduleTick(p_52115_, Fluids.WATER, Fluids.WATER.getTickDelay(p_52114_));
        }

        return super.updateShape(p_52111_, p_52112_, p_52113_, p_52114_, p_52115_, p_52116_);
    }

    @Override
    protected VoxelShape getShape(BlockState p_52122_, BlockGetter p_52123_, BlockPos p_52124_, CollisionContext p_52125_)
    {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_52096_)
    {
        FluidState fluidstate = p_52096_.getLevel().getFluidState(p_52096_.getClickedPos());
        return this.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf(fluidstate.is(FluidTags.WATER) && fluidstate.getAmount() == 8));
    }

    @Override
    protected boolean isPathfindable(BlockState p_52106_, PathComputationType p_52109_)
    {
        return false;
    }
}
