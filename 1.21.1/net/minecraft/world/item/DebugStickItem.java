package net.minecraft.world.item;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class DebugStickItem extends Item
{
    public DebugStickItem(Item.Properties p_40948_)
    {
        super(p_40948_);
    }

    @Override
    public boolean canAttackBlock(BlockState p_40962_, Level p_40963_, BlockPos p_40964_, Player p_40965_)
    {
        if (!p_40963_.isClientSide)
        {
            this.handleInteraction(p_40965_, p_40962_, p_40963_, p_40964_, false, p_40965_.getItemInHand(InteractionHand.MAIN_HAND));
        }

        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext p_40960_)
    {
        Player player = p_40960_.getPlayer();
        Level level = p_40960_.getLevel();

        if (!level.isClientSide && player != null)
        {
            BlockPos blockpos = p_40960_.getClickedPos();

            if (!this.handleInteraction(player, level.getBlockState(blockpos), level, blockpos, true, p_40960_.getItemInHand()))
            {
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean handleInteraction(Player p_150803_, BlockState p_150804_, LevelAccessor p_150805_, BlockPos p_150806_, boolean p_150807_, ItemStack p_150808_)
    {
        if (!p_150803_.canUseGameMasterBlocks())
        {
            return false;
        }
        else
        {
            Holder<Block> holder = p_150804_.getBlockHolder();
            StateDefinition<Block, BlockState> statedefinition = holder.value().getStateDefinition();
            Collection < Property<? >> collection = statedefinition.getProperties();

            if (collection.isEmpty())
            {
                message(p_150803_, Component.translatable(this.getDescriptionId() + ".empty", holder.getRegisteredName()));
                return false;
            }
            else
            {
                DebugStickState debugstickstate = p_150808_.get(DataComponents.DEBUG_STICK_STATE);

                if (debugstickstate == null)
                {
                    return false;
                }
                else
                {
                    Property<?> property = debugstickstate.properties().get(holder);

                    if (p_150807_)
                    {
                        if (property == null)
                        {
                            property = collection.iterator().next();
                        }

                        BlockState blockstate = cycleState(p_150804_, property, p_150803_.isSecondaryUseActive());
                        p_150805_.setBlock(p_150806_, blockstate, 18);
                        message(p_150803_, Component.translatable(this.getDescriptionId() + ".update", property.getName(), getNameHelper(blockstate, property)));
                    }
                    else
                    {
                        property = getRelative(collection, property, p_150803_.isSecondaryUseActive());
                        p_150808_.set(DataComponents.DEBUG_STICK_STATE, debugstickstate.withProperty(holder, property));
                        message(p_150803_, Component.translatable(this.getDescriptionId() + ".select", property.getName(), getNameHelper(p_150804_, property)));
                    }

                    return true;
                }
            }
        }
    }

    private static <T extends Comparable<T>> BlockState cycleState(BlockState p_40970_, Property<T> p_40971_, boolean p_40972_)
    {
        return p_40970_.setValue(p_40971_, getRelative(p_40971_.getPossibleValues(), p_40970_.getValue(p_40971_), p_40972_));
    }

    private static <T> T getRelative(Iterable<T> p_40974_, @Nullable T p_40975_, boolean p_40976_)
    {
        return p_40976_ ? Util.findPreviousInIterable(p_40974_, p_40975_) : Util.findNextInIterable(p_40974_, p_40975_);
    }

    private static void message(Player p_40957_, Component p_40958_)
    {
        ((ServerPlayer)p_40957_).sendSystemMessage(p_40958_, true);
    }

    private static <T extends Comparable<T>> String getNameHelper(BlockState p_40967_, Property<T> p_40968_)
    {
        return p_40968_.getName(p_40967_.getValue(p_40968_));
    }
}
