package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PiglinWallSkullBlock extends WallSkullBlock
{
    public static final MapCodec<PiglinWallSkullBlock> CODEC = simpleCodec(PiglinWallSkullBlock::new);
    private static final Map<Direction, VoxelShape> AABBS = Maps.immutableEnumMap(
                Map.of(
                    Direction.NORTH,
                    Block.box(3.0, 4.0, 8.0, 13.0, 12.0, 16.0),
                    Direction.SOUTH,
                    Block.box(3.0, 4.0, 0.0, 13.0, 12.0, 8.0),
                    Direction.EAST,
                    Block.box(0.0, 4.0, 3.0, 8.0, 12.0, 13.0),
                    Direction.WEST,
                    Block.box(8.0, 4.0, 3.0, 16.0, 12.0, 13.0)
                )
            );

    @Override
    public MapCodec<PiglinWallSkullBlock> codec()
    {
        return CODEC;
    }

    public PiglinWallSkullBlock(BlockBehaviour.Properties p_261530_)
    {
        super(SkullBlock.Types.PIGLIN, p_261530_);
    }

    @Override
    protected VoxelShape getShape(BlockState p_261765_, BlockGetter p_261604_, BlockPos p_261948_, CollisionContext p_261889_)
    {
        return AABBS.get(p_261765_.getValue(FACING));
    }
}
