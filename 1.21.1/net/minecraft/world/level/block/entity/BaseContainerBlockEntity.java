package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BaseContainerBlockEntity extends BlockEntity implements Container, MenuProvider, Nameable
{
    private LockCode lockKey = LockCode.NO_LOCK;
    @Nullable
    private Component name;

    protected BaseContainerBlockEntity(BlockEntityType<?> p_155076_, BlockPos p_155077_, BlockState p_155078_)
    {
        super(p_155076_, p_155077_, p_155078_);
    }

    @Override
    protected void loadAdditional(CompoundTag p_335335_, HolderLookup.Provider p_329555_)
    {
        super.loadAdditional(p_335335_, p_329555_);
        this.lockKey = LockCode.fromTag(p_335335_);

        if (p_335335_.contains("CustomName", 8))
        {
            this.name = parseCustomNameSafe(p_335335_.getString("CustomName"), p_329555_);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag p_187461_, HolderLookup.Provider p_335192_)
    {
        super.saveAdditional(p_187461_, p_335192_);
        this.lockKey.addToTag(p_187461_);

        if (this.name != null)
        {
            p_187461_.putString("CustomName", Component.Serializer.toJson(this.name, p_335192_));
        }
    }

    @Override
    public Component getName()
    {
        return this.name != null ? this.name : this.getDefaultName();
    }

    @Override
    public Component getDisplayName()
    {
        return this.getName();
    }

    @Nullable
    @Override
    public Component getCustomName()
    {
        return this.name;
    }

    protected abstract Component getDefaultName();

    public boolean canOpen(Player p_58645_)
    {
        return canUnlock(p_58645_, this.lockKey, this.getDisplayName());
    }

    public static boolean canUnlock(Player p_58630_, LockCode p_58631_, Component p_58632_)
    {
        if (!p_58630_.isSpectator() && !p_58631_.unlocksWith(p_58630_.getMainHandItem()))
        {
            p_58630_.displayClientMessage(Component.translatable("container.isLocked", p_58632_), true);
            p_58630_.playNotifySound(SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
            return false;
        }
        else
        {
            return true;
        }
    }

    protected abstract NonNullList<ItemStack> getItems();

    protected abstract void setItems(NonNullList<ItemStack> p_330472_);

    @Override
    public boolean isEmpty()
    {
        for (ItemStack itemstack : this.getItems())
        {
            if (!itemstack.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int p_334660_)
    {
        return this.getItems().get(p_334660_);
    }

    @Override
    public ItemStack removeItem(int p_333934_, int p_332088_)
    {
        ItemStack itemstack = ContainerHelper.removeItem(this.getItems(), p_333934_, p_332088_);

        if (!itemstack.isEmpty())
        {
            this.setChanged();
        }

        return itemstack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_329940_)
    {
        return ContainerHelper.takeItem(this.getItems(), p_329940_);
    }

    @Override
    public void setItem(int p_331067_, ItemStack p_333112_)
    {
        this.getItems().set(p_331067_, p_333112_);
        p_333112_.limitSize(this.getMaxStackSize(p_333112_));
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player p_330935_)
    {
        return Container.stillValidBlockEntity(this, p_330935_);
    }

    @Override
    public void clearContent()
    {
        this.getItems().clear();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int p_58641_, Inventory p_58642_, Player p_58643_)
    {
        return this.canOpen(p_58643_) ? this.createMenu(p_58641_, p_58642_) : null;
    }

    protected abstract AbstractContainerMenu createMenu(int p_58627_, Inventory p_58628_);

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput p_329127_)
    {
        super.applyImplicitComponents(p_329127_);
        this.name = p_329127_.get(DataComponents.CUSTOM_NAME);
        this.lockKey = p_329127_.getOrDefault(DataComponents.LOCK, LockCode.NO_LOCK);
        p_329127_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.getItems());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_336292_)
    {
        super.collectImplicitComponents(p_336292_);
        p_336292_.set(DataComponents.CUSTOM_NAME, this.name);

        if (!this.lockKey.equals(LockCode.NO_LOCK))
        {
            p_336292_.set(DataComponents.LOCK, this.lockKey);
        }

        p_336292_.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag p_329140_)
    {
        p_329140_.remove("CustomName");
        p_329140_.remove("Lock");
        p_329140_.remove("Items");
    }
}
