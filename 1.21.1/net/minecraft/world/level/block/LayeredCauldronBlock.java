package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class LayeredCauldronBlock extends AbstractCauldronBlock
{
    public static final MapCodec<LayeredCauldronBlock> CODEC = RecordCodecBuilder.mapCodec(
                p_309287_ -> p_309287_.group(
                    Biome.Precipitation.CODEC.fieldOf("precipitation").forGetter(p_309289_ -> p_309289_.precipitationType),
                    CauldronInteraction.CODEC.fieldOf("interactions").forGetter(p_309288_ -> p_309288_.interactions),
                    propertiesCodec()
                )
                .apply(p_309287_, LayeredCauldronBlock::new)
            );
    public static final int MIN_FILL_LEVEL = 1;
    public static final int MAX_FILL_LEVEL = 3;
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_CAULDRON;
    private static final int BASE_CONTENT_HEIGHT = 6;
    private static final double HEIGHT_PER_LEVEL = 3.0;
    private final Biome.Precipitation precipitationType;

    @Override
    public MapCodec<LayeredCauldronBlock> codec()
    {
        return CODEC;
    }

    public LayeredCauldronBlock(Biome.Precipitation p_310517_, CauldronInteraction.InteractionMap p_313151_, BlockBehaviour.Properties p_153522_)
    {
        super(p_153522_, p_313151_);
        this.precipitationType = p_310517_;
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, Integer.valueOf(1)));
    }

    @Override
    public boolean isFull(BlockState p_153555_)
    {
        return p_153555_.getValue(LEVEL) == 3;
    }

    @Override
    protected boolean canReceiveStalactiteDrip(Fluid p_153551_)
    {
        return p_153551_ == Fluids.WATER && this.precipitationType == Biome.Precipitation.RAIN;
    }

    @Override
    protected double getContentHeight(BlockState p_153528_)
    {
        return (6.0 + (double)p_153528_.getValue(LEVEL).intValue() * 3.0) / 16.0;
    }

    @Override
    protected void entityInside(BlockState p_153534_, Level p_153535_, BlockPos p_153536_, Entity p_153537_)
    {
        if (!p_153535_.isClientSide && p_153537_.isOnFire() && this.isEntityInsideContent(p_153534_, p_153536_, p_153537_))
        {
            p_153537_.clearFire();

            if (p_153537_.mayInteract(p_153535_, p_153536_))
            {
                this.handleEntityOnFireInside(p_153534_, p_153535_, p_153536_);
            }
        }
    }

    private void handleEntityOnFireInside(BlockState p_153556_, Level p_153557_, BlockPos p_153558_)
    {
        if (this.precipitationType == Biome.Precipitation.SNOW)
        {
            lowerFillLevel(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LEVEL, p_153556_.getValue(LEVEL)), p_153557_, p_153558_);
        }
        else
        {
            lowerFillLevel(p_153556_, p_153557_, p_153558_);
        }
    }

    public static void lowerFillLevel(BlockState p_153560_, Level p_153561_, BlockPos p_153562_)
    {
        int i = p_153560_.getValue(LEVEL) - 1;
        BlockState blockstate = i == 0 ? Blocks.CAULDRON.defaultBlockState() : p_153560_.setValue(LEVEL, Integer.valueOf(i));
        p_153561_.setBlockAndUpdate(p_153562_, blockstate);
        p_153561_.gameEvent(GameEvent.BLOCK_CHANGE, p_153562_, GameEvent.Context.of(blockstate));
    }

    @Override
    public void handlePrecipitation(BlockState p_153539_, Level p_153540_, BlockPos p_153541_, Biome.Precipitation p_153542_)
    {
        if (CauldronBlock.shouldHandlePrecipitation(p_153540_, p_153542_) && p_153539_.getValue(LEVEL) != 3 && p_153542_ == this.precipitationType)
        {
            BlockState blockstate = p_153539_.cycle(LEVEL);
            p_153540_.setBlockAndUpdate(p_153541_, blockstate);
            p_153540_.gameEvent(GameEvent.BLOCK_CHANGE, p_153541_, GameEvent.Context.of(blockstate));
        }
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_153530_, Level p_153531_, BlockPos p_153532_)
    {
        return p_153530_.getValue(LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_153549_)
    {
        p_153549_.add(LEVEL);
    }

    @Override
    protected void receiveStalactiteDrip(BlockState p_153544_, Level p_153545_, BlockPos p_153546_, Fluid p_153547_)
    {
        if (!this.isFull(p_153544_))
        {
            BlockState blockstate = p_153544_.setValue(LEVEL, Integer.valueOf(p_153544_.getValue(LEVEL) + 1));
            p_153545_.setBlockAndUpdate(p_153546_, blockstate);
            p_153545_.gameEvent(GameEvent.BLOCK_CHANGE, p_153546_, GameEvent.Context.of(blockstate));
            p_153545_.levelEvent(1047, p_153546_, 0);
        }
    }
}
