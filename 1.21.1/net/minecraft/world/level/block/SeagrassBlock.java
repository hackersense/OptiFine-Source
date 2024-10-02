package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SeagrassBlock extends BushBlock implements BonemealableBlock, LiquidBlockContainer
{
    public static final MapCodec<SeagrassBlock> CODEC = simpleCodec(SeagrassBlock::new);
    protected static final float AABB_OFFSET = 6.0F;
    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    @Override
    public MapCodec<SeagrassBlock> codec()
    {
        return CODEC;
    }

    protected SeagrassBlock(BlockBehaviour.Properties p_154496_)
    {
        super(p_154496_);
    }

    @Override
    protected VoxelShape getShape(BlockState p_154525_, BlockGetter p_154526_, BlockPos p_154527_, CollisionContext p_154528_)
    {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState p_154539_, BlockGetter p_154540_, BlockPos p_154541_)
    {
        return p_154539_.isFaceSturdy(p_154540_, p_154541_, Direction.UP) && !p_154539_.is(Blocks.MAGMA_BLOCK);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_154503_)
    {
        FluidState fluidstate = p_154503_.getLevel().getFluidState(p_154503_.getClickedPos());
        return fluidstate.is(FluidTags.WATER) && fluidstate.getAmount() == 8 ? super.getStateForPlacement(p_154503_) : null;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_154530_, Direction p_154531_, BlockState p_154532_, LevelAccessor p_154533_, BlockPos p_154534_, BlockPos p_154535_
    )
    {
        BlockState blockstate = super.updateShape(p_154530_, p_154531_, p_154532_, p_154533_, p_154534_, p_154535_);

        if (!blockstate.isAir())
        {
            p_154533_.scheduleTick(p_154534_, Fluids.WATER, Fluids.WATER.getTickDelay(p_154533_));
        }

        return blockstate;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_298898_, BlockPos p_154506_, BlockState p_154507_)
    {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level p_222428_, RandomSource p_222429_, BlockPos p_222430_, BlockState p_222431_)
    {
        return true;
    }

    @Override
    protected FluidState getFluidState(BlockState p_154537_)
    {
        return Fluids.WATER.getSource(false);
    }

    @Override
    public void performBonemeal(ServerLevel p_222423_, RandomSource p_222424_, BlockPos p_222425_, BlockState p_222426_)
    {
        BlockState blockstate = Blocks.TALL_SEAGRASS.defaultBlockState();
        BlockState blockstate1 = blockstate.setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER);
        BlockPos blockpos = p_222425_.above();

        if (p_222423_.getBlockState(blockpos).is(Blocks.WATER))
        {
            p_222423_.setBlock(p_222425_, blockstate, 2);
            p_222423_.setBlock(blockpos, blockstate1, 2);
        }
    }

    @Override
    public boolean canPlaceLiquid(@Nullable Player p_297962_, BlockGetter p_299850_, BlockPos p_154511_, BlockState p_154512_, Fluid p_299663_)
    {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor p_154520_, BlockPos p_154521_, BlockState p_154522_, FluidState p_154523_)
    {
        return false;
    }
}
