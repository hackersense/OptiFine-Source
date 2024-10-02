package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.projectile.windcharge.BreezeWindCharge;
import net.minecraft.world.phys.Vec3;

public class Shoot extends Behavior<Breeze>
{
    private static final int ATTACK_RANGE_MIN_SQRT = 4;
    private static final int ATTACK_RANGE_MAX_SQRT = 256;
    private static final int UNCERTAINTY_BASE = 5;
    private static final int UNCERTAINTY_MULTIPLIER = 4;
    private static final float PROJECTILE_MOVEMENT_SCALE = 0.7F;
    private static final int SHOOT_INITIAL_DELAY_TICKS = Math.round(15.0F);
    private static final int SHOOT_RECOVER_DELAY_TICKS = Math.round(4.0F);
    private static final int SHOOT_COOLDOWN_TICKS = Math.round(10.0F);

    @VisibleForTesting
    public Shoot()
    {
        super(
            ImmutableMap.of(
                MemoryModuleType.ATTACK_TARGET,
                MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.BREEZE_SHOOT_COOLDOWN,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREEZE_SHOOT_CHARGING,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREEZE_SHOOT_RECOVERING,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREEZE_SHOOT,
                MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.WALK_TARGET,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREEZE_JUMP_TARGET,
                MemoryStatus.VALUE_ABSENT
            ),
            SHOOT_INITIAL_DELAY_TICKS + 1 + SHOOT_RECOVER_DELAY_TICKS
        );
    }

    protected boolean checkExtraStartConditions(ServerLevel p_310608_, Breeze p_310203_)
    {
        return p_310203_.getPose() != Pose.STANDING
               ? false
               : p_310203_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).map(p_311282_ -> isTargetWithinRange(p_310203_, p_311282_)).map(p_311912_ ->
        {
            if (!p_311912_)
            {
                p_310203_.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT);
            }

            return (Boolean)p_311912_;
        }).orElse(false);
    }

    protected boolean canStillUse(ServerLevel p_309829_, Breeze p_312308_, long p_310493_)
    {
        return p_312308_.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && p_312308_.getBrain().hasMemoryValue(MemoryModuleType.BREEZE_SHOOT);
    }

    protected void start(ServerLevel p_312287_, Breeze p_310847_, long p_311799_)
    {
        p_310847_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(p_312466_ -> p_310847_.setPose(Pose.SHOOTING));
        p_310847_.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_CHARGING, Unit.INSTANCE, (long)SHOOT_INITIAL_DELAY_TICKS);
        p_310847_.playSound(SoundEvents.BREEZE_INHALE, 1.0F, 1.0F);
    }

    protected void stop(ServerLevel p_312573_, Breeze p_309852_, long p_310968_)
    {
        if (p_309852_.getPose() == Pose.SHOOTING)
        {
            p_309852_.setPose(Pose.STANDING);
        }

        p_309852_.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_COOLDOWN, Unit.INSTANCE, (long)SHOOT_COOLDOWN_TICKS);
        p_309852_.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT);
    }

    protected void tick(ServerLevel p_312469_, Breeze p_309721_, long p_312577_)
    {
        Brain<Breeze> brain = p_309721_.getBrain();
        LivingEntity livingentity = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);

        if (livingentity != null)
        {
            p_309721_.lookAt(EntityAnchorArgument.Anchor.EYES, livingentity.position());

            if (!brain.getMemory(MemoryModuleType.BREEZE_SHOOT_CHARGING).isPresent() && !brain.getMemory(MemoryModuleType.BREEZE_SHOOT_RECOVERING).isPresent())
            {
                brain.setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_RECOVERING, Unit.INSTANCE, (long)SHOOT_RECOVER_DELAY_TICKS);

                if (isFacingTarget(p_309721_, livingentity))
                {
                    double d0 = livingentity.getX() - p_309721_.getX();
                    double d1 = livingentity.getY(livingentity.isPassenger() ? 0.8 : 0.3) - p_309721_.getY(0.5);
                    double d2 = livingentity.getZ() - p_309721_.getZ();
                    BreezeWindCharge breezewindcharge = new BreezeWindCharge(p_309721_, p_312469_);
                    p_309721_.playSound(SoundEvents.BREEZE_SHOOT, 1.5F, 1.0F);
                    breezewindcharge.shoot(d0, d1, d2, 0.7F, (float)(5 - p_312469_.getDifficulty().getId() * 4));
                    p_312469_.addFreshEntity(breezewindcharge);
                }
            }
        }
    }

    @VisibleForTesting
    public static boolean isFacingTarget(Breeze p_311537_, LivingEntity p_310664_)
    {
        Vec3 vec3 = p_311537_.getViewVector(1.0F);
        Vec3 vec31 = p_310664_.position().subtract(p_311537_.position()).normalize();
        return vec3.dot(vec31) > 0.5;
    }

    private static boolean isTargetWithinRange(Breeze p_311470_, LivingEntity p_309385_)
    {
        double d0 = p_311470_.position().distanceToSqr(p_309385_.position());
        return d0 > 4.0 && d0 < 256.0;
    }
}
