package net.minecraftforge.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;

public class PartEntity<T extends Entity> extends Entity
{
    private final T parent;

    public PartEntity(T parent)
    {
        super(parent.getType(), parent.level());
        this.parent = parent;
    }

    public T getParent()
    {
        return this.parent;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builderIn)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound)
    {
        throw new UnsupportedOperationException();
    }
}
