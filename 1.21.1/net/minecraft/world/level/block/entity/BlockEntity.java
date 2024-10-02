package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import org.slf4j.Logger;

public abstract class BlockEntity extends CapabilityProvider<BlockEntity> implements IForgeBlockEntity
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockEntityType<?> type;
    @Nullable
    protected Level level;
    protected final BlockPos worldPosition;
    protected boolean remove;
    private BlockState blockState;
    private DataComponentMap components = DataComponentMap.EMPTY;
    public CompoundTag nbtTag;
    public long nbtTagUpdateMs = 0L;

    public BlockEntity(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_)
    {
        super(BlockEntity.class);
        this.type = p_155228_;
        this.worldPosition = p_155229_.immutable();
        this.validateBlockState(p_155230_);
        this.blockState = p_155230_;
        this.gatherCapabilities();
    }

    private void validateBlockState(BlockState p_345558_)
    {
        if (!this.isValidBlockState(p_345558_))
        {
            throw new IllegalStateException("Invalid block entity " + this.getNameForReporting() + " state at " + this.worldPosition + ", got " + p_345558_);
        }
    }

    public boolean isValidBlockState(BlockState p_345570_)
    {
        return this.type.isValid(p_345570_);
    }

    public static BlockPos getPosFromTag(CompoundTag p_187473_)
    {
        return new BlockPos(p_187473_.getInt("x"), p_187473_.getInt("y"), p_187473_.getInt("z"));
    }

    @Nullable
    public Level getLevel()
    {
        return this.level;
    }

    public void setLevel(Level p_155231_)
    {
        this.level = p_155231_;
    }

    public boolean hasLevel()
    {
        return this.level != null;
    }

    protected void loadAdditional(CompoundTag p_331149_, HolderLookup.Provider p_333170_)
    {
        if (this.getCapabilities() != null && p_331149_.contains("ForgeCaps"))
        {
            this.deserializeCaps(p_333170_, p_331149_.getCompound("ForgeCaps"));
        }
    }

    public final void loadWithComponents(CompoundTag p_331756_, HolderLookup.Provider p_335164_)
    {
        this.loadAdditional(p_331756_, p_335164_);
        BlockEntity.ComponentHelper.COMPONENTS_CODEC
        .parse(p_335164_.createSerializationContext(NbtOps.INSTANCE), p_331756_)
        .resultOrPartial(p_318380_0_ -> LOGGER.warn("Failed to load components: {}", p_318380_0_))
        .ifPresent(p_318382_1_ -> this.components = p_318382_1_);
    }

    public final void loadCustomOnly(CompoundTag p_333694_, HolderLookup.Provider p_332017_)
    {
        this.loadAdditional(p_333694_, p_332017_);
    }

    protected void saveAdditional(CompoundTag p_187471_, HolderLookup.Provider p_327783_)
    {
        if (this.getCapabilities() != null)
        {
            p_187471_.put("ForgeCaps", this.serializeCaps(p_327783_));
        }
    }

    public final CompoundTag saveWithFullMetadata(HolderLookup.Provider p_331193_)
    {
        CompoundTag compoundtag = this.saveWithoutMetadata(p_331193_);
        this.saveMetadata(compoundtag);
        return compoundtag;
    }

    public final CompoundTag saveWithId(HolderLookup.Provider p_332686_)
    {
        CompoundTag compoundtag = this.saveWithoutMetadata(p_332686_);
        this.saveId(compoundtag);
        return compoundtag;
    }

    public final CompoundTag saveWithoutMetadata(HolderLookup.Provider p_332372_)
    {
        CompoundTag compoundtag = new CompoundTag();
        this.saveAdditional(compoundtag, p_332372_);
        BlockEntity.ComponentHelper.COMPONENTS_CODEC
        .encodeStart(p_332372_.createSerializationContext(NbtOps.INSTANCE), this.components)
        .resultOrPartial(p_318379_0_ -> LOGGER.warn("Failed to save components: {}", p_318379_0_))
        .ifPresent(p_318384_1_ -> compoundtag.merge((CompoundTag)p_318384_1_));
        return compoundtag;
    }

    public final CompoundTag saveCustomOnly(HolderLookup.Provider p_333091_)
    {
        CompoundTag compoundtag = new CompoundTag();
        this.saveAdditional(compoundtag, p_333091_);
        return compoundtag;
    }

    public final CompoundTag saveCustomAndMetadata(HolderLookup.Provider p_334487_)
    {
        CompoundTag compoundtag = this.saveCustomOnly(p_334487_);
        this.saveMetadata(compoundtag);
        return compoundtag;
    }

    private void saveId(CompoundTag p_187475_)
    {
        ResourceLocation resourcelocation = BlockEntityType.getKey(this.getType());

        if (resourcelocation == null)
        {
            throw new RuntimeException(this.getClass() + " is missing a mapping! This is a bug!");
        }
        else
        {
            p_187475_.putString("id", resourcelocation.toString());
        }
    }

    public static void addEntityType(CompoundTag p_187469_, BlockEntityType<?> p_187470_)
    {
        p_187469_.putString("id", BlockEntityType.getKey(p_187470_).toString());
    }

    public void saveToItem(ItemStack p_187477_, HolderLookup.Provider p_336381_)
    {
        CompoundTag compoundtag = this.saveCustomOnly(p_336381_);
        this.removeComponentsFromTag(compoundtag);
        BlockItem.setBlockEntityData(p_187477_, this.getType(), compoundtag);
        p_187477_.applyComponents(this.collectComponents());
    }

    private void saveMetadata(CompoundTag p_187479_)
    {
        this.saveId(p_187479_);
        p_187479_.putInt("x", this.worldPosition.getX());
        p_187479_.putInt("y", this.worldPosition.getY());
        p_187479_.putInt("z", this.worldPosition.getZ());
    }

    @Nullable
    public static BlockEntity loadStatic(BlockPos p_155242_, BlockState p_155243_, CompoundTag p_155244_, HolderLookup.Provider p_336084_)
    {
        String s = p_155244_.getString("id");
        ResourceLocation resourcelocation = ResourceLocation.tryParse(s);

        if (resourcelocation == null)
        {
            LOGGER.error("Block entity has invalid type: {}", s);
            return null;
        }
        else
        {
            return BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(resourcelocation).map(p_155236_3_ ->
            {
                try {
                    return p_155236_3_.create(p_155242_, p_155243_);
                }
                catch (Throwable throwable)
                {
                    LOGGER.error("Failed to create block entity {}", s, throwable);
                    return null;
                }
            }).map(p_318381_3_ ->
            {
                try {
                    p_318381_3_.loadWithComponents(p_155244_, p_336084_);
                    return (BlockEntity)p_318381_3_;
                }
                catch (Throwable throwable)
                {
                    LOGGER.error("Failed to load data for block entity {}", s, throwable);
                    return null;
                }
            }).orElseGet(() ->
            {
                LOGGER.warn("Skipping BlockEntity with id {}", s);
                return null;
            });
        }
    }

    public void setChanged()
    {
        if (this.level != null)
        {
            setChanged(this.level, this.worldPosition, this.blockState);
        }

        this.nbtTag = null;
    }

    protected static void setChanged(Level p_155233_, BlockPos p_155234_, BlockState p_155235_)
    {
        p_155233_.blockEntityChanged(p_155234_);

        if (!p_155235_.isAir())
        {
            p_155233_.updateNeighbourForOutputSignal(p_155234_, p_155235_.getBlock());
        }
    }

    public BlockPos getBlockPos()
    {
        return this.worldPosition;
    }

    public BlockState getBlockState()
    {
        return this.blockState;
    }

    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return null;
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider p_329179_)
    {
        return new CompoundTag();
    }

    public boolean isRemoved()
    {
        return this.remove;
    }

    public void setRemoved()
    {
        this.remove = true;
        this.invalidateCaps();
        this.requestModelDataUpdate();
    }

    @Override
    public void onChunkUnloaded()
    {
        this.invalidateCaps();
    }

    public void clearRemoved()
    {
        this.remove = false;
    }

    public boolean triggerEvent(int p_58889_, int p_58890_)
    {
        return false;
    }

    public void fillCrashReportCategory(CrashReportCategory p_58887_)
    {
        p_58887_.setDetail("Name", this::getNameForReporting);

        if (this.level != null)
        {
            CrashReportCategory.populateBlockDetails(p_58887_, this.level, this.worldPosition, this.getBlockState());
            CrashReportCategory.populateBlockDetails(p_58887_, this.level, this.worldPosition, this.level.getBlockState(this.worldPosition));
        }
    }

    private String getNameForReporting()
    {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.getType()) + " // " + this.getClass().getCanonicalName();
    }

    public boolean onlyOpCanSetNbt()
    {
        return false;
    }

    public BlockEntityType<?> getType()
    {
        return this.type;
    }

    @Deprecated
    public void setBlockState(BlockState p_155251_)
    {
        this.validateBlockState(p_155251_);
        this.blockState = p_155251_;
    }

    protected void applyImplicitComponents(BlockEntity.DataComponentInput p_330805_)
    {
    }

    public final void applyComponentsFromItemStack(ItemStack p_328941_)
    {
        this.applyComponents(p_328941_.getPrototype(), p_328941_.getComponentsPatch());
    }

    public final void applyComponents(DataComponentMap p_335232_, DataComponentPatch p_331646_)
    {
        final Set < DataComponentType<? >> set = new HashSet<>();
        set.add(DataComponents.BLOCK_ENTITY_DATA);
        final DataComponentMap datacomponentmap = PatchedDataComponentMap.fromPatch(p_335232_, p_331646_);
        this.applyImplicitComponents(new BlockEntity.DataComponentInput()
        {
            @Nullable
            @Override
            public <T> T get(DataComponentType<T> p_335233_)
            {
                set.add(p_335233_);
                return datacomponentmap.get(p_335233_);
            }
            @Override
            public <T> T getOrDefault(DataComponentType <? extends T > p_334887_, T p_333244_)
            {
                set.add(p_334887_);
                return datacomponentmap.getOrDefault(p_334887_, p_333244_);
            }
        });
        DataComponentPatch datacomponentpatch = p_331646_.forget(set::contains);
        this.components = datacomponentpatch.split().added();
    }

    protected void collectImplicitComponents(DataComponentMap.Builder p_328216_)
    {
    }

    @Deprecated
    public void removeComponentsFromTag(CompoundTag p_334718_)
    {
    }

    public final DataComponentMap collectComponents()
    {
        DataComponentMap.Builder datacomponentmap$builder = DataComponentMap.builder();
        datacomponentmap$builder.addAll(this.components);
        this.collectImplicitComponents(datacomponentmap$builder);
        return datacomponentmap$builder.build();
    }

    public DataComponentMap components()
    {
        return this.components;
    }

    public void setComponents(DataComponentMap p_335672_)
    {
        this.components = p_335672_;
    }

    @Nullable
    public static Component parseCustomNameSafe(String p_336419_, HolderLookup.Provider p_336417_)
    {
        try
        {
            return Component.Serializer.fromJson(p_336419_, p_336417_);
        }
        catch (Exception exception)
        {
            LOGGER.warn("Failed to parse custom name from string '{}', discarding", p_336419_, exception);
            return null;
        }
    }

    static class ComponentHelper
    {
        public static final Codec<DataComponentMap> COMPONENTS_CODEC = DataComponentMap.CODEC.optionalFieldOf("components", DataComponentMap.EMPTY).codec();

        private ComponentHelper()
        {
        }
    }

    protected interface DataComponentInput
    {
        @Nullable
        <T> T get(DataComponentType<T> p_332690_);

        <T> T getOrDefault(DataComponentType <? extends T > p_330702_, T p_330858_);
    }
}
