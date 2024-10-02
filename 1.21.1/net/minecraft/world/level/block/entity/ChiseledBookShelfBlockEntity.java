package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import org.slf4j.Logger;

public class ChiseledBookShelfBlockEntity extends BlockEntity implements Container
{
    public static final int MAX_BOOKS_IN_STORAGE = 6;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
    private int lastInteractedSlot = -1;

    public ChiseledBookShelfBlockEntity(BlockPos p_249541_, BlockState p_251752_)
    {
        super(BlockEntityType.CHISELED_BOOKSHELF, p_249541_, p_251752_);
    }

    private void updateState(int p_261806_)
    {
        if (p_261806_ >= 0 && p_261806_ < 6)
        {
            this.lastInteractedSlot = p_261806_;
            BlockState blockstate = this.getBlockState();

            for (int i = 0; i < ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); i++)
            {
                boolean flag = !this.getItem(i).isEmpty();
                BooleanProperty booleanproperty = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i);
                blockstate = blockstate.setValue(booleanproperty, Boolean.valueOf(flag));
            }

            Objects.requireNonNull(this.level).setBlock(this.worldPosition, blockstate, 3);
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.worldPosition, GameEvent.Context.of(blockstate));
        }
        else
        {
            LOGGER.error("Expected slot 0-5, got {}", p_261806_);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag p_332537_, HolderLookup.Provider p_330403_)
    {
        super.loadAdditional(p_332537_, p_330403_);
        this.items.clear();
        ContainerHelper.loadAllItems(p_332537_, this.items, p_330403_);
        this.lastInteractedSlot = p_332537_.getInt("last_interacted_slot");
    }

    @Override
    protected void saveAdditional(CompoundTag p_251872_, HolderLookup.Provider p_331563_)
    {
        super.saveAdditional(p_251872_, p_331563_);
        ContainerHelper.saveAllItems(p_251872_, this.items, true, p_331563_);
        p_251872_.putInt("last_interacted_slot", this.lastInteractedSlot);
    }

    public int count()
    {
        return (int)this.items.stream().filter(Predicate.not(ItemStack::isEmpty)).count();
    }

    @Override
    public void clearContent()
    {
        this.items.clear();
    }

    @Override
    public int getContainerSize()
    {
        return 6;
    }

    @Override
    public boolean isEmpty()
    {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int p_256203_)
    {
        return this.items.get(p_256203_);
    }

    @Override
    public ItemStack removeItem(int p_255828_, int p_255673_)
    {
        ItemStack itemstack = Objects.requireNonNullElse(this.items.get(p_255828_), ItemStack.EMPTY);
        this.items.set(p_255828_, ItemStack.EMPTY);

        if (!itemstack.isEmpty())
        {
            this.updateState(p_255828_);
        }

        return itemstack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_255874_)
    {
        return this.removeItem(p_255874_, 1);
    }

    @Override
    public void setItem(int p_256610_, ItemStack p_255789_)
    {
        if (p_255789_.is(ItemTags.BOOKSHELF_BOOKS))
        {
            this.items.set(p_256610_, p_255789_);
            this.updateState(p_256610_);
        }
        else if (p_255789_.isEmpty())
        {
            this.removeItem(p_256610_, 1);
        }
    }

    @Override
    public boolean canTakeItem(Container p_282172_, int p_281387_, ItemStack p_283257_)
    {
        return p_282172_.hasAnyMatching(
                   p_327306_ -> p_327306_.isEmpty()
                   ? true
                   : ItemStack.isSameItemSameComponents(p_283257_, p_327306_) && p_327306_.getCount() + p_283257_.getCount() <= p_282172_.getMaxStackSize(p_327306_)
               );
    }

    @Override
    public int getMaxStackSize()
    {
        return 1;
    }

    @Override
    public boolean stillValid(Player p_256481_)
    {
        return Container.stillValidBlockEntity(this, p_256481_);
    }

    @Override
    public boolean canPlaceItem(int p_256567_, ItemStack p_255922_)
    {
        return p_255922_.is(ItemTags.BOOKSHELF_BOOKS) && this.getItem(p_256567_).isEmpty() && p_255922_.getCount() == this.getMaxStackSize();
    }

    public int getLastInteractedSlot()
    {
        return this.lastInteractedSlot;
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput p_333011_)
    {
        super.applyImplicitComponents(p_333011_);
        p_333011_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.items);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_331494_)
    {
        super.collectImplicitComponents(p_331494_);
        p_331494_.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.items));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag p_329882_)
    {
        p_329882_.remove("Items");
    }
}
