package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

public abstract class RandomizableContainerBlockEntity extends BaseContainerBlockEntity implements RandomizableContainer
{
    @Nullable
    protected ResourceKey<LootTable> lootTable;
    protected long lootTableSeed = 0L;

    protected RandomizableContainerBlockEntity(BlockEntityType<?> p_155629_, BlockPos p_155630_, BlockState p_155631_)
    {
        super(p_155629_, p_155630_, p_155631_);
    }

    @Nullable
    @Override
    public ResourceKey<LootTable> getLootTable()
    {
        return this.lootTable;
    }

    @Override
    public void setLootTable(@Nullable ResourceKey<LootTable> p_328444_)
    {
        this.lootTable = p_328444_;
    }

    @Override
    public long getLootTableSeed()
    {
        return this.lootTableSeed;
    }

    @Override
    public void setLootTableSeed(long p_311658_)
    {
        this.lootTableSeed = p_311658_;
    }

    @Override
    public boolean isEmpty()
    {
        this.unpackLootTable(null);
        return super.isEmpty();
    }

    @Override
    public ItemStack getItem(int p_59611_)
    {
        this.unpackLootTable(null);
        return super.getItem(p_59611_);
    }

    @Override
    public ItemStack removeItem(int p_59613_, int p_59614_)
    {
        this.unpackLootTable(null);
        return super.removeItem(p_59613_, p_59614_);
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_59630_)
    {
        this.unpackLootTable(null);
        return super.removeItemNoUpdate(p_59630_);
    }

    @Override
    public void setItem(int p_59616_, ItemStack p_59617_)
    {
        this.unpackLootTable(null);
        super.setItem(p_59616_, p_59617_);
    }

    @Override
    public boolean canOpen(Player p_59643_)
    {
        return super.canOpen(p_59643_) && (this.lootTable == null || !p_59643_.isSpectator());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int p_59637_, Inventory p_59638_, Player p_59639_)
    {
        if (this.canOpen(p_59639_))
        {
            this.unpackLootTable(p_59638_.player);
            return this.createMenu(p_59637_, p_59638_);
        }
        else
        {
            return null;
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput p_330597_)
    {
        super.applyImplicitComponents(p_330597_);
        SeededContainerLoot seededcontainerloot = p_330597_.get(DataComponents.CONTAINER_LOOT);

        if (seededcontainerloot != null)
        {
            this.lootTable = seededcontainerloot.lootTable();
            this.lootTableSeed = seededcontainerloot.seed();
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_329123_)
    {
        super.collectImplicitComponents(p_329123_);

        if (this.lootTable != null)
        {
            p_329123_.set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(this.lootTable, this.lootTableSeed));
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag p_331651_)
    {
        super.removeComponentsFromTag(p_331651_);
        p_331651_.remove("LootTable");
        p_331651_.remove("LootTableSeed");
    }
}
