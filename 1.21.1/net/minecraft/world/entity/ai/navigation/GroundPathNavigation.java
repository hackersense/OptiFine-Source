package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class GroundPathNavigation extends PathNavigation
{
    private boolean avoidSun;

    public GroundPathNavigation(Mob p_26448_, Level p_26449_)
    {
        super(p_26448_, p_26449_);
    }

    @Override
    protected PathFinder createPathFinder(int p_26453_)
    {
        this.nodeEvaluator = new WalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, p_26453_);
    }

    @Override
    protected boolean canUpdatePath()
    {
        return this.mob.onGround() || this.mob.isInLiquid() || this.mob.isPassenger();
    }

    @Override
    protected Vec3 getTempMobPos()
    {
        return new Vec3(this.mob.getX(), (double)this.getSurfaceY(), this.mob.getZ());
    }

    @Override
    public Path createPath(BlockPos p_26475_, int p_26476_)
    {
        LevelChunk levelchunk = this.level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(p_26475_.getX()), SectionPos.blockToSectionCoord(p_26475_.getZ()));

        if (levelchunk == null)
        {
            return null;
        }
        else
        {
            if (levelchunk.getBlockState(p_26475_).isAir())
            {
                BlockPos blockpos = p_26475_.below();

                while (blockpos.getY() > this.level.getMinBuildHeight() && levelchunk.getBlockState(blockpos).isAir())
                {
                    blockpos = blockpos.below();
                }

                if (blockpos.getY() > this.level.getMinBuildHeight())
                {
                    return super.createPath(blockpos.above(), p_26476_);
                }

                while (blockpos.getY() < this.level.getMaxBuildHeight() && levelchunk.getBlockState(blockpos).isAir())
                {
                    blockpos = blockpos.above();
                }

                p_26475_ = blockpos;
            }

            if (!levelchunk.getBlockState(p_26475_).isSolid())
            {
                return super.createPath(p_26475_, p_26476_);
            }
            else
            {
                BlockPos blockpos1 = p_26475_.above();

                while (blockpos1.getY() < this.level.getMaxBuildHeight() && levelchunk.getBlockState(blockpos1).isSolid())
                {
                    blockpos1 = blockpos1.above();
                }

                return super.createPath(blockpos1, p_26476_);
            }
        }
    }

    @Override
    public Path createPath(Entity p_26465_, int p_26466_)
    {
        return this.createPath(p_26465_.blockPosition(), p_26466_);
    }

    private int getSurfaceY()
    {
        if (this.mob.isInWater() && this.canFloat())
        {
            int i = this.mob.getBlockY();
            BlockState blockstate = this.level.getBlockState(BlockPos.containing(this.mob.getX(), (double)i, this.mob.getZ()));
            int j = 0;

            while (blockstate.is(Blocks.WATER))
            {
                blockstate = this.level.getBlockState(BlockPos.containing(this.mob.getX(), (double)(++i), this.mob.getZ()));

                if (++j > 16)
                {
                    return this.mob.getBlockY();
                }
            }

            return i;
        }
        else
        {
            return Mth.floor(this.mob.getY() + 0.5);
        }
    }

    @Override
    protected void trimPath()
    {
        super.trimPath();

        if (this.avoidSun)
        {
            if (this.level.canSeeSky(BlockPos.containing(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ())))
            {
                return;
            }

            for (int i = 0; i < this.path.getNodeCount(); i++)
            {
                Node node = this.path.getNode(i);

                if (this.level.canSeeSky(new BlockPos(node.x, node.y, node.z)))
                {
                    this.path.truncateNodes(i);
                    return;
                }
            }
        }
    }

    protected boolean hasValidPathType(PathType p_329492_)
    {
        if (p_329492_ == PathType.WATER)
        {
            return false;
        }
        else
        {
            return p_329492_ == PathType.LAVA ? false : p_329492_ != PathType.OPEN;
        }
    }

    public void setCanOpenDoors(boolean p_26478_)
    {
        this.nodeEvaluator.setCanOpenDoors(p_26478_);
    }

    public boolean canPassDoors()
    {
        return this.nodeEvaluator.canPassDoors();
    }

    public void setCanPassDoors(boolean p_148215_)
    {
        this.nodeEvaluator.setCanPassDoors(p_148215_);
    }

    public boolean canOpenDoors()
    {
        return this.nodeEvaluator.canPassDoors();
    }

    public void setAvoidSun(boolean p_26491_)
    {
        this.avoidSun = p_26491_;
    }

    public void setCanWalkOverFences(boolean p_255877_)
    {
        this.nodeEvaluator.setCanWalkOverFences(p_255877_);
    }
}
