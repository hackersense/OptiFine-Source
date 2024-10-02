package net.minecraft.world.entity.vehicle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractMinecart extends VehicleEntity
{
    private static final Vec3 LOWERED_PASSENGER_ATTACHMENT = new Vec3(0.0, 0.0, 0.0);
    private static final EntityDataAccessor<Integer> DATA_ID_DISPLAY_BLOCK = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ID_DISPLAY_OFFSET = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ID_CUSTOM_DISPLAY = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.BOOLEAN);
    private static final ImmutableMap<Pose, ImmutableList<Integer>> POSE_DISMOUNT_HEIGHTS = ImmutableMap.of(
                Pose.STANDING, ImmutableList.of(0, 1, -1), Pose.CROUCHING, ImmutableList.of(0, 1, -1), Pose.SWIMMING, ImmutableList.of(0, 1)
            );
    protected static final float WATER_SLOWDOWN_FACTOR = 0.95F;
    private boolean flipped;
    private boolean onRails;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;
    private Vec3 targetDeltaMovement = Vec3.ZERO;
    private static final Map<RailShape, Pair<Vec3i, Vec3i>> EXITS = Util.make(Maps.newEnumMap(RailShape.class), p_38135_ ->
    {
        Vec3i vec3i = Direction.WEST.getNormal();
        Vec3i vec3i1 = Direction.EAST.getNormal();
        Vec3i vec3i2 = Direction.NORTH.getNormal();
        Vec3i vec3i3 = Direction.SOUTH.getNormal();
        Vec3i vec3i4 = vec3i.below();
        Vec3i vec3i5 = vec3i1.below();
        Vec3i vec3i6 = vec3i2.below();
        Vec3i vec3i7 = vec3i3.below();
        p_38135_.put(RailShape.NORTH_SOUTH, Pair.of(vec3i2, vec3i3));
        p_38135_.put(RailShape.EAST_WEST, Pair.of(vec3i, vec3i1));
        p_38135_.put(RailShape.ASCENDING_EAST, Pair.of(vec3i4, vec3i1));
        p_38135_.put(RailShape.ASCENDING_WEST, Pair.of(vec3i, vec3i5));
        p_38135_.put(RailShape.ASCENDING_NORTH, Pair.of(vec3i2, vec3i7));
        p_38135_.put(RailShape.ASCENDING_SOUTH, Pair.of(vec3i6, vec3i3));
        p_38135_.put(RailShape.SOUTH_EAST, Pair.of(vec3i3, vec3i1));
        p_38135_.put(RailShape.SOUTH_WEST, Pair.of(vec3i3, vec3i));
        p_38135_.put(RailShape.NORTH_WEST, Pair.of(vec3i2, vec3i));
        p_38135_.put(RailShape.NORTH_EAST, Pair.of(vec3i2, vec3i1));
    });

    protected AbstractMinecart(EntityType<?> p_38087_, Level p_38088_)
    {
        super(p_38087_, p_38088_);
        this.blocksBuilding = true;
    }

    protected AbstractMinecart(EntityType<?> p_38090_, Level p_38091_, double p_38092_, double p_38093_, double p_38094_)
    {
        this(p_38090_, p_38091_);
        this.setPos(p_38092_, p_38093_, p_38094_);
        this.xo = p_38092_;
        this.yo = p_38093_;
        this.zo = p_38094_;
    }

    public static AbstractMinecart createMinecart(
        ServerLevel p_310486_,
        double p_38121_,
        double p_38122_,
        double p_38123_,
        AbstractMinecart.Type p_38124_,
        ItemStack p_311363_,
        @Nullable Player p_310754_
    )
    {

        AbstractMinecart abstractminecart = (AbstractMinecart)(switch (p_38124_)
    {
        case CHEST -> new MinecartChest(p_310486_, p_38121_, p_38122_, p_38123_);

            case FURNACE -> new MinecartFurnace(p_310486_, p_38121_, p_38122_, p_38123_);

            case TNT -> new MinecartTNT(p_310486_, p_38121_, p_38122_, p_38123_);

            case SPAWNER -> new MinecartSpawner(p_310486_, p_38121_, p_38122_, p_38123_);

            case HOPPER -> new MinecartHopper(p_310486_, p_38121_, p_38122_, p_38123_);

            case COMMAND_BLOCK -> new MinecartCommandBlock(p_310486_, p_38121_, p_38122_, p_38123_);

            default -> new Minecart(p_310486_, p_38121_, p_38122_, p_38123_);
        });
        EntityType.<AbstractMinecart>createDefaultStackConfig(p_310486_, p_311363_, p_310754_).accept(abstractminecart);
        return abstractminecart;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission()
    {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_333316_)
    {
        super.defineSynchedData(p_333316_);
        p_333316_.define(DATA_ID_DISPLAY_BLOCK, Block.getId(Blocks.AIR.defaultBlockState()));
        p_333316_.define(DATA_ID_DISPLAY_OFFSET, 6);
        p_333316_.define(DATA_ID_CUSTOM_DISPLAY, false);
    }

    @Override
    public boolean canCollideWith(Entity p_38168_)
    {
        return Boat.canVehicleCollide(this, p_38168_);
    }

    @Override
    public boolean isPushable()
    {
        return true;
    }

    @Override
    public Vec3 getRelativePortalPosition(Direction.Axis p_38132_, BlockUtil.FoundRectangle p_38133_)
    {
        return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(p_38132_, p_38133_));
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity p_300806_, EntityDimensions p_300201_, float p_299127_)
    {
        boolean flag = p_300806_ instanceof Villager || p_300806_ instanceof WanderingTrader;
        return flag ? LOWERED_PASSENGER_ATTACHMENT : super.getPassengerAttachmentPoint(p_300806_, p_300201_, p_299127_);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity p_38145_)
    {
        Direction direction = this.getMotionDirection();

        if (direction.getAxis() == Direction.Axis.Y)
        {
            return super.getDismountLocationForPassenger(p_38145_);
        }
        else
        {
            int[][] aint = DismountHelper.offsetsForDirection(direction);
            BlockPos blockpos = this.blockPosition();
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
            ImmutableList<Pose> immutablelist = p_38145_.getDismountPoses();

            for (Pose pose : immutablelist)
            {
                EntityDimensions entitydimensions = p_38145_.getDimensions(pose);
                float f = Math.min(entitydimensions.width(), 1.0F) / 2.0F;

                for (int i : POSE_DISMOUNT_HEIGHTS.get(pose))
                {
                    for (int[] aint1 : aint)
                    {
                        blockpos$mutableblockpos.set(blockpos.getX() + aint1[0], blockpos.getY() + i, blockpos.getZ() + aint1[1]);
                        double d0 = this.level()
                                    .getBlockFloorHeight(
                                        DismountHelper.nonClimbableShape(this.level(), blockpos$mutableblockpos),
                                        () -> DismountHelper.nonClimbableShape(this.level(), blockpos$mutableblockpos.below())
                                    );

                        if (DismountHelper.isBlockFloorValid(d0))
                        {
                            AABB aabb = new AABB((double)(-f), 0.0, (double)(-f), (double)f, (double)entitydimensions.height(), (double)f);
                            Vec3 vec3 = Vec3.upFromBottomCenterOf(blockpos$mutableblockpos, d0);

                            if (DismountHelper.canDismountTo(this.level(), p_38145_, aabb.move(vec3)))
                            {
                                p_38145_.setPose(pose);
                                return vec3;
                            }
                        }
                    }
                }
            }

            double d1 = this.getBoundingBox().maxY;
            blockpos$mutableblockpos.set((double)blockpos.getX(), d1, (double)blockpos.getZ());

            for (Pose pose1 : immutablelist)
            {
                double d2 = (double)p_38145_.getDimensions(pose1).height();
                int j = Mth.ceil(d1 - (double)blockpos$mutableblockpos.getY() + d2);
                double d3 = DismountHelper.findCeilingFrom(
                                blockpos$mutableblockpos, j, p_341494_ -> this.level().getBlockState(p_341494_).getCollisionShape(this.level(), p_341494_)
                            );

                if (d1 + d2 <= d3)
                {
                    p_38145_.setPose(pose1);
                    break;
                }
            }

            return super.getDismountLocationForPassenger(p_38145_);
        }
    }

    @Override
    protected float getBlockSpeedFactor()
    {
        BlockState blockstate = this.level().getBlockState(this.blockPosition());
        return blockstate.is(BlockTags.RAILS) ? 1.0F : super.getBlockSpeedFactor();
    }

    @Override
    public void animateHurt(float p_265349_)
    {
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(this.getDamage() + this.getDamage() * 10.0F);
    }

    @Override
    public boolean isPickable()
    {
        return !this.isRemoved();
    }

    private static Pair<Vec3i, Vec3i> exits(RailShape p_38126_)
    {
        return EXITS.get(p_38126_);
    }

    @Override
    public Direction getMotionDirection()
    {
        return this.flipped ? this.getDirection().getOpposite().getClockWise() : this.getDirection().getClockWise();
    }

    @Override
    protected double getDefaultGravity()
    {
        return this.isInWater() ? 0.005 : 0.04;
    }

    @Override
    public void tick()
    {
        if (this.getHurtTime() > 0)
        {
            this.setHurtTime(this.getHurtTime() - 1);
        }

        if (this.getDamage() > 0.0F)
        {
            this.setDamage(this.getDamage() - 1.0F);
        }

        this.checkBelowWorld();
        this.handlePortal();

        if (this.level().isClientSide)
        {
            if (this.lerpSteps > 0)
            {
                this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
                this.lerpSteps--;
            }
            else
            {
                this.reapplyPosition();
                this.setRot(this.getYRot(), this.getXRot());
            }
        }
        else
        {
            this.applyGravity();
            int i = Mth.floor(this.getX());
            int j = Mth.floor(this.getY());
            int k = Mth.floor(this.getZ());

            if (this.level().getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS))
            {
                j--;
            }

            BlockPos blockpos = new BlockPos(i, j, k);
            BlockState blockstate = this.level().getBlockState(blockpos);
            this.onRails = BaseRailBlock.isRail(blockstate);

            if (this.onRails)
            {
                this.moveAlongTrack(blockpos, blockstate);

                if (blockstate.is(Blocks.ACTIVATOR_RAIL))
                {
                    this.activateMinecart(i, j, k, blockstate.getValue(PoweredRailBlock.POWERED));
                }
            }
            else
            {
                this.comeOffTrack();
            }

            this.checkInsideBlocks();
            this.setXRot(0.0F);
            double d0 = this.xo - this.getX();
            double d1 = this.zo - this.getZ();

            if (d0 * d0 + d1 * d1 > 0.001)
            {
                this.setYRot((float)(Mth.atan2(d1, d0) * 180.0 / Math.PI));

                if (this.flipped)
                {
                    this.setYRot(this.getYRot() + 180.0F);
                }
            }

            double d2 = (double)Mth.wrapDegrees(this.getYRot() - this.yRotO);

            if (d2 < -170.0 || d2 >= 170.0)
            {
                this.setYRot(this.getYRot() + 180.0F);
                this.flipped = !this.flipped;
            }

            this.setRot(this.getYRot(), this.getXRot());

            if (this.getMinecartType() == AbstractMinecart.Type.RIDEABLE && this.getDeltaMovement().horizontalDistanceSqr() > 0.01)
            {
                List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate(0.2F, 0.0, 0.2F), EntitySelector.pushableBy(this));

                if (!list.isEmpty())
                {
                    for (Entity entity1 : list)
                    {
                        if (!(entity1 instanceof Player)
                                && !(entity1 instanceof IronGolem)
                                && !(entity1 instanceof AbstractMinecart)
                                && !this.isVehicle()
                                && !entity1.isPassenger())
                        {
                            entity1.startRiding(this);
                        }
                        else
                        {
                            entity1.push(this);
                        }
                    }
                }
            }
            else
            {
                for (Entity entity : this.level().getEntities(this, this.getBoundingBox().inflate(0.2F, 0.0, 0.2F)))
                {
                    if (!this.hasPassenger(entity) && entity.isPushable() && entity instanceof AbstractMinecart)
                    {
                        entity.push(this);
                    }
                }
            }

            this.updateInWaterStateAndDoFluidPushing();

            if (this.isInLava())
            {
                this.lavaHurt();
                this.fallDistance *= 0.5F;
            }

            this.firstTick = false;
        }
    }

    protected double getMaxSpeed()
    {
        return (this.isInWater() ? 4.0 : 8.0) / 20.0;
    }

    public void activateMinecart(int p_38111_, int p_38112_, int p_38113_, boolean p_38114_)
    {
    }

    protected void comeOffTrack()
    {
        double d0 = this.getMaxSpeed();
        Vec3 vec3 = this.getDeltaMovement();
        this.setDeltaMovement(Mth.clamp(vec3.x, -d0, d0), vec3.y, Mth.clamp(vec3.z, -d0, d0));

        if (this.onGround())
        {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());

        if (!this.onGround())
        {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.95));
        }
    }

    protected void moveAlongTrack(BlockPos p_38156_, BlockState p_38157_)
    {
        this.resetFallDistance();
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        Vec3 vec3 = this.getPos(d0, d1, d2);
        d1 = (double)p_38156_.getY();
        boolean flag = false;
        boolean flag1 = false;

        if (p_38157_.is(Blocks.POWERED_RAIL))
        {
            flag = p_38157_.getValue(PoweredRailBlock.POWERED);
            flag1 = !flag;
        }

        double d3 = 0.0078125;

        if (this.isInWater())
        {
            d3 *= 0.2;
        }

        Vec3 vec31 = this.getDeltaMovement();
        RailShape railshape = p_38157_.getValue(((BaseRailBlock)p_38157_.getBlock()).getShapeProperty());

        switch (railshape)
        {
            case ASCENDING_EAST:
                this.setDeltaMovement(vec31.add(-d3, 0.0, 0.0));
                d1++;
                break;

            case ASCENDING_WEST:
                this.setDeltaMovement(vec31.add(d3, 0.0, 0.0));
                d1++;
                break;

            case ASCENDING_NORTH:
                this.setDeltaMovement(vec31.add(0.0, 0.0, d3));
                d1++;
                break;

            case ASCENDING_SOUTH:
                this.setDeltaMovement(vec31.add(0.0, 0.0, -d3));
                d1++;
        }

        vec31 = this.getDeltaMovement();
        Pair<Vec3i, Vec3i> pair = exits(railshape);
        Vec3i vec3i = pair.getFirst();
        Vec3i vec3i1 = pair.getSecond();
        double d4 = (double)(vec3i1.getX() - vec3i.getX());
        double d5 = (double)(vec3i1.getZ() - vec3i.getZ());
        double d6 = Math.sqrt(d4 * d4 + d5 * d5);
        double d7 = vec31.x * d4 + vec31.z * d5;

        if (d7 < 0.0)
        {
            d4 = -d4;
            d5 = -d5;
        }

        double d8 = Math.min(2.0, vec31.horizontalDistance());
        vec31 = new Vec3(d8 * d4 / d6, vec31.y, d8 * d5 / d6);
        this.setDeltaMovement(vec31);
        Entity entity = this.getFirstPassenger();

        if (entity instanceof Player)
        {
            Vec3 vec32 = entity.getDeltaMovement();
            double d9 = vec32.horizontalDistanceSqr();
            double d11 = this.getDeltaMovement().horizontalDistanceSqr();

            if (d9 > 1.0E-4 && d11 < 0.01)
            {
                this.setDeltaMovement(this.getDeltaMovement().add(vec32.x * 0.1, 0.0, vec32.z * 0.1));
                flag1 = false;
            }
        }

        if (flag1)
        {
            double d22 = this.getDeltaMovement().horizontalDistance();

            if (d22 < 0.03)
            {
                this.setDeltaMovement(Vec3.ZERO);
            }
            else
            {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.0, 0.5));
            }
        }

        double d23 = (double)p_38156_.getX() + 0.5 + (double)vec3i.getX() * 0.5;
        double d10 = (double)p_38156_.getZ() + 0.5 + (double)vec3i.getZ() * 0.5;
        double d12 = (double)p_38156_.getX() + 0.5 + (double)vec3i1.getX() * 0.5;
        double d13 = (double)p_38156_.getZ() + 0.5 + (double)vec3i1.getZ() * 0.5;
        d4 = d12 - d23;
        d5 = d13 - d10;
        double d14;

        if (d4 == 0.0)
        {
            d14 = d2 - (double)p_38156_.getZ();
        }
        else if (d5 == 0.0)
        {
            d14 = d0 - (double)p_38156_.getX();
        }
        else
        {
            double d15 = d0 - d23;
            double d16 = d2 - d10;
            d14 = (d15 * d4 + d16 * d5) * 2.0;
        }

        d0 = d23 + d4 * d14;
        d2 = d10 + d5 * d14;
        this.setPos(d0, d1, d2);
        double d24 = this.isVehicle() ? 0.75 : 1.0;
        double d25 = this.getMaxSpeed();
        vec31 = this.getDeltaMovement();
        this.move(MoverType.SELF, new Vec3(Mth.clamp(d24 * vec31.x, -d25, d25), 0.0, Mth.clamp(d24 * vec31.z, -d25, d25)));

        if (vec3i.getY() != 0
                && Mth.floor(this.getX()) - p_38156_.getX() == vec3i.getX()
                && Mth.floor(this.getZ()) - p_38156_.getZ() == vec3i.getZ())
        {
            this.setPos(this.getX(), this.getY() + (double)vec3i.getY(), this.getZ());
        }
        else if (vec3i1.getY() != 0
                 && Mth.floor(this.getX()) - p_38156_.getX() == vec3i1.getX()
                 && Mth.floor(this.getZ()) - p_38156_.getZ() == vec3i1.getZ())
        {
            this.setPos(this.getX(), this.getY() + (double)vec3i1.getY(), this.getZ());
        }

        this.applyNaturalSlowdown();
        Vec3 vec33 = this.getPos(this.getX(), this.getY(), this.getZ());

        if (vec33 != null && vec3 != null)
        {
            double d17 = (vec3.y - vec33.y) * 0.05;
            Vec3 vec34 = this.getDeltaMovement();
            double d18 = vec34.horizontalDistance();

            if (d18 > 0.0)
            {
                this.setDeltaMovement(vec34.multiply((d18 + d17) / d18, 1.0, (d18 + d17) / d18));
            }

            this.setPos(this.getX(), vec33.y, this.getZ());
        }

        int j = Mth.floor(this.getX());
        int i = Mth.floor(this.getZ());

        if (j != p_38156_.getX() || i != p_38156_.getZ())
        {
            Vec3 vec35 = this.getDeltaMovement();
            double d26 = vec35.horizontalDistance();
            this.setDeltaMovement(d26 * (double)(j - p_38156_.getX()), vec35.y, d26 * (double)(i - p_38156_.getZ()));
        }

        if (flag)
        {
            Vec3 vec36 = this.getDeltaMovement();
            double d27 = vec36.horizontalDistance();

            if (d27 > 0.01)
            {
                double d19 = 0.06;
                this.setDeltaMovement(vec36.add(vec36.x / d27 * 0.06, 0.0, vec36.z / d27 * 0.06));
            }
            else
            {
                Vec3 vec37 = this.getDeltaMovement();
                double d20 = vec37.x;
                double d21 = vec37.z;

                if (railshape == RailShape.EAST_WEST)
                {
                    if (this.isRedstoneConductor(p_38156_.west()))
                    {
                        d20 = 0.02;
                    }
                    else if (this.isRedstoneConductor(p_38156_.east()))
                    {
                        d20 = -0.02;
                    }
                }
                else
                {
                    if (railshape != RailShape.NORTH_SOUTH)
                    {
                        return;
                    }

                    if (this.isRedstoneConductor(p_38156_.north()))
                    {
                        d21 = 0.02;
                    }
                    else if (this.isRedstoneConductor(p_38156_.south()))
                    {
                        d21 = -0.02;
                    }
                }

                this.setDeltaMovement(d20, vec37.y, d21);
            }
        }
    }

    @Override
    public boolean isOnRails()
    {
        return this.onRails;
    }

    private boolean isRedstoneConductor(BlockPos p_38130_)
    {
        return this.level().getBlockState(p_38130_).isRedstoneConductor(this.level(), p_38130_);
    }

    protected void applyNaturalSlowdown()
    {
        double d0 = this.isVehicle() ? 0.997 : 0.96;
        Vec3 vec3 = this.getDeltaMovement();
        vec3 = vec3.multiply(d0, 0.0, d0);

        if (this.isInWater())
        {
            vec3 = vec3.scale(0.95F);
        }

        this.setDeltaMovement(vec3);
    }

    @Nullable
    public Vec3 getPosOffs(double p_38097_, double p_38098_, double p_38099_, double p_38100_)
    {
        int i = Mth.floor(p_38097_);
        int j = Mth.floor(p_38098_);
        int k = Mth.floor(p_38099_);

        if (this.level().getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS))
        {
            j--;
        }

        BlockState blockstate = this.level().getBlockState(new BlockPos(i, j, k));

        if (BaseRailBlock.isRail(blockstate))
        {
            RailShape railshape = blockstate.getValue(((BaseRailBlock)blockstate.getBlock()).getShapeProperty());
            p_38098_ = (double)j;

            if (railshape.isAscending())
            {
                p_38098_ = (double)(j + 1);
            }

            Pair<Vec3i, Vec3i> pair = exits(railshape);
            Vec3i vec3i = pair.getFirst();
            Vec3i vec3i1 = pair.getSecond();
            double d0 = (double)(vec3i1.getX() - vec3i.getX());
            double d1 = (double)(vec3i1.getZ() - vec3i.getZ());
            double d2 = Math.sqrt(d0 * d0 + d1 * d1);
            d0 /= d2;
            d1 /= d2;
            p_38097_ += d0 * p_38100_;
            p_38099_ += d1 * p_38100_;

            if (vec3i.getY() != 0 && Mth.floor(p_38097_) - i == vec3i.getX() && Mth.floor(p_38099_) - k == vec3i.getZ())
            {
                p_38098_ += (double)vec3i.getY();
            }
            else if (vec3i1.getY() != 0 && Mth.floor(p_38097_) - i == vec3i1.getX() && Mth.floor(p_38099_) - k == vec3i1.getZ())
            {
                p_38098_ += (double)vec3i1.getY();
            }

            return this.getPos(p_38097_, p_38098_, p_38099_);
        }
        else
        {
            return null;
        }
    }

    @Nullable
    public Vec3 getPos(double p_38180_, double p_38181_, double p_38182_)
    {
        int i = Mth.floor(p_38180_);
        int j = Mth.floor(p_38181_);
        int k = Mth.floor(p_38182_);

        if (this.level().getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS))
        {
            j--;
        }

        BlockState blockstate = this.level().getBlockState(new BlockPos(i, j, k));

        if (BaseRailBlock.isRail(blockstate))
        {
            RailShape railshape = blockstate.getValue(((BaseRailBlock)blockstate.getBlock()).getShapeProperty());
            Pair<Vec3i, Vec3i> pair = exits(railshape);
            Vec3i vec3i = pair.getFirst();
            Vec3i vec3i1 = pair.getSecond();
            double d0 = (double)i + 0.5 + (double)vec3i.getX() * 0.5;
            double d1 = (double)j + 0.0625 + (double)vec3i.getY() * 0.5;
            double d2 = (double)k + 0.5 + (double)vec3i.getZ() * 0.5;
            double d3 = (double)i + 0.5 + (double)vec3i1.getX() * 0.5;
            double d4 = (double)j + 0.0625 + (double)vec3i1.getY() * 0.5;
            double d5 = (double)k + 0.5 + (double)vec3i1.getZ() * 0.5;
            double d6 = d3 - d0;
            double d7 = (d4 - d1) * 2.0;
            double d8 = d5 - d2;
            double d9;

            if (d6 == 0.0)
            {
                d9 = p_38182_ - (double)k;
            }
            else if (d8 == 0.0)
            {
                d9 = p_38180_ - (double)i;
            }
            else
            {
                double d10 = p_38180_ - d0;
                double d11 = p_38182_ - d2;
                d9 = (d10 * d6 + d11 * d8) * 2.0;
            }

            p_38180_ = d0 + d6 * d9;
            p_38181_ = d1 + d7 * d9;
            p_38182_ = d2 + d8 * d9;

            if (d7 < 0.0)
            {
                p_38181_++;
            }
            else if (d7 > 0.0)
            {
                p_38181_ += 0.5;
            }

            return new Vec3(p_38180_, p_38181_, p_38182_);
        }
        else
        {
            return null;
        }
    }

    @Override
    public AABB getBoundingBoxForCulling()
    {
        AABB aabb = this.getBoundingBox();
        return this.hasCustomDisplay() ? aabb.inflate((double)Math.abs(this.getDisplayOffset()) / 16.0) : aabb;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag p_38137_)
    {
        if (p_38137_.getBoolean("CustomDisplayTile"))
        {
            this.setDisplayBlockState(NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), p_38137_.getCompound("DisplayState")));
            this.setDisplayOffset(p_38137_.getInt("DisplayOffset"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag p_38151_)
    {
        if (this.hasCustomDisplay())
        {
            p_38151_.putBoolean("CustomDisplayTile", true);
            p_38151_.put("DisplayState", NbtUtils.writeBlockState(this.getDisplayBlockState()));
            p_38151_.putInt("DisplayOffset", this.getDisplayOffset());
        }
    }

    @Override
    public void push(Entity p_38165_)
    {
        if (!this.level().isClientSide)
        {
            if (!p_38165_.noPhysics && !this.noPhysics)
            {
                if (!this.hasPassenger(p_38165_))
                {
                    double d0 = p_38165_.getX() - this.getX();
                    double d1 = p_38165_.getZ() - this.getZ();
                    double d2 = d0 * d0 + d1 * d1;

                    if (d2 >= 1.0E-4F)
                    {
                        d2 = Math.sqrt(d2);
                        d0 /= d2;
                        d1 /= d2;
                        double d3 = 1.0 / d2;

                        if (d3 > 1.0)
                        {
                            d3 = 1.0;
                        }

                        d0 *= d3;
                        d1 *= d3;
                        d0 *= 0.1F;
                        d1 *= 0.1F;
                        d0 *= 0.5;
                        d1 *= 0.5;

                        if (p_38165_ instanceof AbstractMinecart)
                        {
                            double d4 = p_38165_.getX() - this.getX();
                            double d5 = p_38165_.getZ() - this.getZ();
                            Vec3 vec3 = new Vec3(d4, 0.0, d5).normalize();
                            Vec3 vec31 = new Vec3(
                                (double)Mth.cos(this.getYRot() * (float)(Math.PI / 180.0)),
                                0.0,
                                (double)Mth.sin(this.getYRot() * (float)(Math.PI / 180.0))
                            )
                            .normalize();
                            double d6 = Math.abs(vec3.dot(vec31));

                            if (d6 < 0.8F)
                            {
                                return;
                            }

                            Vec3 vec32 = this.getDeltaMovement();
                            Vec3 vec33 = p_38165_.getDeltaMovement();

                            if (((AbstractMinecart)p_38165_).getMinecartType() == AbstractMinecart.Type.FURNACE && this.getMinecartType() != AbstractMinecart.Type.FURNACE)
                            {
                                this.setDeltaMovement(vec32.multiply(0.2, 1.0, 0.2));
                                this.push(vec33.x - d0, 0.0, vec33.z - d1);
                                p_38165_.setDeltaMovement(vec33.multiply(0.95, 1.0, 0.95));
                            }
                            else if (((AbstractMinecart)p_38165_).getMinecartType() != AbstractMinecart.Type.FURNACE
                                     && this.getMinecartType() == AbstractMinecart.Type.FURNACE)
                            {
                                p_38165_.setDeltaMovement(vec33.multiply(0.2, 1.0, 0.2));
                                p_38165_.push(vec32.x + d0, 0.0, vec32.z + d1);
                                this.setDeltaMovement(vec32.multiply(0.95, 1.0, 0.95));
                            }
                            else
                            {
                                double d7 = (vec33.x + vec32.x) / 2.0;
                                double d8 = (vec33.z + vec32.z) / 2.0;
                                this.setDeltaMovement(vec32.multiply(0.2, 1.0, 0.2));
                                this.push(d7 - d0, 0.0, d8 - d1);
                                p_38165_.setDeltaMovement(vec33.multiply(0.2, 1.0, 0.2));
                                p_38165_.push(d7 + d0, 0.0, d8 + d1);
                            }
                        }
                        else
                        {
                            this.push(-d0, 0.0, -d1);
                            p_38165_.push(d0 / 4.0, 0.0, d1 / 4.0);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void lerpTo(double p_38102_, double p_38103_, double p_38104_, float p_38105_, float p_38106_, int p_38107_)
    {
        this.lerpX = p_38102_;
        this.lerpY = p_38103_;
        this.lerpZ = p_38104_;
        this.lerpYRot = (double)p_38105_;
        this.lerpXRot = (double)p_38106_;
        this.lerpSteps = p_38107_ + 2;
        this.setDeltaMovement(this.targetDeltaMovement);
    }

    @Override
    public double lerpTargetX()
    {
        return this.lerpSteps > 0 ? this.lerpX : this.getX();
    }

    @Override
    public double lerpTargetY()
    {
        return this.lerpSteps > 0 ? this.lerpY : this.getY();
    }

    @Override
    public double lerpTargetZ()
    {
        return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
    }

    @Override
    public float lerpTargetXRot()
    {
        return this.lerpSteps > 0 ? (float)this.lerpXRot : this.getXRot();
    }

    @Override
    public float lerpTargetYRot()
    {
        return this.lerpSteps > 0 ? (float)this.lerpYRot : this.getYRot();
    }

    @Override
    public void lerpMotion(double p_38171_, double p_38172_, double p_38173_)
    {
        this.targetDeltaMovement = new Vec3(p_38171_, p_38172_, p_38173_);
        this.setDeltaMovement(this.targetDeltaMovement);
    }

    public abstract AbstractMinecart.Type getMinecartType();

    public BlockState getDisplayBlockState()
    {
        return !this.hasCustomDisplay() ? this.getDefaultDisplayBlockState() : Block.stateById(this.getEntityData().get(DATA_ID_DISPLAY_BLOCK));
    }

    public BlockState getDefaultDisplayBlockState()
    {
        return Blocks.AIR.defaultBlockState();
    }

    public int getDisplayOffset()
    {
        return !this.hasCustomDisplay() ? this.getDefaultDisplayOffset() : this.getEntityData().get(DATA_ID_DISPLAY_OFFSET);
    }

    public int getDefaultDisplayOffset()
    {
        return 6;
    }

    public void setDisplayBlockState(BlockState p_38147_)
    {
        this.getEntityData().set(DATA_ID_DISPLAY_BLOCK, Block.getId(p_38147_));
        this.setCustomDisplay(true);
    }

    public void setDisplayOffset(int p_38175_)
    {
        this.getEntityData().set(DATA_ID_DISPLAY_OFFSET, p_38175_);
        this.setCustomDisplay(true);
    }

    public boolean hasCustomDisplay()
    {
        return this.getEntityData().get(DATA_ID_CUSTOM_DISPLAY);
    }

    public void setCustomDisplay(boolean p_38139_)
    {
        this.getEntityData().set(DATA_ID_CUSTOM_DISPLAY, p_38139_);
    }

    @Override
    public ItemStack getPickResult()
    {

        return new ItemStack(switch (this.getMinecartType())
    {
        case CHEST -> Items.CHEST_MINECART;

        case FURNACE -> Items.FURNACE_MINECART;

        case TNT -> Items.TNT_MINECART;

        default -> Items.MINECART;

        case HOPPER -> Items.HOPPER_MINECART;

        case COMMAND_BLOCK -> Items.COMMAND_BLOCK_MINECART;
    });
    }

    public static enum Type
    {
        RIDEABLE,
        CHEST,
        FURNACE,
        TNT,
        SPAWNER,
        HOPPER,
        COMMAND_BLOCK;
    }
}
