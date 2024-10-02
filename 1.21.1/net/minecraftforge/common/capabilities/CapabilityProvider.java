package net.minecraftforge.common.capabilities;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public abstract class CapabilityProvider<B>
{
    protected CapabilityProvider(Class<B> baseClass)
    {
    }

    public final void gatherCapabilities()
    {
    }

    protected final CapabilityDispatcher getCapabilities()
    {
        return null;
    }

    protected final void deserializeCaps(HolderLookup.Provider registryAccess, CompoundTag tag)
    {
    }

    protected final CompoundTag serializeCaps(HolderLookup.Provider registryAccess)
    {
        return null;
    }

    public void invalidateCaps()
    {
    }
}
