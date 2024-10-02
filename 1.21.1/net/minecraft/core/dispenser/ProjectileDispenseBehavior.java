package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class ProjectileDispenseBehavior extends DefaultDispenseItemBehavior
{
    private final ProjectileItem projectileItem;
    private final ProjectileItem.DispenseConfig dispenseConfig;

    public ProjectileDispenseBehavior(Item p_328671_)
    {
        if (p_328671_ instanceof ProjectileItem projectileitem)
        {
            this.projectileItem = projectileitem;
            this.dispenseConfig = projectileitem.createDispenseConfig();
        }
        else
        {
            throw new IllegalArgumentException(p_328671_ + " not instance of " + ProjectileItem.class.getSimpleName());
        }
    }

    @Override
    public ItemStack execute(BlockSource p_334330_, ItemStack p_328814_)
    {
        Level level = p_334330_.level();
        Direction direction = p_334330_.state().getValue(DispenserBlock.FACING);
        Position position = this.dispenseConfig.positionFunction().getDispensePosition(p_334330_, direction);
        Projectile projectile = this.projectileItem.asProjectile(level, position, p_328814_, direction);
        this.projectileItem
        .shoot(
            projectile,
            (double)direction.getStepX(),
            (double)direction.getStepY(),
            (double)direction.getStepZ(),
            this.dispenseConfig.power(),
            this.dispenseConfig.uncertainty()
        );
        level.addFreshEntity(projectile);
        p_328814_.shrink(1);
        return p_328814_;
    }

    @Override
    protected void playSound(BlockSource p_330598_)
    {
        p_330598_.level().levelEvent(this.dispenseConfig.overrideDispenseEvent().orElse(1002), p_330598_.pos(), 0);
    }
}
