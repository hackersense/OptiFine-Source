package net.minecraft.core.cauldron;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public interface CauldronInteraction
{
    Map<String, CauldronInteraction.InteractionMap> INTERACTIONS = new Object2ObjectArrayMap<>();
    Codec<CauldronInteraction.InteractionMap> CODEC = Codec.stringResolver(CauldronInteraction.InteractionMap::name, INTERACTIONS::get);
    CauldronInteraction.InteractionMap EMPTY = newInteractionMap("empty");
    CauldronInteraction.InteractionMap WATER = newInteractionMap("water");
    CauldronInteraction.InteractionMap LAVA = newInteractionMap("lava");
    CauldronInteraction.InteractionMap POWDER_SNOW = newInteractionMap("powder_snow");
    CauldronInteraction FILL_WATER = (p_325752_, p_325753_, p_325754_, p_325755_, p_325756_, p_325757_) -> emptyBucket(
                                        p_325753_,
                                        p_325754_,
                                        p_325755_,
                                        p_325756_,
                                        p_325757_,
                                        Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, Integer.valueOf(3)),
                                        SoundEvents.BUCKET_EMPTY
                                    );
    CauldronInteraction FILL_LAVA = (p_325776_, p_325777_, p_325778_, p_325779_, p_325780_, p_325781_) -> emptyBucket(
                                        p_325777_, p_325778_, p_325779_, p_325780_, p_325781_, Blocks.LAVA_CAULDRON.defaultBlockState(), SoundEvents.BUCKET_EMPTY_LAVA
                                    );
    CauldronInteraction FILL_POWDER_SNOW = (p_325782_, p_325783_, p_325784_, p_325785_, p_325786_, p_325787_) -> emptyBucket(
                                        p_325783_,
                                        p_325784_,
                                        p_325785_,
                                        p_325786_,
                                        p_325787_,
                                        Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, Integer.valueOf(3)),
                                        SoundEvents.BUCKET_EMPTY_POWDER_SNOW
                                    );
    CauldronInteraction SHULKER_BOX = (p_340986_, p_340987_, p_340988_, p_340989_, p_340990_, p_340991_) ->
    {
        Block block = Block.byItem(p_340991_.getItem());

        if (!(block instanceof ShulkerBoxBlock))
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        else {
            if (!p_340987_.isClientSide)
            {
                ItemStack itemstack = p_340991_.transmuteCopy(Blocks.SHULKER_BOX, 1);
                p_340989_.setItemInHand(p_340990_, ItemUtils.createFilledResult(p_340991_, p_340989_, itemstack, false));
                p_340989_.awardStat(Stats.CLEAN_SHULKER_BOX);
                LayeredCauldronBlock.lowerFillLevel(p_340986_, p_340987_, p_340988_);
            }

            return ItemInteractionResult.sidedSuccess(p_340987_.isClientSide);
        }
    };
    CauldronInteraction BANNER = (p_340992_, p_340993_, p_340994_, p_340995_, p_340996_, p_340997_) ->
    {
        BannerPatternLayers bannerpatternlayers = p_340997_.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);

        if (bannerpatternlayers.layers().isEmpty())
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        else {
            if (!p_340993_.isClientSide)
            {
                ItemStack itemstack = p_340997_.copyWithCount(1);
                itemstack.set(DataComponents.BANNER_PATTERNS, bannerpatternlayers.removeLast());
                p_340995_.setItemInHand(p_340996_, ItemUtils.createFilledResult(p_340997_, p_340995_, itemstack, false));
                p_340995_.awardStat(Stats.CLEAN_BANNER);
                LayeredCauldronBlock.lowerFillLevel(p_340992_, p_340993_, p_340994_);
            }

            return ItemInteractionResult.sidedSuccess(p_340993_.isClientSide);
        }
    };
    CauldronInteraction DYED_ITEM = (p_325770_, p_325771_, p_325772_, p_325773_, p_325774_, p_325775_) ->
    {
        if (!p_325775_.is(ItemTags.DYEABLE))
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        else if (!p_325775_.has(DataComponents.DYED_COLOR))
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        else {
            if (!p_325771_.isClientSide)
            {
                p_325775_.remove(DataComponents.DYED_COLOR);
                p_325773_.awardStat(Stats.CLEAN_ARMOR);
                LayeredCauldronBlock.lowerFillLevel(p_325770_, p_325771_, p_325772_);
            }

            return ItemInteractionResult.sidedSuccess(p_325771_.isClientSide);
        }
    };

    static CauldronInteraction.InteractionMap newInteractionMap(String p_311265_)
    {
        Object2ObjectOpenHashMap<Item, CauldronInteraction> object2objectopenhashmap = new Object2ObjectOpenHashMap<>();
        object2objectopenhashmap.defaultReturnValue(
            (p_325722_, p_325723_, p_325724_, p_325725_, p_325726_, p_325727_) -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        );
        CauldronInteraction.InteractionMap cauldroninteraction$interactionmap = new CauldronInteraction.InteractionMap(p_311265_, object2objectopenhashmap);
        INTERACTIONS.put(p_311265_, cauldroninteraction$interactionmap);
        return cauldroninteraction$interactionmap;
    }

    ItemInteractionResult interact(BlockState p_175711_, Level p_175712_, BlockPos p_175713_, Player p_175714_, InteractionHand p_175715_, ItemStack p_175716_);

    static void bootStrap()
    {
        Map<Item, CauldronInteraction> map = EMPTY.map();
        addDefaultInteractions(map);
        map.put(Items.POTION, (p_175732_, p_175733_, p_175734_, p_175735_, p_175736_, p_175737_) ->
        {
            PotionContents potioncontents = p_175737_.get(DataComponents.POTION_CONTENTS);

            if (potioncontents != null && potioncontents.is(Potions.WATER))
            {
                if (!p_175733_.isClientSide)
                {
                    Item item = p_175737_.getItem();
                    p_175735_.setItemInHand(p_175736_, ItemUtils.createFilledResult(p_175737_, p_175735_, new ItemStack(Items.GLASS_BOTTLE)));
                    p_175735_.awardStat(Stats.USE_CAULDRON);
                    p_175735_.awardStat(Stats.ITEM_USED.get(item));
                    p_175733_.setBlockAndUpdate(p_175734_, Blocks.WATER_CAULDRON.defaultBlockState());
                    p_175733_.playSound(null, p_175734_, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    p_175733_.gameEvent(null, GameEvent.FLUID_PLACE, p_175734_);
                }

                return ItemInteractionResult.sidedSuccess(p_175733_.isClientSide);
            }
            else {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
        });
        Map<Item, CauldronInteraction> map1 = WATER.map();
        addDefaultInteractions(map1);
        map1.put(
            Items.BUCKET,
            (p_325728_, p_325729_, p_325730_, p_325731_, p_325732_, p_325733_) -> fillBucket(
                p_325728_,
                p_325729_,
                p_325730_,
                p_325731_,
                p_325732_,
                p_325733_,
                new ItemStack(Items.WATER_BUCKET),
                p_175660_ -> p_175660_.getValue(LayeredCauldronBlock.LEVEL) == 3,
                SoundEvents.BUCKET_FILL
            )
        );
        map1.put(Items.GLASS_BOTTLE, (p_325758_, p_325759_, p_325760_, p_325761_, p_325762_, p_325763_) ->
        {
            if (!p_325759_.isClientSide)
            {
                Item item = p_325763_.getItem();
                p_325761_.setItemInHand(p_325762_, ItemUtils.createFilledResult(p_325763_, p_325761_, PotionContents.createItemStack(Items.POTION, Potions.WATER)));
                p_325761_.awardStat(Stats.USE_CAULDRON);
                p_325761_.awardStat(Stats.ITEM_USED.get(item));
                LayeredCauldronBlock.lowerFillLevel(p_325758_, p_325759_, p_325760_);
                p_325759_.playSound(null, p_325760_, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                p_325759_.gameEvent(null, GameEvent.FLUID_PICKUP, p_325760_);
            }

            return ItemInteractionResult.sidedSuccess(p_325759_.isClientSide);
        });
        map1.put(Items.POTION, (p_175704_, p_175705_, p_175706_, p_175707_, p_175708_, p_175709_) ->
        {
            if (p_175704_.getValue(LayeredCauldronBlock.LEVEL) == 3)
            {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            else {
                PotionContents potioncontents = p_175709_.get(DataComponents.POTION_CONTENTS);

                if (potioncontents != null && potioncontents.is(Potions.WATER))
                {
                    if (!p_175705_.isClientSide)
                    {
                        p_175707_.setItemInHand(p_175708_, ItemUtils.createFilledResult(p_175709_, p_175707_, new ItemStack(Items.GLASS_BOTTLE)));
                        p_175707_.awardStat(Stats.USE_CAULDRON);
                        p_175707_.awardStat(Stats.ITEM_USED.get(p_175709_.getItem()));
                        p_175705_.setBlockAndUpdate(p_175706_, p_175704_.cycle(LayeredCauldronBlock.LEVEL));
                        p_175705_.playSound(null, p_175706_, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                        p_175705_.gameEvent(null, GameEvent.FLUID_PLACE, p_175706_);
                    }

                    return ItemInteractionResult.sidedSuccess(p_175705_.isClientSide);
                }
                else {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
            }
        });
        map1.put(Items.LEATHER_BOOTS, DYED_ITEM);
        map1.put(Items.LEATHER_LEGGINGS, DYED_ITEM);
        map1.put(Items.LEATHER_CHESTPLATE, DYED_ITEM);
        map1.put(Items.LEATHER_HELMET, DYED_ITEM);
        map1.put(Items.LEATHER_HORSE_ARMOR, DYED_ITEM);
        map1.put(Items.WOLF_ARMOR, DYED_ITEM);
        map1.put(Items.WHITE_BANNER, BANNER);
        map1.put(Items.GRAY_BANNER, BANNER);
        map1.put(Items.BLACK_BANNER, BANNER);
        map1.put(Items.BLUE_BANNER, BANNER);
        map1.put(Items.BROWN_BANNER, BANNER);
        map1.put(Items.CYAN_BANNER, BANNER);
        map1.put(Items.GREEN_BANNER, BANNER);
        map1.put(Items.LIGHT_BLUE_BANNER, BANNER);
        map1.put(Items.LIGHT_GRAY_BANNER, BANNER);
        map1.put(Items.LIME_BANNER, BANNER);
        map1.put(Items.MAGENTA_BANNER, BANNER);
        map1.put(Items.ORANGE_BANNER, BANNER);
        map1.put(Items.PINK_BANNER, BANNER);
        map1.put(Items.PURPLE_BANNER, BANNER);
        map1.put(Items.RED_BANNER, BANNER);
        map1.put(Items.YELLOW_BANNER, BANNER);
        map1.put(Items.WHITE_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.GRAY_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.BLACK_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.BLUE_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.BROWN_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.CYAN_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.GREEN_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.LIGHT_BLUE_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.LIGHT_GRAY_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.LIME_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.MAGENTA_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.ORANGE_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.PINK_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.PURPLE_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.RED_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.YELLOW_SHULKER_BOX, SHULKER_BOX);
        Map<Item, CauldronInteraction> map2 = LAVA.map();
        map2.put(
            Items.BUCKET,
            (p_325734_, p_325735_, p_325736_, p_325737_, p_325738_, p_325739_) -> fillBucket(
                p_325734_, p_325735_, p_325736_, p_325737_, p_325738_, p_325739_, new ItemStack(Items.LAVA_BUCKET), p_175651_ -> true, SoundEvents.BUCKET_FILL_LAVA
            )
        );
        addDefaultInteractions(map2);
        Map<Item, CauldronInteraction> map3 = POWDER_SNOW.map();
        map3.put(
            Items.BUCKET,
            (p_325740_, p_325741_, p_325742_, p_325743_, p_325744_, p_325745_) -> fillBucket(
                p_325740_,
                p_325741_,
                p_325742_,
                p_325743_,
                p_325744_,
                p_325745_,
                new ItemStack(Items.POWDER_SNOW_BUCKET),
                p_175627_ -> p_175627_.getValue(LayeredCauldronBlock.LEVEL) == 3,
                SoundEvents.BUCKET_FILL_POWDER_SNOW
            )
        );
        addDefaultInteractions(map3);
    }

    static void addDefaultInteractions(Map<Item, CauldronInteraction> p_175648_)
    {
        p_175648_.put(Items.LAVA_BUCKET, FILL_LAVA);
        p_175648_.put(Items.WATER_BUCKET, FILL_WATER);
        p_175648_.put(Items.POWDER_SNOW_BUCKET, FILL_POWDER_SNOW);
    }

    static ItemInteractionResult fillBucket(
        BlockState p_175636_,
        Level p_175637_,
        BlockPos p_175638_,
        Player p_175639_,
        InteractionHand p_175640_,
        ItemStack p_175641_,
        ItemStack p_175642_,
        Predicate<BlockState> p_175643_,
        SoundEvent p_175644_
    )
    {
        if (!p_175643_.test(p_175636_))
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        else
        {
            if (!p_175637_.isClientSide)
            {
                Item item = p_175641_.getItem();
                p_175639_.setItemInHand(p_175640_, ItemUtils.createFilledResult(p_175641_, p_175639_, p_175642_));
                p_175639_.awardStat(Stats.USE_CAULDRON);
                p_175639_.awardStat(Stats.ITEM_USED.get(item));
                p_175637_.setBlockAndUpdate(p_175638_, Blocks.CAULDRON.defaultBlockState());
                p_175637_.playSound(null, p_175638_, p_175644_, SoundSource.BLOCKS, 1.0F, 1.0F);
                p_175637_.gameEvent(null, GameEvent.FLUID_PICKUP, p_175638_);
            }

            return ItemInteractionResult.sidedSuccess(p_175637_.isClientSide);
        }
    }

    static ItemInteractionResult emptyBucket(
        Level p_175619_, BlockPos p_175620_, Player p_175621_, InteractionHand p_175622_, ItemStack p_175623_, BlockState p_175624_, SoundEvent p_175625_
    )
    {
        if (!p_175619_.isClientSide)
        {
            Item item = p_175623_.getItem();
            p_175621_.setItemInHand(p_175622_, ItemUtils.createFilledResult(p_175623_, p_175621_, new ItemStack(Items.BUCKET)));
            p_175621_.awardStat(Stats.FILL_CAULDRON);
            p_175621_.awardStat(Stats.ITEM_USED.get(item));
            p_175619_.setBlockAndUpdate(p_175620_, p_175624_);
            p_175619_.playSound(null, p_175620_, p_175625_, SoundSource.BLOCKS, 1.0F, 1.0F);
            p_175619_.gameEvent(null, GameEvent.FLUID_PLACE, p_175620_);
        }

        return ItemInteractionResult.sidedSuccess(p_175619_.isClientSide);
    }

    public static record InteractionMap(String name, Map<Item, CauldronInteraction> map)
    {
    }
}
