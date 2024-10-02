package net.minecraft.world.effect;

import net.minecraft.world.entity.LivingEntity;

class PoisonMobEffect extends MobEffect
{
    protected PoisonMobEffect(MobEffectCategory p_301256_, int p_299285_)
    {
        super(p_301256_, p_299285_);
    }

    @Override
    public boolean applyEffectTick(LivingEntity p_299064_, int p_299074_)
    {
        if (p_299064_.getHealth() > 1.0F)
        {
            p_299064_.hurt(p_299064_.damageSources().magic(), 1.0F);
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int p_297494_, int p_301016_)
    {
        int i = 25 >> p_301016_;
        return i > 0 ? p_297494_ % i == 0 : true;
    }
}
