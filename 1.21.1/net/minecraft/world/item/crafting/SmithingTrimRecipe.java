package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.level.Level;

public class SmithingTrimRecipe implements SmithingRecipe
{
    final Ingredient template;
    final Ingredient base;
    final Ingredient addition;

    public SmithingTrimRecipe(Ingredient p_267298_, Ingredient p_266862_, Ingredient p_267050_)
    {
        this.template = p_267298_;
        this.base = p_266862_;
        this.addition = p_267050_;
    }

    public boolean matches(SmithingRecipeInput p_344106_, Level p_266798_)
    {
        return this.template.test(p_344106_.template()) && this.base.test(p_344106_.base()) && this.addition.test(p_344106_.addition());
    }

    public ItemStack assemble(SmithingRecipeInput p_344440_, HolderLookup.Provider p_330268_)
    {
        ItemStack itemstack = p_344440_.base();

        if (this.base.test(itemstack))
        {
            Optional<Holder.Reference<TrimMaterial>> optional = TrimMaterials.getFromIngredient(p_330268_, p_344440_.addition());
            Optional<Holder.Reference<TrimPattern>> optional1 = TrimPatterns.getFromTemplate(p_330268_, p_344440_.template());

            if (optional.isPresent() && optional1.isPresent())
            {
                ArmorTrim armortrim = itemstack.get(DataComponents.TRIM);

                if (armortrim != null && armortrim.hasPatternAndMaterial(optional1.get(), optional.get()))
                {
                    return ItemStack.EMPTY;
                }

                ItemStack itemstack1 = itemstack.copyWithCount(1);
                itemstack1.set(DataComponents.TRIM, new ArmorTrim(optional.get(), optional1.get()));
                return itemstack1;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider p_335603_)
    {
        ItemStack itemstack = new ItemStack(Items.IRON_CHESTPLATE);
        Optional<Holder.Reference<TrimPattern>> optional = p_335603_.lookupOrThrow(Registries.TRIM_PATTERN).listElements().findFirst();
        Optional<Holder.Reference<TrimMaterial>> optional1 = p_335603_.lookupOrThrow(Registries.TRIM_MATERIAL).get(TrimMaterials.REDSTONE);

        if (optional.isPresent() && optional1.isPresent())
        {
            itemstack.set(DataComponents.TRIM, new ArmorTrim(optional1.get(), optional.get()));
        }

        return itemstack;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack p_266762_)
    {
        return this.template.test(p_266762_);
    }

    @Override
    public boolean isBaseIngredient(ItemStack p_266795_)
    {
        return this.base.test(p_266795_);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack p_266922_)
    {
        return this.addition.test(p_266922_);
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return RecipeSerializer.SMITHING_TRIM;
    }

    @Override
    public boolean isIncomplete()
    {
        return Stream.of(this.template, this.base, this.addition).anyMatch(Ingredient::isEmpty);
    }

    public static class Serializer implements RecipeSerializer<SmithingTrimRecipe>
    {
        private static final MapCodec<SmithingTrimRecipe> CODEC = RecordCodecBuilder.mapCodec(
                    p_297394_ -> p_297394_.group(
                        Ingredient.CODEC.fieldOf("template").forGetter(p_298441_ -> p_298441_.template),
                        Ingredient.CODEC.fieldOf("base").forGetter(p_297838_ -> p_297838_.base),
                        Ingredient.CODEC.fieldOf("addition").forGetter(p_299309_ -> p_299309_.addition)
                    )
                    .apply(p_297394_, SmithingTrimRecipe::new)
                );
        public static final StreamCodec<RegistryFriendlyByteBuf, SmithingTrimRecipe> STREAM_CODEC = StreamCodec.of(
                    SmithingTrimRecipe.Serializer::toNetwork, SmithingTrimRecipe.Serializer::fromNetwork
                );

        @Override
        public MapCodec<SmithingTrimRecipe> codec()
        {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SmithingTrimRecipe> streamCodec()
        {
            return STREAM_CODEC;
        }

        private static SmithingTrimRecipe fromNetwork(RegistryFriendlyByteBuf p_333367_)
        {
            Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(p_333367_);
            Ingredient ingredient1 = Ingredient.CONTENTS_STREAM_CODEC.decode(p_333367_);
            Ingredient ingredient2 = Ingredient.CONTENTS_STREAM_CODEC.decode(p_333367_);
            return new SmithingTrimRecipe(ingredient, ingredient1, ingredient2);
        }

        private static void toNetwork(RegistryFriendlyByteBuf p_335485_, SmithingTrimRecipe p_335201_)
        {
            Ingredient.CONTENTS_STREAM_CODEC.encode(p_335485_, p_335201_.template);
            Ingredient.CONTENTS_STREAM_CODEC.encode(p_335485_, p_335201_.base);
            Ingredient.CONTENTS_STREAM_CODEC.encode(p_335485_, p_335201_.addition);
        }
    }
}
