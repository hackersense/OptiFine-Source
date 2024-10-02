package net.minecraft.data.worldgen.placement;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountOnEveryLayerPlacement;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

public class TreePlacements
{
    public static final ResourceKey<PlacedFeature> CRIMSON_FUNGI = PlacementUtils.createKey("crimson_fungi");
    public static final ResourceKey<PlacedFeature> WARPED_FUNGI = PlacementUtils.createKey("warped_fungi");
    public static final ResourceKey<PlacedFeature> OAK_CHECKED = PlacementUtils.createKey("oak_checked");
    public static final ResourceKey<PlacedFeature> DARK_OAK_CHECKED = PlacementUtils.createKey("dark_oak_checked");
    public static final ResourceKey<PlacedFeature> BIRCH_CHECKED = PlacementUtils.createKey("birch_checked");
    public static final ResourceKey<PlacedFeature> ACACIA_CHECKED = PlacementUtils.createKey("acacia_checked");
    public static final ResourceKey<PlacedFeature> SPRUCE_CHECKED = PlacementUtils.createKey("spruce_checked");
    public static final ResourceKey<PlacedFeature> MANGROVE_CHECKED = PlacementUtils.createKey("mangrove_checked");
    public static final ResourceKey<PlacedFeature> CHERRY_CHECKED = PlacementUtils.createKey("cherry_checked");
    public static final ResourceKey<PlacedFeature> PINE_ON_SNOW = PlacementUtils.createKey("pine_on_snow");
    public static final ResourceKey<PlacedFeature> SPRUCE_ON_SNOW = PlacementUtils.createKey("spruce_on_snow");
    public static final ResourceKey<PlacedFeature> PINE_CHECKED = PlacementUtils.createKey("pine_checked");
    public static final ResourceKey<PlacedFeature> JUNGLE_TREE_CHECKED = PlacementUtils.createKey("jungle_tree");
    public static final ResourceKey<PlacedFeature> FANCY_OAK_CHECKED = PlacementUtils.createKey("fancy_oak_checked");
    public static final ResourceKey<PlacedFeature> MEGA_JUNGLE_TREE_CHECKED = PlacementUtils.createKey("mega_jungle_tree_checked");
    public static final ResourceKey<PlacedFeature> MEGA_SPRUCE_CHECKED = PlacementUtils.createKey("mega_spruce_checked");
    public static final ResourceKey<PlacedFeature> MEGA_PINE_CHECKED = PlacementUtils.createKey("mega_pine_checked");
    public static final ResourceKey<PlacedFeature> TALL_MANGROVE_CHECKED = PlacementUtils.createKey("tall_mangrove_checked");
    public static final ResourceKey<PlacedFeature> JUNGLE_BUSH = PlacementUtils.createKey("jungle_bush");
    public static final ResourceKey<PlacedFeature> SUPER_BIRCH_BEES_0002 = PlacementUtils.createKey("super_birch_bees_0002");
    public static final ResourceKey<PlacedFeature> SUPER_BIRCH_BEES = PlacementUtils.createKey("super_birch_bees");
    public static final ResourceKey<PlacedFeature> OAK_BEES_0002 = PlacementUtils.createKey("oak_bees_0002");
    public static final ResourceKey<PlacedFeature> OAK_BEES_002 = PlacementUtils.createKey("oak_bees_002");
    public static final ResourceKey<PlacedFeature> BIRCH_BEES_0002_PLACED = PlacementUtils.createKey("birch_bees_0002");
    public static final ResourceKey<PlacedFeature> BIRCH_BEES_002 = PlacementUtils.createKey("birch_bees_002");
    public static final ResourceKey<PlacedFeature> FANCY_OAK_BEES_0002 = PlacementUtils.createKey("fancy_oak_bees_0002");
    public static final ResourceKey<PlacedFeature> FANCY_OAK_BEES_002 = PlacementUtils.createKey("fancy_oak_bees_002");
    public static final ResourceKey<PlacedFeature> FANCY_OAK_BEES = PlacementUtils.createKey("fancy_oak_bees");
    public static final ResourceKey<PlacedFeature> CHERRY_BEES_005 = PlacementUtils.createKey("cherry_bees_005");

