package net.minecraft.world;

import java.util.List;
import net.minecraft.util.TimeUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class TickRateManager
{
    public static final float MIN_TICKRATE = 1.0F;
    protected float tickrate = 20.0F;
    protected long nanosecondsPerTick = TimeUtil.NANOSECONDS_PER_SECOND / 20L;
    protected int frozenTicksToRun = 0;
    protected boolean runGameElements = true;
    protected boolean isFrozen = false;

    public void setTickRate(float p_312754_)
    {
        this.tickrate = Math.max(p_312754_, 1.0F);
        this.nanosecondsPerTick = (long)((double)TimeUtil.NANOSECONDS_PER_SECOND / (double)this.tickrate);
    }

    public float tickrate()
    {
        return this.tickrate;
    }

    public float millisecondsPerTick()
    {
        return (float)this.nanosecondsPerTick / (float)TimeUtil.NANOSECONDS_PER_MILLISECOND;
    }

    public long nanosecondsPerTick()
    {
        return this.nanosecondsPerTick;
    }

    public boolean runsNormally()
    {
        return this.runGameElements;
    }

    public boolean isSteppingForward()
    {
        return this.frozenTicksToRun > 0;
    }

    public void setFrozenTicksToRun(int p_312047_)
    {
        this.frozenTicksToRun = p_312047_;
    }

    public int frozenTicksToRun()
    {
        return this.frozenTicksToRun;
    }

    public void setFrozen(boolean p_312988_)
    {
        this.isFrozen = p_312988_;
    }

    public boolean isFrozen()
    {
        return this.isFrozen;
    }

    public void tick()
    {
        this.runGameElements = !this.isFrozen || this.frozenTicksToRun > 0;

        if (this.frozenTicksToRun > 0)
        {
            this.frozenTicksToRun--;
        }
    }

    public boolean isEntityFrozen(Entity p_311574_)
    {
        return !this.runsNormally() && !(p_311574_ instanceof Player) && !this.hasPlayerPassengers(p_311574_);
    }

    private boolean hasPlayerPassengers(Entity entity)
    {
        List<Entity> list = entity.getPassengers();

        for (int i = 0; i < list.size(); i++)
        {
            Entity entityx = list.get(i);

            if (entityx instanceof Player)
            {
                return true;
            }

            if (this.hasPlayerPassengers(entityx))
            {
                return true;
            }
        }

        return false;
    }
}
