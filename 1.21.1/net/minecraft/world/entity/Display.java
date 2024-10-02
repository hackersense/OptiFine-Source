package net.minecraft.world.entity;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Brightness;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

public abstract class Display extends Entity
{
    static final Logger LOGGER = LogUtils.getLogger();
    public static final int NO_BRIGHTNESS_OVERRIDE = -1;
    private static final EntityDataAccessor<Integer> DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_POS_ROT_INTERPOLATION_DURATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_TRANSLATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> DATA_SCALE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Quaternionf> DATA_LEFT_ROTATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Quaternionf> DATA_RIGHT_ROTATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Byte> DATA_BILLBOARD_RENDER_CONSTRAINTS_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_BRIGHTNESS_OVERRIDE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_VIEW_RANGE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SHADOW_RADIUS_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SHADOW_STRENGTH_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WIDTH_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_GLOW_COLOR_OVERRIDE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final IntSet RENDER_STATE_IDS = IntSet.of(
            DATA_TRANSLATION_ID.id(),
            DATA_SCALE_ID.id(),
            DATA_LEFT_ROTATION_ID.id(),
            DATA_RIGHT_ROTATION_ID.id(),
            DATA_BILLBOARD_RENDER_CONSTRAINTS_ID.id(),
            DATA_BRIGHTNESS_OVERRIDE_ID.id(),
            DATA_SHADOW_RADIUS_ID.id(),
            DATA_SHADOW_STRENGTH_ID.id()
                                            );
    private static final float INITIAL_SHADOW_RADIUS = 0.0F;
    private static final float INITIAL_SHADOW_STRENGTH = 1.0F;
    private static final int NO_GLOW_COLOR_OVERRIDE = -1;
    public static final String TAG_POS_ROT_INTERPOLATION_DURATION = "teleport_duration";
    public static final String TAG_TRANSFORMATION_INTERPOLATION_DURATION = "interpolation_duration";
    public static final String TAG_TRANSFORMATION_START_INTERPOLATION = "start_interpolation";
    public static final String TAG_TRANSFORMATION = "transformation";
    public static final String TAG_BILLBOARD = "billboard";
    public static final String TAG_BRIGHTNESS = "brightness";
    public static final String TAG_VIEW_RANGE = "view_range";
    public static final String TAG_SHADOW_RADIUS = "shadow_radius";
    public static final String TAG_SHADOW_STRENGTH = "shadow_strength";
    public static final String TAG_WIDTH = "width";
    public static final String TAG_HEIGHT = "height";
    public static final String TAG_GLOW_COLOR_OVERRIDE = "glow_color_override";
    private long interpolationStartClientTick = -2147483648L;
    private int interpolationDuration;
    private float lastProgress;
    private AABB cullingBoundingBox;
    protected boolean updateRenderState;
    private boolean updateStartTick;
    private boolean updateInterpolationDuration;
    @Nullable
    private Display.RenderState renderState;
    @Nullable
    private Display.PosRotInterpolationTarget posRotInterpolationTarget;

    public Display(EntityType<?> p_270360_, Level p_270280_)
    {
        super(p_270360_, p_270280_);
        this.noPhysics = true;
        this.noCulling = true;
        this.cullingBoundingBox = this.getBoundingBox();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> p_270275_)
    {
        super.onSyncedDataUpdated(p_270275_);

        if (DATA_HEIGHT_ID.equals(p_270275_) || DATA_WIDTH_ID.equals(p_270275_))
        {
            this.updateCulling();
        }

        if (DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID.equals(p_270275_))
        {
            this.updateStartTick = true;
        }

        if (DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID.equals(p_270275_))
        {
            this.updateInterpolationDuration = true;
        }

        if (RENDER_STATE_IDS.contains(p_270275_.id()))
        {
            this.updateRenderState = true;
        }
    }

    private static Transformation createTransformation(SynchedEntityData p_270278_)
    {
        Vector3f vector3f = p_270278_.get(DATA_TRANSLATION_ID);
        Quaternionf quaternionf = p_270278_.get(DATA_LEFT_ROTATION_ID);
        Vector3f vector3f1 = p_270278_.get(DATA_SCALE_ID);
        Quaternionf quaternionf1 = p_270278_.get(DATA_RIGHT_ROTATION_ID);
        return new Transformation(vector3f, quaternionf, vector3f1, quaternionf1);
    }

