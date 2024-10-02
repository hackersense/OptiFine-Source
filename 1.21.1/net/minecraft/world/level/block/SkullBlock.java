package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SkullBlock extends AbstractSkullBlock
{
    public static final MapCodec<SkullBlock> CODEC = RecordCodecBuilder.mapCodec(
                p_311084_ -> p_311084_.group(SkullBlock.Type.CODEC.fieldOf("kind").forGetter(AbstractSkullBlock::getType), propertiesCodec())
                .apply(p_311084_, SkullBlock::new)
            );
    public static final int MAX = RotationSegment.getMaxSegmentIndex();
    private static final int ROTATIONS = MAX + 1;
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    protected static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);
    protected static final VoxelShape PIGLIN_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 8.0, 13.0);

    @Override
    public MapCodec <? extends SkullBlock > codec()
    {
        return CODEC;
    }

    protected SkullBlock(SkullBlock.Type p_56318_, BlockBehaviour.Properties p_56319_)
    {
        super(p_56318_, p_56319_);
        this.registerDefaultState(this.defaultBlockState().setValue(ROTATION, Integer.valueOf(0)));
    }

    @Override
    protected VoxelShape getShape(BlockState p_56331_, BlockGetter p_56332_, BlockPos p_56333_, CollisionContext p_56334_)
    {
        return this.getType() == SkullBlock.Types.PIGLIN ? PIGLIN_SHAPE : SHAPE;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState p_56336_, BlockGetter p_56337_, BlockPos p_56338_)
    {
        return Shapes.empty();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_56321_)
    {
        return super.getStateForPlacement(p_56321_).setValue(ROTATION, Integer.valueOf(RotationSegment.convertToSegment(p_56321_.getRotation())));
    }

    @Override
    protected BlockState rotate(BlockState p_56326_, Rotation p_56327_)
    {
        return p_56326_.setValue(ROTATION, Integer.valueOf(p_56327_.rotate(p_56326_.getValue(ROTATION), ROTATIONS)));
    }

    @Override
    protected BlockState mirror(BlockState p_56323_, Mirror p_56324_)
    {
        return p_56323_.setValue(ROTATION, Integer.valueOf(p_56324_.mirror(p_56323_.getValue(ROTATION), ROTATIONS)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_56329_)
    {
        super.createBlockStateDefinition(p_56329_);
        p_56329_.add(ROTATION);
    }

    public interface Type extends StringRepresentable
    {
        Map<String, SkullBlock.Type> TYPES = new Object2ObjectArrayMap<>();
        Codec<SkullBlock.Type> CODEC = Codec.stringResolver(StringRepresentable::getSerializedName, TYPES::get);
    }

    public static enum Types implements SkullBlock.Type
    {
        SKELETON("skeleton"),
        WITHER_SKELETON("wither_skeleton"),
        PLAYER("player"),
        ZOMBIE("zombie"),
        CREEPER("creeper"),
        PIGLIN("piglin"),
        DRAGON("dragon");

        private final String name;

        private Types(final String p_310892_)
        {
            this.name = p_310892_;
            TYPES.put(p_310892_, this);
        }

        @Override
        public String getSerializedName()
        {
            return this.name;
        }
    }
}
