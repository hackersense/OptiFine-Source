package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;

public class CalibratedSculkSensorBlock extends SculkSensorBlock
{
    public static final MapCodec<CalibratedSculkSensorBlock> CODEC = simpleCodec(CalibratedSculkSensorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    @Override
    public MapCodec<CalibratedSculkSensorBlock> codec()
    {
        return CODEC;
    }

    public CalibratedSculkSensorBlock(BlockBehaviour.Properties p_277532_)
    {
        super(p_277532_);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_277925_, BlockState p_277938_)
    {
        return new CalibratedSculkSensorBlockEntity(p_277925_, p_277938_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_277645_, BlockState p_278033_, BlockEntityType<T> p_277641_)
    {
        return !p_277645_.isClientSide
               ? createTickerHelper(
                   p_277641_,
                   BlockEntityType.CALIBRATED_SCULK_SENSOR,
                   (p_341823_, p_341824_, p_341825_, p_341826_) -> VibrationSystem.Ticker.tick(p_341823_, p_341826_.getVibrationData(), p_341826_.getVibrationUser())
               )
               : null;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_277423_)
    {
        return super.getStateForPlacement(p_277423_).setValue(FACING, p_277423_.getHorizontalDirection());
    }

    @Override
    public int getSignal(BlockState p_277782_, BlockGetter p_277556_, BlockPos p_277903_, Direction p_278059_)
    {
        return p_278059_ != p_277782_.getValue(FACING) ? super.getSignal(p_277782_, p_277556_, p_277903_, p_278059_) : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_277652_)
    {
        super.createBlockStateDefinition(p_277652_);
        p_277652_.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState p_277545_, Rotation p_277482_)
    {
        return p_277545_.setValue(FACING, p_277482_.rotate(p_277545_.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState p_277615_, Mirror p_277916_)
    {
        return p_277615_.rotate(p_277916_.getRotation(p_277615_.getValue(FACING)));
    }

    @Override
    public int getActiveTicks()
    {
        return 10;
    }
}
