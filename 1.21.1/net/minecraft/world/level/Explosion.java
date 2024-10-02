package net.minecraft.world.level;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Explosion
{
    private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new ExplosionDamageCalculator();
    private static final int MAX_DROPS_PER_COMBINED_STACK = 16;
    private final boolean fire;
    private final Explosion.BlockInteraction blockInteraction;
    private final RandomSource random = RandomSource.create();
    private final Level level;
    private final double x;
    private final double y;
    private final double z;
    @Nullable
    private final Entity source;
    private final float radius;
    private final DamageSource damageSource;
    private final ExplosionDamageCalculator damageCalculator;
    private final ParticleOptions smallExplosionParticles;
    private final ParticleOptions largeExplosionParticles;
    private final Holder<SoundEvent> explosionSound;
    private final ObjectArrayList<BlockPos> toBlow = new ObjectArrayList<>();
    private final Map<Player, Vec3> hitPlayers = Maps.newHashMap();

    public static DamageSource getDefaultDamageSource(Level p_309890_, @Nullable Entity p_311046_)
    {
        return p_309890_.damageSources().explosion(p_311046_, getIndirectSourceEntityInternal(p_311046_));
    }

    public Explosion(
        Level p_46024_,
        @Nullable Entity p_46025_,
        double p_46026_,
        double p_46027_,
        double p_46028_,
        float p_46029_,
        List<BlockPos> p_46030_,
        Explosion.BlockInteraction p_312129_,
        ParticleOptions p_311112_,
        ParticleOptions p_311120_,
        Holder<SoundEvent> p_333616_
    )
    {
        this(p_46024_, p_46025_, getDefaultDamageSource(p_46024_, p_46025_), null, p_46026_, p_46027_, p_46028_, p_46029_, false, p_312129_, p_311112_, p_311120_, p_333616_);
        this.toBlow.addAll(p_46030_);
    }

    public Explosion(
        Level p_46041_,
        @Nullable Entity p_46042_,
        double p_46043_,
        double p_46044_,
        double p_46045_,
        float p_46046_,
        boolean p_46047_,
        Explosion.BlockInteraction p_46048_,
        List<BlockPos> p_46049_
    )
    {
        this(p_46041_, p_46042_, p_46043_, p_46044_, p_46045_, p_46046_, p_46047_, p_46048_);
        this.toBlow.addAll(p_46049_);
    }

    public Explosion(
        Level p_46032_,
        @Nullable Entity p_46033_,
        double p_46034_,
        double p_46035_,
        double p_46036_,
        float p_46037_,
        boolean p_46038_,
        Explosion.BlockInteraction p_46039_
    )
    {
        this(
            p_46032_,
            p_46033_,
            getDefaultDamageSource(p_46032_, p_46033_),
            null,
            p_46034_,
            p_46035_,
            p_46036_,
            p_46037_,
            p_46038_,
            p_46039_,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            SoundEvents.GENERIC_EXPLODE
        );
    }

    public Explosion(
        Level p_46051_,
        @Nullable Entity p_46052_,
        @Nullable DamageSource p_46053_,
        @Nullable ExplosionDamageCalculator p_46054_,
        double p_46055_,
        double p_46056_,
        double p_46057_,
        float p_46058_,
        boolean p_46059_,
        Explosion.BlockInteraction p_46060_,
        ParticleOptions p_312175_,
        ParticleOptions p_310459_,
        Holder<SoundEvent> p_328940_
    )
    {
        this.level = p_46051_;
        this.source = p_46052_;
        this.radius = p_46058_;
        this.x = p_46055_;
        this.y = p_46056_;
        this.z = p_46057_;
        this.fire = p_46059_;
        this.blockInteraction = p_46060_;
        this.damageSource = p_46053_ == null ? p_46051_.damageSources().explosion(this) : p_46053_;
        this.damageCalculator = p_46054_ == null ? this.makeDamageCalculator(p_46052_) : p_46054_;
        this.smallExplosionParticles = p_312175_;
        this.largeExplosionParticles = p_310459_;
        this.explosionSound = p_328940_;
    }

    private ExplosionDamageCalculator makeDamageCalculator(@Nullable Entity p_46063_)
    {
        return (ExplosionDamageCalculator)(p_46063_ == null ? EXPLOSION_DAMAGE_CALCULATOR : new EntityBasedExplosionDamageCalculator(p_46063_));
    }

    public static float getSeenPercent(Vec3 p_46065_, Entity p_46066_)
    {
        AABB aabb = p_46066_.getBoundingBox();
        double d0 = 1.0 / ((aabb.maxX - aabb.minX) * 2.0 + 1.0);
        double d1 = 1.0 / ((aabb.maxY - aabb.minY) * 2.0 + 1.0);
        double d2 = 1.0 / ((aabb.maxZ - aabb.minZ) * 2.0 + 1.0);
        double d3 = (1.0 - Math.floor(1.0 / d0) * d0) / 2.0;
        double d4 = (1.0 - Math.floor(1.0 / d2) * d2) / 2.0;

        if (!(d0 < 0.0) && !(d1 < 0.0) && !(d2 < 0.0))
        {
            int i = 0;
            int j = 0;

            for (double d5 = 0.0; d5 <= 1.0; d5 += d0)
            {
                for (double d6 = 0.0; d6 <= 1.0; d6 += d1)
                {
                    for (double d7 = 0.0; d7 <= 1.0; d7 += d2)
                    {
                        double d8 = Mth.lerp(d5, aabb.minX, aabb.maxX);
                        double d9 = Mth.lerp(d6, aabb.minY, aabb.maxY);
                        double d10 = Mth.lerp(d7, aabb.minZ, aabb.maxZ);
                        Vec3 vec3 = new Vec3(d8 + d3, d9, d10 + d4);

                        if (p_46066_.level()
                                .clip(new ClipContext(vec3, p_46065_, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p_46066_))
                                .getType()
                                == HitResult.Type.MISS)
                        {
                            i++;
                        }

                        j++;
                    }
                }
            }

            return (float)i / (float)j;
        }
        else
        {
            return 0.0F;
        }
    }

    public float radius()
    {
        return this.radius;
    }

    public Vec3 center()
    {
        return new Vec3(this.x, this.y, this.z);
    }

    public void explode()
    {
        this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
        Set<BlockPos> set = Sets.newHashSet();
        int i = 16;

        for (int j = 0; j < 16; j++)
        {
            for (int k = 0; k < 16; k++)
            {
                for (int l = 0; l < 16; l++)
                {
                    if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15)
                    {
                        double d0 = (double)((float)j / 15.0F * 2.0F - 1.0F);
                        double d1 = (double)((float)k / 15.0F * 2.0F - 1.0F);
                        double d2 = (double)((float)l / 15.0F * 2.0F - 1.0F);
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                        d0 /= d3;
                        d1 /= d3;
                        d2 /= d3;
                        float f = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
                        double d4 = this.x;
                        double d6 = this.y;
                        double d8 = this.z;

                        for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F)
                        {
                            BlockPos blockpos = BlockPos.containing(d4, d6, d8);
                            BlockState blockstate = this.level.getBlockState(blockpos);
                            FluidState fluidstate = this.level.getFluidState(blockpos);

                            if (!this.level.isInWorldBounds(blockpos))
                            {
                                break;
                            }

                            Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance(this, this.level, blockpos, blockstate, fluidstate);

                            if (optional.isPresent())
                            {
                                f -= (optional.get() + 0.3F) * 0.3F;
                            }

                            if (f > 0.0F && this.damageCalculator.shouldBlockExplode(this, this.level, blockpos, blockstate, f))
                            {
                                set.add(blockpos);
                            }

                            d4 += d0 * 0.3F;
                            d6 += d1 * 0.3F;
                            d8 += d2 * 0.3F;
                        }
                    }
                }
            }
        }

        this.toBlow.addAll(set);
        float f2 = this.radius * 2.0F;
        int k1 = Mth.floor(this.x - (double)f2 - 1.0);
        int l1 = Mth.floor(this.x + (double)f2 + 1.0);
        int i2 = Mth.floor(this.y - (double)f2 - 1.0);
        int i1 = Mth.floor(this.y + (double)f2 + 1.0);
        int j2 = Mth.floor(this.z - (double)f2 - 1.0);
        int j1 = Mth.floor(this.z + (double)f2 + 1.0);
        List<Entity> list = this.level.getEntities(this.source, new AABB((double)k1, (double)i2, (double)j2, (double)l1, (double)i1, (double)j1));
        Vec3 vec3 = new Vec3(this.x, this.y, this.z);

        for (Entity entity : list)
        {
            if (!entity.ignoreExplosion(this))
            {
                double d11 = Math.sqrt(entity.distanceToSqr(vec3)) / (double)f2;

                if (d11 <= 1.0)
                {
                    double d5 = entity.getX() - this.x;
                    double d7 = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - this.y;
                    double d9 = entity.getZ() - this.z;
                    double d12 = Math.sqrt(d5 * d5 + d7 * d7 + d9 * d9);

                    if (d12 != 0.0)
                    {
                        d5 /= d12;
                        d7 /= d12;
                        d9 /= d12;

                        if (this.damageCalculator.shouldDamageEntity(this, entity))
                        {
                            entity.hurt(this.damageSource, this.damageCalculator.getEntityDamageAmount(this, entity));
                        }

                        double d13 = (1.0 - d11) * (double)getSeenPercent(vec3, entity) * (double)this.damageCalculator.getKnockbackMultiplier(entity);
                        double d10;

                        if (entity instanceof LivingEntity livingentity)
                        {
                            d10 = d13 * (1.0 - livingentity.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE));
                        }
                        else
                        {
                            d10 = d13;
                        }

                        d5 *= d10;
                        d7 *= d10;
                        d9 *= d10;
                        Vec3 vec31 = new Vec3(d5, d7, d9);
                        entity.setDeltaMovement(entity.getDeltaMovement().add(vec31));

                        if (entity instanceof Player)
                        {
                            Player player = (Player)entity;

                            if (!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying))
                            {
                                this.hitPlayers.put(player, vec31);
                            }
                        }

                        entity.onExplosionHit(this.source);
                    }
                }
            }
        }
    }

    public void finalizeExplosion(boolean p_46076_)
    {
        if (this.level.isClientSide)
        {
            this.level
            .playLocalSound(
                this.x,
                this.y,
                this.z,
                this.explosionSound.value(),
                SoundSource.BLOCKS,
                4.0F,
                (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F,
                false
            );
        }

        boolean flag = this.interactsWithBlocks();

        if (p_46076_)
        {
            ParticleOptions particleoptions;

            if (!(this.radius < 2.0F) && flag)
            {
                particleoptions = this.largeExplosionParticles;
            }
            else
            {
                particleoptions = this.smallExplosionParticles;
            }

            this.level.addParticle(particleoptions, this.x, this.y, this.z, 1.0, 0.0, 0.0);
        }

        if (flag)
        {
            this.level.getProfiler().push("explosion_blocks");
            List<Pair<ItemStack, BlockPos>> list = new ArrayList<>();
            Util.shuffle(this.toBlow, this.level.random);

            for (BlockPos blockpos : this.toBlow)
            {
                this.level.getBlockState(blockpos).onExplosionHit(this.level, blockpos, this, (p_309264_, p_309265_) -> addOrAppendStack(list, p_309264_, p_309265_));
            }

            for (Pair<ItemStack, BlockPos> pair : list)
            {
                Block.popResource(this.level, pair.getSecond(), pair.getFirst());
            }

            this.level.getProfiler().pop();
        }

        if (this.fire)
        {
            for (BlockPos blockpos1 : this.toBlow)
            {
                if (this.random.nextInt(3) == 0
                        && this.level.getBlockState(blockpos1).isAir()
                        && this.level.getBlockState(blockpos1.below()).isSolidRender(this.level, blockpos1.below()))
                {
                    this.level.setBlockAndUpdate(blockpos1, BaseFireBlock.getState(this.level, blockpos1));
                }
            }
        }
    }

    private static void addOrAppendStack(List<Pair<ItemStack, BlockPos>> p_311090_, ItemStack p_311817_, BlockPos p_309821_)
    {
        for (int i = 0; i < p_311090_.size(); i++)
        {
            Pair<ItemStack, BlockPos> pair = p_311090_.get(i);
            ItemStack itemstack = pair.getFirst();

            if (ItemEntity.areMergable(itemstack, p_311817_))
            {
                p_311090_.set(i, Pair.of(ItemEntity.merge(itemstack, p_311817_, 16), pair.getSecond()));

                if (p_311817_.isEmpty())
                {
                    return;
                }
            }
        }

        p_311090_.add(Pair.of(p_311817_, p_309821_));
    }

    public boolean interactsWithBlocks()
    {
        return this.blockInteraction != Explosion.BlockInteraction.KEEP;
    }

    public Map<Player, Vec3> getHitPlayers()
    {
        return this.hitPlayers;
    }

    @Nullable
    private static LivingEntity getIndirectSourceEntityInternal(@Nullable Entity p_309719_)
    {
        if (p_309719_ == null)
        {
            return null;
        }
        else if (p_309719_ instanceof PrimedTnt primedtnt)
        {
            return primedtnt.getOwner();
        }
        else if (p_309719_ instanceof LivingEntity)
        {
            return (LivingEntity)p_309719_;
        }
        else
        {
            if (p_309719_ instanceof Projectile projectile)
            {
                Entity entity = projectile.getOwner();

                if (entity instanceof LivingEntity)
                {
                    return (LivingEntity)entity;
                }
            }

            return null;
        }
    }

    @Nullable
    public LivingEntity getIndirectSourceEntity()
    {
        return getIndirectSourceEntityInternal(this.source);
    }

    @Nullable
    public Entity getDirectSourceEntity()
    {
        return this.source;
    }

    public void clearToBlow()
    {
        this.toBlow.clear();
    }

    public List<BlockPos> getToBlow()
    {
        return this.toBlow;
    }

    public Explosion.BlockInteraction getBlockInteraction()
    {
        return this.blockInteraction;
    }

    public ParticleOptions getSmallExplosionParticles()
    {
        return this.smallExplosionParticles;
    }

    public ParticleOptions getLargeExplosionParticles()
    {
        return this.largeExplosionParticles;
    }

    public Holder<SoundEvent> getExplosionSound()
    {
        return this.explosionSound;
    }

    public boolean canTriggerBlocks()
    {
        if (this.blockInteraction == Explosion.BlockInteraction.TRIGGER_BLOCK && !this.level.isClientSide())
        {
            return this.source != null && this.source.getType() == EntityType.BREEZE_WIND_CHARGE ? this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) : true;
        }
        else
        {
            return false;
        }
    }

    public static enum BlockInteraction
    {
        KEEP,
        DESTROY,
        DESTROY_WITH_DECAY,
        TRIGGER_BLOCK;
    }
}
