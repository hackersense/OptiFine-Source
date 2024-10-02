package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class FireChargeItem extends Item implements ProjectileItem
{
    public FireChargeItem(Item.Properties p_41202_)
    {
        super(p_41202_);
    }

    @Override
    public InteractionResult useOn(UseOnContext p_41204_)
    {
        Level level = p_41204_.getLevel();
        BlockPos blockpos = p_41204_.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        boolean flag = false;

        if (!CampfireBlock.canLight(blockstate) && !CandleBlock.canLight(blockstate) && !CandleCakeBlock.canLight(blockstate))
        {
            blockpos = blockpos.relative(p_41204_.getClickedFace());

            if (BaseFireBlock.canBePlacedAt(level, blockpos, p_41204_.getHorizontalDirection()))
            {
                this.playSound(level, blockpos);
                level.setBlockAndUpdate(blockpos, BaseFireBlock.getState(level, blockpos));
                level.gameEvent(p_41204_.getPlayer(), GameEvent.BLOCK_PLACE, blockpos);
                flag = true;
            }
        }
        else
        {
            this.playSound(level, blockpos);
            level.setBlockAndUpdate(blockpos, blockstate.setValue(BlockStateProperties.LIT, Boolean.valueOf(true)));
            level.gameEvent(p_41204_.getPlayer(), GameEvent.BLOCK_CHANGE, blockpos);
            flag = true;
        }

        if (flag)
        {
            p_41204_.getItemInHand().shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        else
        {
            return InteractionResult.FAIL;
        }
    }

    private void playSound(Level p_41206_, BlockPos p_41207_)
    {
        RandomSource randomsource = p_41206_.getRandom();
        p_41206_.playSound(null, p_41207_, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F);
    }

    @Override
    public Projectile asProjectile(Level p_333696_, Position p_332623_, ItemStack p_335300_, Direction p_332824_)
    {
        RandomSource randomsource = p_333696_.getRandom();
        double d0 = randomsource.triangle((double)p_332824_.getStepX(), 0.11485000000000001);
        double d1 = randomsource.triangle((double)p_332824_.getStepY(), 0.11485000000000001);
        double d2 = randomsource.triangle((double)p_332824_.getStepZ(), 0.11485000000000001);
        Vec3 vec3 = new Vec3(d0, d1, d2);
        SmallFireball smallfireball = new SmallFireball(p_333696_, p_332623_.x(), p_332623_.y(), p_332623_.z(), vec3.normalize());
        smallfireball.setItem(p_335300_);
        return smallfireball;
    }

    @Override
    public void shoot(Projectile p_333684_, double p_331158_, double p_330156_, double p_328098_, float p_334367_, float p_329865_)
    {
    }

    @Override
    public ProjectileItem.DispenseConfig createDispenseConfig()
    {
        return ProjectileItem.DispenseConfig.builder()
               .positionFunction((p_334997_, p_333408_) -> DispenserBlock.getDispensePosition(p_334997_, 1.0, Vec3.ZERO))
               .uncertainty(6.6666665F)
               .power(1.0F)
               .overrideDispenseEvent(1018)
               .build();
    }
}
