package net.optifine;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public class RandomEntity implements IRandomEntity
{
    private Entity entity;

    @Override
    public int getId()
    {
        UUID uuid = this.entity.getUUID();
        long i = uuid.getLeastSignificantBits();
        return (int)(i & 2147483647L);
    }

    @Override
    public BlockPos getSpawnPosition()
    {
        return this.entity.getEntityData().spawnPosition;
    }

    @Override
    public Biome getSpawnBiome()
    {
        return this.entity.getEntityData().spawnBiome;
    }

    @Override
    public String getName()
    {
        return this.entity.hasCustomName() ? this.entity.getCustomName().getString() : null;
    }

    @Override
    public int getHealth()
    {
        return !(this.entity instanceof LivingEntity livingentity) ? 0 : (int)livingentity.getHealth();
    }

    @Override
    public int getMaxHealth()
    {
        return !(this.entity instanceof LivingEntity livingentity) ? 0 : (int)livingentity.getMaxHealth();
    }

    public Entity getEntity()
    {
        return this.entity;
    }

    public void setEntity(Entity entity)
    {
        this.entity = entity;
    }

    @Override
    public CompoundTag getNbtTag()
    {
        SynchedEntityData synchedentitydata = this.entity.getEntityData();
        CompoundTag compoundtag = synchedentitydata.nbtTag;
        long i = System.currentTimeMillis();

        if (compoundtag == null || synchedentitydata.nbtTagUpdateMs < i - 1000L)
        {
            compoundtag = new CompoundTag();
            this.entity.saveWithoutId(compoundtag);

            if (this.entity instanceof TamableAnimal tamableanimal)
            {
                compoundtag.putBoolean("Sitting", tamableanimal.isInSittingPose());
            }

            synchedentitydata.nbtTag = compoundtag;
            synchedentitydata.nbtTagUpdateMs = i;
        }

        return compoundtag;
    }

    @Override
    public DyeColor getColor()
    {
        return RandomEntityRule.getEntityColor(this.entity);
    }

    @Override
    public BlockState getBlockState()
    {
        if (this.entity instanceof ItemEntity itementity && itementity.getItem().getItem() instanceof BlockItem blockitem)
        {
            return blockitem.getBlock().defaultBlockState();
        }

        SynchedEntityData synchedentitydata = this.entity.getEntityData();
        BlockState blockstate = synchedentitydata.blockStateOn;
        long i = System.currentTimeMillis();

        if (blockstate == null || synchedentitydata.blockStateOnUpdateMs < i - 50L)
        {
            BlockPos blockpos = this.entity.blockPosition();
            blockstate = this.entity.getCommandSenderWorld().getBlockState(blockpos);

            if (blockstate.isAir())
            {
                blockstate = this.entity.getCommandSenderWorld().getBlockState(blockpos.below());
            }

            synchedentitydata.blockStateOn = blockstate;
            synchedentitydata.blockStateOnUpdateMs = i;
        }

        return blockstate;
    }

    @Override
    public String toString()
    {
        return this.entity.toString();
    }
}
