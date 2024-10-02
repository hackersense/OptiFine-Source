package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;

public class WaterDropParticle extends TextureSheetParticle
{
    protected WaterDropParticle(ClientLevel p_108484_, double p_108485_, double p_108486_, double p_108487_)
    {
        super(p_108484_, p_108485_, p_108486_, p_108487_, 0.0, 0.0, 0.0);
        this.xd *= 0.3F;
        this.yd = Math.random() * 0.2F + 0.1F;
        this.zd *= 0.3F;
        this.setSize(0.01F, 0.01F);
        this.gravity = 0.06F;
        this.lifetime = (int)(8.0 / (Math.random() * 0.8 + 0.2));
    }

    @Override
    public ParticleRenderType getRenderType()
    {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void tick()
    {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.lifetime-- <= 0)
        {
            this.remove();
        }
        else
        {
            this.yd = this.yd - (double)this.gravity;
            this.move(this.xd, this.yd, this.zd);
            this.xd *= 0.98F;
            this.yd *= 0.98F;
            this.zd *= 0.98F;

            if (this.onGround)
            {
                if (Math.random() < 0.5)
                {
                    this.remove();
                }

                this.xd *= 0.7F;
                this.zd *= 0.7F;
            }

            BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
            double d0 = Math.max(
                            this.level
                            .getBlockState(blockpos)
                            .getCollisionShape(this.level, blockpos)
                            .max(Direction.Axis.Y, this.x - (double)blockpos.getX(), this.z - (double)blockpos.getZ()),
                            (double)this.level.getFluidState(blockpos).getHeight(this.level, blockpos)
                        );

            if (d0 > 0.0 && this.y < (double)blockpos.getY() + d0)
            {
                this.remove();
            }
        }
    }

    public static class Provider implements ParticleProvider<SimpleParticleType>
    {
        private final SpriteSet sprite;

        public Provider(SpriteSet p_108492_)
        {
            this.sprite = p_108492_;
        }

        public Particle createParticle(
            SimpleParticleType p_108503_,
            ClientLevel p_108504_,
            double p_108505_,
            double p_108506_,
            double p_108507_,
            double p_108508_,
            double p_108509_,
            double p_108510_
        )
        {
            WaterDropParticle waterdropparticle = new WaterDropParticle(p_108504_, p_108505_, p_108506_, p_108507_);
            waterdropparticle.pickSprite(this.sprite);
            return waterdropparticle;
        }
    }
}
