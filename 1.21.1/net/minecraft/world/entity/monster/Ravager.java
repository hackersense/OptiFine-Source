package net.minecraft.world.entity.monster;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Ravager extends Raider
{
    private static final Predicate<Entity> NO_RAVAGER_AND_ALIVE = p_33346_ -> p_33346_.isAlive() && !(p_33346_ instanceof Ravager);
    private static final double BASE_MOVEMENT_SPEED = 0.3;
    private static final double ATTACK_MOVEMENT_SPEED = 0.35;
    private static final int STUNNED_COLOR = 8356754;
    private static final float STUNNED_COLOR_BLUE = 0.57254905F;
    private static final float STUNNED_COLOR_GREEN = 0.5137255F;
    private static final float STUNNED_COLOR_RED = 0.49803922F;
    private static final int ATTACK_DURATION = 10;
    public static final int STUN_DURATION = 40;
    private int attackTick;
    private int stunnedTick;
    private int roarTick;

    public Ravager(EntityType <? extends Ravager > p_33325_, Level p_33326_)
    {
        super(p_33325_, p_33326_);
        this.xpReward = 20;
        this.setPathfindingMalus(PathType.LEAVES, 0.0F);
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.4));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, Raider.class).setAlertOthers());
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true, p_199899_ -> !p_199899_.isBaby()));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    protected void updateControlFlags()
    {
        boolean flag = !(this.getControllingPassenger() instanceof Mob) || this.getControllingPassenger().getType().is(EntityTypeTags.RAIDERS);
        boolean flag1 = !(this.getVehicle() instanceof Boat);
        this.goalSelector.setControlFlag(Goal.Flag.MOVE, flag);
        this.goalSelector.setControlFlag(Goal.Flag.JUMP, flag && flag1);
        this.goalSelector.setControlFlag(Goal.Flag.LOOK, flag);
        this.goalSelector.setControlFlag(Goal.Flag.TARGET, flag);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Monster.createMonsterAttributes()
               .add(Attributes.MAX_HEALTH, 100.0)
               .add(Attributes.MOVEMENT_SPEED, 0.3)
               .add(Attributes.KNOCKBACK_RESISTANCE, 0.75)
               .add(Attributes.ATTACK_DAMAGE, 12.0)
               .add(Attributes.ATTACK_KNOCKBACK, 1.5)
               .add(Attributes.FOLLOW_RANGE, 32.0)
               .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag p_33353_)
    {
        super.addAdditionalSaveData(p_33353_);
        p_33353_.putInt("AttackTick", this.attackTick);
        p_33353_.putInt("StunTick", this.stunnedTick);
        p_33353_.putInt("RoarTick", this.roarTick);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag p_33344_)
    {
        super.readAdditionalSaveData(p_33344_);
        this.attackTick = p_33344_.getInt("AttackTick");
        this.stunnedTick = p_33344_.getInt("StunTick");
        this.roarTick = p_33344_.getInt("RoarTick");
    }

    @Override
    public SoundEvent getCelebrateSound()
    {
        return SoundEvents.RAVAGER_CELEBRATE;
    }

    @Override
    public int getMaxHeadYRot()
    {
        return 45;
    }

    @Override
    public void aiStep()
    {
        super.aiStep();

        if (this.isAlive())
        {
            if (this.isImmobile())
            {
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
            }
            else
            {
                double d0 = this.getTarget() != null ? 0.35 : 0.3;
                double d1 = this.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Mth.lerp(0.1, d1, d0));
            }

            if (this.horizontalCollision && this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))
            {
                boolean flag = false;
                AABB aabb = this.getBoundingBox().inflate(0.2);

                for (BlockPos blockpos : BlockPos.betweenClosed(
                            Mth.floor(aabb.minX),
                            Mth.floor(aabb.minY),
                            Mth.floor(aabb.minZ),
                            Mth.floor(aabb.maxX),
                            Mth.floor(aabb.maxY),
                            Mth.floor(aabb.maxZ)
                        ))
                {
                    BlockState blockstate = this.level().getBlockState(blockpos);
                    Block block = blockstate.getBlock();

                    if (block instanceof LeavesBlock)
                    {
                        flag = this.level().destroyBlock(blockpos, true, this) || flag;
                    }
                }

                if (!flag && this.onGround())
                {
                    this.jumpFromGround();
                }
            }

            if (this.roarTick > 0)
            {
                this.roarTick--;

                if (this.roarTick == 10)
                {
                    this.roar();
                }
            }

            if (this.attackTick > 0)
            {
                this.attackTick--;
            }

            if (this.stunnedTick > 0)
            {
                this.stunnedTick--;
                this.stunEffect();

                if (this.stunnedTick == 0)
                {
                    this.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 1.0F);
                    this.roarTick = 20;
                }
            }
        }
    }

    private void stunEffect()
    {
        if (this.random.nextInt(6) == 0)
        {
            double d0 = this.getX()
                        - (double)this.getBbWidth() * Math.sin((double)(this.yBodyRot * (float)(Math.PI / 180.0)))
                        + (this.random.nextDouble() * 0.6 - 0.3);
            double d1 = this.getY() + (double)this.getBbHeight() - 0.3;
            double d2 = this.getZ()
                        + (double)this.getBbWidth() * Math.cos((double)(this.yBodyRot * (float)(Math.PI / 180.0)))
                        + (this.random.nextDouble() * 0.6 - 0.3);
            this.level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.49803922F, 0.5137255F, 0.57254905F), d0, d1, d2, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected boolean isImmobile()
    {
        return super.isImmobile() || this.attackTick > 0 || this.stunnedTick > 0 || this.roarTick > 0;
    }

    @Override
    public boolean hasLineOfSight(Entity p_149755_)
    {
        return this.stunnedTick <= 0 && this.roarTick <= 0 ? super.hasLineOfSight(p_149755_) : false;
    }

    @Override
    protected void blockedByShield(LivingEntity p_33361_)
    {
        if (this.roarTick == 0)
        {
            if (this.random.nextDouble() < 0.5)
            {
                this.stunnedTick = 40;
                this.playSound(SoundEvents.RAVAGER_STUNNED, 1.0F, 1.0F);
                this.level().broadcastEntityEvent(this, (byte)39);
                p_33361_.push(this);
            }
            else
            {
                this.strongKnockback(p_33361_);
            }

            p_33361_.hurtMarked = true;
        }
    }

    private void roar()
    {
        if (this.isAlive())
        {
            for (LivingEntity livingentity : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0), NO_RAVAGER_AND_ALIVE))
            {
                if (!(livingentity instanceof AbstractIllager))
                {
                    livingentity.hurt(this.damageSources().mobAttack(this), 6.0F);
                }

                this.strongKnockback(livingentity);
            }

            Vec3 vec3 = this.getBoundingBox().getCenter();

            for (int i = 0; i < 40; i++)
            {
                double d0 = this.random.nextGaussian() * 0.2;
                double d1 = this.random.nextGaussian() * 0.2;
                double d2 = this.random.nextGaussian() * 0.2;
                this.level().addParticle(ParticleTypes.POOF, vec3.x, vec3.y, vec3.z, d0, d1, d2);
            }

            this.gameEvent(GameEvent.ENTITY_ACTION);
        }
    }

    private void strongKnockback(Entity p_33340_)
    {
        double d0 = p_33340_.getX() - this.getX();
        double d1 = p_33340_.getZ() - this.getZ();
        double d2 = Math.max(d0 * d0 + d1 * d1, 0.001);
        p_33340_.push(d0 / d2 * 4.0, 0.2, d1 / d2 * 4.0);
    }

    @Override
    public void handleEntityEvent(byte p_33335_)
    {
        if (p_33335_ == 4)
        {
            this.attackTick = 10;
            this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 1.0F);
        }
        else if (p_33335_ == 39)
        {
            this.stunnedTick = 40;
        }

        super.handleEntityEvent(p_33335_);
    }

    public int getAttackTick()
    {
        return this.attackTick;
    }

    public int getStunnedTick()
    {
        return this.stunnedTick;
    }

    public int getRoarTick()
    {
        return this.roarTick;
    }

    @Override
    public boolean doHurtTarget(Entity p_33328_)
    {
        this.attackTick = 10;
        this.level().broadcastEntityEvent(this, (byte)4);
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 1.0F);
        return super.doHurtTarget(p_33328_);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource p_33359_)
    {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound()
    {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos p_33350_, BlockState p_33351_)
    {
        this.playSound(SoundEvents.RAVAGER_STEP, 0.15F, 1.0F);
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader p_33342_)
    {
        return !p_33342_.containsAnyLiquid(this.getBoundingBox());
    }

    @Override
    public void applyRaidBuffs(ServerLevel p_342846_, int p_33337_, boolean p_33338_)
    {
    }

    @Override
    public boolean canBeLeader()
    {
        return false;
    }

    @Override
    protected AABB getAttackBoundingBox()
    {
        AABB aabb = super.getAttackBoundingBox();
        return aabb.deflate(0.05, 0.0, 0.05);
    }
}
