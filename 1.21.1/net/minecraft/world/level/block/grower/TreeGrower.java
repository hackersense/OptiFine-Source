package net.minecraft.world.level.block.grower;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public final class TreeGrower
{
    private static final Map<String, TreeGrower> GROWERS = new Object2ObjectArrayMap<>();
    public static final Codec<TreeGrower> CODEC = Codec.stringResolver(p_310196_ -> p_310196_.name, GROWERS::get);
    public static final TreeGrower OAK = new TreeGrower(
        "oak",
        0.1F,
        Optional.empty(),
        Optional.empty(),
        Optional.of(TreeFeatures.OAK),
        Optional.of(TreeFeatures.FANCY_OAK),
        Optional.of(TreeFeatures.OAK_BEES_005),
        Optional.of(TreeFeatures.FANCY_OAK_BEES_005)
    );
    public static final TreeGrower SPRUCE = new TreeGrower(
        "spruce",
        0.5F,
        Optional.of(TreeFeatures.MEGA_SPRUCE),
        Optional.of(TreeFeatures.MEGA_PINE),
        Optional.of(TreeFeatures.SPRUCE),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    );
    public static final TreeGrower MANGROVE = new TreeGrower(
        "mangrove",
        0.85F,
        Optional.empty(),
        Optional.empty(),
        Optional.of(TreeFeatures.MANGROVE),
        Optional.of(TreeFeatures.TALL_MANGROVE),
        Optional.empty(),
        Optional.empty()
    );
    public static final TreeGrower AZALEA = new TreeGrower("azalea", Optional.empty(), Optional.of(TreeFeatures.AZALEA_TREE), Optional.empty());
    public static final TreeGrower BIRCH = new TreeGrower(
        "birch", Optional.empty(), Optional.of(TreeFeatures.BIRCH), Optional.of(TreeFeatures.BIRCH_BEES_005)
    );
    public static final TreeGrower JUNGLE = new TreeGrower(
        "jungle", Optional.of(TreeFeatures.MEGA_JUNGLE_TREE), Optional.of(TreeFeatures.JUNGLE_TREE_NO_VINE), Optional.empty()
    );
    public static final TreeGrower ACACIA = new TreeGrower("acacia", Optional.empty(), Optional.of(TreeFeatures.ACACIA), Optional.empty());
    public static final TreeGrower CHERRY = new TreeGrower(
        "cherry", Optional.empty(), Optional.of(TreeFeatures.CHERRY), Optional.of(TreeFeatures.CHERRY_BEES_005)
    );
    public static final TreeGrower DARK_OAK = new TreeGrower("dark_oak", Optional.of(TreeFeatures.DARK_OAK), Optional.empty(), Optional.empty());
    private final String name;
    private final float secondaryChance;
    private final Optional < ResourceKey < ConfiguredFeature <? , ? >>> megaTree;
    private final Optional < ResourceKey < ConfiguredFeature <? , ? >>> secondaryMegaTree;
    private final Optional < ResourceKey < ConfiguredFeature <? , ? >>> tree;
    private final Optional < ResourceKey < ConfiguredFeature <? , ? >>> secondaryTree;
    private final Optional < ResourceKey < ConfiguredFeature <? , ? >>> flowers;
    private final Optional < ResourceKey < ConfiguredFeature <? , ? >>> secondaryFlowers;

    public TreeGrower(
        String p_311110_,
        Optional < ResourceKey < ConfiguredFeature <? , ? >>> p_309803_,
        Optional < ResourceKey < ConfiguredFeature <? , ? >>> p_311829_,
        Optional < ResourceKey < ConfiguredFeature <? , ? >>> p_310077_
    )
    {
        this(p_311110_, 0.0F, p_309803_, Optional.empty(), p_311829_, Optional.empty(), p_310077_, Optional.empty());
    }

    public TreeGrower(
        String p_310538_,
        float p_312608_,
        Optional < ResourceKey < ConfiguredFeature <? , ? >>> p_311356_,
        Optional < ResourceKey < ConfiguredFeature <? , ? >>> p_309855_,
        Optional < ResourceKey < ConfiguredFeature <? , ? >>> p_312520_,
        Optional < ResourceKey < ConfiguredFeature <? , ? >>> p_310394_,
        Optional < ResourceKey < ConfiguredFeature <? , ? >>> p_309623_,
        Optional < ResourceKey < ConfiguredFeature <? , ? >>> p_310708_
    )
    {
        this.name = p_310538_;
        this.secondaryChance = p_312608_;
        this.megaTree = p_311356_;
        this.secondaryMegaTree = p_309855_;
        this.tree = p_312520_;
        this.secondaryTree = p_310394_;
        this.flowers = p_309623_;
        this.secondaryFlowers = p_310708_;
        GROWERS.put(p_310538_, this);
    }

    @Nullable
    private ResourceKey < ConfiguredFeature <? , ? >> getConfiguredFeature(RandomSource p_312729_, boolean p_311061_)
    {
        if (p_312729_.nextFloat() < this.secondaryChance)
        {
            if (p_311061_ && this.secondaryFlowers.isPresent())
            {
                return this.secondaryFlowers.get();
            }

            if (this.secondaryTree.isPresent())
            {
                return this.secondaryTree.get();
            }
        }

        return p_311061_ && this.flowers.isPresent() ? this.flowers.get() : this.tree.orElse(null);
    }

    @Nullable
    private ResourceKey < ConfiguredFeature <? , ? >> getConfiguredMegaFeature(RandomSource p_309400_)
    {
        return this.secondaryMegaTree.isPresent() && p_309400_.nextFloat() < this.secondaryChance ? this.secondaryMegaTree.get() : this.megaTree.orElse(null);
    }

    public boolean growTree(ServerLevel p_309830_, ChunkGenerator p_311976_, BlockPos p_310327_, BlockState p_312382_, RandomSource p_309951_)
    {
        ResourceKey < ConfiguredFeature <? , ? >> resourcekey = this.getConfiguredMegaFeature(p_309951_);

        if (resourcekey != null)
        {
            Holder < ConfiguredFeature <? , ? >> holder = p_309830_.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(resourcekey).orElse(null);

            if (holder != null)
            {
                for (int i = 0; i >= -1; i--)
                {
                    for (int j = 0; j >= -1; j--)
                    {
                        if (isTwoByTwoSapling(p_312382_, p_309830_, p_310327_, i, j))
                        {
                            ConfiguredFeature <? , ? > configuredfeature = holder.value();
                            BlockState blockstate = Blocks.AIR.defaultBlockState();
                            p_309830_.setBlock(p_310327_.offset(i, 0, j), blockstate, 4);
                            p_309830_.setBlock(p_310327_.offset(i + 1, 0, j), blockstate, 4);
                            p_309830_.setBlock(p_310327_.offset(i, 0, j + 1), blockstate, 4);
                            p_309830_.setBlock(p_310327_.offset(i + 1, 0, j + 1), blockstate, 4);

                            if (configuredfeature.place(p_309830_, p_311976_, p_309951_, p_310327_.offset(i, 0, j)))
                            {
                                return true;
                            }

                            p_309830_.setBlock(p_310327_.offset(i, 0, j), p_312382_, 4);
                            p_309830_.setBlock(p_310327_.offset(i + 1, 0, j), p_312382_, 4);
                            p_309830_.setBlock(p_310327_.offset(i, 0, j + 1), p_312382_, 4);
                            p_309830_.setBlock(p_310327_.offset(i + 1, 0, j + 1), p_312382_, 4);
                            return false;
                        }
                    }
                }
            }
        }

        ResourceKey < ConfiguredFeature <? , ? >> resourcekey1 = this.getConfiguredFeature(p_309951_, this.hasFlowers(p_309830_, p_310327_));

        if (resourcekey1 == null)
        {
            return false;
        }
        else
        {
            Holder < ConfiguredFeature <? , ? >> holder1 = p_309830_.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(resourcekey1).orElse(null);

            if (holder1 == null)
            {
                return false;
            }
            else
            {
                ConfiguredFeature <? , ? > configuredfeature1 = holder1.value();
                BlockState blockstate1 = p_309830_.getFluidState(p_310327_).createLegacyBlock();
                p_309830_.setBlock(p_310327_, blockstate1, 4);

                if (configuredfeature1.place(p_309830_, p_311976_, p_309951_, p_310327_))
                {
                    if (p_309830_.getBlockState(p_310327_) == blockstate1)
                    {
                        p_309830_.sendBlockUpdated(p_310327_, p_312382_, blockstate1, 2);
                    }

                    return true;
                }
                else
                {
                    p_309830_.setBlock(p_310327_, p_312382_, 4);
                    return false;
                }
            }
        }
    }

    private static boolean isTwoByTwoSapling(BlockState p_310256_, BlockGetter p_311754_, BlockPos p_312442_, int p_310725_, int p_310118_)
    {
        Block block = p_310256_.getBlock();
        return p_311754_.getBlockState(p_312442_.offset(p_310725_, 0, p_310118_)).is(block)
               && p_311754_.getBlockState(p_312442_.offset(p_310725_ + 1, 0, p_310118_)).is(block)
               && p_311754_.getBlockState(p_312442_.offset(p_310725_, 0, p_310118_ + 1)).is(block)
               && p_311754_.getBlockState(p_312442_.offset(p_310725_ + 1, 0, p_310118_ + 1)).is(block);
    }

    private boolean hasFlowers(LevelAccessor p_312531_, BlockPos p_312326_)
    {
        for (BlockPos blockpos : BlockPos.MutableBlockPos.betweenClosed(
                    p_312326_.below().north(2).west(2), p_312326_.above().south(2).east(2)
                ))
        {
            if (p_312531_.getBlockState(blockpos).is(BlockTags.FLOWERS))
            {
                return true;
            }
        }

        return false;
    }
}
