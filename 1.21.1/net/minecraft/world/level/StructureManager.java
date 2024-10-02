package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.chunk.StructureAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public class StructureManager
{
    private final LevelAccessor level;
    private final WorldOptions worldOptions;
    private final StructureCheck structureCheck;

    public StructureManager(LevelAccessor p_249675_, WorldOptions p_248820_, StructureCheck p_249103_)
    {
        this.level = p_249675_;
        this.worldOptions = p_248820_;
        this.structureCheck = p_249103_;
    }

    public StructureManager forWorldGenRegion(WorldGenRegion p_220469_)
    {
        if (p_220469_.getLevel() != this.level)
        {
            throw new IllegalStateException("Using invalid structure manager (source level: " + p_220469_.getLevel() + ", region: " + p_220469_);
        }
        else
        {
            return new StructureManager(p_220469_, this.worldOptions, this.structureCheck);
        }
    }

    public List<StructureStart> startsForStructure(ChunkPos p_220478_, Predicate<Structure> p_220479_)
    {
        Map<Structure, LongSet> map = this.level.getChunk(p_220478_.x, p_220478_.z, ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
        Builder<StructureStart> builder = ImmutableList.builder();

        for (Entry<Structure, LongSet> entry : map.entrySet())
        {
            Structure structure = entry.getKey();

            if (p_220479_.test(structure))
            {
                this.fillStartsForStructure(structure, entry.getValue(), builder::add);
            }
        }

        return builder.build();
    }

    public List<StructureStart> startsForStructure(SectionPos p_220505_, Structure p_220506_)
    {
        LongSet longset = this.level.getChunk(p_220505_.x(), p_220505_.z(), ChunkStatus.STRUCTURE_REFERENCES).getReferencesForStructure(p_220506_);
        Builder<StructureStart> builder = ImmutableList.builder();
        this.fillStartsForStructure(p_220506_, longset, builder::add);
        return builder.build();
    }

    public void fillStartsForStructure(Structure p_220481_, LongSet p_220482_, Consumer<StructureStart> p_220483_)
    {
        for (long i : p_220482_)
        {
            SectionPos sectionpos = SectionPos.of(new ChunkPos(i), this.level.getMinSection());
            StructureStart structurestart = this.getStartForStructure(
                                                sectionpos, p_220481_, this.level.getChunk(sectionpos.x(), sectionpos.z(), ChunkStatus.STRUCTURE_STARTS)
                                            );

            if (structurestart != null && structurestart.isValid())
            {
                p_220483_.accept(structurestart);
            }
        }
    }

    @Nullable
    public StructureStart getStartForStructure(SectionPos p_220513_, Structure p_220514_, StructureAccess p_220515_)
    {
        return p_220515_.getStartForStructure(p_220514_);
    }

    public void setStartForStructure(SectionPos p_220517_, Structure p_220518_, StructureStart p_220519_, StructureAccess p_220520_)
    {
        p_220520_.setStartForStructure(p_220518_, p_220519_);
    }

    public void addReferenceForStructure(SectionPos p_220508_, Structure p_220509_, long p_220510_, StructureAccess p_220511_)
    {
        p_220511_.addReferenceForStructure(p_220509_, p_220510_);
    }

    public boolean shouldGenerateStructures()
    {
        return this.worldOptions.generateStructures();
    }

    public StructureStart getStructureAt(BlockPos p_220495_, Structure p_220496_)
    {
        for (StructureStart structurestart : this.startsForStructure(SectionPos.of(p_220495_), p_220496_))
        {
            if (structurestart.getBoundingBox().isInside(p_220495_))
            {
                return structurestart;
            }
        }

        return StructureStart.INVALID_START;
    }

    public StructureStart getStructureWithPieceAt(BlockPos p_220492_, TagKey<Structure> p_220493_)
    {
        return this.getStructureWithPieceAt(p_220492_, p_327244_ -> p_327244_.is(p_220493_));
    }

    public StructureStart getStructureWithPieceAt(BlockPos p_220489_, HolderSet<Structure> p_335973_)
    {
        return this.getStructureWithPieceAt(p_220489_, p_335973_::contains);
    }

    public StructureStart getStructureWithPieceAt(BlockPos p_328568_, Predicate<Holder<Structure>> p_330204_)
    {
        Registry<Structure> registry = this.registryAccess().registryOrThrow(Registries.STRUCTURE);

        for (StructureStart structurestart : this.startsForStructure(
                    new ChunkPos(p_328568_), p_327242_ -> registry.getHolder(registry.getId(p_327242_)).map(p_330204_::test).orElse(false)
                ))
        {
            if (this.structureHasPieceAt(p_328568_, structurestart))
            {
                return structurestart;
            }
        }

        return StructureStart.INVALID_START;
    }

    public StructureStart getStructureWithPieceAt(BlockPos p_220525_, Structure p_220526_)
    {
        for (StructureStart structurestart : this.startsForStructure(SectionPos.of(p_220525_), p_220526_))
        {
            if (this.structureHasPieceAt(p_220525_, structurestart))
            {
                return structurestart;
            }
        }

        return StructureStart.INVALID_START;
    }

    public boolean structureHasPieceAt(BlockPos p_220498_, StructureStart p_220499_)
    {
        for (StructurePiece structurepiece : p_220499_.getPieces())
        {
            if (structurepiece.getBoundingBox().isInside(p_220498_))
            {
                return true;
            }
        }

        return false;
    }

    public boolean hasAnyStructureAt(BlockPos p_220487_)
    {
        SectionPos sectionpos = SectionPos.of(p_220487_);
        return this.level.getChunk(sectionpos.x(), sectionpos.z(), ChunkStatus.STRUCTURE_REFERENCES).hasAnyStructureReferences();
    }

    public Map<Structure, LongSet> getAllStructuresAt(BlockPos p_220523_)
    {
        SectionPos sectionpos = SectionPos.of(p_220523_);
        return this.level.getChunk(sectionpos.x(), sectionpos.z(), ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
    }

    public StructureCheckResult checkStructurePresence(ChunkPos p_220474_, Structure p_220475_, StructurePlacement p_332450_, boolean p_220476_)
    {
        return this.structureCheck.checkStart(p_220474_, p_220475_, p_332450_, p_220476_);
    }

    public void addReference(StructureStart p_220485_)
    {
        p_220485_.addReference();
        this.structureCheck.incrementReference(p_220485_.getChunkPos(), p_220485_.getStructure());
    }

    public RegistryAccess registryAccess()
    {
        return this.level.registryAccess();
    }
}
