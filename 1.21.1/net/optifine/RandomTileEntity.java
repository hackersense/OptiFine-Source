package net.optifine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.optifine.util.TileEntityUtils;

public class RandomTileEntity implements IRandomEntity
{
    private BlockEntity tileEntity;
    private static final CompoundTag EMPTY_TAG = new CompoundTag();

    @Override
    public int getId()
    {
        return Config.getRandom(this.getSpawnPosition(), 0);
    }

    @Override
    public BlockPos getSpawnPosition()
    {
        if (this.tileEntity instanceof BedBlockEntity bedblockentity)
        {
            BlockState blockstate = bedblockentity.getBlockState();
            BedPart bedpart = blockstate.getValue(BedBlock.PART);

            if (bedpart == BedPart.HEAD)
            {
                Direction direction = blockstate.getValue(BedBlock.FACING);
                return this.tileEntity.getBlockPos().relative(direction.getOpposite());
            }
        }

        return this.tileEntity.getBlockPos();
    }

    @Override
    public String getName()
    {
        return TileEntityUtils.getTileEntityName(this.tileEntity);
    }

    @Override
    public Biome getSpawnBiome()
    {
        return this.tileEntity.getLevel().getBiome(this.tileEntity.getBlockPos()).value();
    }

    @Override
    public int getHealth()
    {
        return -1;
    }

    @Override
    public int getMaxHealth()
    {
        return -1;
    }

    public BlockEntity getTileEntity()
    {
        return this.tileEntity;
    }

    public void setTileEntity(BlockEntity tileEntity)
    {
        this.tileEntity = tileEntity;
    }

    @Override
    public CompoundTag getNbtTag()
    {
        CompoundTag compoundtag = this.tileEntity.nbtTag;
        long i = System.currentTimeMillis();

        if (compoundtag == null || this.tileEntity.nbtTagUpdateMs < i - 1000L)
        {
            this.tileEntity.nbtTag = makeNbtTag(this.tileEntity);
            this.tileEntity.nbtTagUpdateMs = i;
        }

        return compoundtag;
    }

    private static CompoundTag makeNbtTag(BlockEntity te)
    {
        Level level = te.getLevel();

        if (level == null)
        {
            return EMPTY_TAG;
        }
        else
        {
            RegistryAccess registryaccess = level.registryAccess();
            return registryaccess == null ? EMPTY_TAG : te.saveWithoutMetadata(registryaccess);
        }
    }

    @Override
    public DyeColor getColor()
    {
        return RandomEntityRule.getBlockEntityColor(this.tileEntity);
    }

    @Override
    public BlockState getBlockState()
    {
        return this.tileEntity.getBlockState();
    }

    @Override
    public String toString()
    {
        return this.tileEntity.toString();
    }
}
