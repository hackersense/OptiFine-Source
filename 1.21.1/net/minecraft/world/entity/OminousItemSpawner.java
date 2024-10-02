package net.minecraft.world.entity;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

public class OminousItemSpawner extends Entity
{
    private static final int SPAWN_ITEM_DELAY_MIN = 60;
    private static final int SPAWN_ITEM_DELAY_MAX = 120;
    private static final String TAG_SPAWN_ITEM_AFTER_TICKS = "spawn_item_after_ticks";
    private static final String TAG_ITEM = "item";
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(OminousItemSpawner.class, EntityDataSerializers.ITEM_STACK);
    public static final int TICKS_BEFORE_ABOUT_TO_SPAWN_SOUND = 36;
    private long spawnItemAfterTicks;

    public OminousItemSpawner(EntityType <? extends OminousItemSpawner > p_330436_, Level p_334777_)
    {
        super(p_330436_, p_334777_);
        this.noPhysics = true;
    }

    public static OminousItemSpawner create(Level p_328154_, ItemStack p_332415_)
    {
        OminousItemSpawner ominousitemspawner = new OminousItemSpawner(EntityType.OMINOUS_ITEM_SPAWNER, p_328154_);
        ominousitemspawner.spawnItemAfterTicks = (long)p_328154_.random.nextIntBetweenInclusive(60, 120);
        ominousitemspawner.setItem(p_332415_);
        return ominousitemspawner;
    }

    @Override
    public void tick()
    {
        super.tick();

        if (this.level().isClientSide)
        {
            this.tickClient();
        }
        else
        {
            this.tickServer();
        }
    }

    private void tickServer()
    {
        if ((long)this.tickCount == this.spawnItemAfterTicks - 36L)
        {
            this.level().playSound(null, this.blockPosition(), SoundEvents.TRIAL_SPAWNER_ABOUT_TO_SPAWN_ITEM, SoundSource.NEUTRAL);
        }

        if ((long)this.tickCount >= this.spawnItemAfterTicks)
        {
            this.spawnItem();
            this.kill();
        }
    }

    private void tickClient()
    {
        if (this.level().getGameTime() % 5L == 0L)
        {
            this.addParticles();
        }
    }

    private void spawnItem()
    {
        Level level = this.level();
        ItemStack itemstack = this.getItem();

        if (!itemstack.isEmpty())
        {
            Entity entity;

            if (itemstack.getItem() instanceof ProjectileItem projectileitem)
            {
                Direction direction = Direction.DOWN;
                Projectile projectile = projectileitem.asProjectile(level, this.position(), itemstack, direction);
                projectile.setOwner(this);
                ProjectileItem.DispenseConfig projectileitem$dispenseconfig = projectileitem.createDispenseConfig();
                projectileitem.shoot(
                    projectile,
                    (double)direction.getStepX(),
                    (double)direction.getStepY(),
                    (double)direction.getStepZ(),
                    projectileitem$dispenseconfig.power(),
                    projectileitem$dispenseconfig.uncertainty()
                );
                projectileitem$dispenseconfig.overrideDispenseEvent().ifPresent(p_341275_ -> level.levelEvent(p_341275_, this.blockPosition(), 0));
                entity = projectile;
            }
            else
            {
                entity = new ItemEntity(level, this.getX(), this.getY(), this.getZ(), itemstack);
            }

            level.addFreshEntity(entity);
            level.levelEvent(3021, this.blockPosition(), 1);
            level.gameEvent(entity, GameEvent.ENTITY_PLACE, this.position());
            this.setItem(ItemStack.EMPTY);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_330200_)
    {
        p_330200_.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag p_331393_)
    {
        ItemStack itemstack = p_331393_.contains("item", 10)
                              ? ItemStack.parse(this.registryAccess(), p_331393_.getCompound("item")).orElse(ItemStack.EMPTY)
                              : ItemStack.EMPTY;
        this.setItem(itemstack);
        this.spawnItemAfterTicks = p_331393_.getLong("spawn_item_after_ticks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag p_329696_)
    {
        if (!this.getItem().isEmpty())
        {
            p_329696_.put("item", this.getItem().save(this.registryAccess()).copy());
        }

        p_329696_.putLong("spawn_item_after_ticks", this.spawnItemAfterTicks);
    }

    @Override
    protected boolean canAddPassenger(Entity p_332041_)
    {
        return false;
    }

    @Override
    protected boolean couldAcceptPassenger()
    {
        return false;
    }

    @Override
    protected void addPassenger(Entity p_333815_)
    {
        throw new IllegalStateException("Should never addPassenger without checking couldAcceptPassenger()");
    }

    @Override
    public PushReaction getPistonPushReaction()
    {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isIgnoringBlockTriggers()
    {
        return true;
    }

    public void addParticles()
    {
        Vec3 vec3 = this.position();
        int i = this.random.nextIntBetweenInclusive(1, 3);

        for (int j = 0; j < i; j++)
        {
            double d0 = 0.4;
            Vec3 vec31 = new Vec3(
                this.getX() + 0.4 * (this.random.nextGaussian() - this.random.nextGaussian()),
                this.getY() + 0.4 * (this.random.nextGaussian() - this.random.nextGaussian()),
                this.getZ() + 0.4 * (this.random.nextGaussian() - this.random.nextGaussian())
            );
            Vec3 vec32 = vec3.vectorTo(vec31);
            this.level().addParticle(ParticleTypes.OMINOUS_SPAWNING, vec3.x(), vec3.y(), vec3.z(), vec32.x(), vec32.y(), vec32.z());
        }
    }

    public ItemStack getItem()
    {
        return this.getEntityData().get(DATA_ITEM);
    }

    private void setItem(ItemStack p_328604_)
    {
        this.getEntityData().set(DATA_ITEM, p_328604_);
    }
}
