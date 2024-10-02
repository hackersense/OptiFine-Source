package net.minecraft.world.item;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class FoodOnAStickItem<T extends Entity & ItemSteerable> extends Item
{
    private final EntityType<T> canInteractWith;
    private final int consumeItemDamage;

    public FoodOnAStickItem(Item.Properties p_41307_, EntityType<T> p_41308_, int p_41309_)
    {
        super(p_41307_);
        this.canInteractWith = p_41308_;
        this.consumeItemDamage = p_41309_;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level p_41314_, Player p_41315_, InteractionHand p_41316_)
    {
        ItemStack itemstack = p_41315_.getItemInHand(p_41316_);

        if (p_41314_.isClientSide)
        {
            return InteractionResultHolder.pass(itemstack);
        }
        else
        {
            Entity entity = p_41315_.getControlledVehicle();

            if (p_41315_.isPassenger() && entity instanceof ItemSteerable itemsteerable && entity.getType() == this.canInteractWith && itemsteerable.boost())
            {
                EquipmentSlot equipmentslot = LivingEntity.getSlotForHand(p_41316_);
                ItemStack itemstack1 = itemstack.hurtAndConvertOnBreak(this.consumeItemDamage, Items.FISHING_ROD, p_41315_, equipmentslot);
                return InteractionResultHolder.success(itemstack1);
            }

            p_41315_.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.pass(itemstack);
        }
    }
}
