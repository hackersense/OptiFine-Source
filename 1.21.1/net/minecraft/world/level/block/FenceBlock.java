package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FenceBlock extends CrossCollisionBlock
{
    public static final MapCodec<FenceBlock> CODEC = simpleCodec(FenceBlock::new);
    private final VoxelShape[] occlusionByIndex;

    @Override
    public MapCodec<FenceBlock> codec()
    {
        return CODEC;
    }

    public FenceBlock(BlockBehaviour.Properties p_53302_)
    {
        super(2.0F, 2.0F, 16.0F, 16.0F, 24.0F, p_53302_);
        this.registerDefaultState(
            this.stateDefinition
            .any()
            .setValue(NORTH, Boolean.valueOf(false))
            .setValue(EAST, Boolean.valueOf(false))
            .setValue(SOUTH, Boolean.valueOf(false))
            .setValue(WEST, Boolean.valueOf(false))
            .setValue(WATERLOGGED, Boolean.valueOf(false))
        );
        this.occlusionByIndex = this.makeShapes(2.0F, 1.0F, 16.0F, 6.0F, 15.0F);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState p_53338_, BlockGetter p_53339_, BlockPos p_53340_)
    {
        return this.occlusionByIndex[this.getAABBIndex(p_53338_)];
    }

    @Override
    protected VoxelShape getVisualShape(BlockState p_53311_, BlockGetter p_53312_, BlockPos p_53313_, CollisionContext p_53314_)
    {
        return this.getShape(p_53311_, p_53312_, p_53313_, p_53314_);
    }

    @Override
    protected boolean isPathfindable(BlockState p_53306_, PathComputationType p_53309_)
    {
        return false;
    }

    public boolean connectsTo(BlockState p_53330_, boolean p_53331_, Direction p_53332_)
    {
        Block block = p_53330_.getBlock();
        boolean flag = this.isSameFence(p_53330_);
        boolean flag1 = block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(p_53330_, p_53332_);
        return !isExceptionForConnection(p_53330_) && p_53331_ || flag || flag1;
    }

    private boolean isSameFence(BlockState p_153255_)
    {
        return p_153255_.is(BlockTags.FENCES) && p_153255_.is(BlockTags.WOODEN_FENCES) == this.defaultBlockState().is(BlockTags.WOODEN_FENCES);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack p_336086_, BlockState p_332319_, Level p_336345_, BlockPos p_335994_, Player p_335277_, InteractionHand p_328795_, BlockHitResult p_333676_
    )
    {
        if (p_336345_.isClientSide)
        {
            return p_336086_.is(Items.LEAD) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        else
        {
            return super.useItemOn(p_336086_, p_332319_, p_336345_, p_335994_, p_335277_, p_328795_, p_333676_);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_328142_, Level p_333097_, BlockPos p_335860_, Player p_334259_, BlockHitResult p_333666_)
    {
        return !p_333097_.isClientSide() ? LeadItem.bindPlayerMobs(p_334259_, p_333097_, p_335860_) : InteractionResult.PASS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_53304_)
    {
        BlockGetter blockgetter = p_53304_.getLevel();
        BlockPos blockpos = p_53304_.getClickedPos();
        FluidState fluidstate = p_53304_.getLevel().getFluidState(p_53304_.getClickedPos());
        BlockPos blockpos1 = blockpos.north();
        BlockPos blockpos2 = blockpos.east();
        BlockPos blockpos3 = blockpos.south();
        BlockPos blockpos4 = blockpos.west();
        BlockState blockstate = blockgetter.getBlockState(blockpos1);
        BlockState blockstate1 = blockgetter.getBlockState(blockpos2);
        BlockState blockstate2 = blockgetter.getBlockState(blockpos3);
        BlockState blockstate3 = blockgetter.getBlockState(blockpos4);
        return super.getStateForPlacement(p_53304_)
               .setValue(NORTH, Boolean.valueOf(this.connectsTo(blockstate, blockstate.isFaceSturdy(blockgetter, blockpos1, Direction.SOUTH), Direction.SOUTH)))
               .setValue(EAST, Boolean.valueOf(this.connectsTo(blockstate1, blockstate1.isFaceSturdy(blockgetter, blockpos2, Direction.WEST), Direction.WEST)))
               .setValue(SOUTH, Boolean.valueOf(this.connectsTo(blockstate2, blockstate2.isFaceSturdy(blockgetter, blockpos3, Direction.NORTH), Direction.NORTH)))
               .setValue(WEST, Boolean.valueOf(this.connectsTo(blockstate3, blockstate3.isFaceSturdy(blockgetter, blockpos4, Direction.EAST), Direction.EAST)))
               .setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
    }

    @Override
    protected BlockState updateShape(BlockState p_53323_, Direction p_53324_, BlockState p_53325_, LevelAccessor p_53326_, BlockPos p_53327_, BlockPos p_53328_)
    {
        if (p_53323_.getValue(WATERLOGGED))
        {
            p_53326_.scheduleTick(p_53327_, Fluids.WATER, Fluids.WATER.getTickDelay(p_53326_));
        }

        return p_53324_.getAxis().getPlane() == Direction.Plane.HORIZONTAL
               ? p_53323_.setValue(
                   PROPERTY_BY_DIRECTION.get(p_53324_),
                   Boolean.valueOf(this.connectsTo(p_53325_, p_53325_.isFaceSturdy(p_53326_, p_53328_, p_53324_.getOpposite()), p_53324_.getOpposite()))
               )
               : super.updateShape(p_53323_, p_53324_, p_53325_, p_53326_, p_53327_, p_53328_);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_53334_)
    {
        p_53334_.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
    }
}
