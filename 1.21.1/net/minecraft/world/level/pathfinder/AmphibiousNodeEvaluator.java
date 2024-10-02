package net.minecraft.world.level.pathfinder;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;

public class AmphibiousNodeEvaluator extends WalkNodeEvaluator
{
    private final boolean prefersShallowSwimming;
    private float oldWalkableCost;
    private float oldWaterBorderCost;

    public AmphibiousNodeEvaluator(boolean p_164659_)
    {
        this.prefersShallowSwimming = p_164659_;
    }

    @Override
    public void prepare(PathNavigationRegion p_164671_, Mob p_164672_)
    {
        super.prepare(p_164671_, p_164672_);
        p_164672_.setPathfindingMalus(PathType.WATER, 0.0F);
        this.oldWalkableCost = p_164672_.getPathfindingMalus(PathType.WALKABLE);
        p_164672_.setPathfindingMalus(PathType.WALKABLE, 6.0F);
        this.oldWaterBorderCost = p_164672_.getPathfindingMalus(PathType.WATER_BORDER);
        p_164672_.setPathfindingMalus(PathType.WATER_BORDER, 4.0F);
    }

    @Override
    public void done()
    {
        this.mob.setPathfindingMalus(PathType.WALKABLE, this.oldWalkableCost);
        this.mob.setPathfindingMalus(PathType.WATER_BORDER, this.oldWaterBorderCost);
        super.done();
    }

    @Override
    public Node getStart()
    {
        return !this.mob.isInWater()
               ? super.getStart()
               : this.getStartNode(
                   new BlockPos(
                       Mth.floor(this.mob.getBoundingBox().minX),
                       Mth.floor(this.mob.getBoundingBox().minY + 0.5),
                       Mth.floor(this.mob.getBoundingBox().minZ)
                   )
               );
    }

    @Override
    public Target getTarget(double p_330100_, double p_334194_, double p_330998_)
    {
        return this.getTargetNodeAt(p_330100_, p_334194_ + 0.5, p_330998_);
    }

    @Override
    public int getNeighbors(Node[] p_164676_, Node p_164677_)
    {
        int i = super.getNeighbors(p_164676_, p_164677_);
        PathType pathtype = this.getCachedPathType(p_164677_.x, p_164677_.y + 1, p_164677_.z);
        PathType pathtype1 = this.getCachedPathType(p_164677_.x, p_164677_.y, p_164677_.z);
        int j;

        if (this.mob.getPathfindingMalus(pathtype) >= 0.0F && pathtype1 != PathType.STICKY_HONEY)
        {
            j = Mth.floor(Math.max(1.0F, this.mob.maxUpStep()));
        }
        else
        {
            j = 0;
        }

        double d0 = this.getFloorLevel(new BlockPos(p_164677_.x, p_164677_.y, p_164677_.z));
        Node node = this.findAcceptedNode(p_164677_.x, p_164677_.y + 1, p_164677_.z, Math.max(0, j - 1), d0, Direction.UP, pathtype1);
        Node node1 = this.findAcceptedNode(p_164677_.x, p_164677_.y - 1, p_164677_.z, j, d0, Direction.DOWN, pathtype1);

        if (this.isVerticalNeighborValid(node, p_164677_))
        {
            p_164676_[i++] = node;
        }

        if (this.isVerticalNeighborValid(node1, p_164677_) && pathtype1 != PathType.TRAPDOOR)
        {
            p_164676_[i++] = node1;
        }

        for (int k = 0; k < i; k++)
        {
            Node node2 = p_164676_[k];

            if (node2.type == PathType.WATER && this.prefersShallowSwimming && node2.y < this.mob.level().getSeaLevel() - 10)
            {
                node2.costMalus++;
            }
        }

        return i;
    }

    private boolean isVerticalNeighborValid(@Nullable Node p_230611_, Node p_230612_)
    {
        return this.isNeighborValid(p_230611_, p_230612_) && p_230611_.type == PathType.WATER;
    }

    @Override
    protected boolean isAmphibious()
    {
        return true;
    }

    @Override
    public PathType getPathType(PathfindingContext p_336213_, int p_329171_, int p_336028_, int p_327966_)
    {
        PathType pathtype = p_336213_.getPathTypeFromState(p_329171_, p_336028_, p_327966_);

        if (pathtype == PathType.WATER)
        {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (Direction direction : Direction.values())
            {
                blockpos$mutableblockpos.set(p_329171_, p_336028_, p_327966_).move(direction);
                PathType pathtype1 = p_336213_.getPathTypeFromState(
                                         blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getY(), blockpos$mutableblockpos.getZ()
                                     );

                if (pathtype1 == PathType.BLOCKED)
                {
                    return PathType.WATER_BORDER;
                }
            }

            return PathType.WATER;
        }
        else
        {
            return super.getPathType(p_336213_, p_329171_, p_336028_, p_327966_);
        }
    }
}