    @Override
    public void tick()
    {
        Entity entity = this.getVehicle();

        if (entity != null && entity.isRemoved())
        {
            this.stopRiding();
        }

        if (this.level().isClientSide)
        {
            if (this.updateStartTick)
            {
                this.updateStartTick = false;
                int i = this.getTransformationInterpolationDelay();
                this.interpolationStartClientTick = (long)(this.tickCount + i);
            }

            if (this.updateInterpolationDuration)
            {
                this.updateInterpolationDuration = false;
                this.interpolationDuration = this.getTransformationInterpolationDuration();
            }

            if (this.updateRenderState)
            {
                this.updateRenderState = false;
                boolean flag = this.interpolationDuration != 0;

                if (flag && this.renderState != null)
                {
                    this.renderState = this.createInterpolatedRenderState(this.renderState, this.lastProgress);
                }
                else
                {
                    this.renderState = this.createFreshRenderState();
                }

                this.updateRenderSubState(flag, this.lastProgress);
            }

            if (this.posRotInterpolationTarget != null)
            {
                if (this.posRotInterpolationTarget.steps == 0)
                {
                    this.posRotInterpolationTarget.applyTargetPosAndRot(this);
                    this.setOldPosAndRot();
                    this.posRotInterpolationTarget = null;
                }
                else
                {
                    this.posRotInterpolationTarget.applyLerpStep(this);
                    this.posRotInterpolationTarget.steps--;

                    if (this.posRotInterpolationTarget.steps == 0)
                    {
                        this.posRotInterpolationTarget = null;
                    }
                }
            }
        }
    }

