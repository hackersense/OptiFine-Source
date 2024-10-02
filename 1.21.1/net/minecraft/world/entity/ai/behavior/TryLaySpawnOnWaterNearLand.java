package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;

public class TryLaySpawnOnWaterNearLand
{
    public static BehaviorControl<LivingEntity> create(Block p_259207_)
    {
        return BehaviorBuilder.create(
                   p_259781_ -> p_259781_.group(
                       p_259781_.absent(MemoryModuleType.ATTACK_TARGET),
                       p_259781_.present(MemoryModuleType.WALK_TARGET),
                       p_259781_.present(MemoryModuleType.IS_PREGNANT)
                   )
                   .apply(
                       p_259781_,
                       (p_259765_, p_259602_, p_260037_) -> (p_269881_, p_269882_, p_269883_) ->
        {
            if (!p_269882_.isInWater() && p_269882_.onGround())
            {
                BlockPos blockpos = p_269882_.blockPosition().below();

                for (Direction direction : Direction.Plane.HORIZONTAL)
                {
                    BlockPos blockpos1 = blockpos.relative(direction);

                    if (p_269881_.getBlockState(blockpos1).getCollisionShape(p_269881_, blockpos1).getFaceShape(Direction.UP).isEmpty()
                    && p_269881_.getFluidState(blockpos1).is(Fluids.WATER))
                    {
                        BlockPos blockpos2 = blockpos1.above();

                        if (p_269881_.getBlockState(blockpos2).isAir())
                        {
                            BlockState blockstate = p_259207_.defaultBlockState();
                            p_269881_.setBlock(blockpos2, blockstate, 3);
                            p_269881_.gameEvent(GameEvent.BLOCK_PLACE, blockpos2, GameEvent.Context.of(p_269882_, blockstate));
                            p_269881_.playSound(null, p_269882_, SoundEvents.FROG_LAY_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
                            p_260037_.erase();
                            return true;
                        }
                    }
                }

                return true;
            }
            else {
                return false;
            }
        }
                   )
               );
    }
}
