package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class SignBlock extends BaseEntityBlock implements SimpleWaterloggedBlock
{
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    protected static final float AABB_OFFSET = 4.0F;
    protected static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private final WoodType type;

    protected SignBlock(WoodType p_56274_, BlockBehaviour.Properties p_56273_)
    {
        super(p_56273_);
        this.type = p_56274_;
    }

    @Override
    protected abstract MapCodec <? extends SignBlock > codec();

    @Override
    protected BlockState updateShape(BlockState p_56285_, Direction p_56286_, BlockState p_56287_, LevelAccessor p_56288_, BlockPos p_56289_, BlockPos p_56290_)
    {
        if (p_56285_.getValue(WATERLOGGED))
        {
            p_56288_.scheduleTick(p_56289_, Fluids.WATER, Fluids.WATER.getTickDelay(p_56288_));
        }

        return super.updateShape(p_56285_, p_56286_, p_56287_, p_56288_, p_56289_, p_56290_);
    }

    @Override
    protected VoxelShape getShape(BlockState p_56293_, BlockGetter p_56294_, BlockPos p_56295_, CollisionContext p_56296_)
    {
        return SHAPE;
    }

    @Override
    public boolean isPossibleToRespawnInThis(BlockState p_279137_)
    {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_154556_, BlockState p_154557_)
    {
        return new SignBlockEntity(p_154556_, p_154557_);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack p_333491_, BlockState p_331465_, Level p_334341_, BlockPos p_330848_, Player p_330127_, InteractionHand p_331896_, BlockHitResult p_335647_
    )
    {
        if (p_334341_.getBlockEntity(p_330848_) instanceof SignBlockEntity signblockentity)
        {
            SignApplicator signapplicator1 = p_333491_.getItem() instanceof SignApplicator signapplicator ? signapplicator : null;
            boolean flag = signapplicator1 != null && p_330127_.mayBuild();

            if (!p_334341_.isClientSide)
            {
                if (flag && !signblockentity.isWaxed() && !this.otherPlayerIsEditingSign(p_330127_, signblockentity))
                {
                    boolean flag1 = signblockentity.isFacingFrontText(p_330127_);

                    if (signapplicator1.canApplyToSign(signblockentity.getText(flag1), p_330127_)
                            && signapplicator1.tryApplyToSign(p_334341_, signblockentity, flag1, p_330127_))
                    {
                        signblockentity.executeClickCommandsIfPresent(p_330127_, p_334341_, p_330848_, flag1);
                        p_330127_.awardStat(Stats.ITEM_USED.get(p_333491_.getItem()));
                        p_334341_.gameEvent(GameEvent.BLOCK_CHANGE, signblockentity.getBlockPos(), GameEvent.Context.of(p_330127_, signblockentity.getBlockState()));
                        p_333491_.consume(1, p_330127_);
                        return ItemInteractionResult.SUCCESS;
                    }
                    else
                    {
                        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                    }
                }
                else
                {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
            }
            else
            {
                return !flag && !signblockentity.isWaxed() ? ItemInteractionResult.CONSUME : ItemInteractionResult.SUCCESS;
            }
        }
        else
        {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_333550_, Level p_334186_, BlockPos p_333719_, Player p_328842_, BlockHitResult p_335719_)
    {
        if (p_334186_.getBlockEntity(p_333719_) instanceof SignBlockEntity signblockentity)
        {
            if (p_334186_.isClientSide)
            {
                Util.pauseInIde(new IllegalStateException("Expected to only call this on server"));
            }

            boolean flag1 = signblockentity.isFacingFrontText(p_328842_);
            boolean flag = signblockentity.executeClickCommandsIfPresent(p_328842_, p_334186_, p_333719_, flag1);

            if (signblockentity.isWaxed())
            {
                p_334186_.playSound(null, signblockentity.getBlockPos(), signblockentity.getSignInteractionFailedSoundEvent(), SoundSource.BLOCKS);
                return InteractionResult.SUCCESS;
            }
            else if (flag)
            {
                return InteractionResult.SUCCESS;
            }
            else if (!this.otherPlayerIsEditingSign(p_328842_, signblockentity) && p_328842_.mayBuild() && this.hasEditableText(p_328842_, signblockentity, flag1))
            {
                this.openTextEdit(p_328842_, signblockentity, flag1);
                return InteractionResult.SUCCESS;
            }
            else
            {
                return InteractionResult.PASS;
            }
        }
        else
        {
            return InteractionResult.PASS;
        }
    }

    private boolean hasEditableText(Player p_279394_, SignBlockEntity p_279187_, boolean p_279225_)
    {
        SignText signtext = p_279187_.getText(p_279225_);
        return Arrays.stream(signtext.getMessages(p_279394_.isTextFilteringEnabled()))
               .allMatch(p_327267_ -> p_327267_.equals(CommonComponents.EMPTY) || p_327267_.getContents() instanceof PlainTextContents);
    }

    public abstract float getYRotationDegrees(BlockState p_277705_);

    public Vec3 getSignHitboxCenterPosition(BlockState p_278294_)
    {
        return new Vec3(0.5, 0.5, 0.5);
    }

    @Override
    protected FluidState getFluidState(BlockState p_56299_)
    {
        return p_56299_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_56299_);
    }

    public WoodType type()
    {
        return this.type;
    }

    public static WoodType getWoodType(Block p_251096_)
    {
        WoodType woodtype;

        if (p_251096_ instanceof SignBlock)
        {
            woodtype = ((SignBlock)p_251096_).type();
        }
        else
        {
            woodtype = WoodType.OAK;
        }

        return woodtype;
    }

    public void openTextEdit(Player p_277738_, SignBlockEntity p_277467_, boolean p_277771_)
    {
        p_277467_.setAllowedPlayerEditor(p_277738_.getUUID());
        p_277738_.openTextEdit(p_277467_, p_277771_);
    }

    private boolean otherPlayerIsEditingSign(Player p_277952_, SignBlockEntity p_277599_)
    {
        UUID uuid = p_277599_.getPlayerWhoMayEdit();
        return uuid != null && !uuid.equals(p_277952_.getUUID());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_277367_, BlockState p_277896_, BlockEntityType<T> p_277724_)
    {
        return createTickerHelper(p_277724_, BlockEntityType.SIGN, SignBlockEntity::tick);
    }
}
