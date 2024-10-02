package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class HeartParticle extends TextureSheetParticle
{
    HeartParticle(ClientLevel p_106847_, double p_106848_, double p_106849_, double p_106850_)
    {
        super(p_106847_, p_106848_, p_106849_, p_106850_, 0.0, 0.0, 0.0);
        this.speedUpWhenYMotionIsBlocked = true;
        this.friction = 0.86F;
        this.xd *= 0.01F;
        this.yd *= 0.01F;
        this.zd *= 0.01F;
        this.yd += 0.1;
        this.quadSize *= 1.5F;
        this.lifetime = 16;
        this.hasPhysics = false;
    }

    @Override
    public ParticleRenderType getRenderType()
    {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public float getQuadSize(float p_106860_)
    {
        return this.quadSize * Mth.clamp(((float)this.age + p_106860_) / (float)this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    public static class AngryVillagerProvider implements ParticleProvider<SimpleParticleType>
    {
        private final SpriteSet sprite;

        public AngryVillagerProvider(SpriteSet p_106863_)
        {
            this.sprite = p_106863_;
        }

        public Particle createParticle(
            SimpleParticleType p_106874_,
            ClientLevel p_106875_,
            double p_106876_,
            double p_106877_,
            double p_106878_,
            double p_106879_,
            double p_106880_,
            double p_106881_
        )
        {
            HeartParticle heartparticle = new HeartParticle(p_106875_, p_106876_, p_106877_ + 0.5, p_106878_);
            heartparticle.pickSprite(this.sprite);
            heartparticle.setColor(1.0F, 1.0F, 1.0F);
            return heartparticle;
        }
    }

    public static class Provider implements ParticleProvider<SimpleParticleType>
    {
        private final SpriteSet sprite;

        public Provider(SpriteSet p_106884_)
        {
            this.sprite = p_106884_;
        }

        public Particle createParticle(
            SimpleParticleType p_106895_,
            ClientLevel p_106896_,
            double p_106897_,
            double p_106898_,
            double p_106899_,
            double p_106900_,
            double p_106901_,
            double p_106902_
        )
        {
            HeartParticle heartparticle = new HeartParticle(p_106896_, p_106897_, p_106898_, p_106899_);
            heartparticle.pickSprite(this.sprite);
            return heartparticle;
        }
    }
}
