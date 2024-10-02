package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MaceItem extends Item
{
    private static final int DEFAULT_ATTACK_DAMAGE = 5;
    private static final float DEFAULT_ATTACK_SPEED = -3.4F;
    public static final float SMASH_ATTACK_FALL_THRESHOLD = 1.5F;
    private static final float SMASH_ATTACK_HEAVY_THRESHOLD = 5.0F;
    public static final float SMASH_ATTACK_KNOCKBACK_RADIUS = 3.5F;
    private static final float SMASH_ATTACK_KNOCKBACK_POWER = 0.7F;

    public MaceItem(Item.Properties p_329217_)
    {
        super(p_329217_);
    }

    public static ItemAttributeModifiers createAttributes()
    {
        return ItemAttributeModifiers.builder()
               .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
               .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.4F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
               .build();
    }

    public static Tool createToolProperties()
    {
        return new Tool(List.of(), 1.0F, 2);
    }

    @Override
    public boolean canAttackBlock(BlockState p_330271_, Level p_332833_, BlockPos p_334020_, Player p_336375_)
    {
        return !p_336375_.isCreative();
    }

    @Override
    public int getEnchantmentValue()
    {
        return 15;
    }

    @Override
    public boolean hurtEnemy(ItemStack p_329476_, LivingEntity p_332492_, LivingEntity p_333391_)
    {
        if (p_333391_ instanceof ServerPlayer serverplayer && canSmashAttack(serverplayer))
        {
            ServerLevel serverlevel = (ServerLevel)p_333391_.level();

            if (serverplayer.isIgnoringFallDamageFromCurrentImpulse() && serverplayer.currentImpulseImpactPos != null)
            {
                if (serverplayer.currentImpulseImpactPos.y > serverplayer.position().y)
                {
                    serverplayer.currentImpulseImpactPos = serverplayer.position();
                }
            }
            else
            {
                serverplayer.currentImpulseImpactPos = serverplayer.position();
            }

            serverplayer.setIgnoreFallDamageFromCurrentImpulse(true);
            serverplayer.setDeltaMovement(serverplayer.getDeltaMovement().with(Direction.Axis.Y, 0.01F));
            serverplayer.connection.send(new ClientboundSetEntityMotionPacket(serverplayer));

            if (p_332492_.onGround())
            {
                serverplayer.setSpawnExtraParticlesOnFall(true);
                SoundEvent soundevent = serverplayer.fallDistance > 5.0F ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND;
                serverlevel.playSound(
                    null, serverplayer.getX(), serverplayer.getY(), serverplayer.getZ(), soundevent, serverplayer.getSoundSource(), 1.0F, 1.0F
                );
            }
            else
            {
                serverlevel.playSound(
                    null, serverplayer.getX(), serverplayer.getY(), serverplayer.getZ(), SoundEvents.MACE_SMASH_AIR, serverplayer.getSoundSource(), 1.0F, 1.0F
                );
            }

            knockback(serverlevel, serverplayer, p_332492_);
        }

        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack p_344750_, LivingEntity p_344000_, LivingEntity p_342605_)
    {
        p_344750_.hurtAndBreak(1, p_342605_, EquipmentSlot.MAINHAND);

        if (canSmashAttack(p_342605_))
        {
            p_342605_.resetFallDistance();
        }
    }

    @Override
    public boolean isValidRepairItem(ItemStack p_335618_, ItemStack p_332323_)
    {
        return p_332323_.is(Items.BREEZE_ROD);
    }

    @Override
    public float getAttackDamageBonus(Entity p_344513_, float p_333106_, DamageSource p_345351_)
    {
        if (p_345351_.getDirectEntity() instanceof LivingEntity livingentity)
        {
            if (!canSmashAttack(livingentity))
            {
                return 0.0F;
            }
            else
            {
                float f3 = 3.0F;
                float f = 8.0F;
                float f1 = livingentity.fallDistance;
                float f2;

                if (f1 <= 3.0F)
                {
                    f2 = 4.0F * f1;
                }
                else if (f1 <= 8.0F)
                {
                    f2 = 12.0F + 2.0F * (f1 - 3.0F);
                }
                else
                {
                    f2 = 22.0F + f1 - 8.0F;
                }

                return livingentity.level() instanceof ServerLevel serverlevel
                       ? f2 + EnchantmentHelper.modifyFallBasedDamage(serverlevel, livingentity.getWeaponItem(), p_344513_, p_345351_, 0.0F) * f1
                       : f2;
            }
        }
        else
        {
            return 0.0F;
        }
    }

    private static void knockback(Level p_332228_, Player p_335060_, Entity p_335011_)
    {
        p_332228_.levelEvent(2013, p_335011_.getOnPos(), 750);
        p_332228_.getEntitiesOfClass(LivingEntity.class, p_335011_.getBoundingBox().inflate(3.5), knockbackPredicate(p_335060_, p_335011_)).forEach(p_341573_ ->
        {
            Vec3 vec3 = p_341573_.position().subtract(p_335011_.position());
            double d0 = getKnockbackPower(p_335060_, p_341573_, vec3);
            Vec3 vec31 = vec3.normalize().scale(d0);

            if (d0 > 0.0)
            {
                p_341573_.push(vec31.x, 0.7F, vec31.z);

                if (p_341573_ instanceof ServerPlayer serverplayer)
                {
                    serverplayer.connection.send(new ClientboundSetEntityMotionPacket(serverplayer));
                }
            }
        });
    }

    private static Predicate<LivingEntity> knockbackPredicate(Player p_334836_, Entity p_334480_)
    {
        return p_341576_ ->
        {
            boolean flag;
            boolean flag1;
            boolean flag2;
            boolean flag6;
            label62: {
                flag = !p_341576_.isSpectator();
                flag1 = p_341576_ != p_334836_ && p_341576_ != p_334480_;
                flag2 = !p_334836_.isAlliedTo(p_341576_);

                if (p_341576_ instanceof TamableAnimal tamableanimal && tamableanimal.isTame() && p_334836_.getUUID().equals(tamableanimal.getOwnerUUID()))
                {
                    flag6 = true;
                    break label62;
                }

                flag6 = false;
            }

            boolean flag3;
            label55: {
                flag3 = !flag6;

                if (p_341576_ instanceof ArmorStand armorstand && armorstand.isMarker())
                {
                    flag6 = false;
                    break label55;
                }

                flag6 = true;
            }

            boolean flag4 = flag6;
            boolean flag5 = p_334480_.distanceToSqr(p_341576_) <= Math.pow(3.5, 2.0);
            return flag && flag1 && flag2 && flag3 && flag4 && flag5;
        };
    }

    private static double getKnockbackPower(Player p_328672_, LivingEntity p_334129_, Vec3 p_335583_)
    {
        return (3.5 - p_335583_.length()) * 0.7F * (double)(p_328672_.fallDistance > 5.0F ? 2 : 1) * (1.0 - p_334129_.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
    }

    public static boolean canSmashAttack(LivingEntity p_345213_)
    {
        return p_345213_.fallDistance > 1.5F && !p_345213_.isFallFlying();
    }
}
