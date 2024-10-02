package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class BannerDuplicateRecipe extends CustomRecipe
{
    public BannerDuplicateRecipe(CraftingBookCategory p_250373_)
    {
        super(p_250373_);
    }

    public boolean matches(CraftingInput p_344586_, Level p_43786_)
    {
        DyeColor dyecolor = null;
        ItemStack itemstack = null;
        ItemStack itemstack1 = null;

        for (int i = 0; i < p_344586_.size(); i++)
        {
            ItemStack itemstack2 = p_344586_.getItem(i);

            if (!itemstack2.isEmpty())
            {
                Item item = itemstack2.getItem();

                if (!(item instanceof BannerItem))
                {
                    return false;
                }

                BannerItem banneritem = (BannerItem)item;

                if (dyecolor == null)
                {
                    dyecolor = banneritem.getColor();
                }
                else if (dyecolor != banneritem.getColor())
                {
                    return false;
                }

                int j = itemstack2.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().size();

                if (j > 6)
                {
                    return false;
                }

                if (j > 0)
                {
                    if (itemstack != null)
                    {
                        return false;
                    }

                    itemstack = itemstack2;
                }
                else
                {
                    if (itemstack1 != null)
                    {
                        return false;
                    }

                    itemstack1 = itemstack2;
                }
            }
        }

        return itemstack != null && itemstack1 != null;
    }

    public ItemStack assemble(CraftingInput p_344878_, HolderLookup.Provider p_333234_)
    {
        for (int i = 0; i < p_344878_.size(); i++)
        {
            ItemStack itemstack = p_344878_.getItem(i);

            if (!itemstack.isEmpty())
            {
                int j = itemstack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().size();

                if (j > 0 && j <= 6)
                {
                    return itemstack.copyWithCount(1);
                }
            }
        }

        return ItemStack.EMPTY;
    }

    public NonNullList<ItemStack> getRemainingItems(CraftingInput p_342084_)
    {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(p_342084_.size(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); i++)
        {
            ItemStack itemstack = p_342084_.getItem(i);

            if (!itemstack.isEmpty())
            {
                if (itemstack.getItem().hasCraftingRemainingItem())
                {
                    nonnulllist.set(i, new ItemStack(itemstack.getItem().getCraftingRemainingItem()));
                }
                else if (!itemstack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().isEmpty())
                {
                    nonnulllist.set(i, itemstack.copyWithCount(1));
                }
            }
        }

        return nonnulllist;
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return RecipeSerializer.BANNER_DUPLICATE;
    }

    @Override
    public boolean canCraftInDimensions(int p_43775_, int p_43776_)
    {
        return p_43775_ * p_43776_ >= 2;
    }
}
