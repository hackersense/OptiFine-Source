package net.minecraft.util.datafix;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFixerBuilder.Result;
import com.mojang.datafixers.schemas.Schema;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.util.datafix.fixes.AbstractArrowPickupFix;
import net.minecraft.util.datafix.fixes.AddFlagIfNotPresentFix;
import net.minecraft.util.datafix.fixes.AddNewChoices;
import net.minecraft.util.datafix.fixes.AdvancementsFix;
import net.minecraft.util.datafix.fixes.AdvancementsRenameFix;
import net.minecraft.util.datafix.fixes.AreaEffectCloudPotionFix;
import net.minecraft.util.datafix.fixes.AttributeModifierIdFix;
import net.minecraft.util.datafix.fixes.AttributesRename;
import net.minecraft.util.datafix.fixes.BannerEntityCustomNameToOverrideComponentFix;
import net.minecraft.util.datafix.fixes.BannerPatternFormatFix;
import net.minecraft.util.datafix.fixes.BedItemColorFix;
import net.minecraft.util.datafix.fixes.BeehiveFieldRenameFix;
import net.minecraft.util.datafix.fixes.BiomeFix;
import net.minecraft.util.datafix.fixes.BitStorageAlignFix;
import net.minecraft.util.datafix.fixes.BlendingDataFix;
import net.minecraft.util.datafix.fixes.BlendingDataRemoveFromNetherEndFix;
import net.minecraft.util.datafix.fixes.BlockEntityBannerColorFix;
import net.minecraft.util.datafix.fixes.BlockEntityBlockStateFix;
import net.minecraft.util.datafix.fixes.BlockEntityCustomNameToComponentFix;
import net.minecraft.util.datafix.fixes.BlockEntityIdFix;
import net.minecraft.util.datafix.fixes.BlockEntityJukeboxFix;
import net.minecraft.util.datafix.fixes.BlockEntityKeepPacked;
import net.minecraft.util.datafix.fixes.BlockEntityRenameFix;
import net.minecraft.util.datafix.fixes.BlockEntityShulkerBoxColorFix;
import net.minecraft.util.datafix.fixes.BlockEntitySignDoubleSidedEditableTextFix;
import net.minecraft.util.datafix.fixes.BlockEntitySignTextStrictJsonFix;
import net.minecraft.util.datafix.fixes.BlockEntityUUIDFix;
import net.minecraft.util.datafix.fixes.BlockNameFlatteningFix;
import net.minecraft.util.datafix.fixes.BlockPosFormatAndRenamesFix;
import net.minecraft.util.datafix.fixes.BlockRenameFix;
import net.minecraft.util.datafix.fixes.BlockStateStructureTemplateFix;
import net.minecraft.util.datafix.fixes.CatTypeFix;
import net.minecraft.util.datafix.fixes.CauldronRenameFix;
import net.minecraft.util.datafix.fixes.CavesAndCliffsRenames;
import net.minecraft.util.datafix.fixes.ChestedHorsesInventoryZeroIndexingFix;
import net.minecraft.util.datafix.fixes.ChunkBedBlockEntityInjecterFix;
import net.minecraft.util.datafix.fixes.ChunkBiomeFix;
import net.minecraft.util.datafix.fixes.ChunkDeleteIgnoredLightDataFix;
import net.minecraft.util.datafix.fixes.ChunkDeleteLightFix;
import net.minecraft.util.datafix.fixes.ChunkHeightAndBiomeFix;
import net.minecraft.util.datafix.fixes.ChunkLightRemoveFix;
import net.minecraft.util.datafix.fixes.ChunkPalettedStorageFix;
import net.minecraft.util.datafix.fixes.ChunkProtoTickListFix;
import net.minecraft.util.datafix.fixes.ChunkRenamesFix;
import net.minecraft.util.datafix.fixes.ChunkStatusFix;
import net.minecraft.util.datafix.fixes.ChunkStatusFix2;
import net.minecraft.util.datafix.fixes.ChunkStructuresTemplateRenameFix;
import net.minecraft.util.datafix.fixes.ChunkToProtochunkFix;
import net.minecraft.util.datafix.fixes.ColorlessShulkerEntityFix;
import net.minecraft.util.datafix.fixes.CriteriaRenameFix;
import net.minecraft.util.datafix.fixes.DecoratedPotFieldRenameFix;
import net.minecraft.util.datafix.fixes.DropInvalidSignDataFix;
import net.minecraft.util.datafix.fixes.DyeItemRenameFix;
import net.minecraft.util.datafix.fixes.EffectDurationFix;
import net.minecraft.util.datafix.fixes.EmptyItemInHotbarFix;
import net.minecraft.util.datafix.fixes.EmptyItemInVillagerTradeFix;
import net.minecraft.util.datafix.fixes.EntityArmorStandSilentFix;
import net.minecraft.util.datafix.fixes.EntityBlockStateFix;
import net.minecraft.util.datafix.fixes.EntityBrushableBlockFieldsRenameFix;
import net.minecraft.util.datafix.fixes.EntityCatSplitFix;
import net.minecraft.util.datafix.fixes.EntityCodSalmonFix;
import net.minecraft.util.datafix.fixes.EntityCustomNameToComponentFix;
import net.minecraft.util.datafix.fixes.EntityElderGuardianSplitFix;
import net.minecraft.util.datafix.fixes.EntityEquipmentToArmorAndHandFix;
import net.minecraft.util.datafix.fixes.EntityGoatMissingStateFix;
import net.minecraft.util.datafix.fixes.EntityHealthFix;
import net.minecraft.util.datafix.fixes.EntityHorseSaddleFix;
import net.minecraft.util.datafix.fixes.EntityHorseSplitFix;
import net.minecraft.util.datafix.fixes.EntityIdFix;
import net.minecraft.util.datafix.fixes.EntityItemFrameDirectionFix;
import net.minecraft.util.datafix.fixes.EntityMinecartIdentifiersFix;
import net.minecraft.util.datafix.fixes.EntityPaintingFieldsRenameFix;
import net.minecraft.util.datafix.fixes.EntityPaintingItemFrameDirectionFix;
import net.minecraft.util.datafix.fixes.EntityPaintingMotiveFix;
import net.minecraft.util.datafix.fixes.EntityProjectileOwnerFix;
import net.minecraft.util.datafix.fixes.EntityPufferfishRenameFix;
import net.minecraft.util.datafix.fixes.EntityRavagerRenameFix;
import net.minecraft.util.datafix.fixes.EntityRedundantChanceTagsFix;
import net.minecraft.util.datafix.fixes.EntityRidingToPassengersFix;
import net.minecraft.util.datafix.fixes.EntityShulkerColorFix;
import net.minecraft.util.datafix.fixes.EntityShulkerRotationFix;
import net.minecraft.util.datafix.fixes.EntitySkeletonSplitFix;
import net.minecraft.util.datafix.fixes.EntityStringUuidFix;
import net.minecraft.util.datafix.fixes.EntityTheRenameningFix;
import net.minecraft.util.datafix.fixes.EntityTippedArrowFix;
import net.minecraft.util.datafix.fixes.EntityUUIDFix;
import net.minecraft.util.datafix.fixes.EntityVariantFix;
import net.minecraft.util.datafix.fixes.EntityWolfColorFix;
import net.minecraft.util.datafix.fixes.EntityZombieSplitFix;
import net.minecraft.util.datafix.fixes.EntityZombieVillagerTypeFix;
import net.minecraft.util.datafix.fixes.EntityZombifiedPiglinRenameFix;
import net.minecraft.util.datafix.fixes.FeatureFlagRemoveFix;
import net.minecraft.util.datafix.fixes.FilteredBooksFix;
import net.minecraft.util.datafix.fixes.FilteredSignsFix;
import net.minecraft.util.datafix.fixes.FixProjectileStoredItem;
import net.minecraft.util.datafix.fixes.ForcePoiRebuild;
import net.minecraft.util.datafix.fixes.FurnaceRecipeFix;
import net.minecraft.util.datafix.fixes.GoatHornIdFix;
import net.minecraft.util.datafix.fixes.GossipUUIDFix;
import net.minecraft.util.datafix.fixes.HeightmapRenamingFix;
import net.minecraft.util.datafix.fixes.HorseBodyArmorItemFix;
import net.minecraft.util.datafix.fixes.IglooMetadataRemovalFix;
import net.minecraft.util.datafix.fixes.ItemBannerColorFix;
import net.minecraft.util.datafix.fixes.ItemCustomNameToComponentFix;
import net.minecraft.util.datafix.fixes.ItemIdFix;
import net.minecraft.util.datafix.fixes.ItemLoreFix;
import net.minecraft.util.datafix.fixes.ItemPotionFix;
import net.minecraft.util.datafix.fixes.ItemRemoveBlockEntityTagFix;
import net.minecraft.util.datafix.fixes.ItemRenameFix;
import net.minecraft.util.datafix.fixes.ItemShulkerBoxColorFix;
import net.minecraft.util.datafix.fixes.ItemSpawnEggFix;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;
import net.minecraft.util.datafix.fixes.ItemStackCustomNameToOverrideComponentFix;
import net.minecraft.util.datafix.fixes.ItemStackEnchantmentNamesFix;
import net.minecraft.util.datafix.fixes.ItemStackMapIdFix;
import net.minecraft.util.datafix.fixes.ItemStackSpawnEggFix;
import net.minecraft.util.datafix.fixes.ItemStackTheFlatteningFix;
import net.minecraft.util.datafix.fixes.ItemStackUUIDFix;
import net.minecraft.util.datafix.fixes.ItemWaterPotionFix;
import net.minecraft.util.datafix.fixes.ItemWrittenBookPagesStrictJsonFix;
import net.minecraft.util.datafix.fixes.JigsawPropertiesFix;
import net.minecraft.util.datafix.fixes.JigsawRotationFix;
import net.minecraft.util.datafix.fixes.JukeboxTicksSinceSongStartedFix;
import net.minecraft.util.datafix.fixes.LeavesFix;
import net.minecraft.util.datafix.fixes.LegacyDragonFightFix;
import net.minecraft.util.datafix.fixes.LevelDataGeneratorOptionsFix;
import net.minecraft.util.datafix.fixes.LevelFlatGeneratorInfoFix;
import net.minecraft.util.datafix.fixes.LevelLegacyWorldGenSettingsFix;
import net.minecraft.util.datafix.fixes.LevelUUIDFix;
import net.minecraft.util.datafix.fixes.LodestoneCompassComponentFix;
import net.minecraft.util.datafix.fixes.MapBannerBlockPosFormatFix;
import net.minecraft.util.datafix.fixes.MapIdFix;
import net.minecraft.util.datafix.fixes.MemoryExpiryDataFix;
import net.minecraft.util.datafix.fixes.MissingDimensionFix;
import net.minecraft.util.datafix.fixes.MobEffectIdFix;
import net.minecraft.util.datafix.fixes.MobSpawnerEntityIdentifiersFix;
import net.minecraft.util.datafix.fixes.NamedEntityFix;
import net.minecraft.util.datafix.fixes.NamespacedTypeRenameFix;
import net.minecraft.util.datafix.fixes.NewVillageFix;
import net.minecraft.util.datafix.fixes.ObjectiveDisplayNameFix;
import net.minecraft.util.datafix.fixes.ObjectiveRenderTypeFix;
import net.minecraft.util.datafix.fixes.OminousBannerBlockEntityRenameFix;
import net.minecraft.util.datafix.fixes.OminousBannerRenameFix;
import net.minecraft.util.datafix.fixes.OptionsAccessibilityOnboardFix;
import net.minecraft.util.datafix.fixes.OptionsAddTextBackgroundFix;
import net.minecraft.util.datafix.fixes.OptionsAmbientOcclusionFix;
import net.minecraft.util.datafix.fixes.OptionsForceVBOFix;
import net.minecraft.util.datafix.fixes.OptionsKeyLwjgl3Fix;
import net.minecraft.util.datafix.fixes.OptionsKeyTranslationFix;
import net.minecraft.util.datafix.fixes.OptionsLowerCaseLanguageFix;
import net.minecraft.util.datafix.fixes.OptionsMenuBlurrinessFix;
import net.minecraft.util.datafix.fixes.OptionsProgrammerArtFix;
import net.minecraft.util.datafix.fixes.OptionsRenameFieldFix;
import net.minecraft.util.datafix.fixes.OverreachingTickFix;
import net.minecraft.util.datafix.fixes.ParticleUnflatteningFix;
import net.minecraft.util.datafix.fixes.PlayerHeadBlockProfileFix;
import net.minecraft.util.datafix.fixes.PlayerUUIDFix;
import net.minecraft.util.datafix.fixes.PoiTypeRemoveFix;
import net.minecraft.util.datafix.fixes.PoiTypeRenameFix;
import net.minecraft.util.datafix.fixes.PrimedTntBlockStateFixer;
import net.minecraft.util.datafix.fixes.ProjectileStoredWeaponFix;
import net.minecraft.util.datafix.fixes.RandomSequenceSettingsFix;
import net.minecraft.util.datafix.fixes.RecipesFix;
import net.minecraft.util.datafix.fixes.RecipesRenameningFix;
import net.minecraft.util.datafix.fixes.RedstoneWireConnectionsFix;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.fixes.RemapChunkStatusFix;
import net.minecraft.util.datafix.fixes.RemoveEmptyItemInBrushableBlockFix;
import net.minecraft.util.datafix.fixes.RemoveGolemGossipFix;
import net.minecraft.util.datafix.fixes.RenameEnchantmentsFix;
import net.minecraft.util.datafix.fixes.RenamedCoralFansFix;
import net.minecraft.util.datafix.fixes.RenamedCoralFix;
import net.minecraft.util.datafix.fixes.ReorganizePoi;
import net.minecraft.util.datafix.fixes.SavedDataFeaturePoolElementFix;
import net.minecraft.util.datafix.fixes.SavedDataUUIDFix;
import net.minecraft.util.datafix.fixes.ScoreboardDisplaySlotFix;
import net.minecraft.util.datafix.fixes.SpawnerDataFix;
import net.minecraft.util.datafix.fixes.StatsCounterFix;
import net.minecraft.util.datafix.fixes.StatsRenameFix;
import net.minecraft.util.datafix.fixes.StriderGravityFix;
import net.minecraft.util.datafix.fixes.StructureReferenceCountFix;
import net.minecraft.util.datafix.fixes.StructureSettingsFlattenFix;
import net.minecraft.util.datafix.fixes.StructuresBecomeConfiguredFix;
import net.minecraft.util.datafix.fixes.TeamDisplayNameFix;
import net.minecraft.util.datafix.fixes.TippedArrowPotionToItemFix;
import net.minecraft.util.datafix.fixes.TrappedChestBlockEntityFix;
import net.minecraft.util.datafix.fixes.TrialSpawnerConfigFix;
import net.minecraft.util.datafix.fixes.VariantRenameFix;
import net.minecraft.util.datafix.fixes.VillagerDataFix;
import net.minecraft.util.datafix.fixes.VillagerFollowRangeFix;
import net.minecraft.util.datafix.fixes.VillagerRebuildLevelAndXpFix;
import net.minecraft.util.datafix.fixes.VillagerTradeFix;
import net.minecraft.util.datafix.fixes.WallPropertyFix;
import net.minecraft.util.datafix.fixes.WeaponSmithChestLootTableFix;
import net.minecraft.util.datafix.fixes.WorldGenSettingsDisallowOldCustomWorldsFix;
import net.minecraft.util.datafix.fixes.WorldGenSettingsFix;
import net.minecraft.util.datafix.fixes.WorldGenSettingsHeightAndBiomeFix;
import net.minecraft.util.datafix.fixes.WriteAndReadFix;
import net.minecraft.util.datafix.fixes.ZombieVillagerRebuildXpFix;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import net.minecraft.util.datafix.schemas.V100;
import net.minecraft.util.datafix.schemas.V102;
import net.minecraft.util.datafix.schemas.V1022;
import net.minecraft.util.datafix.schemas.V106;
import net.minecraft.util.datafix.schemas.V107;
import net.minecraft.util.datafix.schemas.V1125;
import net.minecraft.util.datafix.schemas.V135;
import net.minecraft.util.datafix.schemas.V143;
import net.minecraft.util.datafix.schemas.V1451;
import net.minecraft.util.datafix.schemas.V1451_1;
import net.minecraft.util.datafix.schemas.V1451_2;
import net.minecraft.util.datafix.schemas.V1451_3;
import net.minecraft.util.datafix.schemas.V1451_4;
import net.minecraft.util.datafix.schemas.V1451_5;
import net.minecraft.util.datafix.schemas.V1451_6;
import net.minecraft.util.datafix.schemas.V1460;
import net.minecraft.util.datafix.schemas.V1466;
import net.minecraft.util.datafix.schemas.V1470;
import net.minecraft.util.datafix.schemas.V1481;
import net.minecraft.util.datafix.schemas.V1483;
import net.minecraft.util.datafix.schemas.V1486;
import net.minecraft.util.datafix.schemas.V1510;
import net.minecraft.util.datafix.schemas.V1800;
import net.minecraft.util.datafix.schemas.V1801;
import net.minecraft.util.datafix.schemas.V1904;
import net.minecraft.util.datafix.schemas.V1906;
import net.minecraft.util.datafix.schemas.V1909;
import net.minecraft.util.datafix.schemas.V1920;
import net.minecraft.util.datafix.schemas.V1928;
import net.minecraft.util.datafix.schemas.V1929;
import net.minecraft.util.datafix.schemas.V1931;
import net.minecraft.util.datafix.schemas.V2100;
import net.minecraft.util.datafix.schemas.V2501;
import net.minecraft.util.datafix.schemas.V2502;
import net.minecraft.util.datafix.schemas.V2505;
import net.minecraft.util.datafix.schemas.V2509;
import net.minecraft.util.datafix.schemas.V2519;
import net.minecraft.util.datafix.schemas.V2522;
import net.minecraft.util.datafix.schemas.V2551;
import net.minecraft.util.datafix.schemas.V2568;
import net.minecraft.util.datafix.schemas.V2571;
import net.minecraft.util.datafix.schemas.V2684;
import net.minecraft.util.datafix.schemas.V2686;
import net.minecraft.util.datafix.schemas.V2688;
import net.minecraft.util.datafix.schemas.V2704;
import net.minecraft.util.datafix.schemas.V2707;
import net.minecraft.util.datafix.schemas.V2831;
import net.minecraft.util.datafix.schemas.V2832;
import net.minecraft.util.datafix.schemas.V2842;
import net.minecraft.util.datafix.schemas.V3076;
import net.minecraft.util.datafix.schemas.V3078;
import net.minecraft.util.datafix.schemas.V3081;
import net.minecraft.util.datafix.schemas.V3082;
import net.minecraft.util.datafix.schemas.V3083;
import net.minecraft.util.datafix.schemas.V3202;
import net.minecraft.util.datafix.schemas.V3203;
import net.minecraft.util.datafix.schemas.V3204;
import net.minecraft.util.datafix.schemas.V3325;
import net.minecraft.util.datafix.schemas.V3326;
import net.minecraft.util.datafix.schemas.V3327;
import net.minecraft.util.datafix.schemas.V3328;
import net.minecraft.util.datafix.schemas.V3438;
import net.minecraft.util.datafix.schemas.V3448;
import net.minecraft.util.datafix.schemas.V3682;
import net.minecraft.util.datafix.schemas.V3683;
import net.minecraft.util.datafix.schemas.V3685;
import net.minecraft.util.datafix.schemas.V3689;
import net.minecraft.util.datafix.schemas.V3799;
import net.minecraft.util.datafix.schemas.V3807;
import net.minecraft.util.datafix.schemas.V3808;
import net.minecraft.util.datafix.schemas.V3808_1;
import net.minecraft.util.datafix.schemas.V3808_2;
import net.minecraft.util.datafix.schemas.V3816;
import net.minecraft.util.datafix.schemas.V3818;
import net.minecraft.util.datafix.schemas.V3818_3;
import net.minecraft.util.datafix.schemas.V3818_4;
import net.minecraft.util.datafix.schemas.V3818_5;
import net.minecraft.util.datafix.schemas.V3825;
import net.minecraft.util.datafix.schemas.V3938;
import net.minecraft.util.datafix.schemas.V501;
import net.minecraft.util.datafix.schemas.V700;
import net.minecraft.util.datafix.schemas.V701;
import net.minecraft.util.datafix.schemas.V702;
import net.minecraft.util.datafix.schemas.V703;
import net.minecraft.util.datafix.schemas.V704;
import net.minecraft.util.datafix.schemas.V705;
import net.minecraft.util.datafix.schemas.V808;
import net.minecraft.util.datafix.schemas.V99;

