package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class Beardifier implements DensityFunctions.BeardifierOrMarker
{
    public static final int BEARD_KERNEL_RADIUS = 12;
    private static final int BEARD_KERNEL_SIZE = 24;
    private static final float[] BEARD_KERNEL = Util.make(new float[13824], p_158082_ ->
    {
        for (int i = 0; i < 24; i++)
        {
            for (int j = 0; j < 24; j++)
            {
                for (int k = 0; k < 24; k++)
                {
                    p_158082_[i * 24 * 24 + j * 24 + k] = (float)computeBeardContribution(j - 12, k - 12, i - 12);
                }
            }
        }
    });
    private final ObjectListIterator<Beardifier.Rigid> pieceIterator;
    private final ObjectListIterator<JigsawJunction> junctionIterator;

    public static Beardifier forStructuresInChunk(StructureManager p_223938_, ChunkPos p_223939_)
    {
        int i = p_223939_.getMinBlockX();
        int j = p_223939_.getMinBlockZ();
        ObjectList<Beardifier.Rigid> objectlist = new ObjectArrayList<>(10);
        ObjectList<JigsawJunction> objectlist1 = new ObjectArrayList<>(32);
        p_223938_.startsForStructure(p_223939_, p_223941_ -> p_223941_.terrainAdaptation() != TerrainAdjustment.NONE).forEach(p_223936_ ->
        {
            TerrainAdjustment terrainadjustment = p_223936_.getStructure().terrainAdaptation();

            for (StructurePiece structurepiece : p_223936_.getPieces())
            {
                if (structurepiece.isCloseToChunk(p_223939_, 12))
                {
                    if (structurepiece instanceof PoolElementStructurePiece)
                    {
                        PoolElementStructurePiece poolelementstructurepiece = (PoolElementStructurePiece)structurepiece;
                        StructureTemplatePool.Projection structuretemplatepool$projection = poolelementstructurepiece.getElement().getProjection();

                        if (structuretemplatepool$projection == StructureTemplatePool.Projection.RIGID)
                        {
                            objectlist.add(new Beardifier.Rigid(poolelementstructurepiece.getBoundingBox(), terrainadjustment, poolelementstructurepiece.getGroundLevelDelta()));
                        }

                        for (JigsawJunction jigsawjunction : poolelementstructurepiece.getJunctions())
                        {
                            int k = jigsawjunction.getSourceX();
                            int l = jigsawjunction.getSourceZ();

                            if (k > i - 12 && l > j - 12 && k < i + 15 + 12 && l < j + 15 + 12)
                            {
                                objectlist1.add(jigsawjunction);
                            }
                        }
                    }
                    else
                    {
                        objectlist.add(new Beardifier.Rigid(structurepiece.getBoundingBox(), terrainadjustment, 0));
                    }
                }
            }
        });
        return new Beardifier(objectlist.iterator(), objectlist1.iterator());
    }

    @VisibleForTesting
    public Beardifier(ObjectListIterator<Beardifier.Rigid> p_223917_, ObjectListIterator<JigsawJunction> p_223918_)
    {
        this.pieceIterator = p_223917_;
        this.junctionIterator = p_223918_;
    }

    @Override
    public double compute(DensityFunction.FunctionContext p_208200_)
    {
        int i = p_208200_.blockX();
        int j = p_208200_.blockY();
        int k = p_208200_.blockZ();
        double d0 = 0.0;

        while (this.pieceIterator.hasNext())
        {
            Beardifier.Rigid beardifier$rigid = this.pieceIterator.next();
            BoundingBox boundingbox = beardifier$rigid.box();
            int l = beardifier$rigid.groundLevelDelta();
            int i1 = Math.max(0, Math.max(boundingbox.minX() - i, i - boundingbox.maxX()));
            int j1 = Math.max(0, Math.max(boundingbox.minZ() - k, k - boundingbox.maxZ()));
            int k1 = boundingbox.minY() + l;
            int l1 = j - k1;

            int i2 = switch (beardifier$rigid.terrainAdjustment())
            {
                case NONE -> 0;

                case BURY, BEARD_THIN -> l1;

                case BEARD_BOX -> Math.max(0, Math.max(k1 - j, j - boundingbox.maxY()));

                case ENCAPSULATE -> Math.max(0, Math.max(boundingbox.minY() - j, j - boundingbox.maxY()));
            };

            d0 += switch (beardifier$rigid.terrainAdjustment())
            {
                case NONE -> 0.0;

                case BURY -> getBuryContribution((double)i1, (double)i2 / 2.0, (double)j1);

                case BEARD_THIN, BEARD_BOX -> getBeardContribution(i1, i2, j1, l1) * 0.8;

                case ENCAPSULATE -> getBuryContribution((double)i1 / 2.0, (double)i2 / 2.0, (double)j1 / 2.0) * 0.8;
            };
        }

        this.pieceIterator.back(Integer.MAX_VALUE);

        while (this.junctionIterator.hasNext())
        {
            JigsawJunction jigsawjunction = this.junctionIterator.next();
            int j2 = i - jigsawjunction.getSourceX();
            int k2 = j - jigsawjunction.getSourceGroundY();
            int l2 = k - jigsawjunction.getSourceZ();
            d0 += getBeardContribution(j2, k2, l2, k2) * 0.4;
        }

        this.junctionIterator.back(Integer.MAX_VALUE);
        return d0;
    }

    @Override
    public double minValue()
    {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double maxValue()
    {
        return Double.POSITIVE_INFINITY;
    }

    private static double getBuryContribution(double p_328731_, double p_336073_, double p_329819_)
    {
        double d0 = Mth.length(p_328731_, p_336073_, p_329819_);
        return Mth.clampedMap(d0, 0.0, 6.0, 1.0, 0.0);
    }

    private static double getBeardContribution(int p_223926_, int p_223927_, int p_223928_, int p_223929_)
    {
        int i = p_223926_ + 12;
        int j = p_223927_ + 12;
        int k = p_223928_ + 12;

        if (isInKernelRange(i) && isInKernelRange(j) && isInKernelRange(k))
        {
            double d0 = (double)p_223929_ + 0.5;
            double d1 = Mth.lengthSquared((double)p_223926_, d0, (double)p_223928_);
            double d2 = -d0 * Mth.fastInvSqrt(d1 / 2.0) / 2.0;
            return d2 * (double)BEARD_KERNEL[k * 24 * 24 + i * 24 + j];
        }
        else
        {
            return 0.0;
        }
    }

    private static boolean isInKernelRange(int p_223920_)
    {
        return p_223920_ >= 0 && p_223920_ < 24;
    }

    private static double computeBeardContribution(int p_158092_, int p_158093_, int p_158094_)
    {
        return computeBeardContribution(p_158092_, (double)p_158093_ + 0.5, p_158094_);
    }

    private static double computeBeardContribution(int p_223922_, double p_223923_, int p_223924_)
    {
        double d0 = Mth.lengthSquared((double)p_223922_, p_223923_, (double)p_223924_);
        return Math.pow(Math.E, -d0 / 16.0);
    }

    @VisibleForTesting
    public static record Rigid(BoundingBox box, TerrainAdjustment terrainAdjustment, int groundLevelDelta)
    {
    }
}
