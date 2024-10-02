package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallBannerBlock extends AbstractBannerBlock
{
    public static final MapCodec<WallBannerBlock> CODEC = RecordCodecBuilder.mapCodec(
                p_311841_ -> p_311841_.group(DyeColor.CODEC.fieldOf("color").forGetter(AbstractBannerBlock::getColor), propertiesCodec())
                .apply(p_311841_, WallBannerBlock::new)
            );
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = Maps.newEnumMap(
                ImmutableMap.of(
                    Direction.NORTH,
                    Block.box(0.0, 0.0, 14.0, 16.0, 12.5, 16.0),
                    Direction.SOUTH,
                    Block.box(0.0, 0.0, 0.0, 16.0, 12.5, 2.0),
                    Direction.WEST,
                    Block.box(14.0, 0.0, 0.0, 16.0, 12.5, 16.0),
                    Direction.EAST,
                    Block.box(0.0, 0.0, 0.0, 2.0, 12.5, 16.0)
                )
            );

    @Override
    public MapCodec<WallBannerBlock> codec()
    {
        return CODEC;
    }

    public WallBannerBlock(DyeColor p_57920_, BlockBehaviour.Properties p_57921_)
    {
        super(p_57920_, p_57921_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public String getDescriptionId()
    {
        return this.asItem().getDescriptionId();
    }

    @Override
    protected boolean canSurvive(BlockState p_57925_, LevelReader p_57926_, BlockPos p_57927_)
    {
        return p_57926_.getBlockState(p_57927_.relative(p_57925_.getValue(FACING).getOpposite())).isSolid();
    }

    @Override
    protected BlockState updateShape(BlockState p_57935_, Direction p_57936_, BlockState p_57937_, LevelAccessor p_57938_, BlockPos p_57939_, BlockPos p_57940_)
    {
        return p_57936_ == p_57935_.getValue(FACING).getOpposite() && !p_57935_.canSurvive(p_57938_, p_57939_)
               ? Blocks.AIR.defaultBlockState()
               : super.updateShape(p_57935_, p_57936_, p_57937_, p_57938_, p_57939_, p_57940_);
    }

    @Override
    protected VoxelShape getShape(BlockState p_57944_, BlockGetter p_57945_, BlockPos p_57946_, CollisionContext p_57947_)
    {
        return SHAPES.get(p_57944_.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_57923_)
    {
        BlockState blockstate = this.defaultBlockState();
        LevelReader levelreader = p_57923_.getLevel();
        BlockPos blockpos = p_57923_.getClickedPos();
        Direction[] adirection = p_57923_.getNearestLookingDirections();

        for (Direction direction : adirection)
        {
            if (direction.getAxis().isHorizontal())
            {
                Direction direction1 = direction.getOpposite();
                blockstate = blockstate.setValue(FACING, direction1);

                if (blockstate.canSurvive(levelreader, blockpos))
                {
                    return blockstate;
                }
            }
        }

        return null;
    }

    @Override
    protected BlockState rotate(BlockState p_57932_, Rotation p_57933_)
    {
        return p_57932_.setValue(FACING, p_57933_.rotate(p_57932_.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState p_57929_, Mirror p_57930_)
    {
        return p_57929_.rotate(p_57930_.getRotation(p_57929_.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_57942_)
    {
        p_57942_.add(FACING);
    }
}
