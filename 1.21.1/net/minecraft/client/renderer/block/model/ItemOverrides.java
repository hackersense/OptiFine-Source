package net.minecraft.client.renderer.block.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.optifine.Config;
import net.optifine.ItemOverrideCache;

public class ItemOverrides
{
    public static final ItemOverrides EMPTY = new ItemOverrides();
    public static final float NO_OVERRIDE = Float.NEGATIVE_INFINITY;
    private final ItemOverrides.BakedOverride[] overrides;
    private final ResourceLocation[] properties;
    private ItemOverrideCache itemOverrideCache;
    public static ResourceLocation lastModelLocation = null;

    private ItemOverrides()
    {
        this.overrides = new ItemOverrides.BakedOverride[0];
        this.properties = new ResourceLocation[0];
    }

    public ItemOverrides(ModelBaker p_251211_, BlockModel p_111741_, List<ItemOverride> p_111743_)
    {
        this(p_251211_, p_111741_, p_111743_, p_251211_.getModelTextureGetter());
    }

    public ItemOverrides(
        ModelBaker modelBakeryIn, UnbakedModel blockModelIn, List<ItemOverride> itemOverridesIn, Function<Material, TextureAtlasSprite> spriteGetter
    )
    {
        this.properties = itemOverridesIn.stream()
                         .flatMap(ItemOverride::getPredicates)
                         .map(ItemOverride.Predicate::getProperty)
                         .distinct()
                         .toArray(ResourceLocation[]::new);
        Object2IntMap<ResourceLocation> object2intmap = new Object2IntOpenHashMap<>();

        for (int i = 0; i < this.properties.length; i++)
        {
            object2intmap.put(this.properties[i], i);
        }

        List<ItemOverrides.BakedOverride> list = Lists.newArrayList();

        for (int j = itemOverridesIn.size() - 1; j >= 0; j--)
        {
            ItemOverride itemoverride = itemOverridesIn.get(j);
            BakedModel bakedmodel = this.bakeModel(modelBakeryIn, blockModelIn, itemoverride, spriteGetter);
            ItemOverrides.PropertyMatcher[] aitemoverrides$propertymatcher = itemoverride.getPredicates().map(checkIn ->
            {
                int k = object2intmap.getInt(checkIn.getProperty());
                return new ItemOverrides.PropertyMatcher(k, checkIn.getValue());
            }).toArray(ItemOverrides.PropertyMatcher[]::new);
            list.add(new ItemOverrides.BakedOverride(aitemoverrides$propertymatcher, bakedmodel));
            ItemOverrides.BakedOverride itemoverrides$bakedoverride = list.get(list.size() - 1);
            itemoverrides$bakedoverride.location = itemoverride.getModel();
        }

        this.overrides = list.toArray(new ItemOverrides.BakedOverride[0]);

        if (itemOverridesIn.size() > 65)
        {
            this.itemOverrideCache = ItemOverrideCache.make(itemOverridesIn);
        }
    }

    @Nullable
    private BakedModel bakeModel(
        ModelBaker modelBakeryIn, UnbakedModel blockModelIn, ItemOverride itemOverrideIn, Function<Material, TextureAtlasSprite> spriteGetter
    )
    {
        UnbakedModel unbakedmodel = modelBakeryIn.getModel(itemOverrideIn.getModel());
        return Objects.equals(unbakedmodel, blockModelIn) ? null : modelBakeryIn.bake(itemOverrideIn.getModel(), BlockModelRotation.X0_Y0, spriteGetter);
    }

    @Nullable
    public BakedModel resolve(BakedModel p_173465_, ItemStack p_173466_, @Nullable ClientLevel p_173467_, @Nullable LivingEntity p_173468_, int p_173469_)
    {
        boolean flag = Config.isCustomItems();

        if (flag)
        {
            lastModelLocation = null;
        }

        if (this.overrides.length != 0)
        {
            if (this.itemOverrideCache != null)
            {
                Integer integer = this.itemOverrideCache.getModelIndex(p_173466_, p_173467_, p_173468_);

                if (integer != null)
                {
                    int k = integer;

                    if (k >= 0 && k < this.overrides.length)
                    {
                        if (flag)
                        {
                            lastModelLocation = this.overrides[k].location;
                        }

                        BakedModel bakedmodel = this.overrides[k].model;

                        if (bakedmodel != null)
                        {
                            return bakedmodel;
                        }
                    }

                    return p_173465_;
                }
            }

            int j = this.properties.length;
            float[] afloat = new float[j];

            for (int i = 0; i < j; i++)
            {
                ResourceLocation resourcelocation = this.properties[i];
                ItemPropertyFunction itempropertyfunction = ItemProperties.getProperty(p_173466_, resourcelocation);

                if (itempropertyfunction != null)
                {
                    afloat[i] = itempropertyfunction.call(p_173466_, p_173467_, p_173468_, p_173469_);
                }
                else
                {
                    afloat[i] = Float.NEGATIVE_INFINITY;
                }
            }

            for (int l = 0; l < this.overrides.length; l++)
            {
                ItemOverrides.BakedOverride itemoverrides$bakedoverride = this.overrides[l];

                if (itemoverrides$bakedoverride.test(afloat))
                {
                    BakedModel bakedmodel1 = itemoverrides$bakedoverride.model;

                    if (flag)
                    {
                        lastModelLocation = itemoverrides$bakedoverride.location;
                    }

                    if (this.itemOverrideCache != null)
                    {
                        this.itemOverrideCache.putModelIndex(p_173466_, p_173467_, p_173468_, l);
                    }

                    if (bakedmodel1 == null)
                    {
                        return p_173465_;
                    }

                    return bakedmodel1;
                }
            }
        }

        return p_173465_;
    }

    public ImmutableList<ItemOverrides.BakedOverride> getOverrides()
    {
        return ImmutableList.copyOf(this.overrides);
    }

    static class BakedOverride
    {
        private final ItemOverrides.PropertyMatcher[] matchers;
        @Nullable
        final BakedModel model;
        private ResourceLocation location;

        BakedOverride(ItemOverrides.PropertyMatcher[] p_173483_, @Nullable BakedModel p_173484_)
        {
            this.matchers = p_173483_;
            this.model = p_173484_;
        }

        boolean test(float[] p_173486_)
        {
            for (ItemOverrides.PropertyMatcher itemoverrides$propertymatcher : this.matchers)
            {
                float f = p_173486_[itemoverrides$propertymatcher.index];

                if (f < itemoverrides$propertymatcher.value)
                {
                    return false;
                }
            }

            return true;
        }
    }

    static class PropertyMatcher
    {
        public final int index;
        public final float value;

        PropertyMatcher(int p_173490_, float p_173491_)
        {
            this.index = p_173490_;
            this.value = p_173491_;
        }
    }
}
