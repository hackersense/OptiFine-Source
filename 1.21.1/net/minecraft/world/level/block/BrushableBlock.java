package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class BrushableBlock extends BaseEntityBlock implements Fallable
{
    public static final MapCodec<BrushableBlock> CODEC = RecordCodecBuilder.mapCodec(
                p_341822_ -> p_341822_.group(
                    BuiltInRegistries.BLOCK.byNameCodec().fieldOf("turns_into").forGetter(BrushableBlock::getTurnsInto),
                    BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("brush_sound").forGetter(BrushableBlock::getBrushSound),
                    BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("brush_comleted_sound").forGetter(BrushableBlock::getBrushCompletedSound),
                    propertiesCodec()
                )
                .apply(p_341822_, BrushableBlock::new)
            );
    private static final IntegerProperty DUSTED = BlockStateProperties.DUSTED;
    public static final int TICK_DELAY = 2;
    private final Block turnsInto;
    private final SoundEvent brushSound;
    private final SoundEvent brushCompletedSound;

    @Override
    public MapCodec<BrushableBlock> codec()
    {
        return CODEC;
    }

    public BrushableBlock(Block p_277629_, SoundEvent p_278060_, SoundEvent p_277352_, BlockBehaviour.Properties p_277373_)
    {
        super(p_277373_);
        this.turnsInto = p_277629_;
        this.brushSound = p_278060_;
        this.brushCompletedSound = p_277352_;
        this.registerDefaultState(this.stateDefinition.any().setValue(DUSTED, Integer.valueOf(0)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_277623_)
    {
        p_277623_.add(DUSTED);
    }

    @Override
    public RenderShape getRenderShape(BlockState p_277553_)
    {
        return RenderShape.MODEL;
    }

    @Override
    public void onPlace(BlockState p_277817_, Level p_277984_, BlockPos p_277869_, BlockState p_277926_, boolean p_277736_)
    {
        p_277984_.scheduleTick(p_277869_, this, 2);
    }

    @Override
    public BlockState updateShape(BlockState p_277801_, Direction p_277455_, BlockState p_277832_, LevelAccessor p_277473_, BlockPos p_278111_, BlockPos p_277904_)
    {
        p_277473_.scheduleTick(p_278111_, this, 2);
        return super.updateShape(p_277801_, p_277455_, p_277832_, p_277473_, p_278111_, p_277904_);
    }

    @Override
    public void tick(BlockState p_277544_, ServerLevel p_277779_, BlockPos p_278019_, RandomSource p_277471_)
    {
        if (p_277779_.getBlockEntity(p_278019_) instanceof BrushableBlockEntity brushableblockentity)
        {
            brushableblockentity.checkReset();
        }

        if (FallingBlock.isFree(p_277779_.getBlockState(p_278019_.below())) && p_278019_.getY() >= p_277779_.getMinBuildHeight())
        {
            FallingBlockEntity fallingblockentity = FallingBlockEntity.fall(p_277779_, p_278019_, p_277544_);
            fallingblockentity.disableDrop();
        }
    }

    @Override
    public void onBrokenAfterFall(Level p_278097_, BlockPos p_277734_, FallingBlockEntity p_277539_)
    {
        Vec3 vec3 = p_277539_.getBoundingBox().getCenter();
        p_278097_.levelEvent(2001, BlockPos.containing(vec3), Block.getId(p_277539_.getBlockState()));
        p_278097_.gameEvent(p_277539_, GameEvent.BLOCK_DESTROY, vec3);
    }

    @Override
    public void animateTick(BlockState p_277390_, Level p_277525_, BlockPos p_278107_, RandomSource p_277574_)
    {
        if (p_277574_.nextInt(16) == 0)
        {
            BlockPos blockpos = p_278107_.below();

            if (FallingBlock.isFree(p_277525_.getBlockState(blockpos)))
            {
                double d0 = (double)p_278107_.getX() + p_277574_.nextDouble();
                double d1 = (double)p_278107_.getY() - 0.05;
                double d2 = (double)p_278107_.getZ() + p_277574_.nextDouble();
                p_277525_.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, p_277390_), d0, d1, d2, 0.0, 0.0, 0.0);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_277683_, BlockState p_277381_)
    {
        return new BrushableBlockEntity(p_277683_, p_277381_);
    }

    public Block getTurnsInto()
    {
        return this.turnsInto;
    }

    public SoundEvent getBrushSound()
    {
        return this.brushSound;
    }

    public SoundEvent getBrushCompletedSound()
    {
        return this.brushCompletedSound;
    }
}