    public static void bootstrap(BootstrapContext<PlacedFeature> p_330667_)
    {
        HolderGetter < ConfiguredFeature <? , ? >> holdergetter = p_330667_.lookup(Registries.CONFIGURED_FEATURE);
        Holder < ConfiguredFeature <? , ? >> holder = holdergetter.getOrThrow(TreeFeatures.CRIMSON_FUNGUS);
        Holder < ConfiguredFeature <? , ? >> holder1 = holdergetter.getOrThrow(TreeFeatures.WARPED_FUNGUS);
        Holder < ConfiguredFeature <? , ? >> holder2 = holdergetter.getOrThrow(TreeFeatures.OAK);
        Holder < ConfiguredFeature <? , ? >> holder3 = holdergetter.getOrThrow(TreeFeatures.DARK_OAK);
        Holder < ConfiguredFeature <? , ? >> holder4 = holdergetter.getOrThrow(TreeFeatures.BIRCH);
        Holder < ConfiguredFeature <? , ? >> holder5 = holdergetter.getOrThrow(TreeFeatures.ACACIA);
        Holder < ConfiguredFeature <? , ? >> holder6 = holdergetter.getOrThrow(TreeFeatures.SPRUCE);
        Holder < ConfiguredFeature <? , ? >> holder7 = holdergetter.getOrThrow(TreeFeatures.MANGROVE);
        Holder < ConfiguredFeature <? , ? >> holder8 = holdergetter.getOrThrow(TreeFeatures.CHERRY);
        Holder < ConfiguredFeature <? , ? >> holder9 = holdergetter.getOrThrow(TreeFeatures.PINE);
        Holder < ConfiguredFeature <? , ? >> holder10 = holdergetter.getOrThrow(TreeFeatures.JUNGLE_TREE);
        Holder < ConfiguredFeature <? , ? >> holder11 = holdergetter.getOrThrow(TreeFeatures.FANCY_OAK);
        Holder < ConfiguredFeature <? , ? >> holder12 = holdergetter.getOrThrow(TreeFeatures.MEGA_JUNGLE_TREE);
        Holder < ConfiguredFeature <? , ? >> holder13 = holdergetter.getOrThrow(TreeFeatures.MEGA_SPRUCE);
        Holder < ConfiguredFeature <? , ? >> holder14 = holdergetter.getOrThrow(TreeFeatures.MEGA_PINE);
        Holder < ConfiguredFeature <? , ? >> holder15 = holdergetter.getOrThrow(TreeFeatures.TALL_MANGROVE);
        Holder < ConfiguredFeature <? , ? >> holder16 = holdergetter.getOrThrow(TreeFeatures.JUNGLE_BUSH);
        Holder < ConfiguredFeature <? , ? >> holder17 = holdergetter.getOrThrow(TreeFeatures.SUPER_BIRCH_BEES_0002);
        Holder < ConfiguredFeature <? , ? >> holder18 = holdergetter.getOrThrow(TreeFeatures.SUPER_BIRCH_BEES);
        Holder < ConfiguredFeature <? , ? >> holder19 = holdergetter.getOrThrow(TreeFeatures.OAK_BEES_0002);
        Holder < ConfiguredFeature <? , ? >> holder20 = holdergetter.getOrThrow(TreeFeatures.OAK_BEES_002);
        Holder < ConfiguredFeature <? , ? >> holder21 = holdergetter.getOrThrow(TreeFeatures.BIRCH_BEES_0002);
        Holder < ConfiguredFeature <? , ? >> holder22 = holdergetter.getOrThrow(TreeFeatures.BIRCH_BEES_002);
        Holder < ConfiguredFeature <? , ? >> holder23 = holdergetter.getOrThrow(TreeFeatures.FANCY_OAK_BEES_0002);
        Holder < ConfiguredFeature <? , ? >> holder24 = holdergetter.getOrThrow(TreeFeatures.FANCY_OAK_BEES_002);
        Holder < ConfiguredFeature <? , ? >> holder25 = holdergetter.getOrThrow(TreeFeatures.FANCY_OAK_BEES);
        Holder < ConfiguredFeature <? , ? >> holder26 = holdergetter.getOrThrow(TreeFeatures.CHERRY_BEES_005);
        PlacementUtils.register(p_330667_, CRIMSON_FUNGI, holder, CountOnEveryLayerPlacement.of(8), BiomeFilter.biome());
        PlacementUtils.register(p_330667_, WARPED_FUNGI, holder1, CountOnEveryLayerPlacement.of(8), BiomeFilter.biome());
        PlacementUtils.register(p_330667_, OAK_CHECKED, holder2, PlacementUtils.filteredByBlockSurvival(Blocks.OAK_SAPLING));
        PlacementUtils.register(p_330667_, DARK_OAK_CHECKED, holder3, PlacementUtils.filteredByBlockSurvival(Blocks.DARK_OAK_SAPLING));
        PlacementUtils.register(p_330667_, BIRCH_CHECKED, holder4, PlacementUtils.filteredByBlockSurvival(Blocks.BIRCH_SAPLING));
        PlacementUtils.register(p_330667_, ACACIA_CHECKED, holder5, PlacementUtils.filteredByBlockSurvival(Blocks.ACACIA_SAPLING));
        PlacementUtils.register(p_330667_, SPRUCE_CHECKED, holder6, PlacementUtils.filteredByBlockSurvival(Blocks.SPRUCE_SAPLING));
        PlacementUtils.register(p_330667_, MANGROVE_CHECKED, holder7, PlacementUtils.filteredByBlockSurvival(Blocks.MANGROVE_PROPAGULE));
        PlacementUtils.register(p_330667_, CHERRY_CHECKED, holder8, PlacementUtils.filteredByBlockSurvival(Blocks.CHERRY_SAPLING));
        BlockPredicate blockpredicate = BlockPredicate.matchesBlocks(Direction.DOWN.getNormal(), Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW);
        List<PlacementModifier> list = List.of(
                                           EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.not(BlockPredicate.matchesBlocks(Blocks.POWDER_SNOW)), 8),
                                           BlockPredicateFilter.forPredicate(blockpredicate)
                                       );
        PlacementUtils.register(p_330667_, PINE_ON_SNOW, holder9, list);
        PlacementUtils.register(p_330667_, SPRUCE_ON_SNOW, holder6, list);
        PlacementUtils.register(p_330667_, PINE_CHECKED, holder9, PlacementUtils.filteredByBlockSurvival(Blocks.SPRUCE_SAPLING));
        PlacementUtils.register(p_330667_, JUNGLE_TREE_CHECKED, holder10, PlacementUtils.filteredByBlockSurvival(Blocks.JUNGLE_SAPLING));
        PlacementUtils.register(p_330667_, FANCY_OAK_CHECKED, holder11, PlacementUtils.filteredByBlockSurvival(Blocks.OAK_SAPLING));
        PlacementUtils.register(p_330667_, MEGA_JUNGLE_TREE_CHECKED, holder12, PlacementUtils.filteredByBlockSurvival(Blocks.JUNGLE_SAPLING));
        PlacementUtils.register(p_330667_, MEGA_SPRUCE_CHECKED, holder13, PlacementUtils.filteredByBlockSurvival(Blocks.SPRUCE_SAPLING));
        PlacementUtils.register(p_330667_, MEGA_PINE_CHECKED, holder14, PlacementUtils.filteredByBlockSurvival(Blocks.SPRUCE_SAPLING));
        PlacementUtils.register(p_330667_, TALL_MANGROVE_CHECKED, holder15, PlacementUtils.filteredByBlockSurvival(Blocks.MANGROVE_PROPAGULE));
        PlacementUtils.register(p_330667_, JUNGLE_BUSH, holder16, PlacementUtils.filteredByBlockSurvival(Blocks.OAK_SAPLING));
        PlacementUtils.register(p_330667_, SUPER_BIRCH_BEES_0002, holder17, PlacementUtils.filteredByBlockSurvival(Blocks.BIRCH_SAPLING));
        PlacementUtils.register(p_330667_, SUPER_BIRCH_BEES, holder18, PlacementUtils.filteredByBlockSurvival(Blocks.BIRCH_SAPLING));
        PlacementUtils.register(p_330667_, OAK_BEES_0002, holder19, PlacementUtils.filteredByBlockSurvival(Blocks.OAK_SAPLING));
        PlacementUtils.register(p_330667_, OAK_BEES_002, holder20, PlacementUtils.filteredByBlockSurvival(Blocks.OAK_SAPLING));
        PlacementUtils.register(p_330667_, BIRCH_BEES_0002_PLACED, holder21, PlacementUtils.filteredByBlockSurvival(Blocks.BIRCH_SAPLING));
        PlacementUtils.register(p_330667_, BIRCH_BEES_002, holder22, PlacementUtils.filteredByBlockSurvival(Blocks.BIRCH_SAPLING));
        PlacementUtils.register(p_330667_, FANCY_OAK_BEES_0002, holder23, PlacementUtils.filteredByBlockSurvival(Blocks.OAK_SAPLING));
        PlacementUtils.register(p_330667_, FANCY_OAK_BEES_002, holder24, PlacementUtils.filteredByBlockSurvival(Blocks.OAK_SAPLING));
        PlacementUtils.register(p_330667_, FANCY_OAK_BEES, holder25, PlacementUtils.filteredByBlockSurvival(Blocks.OAK_SAPLING));
        PlacementUtils.register(p_330667_, CHERRY_BEES_005, holder26, PlacementUtils.filteredByBlockSurvival(Blocks.CHERRY_SAPLING));
    }
}
