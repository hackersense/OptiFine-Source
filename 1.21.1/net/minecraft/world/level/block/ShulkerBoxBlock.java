package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShulkerBoxBlock extends BaseEntityBlock
{
    public static final MapCodec<ShulkerBoxBlock> CODEC = RecordCodecBuilder.mapCodec(
                p_309292_ -> p_309292_.group(DyeColor.CODEC.optionalFieldOf("color").forGetter(p_309293_ -> Optional.ofNullable(p_309293_.color)), propertiesCodec())
                .apply(p_309292_, (p_309290_, p_309291_) -> new ShulkerBoxBlock(p_309290_.orElse(null), p_309291_))
            );
    private static final Component UNKNOWN_CONTENTS = Component.translatable("container.shulkerBox.unknownContents");
    private static final float OPEN_AABB_SIZE = 1.0F;
    private static final VoxelShape UP_OPEN_AABB = Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape DOWN_OPEN_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
    private static final VoxelShape WES_OPEN_AABB = Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
    private static final VoxelShape EAST_OPEN_AABB = Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape NORTH_OPEN_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
    private static final VoxelShape SOUTH_OPEN_AABB = Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
    private static final Map<Direction, VoxelShape> OPEN_SHAPE_BY_DIRECTION = Util.make(Maps.newEnumMap(Direction.class), p_258974_ ->
    {
        p_258974_.put(Direction.NORTH, NORTH_OPEN_AABB);
        p_258974_.put(Direction.EAST, EAST_OPEN_AABB);
        p_258974_.put(Direction.SOUTH, SOUTH_OPEN_AABB);
        p_258974_.put(Direction.WEST, WES_OPEN_AABB);
        p_258974_.put(Direction.UP, UP_OPEN_AABB);
        p_258974_.put(Direction.DOWN, DOWN_OPEN_AABB);
    });
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    public static final ResourceLocation CONTENTS = ResourceLocation.withDefaultNamespace("contents");
    @Nullable
    private final DyeColor color;

    @Override
    public MapCodec<ShulkerBoxBlock> codec()
    {
        return CODEC;
    }

    public ShulkerBoxBlock(@Nullable DyeColor p_56188_, BlockBehaviour.Properties p_56189_)
    {
        super(p_56189_);
        this.color = p_56188_;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_154552_, BlockState p_154553_)
    {
        return new ShulkerBoxBlockEntity(this.color, p_154552_, p_154553_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_154543_, BlockState p_154544_, BlockEntityType<T> p_154545_)
    {
        return createTickerHelper(p_154545_, BlockEntityType.SHULKER_BOX, ShulkerBoxBlockEntity::tick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState p_56255_)
    {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_56227_, Level p_56228_, BlockPos p_56229_, Player p_56230_, BlockHitResult p_56232_)
    {
        if (p_56228_.isClientSide)
        {
            return InteractionResult.SUCCESS;
        }
        else if (p_56230_.isSpectator())
        {
            return InteractionResult.CONSUME;
        }
        else if (p_56228_.getBlockEntity(p_56229_) instanceof ShulkerBoxBlockEntity shulkerboxblockentity)
        {
            if (canOpen(p_56227_, p_56228_, p_56229_, shulkerboxblockentity))
            {
                p_56230_.openMenu(shulkerboxblockentity);
                p_56230_.awardStat(Stats.OPEN_SHULKER_BOX);
                PiglinAi.angerNearbyPiglins(p_56230_, true);
            }

            return InteractionResult.CONSUME;
        }
        else
        {
            return InteractionResult.PASS;
        }
    }

    private static boolean canOpen(BlockState p_154547_, Level p_154548_, BlockPos p_154549_, ShulkerBoxBlockEntity p_154550_)
    {
        if (p_154550_.getAnimationStatus() != ShulkerBoxBlockEntity.AnimationStatus.CLOSED)
        {
            return true;
        }
        else
        {
            AABB aabb = Shulker.getProgressDeltaAabb(1.0F, p_154547_.getValue(FACING), 0.0F, 0.5F).move(p_154549_).deflate(1.0E-6);
            return p_154548_.noCollision(aabb);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_56198_)
    {
        return this.defaultBlockState().setValue(FACING, p_56198_.getClickedFace());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_56249_)
    {
        p_56249_.add(FACING);
    }

    @Override
    public BlockState playerWillDestroy(Level p_56212_, BlockPos p_56213_, BlockState p_56214_, Player p_56215_)
    {
        BlockEntity blockentity = p_56212_.getBlockEntity(p_56213_);

        if (blockentity instanceof ShulkerBoxBlockEntity shulkerboxblockentity)
        {
            if (!p_56212_.isClientSide && p_56215_.isCreative() && !shulkerboxblockentity.isEmpty())
            {
                ItemStack itemstack = getColoredItemStack(this.getColor());
                itemstack.applyComponents(blockentity.collectComponents());
                ItemEntity itementity = new ItemEntity(
                    p_56212_, (double)p_56213_.getX() + 0.5, (double)p_56213_.getY() + 0.5, (double)p_56213_.getZ() + 0.5, itemstack
                );
                itementity.setDefaultPickUpDelay();
                p_56212_.addFreshEntity(itementity);
            }
            else
            {
                shulkerboxblockentity.unpackLootTable(p_56215_);
            }
        }

        return super.playerWillDestroy(p_56212_, p_56213_, p_56214_, p_56215_);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState p_287632_, LootParams.Builder p_287691_)
    {
        BlockEntity blockentity = p_287691_.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (blockentity instanceof ShulkerBoxBlockEntity shulkerboxblockentity)
        {
            p_287691_ = p_287691_.withDynamicDrop(CONTENTS, p_56219_ ->
            {
                for (int i = 0; i < shulkerboxblockentity.getContainerSize(); i++)
                {
                    p_56219_.accept(shulkerboxblockentity.getItem(i));
                }
            });
        }

        return super.getDrops(p_287632_, p_287691_);
    }

    @Override
    protected void onRemove(BlockState p_56234_, Level p_56235_, BlockPos p_56236_, BlockState p_56237_, boolean p_56238_)
    {
        if (!p_56234_.is(p_56237_.getBlock()))
        {
            BlockEntity blockentity = p_56235_.getBlockEntity(p_56236_);
            super.onRemove(p_56234_, p_56235_, p_56236_, p_56237_, p_56238_);

            if (blockentity instanceof ShulkerBoxBlockEntity)
            {
                p_56235_.updateNeighbourForOutputSignal(p_56236_, p_56234_.getBlock());
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack p_56193_, Item.TooltipContext p_334789_, List<Component> p_56195_, TooltipFlag p_56196_)
    {
        super.appendHoverText(p_56193_, p_334789_, p_56195_, p_56196_);

        if (p_56193_.has(DataComponents.CONTAINER_LOOT))
        {
            p_56195_.add(UNKNOWN_CONTENTS);
        }

        int i = 0;
        int j = 0;

        for (ItemStack itemstack : p_56193_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems())
        {
            j++;

            if (i <= 4)
            {
                i++;
                p_56195_.add(Component.translatable("container.shulkerBox.itemCount", itemstack.getHoverName(), itemstack.getCount()));
            }
        }

        if (j - i > 0)
        {
            p_56195_.add(Component.translatable("container.shulkerBox.more", j - i).withStyle(ChatFormatting.ITALIC));
        }
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState p_259177_, BlockGetter p_260305_, BlockPos p_259168_)
    {
        if (p_260305_.getBlockEntity(p_259168_) instanceof ShulkerBoxBlockEntity shulkerboxblockentity && !shulkerboxblockentity.isClosed())
        {
            return OPEN_SHAPE_BY_DIRECTION.get(p_259177_.getValue(FACING).getOpposite());
        }

        return Shapes.block();
    }

    @Override
    protected VoxelShape getShape(BlockState p_56257_, BlockGetter p_56258_, BlockPos p_56259_, CollisionContext p_56260_)
    {
        BlockEntity blockentity = p_56258_.getBlockEntity(p_56259_);
        return blockentity instanceof ShulkerBoxBlockEntity ? Shapes.create(((ShulkerBoxBlockEntity)blockentity).getBoundingBox(p_56257_)) : Shapes.block();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState p_330948_, BlockGetter p_331679_, BlockPos p_328038_)
    {
        return false;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState p_56221_)
    {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_56223_, Level p_56224_, BlockPos p_56225_)
    {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(p_56224_.getBlockEntity(p_56225_));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader p_313060_, BlockPos p_56203_, BlockState p_56204_)
    {
        ItemStack itemstack = super.getCloneItemStack(p_313060_, p_56203_, p_56204_);
        p_313060_.getBlockEntity(p_56203_, BlockEntityType.SHULKER_BOX).ifPresent(p_327266_ -> p_327266_.saveToItem(itemstack, p_313060_.registryAccess()));
        return itemstack;
    }

    @Nullable
    public static DyeColor getColorFromItem(Item p_56253_)
    {
        return getColorFromBlock(Block.byItem(p_56253_));
    }

    @Nullable
    public static DyeColor getColorFromBlock(Block p_56263_)
    {
        return p_56263_ instanceof ShulkerBoxBlock ? ((ShulkerBoxBlock)p_56263_).getColor() : null;
    }

    public static Block getBlockByColor(@Nullable DyeColor p_56191_)
    {
        if (p_56191_ == null)
        {
            return Blocks.SHULKER_BOX;
        }
        else
        {

            return switch (p_56191_)
            {
                case WHITE -> Blocks.WHITE_SHULKER_BOX;

                case ORANGE -> Blocks.ORANGE_SHULKER_BOX;

                case MAGENTA -> Blocks.MAGENTA_SHULKER_BOX;

                case LIGHT_BLUE -> Blocks.LIGHT_BLUE_SHULKER_BOX;

                case YELLOW -> Blocks.YELLOW_SHULKER_BOX;

                case LIME -> Blocks.LIME_SHULKER_BOX;

                case PINK -> Blocks.PINK_SHULKER_BOX;

                case GRAY -> Blocks.GRAY_SHULKER_BOX;

                case LIGHT_GRAY -> Blocks.LIGHT_GRAY_SHULKER_BOX;

                case CYAN -> Blocks.CYAN_SHULKER_BOX;

                case BLUE -> Blocks.BLUE_SHULKER_BOX;

                case BROWN -> Blocks.BROWN_SHULKER_BOX;

                case GREEN -> Blocks.GREEN_SHULKER_BOX;

                case RED -> Blocks.RED_SHULKER_BOX;

                case BLACK -> Blocks.BLACK_SHULKER_BOX;

                case PURPLE -> Blocks.PURPLE_SHULKER_BOX;
            };
        }
    }

    @Nullable
    public DyeColor getColor()
    {
        return this.color;
    }

    public static ItemStack getColoredItemStack(@Nullable DyeColor p_56251_)
    {
        return new ItemStack(getBlockByColor(p_56251_));
    }

    @Override
    protected BlockState rotate(BlockState p_56243_, Rotation p_56244_)
    {
        return p_56243_.setValue(FACING, p_56244_.rotate(p_56243_.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState p_56240_, Mirror p_56241_)
    {
        return p_56240_.rotate(p_56241_.getRotation(p_56240_.getValue(FACING)));
    }
}
