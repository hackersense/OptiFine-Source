package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.state.BlockState;

public class CrafterBlockEntity extends RandomizableContainerBlockEntity implements CraftingContainer
{
    public static final int CONTAINER_WIDTH = 3;
    public static final int CONTAINER_HEIGHT = 3;
    public static final int CONTAINER_SIZE = 9;
    public static final int SLOT_DISABLED = 1;
    public static final int SLOT_ENABLED = 0;
    public static final int DATA_TRIGGERED = 9;
    public static final int NUM_DATA = 10;
    private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
    private int craftingTicksRemaining = 0;
    protected final ContainerData containerData = new ContainerData()
    {
        private final int[] slotStates = new int[9];
        private int triggered = 0;
        @Override
        public int get(int p_310435_)
        {
            return p_310435_ == 9 ? this.triggered : this.slotStates[p_310435_];
        }
        @Override
        public void set(int p_313229_, int p_312585_)
        {
            if (p_313229_ == 9)
            {
                this.triggered = p_312585_;
            }
            else
            {
                this.slotStates[p_313229_] = p_312585_;
            }
        }
        @Override
        public int getCount()
        {
            return 10;
        }
    };

    public CrafterBlockEntity(BlockPos p_309972_, BlockState p_313058_)
    {
        super(BlockEntityType.CRAFTER, p_309972_, p_313058_);
    }

    @Override
    protected Component getDefaultName()
    {
        return Component.translatable("container.crafter");
    }

    @Override
    protected AbstractContainerMenu createMenu(int p_312650_, Inventory p_309858_)
    {
        return new CrafterMenu(p_312650_, p_309858_, this, this.containerData);
    }

    public void setSlotState(int p_310046_, boolean p_310331_)
    {
        if (this.slotCanBeDisabled(p_310046_))
        {
            this.containerData.set(p_310046_, p_310331_ ? 0 : 1);
            this.setChanged();
        }
    }

    public boolean isSlotDisabled(int p_312222_)
    {
        return p_312222_ >= 0 && p_312222_ < 9 ? this.containerData.get(p_312222_) == 1 : false;
    }

    @Override
    public boolean canPlaceItem(int p_311324_, ItemStack p_312777_)
    {
        if (this.containerData.get(p_311324_) == 1)
        {
            return false;
        }
        else
        {
            ItemStack itemstack = this.items.get(p_311324_);
            int i = itemstack.getCount();

            if (i >= itemstack.getMaxStackSize())
            {
                return false;
            }
            else
            {
                return itemstack.isEmpty() ? true : !this.smallerStackExist(i, itemstack, p_311324_);
            }
        }
    }

    private boolean smallerStackExist(int p_312152_, ItemStack p_309554_, int p_312872_)
    {
        for (int i = p_312872_ + 1; i < 9; i++)
        {
            if (!this.isSlotDisabled(i))
            {
                ItemStack itemstack = this.getItem(i);

                if (itemstack.isEmpty() || itemstack.getCount() < p_312152_ && ItemStack.isSameItemSameComponents(itemstack, p_309554_))
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void loadAdditional(CompoundTag p_328373_, HolderLookup.Provider p_328741_)
    {
        super.loadAdditional(p_328373_, p_328741_);
        this.craftingTicksRemaining = p_328373_.getInt("crafting_ticks_remaining");
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);

        if (!this.tryLoadLootTable(p_328373_))
        {
            ContainerHelper.loadAllItems(p_328373_, this.items, p_328741_);
        }

        int[] aint = p_328373_.getIntArray("disabled_slots");

        for (int i = 0; i < 9; i++)
        {
            this.containerData.set(i, 0);
        }

        for (int j : aint)
        {
            if (this.slotCanBeDisabled(j))
            {
                this.containerData.set(j, 1);
            }
        }

        this.containerData.set(9, p_328373_.getInt("triggered"));
    }

    @Override
    protected void saveAdditional(CompoundTag p_309594_, HolderLookup.Provider p_330681_)
    {
        super.saveAdditional(p_309594_, p_330681_);
        p_309594_.putInt("crafting_ticks_remaining", this.craftingTicksRemaining);

        if (!this.trySaveLootTable(p_309594_))
        {
            ContainerHelper.saveAllItems(p_309594_, this.items, p_330681_);
        }

        this.addDisabledSlots(p_309594_);
        this.addTriggered(p_309594_);
    }

    @Override
    public int getContainerSize()
    {
        return 9;
    }

    @Override
    public boolean isEmpty()
    {
        for (ItemStack itemstack : this.items)
        {
            if (!itemstack.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int p_310446_)
    {
        return this.items.get(p_310446_);
    }

    @Override
    public void setItem(int p_312882_, ItemStack p_311521_)
    {
        if (this.isSlotDisabled(p_312882_))
        {
            this.setSlotState(p_312882_, true);
        }

        super.setItem(p_312882_, p_311521_);
    }

    @Override
    public boolean stillValid(Player p_311318_)
    {
        return Container.stillValidBlockEntity(this, p_311318_);
    }

    @Override
    public NonNullList<ItemStack> getItems()
    {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> p_311420_)
    {
        this.items = p_311420_;
    }

    @Override
    public int getWidth()
    {
        return 3;
    }

    @Override
    public int getHeight()
    {
        return 3;
    }

    @Override
    public void fillStackedContents(StackedContents p_310482_)
    {
        for (ItemStack itemstack : this.items)
        {
            p_310482_.accountSimpleStack(itemstack);
        }
    }

    private void addDisabledSlots(CompoundTag p_309756_)
    {
        IntList intlist = new IntArrayList();

        for (int i = 0; i < 9; i++)
        {
            if (this.isSlotDisabled(i))
            {
                intlist.add(i);
            }
        }

        p_309756_.putIntArray("disabled_slots", intlist);
    }

    private void addTriggered(CompoundTag p_312165_)
    {
        p_312165_.putInt("triggered", this.containerData.get(9));
    }

    public void setTriggered(boolean p_311394_)
    {
        this.containerData.set(9, p_311394_ ? 1 : 0);
    }

    @VisibleForTesting
    public boolean isTriggered()
    {
        return this.containerData.get(9) == 1;
    }

    public static void serverTick(Level p_311764_, BlockPos p_309568_, BlockState p_311393_, CrafterBlockEntity p_313070_)
    {
        int i = p_313070_.craftingTicksRemaining - 1;

        if (i >= 0)
        {
            p_313070_.craftingTicksRemaining = i;

            if (i == 0)
            {
                p_311764_.setBlock(p_309568_, p_311393_.setValue(CrafterBlock.CRAFTING, Boolean.valueOf(false)), 3);
            }
        }
    }

    public void setCraftingTicksRemaining(int p_312384_)
    {
        this.craftingTicksRemaining = p_312384_;
    }

    public int getRedstoneSignal()
    {
        int i = 0;

        for (int j = 0; j < this.getContainerSize(); j++)
        {
            ItemStack itemstack = this.getItem(j);

            if (!itemstack.isEmpty() || this.isSlotDisabled(j))
            {
                i++;
            }
        }

        return i;
    }

    private boolean slotCanBeDisabled(int p_309429_)
    {
        return p_309429_ > -1 && p_309429_ < 9 && this.items.get(p_309429_).isEmpty();
    }
}
