package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public interface Recipe<T extends RecipeInput>
{
    Codec < Recipe<? >> CODEC = BuiltInRegistries.RECIPE_SERIALIZER.byNameCodec().dispatch(Recipe::getSerializer, RecipeSerializer::codec);
    StreamCodec < RegistryFriendlyByteBuf, Recipe<? >> STREAM_CODEC = ByteBufCodecs.registry(Registries.RECIPE_SERIALIZER)
            .dispatch(Recipe::getSerializer, RecipeSerializer::streamCodec);

    boolean matches(T p_343697_, Level p_44003_);

    ItemStack assemble(T p_343633_, HolderLookup.Provider p_332698_);

    boolean canCraftInDimensions(int p_43999_, int p_44000_);

    ItemStack getResultItem(HolderLookup.Provider p_331967_);

default NonNullList<ItemStack> getRemainingItems(T p_344178_)
    {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(p_344178_.size(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); i++)
        {
            Item item = p_344178_.getItem(i).getItem();

            if (item.hasCraftingRemainingItem())
            {
                nonnulllist.set(i, new ItemStack(item.getCraftingRemainingItem()));
            }
        }

        return nonnulllist;
    }

default NonNullList<Ingredient> getIngredients()
    {
        return NonNullList.create();
    }

default boolean isSpecial()
    {
        return false;
    }

default boolean showNotification()
    {
        return true;
    }

default String getGroup()
    {
        return "";
    }

default ItemStack getToastSymbol()
    {
        return new ItemStack(Blocks.CRAFTING_TABLE);
    }

    RecipeSerializer<?> getSerializer();

    RecipeType<?> getType();

default boolean isIncomplete()
    {
        NonNullList<Ingredient> nonnulllist = this.getIngredients();
        return nonnulllist.isEmpty() || nonnulllist.stream().anyMatch(p_151268_ -> p_151268_.getItems().length == 0);
    }
}