public class DataFixers
{
    private static final BiFunction<Integer, Schema, Schema> SAME = Schema::new;
    private static final BiFunction<Integer, Schema, Schema> SAME_NAMESPACED = NamespacedSchema::new;
    private static final Result DATA_FIXER = createFixerUpper();
    public static final int BLENDING_VERSION = 3441;

    private DataFixers()
    {
    }

    public static DataFixer getDataFixer()
    {
        return DATA_FIXER.fixer();
    }

    private static Result createFixerUpper()
    {
        DataFixerBuilder datafixerbuilder = new DataFixerBuilder(SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        addFixers(datafixerbuilder);
        return datafixerbuilder.build();
    }

    public static CompletableFuture<?> optimize(Set<TypeReference> p_344748_)
    {
        if (p_344748_.isEmpty())
        {
            return CompletableFuture.completedFuture(null);
        }
        else
        {
            Executor executor = Executors.newSingleThreadExecutor(
                                    new ThreadFactoryBuilder().setNameFormat("Datafixer Bootstrap").setDaemon(true).setPriority(1).build()
                                );
            return DATA_FIXER.optimize(p_344748_, executor);
        }
    }

    private static void addFixers(DataFixerBuilder p_14514_)
    {
        p_14514_.addSchema(99, V99::new);
        Schema schema = p_14514_.addSchema(100, V100::new);
        p_14514_.addFixer(new EntityEquipmentToArmorAndHandFix(schema, true));
        Schema schema1 = p_14514_.addSchema(101, SAME);
        p_14514_.addFixer(new BlockEntitySignTextStrictJsonFix(schema1, false));
        Schema schema2 = p_14514_.addSchema(102, V102::new);
        p_14514_.addFixer(new ItemIdFix(schema2, true));
        p_14514_.addFixer(new ItemPotionFix(schema2, false));
        Schema schema3 = p_14514_.addSchema(105, SAME);
        p_14514_.addFixer(new ItemSpawnEggFix(schema3, true));
        Schema schema4 = p_14514_.addSchema(106, V106::new);
        p_14514_.addFixer(new MobSpawnerEntityIdentifiersFix(schema4, true));
        Schema schema5 = p_14514_.addSchema(107, V107::new);
        p_14514_.addFixer(new EntityMinecartIdentifiersFix(schema5));
        Schema schema6 = p_14514_.addSchema(108, SAME);
        p_14514_.addFixer(new EntityStringUuidFix(schema6, true));
        Schema schema7 = p_14514_.addSchema(109, SAME);
        p_14514_.addFixer(new EntityHealthFix(schema7, true));
        Schema schema8 = p_14514_.addSchema(110, SAME);
        p_14514_.addFixer(new EntityHorseSaddleFix(schema8, true));
        Schema schema9 = p_14514_.addSchema(111, SAME);
        p_14514_.addFixer(new EntityPaintingItemFrameDirectionFix(schema9, true));
        Schema schema10 = p_14514_.addSchema(113, SAME);
        p_14514_.addFixer(new EntityRedundantChanceTagsFix(schema10, true));
        Schema schema11 = p_14514_.addSchema(135, V135::new);
        p_14514_.addFixer(new EntityRidingToPassengersFix(schema11, true));
        Schema schema12 = p_14514_.addSchema(143, V143::new);
        p_14514_.addFixer(new EntityTippedArrowFix(schema12, true));
        Schema schema13 = p_14514_.addSchema(147, SAME);
        p_14514_.addFixer(new EntityArmorStandSilentFix(schema13, true));
        Schema schema14 = p_14514_.addSchema(165, SAME);
        p_14514_.addFixer(new ItemWrittenBookPagesStrictJsonFix(schema14, true));
        Schema schema15 = p_14514_.addSchema(501, V501::new);
        p_14514_.addFixer(new AddNewChoices(schema15, "Add 1.10 entities fix", References.ENTITY));
        Schema schema16 = p_14514_.addSchema(502, SAME);
        p_14514_.addFixer(
            ItemRenameFix.create(
                schema16,
                "cooked_fished item renamer",
                p_14533_ -> Objects.equals(NamespacedSchema.ensureNamespaced(p_14533_), "minecraft:cooked_fished") ? "minecraft:cooked_fish" : p_14533_
            )
        );
        p_14514_.addFixer(new EntityZombieVillagerTypeFix(schema16, false));
        Schema schema17 = p_14514_.addSchema(505, SAME);
        p_14514_.addFixer(new OptionsForceVBOFix(schema17, false));
        Schema schema18 = p_14514_.addSchema(700, V700::new);
        p_14514_.addFixer(new EntityElderGuardianSplitFix(schema18, true));
        Schema schema19 = p_14514_.addSchema(701, V701::new);
        p_14514_.addFixer(new EntitySkeletonSplitFix(schema19, true));
        Schema schema20 = p_14514_.addSchema(702, V702::new);
        p_14514_.addFixer(new EntityZombieSplitFix(schema20));
        Schema schema21 = p_14514_.addSchema(703, V703::new);
        p_14514_.addFixer(new EntityHorseSplitFix(schema21, true));
        Schema schema22 = p_14514_.addSchema(704, V704::new);
        p_14514_.addFixer(new BlockEntityIdFix(schema22, true));
        Schema schema23 = p_14514_.addSchema(705, V705::new);
        p_14514_.addFixer(new EntityIdFix(schema23, true));
        Schema schema24 = p_14514_.addSchema(804, SAME_NAMESPACED);
        p_14514_.addFixer(new ItemBannerColorFix(schema24, true));
        Schema schema25 = p_14514_.addSchema(806, SAME_NAMESPACED);
        p_14514_.addFixer(new ItemWaterPotionFix(schema25, false));
        Schema schema26 = p_14514_.addSchema(808, V808::new);
        p_14514_.addFixer(new AddNewChoices(schema26, "added shulker box", References.BLOCK_ENTITY));
        Schema schema27 = p_14514_.addSchema(808, 1, SAME_NAMESPACED);
        p_14514_.addFixer(new EntityShulkerColorFix(schema27, false));
        Schema schema28 = p_14514_.addSchema(813, SAME_NAMESPACED);
        p_14514_.addFixer(new ItemShulkerBoxColorFix(schema28, false));
        p_14514_.addFixer(new BlockEntityShulkerBoxColorFix(schema28, false));
        Schema schema29 = p_14514_.addSchema(816, SAME_NAMESPACED);
        p_14514_.addFixer(new OptionsLowerCaseLanguageFix(schema29, false));
        Schema schema30 = p_14514_.addSchema(820, SAME_NAMESPACED);
        p_14514_.addFixer(ItemRenameFix.create(schema30, "totem item renamer", createRenamer("minecraft:totem", "minecraft:totem_of_undying")));
        Schema schema31 = p_14514_.addSchema(1022, V1022::new);
        p_14514_.addFixer(new WriteAndReadFix(schema31, "added shoulder entities to players", References.PLAYER));
        Schema schema32 = p_14514_.addSchema(1125, V1125::new);
        p_14514_.addFixer(new ChunkBedBlockEntityInjecterFix(schema32, true));
        p_14514_.addFixer(new BedItemColorFix(schema32, false));
        Schema schema33 = p_14514_.addSchema(1344, SAME_NAMESPACED);
        p_14514_.addFixer(new OptionsKeyLwjgl3Fix(schema33, false));
        Schema schema34 = p_14514_.addSchema(1446, SAME_NAMESPACED);
        p_14514_.addFixer(new OptionsKeyTranslationFix(schema34, false));
        Schema schema35 = p_14514_.addSchema(1450, SAME_NAMESPACED);
        p_14514_.addFixer(new BlockStateStructureTemplateFix(schema35, false));
        Schema schema36 = p_14514_.addSchema(1451, V1451::new);
        p_14514_.addFixer(new AddNewChoices(schema36, "AddTrappedChestFix", References.BLOCK_ENTITY));
        Schema schema37 = p_14514_.addSchema(1451, 1, V1451_1::new);
        p_14514_.addFixer(new ChunkPalettedStorageFix(schema37, true));
        Schema schema38 = p_14514_.addSchema(1451, 2, V1451_2::new);
        p_14514_.addFixer(new BlockEntityBlockStateFix(schema38, true));
        Schema schema39 = p_14514_.addSchema(1451, 3, V1451_3::new);
        p_14514_.addFixer(new EntityBlockStateFix(schema39, true));
        p_14514_.addFixer(new ItemStackMapIdFix(schema39, false));
        Schema schema40 = p_14514_.addSchema(1451, 4, V1451_4::new);
        p_14514_.addFixer(new BlockNameFlatteningFix(schema40, true));
        p_14514_.addFixer(new ItemStackTheFlatteningFix(schema40, false));
        Schema schema41 = p_14514_.addSchema(1451, 5, V1451_5::new);
        p_14514_.addFixer(
            new ItemRemoveBlockEntityTagFix(
                schema41,
                false,
                Set.of(
                    "minecraft:note_block",
                    "minecraft:flower_pot",
                    "minecraft:dandelion",
                    "minecraft:poppy",
                    "minecraft:blue_orchid",
                    "minecraft:allium",
                    "minecraft:azure_bluet",
                    "minecraft:red_tulip",
                    "minecraft:orange_tulip",
                    "minecraft:white_tulip",
                    "minecraft:pink_tulip",
                    "minecraft:oxeye_daisy",
                    "minecraft:cactus",
                    "minecraft:brown_mushroom",
                    "minecraft:red_mushroom",
                    "minecraft:oak_sapling",
                    "minecraft:spruce_sapling",
                    "minecraft:birch_sapling",
                    "minecraft:jungle_sapling",
                    "minecraft:acacia_sapling",
                    "minecraft:dark_oak_sapling",
                    "minecraft:dead_bush",
                    "minecraft:fern"
                )
            )
        );
        p_14514_.addFixer(new AddNewChoices(schema41, "RemoveNoteBlockFlowerPotFix", References.BLOCK_ENTITY));
        p_14514_.addFixer(new ItemStackSpawnEggFix(schema41, false, "minecraft:spawn_egg"));
        p_14514_.addFixer(new EntityWolfColorFix(schema41, false));
        p_14514_.addFixer(new BlockEntityBannerColorFix(schema41, false));
        p_14514_.addFixer(new LevelFlatGeneratorInfoFix(schema41, false));
        Schema schema42 = p_14514_.addSchema(1451, 6, V1451_6::new);
        p_14514_.addFixer(new StatsCounterFix(schema42, true));
        p_14514_.addFixer(new BlockEntityJukeboxFix(schema42, false));
        Schema schema43 = p_14514_.addSchema(1451, 7, SAME_NAMESPACED);
        p_14514_.addFixer(new VillagerTradeFix(schema43));
        Schema schema44 = p_14514_.addSchema(1456, SAME_NAMESPACED);
        p_14514_.addFixer(new EntityItemFrameDirectionFix(schema44, false));
        Schema schema45 = p_14514_.addSchema(1458, SAME_NAMESPACED);
        p_14514_.addFixer(new EntityCustomNameToComponentFix(schema45, false));
        p_14514_.addFixer(new ItemCustomNameToComponentFix(schema45, false));
        p_14514_.addFixer(new BlockEntityCustomNameToComponentFix(schema45, false));
        Schema schema46 = p_14514_.addSchema(1460, V1460::new);
        p_14514_.addFixer(new EntityPaintingMotiveFix(schema46, false));
        Schema schema47 = p_14514_.addSchema(1466, V1466::new);
        p_14514_.addFixer(new AddNewChoices(schema47, "Add DUMMY block entity", References.BLOCK_ENTITY));
        p_14514_.addFixer(new ChunkToProtochunkFix(schema47, true));
        Schema schema48 = p_14514_.addSchema(1470, V1470::new);
        p_14514_.addFixer(new AddNewChoices(schema48, "Add 1.13 entities fix", References.ENTITY));
        Schema schema49 = p_14514_.addSchema(1474, SAME_NAMESPACED);
        p_14514_.addFixer(new ColorlessShulkerEntityFix(schema49, false));
        p_14514_.addFixer(
            BlockRenameFix.create(
                schema49,
                "Colorless shulker block fixer",
                p_14531_ -> Objects.equals(NamespacedSchema.ensureNamespaced(p_14531_), "minecraft:purple_shulker_box") ? "minecraft:shulker_box" : p_14531_
            )
        );
        p_14514_.addFixer(
            ItemRenameFix.create(
                schema49,
                "Colorless shulker item fixer",
                p_14516_ -> Objects.equals(NamespacedSchema.ensureNamespaced(p_14516_), "minecraft:purple_shulker_box") ? "minecraft:shulker_box" : p_14516_
            )
        );
        Schema schema50 = p_14514_.addSchema(1475, SAME_NAMESPACED);
        p_14514_.addFixer(
            BlockRenameFix.create(
                schema50, "Flowing fixer", createRenamer(ImmutableMap.of("minecraft:flowing_water", "minecraft:water", "minecraft:flowing_lava", "minecraft:lava"))
            )
        );
        Schema schema51 = p_14514_.addSchema(1480, SAME_NAMESPACED);
        p_14514_.addFixer(BlockRenameFix.create(schema51, "Rename coral blocks", createRenamer(RenamedCoralFix.RENAMED_IDS)));
        p_14514_.addFixer(ItemRenameFix.create(schema51, "Rename coral items", createRenamer(RenamedCoralFix.RENAMED_IDS)));
        Schema schema52 = p_14514_.addSchema(1481, V1481::new);
        p_14514_.addFixer(new AddNewChoices(schema52, "Add conduit", References.BLOCK_ENTITY));
        Schema schema53 = p_14514_.addSchema(1483, V1483::new);
        p_14514_.addFixer(new EntityPufferfishRenameFix(schema53, true));
        p_14514_.addFixer(ItemRenameFix.create(schema53, "Rename pufferfish egg item", createRenamer(EntityPufferfishRenameFix.RENAMED_IDS)));
        Schema schema54 = p_14514_.addSchema(1484, SAME_NAMESPACED);
        p_14514_.addFixer(
            ItemRenameFix.create(
                schema54,
                "Rename seagrass items",
                createRenamer(ImmutableMap.of("minecraft:sea_grass", "minecraft:seagrass", "minecraft:tall_sea_grass", "minecraft:tall_seagrass"))
            )
        );
        p_14514_.addFixer(
            BlockRenameFix.create(
                schema54,
                "Rename seagrass blocks",
                createRenamer(ImmutableMap.of("minecraft:sea_grass", "minecraft:seagrass", "minecraft:tall_sea_grass", "minecraft:tall_seagrass"))
            )
        );
        p_14514_.addFixer(new HeightmapRenamingFix(schema54, false));
        Schema schema55 = p_14514_.addSchema(1486, V1486::new);
        p_14514_.addFixer(new EntityCodSalmonFix(schema55, true));
        p_14514_.addFixer(ItemRenameFix.create(schema55, "Rename cod/salmon egg items", createRenamer(EntityCodSalmonFix.RENAMED_EGG_IDS)));
        Schema schema56 = p_14514_.addSchema(1487, SAME_NAMESPACED);
        p_14514_.addFixer(
            ItemRenameFix.create(
                schema56,
                "Rename prismarine_brick(s)_* blocks",
                createRenamer(
                    ImmutableMap.of(
                        "minecraft:prismarine_bricks_slab",
                        "minecraft:prismarine_brick_slab",
                        "minecraft:prismarine_bricks_stairs",
                        "minecraft:prismarine_brick_stairs"
                    )
                )
            )
        );
        p_14514_.addFixer(
            BlockRenameFix.create(
                schema56,
                "Rename prismarine_brick(s)_* items",
                createRenamer(
                    ImmutableMap.of(
                        "minecraft:prismarine_bricks_slab",
                        "minecraft:prismarine_brick_slab",
                        "minecraft:prismarine_bricks_stairs",
                        "minecraft:prismarine_brick_stairs"
                    )
                )
            )
        );
        Schema schema57 = p_14514_.addSchema(1488, SAME_NAMESPACED);
        p_14514_.addFixer(
            BlockRenameFix.create(
                schema57, "Rename kelp/kelptop", createRenamer(ImmutableMap.of("minecraft:kelp_top", "minecraft:kelp", "minecraft:kelp", "minecraft:kelp_plant"))
            )
        );
        p_14514_.addFixer(ItemRenameFix.create(schema57, "Rename kelptop", createRenamer("minecraft:kelp_top", "minecraft:kelp")));
        p_14514_.addFixer(new NamedEntityFix(schema57, false, "Command block block entity custom name fix", References.BLOCK_ENTITY, "minecraft:command_block")
        {
            @Override
            protected Typed<?> fix(Typed<?> p_14541_)
            {
                return p_14541_.update(DSL.remainderFinder(), EntityCustomNameToComponentFix::fixTagCustomName);
            }
        });
        p_14514_.addFixer(
            new NamedEntityFix(schema57, false, "Command block minecart custom name fix", References.ENTITY, "minecraft:commandblock_minecart")
        {
            @Override
            protected Typed<?> fix(Typed<?> p_14549_)
            {
                return p_14549_.update(DSL.remainderFinder(), EntityCustomNameToComponentFix::fixTagCustomName);
            }
        }
        );
        p_14514_.addFixer(new IglooMetadataRemovalFix(schema57, false));
        Schema schema58 = p_14514_.addSchema(1490, SAME_NAMESPACED);
        p_14514_.addFixer(BlockRenameFix.create(schema58, "Rename melon_block", createRenamer("minecraft:melon_block", "minecraft:melon")));
        p_14514_.addFixer(
            ItemRenameFix.create(
                schema58,
                "Rename melon_block/melon/speckled_melon",
                createRenamer(
                    ImmutableMap.of(
                        "minecraft:melon_block",
                        "minecraft:melon",
                        "minecraft:melon",
                        "minecraft:melon_slice",
                        "minecraft:speckled_melon",
                        "minecraft:glistering_melon_slice"
                    )
                )
            )
        );
        Schema schema59 = p_14514_.addSchema(1492, SAME_NAMESPACED);
        p_14514_.addFixer(new ChunkStructuresTemplateRenameFix(schema59, false));
        Schema schema60 = p_14514_.addSchema(1494, SAME_NAMESPACED);
        p_14514_.addFixer(new ItemStackEnchantmentNamesFix(schema60, false));
        Schema schema61 = p_14514_.addSchema(1496, SAME_NAMESPACED);
        p_14514_.addFixer(new LeavesFix(schema61, false));
        Schema schema62 = p_14514_.addSchema(1500, SAME_NAMESPACED);
        p_14514_.addFixer(new BlockEntityKeepPacked(schema62, false));
        Schema schema63 = p_14514_.addSchema(1501, SAME_NAMESPACED);
        p_14514_.addFixer(new AdvancementsFix(schema63, false));
        Schema schema64 = p_14514_.addSchema(1502, SAME_NAMESPACED);
        p_14514_.addFixer(new NamespacedTypeRenameFix(schema64, "Recipes fix", References.RECIPE, createRenamer(RecipesFix.RECIPES)));
        Schema schema65 = p_14514_.addSchema(1506, SAME_NAMESPACED);
        p_14514_.addFixer(new LevelDataGeneratorOptionsFix(schema65, false));
        Schema schema66 = p_14514_.addSchema(1510, V1510::new);
        p_14514_.addFixer(BlockRenameFix.create(schema66, "Block renamening fix", createRenamer(EntityTheRenameningFix.RENAMED_BLOCKS)));
        p_14514_.addFixer(ItemRenameFix.create(schema66, "Item renamening fix", createRenamer(EntityTheRenameningFix.RENAMED_ITEMS)));
        p_14514_.addFixer(new NamespacedTypeRenameFix(schema66, "Recipes renamening fix", References.RECIPE, createRenamer(RecipesRenameningFix.RECIPES)));
        p_14514_.addFixer(new EntityTheRenameningFix(schema66, true));
        p_14514_.addFixer(
            new StatsRenameFix(
                schema66,
                "SwimStatsRenameFix",
                ImmutableMap.of("minecraft:swim_one_cm", "minecraft:walk_on_water_one_cm", "minecraft:dive_one_cm", "minecraft:walk_under_water_one_cm")
            )
        );
        Schema schema67 = p_14514_.addSchema(1514, SAME_NAMESPACED);
        p_14514_.addFixer(new ObjectiveDisplayNameFix(schema67, false));
        p_14514_.addFixer(new TeamDisplayNameFix(schema67, false));
        p_14514_.addFixer(new ObjectiveRenderTypeFix(schema67, false));
        Schema schema68 = p_14514_.addSchema(1515, SAME_NAMESPACED);
        p_14514_.addFixer(BlockRenameFix.create(schema68, "Rename coral fan blocks", createRenamer(RenamedCoralFansFix.RENAMED_IDS)));
        Schema schema69 = p_14514_.addSchema(1624, SAME_NAMESPACED);
        p_14514_.addFixer(new TrappedChestBlockEntityFix(schema69, false));
        Schema schema70 = p_14514_.addSchema(1800, V1800::new);
        p_14514_.addFixer(new AddNewChoices(schema70, "Added 1.14 mobs fix", References.ENTITY));
        p_14514_.addFixer(ItemRenameFix.create(schema70, "Rename dye items", createRenamer(DyeItemRenameFix.RENAMED_IDS)));
        Schema schema71 = p_14514_.addSchema(1801, V1801::new);
        p_14514_.addFixer(new AddNewChoices(schema71, "Added Illager Beast", References.ENTITY));
        Schema schema72 = p_14514_.addSchema(1802, SAME_NAMESPACED);
        p_14514_.addFixer(
            BlockRenameFix.create(
                schema72,
                "Rename sign blocks & stone slabs",
                createRenamer(
                    ImmutableMap.of(
                        "minecraft:stone_slab",
                        "minecraft:smooth_stone_slab",
                        "minecraft:sign",
                        "minecraft:oak_sign",
                        "minecraft:wall_sign",
                        "minecraft:oak_wall_sign"
                    )
                )
            )
        );
        p_14514_.addFixer(
            ItemRenameFix.create(
                schema72,
                "Rename sign item & stone slabs",
                createRenamer(ImmutableMap.of("minecraft:stone_slab", "minecraft:smooth_stone_slab", "minecraft:sign", "minecraft:oak_sign"))
            )
        );
        Schema schema73 = p_14514_.addSchema(1803, SAME_NAMESPACED);
        p_14514_.addFixer(new ItemLoreFix(schema73, false));
        Schema schema74 = p_14514_.addSchema(1904, V1904::new);
        p_14514_.addFixer(new AddNewChoices(schema74, "Added Cats", References.ENTITY));
        p_14514_.addFixer(new EntityCatSplitFix(schema74, false));
        Schema schema75 = p_14514_.addSchema(1905, SAME_NAMESPACED);
        p_14514_.addFixer(new ChunkStatusFix(schema75, false));
        Schema schema76 = p_14514_.addSchema(1906, V1906::new);
        p_14514_.addFixer(new AddNewChoices(schema76, "Add POI Blocks", References.BLOCK_ENTITY));
        Schema schema77 = p_14514_.addSchema(1909, V1909::new);
        p_14514_.addFixer(new AddNewChoices(schema77, "Add jigsaw", References.BLOCK_ENTITY));
        Schema schema78 = p_14514_.addSchema(1911, SAME_NAMESPACED);
        p_14514_.addFixer(new ChunkStatusFix2(schema78, false));
        Schema schema79 = p_14514_.addSchema(1914, SAME_NAMESPACED);
        p_14514_.addFixer(new WeaponSmithChestLootTableFix(schema79, false));
        Schema schema80 = p_14514_.addSchema(1917, SAME_NAMESPACED);
        p_14514_.addFixer(new CatTypeFix(schema80, false));
        Schema schema81 = p_14514_.addSchema(1918, SAME_NAMESPACED);
        p_14514_.addFixer(new VillagerDataFix(schema81, "minecraft:villager"));
        p_14514_.addFixer(new VillagerDataFix(schema81, "minecraft:zombie_villager"));
        Schema schema82 = p_14514_.addSchema(1920, V1920::new);
        p_14514_.addFixer(new NewVillageFix(schema82, false));
        p_14514_.addFixer(new AddNewChoices(schema82, "Add campfire", References.BLOCK_ENTITY));
        Schema schema83 = p_14514_.addSchema(1925, SAME_NAMESPACED);
        p_14514_.addFixer(new MapIdFix(schema83, false));
        Schema schema84 = p_14514_.addSchema(1928, V1928::new);
        p_14514_.addFixer(new EntityRavagerRenameFix(schema84, true));
        p_14514_.addFixer(ItemRenameFix.create(schema84, "Rename ravager egg item", createRenamer(EntityRavagerRenameFix.RENAMED_IDS)));
        Schema schema85 = p_14514_.addSchema(1929, V1929::new);
        p_14514_.addFixer(new AddNewChoices(schema85, "Add Wandering Trader and Trader Llama", References.ENTITY));
        Schema schema86 = p_14514_.addSchema(1931, V1931::new);
        p_14514_.addFixer(new AddNewChoices(schema86, "Added Fox", References.ENTITY));
        Schema schema87 = p_14514_.addSchema(1936, SAME_NAMESPACED);
        p_14514_.addFixer(new OptionsAddTextBackgroundFix(schema87, false));
        Schema schema88 = p_14514_.addSchema(1946, SAME_NAMESPACED);
        p_14514_.addFixer(new ReorganizePoi(schema88, false));
        Schema schema89 = p_14514_.addSchema(1948, SAME_NAMESPACED);
        p_14514_.addFixer(new OminousBannerRenameFix(schema89));
        Schema schema90 = p_14514_.addSchema(1953, SAME_NAMESPACED);
        p_14514_.addFixer(new OminousBannerBlockEntityRenameFix(schema90, false));
        Schema schema91 = p_14514_.addSchema(1955, SAME_NAMESPACED);
        p_14514_.addFixer(new VillagerRebuildLevelAndXpFix(schema91, false));
        p_14514_.addFixer(new ZombieVillagerRebuildXpFix(schema91, false));
        Schema schema92 = p_14514_.addSchema(1961, SAME_NAMESPACED);
        p_14514_.addFixer(new ChunkLightRemoveFix(schema92, false));
        Schema schema93 = p_14514_.addSchema(1963, SAME_NAMESPACED);
        p_14514_.addFixer(new RemoveGolemGossipFix(schema93, false));
        Schema schema94 = p_14514_.addSchema(2100, V2100::new);
        p_14514_.addFixer(new AddNewChoices(schema94, "Added Bee and Bee Stinger", References.ENTITY));
        p_14514_.addFixer(new AddNewChoices(schema94, "Add beehive", References.BLOCK_ENTITY));
        p_14514_.addFixer(
            new NamespacedTypeRenameFix(schema94, "Rename sugar recipe", References.RECIPE, createRenamer("minecraft:sugar", "minecraft:sugar_from_sugar_cane"))
        );
        p_14514_.addFixer(
            new AdvancementsRenameFix(
                schema94, false, "Rename sugar recipe advancement", createRenamer("minecraft:recipes/misc/sugar", "minecraft:recipes/misc/sugar_from_sugar_cane")
            )
        );
        Schema schema95 = p_14514_.addSchema(2202, SAME_NAMESPACED);
        p_14514_.addFixer(new ChunkBiomeFix(schema95, false));
        Schema schema96 = p_14514_.addSchema(2209, SAME_NAMESPACED);
        UnaryOperator<String> unaryoperator = createRenamer("minecraft:bee_hive", "minecraft:beehive");
        p_14514_.addFixer(ItemRenameFix.create(schema96, "Rename bee_hive item to beehive", unaryoperator));
        p_14514_.addFixer(new PoiTypeRenameFix(schema96, "Rename bee_hive poi to beehive", unaryoperator));
        p_14514_.addFixer(BlockRenameFix.create(schema96, "Rename bee_hive block to beehive", unaryoperator));
        Schema schema97 = p_14514_.addSchema(2211, SAME_NAMESPACED);
        p_14514_.addFixer(new StructureReferenceCountFix(schema97, false));
        Schema schema98 = p_14514_.addSchema(2218, SAME_NAMESPACED);
        p_14514_.addFixer(new ForcePoiRebuild(schema98, false));
        Schema schema99 = p_14514_.addSchema(2501, V2501::new);
        p_14514_.addFixer(new FurnaceRecipeFix(schema99, true));
        Schema schema100 = p_14514_.addSchema(2502, V2502::new);
        p_14514_.addFixer(new AddNewChoices(schema100, "Added Hoglin", References.ENTITY));
        Schema schema101 = p_14514_.addSchema(2503, SAME_NAMESPACED);
        p_14514_.addFixer(new WallPropertyFix(schema101, false));
        p_14514_.addFixer(
            new AdvancementsRenameFix(
                schema101, false, "Composter category change", createRenamer("minecraft:recipes/misc/composter", "minecraft:recipes/decorations/composter")
            )
        );
        Schema schema102 = p_14514_.addSchema(2505, V2505::new);
        p_14514_.addFixer(new AddNewChoices(schema102, "Added Piglin", References.ENTITY));
        p_14514_.addFixer(new MemoryExpiryDataFix(schema102, "minecraft:villager"));
        Schema schema103 = p_14514_.addSchema(2508, SAME_NAMESPACED);
        p_14514_.addFixer(
            ItemRenameFix.create(
                schema103,
                "Renamed fungi items to fungus",
                createRenamer(ImmutableMap.of("minecraft:warped_fungi", "minecraft:warped_fungus", "minecraft:crimson_fungi", "minecraft:crimson_fungus"))
            )
        );
        p_14514_.addFixer(
            BlockRenameFix.create(
                schema103,
                "Renamed fungi blocks to fungus",
                createRenamer(ImmutableMap.of("minecraft:warped_fungi", "minecraft:warped_fungus", "minecraft:crimson_fungi", "minecraft:crimson_fungus"))
            )
        );
        Schema schema104 = p_14514_.addSchema(2509, V2509::new);
        p_14514_.addFixer(new EntityZombifiedPiglinRenameFix(schema104));
        p_14514_.addFixer(ItemRenameFix.create(schema104, "Rename zombie pigman egg item", createRenamer(EntityZombifiedPiglinRenameFix.RENAMED_IDS)));
        Schema schema105 = p_14514_.addSchema(2511, SAME_NAMESPACED);
        p_14514_.addFixer(new EntityProjectileOwnerFix(schema105));
        Schema schema106 = p_14514_.addSchema(2514, SAME_NAMESPACED);
        p_14514_.addFixer(new EntityUUIDFix(schema106));
        p_14514_.addFixer(new BlockEntityUUIDFix(schema106));
        p_14514_.addFixer(new PlayerUUIDFix(schema106));
        p_14514_.addFixer(new LevelUUIDFix(schema106));
        p_14514_.addFixer(new SavedDataUUIDFix(schema106));
        p_14514_.addFixer(new ItemStackUUIDFix(schema106));
        Schema schema107 = p_14514_.addSchema(2516, SAME_NAMESPACED);
        p_14514_.addFixer(new GossipUUIDFix(schema107, "minecraft:villager"));
        p_14514_.addFixer(new GossipUUIDFix(schema107, "minecraft:zombie_villager"));
        Schema schema108 = p_14514_.addSchema(2518, SAME_NAMESPACED);
        p_14514_.addFixer(new JigsawPropertiesFix(schema108, false));
        p_14514_.addFixer(new JigsawRotationFix(schema108, false));
        Schema schema109 = p_14514_.addSchema(2519, V2519::new);
        p_14514_.addFixer(new AddNewChoices(schema109, "Added Strider", References.ENTITY));
        Schema schema110 = p_14514_.addSchema(2522, V2522::new);
        p_14514_.addFixer(new AddNewChoices(schema110, "Added Zoglin", References.ENTITY));
        Schema schema111 = p_14514_.addSchema(2523, SAME_NAMESPACED);
        p_14514_.addFixer(
            new AttributesRename(
                schema111,
                "Attribute renames",
                createRenamerNoNamespace(
                    ImmutableMap.<String, String>builder()
                    .put("generic.maxHealth", "minecraft:generic.max_health")
                    .put("Max Health", "minecraft:generic.max_health")
                    .put("zombie.spawnReinforcements", "minecraft:zombie.spawn_reinforcements")
                    .put("Spawn Reinforcements Chance", "minecraft:zombie.spawn_reinforcements")
                    .put("horse.jumpStrength", "minecraft:horse.jump_strength")
                    .put("Jump Strength", "minecraft:horse.jump_strength")
                    .put("generic.followRange", "minecraft:generic.follow_range")
                    .put("Follow Range", "minecraft:generic.follow_range")
                    .put("generic.knockbackResistance", "minecraft:generic.knockback_resistance")
                    .put("Knockback Resistance", "minecraft:generic.knockback_resistance")
                    .put("generic.movementSpeed", "minecraft:generic.movement_speed")
                    .put("Movement Speed", "minecraft:generic.movement_speed")
                    .put("generic.flyingSpeed", "minecraft:generic.flying_speed")
                    .put("Flying Speed", "minecraft:generic.flying_speed")
                    .put("generic.attackDamage", "minecraft:generic.attack_damage")
                    .put("generic.attackKnockback", "minecraft:generic.attack_knockback")
                    .put("generic.attackSpeed", "minecraft:generic.attack_speed")
                    .put("generic.armorToughness", "minecraft:generic.armor_toughness")
                    .build()
                )
            )
        );
        Schema schema112 = p_14514_.addSchema(2527, SAME_NAMESPACED);
        p_14514_.addFixer(new BitStorageAlignFix(schema112));
        Schema schema113 = p_14514_.addSchema(2528, SAME_NAMESPACED);
        p_14514_.addFixer(
            ItemRenameFix.create(
                schema113,
                "Rename soul fire torch and soul fire lantern",
                createRenamer(ImmutableMap.of("minecraft:soul_fire_torch", "minecraft:soul_torch", "minecraft:soul_fire_lantern", "minecraft:soul_lantern"))
            )
        );
        p_14514_.addFixer(
            BlockRenameFix.create(
                schema113,
                "Rename soul fire torch and soul fire lantern",
                createRenamer(
                    ImmutableMap.of(
                        "minecraft:soul_fire_torch",
                        "minecraft:soul_torch",
                        "minecraft:soul_fire_wall_torch",
                        "minecraft:soul_wall_torch",
                        "minecraft:soul_fire_lantern",
                        "minecraft:soul_lantern"
                    )
                )
            )
        );
        Schema schema114 = p_14514_.addSchema(2529, SAME_NAMESPACED);
        p_14514_.addFixer(new StriderGravityFix(schema114, false));
        Schema schema115 = p_14514_.addSchema(2531, SAME_NAMESPACED);
        p_14514_.addFixer(new RedstoneWireConnectionsFix(schema115));
        Schema schema116 = p_14514_.addSchema(2533, SAME_NAMESPACED);
        p_14514_.addFixer(new VillagerFollowRangeFix(schema116));
        Schema schema117 = p_14514_.addSchema(2535, SAME_NAMESPACED);
        p_14514_.addFixer(new EntityShulkerRotationFix(schema117));
        Schema schema118 = p_14514_.addSchema(2538, SAME_NAMESPACED);
        p_14514_.addFixer(new LevelLegacyWorldGenSettingsFix(schema118));
        Schema schema119 = p_14514_.addSchema(2550, SAME_NAMESPACED);
        p_14514_.addFixer(new WorldGenSettingsFix(schema119));
        Schema schema120 = p_14514_.addSchema(2551, V2551::new);
        p_14514_.addFixer(new WriteAndReadFix(schema120, "add types to WorldGenData", References.WORLD_GEN_SETTINGS));
        Schema schema121 = p_14514_.addSchema(2552, SAME_NAMESPACED);
        p_14514_.addFixer(
            new NamespacedTypeRenameFix(schema121, "Nether biome rename", References.BIOME, createRenamer("minecraft:nether", "minecraft:nether_wastes"))
        );
        Schema schema122 = p_14514_.addSchema(2553, SAME_NAMESPACED);
        p_14514_.addFixer(new NamespacedTypeRenameFix(schema122, "Biomes fix", References.BIOME, createRenamer(BiomeFix.BIOMES)));
        Schema schema123 = p_14514_.addSchema(2558, SAME_NAMESPACED);
        p_14514_.addFixer(new MissingDimensionFix(schema123, false));
        p_14514_.addFixer(new OptionsRenameFieldFix(schema123, false, "Rename swapHands setting", "key_key.swapHands", "key_key.swapOffhand"));
        Schema schema124 = p_14514_.addSchema(2568, V2568::new);
        p_14514_.addFixer(new AddNewChoices(schema124, "Added Piglin Brute", References.ENTITY));
        Schema schema125 = p_14514_.addSchema(2571, V2571::new);
        p_14514_.addFixer(new AddNewChoices(schema125, "Added Goat", References.ENTITY));
        Schema schema126 = p_14514_.addSchema(2679, SAME_NAMESPACED);
        p_14514_.addFixer(new CauldronRenameFix(schema126, false));
        Schema schema127 = p_14514_.addSchema(2680, SAME_NAMESPACED);
        p_14514_.addFixer(ItemRenameFix.create(schema127, "Renamed grass path item to dirt path", createRenamer("minecraft:grass_path", "minecraft:dirt_path")));
        p_14514_.addFixer(BlockRenameFix.create(schema127, "Renamed grass path block to dirt path", createRenamer("minecraft:grass_path", "minecraft:dirt_path")));
        Schema schema128 = p_14514_.addSchema(2684, V2684::new);
        p_14514_.addFixer(new AddNewChoices(schema128, "Added Sculk Sensor", References.BLOCK_ENTITY));
        Schema schema129 = p_14514_.addSchema(2686, V2686::new);
        p_14514_.addFixer(new AddNewChoices(schema129, "Added Axolotl", References.ENTITY));
        Schema schema130 = p_14514_.addSchema(2688, V2688::new);
        p_14514_.addFixer(new AddNewChoices(schema130, "Added Glow Squid", References.ENTITY));
        p_14514_.addFixer(new AddNewChoices(schema130, "Added Glow Item Frame", References.ENTITY));
        Schema schema131 = p_14514_.addSchema(2690, SAME_NAMESPACED);
        ImmutableMap<String, String> immutablemap = ImmutableMap.<String, String>builder()
                .put("minecraft:weathered_copper_block", "minecraft:oxidized_copper_block")
                .put("minecraft:semi_weathered_copper_block", "minecraft:weathered_copper_block")
                .put("minecraft:lightly_weathered_copper_block", "minecraft:exposed_copper_block")
                .put("minecraft:weathered_cut_copper", "minecraft:oxidized_cut_copper")
                .put("minecraft:semi_weathered_cut_copper", "minecraft:weathered_cut_copper")
                .put("minecraft:lightly_weathered_cut_copper", "minecraft:exposed_cut_copper")
                .put("minecraft:weathered_cut_copper_stairs", "minecraft:oxidized_cut_copper_stairs")
                .put("minecraft:semi_weathered_cut_copper_stairs", "minecraft:weathered_cut_copper_stairs")
                .put("minecraft:lightly_weathered_cut_copper_stairs", "minecraft:exposed_cut_copper_stairs")
                .put("minecraft:weathered_cut_copper_slab", "minecraft:oxidized_cut_copper_slab")
                .put("minecraft:semi_weathered_cut_copper_slab", "minecraft:weathered_cut_copper_slab")
                .put("minecraft:lightly_weathered_cut_copper_slab", "minecraft:exposed_cut_copper_slab")
                .put("minecraft:waxed_semi_weathered_copper", "minecraft:waxed_weathered_copper")
                .put("minecraft:waxed_lightly_weathered_copper", "minecraft:waxed_exposed_copper")
                .put("minecraft:waxed_semi_weathered_cut_copper", "minecraft:waxed_weathered_cut_copper")
                .put("minecraft:waxed_lightly_weathered_cut_copper", "minecraft:waxed_exposed_cut_copper")
                .put("minecraft:waxed_semi_weathered_cut_copper_stairs", "minecraft:waxed_weathered_cut_copper_stairs")
                .put("minecraft:waxed_lightly_weathered_cut_copper_stairs", "minecraft:waxed_exposed_cut_copper_stairs")
                .put("minecraft:waxed_semi_weathered_cut_copper_slab", "minecraft:waxed_weathered_cut_copper_slab")
                .put("minecraft:waxed_lightly_weathered_cut_copper_slab", "minecraft:waxed_exposed_cut_copper_slab")
                .build();
        p_14514_.addFixer(ItemRenameFix.create(schema131, "Renamed copper block items to new oxidized terms", createRenamer(immutablemap)));
        p_14514_.addFixer(BlockRenameFix.create(schema131, "Renamed copper blocks to new oxidized terms", createRenamer(immutablemap)));
        Schema schema132 = p_14514_.addSchema(2691, SAME_NAMESPACED);
        ImmutableMap<String, String> immutablemap1 = ImmutableMap.<String, String>builder()
                .put("minecraft:waxed_copper", "minecraft:waxed_copper_block")
                .put("minecraft:oxidized_copper_block", "minecraft:oxidized_copper")
                .put("minecraft:weathered_copper_block", "minecraft:weathered_copper")
                .put("minecraft:exposed_copper_block", "minecraft:exposed_copper")
                .build();
        p_14514_.addFixer(ItemRenameFix.create(schema132, "Rename copper item suffixes", createRenamer(immutablemap1)));
        p_14514_.addFixer(BlockRenameFix.create(schema132, "Rename copper blocks suffixes", createRenamer(immutablemap1)));
        Schema schema133 = p_14514_.addSchema(2693, SAME_NAMESPACED);
        p_14514_.addFixer(new AddFlagIfNotPresentFix(schema133, References.WORLD_GEN_SETTINGS, "has_increased_height_already", false));
        Schema schema134 = p_14514_.addSchema(2696, SAME_NAMESPACED);
        ImmutableMap<String, String> immutablemap2 = ImmutableMap.<String, String>builder()
                .put("minecraft:grimstone", "minecraft:deepslate")
                .put("minecraft:grimstone_slab", "minecraft:cobbled_deepslate_slab")
                .put("minecraft:grimstone_stairs", "minecraft:cobbled_deepslate_stairs")
                .put("minecraft:grimstone_wall", "minecraft:cobbled_deepslate_wall")
                .put("minecraft:polished_grimstone", "minecraft:polished_deepslate")
                .put("minecraft:polished_grimstone_slab", "minecraft:polished_deepslate_slab")
                .put("minecraft:polished_grimstone_stairs", "minecraft:polished_deepslate_stairs")
                .put("minecraft:polished_grimstone_wall", "minecraft:polished_deepslate_wall")
                .put("minecraft:grimstone_tiles", "minecraft:deepslate_tiles")
                .put("minecraft:grimstone_tile_slab", "minecraft:deepslate_tile_slab")
                .put("minecraft:grimstone_tile_stairs", "minecraft:deepslate_tile_stairs")
                .put("minecraft:grimstone_tile_wall", "minecraft:deepslate_tile_wall")
                .put("minecraft:grimstone_bricks", "minecraft:deepslate_bricks")
                .put("minecraft:grimstone_brick_slab", "minecraft:deepslate_brick_slab")
                .put("minecraft:grimstone_brick_stairs", "minecraft:deepslate_brick_stairs")
                .put("minecraft:grimstone_brick_wall", "minecraft:deepslate_brick_wall")
                .put("minecraft:chiseled_grimstone", "minecraft:chiseled_deepslate")
                .build();
        p_14514_.addFixer(ItemRenameFix.create(schema134, "Renamed grimstone block items to deepslate", createRenamer(immutablemap2)));
        p_14514_.addFixer(BlockRenameFix.create(schema134, "Renamed grimstone blocks to deepslate", createRenamer(immutablemap2)));
        Schema schema135 = p_14514_.addSchema(2700, SAME_NAMESPACED);
        p_14514_.addFixer(
            BlockRenameFix.create(
                schema135,
                "Renamed cave vines blocks",
                createRenamer(ImmutableMap.of("minecraft:cave_vines_head", "minecraft:cave_vines", "minecraft:cave_vines_body", "minecraft:cave_vines_plant"))
            )
        );
        Schema schema136 = p_14514_.addSchema(2701, SAME_NAMESPACED);
        p_14514_.addFixer(new SavedDataFeaturePoolElementFix(schema136));
        Schema schema137 = p_14514_.addSchema(2702, SAME_NAMESPACED);
        p_14514_.addFixer(new AbstractArrowPickupFix(schema137));
        Schema schema138 = p_14514_.addSchema(2704, V2704::new);
        p_14514_.addFixer(new AddNewChoices(schema138, "Added Goat", References.ENTITY));
        Schema schema139 = p_14514_.addSchema(2707, V2707::new);
        p_14514_.addFixer(new AddNewChoices(schema139, "Added Marker", References.ENTITY));
        p_14514_.addFixer(new AddFlagIfNotPresentFix(schema139, References.WORLD_GEN_SETTINGS, "has_increased_height_already", true));
        Schema schema140 = p_14514_.addSchema(2710, SAME_NAMESPACED);
        p_14514_.addFixer(
            new StatsRenameFix(schema140, "Renamed play_one_minute stat to play_time", ImmutableMap.of("minecraft:play_one_minute", "minecraft:play_time"))
        );
        Schema schema141 = p_14514_.addSchema(2717, SAME_NAMESPACED);
        p_14514_.addFixer(
            ItemRenameFix.create(
                schema141, "Rename azalea_leaves_flowers", createRenamer(ImmutableMap.of("minecraft:azalea_leaves_flowers", "minecraft:flowering_azalea_leaves"))
            )
        );
        p_14514_.addFixer(
            BlockRenameFix.create(
                schema141,
                "Rename azalea_leaves_flowers items",
                createRenamer(ImmutableMap.of("minecraft:azalea_leaves_flowers", "minecraft:flowering_azalea_leaves"))
            )
        );
        Schema schema142 = p_14514_.addSchema(2825, SAME_NAMESPACED);
        p_14514_.addFixer(new AddFlagIfNotPresentFix(schema142, References.WORLD_GEN_SETTINGS, "has_increased_height_already", false));
        Schema schema143 = p_14514_.addSchema(2831, V2831::new);
        p_14514_.addFixer(new SpawnerDataFix(schema143));
        Schema schema144 = p_14514_.addSchema(2832, V2832::new);
        p_14514_.addFixer(new WorldGenSettingsHeightAndBiomeFix(schema144));
        p_14514_.addFixer(new ChunkHeightAndBiomeFix(schema144));
        Schema schema145 = p_14514_.addSchema(2833, SAME_NAMESPACED);
        p_14514_.addFixer(new WorldGenSettingsDisallowOldCustomWorldsFix(schema145));
        Schema schema146 = p_14514_.addSchema(2838, SAME_NAMESPACED);
        p_14514_.addFixer(
            new NamespacedTypeRenameFix(schema146, "Caves and Cliffs biome renames", References.BIOME, createRenamer(CavesAndCliffsRenames.RENAMES))
        );
        Schema schema147 = p_14514_.addSchema(2841, SAME_NAMESPACED);
        p_14514_.addFixer(new ChunkProtoTickListFix(schema147));
        Schema schema148 = p_14514_.addSchema(2842, V2842::new);
        p_14514_.addFixer(new ChunkRenamesFix(schema148));
        Schema schema149 = p_14514_.addSchema(2843, SAME_NAMESPACED);
        p_14514_.addFixer(new OverreachingTickFix(schema149));
        p_14514_.addFixer(
            new NamespacedTypeRenameFix(schema149, "Remove Deep Warm Ocean", References.BIOME, createRenamer("minecraft:deep_warm_ocean", "minecraft:warm_ocean"))
        );
        Schema schema150 = p_14514_.addSchema(2846, SAME_NAMESPACED);
        p_14514_.addFixer(
            new AdvancementsRenameFix(
                schema150,
                false,
                "Rename some C&C part 2 advancements",
                createRenamer(
                    ImmutableMap.of(
                        "minecraft:husbandry/play_jukebox_in_meadows",
                        "minecraft:adventure/play_jukebox_in_meadows",
                        "minecraft:adventure/caves_and_cliff",
                        "minecraft:adventure/fall_from_world_height",
                        "minecraft:adventure/ride_strider_in_overworld_lava",
                        "minecraft:nether/ride_strider_in_overworld_lava"
                    )
                )
            )
        );
        Schema schema151 = p_14514_.addSchema(2852, SAME_NAMESPACED);
        p_14514_.addFixer(new WorldGenSettingsDisallowOldCustomWorldsFix(schema151));
        Schema schema152 = p_14514_.addSchema(2967, SAME_NAMESPACED);
        p_14514_.addFixer(new StructureSettingsFlattenFix(schema152));
        Schema schema153 = p_14514_.addSchema(2970, SAME_NAMESPACED);
        p_14514_.addFixer(new StructuresBecomeConfiguredFix(schema153));
        Schema schema154 = p_14514_.addSchema(3076, V3076::new);
        p_14514_.addFixer(new AddNewChoices(schema154, "Added Sculk Catalyst", References.BLOCK_ENTITY));
        Schema schema155 = p_14514_.addSchema(3077, SAME_NAMESPACED);
        p_14514_.addFixer(new ChunkDeleteIgnoredLightDataFix(schema155));
        Schema schema156 = p_14514_.addSchema(3078, V3078::new);
        p_14514_.addFixer(new AddNewChoices(schema156, "Added Frog", References.ENTITY));
        p_14514_.addFixer(new AddNewChoices(schema156, "Added Tadpole", References.ENTITY));
        p_14514_.addFixer(new AddNewChoices(schema156, "Added Sculk Shrieker", References.BLOCK_ENTITY));
        Schema schema157 = p_14514_.addSchema(3081, V3081::new);
        p_14514_.addFixer(new AddNewChoices(schema157, "Added Warden", References.ENTITY));
        Schema schema158 = p_14514_.addSchema(3082, V3082::new);
        p_14514_.addFixer(new AddNewChoices(schema158, "Added Chest Boat", References.ENTITY));
        Schema schema159 = p_14514_.addSchema(3083, V3083::new);
        p_14514_.addFixer(new AddNewChoices(schema159, "Added Allay", References.ENTITY));
        Schema schema160 = p_14514_.addSchema(3084, SAME_NAMESPACED);
        p_14514_.addFixer(
            new NamespacedTypeRenameFix(
                schema160,
                "game_event_renames_3084",
                References.GAME_EVENT_NAME,
                createRenamer(
                    ImmutableMap.<String, String>builder()
                    .put("minecraft:block_press", "minecraft:block_activate")
                    .put("minecraft:block_switch", "minecraft:block_activate")
                    .put("minecraft:block_unpress", "minecraft:block_deactivate")
                    .put("minecraft:block_unswitch", "minecraft:block_deactivate")
                    .put("minecraft:drinking_finish", "minecraft:drink")
                    .put("minecraft:elytra_free_fall", "minecraft:elytra_glide")
                    .put("minecraft:entity_damaged", "minecraft:entity_damage")
                    .put("minecraft:entity_dying", "minecraft:entity_die")
                    .put("minecraft:entity_killed", "minecraft:entity_die")
                    .put("minecraft:mob_interact", "minecraft:entity_interact")
                    .put("minecraft:ravager_roar", "minecraft:entity_roar")
                    .put("minecraft:ring_bell", "minecraft:block_change")
                    .put("minecraft:shulker_close", "minecraft:container_close")
                    .put("minecraft:shulker_open", "minecraft:container_open")
                    .put("minecraft:wolf_shaking", "minecraft:entity_shake")
                    .build()
                )
            )
        );
        Schema schema161 = p_14514_.addSchema(3086, SAME_NAMESPACED);
        p_14514_.addFixer(
            new EntityVariantFix(
                schema161,
                "Change cat variant type",
                References.ENTITY,
                "minecraft:cat",
                "CatType",
                Util.make(new Int2ObjectOpenHashMap<String>(), p_216528_ ->
        {
            p_216528_.defaultReturnValue("minecraft:tabby");
            p_216528_.put(0, "minecraft:tabby");
            p_216528_.put(1, "minecraft:black");
            p_216528_.put(2, "minecraft:red");
            p_216528_.put(3, "minecraft:siamese");
            p_216528_.put(4, "minecraft:british");
            p_216528_.put(5, "minecraft:calico");
            p_216528_.put(6, "minecraft:persian");
            p_216528_.put(7, "minecraft:ragdoll");
            p_216528_.put(8, "minecraft:white");
            p_216528_.put(9, "minecraft:jellie");
            p_216528_.put(10, "minecraft:all_black");
        })::get
            )
        );
        ImmutableMap<String, String> immutablemap3 = ImmutableMap.<String, String>builder()
                .put("textures/entity/cat/tabby.png", "minecraft:tabby")
                .put("textures/entity/cat/black.png", "minecraft:black")
                .put("textures/entity/cat/red.png", "minecraft:red")
                .put("textures/entity/cat/siamese.png", "minecraft:siamese")
                .put("textures/entity/cat/british_shorthair.png", "minecraft:british")
                .put("textures/entity/cat/calico.png", "minecraft:calico")
                .put("textures/entity/cat/persian.png", "minecraft:persian")
                .put("textures/entity/cat/ragdoll.png", "minecraft:ragdoll")
                .put("textures/entity/cat/white.png", "minecraft:white")
                .put("textures/entity/cat/jellie.png", "minecraft:jellie")
                .put("textures/entity/cat/all_black.png", "minecraft:all_black")
                .build();
        p_14514_.addFixer(
            new CriteriaRenameFix(
                schema161,
                "Migrate cat variant advancement",
                "minecraft:husbandry/complete_catalogue",
                p_216517_ -> immutablemap3.getOrDefault(p_216517_, p_216517_)
            )
        );
        Schema schema162 = p_14514_.addSchema(3087, SAME_NAMESPACED);
        p_14514_.addFixer(
            new EntityVariantFix(
                schema162,
                "Change frog variant type",
                References.ENTITY,
                "minecraft:frog",
                "Variant",
                Util.make(new Int2ObjectOpenHashMap<String>(), p_216519_ ->
        {
            p_216519_.put(0, "minecraft:temperate");
            p_216519_.put(1, "minecraft:warm");
            p_216519_.put(2, "minecraft:cold");
        })::get
            )
        );
        Schema schema163 = p_14514_.addSchema(3090, SAME_NAMESPACED);
        p_14514_.addFixer(new EntityPaintingFieldsRenameFix(schema163));
        Schema schema164 = p_14514_.addSchema(3093, SAME_NAMESPACED);
        p_14514_.addFixer(new EntityGoatMissingStateFix(schema164));
        Schema schema165 = p_14514_.addSchema(3094, SAME_NAMESPACED);
        p_14514_.addFixer(new GoatHornIdFix(schema165));
        Schema schema166 = p_14514_.addSchema(3097, SAME_NAMESPACED);
        p_14514_.addFixer(new FilteredBooksFix(schema166));
        p_14514_.addFixer(new FilteredSignsFix(schema166));
        Map<String, String> map = Map.of("minecraft:british", "minecraft:british_shorthair");
        p_14514_.addFixer(new VariantRenameFix(schema166, "Rename british shorthair", References.ENTITY, "minecraft:cat", map));
        p_14514_.addFixer(
            new CriteriaRenameFix(
                schema166,
                "Migrate cat variant advancement for british shorthair",
                "minecraft:husbandry/complete_catalogue",
                p_216531_ -> map.getOrDefault(p_216531_, p_216531_)
            )
        );
        p_14514_.addFixer(
            new PoiTypeRemoveFix(schema166, "Remove unpopulated villager PoI types", Set.of("minecraft:unemployed", "minecraft:nitwit")::contains)
        );
        Schema schema167 = p_14514_.addSchema(3108, SAME_NAMESPACED);
        p_14514_.addFixer(new BlendingDataRemoveFromNetherEndFix(schema167));
        Schema schema168 = p_14514_.addSchema(3201, SAME_NAMESPACED);
        p_14514_.addFixer(new OptionsProgrammerArtFix(schema168));
        Schema schema169 = p_14514_.addSchema(3202, V3202::new);
        p_14514_.addFixer(new AddNewChoices(schema169, "Added Hanging Sign", References.BLOCK_ENTITY));
        Schema schema170 = p_14514_.addSchema(3203, V3203::new);
        p_14514_.addFixer(new AddNewChoices(schema170, "Added Camel", References.ENTITY));
        Schema schema171 = p_14514_.addSchema(3204, V3204::new);
        p_14514_.addFixer(new AddNewChoices(schema171, "Added Chiseled Bookshelf", References.BLOCK_ENTITY));
        Schema schema172 = p_14514_.addSchema(3209, SAME_NAMESPACED);
        p_14514_.addFixer(new ItemStackSpawnEggFix(schema172, false, "minecraft:pig_spawn_egg"));
        Schema schema173 = p_14514_.addSchema(3214, SAME_NAMESPACED);
        p_14514_.addFixer(new OptionsAmbientOcclusionFix(schema173));
        Schema schema174 = p_14514_.addSchema(3319, SAME_NAMESPACED);
        p_14514_.addFixer(new OptionsAccessibilityOnboardFix(schema174));
        Schema schema175 = p_14514_.addSchema(3322, SAME_NAMESPACED);
        p_14514_.addFixer(new EffectDurationFix(schema175));
        Schema schema176 = p_14514_.addSchema(3325, V3325::new);
        p_14514_.addFixer(new AddNewChoices(schema176, "Added displays", References.ENTITY));
        Schema schema177 = p_14514_.addSchema(3326, V3326::new);
        p_14514_.addFixer(new AddNewChoices(schema177, "Added Sniffer", References.ENTITY));
        Schema schema178 = p_14514_.addSchema(3327, V3327::new);
        p_14514_.addFixer(new AddNewChoices(schema178, "Archaeology", References.BLOCK_ENTITY));
        Schema schema179 = p_14514_.addSchema(3328, V3328::new);
        p_14514_.addFixer(new AddNewChoices(schema179, "Added interaction", References.ENTITY));
        Schema schema180 = p_14514_.addSchema(3438, V3438::new);
        p_14514_.addFixer(
            BlockEntityRenameFix.create(
                schema180, "Rename Suspicious Sand to Brushable Block", createRenamer("minecraft:suspicious_sand", "minecraft:brushable_block")
            )
        );
        p_14514_.addFixer(new EntityBrushableBlockFieldsRenameFix(schema180));
        p_14514_.addFixer(
            ItemRenameFix.create(
                schema180,
                "Pottery shard renaming",
                createRenamer(
                    ImmutableMap.of(
                        "minecraft:pottery_shard_archer",
                        "minecraft:archer_pottery_shard",
                        "minecraft:pottery_shard_prize",
                        "minecraft:prize_pottery_shard",
                        "minecraft:pottery_shard_arms_up",
                        "minecraft:arms_up_pottery_shard",
                        "minecraft:pottery_shard_skull",
                        "minecraft:skull_pottery_shard"
                    )
                )
            )
        );
        p_14514_.addFixer(new AddNewChoices(schema180, "Added calibrated sculk sensor", References.BLOCK_ENTITY));
        Schema schema181 = p_14514_.addSchema(3439, SAME_NAMESPACED);
        p_14514_.addFixer(new BlockEntitySignDoubleSidedEditableTextFix(schema181, "Updated sign text format for Signs", "minecraft:sign"));
        p_14514_.addFixer(new BlockEntitySignDoubleSidedEditableTextFix(schema181, "Updated sign text format for Hanging Signs", "minecraft:hanging_sign"));
        Schema schema182 = p_14514_.addSchema(3440, SAME_NAMESPACED);
        p_14514_.addFixer(
            new NamespacedTypeRenameFix(
                schema182, "Replace experimental 1.20 overworld", References.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, createRenamer("minecraft:overworld_update_1_20", "minecraft:overworld")
            )
        );
        p_14514_.addFixer(new FeatureFlagRemoveFix(schema182, "Remove 1.20 feature toggle", Set.of("minecraft:update_1_20")));
        Schema schema183 = p_14514_.addSchema(3441, SAME_NAMESPACED);
        p_14514_.addFixer(new BlendingDataFix(schema183));
        Schema schema184 = p_14514_.addSchema(3447, SAME_NAMESPACED);
        p_14514_.addFixer(
            ItemRenameFix.create(
                schema184,
                "Pottery shard item renaming to Pottery sherd",
                createRenamer(
                    Stream.of(
                        "minecraft:angler_pottery_shard",
                        "minecraft:archer_pottery_shard",
                        "minecraft:arms_up_pottery_shard",
                        "minecraft:blade_pottery_shard",
                        "minecraft:brewer_pottery_shard",
                        "minecraft:burn_pottery_shard",
                        "minecraft:danger_pottery_shard",
                        "minecraft:explorer_pottery_shard",
                        "minecraft:friend_pottery_shard",
                        "minecraft:heart_pottery_shard",
                        "minecraft:heartbreak_pottery_shard",
                        "minecraft:howl_pottery_shard",
                        "minecraft:miner_pottery_shard",
                        "minecraft:mourner_pottery_shard",
                        "minecraft:plenty_pottery_shard",
                        "minecraft:prize_pottery_shard",
                        "minecraft:sheaf_pottery_shard",
                        "minecraft:shelter_pottery_shard",
                        "minecraft:skull_pottery_shard",
                        "minecraft:snort_pottery_shard"
                    )
                    .collect(Collectors.toMap(Function.identity(), p_280993_ -> p_280993_.replace("_pottery_shard", "_pottery_sherd")))
                )
            )
        );
        Schema schema185 = p_14514_.addSchema(3448, V3448::new);
        p_14514_.addFixer(new DecoratedPotFieldRenameFix(schema185));
        Schema schema186 = p_14514_.addSchema(3450, SAME_NAMESPACED);
        p_14514_.addFixer(
            new RemapChunkStatusFix(
                schema186,
                "Remove liquid_carvers and heightmap chunk statuses",
                createRenamer(Map.of("minecraft:liquid_carvers", "minecraft:carvers", "minecraft:heightmaps", "minecraft:spawn"))
            )
        );
        Schema schema187 = p_14514_.addSchema(3451, SAME_NAMESPACED);
        p_14514_.addFixer(new ChunkDeleteLightFix(schema187));
        Schema schema188 = p_14514_.addSchema(3459, SAME_NAMESPACED);
        p_14514_.addFixer(new LegacyDragonFightFix(schema188));
        Schema schema189 = p_14514_.addSchema(3564, SAME_NAMESPACED);
        p_14514_.addFixer(new DropInvalidSignDataFix(schema189, "Drop invalid sign datafix data", "minecraft:sign"));
        p_14514_.addFixer(new DropInvalidSignDataFix(schema189, "Drop invalid hanging sign datafix data", "minecraft:hanging_sign"));
        Schema schema190 = p_14514_.addSchema(3565, SAME_NAMESPACED);
        p_14514_.addFixer(new RandomSequenceSettingsFix(schema190));
        Schema schema191 = p_14514_.addSchema(3566, SAME_NAMESPACED);
        p_14514_.addFixer(new ScoreboardDisplaySlotFix(schema191));
        Schema schema192 = p_14514_.addSchema(3568, SAME_NAMESPACED);
        p_14514_.addFixer(new MobEffectIdFix(schema192));
        Schema schema193 = p_14514_.addSchema(3682, V3682::new);
        p_14514_.addFixer(new AddNewChoices(schema193, "Added Crafter", References.BLOCK_ENTITY));
        Schema schema194 = p_14514_.addSchema(3683, V3683::new);
        p_14514_.addFixer(new PrimedTntBlockStateFixer(schema194));
        Schema schema195 = p_14514_.addSchema(3685, V3685::new);
        p_14514_.addFixer(new FixProjectileStoredItem(schema195));
        Schema schema196 = p_14514_.addSchema(3689, V3689::new);
        p_14514_.addFixer(new AddNewChoices(schema196, "Added Breeze", References.ENTITY));
        p_14514_.addFixer(new AddNewChoices(schema196, "Added Trial Spawner", References.BLOCK_ENTITY));
        Schema schema197 = p_14514_.addSchema(3692, SAME_NAMESPACED);
        UnaryOperator<String> unaryoperator1 = createRenamer(Map.of("minecraft:grass", "minecraft:short_grass"));
        p_14514_.addFixer(BlockRenameFix.create(schema197, "Rename grass block to short_grass", unaryoperator1));
        p_14514_.addFixer(ItemRenameFix.create(schema197, "Rename grass item to short_grass", unaryoperator1));
        Schema schema198 = p_14514_.addSchema(3799, V3799::new);
        p_14514_.addFixer(new AddNewChoices(schema198, "Added Armadillo", References.ENTITY));
        Schema schema199 = p_14514_.addSchema(3800, SAME_NAMESPACED);
        UnaryOperator<String> unaryoperator2 = createRenamer(Map.of("minecraft:scute", "minecraft:turtle_scute"));
        p_14514_.addFixer(ItemRenameFix.create(schema199, "Rename scute item to turtle_scute", unaryoperator2));
        Schema schema200 = p_14514_.addSchema(3803, SAME_NAMESPACED);
        p_14514_.addFixer(
            new RenameEnchantmentsFix(schema200, "Rename sweeping enchant to sweeping_edge", Map.of("minecraft:sweeping", "minecraft:sweeping_edge"))
        );
        Schema schema201 = p_14514_.addSchema(3807, V3807::new);
        p_14514_.addFixer(new AddNewChoices(schema201, "Added Vault", References.BLOCK_ENTITY));
        Schema schema202 = p_14514_.addSchema(3807, 1, SAME_NAMESPACED);
        p_14514_.addFixer(new MapBannerBlockPosFormatFix(schema202));
        Schema schema203 = p_14514_.addSchema(3808, V3808::new);
        p_14514_.addFixer(new HorseBodyArmorItemFix(schema203, "minecraft:horse", "ArmorItem", true));
        Schema schema204 = p_14514_.addSchema(3808, 1, V3808_1::new);
        p_14514_.addFixer(new HorseBodyArmorItemFix(schema204, "minecraft:llama", "DecorItem", false));
        Schema schema205 = p_14514_.addSchema(3808, 2, V3808_2::new);
        p_14514_.addFixer(new HorseBodyArmorItemFix(schema205, "minecraft:trader_llama", "DecorItem", false));
        Schema schema206 = p_14514_.addSchema(3809, SAME_NAMESPACED);
        p_14514_.addFixer(new ChestedHorsesInventoryZeroIndexingFix(schema206));
        Schema schema207 = p_14514_.addSchema(3812, SAME_NAMESPACED);
        p_14514_.addFixer(new FixWolfHealth(schema207));
        Schema schema208 = p_14514_.addSchema(3813, SAME_NAMESPACED);
        p_14514_.addFixer(new BlockPosFormatAndRenamesFix(schema208));
        Schema schema209 = p_14514_.addSchema(3814, SAME_NAMESPACED);
        p_14514_.addFixer(
            new AttributesRename(schema209, "Rename jump strength attribute", createRenamer("minecraft:horse.jump_strength", "minecraft:generic.jump_strength"))
        );
        Schema schema210 = p_14514_.addSchema(3816, V3816::new);
        p_14514_.addFixer(new AddNewChoices(schema210, "Added Bogged", References.ENTITY));
        Schema schema211 = p_14514_.addSchema(3818, V3818::new);
        p_14514_.addFixer(new BeehiveFieldRenameFix(schema211));
        p_14514_.addFixer(new EmptyItemInHotbarFix(schema211));
        Schema schema212 = p_14514_.addSchema(3818, 1, SAME_NAMESPACED);
        p_14514_.addFixer(new BannerPatternFormatFix(schema212));
        Schema schema213 = p_14514_.addSchema(3818, 2, SAME_NAMESPACED);
        p_14514_.addFixer(new TippedArrowPotionToItemFix(schema213));
        Schema schema214 = p_14514_.addSchema(3818, 3, V3818_3::new);
        p_14514_.addFixer(new WriteAndReadFix(schema214, "Inject data component types", References.DATA_COMPONENTS));
        Schema schema215 = p_14514_.addSchema(3818, 4, V3818_4::new);
        p_14514_.addFixer(new ParticleUnflatteningFix(schema215));
        Schema schema216 = p_14514_.addSchema(3818, 5, V3818_5::new);
        p_14514_.addFixer(new ItemStackComponentizationFix(schema216));
        Schema schema217 = p_14514_.addSchema(3818, 6, SAME_NAMESPACED);
        p_14514_.addFixer(new AreaEffectCloudPotionFix(schema217));
        Schema schema218 = p_14514_.addSchema(3820, SAME_NAMESPACED);
        p_14514_.addFixer(new PlayerHeadBlockProfileFix(schema218));
        p_14514_.addFixer(new LodestoneCompassComponentFix(schema218));
        Schema schema219 = p_14514_.addSchema(3825, V3825::new);
        p_14514_.addFixer(new ItemStackCustomNameToOverrideComponentFix(schema219));
        p_14514_.addFixer(new BannerEntityCustomNameToOverrideComponentFix(schema219));
        p_14514_.addFixer(new TrialSpawnerConfigFix(schema219));
        p_14514_.addFixer(new AddNewChoices(schema219, "Added Ominous Item Spawner", References.ENTITY));
        Schema schema220 = p_14514_.addSchema(3828, SAME_NAMESPACED);
        p_14514_.addFixer(new EmptyItemInVillagerTradeFix(schema220));
        Schema schema221 = p_14514_.addSchema(3833, SAME_NAMESPACED);
        p_14514_.addFixer(new RemoveEmptyItemInBrushableBlockFix(schema221));
        Schema schema222 = p_14514_.addSchema(3938, V3938::new);
        p_14514_.addFixer(new ProjectileStoredWeaponFix(schema222));
        Schema schema223 = p_14514_.addSchema(3939, SAME_NAMESPACED);
        p_14514_.addFixer(new FeatureFlagRemoveFix(schema223, "Remove 1.21 feature toggle", Set.of("minecraft:update_1_21")));
        Schema schema224 = p_14514_.addSchema(3943, SAME_NAMESPACED);
        p_14514_.addFixer(new OptionsMenuBlurrinessFix(schema224));
        Schema schema225 = p_14514_.addSchema(3945, SAME_NAMESPACED);
        p_14514_.addFixer(new AttributeModifierIdFix(schema225));
        p_14514_.addFixer(new JukeboxTicksSinceSongStartedFix(schema225));
    }

    private static UnaryOperator<String> createRenamerNoNamespace(Map<String, String> p_330646_)
    {
        return p_216526_ -> p_330646_.getOrDefault(p_216526_, p_216526_);
    }

    private static UnaryOperator<String> createRenamer(Map<String, String> p_14525_)
    {
        return p_326540_ -> p_14525_.getOrDefault(NamespacedSchema.ensureNamespaced(p_326540_), p_326540_);
    }

    private static UnaryOperator<String> createRenamer(String p_14518_, String p_14519_)
    {
        return p_326538_ -> Objects.equals(NamespacedSchema.ensureNamespaced(p_326538_), p_14518_) ? p_14519_ : p_326538_;
    }
}
