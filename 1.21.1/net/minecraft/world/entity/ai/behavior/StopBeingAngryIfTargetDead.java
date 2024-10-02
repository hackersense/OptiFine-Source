package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.GameRules;

public class StopBeingAngryIfTargetDead
{
    public static BehaviorControl<LivingEntity> create()
    {
        return BehaviorBuilder.create(
                   p_258814_ -> p_258814_.group(p_258814_.present(MemoryModuleType.ANGRY_AT))
                   .apply(
                       p_258814_,
                       p_258813_ -> (p_258807_, p_258808_, p_258809_) ->
        {
            Optional.ofNullable(p_258807_.getEntity(p_258814_.get(p_258813_)))
            .map(p_258802_ -> p_258802_ instanceof LivingEntity livingentity ? livingentity : null)
            .filter(LivingEntity::isDeadOrDying)
            .filter(p_341371_ -> p_341371_.getType() != EntityType.PLAYER || p_258807_.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS))
            .ifPresent(p_258811_ -> p_258813_.erase());
            return true;
        }
                   )
               );
    }
}
