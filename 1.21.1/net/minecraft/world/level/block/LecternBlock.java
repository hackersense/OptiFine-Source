package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LecternBlock extends BaseEntityBlock
{
    public static final MapCodec<LecternBlock> CODEC = simpleCodec(LecternBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty HAS_BOOK = BlockStateProperties.HAS_BOOK;
    public static final VoxelShape SHAPE_BASE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
    public static final VoxelShape SHAPE_POST = Block.box(4.0, 2.0, 4.0, 12.0, 14.0, 12.0);
    public static final VoxelShape SHAPE_COMMON = Shapes.or(SHAPE_BASE, SHAPE_POST);
    public static final VoxelShape SHAPE_TOP_PLATE = Block.box(0.0, 15.0, 0.0, 16.0, 15.0, 16.0);
    public static final VoxelShape SHAPE_COLLISION = Shapes.or(SHAPE_COMMON, SHAPE_TOP_PLATE);
    public static final VoxelShape SHAPE_WEST = Shapes.or(
                Block.box(1.0, 10.0, 0.0, 5.333333, 14.0, 16.0),
                Block.box(5.333333, 12.0, 0.0, 9.666667, 16.0, 16.0),
                Block.box(9.666667, 14.0, 0.0, 14.0, 18.0, 16.0),
                SHAPE_COMMON
            );
    public static final VoxelShape SHAPE_NORTH = Shapes.or(
                Block.box(0.0, 10.0, 1.0, 16.0, 14.0, 5.333333),
                Block.box(0.0, 12.0, 5.333333, 16.0, 16.0, 9.666667),
                Block.box(0.0, 14.0, 9.666667, 16.0, 18.0, 14.0),
                SHAPE_COMMON
            );
    public static final VoxelShape SHAPE_EAST = Shapes.or(
                Block.box(10.666667, 10.0, 0.0, 15.0, 14.0, 16.0),
                Block.box(6.333333, 12.0, 0.0, 10.666667, 16.0, 16.0),
                Block.box(2.0, 14.0, 0.0, 6.333333, 18.0, 16.0),
                SHAPE_COMMON
            );
    public static final VoxelShape SHAPE_SOUTH = Shapes.or(
                Block.box(0.0, 10.0, 10.666667, 16.0, 14.0, 15.0),
                Block.box(0.0, 12.0, 6.333333, 16.0, 16.0, 10.666667),
                Block.box(0.0, 14.0, 2.0, 16.0, 18.0, 6.333333),
                SHAPE_COMMON
            );
    private static final int PAGE_CHANGE_IMPULSE_TICKS = 2;

    @Override
    public MapCodec<LecternBlock> codec()
    {
        return CODEC;
    }

    protected LecternBlock(BlockBehaviour.Properties p_54479_)
    {
        super(p_54479_);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, Boolean.valueOf(false)).setValue(HAS_BOOK, Boolean.valueOf(false))
        );
    }

    @Override
    protected RenderShape getRenderShape(BlockState p_54559_)
    {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState p_54584_, BlockGetter p_54585_, BlockPos p_54586_)
    {
        return SHAPE_COMMON;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState p_54582_)
    {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_54481_)
    {
        Level level = p_54481_.getLevel();
        ItemStack itemstack = p_54481_.getItemInHand();
        Player player = p_54481_.getPlayer();
        boolean flag = false;

        if (!level.isClientSide && player != null && player.canUseGameMasterBlocks())
        {
            CustomData customdata = itemstack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);

            if (customdata.contains("Book"))
            {
                flag = true;
            }
        }

        return this.defaultBlockState().setValue(FACING, p_54481_.getHorizontalDirection().getOpposite()).setValue(HAS_BOOK, Boolean.valueOf(flag));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState p_54577_, BlockGetter p_54578_, BlockPos p_54579_, CollisionContext p_54580_)
    {
        return SHAPE_COLLISION;
    }

    @Override
    protected VoxelShape getShape(BlockState p_54561_, BlockGetter p_54562_, BlockPos p_54563_, CollisionContext p_54564_)
    {
        switch ((Direction)p_54561_.getValue(FACING))
        {
            case NORTH:
                return SHAPE_NORTH;

            case SOUTH:
                return SHAPE_SOUTH;

            case EAST:
                return SHAPE_EAST;

            case WEST:
                return SHAPE_WEST;

            default:
                return SHAPE_COMMON;
        }
    }

    @Override
    protected BlockState rotate(BlockState p_54540_, Rotation p_54541_)
    {
        return p_54540_.setValue(FACING, p_54541_.rotate(p_54540_.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState p_54537_, Mirror p_54538_)
    {
        return p_54537_.rotate(p_54538_.getRotation(p_54537_.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54543_)
    {
        p_54543_.add(FACING, POWERED, HAS_BOOK);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_153573_, BlockState p_153574_)
    {
        return new LecternBlockEntity(p_153573_, p_153574_);
    }

    public static boolean tryPlaceBook(@Nullable LivingEntity p_344930_, Level p_270604_, BlockPos p_270276_, BlockState p_270445_, ItemStack p_270458_)
    {
        if (!p_270445_.getValue(HAS_BOOK))
        {
            if (!p_270604_.isClientSide)
            {
                placeBook(p_344930_, p_270604_, p_270276_, p_270445_, p_270458_);
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    private static void placeBook(@Nullable LivingEntity p_343476_, Level p_270065_, BlockPos p_270155_, BlockState p_270753_, ItemStack p_270173_)
    {
        if (p_270065_.getBlockEntity(p_270155_) instanceof LecternBlockEntity lecternblockentity)
        {
            lecternblockentity.setBook(p_270173_.consumeAndReturn(1, p_343476_));
            resetBookState(p_343476_, p_270065_, p_270155_, p_270753_, true);
            p_270065_.playSound(null, p_270155_, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public static void resetBookState(@Nullable Entity p_270231_, Level p_270114_, BlockPos p_270251_, BlockState p_270758_, boolean p_270452_)
    {
        BlockState blockstate = p_270758_.setValue(POWERED, Boolean.valueOf(false)).setValue(HAS_BOOK, Boolean.valueOf(p_270452_));
        p_270114_.setBlock(p_270251_, blockstate, 3);
        p_270114_.gameEvent(GameEvent.BLOCK_CHANGE, p_270251_, GameEvent.Context.of(p_270231_, blockstate));
        updateBelow(p_270114_, p_270251_, p_270758_);
    }

    public static void signalPageChange(Level p_54489_, BlockPos p_54490_, BlockState p_54491_)
    {
        changePowered(p_54489_, p_54490_, p_54491_, true);
        p_54489_.scheduleTick(p_54490_, p_54491_.getBlock(), 2);
        p_54489_.levelEvent(1043, p_54490_, 0);
    }

    private static void changePowered(Level p_54554_, BlockPos p_54555_, BlockState p_54556_, boolean p_54557_)
    {
        p_54554_.setBlock(p_54555_, p_54556_.setValue(POWERED, Boolean.valueOf(p_54557_)), 3);
        updateBelow(p_54554_, p_54555_, p_54556_);
    }

    private static void updateBelow(Level p_54545_, BlockPos p_54546_, BlockState p_54547_)
    {
        p_54545_.updateNeighborsAt(p_54546_.below(), p_54547_.getBlock());
    }

    @Override
    protected void tick(BlockState p_221388_, ServerLevel p_221389_, BlockPos p_221390_, RandomSource p_221391_)
    {
        changePowered(p_221389_, p_221390_, p_221388_, false);
    }

    @Override
    protected void onRemove(BlockState p_54531_, Level p_54532_, BlockPos p_54533_, BlockState p_54534_, boolean p_54535_)
    {
        if (!p_54531_.is(p_54534_.getBlock()))
        {
            if (p_54531_.getValue(HAS_BOOK))
            {
                this.popBook(p_54531_, p_54532_, p_54533_);
            }

            super.onRemove(p_54531_, p_54532_, p_54533_, p_54534_, p_54535_);

            if (p_54531_.getValue(POWERED))
            {
                p_54532_.updateNeighborsAt(p_54533_.below(), this);
            }
        }
    }

    private void popBook(BlockState p_54588_, Level p_54589_, BlockPos p_54590_)
    {
        if (p_54589_.getBlockEntity(p_54590_) instanceof LecternBlockEntity lecternblockentity)
        {
            Direction direction = p_54588_.getValue(FACING);
            ItemStack itemstack = lecternblockentity.getBook().copy();
            float f = 0.25F * (float)direction.getStepX();
            float f1 = 0.25F * (float)direction.getStepZ();
            ItemEntity itementity = new ItemEntity(
                p_54589_,
                (double)p_54590_.getX() + 0.5 + (double)f,
                (double)(p_54590_.getY() + 1),
                (double)p_54590_.getZ() + 0.5 + (double)f1,
                itemstack
            );
            itementity.setDefaultPickUpDelay();
            p_54589_.addFreshEntity(itementity);
            lecternblockentity.clearContent();
        }
    }

    @Override
    protected boolean isSignalSource(BlockState p_54575_)
    {
        return true;
    }

    @Override
    protected int getSignal(BlockState p_54515_, BlockGetter p_54516_, BlockPos p_54517_, Direction p_54518_)
    {
        return p_54515_.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState p_54566_, BlockGetter p_54567_, BlockPos p_54568_, Direction p_54569_)
    {
        return p_54569_ == Direction.UP && p_54566_.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState p_54503_)
    {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_54520_, Level p_54521_, BlockPos p_54522_)
    {
        if (p_54520_.getValue(HAS_BOOK))
        {
            BlockEntity blockentity = p_54521_.getBlockEntity(p_54522_);

            if (blockentity instanceof LecternBlockEntity)
            {
                return ((LecternBlockEntity)blockentity).getRedstoneSignal();
            }
        }

        return 0;
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack p_333093_, BlockState p_335984_, Level p_334086_, BlockPos p_332284_, Player p_332545_, InteractionHand p_328802_, BlockHitResult p_328840_
    )
    {
        if (p_335984_.getValue(HAS_BOOK))
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        else if (p_333093_.is(ItemTags.LECTERN_BOOKS))
        {
            return tryPlaceBook(p_332545_, p_334086_, p_332284_, p_335984_, p_333093_)
                   ? ItemInteractionResult.sidedSuccess(p_334086_.isClientSide)
                   : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        else
        {
            return p_333093_.isEmpty() && p_328802_ == InteractionHand.MAIN_HAND
                   ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
                   : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_331321_, Level p_329665_, BlockPos p_335448_, Player p_333152_, BlockHitResult p_331406_)
    {
        if (p_331321_.getValue(HAS_BOOK))
        {
            if (!p_329665_.isClientSide)
            {
                this.openScreen(p_329665_, p_335448_, p_333152_);
            }

            return InteractionResult.sidedSuccess(p_329665_.isClientSide);
        }
        else
        {
            return InteractionResult.CONSUME;
        }
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState p_54571_, Level p_54572_, BlockPos p_54573_)
    {
        return !p_54571_.getValue(HAS_BOOK) ? null : super.getMenuProvider(p_54571_, p_54572_, p_54573_);
    }

    private void openScreen(Level p_54485_, BlockPos p_54486_, Player p_54487_)
    {
        BlockEntity blockentity = p_54485_.getBlockEntity(p_54486_);

        if (blockentity instanceof LecternBlockEntity)
        {
            p_54487_.openMenu((LecternBlockEntity)blockentity);
            p_54487_.awardStat(Stats.INTERACT_WITH_LECTERN);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState p_54510_, PathComputationType p_54513_)
    {
        return false;
    }
}
