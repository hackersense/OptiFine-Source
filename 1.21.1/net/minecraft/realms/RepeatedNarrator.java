package net.minecraft.realms;

import com.google.common.util.concurrent.RateLimiter;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.GameNarrator;
import net.minecraft.network.chat.Component;

public class RepeatedNarrator
{
    private final float permitsPerSecond;
    private final AtomicReference<RepeatedNarrator.Params> params = new AtomicReference<>();

    public RepeatedNarrator(Duration p_120788_)
    {
        this.permitsPerSecond = 1000.0F / (float)p_120788_.toMillis();
    }

    public void narrate(GameNarrator p_240528_, Component p_240604_)
    {
        RepeatedNarrator.Params repeatednarrator$params = this.params
                .updateAndGet(
                    p_326139_ -> p_326139_ != null && p_240604_.equals(p_326139_.narration)
                    ? p_326139_
                    : new RepeatedNarrator.Params(p_240604_, RateLimiter.create((double)this.permitsPerSecond))
                );

        if (repeatednarrator$params.rateLimiter.tryAcquire(1))
        {
            p_240528_.sayNow(p_240604_);
        }
    }

    static class Params
    {
        final Component narration;
        final RateLimiter rateLimiter;

        Params(Component p_175082_, RateLimiter p_175083_)
        {
            this.narration = p_175082_;
            this.rateLimiter = p_175083_;
        }
    }
}
