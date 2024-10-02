package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;

class BadOmenMobEffect extends MobEffect
{
    protected BadOmenMobEffect(MobEffectCategory p_298574_, int p_301000_)
    {
        super(p_298574_, p_301000_);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int p_297444_, int p_300866_)
    {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity p_299568_, int p_299125_)
    {
        if (p_299568_ instanceof ServerPlayer serverplayer && !serverplayer.isSpectator())
        {
            ServerLevel serverlevel = serverplayer.serverLevel();

            if (serverlevel.getDifficulty() != Difficulty.PEACEFUL && serverlevel.isVillage(serverplayer.blockPosition()))
            {
                Raid raid = serverlevel.getRaidAt(serverplayer.blockPosition());

                if (raid == null || raid.getRaidOmenLevel() < raid.getMaxRaidOmenLevel())
                {
                    serverplayer.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN, 600, p_299125_));
                    serverplayer.setRaidOmenPosition(serverplayer.blockPosition());
                    return false;
                }
            }
        }

        return true;
    }
}
