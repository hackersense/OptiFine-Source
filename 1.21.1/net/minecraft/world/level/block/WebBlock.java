package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class WebBlock extends Block
{
    public static final MapCodec<WebBlock> CODEC = simpleCodec(WebBlock::new);

    @Override
    public MapCodec<WebBlock> codec()
    {
        return CODEC;
    }

    public WebBlock(BlockBehaviour.Properties p_58178_)
    {
        super(p_58178_);
    }

    @Override
    protected void entityInside(BlockState p_58180_, Level p_58181_, BlockPos p_58182_, Entity p_58183_)
    {
        Vec3 vec3 = new Vec3(0.25, 0.05F, 0.25);

        if (p_58183_ instanceof LivingEntity livingentity && livingentity.hasEffect(MobEffects.WEAVING))
        {
            vec3 = new Vec3(0.5, 0.25, 0.5);
        }

        p_58183_.makeStuckInBlock(p_58180_, vec3);
    }
}
