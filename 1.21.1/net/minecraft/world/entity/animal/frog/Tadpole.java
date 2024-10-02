package net.minecraft.world.entity.animal.frog;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class Tadpole extends AbstractFish
{
    @VisibleForTesting
    public static int ticksToBeFrog = Math.abs(-24000);
    public static final float HITBOX_WIDTH = 0.4F;
    public static final float HITBOX_HEIGHT = 0.3F;
    private int age;
    protected static final ImmutableList < SensorType <? extends Sensor <? super Tadpole >>> SENSOR_TYPES = ImmutableList.of(
                SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.HURT_BY, SensorType.FROG_TEMPTATIONS
            );
    protected static final ImmutableList < MemoryModuleType<? >> MEMORY_TYPES = ImmutableList.of(
                MemoryModuleType.LOOK_TARGET,
                MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                MemoryModuleType.WALK_TARGET,
                MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                MemoryModuleType.PATH,
                MemoryModuleType.NEAREST_VISIBLE_ADULT,
                MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
                MemoryModuleType.IS_TEMPTED,
                MemoryModuleType.TEMPTING_PLAYER,
                MemoryModuleType.BREED_TARGET,
                MemoryModuleType.IS_PANICKING
            );

    public Tadpole(EntityType <? extends AbstractFish > p_218686_, Level p_218687_)
    {
        super(p_218686_, p_218687_);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    @Override
    protected PathNavigation createNavigation(Level p_218694_)
    {
        return new WaterBoundPathNavigation(this, p_218694_);
    }

    @Override
    protected Brain.Provider<Tadpole> brainProvider()
    {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> p_218696_)
    {
        return TadpoleAi.makeBrain(this.brainProvider().makeBrain(p_218696_));
    }

    @Override
    public Brain<Tadpole> getBrain()
    {
        return (Brain<Tadpole>)super.getBrain();
    }

    @Override
    protected SoundEvent getFlopSound()
    {
        return SoundEvents.TADPOLE_FLOP;
    }

    @Override
    protected void customServerAiStep()
    {
        this.level().getProfiler().push("tadpoleBrain");
        this.getBrain().tick((ServerLevel)this.level(), this);
        this.level().getProfiler().pop();
        this.level().getProfiler().push("tadpoleActivityUpdate");
        TadpoleAi.updateActivity(this);
        this.level().getProfiler().pop();
        super.customServerAiStep();
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 1.0).add(Attributes.MAX_HEALTH, 6.0);
    }

    @Override
    public void aiStep()
    {
        super.aiStep();

        if (!this.level().isClientSide)
        {
            this.setAge(this.age + 1);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag p_218709_)
    {
        super.addAdditionalSaveData(p_218709_);
        p_218709_.putInt("Age", this.age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag p_218698_)
    {
        super.readAdditionalSaveData(p_218698_);
        this.setAge(p_218698_.getInt("Age"));
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound()
    {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource p_218713_)
    {
        return SoundEvents.TADPOLE_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound()
    {
        return SoundEvents.TADPOLE_DEATH;
    }

    @Override
    public InteractionResult mobInteract(Player p_218703_, InteractionHand p_218704_)
    {
        ItemStack itemstack = p_218703_.getItemInHand(p_218704_);

        if (this.isFood(itemstack))
        {
            this.feed(p_218703_, itemstack);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        else
        {
            return Bucketable.bucketMobPickup(p_218703_, p_218704_, this).orElse(super.mobInteract(p_218703_, p_218704_));
        }
    }

    @Override
    protected void sendDebugPackets()
    {
        super.sendDebugPackets();
        DebugPackets.sendEntityBrain(this);
    }

    @Override
    public boolean fromBucket()
    {
        return true;
    }

    @Override
    public void setFromBucket(boolean p_218732_)
    {
    }

    @Override
    public void saveToBucketTag(ItemStack p_218725_)
    {
        Bucketable.saveDefaultDataToBucketTag(this, p_218725_);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, p_218725_, p_328188_ -> p_328188_.putInt("Age", this.getAge()));
    }

    @Override
    public void loadFromBucketTag(CompoundTag p_218715_)
    {
        Bucketable.loadDefaultDataFromBucketTag(this, p_218715_);

        if (p_218715_.contains("Age"))
        {
            this.setAge(p_218715_.getInt("Age"));
        }
    }

    @Override
    public ItemStack getBucketItemStack()
    {
        return new ItemStack(Items.TADPOLE_BUCKET);
    }

    @Override
    public SoundEvent getPickupSound()
    {
        return SoundEvents.BUCKET_FILL_TADPOLE;
    }

    private boolean isFood(ItemStack p_218727_)
    {
        return p_218727_.is(ItemTags.FROG_FOOD);
    }

    private void feed(Player p_218691_, ItemStack p_218692_)
    {
        this.usePlayerItem(p_218691_, p_218692_);
        this.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(this.getTicksLeftUntilAdult()));
        this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
    }

    private void usePlayerItem(Player p_218706_, ItemStack p_218707_)
    {
        p_218707_.consume(1, p_218706_);
    }

    private int getAge()
    {
        return this.age;
    }

    private void ageUp(int p_218701_)
    {
        this.setAge(this.age + p_218701_ * 20);
    }

    private void setAge(int p_218711_)
    {
        this.age = p_218711_;

        if (this.age >= ticksToBeFrog)
        {
            this.ageUp();
        }
    }

    private void ageUp()
    {
        if (this.level() instanceof ServerLevel serverlevel)
        {
            Frog frog = EntityType.FROG.create(this.level());

            if (frog != null)
            {
                frog.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
                frog.finalizeSpawn(serverlevel, this.level().getCurrentDifficultyAt(frog.blockPosition()), MobSpawnType.CONVERSION, null);
                frog.setNoAi(this.isNoAi());

                if (this.hasCustomName())
                {
                    frog.setCustomName(this.getCustomName());
                    frog.setCustomNameVisible(this.isCustomNameVisible());
                }

                frog.setPersistenceRequired();
                frog.fudgePositionAfterSizeChange(this.getDimensions(this.getPose()));
                this.playSound(SoundEvents.TADPOLE_GROW_UP, 0.15F, 1.0F);
                serverlevel.addFreshEntityWithPassengers(frog);
                this.discard();
            }
        }
    }

    private int getTicksLeftUntilAdult()
    {
        return Math.max(0, ticksToBeFrog - this.age);
    }

    @Override
    public boolean shouldDropExperience()
    {
        return false;
    }
}
