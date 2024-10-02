package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class VibrationSignalParticle extends TextureSheetParticle
{
    private final PositionSource target;
    private float rot;
    private float rotO;
    private float pitch;
    private float pitchO;

    VibrationSignalParticle(ClientLevel p_234105_, double p_234106_, double p_234107_, double p_234108_, PositionSource p_234109_, int p_234110_)
    {
        super(p_234105_, p_234106_, p_234107_, p_234108_, 0.0, 0.0, 0.0);
        this.quadSize = 0.3F;
        this.target = p_234109_;
        this.lifetime = p_234110_;
        Optional<Vec3> optional = p_234109_.getPosition(p_234105_);

        if (optional.isPresent())
        {
            Vec3 vec3 = optional.get();
            double d0 = p_234106_ - vec3.x();
            double d1 = p_234107_ - vec3.y();
            double d2 = p_234108_ - vec3.z();
            this.rotO = this.rot = (float)Mth.atan2(d0, d2);
            this.pitchO = this.pitch = (float)Mth.atan2(d1, Math.sqrt(d0 * d0 + d2 * d2));
        }
    }

    @Override
    public void render(VertexConsumer p_172475_, Camera p_172476_, float p_172477_)
    {
        float f = Mth.sin(((float)this.age + p_172477_ - (float)(Math.PI * 2)) * 0.05F) * 2.0F;
        float f1 = Mth.lerp(p_172477_, this.rotO, this.rot);
        float f2 = Mth.lerp(p_172477_, this.pitchO, this.pitch) + (float)(Math.PI / 2);
        Quaternionf quaternionf = new Quaternionf();
        quaternionf.rotationY(f1).rotateX(-f2).rotateY(f);
        this.renderRotatedQuad(p_172475_, p_172476_, quaternionf, p_172477_);
        quaternionf.rotationY((float) - Math.PI + f1).rotateX(f2).rotateY(f);
        this.renderRotatedQuad(p_172475_, p_172476_, quaternionf, p_172477_);
    }

    @Override
    public int getLightColor(float p_172469_)
    {
        return 240;
    }

    @Override
    public ParticleRenderType getRenderType()
    {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick()
    {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime)
        {
            this.remove();
        }
        else
        {
            Optional<Vec3> optional = this.target.getPosition(this.level);

            if (optional.isEmpty())
            {
                this.remove();
            }
            else
            {
                int i = this.lifetime - this.age;
                double d0 = 1.0 / (double)i;
                Vec3 vec3 = optional.get();
                this.x = Mth.lerp(d0, this.x, vec3.x());
                this.y = Mth.lerp(d0, this.y, vec3.y());
                this.z = Mth.lerp(d0, this.z, vec3.z());
                double d1 = this.x - vec3.x();
                double d2 = this.y - vec3.y();
                double d3 = this.z - vec3.z();
                this.rotO = this.rot;
                this.rot = (float)Mth.atan2(d1, d3);
                this.pitchO = this.pitch;
                this.pitch = (float)Mth.atan2(d2, Math.sqrt(d1 * d1 + d3 * d3));
            }
        }
    }

    public static class Provider implements ParticleProvider<VibrationParticleOption>
    {
        private final SpriteSet sprite;

        public Provider(SpriteSet p_172490_)
        {
            this.sprite = p_172490_;
        }

        public Particle createParticle(
            VibrationParticleOption p_172501_,
            ClientLevel p_172502_,
            double p_172503_,
            double p_172504_,
            double p_172505_,
            double p_172506_,
            double p_172507_,
            double p_172508_
        )
        {
            VibrationSignalParticle vibrationsignalparticle = new VibrationSignalParticle(
                p_172502_, p_172503_, p_172504_, p_172505_, p_172501_.getDestination(), p_172501_.getArrivalInTicks()
            );
            vibrationsignalparticle.pickSprite(this.sprite);
            vibrationsignalparticle.setAlpha(1.0F);
            return vibrationsignalparticle;
        }
    }
}