    protected abstract void updateRenderSubState(boolean p_277603_, float p_277810_);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_327982_)
    {
        p_327982_.define(DATA_POS_ROT_INTERPOLATION_DURATION_ID, 0);
        p_327982_.define(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, 0);
        p_327982_.define(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, 0);
        p_327982_.define(DATA_TRANSLATION_ID, new Vector3f());
        p_327982_.define(DATA_SCALE_ID, new Vector3f(1.0F, 1.0F, 1.0F));
        p_327982_.define(DATA_RIGHT_ROTATION_ID, new Quaternionf());
        p_327982_.define(DATA_LEFT_ROTATION_ID, new Quaternionf());
        p_327982_.define(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID, Display.BillboardConstraints.FIXED.getId());
        p_327982_.define(DATA_BRIGHTNESS_OVERRIDE_ID, -1);
        p_327982_.define(DATA_VIEW_RANGE_ID, 1.0F);
        p_327982_.define(DATA_SHADOW_RADIUS_ID, 0.0F);
        p_327982_.define(DATA_SHADOW_STRENGTH_ID, 1.0F);
        p_327982_.define(DATA_WIDTH_ID, 0.0F);
        p_327982_.define(DATA_HEIGHT_ID, 0.0F);
        p_327982_.define(DATA_GLOW_COLOR_OVERRIDE_ID, -1);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag p_270854_)
    {
        if (p_270854_.contains("transformation"))
        {
            Transformation.EXTENDED_CODEC
            .decode(NbtOps.INSTANCE, p_270854_.get("transformation"))
            .resultOrPartial(Util.prefix("Display entity", LOGGER::error))
            .ifPresent(p_270952_ -> this.setTransformation(p_270952_.getFirst()));
        }

        if (p_270854_.contains("interpolation_duration", 99))
        {
            int i = p_270854_.getInt("interpolation_duration");
            this.setTransformationInterpolationDuration(i);
        }

        if (p_270854_.contains("start_interpolation", 99))
        {
            int j = p_270854_.getInt("start_interpolation");
            this.setTransformationInterpolationDelay(j);
        }

        if (p_270854_.contains("teleport_duration", 99))
        {
            int k = p_270854_.getInt("teleport_duration");
            this.setPosRotInterpolationDuration(Mth.clamp(k, 0, 59));
        }

        if (p_270854_.contains("billboard", 8))
        {
            Display.BillboardConstraints.CODEC
            .decode(NbtOps.INSTANCE, p_270854_.get("billboard"))
            .resultOrPartial(Util.prefix("Display entity", LOGGER::error))
            .ifPresent(p_270691_ -> this.setBillboardConstraints(p_270691_.getFirst()));
        }

        if (p_270854_.contains("view_range", 99))
        {
            this.setViewRange(p_270854_.getFloat("view_range"));
        }

        if (p_270854_.contains("shadow_radius", 99))
        {
            this.setShadowRadius(p_270854_.getFloat("shadow_radius"));
        }

        if (p_270854_.contains("shadow_strength", 99))
        {
            this.setShadowStrength(p_270854_.getFloat("shadow_strength"));
        }

        if (p_270854_.contains("width", 99))
        {
            this.setWidth(p_270854_.getFloat("width"));
        }

        if (p_270854_.contains("height", 99))
        {
            this.setHeight(p_270854_.getFloat("height"));
        }

        if (p_270854_.contains("glow_color_override", 99))
        {
            this.setGlowColorOverride(p_270854_.getInt("glow_color_override"));
        }

        if (p_270854_.contains("brightness", 10))
        {
            Brightness.CODEC
            .decode(NbtOps.INSTANCE, p_270854_.get("brightness"))
            .resultOrPartial(Util.prefix("Display entity", LOGGER::error))
            .ifPresent(p_270247_ -> this.setBrightnessOverride(p_270247_.getFirst()));
        }
        else
        {
            this.setBrightnessOverride(null);
        }
    }

    private void setTransformation(Transformation p_270186_)
    {
        this.entityData.set(DATA_TRANSLATION_ID, p_270186_.getTranslation());
        this.entityData.set(DATA_LEFT_ROTATION_ID, p_270186_.getLeftRotation());
        this.entityData.set(DATA_SCALE_ID, p_270186_.getScale());
        this.entityData.set(DATA_RIGHT_ROTATION_ID, p_270186_.getRightRotation());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag p_270779_)
    {
        Transformation.EXTENDED_CODEC
        .encodeStart(NbtOps.INSTANCE, createTransformation(this.entityData))
        .ifSuccess(p_270528_ -> p_270779_.put("transformation", p_270528_));
        Display.BillboardConstraints.CODEC
        .encodeStart(NbtOps.INSTANCE, this.getBillboardConstraints())
        .ifSuccess(p_270227_ -> p_270779_.put("billboard", p_270227_));
        p_270779_.putInt("interpolation_duration", this.getTransformationInterpolationDuration());
        p_270779_.putInt("teleport_duration", this.getPosRotInterpolationDuration());
        p_270779_.putFloat("view_range", this.getViewRange());
        p_270779_.putFloat("shadow_radius", this.getShadowRadius());
        p_270779_.putFloat("shadow_strength", this.getShadowStrength());
        p_270779_.putFloat("width", this.getWidth());
        p_270779_.putFloat("height", this.getHeight());
        p_270779_.putInt("glow_color_override", this.getGlowColorOverride());
        Brightness brightness = this.getBrightnessOverride();

        if (brightness != null)
        {
            Brightness.CODEC.encodeStart(NbtOps.INSTANCE, brightness).ifSuccess(p_270121_ -> p_270779_.put("brightness", p_270121_));
        }
    }

    @Override
    public void lerpTo(double p_297677_, double p_301293_, double p_301384_, float p_300635_, float p_299108_, int p_299659_)
    {
        int i = this.getPosRotInterpolationDuration();
        this.posRotInterpolationTarget = new Display.PosRotInterpolationTarget(i, p_297677_, p_301293_, p_301384_, (double)p_300635_, (double)p_299108_);
    }

    @Override
    public double lerpTargetX()
    {
        return this.posRotInterpolationTarget != null ? this.posRotInterpolationTarget.targetX : this.getX();
    }

    @Override
    public double lerpTargetY()
    {
        return this.posRotInterpolationTarget != null ? this.posRotInterpolationTarget.targetY : this.getY();
    }

    @Override
    public double lerpTargetZ()
    {
        return this.posRotInterpolationTarget != null ? this.posRotInterpolationTarget.targetZ : this.getZ();
    }

    @Override
    public float lerpTargetXRot()
    {
        return this.posRotInterpolationTarget != null ? (float)this.posRotInterpolationTarget.targetXRot : this.getXRot();
    }

    @Override
    public float lerpTargetYRot()
    {
        return this.posRotInterpolationTarget != null ? (float)this.posRotInterpolationTarget.targetYRot : this.getYRot();
    }

    @Override
    public AABB getBoundingBoxForCulling()
    {
        return this.cullingBoundingBox;
    }

    @Override
    public PushReaction getPistonPushReaction()
    {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isIgnoringBlockTriggers()
    {
        return true;
    }

    @Nullable
    public Display.RenderState renderState()
    {
        return this.renderState;
    }

    private void setTransformationInterpolationDuration(int p_297488_)
    {
        this.entityData.set(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, p_297488_);
    }

    private int getTransformationInterpolationDuration()
    {
        return this.entityData.get(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID);
    }

    private void setTransformationInterpolationDelay(int p_300640_)
    {
        this.entityData.set(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, p_300640_, true);
    }

    private int getTransformationInterpolationDelay()
    {
        return this.entityData.get(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID);
    }

    private void setPosRotInterpolationDuration(int p_300107_)
    {
        this.entityData.set(DATA_POS_ROT_INTERPOLATION_DURATION_ID, p_300107_);
    }

    private int getPosRotInterpolationDuration()
    {
        return this.entityData.get(DATA_POS_ROT_INTERPOLATION_DURATION_ID);
    }

    private void setBillboardConstraints(Display.BillboardConstraints p_270345_)
    {
        this.entityData.set(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID, p_270345_.getId());
    }

    private Display.BillboardConstraints getBillboardConstraints()
    {
        return Display.BillboardConstraints.BY_ID.apply(this.entityData.get(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID));
    }

    private void setBrightnessOverride(@Nullable Brightness p_270461_)
    {
        this.entityData.set(DATA_BRIGHTNESS_OVERRIDE_ID, p_270461_ != null ? p_270461_.pack() : -1);
    }

    @Nullable
    private Brightness getBrightnessOverride()
    {
        int i = this.entityData.get(DATA_BRIGHTNESS_OVERRIDE_ID);
        return i != -1 ? Brightness.unpack(i) : null;
    }

    private int getPackedBrightnessOverride()
    {
        return this.entityData.get(DATA_BRIGHTNESS_OVERRIDE_ID);
    }

    private void setViewRange(float p_270907_)
    {
        this.entityData.set(DATA_VIEW_RANGE_ID, p_270907_);
    }

    private float getViewRange()
    {
        return this.entityData.get(DATA_VIEW_RANGE_ID);
    }

    private void setShadowRadius(float p_270122_)
    {
        this.entityData.set(DATA_SHADOW_RADIUS_ID, p_270122_);
    }

    private float getShadowRadius()
    {
        return this.entityData.get(DATA_SHADOW_RADIUS_ID);
    }

    private void setShadowStrength(float p_270866_)
    {
        this.entityData.set(DATA_SHADOW_STRENGTH_ID, p_270866_);
    }

    private float getShadowStrength()
    {
        return this.entityData.get(DATA_SHADOW_STRENGTH_ID);
    }

    private void setWidth(float p_270741_)
    {
        this.entityData.set(DATA_WIDTH_ID, p_270741_);
    }

    private float getWidth()
    {
        return this.entityData.get(DATA_WIDTH_ID);
    }

    private void setHeight(float p_270716_)
    {
        this.entityData.set(DATA_HEIGHT_ID, p_270716_);
    }

    private int getGlowColorOverride()
    {
        return this.entityData.get(DATA_GLOW_COLOR_OVERRIDE_ID);
    }

    private void setGlowColorOverride(int p_270784_)
    {
        this.entityData.set(DATA_GLOW_COLOR_OVERRIDE_ID, p_270784_);
    }

    public float calculateInterpolationProgress(float p_272675_)
    {
        int i = this.interpolationDuration;

        if (i <= 0)
        {
            return 1.0F;
        }
        else
        {
            float f = (float)((long)this.tickCount - this.interpolationStartClientTick);
            float f1 = f + p_272675_;
            float f2 = Mth.clamp(Mth.inverseLerp(f1, 0.0F, (float)i), 0.0F, 1.0F);
            this.lastProgress = f2;
            return f2;
        }
    }

    private float getHeight()
    {
        return this.entityData.get(DATA_HEIGHT_ID);
    }

    @Override
    public void setPos(double p_270091_, double p_270983_, double p_270419_)
    {
        super.setPos(p_270091_, p_270983_, p_270419_);
        this.updateCulling();
    }

    private void updateCulling()
    {
        float f = this.getWidth();
        float f1 = this.getHeight();

        if (f != 0.0F && f1 != 0.0F)
        {
            this.noCulling = false;
            float f2 = f / 2.0F;
            double d0 = this.getX();
            double d1 = this.getY();
            double d2 = this.getZ();
            this.cullingBoundingBox = new AABB(d0 - (double)f2, d1, d2 - (double)f2, d0 + (double)f2, d1 + (double)f1, d2 + (double)f2);
        }
        else
        {
            this.noCulling = true;
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double p_270991_)
    {
        return p_270991_ < Mth.square((double)this.getViewRange() * 64.0 * getViewScale());
    }

    @Override
    public int getTeamColor()
    {
        int i = this.getGlowColorOverride();
        return i != -1 ? i : super.getTeamColor();
    }

    private Display.RenderState createFreshRenderState()
    {
        return new Display.RenderState(
                   Display.GenericInterpolator.constant(createTransformation(this.entityData)),
                   this.getBillboardConstraints(),
                   this.getPackedBrightnessOverride(),
                   Display.FloatInterpolator.constant(this.getShadowRadius()),
                   Display.FloatInterpolator.constant(this.getShadowStrength()),
                   this.getGlowColorOverride()
               );
    }

    private Display.RenderState createInterpolatedRenderState(Display.RenderState p_277365_, float p_277948_)
    {
        Transformation transformation = p_277365_.transformation.get(p_277948_);
        float f = p_277365_.shadowRadius.get(p_277948_);
        float f1 = p_277365_.shadowStrength.get(p_277948_);
        return new Display.RenderState(
                   new Display.TransformationInterpolator(transformation, createTransformation(this.entityData)),
                   this.getBillboardConstraints(),
                   this.getPackedBrightnessOverride(),
                   new Display.LinearFloatInterpolator(f, this.getShadowRadius()),
                   new Display.LinearFloatInterpolator(f1, this.getShadowStrength()),
                   this.getGlowColorOverride()
               );
    }

    public static enum BillboardConstraints implements StringRepresentable
    {
        FIXED((byte)0, "fixed"),
        VERTICAL((byte)1, "vertical"),
        HORIZONTAL((byte)2, "horizontal"),
        CENTER((byte)3, "center");

        public static final Codec<Display.BillboardConstraints> CODEC = StringRepresentable.fromEnum(Display.BillboardConstraints::values);
        public static final IntFunction<Display.BillboardConstraints> BY_ID = ByIdMap.continuous(
            Display.BillboardConstraints::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO
        );
        private final byte id;
        private final String name;

        private BillboardConstraints(final byte p_270785_, final String p_270544_)
        {
            this.name = p_270544_;
            this.id = p_270785_;
        }

        @Override
        public String getSerializedName()
        {
            return this.name;
        }

        byte getId()
        {
            return this.id;
        }
    }

    public static class BlockDisplay extends Display
    {
        public static final String TAG_BLOCK_STATE = "block_state";
        private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE_ID = SynchedEntityData.defineId(Display.BlockDisplay.class, EntityDataSerializers.BLOCK_STATE);
        @Nullable
        private Display.BlockDisplay.BlockRenderState blockRenderState;

        public BlockDisplay(EntityType<?> p_271022_, Level p_270442_)
        {
            super(p_271022_, p_270442_);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder p_329731_)
        {
            super.defineSynchedData(p_329731_);
            p_329731_.define(DATA_BLOCK_STATE_ID, Blocks.AIR.defaultBlockState());
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> p_277476_)
        {
            super.onSyncedDataUpdated(p_277476_);

            if (p_277476_.equals(DATA_BLOCK_STATE_ID))
            {
                this.updateRenderState = true;
            }
        }

        private BlockState getBlockState()
        {
            return this.entityData.get(DATA_BLOCK_STATE_ID);
        }

        private void setBlockState(BlockState p_270267_)
        {
            this.entityData.set(DATA_BLOCK_STATE_ID, p_270267_);
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag p_270139_)
        {
            super.readAdditionalSaveData(p_270139_);
            this.setBlockState(NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), p_270139_.getCompound("block_state")));
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag p_270469_)
        {
            super.addAdditionalSaveData(p_270469_);
            p_270469_.put("block_state", NbtUtils.writeBlockState(this.getBlockState()));
        }

        @Nullable
        public Display.BlockDisplay.BlockRenderState blockRenderState()
        {
            return this.blockRenderState;
        }

        @Override
        protected void updateRenderSubState(boolean p_277802_, float p_277688_)
        {
            this.blockRenderState = new Display.BlockDisplay.BlockRenderState(this.getBlockState());
        }

        public static record BlockRenderState(BlockState blockState)
        {
        }
    }

    static record ColorInterpolator(int previous, int current) implements Display.IntInterpolator
    {
        @Override
        public int get(float p_278012_)
        {
            return FastColor.ARGB32.lerp(p_278012_, this.previous, this.current);
        }
    }

    @FunctionalInterface
    public interface FloatInterpolator
    {
        static Display.FloatInterpolator constant(float p_277894_)
        {
            return p_278040_ -> p_277894_;
        }

        float get(float p_270330_);
    }

    @FunctionalInterface
    public interface GenericInterpolator<T>
    {
        static <T> Display.GenericInterpolator<T> constant(T p_277718_)
        {
            return p_277907_ -> p_277718_;
        }

        T get(float p_270270_);
    }

    @FunctionalInterface
    public interface IntInterpolator
    {
        static Display.IntInterpolator constant(int p_277348_)
        {
            return p_277356_ -> p_277348_;
        }

        int get(float p_270183_);
    }

    public static class ItemDisplay extends Display
    {
        private static final String TAG_ITEM = "item";
        private static final String TAG_ITEM_DISPLAY = "item_display";
        private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK_ID = SynchedEntityData.defineId(Display.ItemDisplay.class, EntityDataSerializers.ITEM_STACK);
        private static final EntityDataAccessor<Byte> DATA_ITEM_DISPLAY_ID = SynchedEntityData.defineId(Display.ItemDisplay.class, EntityDataSerializers.BYTE);
        private final SlotAccess slot = SlotAccess.of(this::getItemStack, this::setItemStack);
        @Nullable
        private Display.ItemDisplay.ItemRenderState itemRenderState;

        public ItemDisplay(EntityType<?> p_270104_, Level p_270735_)
        {
            super(p_270104_, p_270735_);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder p_332308_)
        {
            super.defineSynchedData(p_332308_);
            p_332308_.define(DATA_ITEM_STACK_ID, ItemStack.EMPTY);
            p_332308_.define(DATA_ITEM_DISPLAY_ID, ItemDisplayContext.NONE.getId());
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> p_277793_)
        {
            super.onSyncedDataUpdated(p_277793_);

            if (DATA_ITEM_STACK_ID.equals(p_277793_) || DATA_ITEM_DISPLAY_ID.equals(p_277793_))
            {
                this.updateRenderState = true;
            }
        }

        private ItemStack getItemStack()
        {
            return this.entityData.get(DATA_ITEM_STACK_ID);
        }

        private void setItemStack(ItemStack p_270310_)
        {
            this.entityData.set(DATA_ITEM_STACK_ID, p_270310_);
        }

        private void setItemTransform(ItemDisplayContext p_270370_)
        {
            this.entityData.set(DATA_ITEM_DISPLAY_ID, p_270370_.getId());
        }

        private ItemDisplayContext getItemTransform()
        {
            return ItemDisplayContext.BY_ID.apply(this.entityData.get(DATA_ITEM_DISPLAY_ID));
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag p_270713_)
        {
            super.readAdditionalSaveData(p_270713_);

            if (p_270713_.contains("item"))
            {
                this.setItemStack(ItemStack.parse(this.registryAccess(), p_270713_.getCompound("item")).orElse(ItemStack.EMPTY));
            }
            else
            {
                this.setItemStack(ItemStack.EMPTY);
            }

            if (p_270713_.contains("item_display", 8))
            {
                ItemDisplayContext.CODEC
                .decode(NbtOps.INSTANCE, p_270713_.get("item_display"))
                .resultOrPartial(Util.prefix("Display entity", Display.LOGGER::error))
                .ifPresent(p_270456_ -> this.setItemTransform(p_270456_.getFirst()));
            }
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag p_270669_)
        {
            super.addAdditionalSaveData(p_270669_);

            if (!this.getItemStack().isEmpty())
            {
                p_270669_.put("item", this.getItemStack().save(this.registryAccess()));
            }

            ItemDisplayContext.CODEC.encodeStart(NbtOps.INSTANCE, this.getItemTransform()).ifSuccess(p_270615_ -> p_270669_.put("item_display", p_270615_));
        }

        @Override
        public SlotAccess getSlot(int p_270599_)
        {
            return p_270599_ == 0 ? this.slot : SlotAccess.NULL;
        }

        @Nullable
        public Display.ItemDisplay.ItemRenderState itemRenderState()
        {
            return this.itemRenderState;
        }

        @Override
        protected void updateRenderSubState(boolean p_277976_, float p_277708_)
        {
            ItemStack itemstack = this.getItemStack();
            itemstack.setEntityRepresentation(this);
            this.itemRenderState = new Display.ItemDisplay.ItemRenderState(itemstack, this.getItemTransform());
        }

        public static record ItemRenderState(ItemStack itemStack, ItemDisplayContext itemTransform)
        {
        }
    }

    static record LinearFloatInterpolator(float previous, float current) implements Display.FloatInterpolator
    {
        @Override
        public float get(float p_277511_)
        {
            return Mth.lerp(p_277511_, this.previous, this.current);
        }
    }

    static record LinearIntInterpolator(int previous, int current) implements Display.IntInterpolator
    {
        @Override
        public int get(float p_277960_)
        {
            return Mth.lerpInt(p_277960_, this.previous, this.current);
        }
    }

    static class PosRotInterpolationTarget
    {
        int steps;
        final double targetX;
        final double targetY;
        final double targetZ;
        final double targetYRot;
        final double targetXRot;

        PosRotInterpolationTarget(int p_297638_, double p_297433_, double p_297414_, double p_300814_, double p_297927_, double p_297784_)
        {
            this.steps = p_297638_;
            this.targetX = p_297433_;
            this.targetY = p_297414_;
            this.targetZ = p_300814_;
            this.targetYRot = p_297927_;
            this.targetXRot = p_297784_;
        }

        void applyTargetPosAndRot(Entity p_297540_)
        {
            p_297540_.setPos(this.targetX, this.targetY, this.targetZ);
            p_297540_.setRot((float)this.targetYRot, (float)this.targetXRot);
        }

        void applyLerpStep(Entity p_300902_)
        {
            p_300902_.lerpPositionAndRotationStep(this.steps, this.targetX, this.targetY, this.targetZ, this.targetYRot, this.targetXRot);
        }
    }

    public static record RenderState(
        Display.GenericInterpolator<Transformation> transformation,
        Display.BillboardConstraints billboardConstraints,
        int brightnessOverride,
        Display.FloatInterpolator shadowRadius,
        Display.FloatInterpolator shadowStrength,
        int glowColorOverride
    )
    {
    }

    public static class TextDisplay extends Display
    {
        public static final String TAG_TEXT = "text";
        private static final String TAG_LINE_WIDTH = "line_width";
        private static final String TAG_TEXT_OPACITY = "text_opacity";
        private static final String TAG_BACKGROUND_COLOR = "background";
        private static final String TAG_SHADOW = "shadow";
        private static final String TAG_SEE_THROUGH = "see_through";
        private static final String TAG_USE_DEFAULT_BACKGROUND = "default_background";
        private static final String TAG_ALIGNMENT = "alignment";
        public static final byte FLAG_SHADOW = 1;
        public static final byte FLAG_SEE_THROUGH = 2;
        public static final byte FLAG_USE_DEFAULT_BACKGROUND = 4;
        public static final byte FLAG_ALIGN_LEFT = 8;
        public static final byte FLAG_ALIGN_RIGHT = 16;
        private static final byte INITIAL_TEXT_OPACITY = -1;
        public static final int INITIAL_BACKGROUND = 1073741824;
        private static final EntityDataAccessor<Component> DATA_TEXT_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.COMPONENT);
        private static final EntityDataAccessor<Integer> DATA_LINE_WIDTH_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Integer> DATA_BACKGROUND_COLOR_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Byte> DATA_TEXT_OPACITY_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.BYTE);
        private static final EntityDataAccessor<Byte> DATA_STYLE_FLAGS_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.BYTE);
        private static final IntSet TEXT_RENDER_STATE_IDS = IntSet.of(
                DATA_TEXT_ID.id(), DATA_LINE_WIDTH_ID.id(), DATA_BACKGROUND_COLOR_ID.id(), DATA_TEXT_OPACITY_ID.id(), DATA_STYLE_FLAGS_ID.id()
                                                );
        @Nullable
        private Display.TextDisplay.CachedInfo clientDisplayCache;
        @Nullable
        private Display.TextDisplay.TextRenderState textRenderState;

        public TextDisplay(EntityType<?> p_270708_, Level p_270736_)
        {
            super(p_270708_, p_270736_);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder p_330653_)
        {
            super.defineSynchedData(p_330653_);
            p_330653_.define(DATA_TEXT_ID, Component.empty());
            p_330653_.define(DATA_LINE_WIDTH_ID, 200);
            p_330653_.define(DATA_BACKGROUND_COLOR_ID, 1073741824);
            p_330653_.define(DATA_TEXT_OPACITY_ID, (byte) - 1);
            p_330653_.define(DATA_STYLE_FLAGS_ID, (byte)0);
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> p_270797_)
        {
            super.onSyncedDataUpdated(p_270797_);

            if (TEXT_RENDER_STATE_IDS.contains(p_270797_.id()))
            {
                this.updateRenderState = true;
            }
        }

        private Component getText()
        {
            return this.entityData.get(DATA_TEXT_ID);
        }

        private void setText(Component p_270902_)
        {
            this.entityData.set(DATA_TEXT_ID, p_270902_);
        }

        private int getLineWidth()
        {
            return this.entityData.get(DATA_LINE_WIDTH_ID);
        }

        private void setLineWidth(int p_270545_)
        {
            this.entityData.set(DATA_LINE_WIDTH_ID, p_270545_);
        }

        private byte getTextOpacity()
        {
            return this.entityData.get(DATA_TEXT_OPACITY_ID);
        }

        private void setTextOpacity(byte p_270583_)
        {
            this.entityData.set(DATA_TEXT_OPACITY_ID, p_270583_);
        }

        private int getBackgroundColor()
        {
            return this.entityData.get(DATA_BACKGROUND_COLOR_ID);
        }

        private void setBackgroundColor(int p_270241_)
        {
            this.entityData.set(DATA_BACKGROUND_COLOR_ID, p_270241_);
        }

        private byte getFlags()
        {
            return this.entityData.get(DATA_STYLE_FLAGS_ID);
        }

        private void setFlags(byte p_270855_)
        {
            this.entityData.set(DATA_STYLE_FLAGS_ID, p_270855_);
        }

        private static byte loadFlag(byte p_270219_, CompoundTag p_270994_, String p_270958_, byte p_270701_)
        {
            return p_270994_.getBoolean(p_270958_) ? (byte)(p_270219_ | p_270701_) : p_270219_;
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag p_270714_)
        {
            super.readAdditionalSaveData(p_270714_);

            if (p_270714_.contains("line_width", 99))
            {
                this.setLineWidth(p_270714_.getInt("line_width"));
            }

            if (p_270714_.contains("text_opacity", 99))
            {
                this.setTextOpacity(p_270714_.getByte("text_opacity"));
            }

            if (p_270714_.contains("background", 99))
            {
                this.setBackgroundColor(p_270714_.getInt("background"));
            }

            byte b0 = loadFlag((byte)0, p_270714_, "shadow", (byte)1);
            b0 = loadFlag(b0, p_270714_, "see_through", (byte)2);
            b0 = loadFlag(b0, p_270714_, "default_background", (byte)4);
            Optional<Display.TextDisplay.Align> optional = Display.TextDisplay.Align.CODEC
                    .decode(NbtOps.INSTANCE, p_270714_.get("alignment"))
                    .resultOrPartial(Util.prefix("Display entity", Display.LOGGER::error))
                    .map(Pair::getFirst);

            if (optional.isPresent())
            {

                b0 = switch ((Display.TextDisplay.Align)optional.get())
                {
                    case CENTER -> b0;

                    case LEFT -> (byte)(b0 | 8);

                    case RIGHT -> (byte)(b0 | 16);
                };
            }

            this.setFlags(b0);

            if (p_270714_.contains("text", 8))
            {
                String s = p_270714_.getString("text");

                try
                {
                    Component component = Component.Serializer.fromJson(s, this.registryAccess());

                    if (component != null)
                    {
                        CommandSourceStack commandsourcestack = this.createCommandSourceStack().withPermission(2);
                        Component component1 = ComponentUtils.updateForEntity(commandsourcestack, component, this, 0);
                        this.setText(component1);
                    }
                    else
                    {
                        this.setText(Component.empty());
                    }
                }
                catch (Exception exception)
                {
                    Display.LOGGER.warn("Failed to parse display entity text {}", s, exception);
                }
            }
        }

        private static void storeFlag(byte p_270879_, CompoundTag p_270177_, String p_270294_, byte p_270853_)
        {
            p_270177_.putBoolean(p_270294_, (p_270879_ & p_270853_) != 0);
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag p_270268_)
        {
            super.addAdditionalSaveData(p_270268_);
            p_270268_.putString("text", Component.Serializer.toJson(this.getText(), this.registryAccess()));
            p_270268_.putInt("line_width", this.getLineWidth());
            p_270268_.putInt("background", this.getBackgroundColor());
            p_270268_.putByte("text_opacity", this.getTextOpacity());
            byte b0 = this.getFlags();
            storeFlag(b0, p_270268_, "shadow", (byte)1);
            storeFlag(b0, p_270268_, "see_through", (byte)2);
            storeFlag(b0, p_270268_, "default_background", (byte)4);
            Display.TextDisplay.Align.CODEC
            .encodeStart(NbtOps.INSTANCE, getAlign(b0))
            .ifSuccess(p_271001_ -> p_270268_.put("alignment", p_271001_));
        }

        @Override
        protected void updateRenderSubState(boolean p_277565_, float p_277967_)
        {
            if (p_277565_ && this.textRenderState != null)
            {
                this.textRenderState = this.createInterpolatedTextRenderState(this.textRenderState, p_277967_);
            }
            else
            {
                this.textRenderState = this.createFreshTextRenderState();
            }

            this.clientDisplayCache = null;
        }

        @Nullable
        public Display.TextDisplay.TextRenderState textRenderState()
        {
            return this.textRenderState;
        }

        private Display.TextDisplay.TextRenderState createFreshTextRenderState()
        {
            return new Display.TextDisplay.TextRenderState(
                       this.getText(),
                       this.getLineWidth(),
                       Display.IntInterpolator.constant(this.getTextOpacity()),
                       Display.IntInterpolator.constant(this.getBackgroundColor()),
                       this.getFlags()
                   );
        }

        private Display.TextDisplay.TextRenderState createInterpolatedTextRenderState(Display.TextDisplay.TextRenderState p_278000_, float p_277646_)
        {
            int i = p_278000_.backgroundColor.get(p_277646_);
            int j = p_278000_.textOpacity.get(p_277646_);
            return new Display.TextDisplay.TextRenderState(
                       this.getText(),
                       this.getLineWidth(),
                       new Display.LinearIntInterpolator(j, this.getTextOpacity()),
                       new Display.ColorInterpolator(i, this.getBackgroundColor()),
                       this.getFlags()
                   );
        }

        public Display.TextDisplay.CachedInfo cacheDisplay(Display.TextDisplay.LineSplitter p_270682_)
        {
            if (this.clientDisplayCache == null)
            {
                if (this.textRenderState != null)
                {
                    this.clientDisplayCache = p_270682_.split(this.textRenderState.text(), this.textRenderState.lineWidth());
                }
                else
                {
                    this.clientDisplayCache = new Display.TextDisplay.CachedInfo(List.of(), 0);
                }
            }

            return this.clientDisplayCache;
        }

        public static Display.TextDisplay.Align getAlign(byte p_270911_)
        {
            if ((p_270911_ & 8) != 0)
            {
                return Display.TextDisplay.Align.LEFT;
            }
            else
            {
                return (p_270911_ & 16) != 0 ? Display.TextDisplay.Align.RIGHT : Display.TextDisplay.Align.CENTER;
            }
        }

        public static enum Align implements StringRepresentable
        {
            CENTER("center"),
            LEFT("left"),
            RIGHT("right");

            public static final Codec<Display.TextDisplay.Align> CODEC = StringRepresentable.fromEnum(Display.TextDisplay.Align::values);
            private final String name;

            private Align(final String p_270554_)
            {
                this.name = p_270554_;
            }

            @Override
            public String getSerializedName()
            {
                return this.name;
            }
        }

        public static record CachedInfo(List<Display.TextDisplay.CachedLine> lines, int width)
        {
        }

        public static record CachedLine(FormattedCharSequence contents, int width)
        {
        }

        @FunctionalInterface
        public interface LineSplitter
        {
            Display.TextDisplay.CachedInfo split(Component p_270086_, int p_270526_);
        }

        public static record TextRenderState(
            Component text, int lineWidth, Display.IntInterpolator textOpacity, Display.IntInterpolator backgroundColor, byte flags
        )
        {
        }
    }

    static record TransformationInterpolator(Transformation previous, Transformation current) implements Display.GenericInterpolator<Transformation>
    {
        public Transformation get(float p_278027_)
        {
            return (double)p_278027_ >= 1.0 ? this.current : this.previous.slerp(this.current, p_278027_);
        }
    }
}
