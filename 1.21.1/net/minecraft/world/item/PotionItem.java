package net.minecraft.world.item;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class PotionItem extends Item
{
    private static final int DRINK_DURATION = 32;

    public PotionItem(Item.Properties p_42979_)
    {
        super(p_42979_);
    }

    @Override
    public ItemStack getDefaultInstance()
    {
        ItemStack itemstack = super.getDefaultInstance();
        itemstack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        return itemstack;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack p_42984_, Level p_42985_, LivingEntity p_42986_)
    {
        Player player = p_42986_ instanceof Player ? (Player)p_42986_ : null;

        if (player instanceof ServerPlayer)
        {
            CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)player, p_42984_);
        }

        if (!p_42985_.isClientSide)
        {
            PotionContents potioncontents = p_42984_.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            potioncontents.forEachEffect(p_327729_ ->
            {
                if (p_327729_.getEffect().value().isInstantenous())
                {
                    p_327729_.getEffect().value().applyInstantenousEffect(player, player, p_42986_, p_327729_.getAmplifier(), 1.0);
                }
                else {
                    p_42986_.addEffect(p_327729_);
                }
            });
        }

        if (player != null)
        {
            player.awardStat(Stats.ITEM_USED.get(this));
            p_42984_.consume(1, player);
        }

        if (player == null || !player.hasInfiniteMaterials())
        {
            if (p_42984_.isEmpty())
            {
                return new ItemStack(Items.GLASS_BOTTLE);
            }

            if (player != null)
            {
                player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
            }
        }

        p_42986_.gameEvent(GameEvent.DRINK);
        return p_42984_;
    }

    @Override
    public InteractionResult useOn(UseOnContext p_220235_)
    {
        Level level = p_220235_.getLevel();
        BlockPos blockpos = p_220235_.getClickedPos();
        Player player = p_220235_.getPlayer();
        ItemStack itemstack = p_220235_.getItemInHand();
        PotionContents potioncontents = itemstack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        BlockState blockstate = level.getBlockState(blockpos);

        if (p_220235_.getClickedFace() != Direction.DOWN && blockstate.is(BlockTags.CONVERTABLE_TO_MUD) && potioncontents.is(Potions.WATER))
        {
            level.playSound(null, blockpos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.setItemInHand(p_220235_.getHand(), ItemUtils.createFilledResult(itemstack, player, new ItemStack(Items.GLASS_BOTTLE)));
            player.awardStat(Stats.ITEM_USED.get(itemstack.getItem()));

            if (!level.isClientSide)
            {
                ServerLevel serverlevel = (ServerLevel)level;

                for (int i = 0; i < 5; i++)
                {
                    serverlevel.sendParticles(
                        ParticleTypes.SPLASH,
                        (double)blockpos.getX() + level.random.nextDouble(),
                        (double)(blockpos.getY() + 1),
                        (double)blockpos.getZ() + level.random.nextDouble(),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        1.0
                    );
                }
            }

            level.playSound(null, blockpos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, blockpos);
            level.setBlockAndUpdate(blockpos, Blocks.MUD.defaultBlockState());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        else
        {
            return InteractionResult.PASS;
        }
    }

    @Override
    public int getUseDuration(ItemStack p_43001_, LivingEntity p_345280_)
    {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack p_42997_)
    {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level p_42993_, Player p_42994_, InteractionHand p_42995_)
    {
        return ItemUtils.startUsingInstantly(p_42993_, p_42994_, p_42995_);
    }

    @Override
    public String getDescriptionId(ItemStack p_43003_)
    {
        return Potion.getName(p_43003_.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion(), this.getDescriptionId() + ".effect.");
    }

    @Override
    public void appendHoverText(ItemStack p_42988_, Item.TooltipContext p_332780_, List<Component> p_42990_, TooltipFlag p_42991_)
    {
        PotionContents potioncontents = p_42988_.get(DataComponents.POTION_CONTENTS);

        if (potioncontents != null)
        {
            potioncontents.addPotionTooltip(p_42990_::add, 1.0F, p_332780_.tickRate());
        }
    }
}
