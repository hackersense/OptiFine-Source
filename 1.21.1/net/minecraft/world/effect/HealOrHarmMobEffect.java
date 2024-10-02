package net.minecraft.world.effect;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

class HealOrHarmMobEffect extends InstantenousMobEffect
{
    private final boolean isHarm;

    public HealOrHarmMobEffect(MobEffectCategory p_299212_, int p_300917_, boolean p_300221_)
    {
        super(p_299212_, p_300917_);
        this.isHarm = p_300221_;
    }

    @Override
    public boolean applyEffectTick(LivingEntity p_300845_, int p_301393_)
    {
        if (this.isHarm == p_300845_.isInvertedHealAndHarm())
        {
            p_300845_.heal((float)Math.max(4 << p_301393_, 0));
        }
        else
        {
            p_300845_.hurt(p_300845_.damageSources().magic(), (float)(6 << p_301393_));
        }

        return true;
    }

    @Override
    public void applyInstantenousEffect(@Nullable Entity p_298495_, @Nullable Entity p_298887_, LivingEntity p_298479_, int p_298172_, double p_298163_)
    {
        if (this.isHarm == p_298479_.isInvertedHealAndHarm())
        {
            int i = (int)(p_298163_ * (double)(4 << p_298172_) + 0.5);
            p_298479_.heal((float)i);
        }
        else
        {
            int j = (int)(p_298163_ * (double)(6 << p_298172_) + 0.5);

            if (p_298495_ == null)
            {
                p_298479_.hurt(p_298479_.damageSources().magic(), (float)j);
            }
            else
            {
                p_298479_.hurt(p_298479_.damageSources().indirectMagic(p_298495_, p_298887_), (float)j);
            }
        }
    }
}
