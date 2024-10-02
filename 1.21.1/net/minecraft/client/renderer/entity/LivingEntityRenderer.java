package net.minecraft.client.renderer.entity;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.scores.Team;
import net.optifine.Config;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.Shaders;
import org.slf4j.Logger;

public abstract class LivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements RenderLayerParent<T, M>
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float EYE_BED_OFFSET = 0.1F;
    public M model;
    protected final List<RenderLayer<T, M>> layers = Lists.newArrayList();
    public float renderLimbSwing;
    public float renderLimbSwingAmount;
    public float renderAgeInTicks;
    public float renderHeadYaw;
    public float renderHeadPitch;
    public static final boolean animateModelLiving = Boolean.getBoolean("animate.model.living");
    private static boolean renderItemHead = false;

    public LivingEntityRenderer(EntityRendererProvider.Context p_174289_, M p_174290_, float p_174291_)
    {
        super(p_174289_);
        this.model = p_174290_;
        this.shadowRadius = p_174291_;
    }

    public final boolean addLayer(RenderLayer<T, M> p_115327_)
    {
        return this.layers.add(p_115327_);
    }

    @Override
    public M getModel()
    {
        return this.model;
    }

    public void render(T p_115308_, float p_115309_, float p_115310_, PoseStack p_115311_, MultiBufferSource p_115312_, int p_115313_)
    {
        if (!Reflector.ForgeEventFactoryClient_onRenderLivingPre.exists()
                || !Reflector.ForgeEventFactoryClient_onRenderLivingPre.callBoolean(p_115308_, this, p_115310_, p_115311_, p_115312_, p_115313_))
        {
            if (animateModelLiving)
            {
                p_115308_.walkAnimation.setSpeed(1.5F);
            }

            p_115311_.pushPose();
            this.model.attackTime = this.getAttackAnim(p_115308_, p_115310_);
            this.model.riding = p_115308_.isPassenger();

            if (Reflector.IForgeEntity_shouldRiderSit.exists())
            {
                this.model.riding = p_115308_.isPassenger()
                                           && p_115308_.getVehicle() != null
                                           && Reflector.callBoolean(p_115308_.getVehicle(), Reflector.IForgeEntity_shouldRiderSit);
            }

            this.model.young = p_115308_.isBaby();
            float f = Mth.rotLerp(p_115310_, p_115308_.yBodyRotO, p_115308_.yBodyRot);
            float f1 = Mth.rotLerp(p_115310_, p_115308_.yHeadRotO, p_115308_.yHeadRot);
            float f2 = f1 - f;

            if (this.model.riding && p_115308_.isPassenger() && p_115308_.getVehicle() instanceof LivingEntity livingentity)
            {
                f = Mth.rotLerp(p_115310_, livingentity.yBodyRotO, livingentity.yBodyRot);
                f2 = f1 - f;
                float f8 = Mth.wrapDegrees(f2);

                if (f8 < -85.0F)
                {
                    f8 = -85.0F;
                }

                if (f8 >= 85.0F)
                {
                    f8 = 85.0F;
                }

                f = f1 - f8;

                if (f8 * f8 > 2500.0F)
                {
                    f += f8 * 0.2F;
                }

                f2 = f1 - f;
            }

            float f7 = Mth.lerp(p_115310_, p_115308_.xRotO, p_115308_.getXRot());

            if (isEntityUpsideDown(p_115308_))
            {
                f7 *= -1.0F;
                f2 *= -1.0F;
            }

            f2 = Mth.wrapDegrees(f2);

            if (p_115308_.hasPose(Pose.SLEEPING))
            {
                Direction direction = p_115308_.getBedOrientation();

                if (direction != null)
                {
                    float f3 = p_115308_.getEyeHeight(Pose.STANDING) - 0.1F;
                    p_115311_.translate((float)(-direction.getStepX()) * f3, 0.0F, (float)(-direction.getStepZ()) * f3);
                }
            }

            float f9 = p_115308_.getScale();
            p_115311_.scale(f9, f9, f9);
            float f10 = this.getBob(p_115308_, p_115310_);
            this.setupRotations(p_115308_, p_115311_, f10, f, p_115310_, f9);
            p_115311_.scale(-1.0F, -1.0F, 1.0F);
            this.scale(p_115308_, p_115311_, p_115310_);
            p_115311_.translate(0.0F, -1.501F, 0.0F);
            float f4 = 0.0F;
            float f5 = 0.0F;

            if (!p_115308_.isPassenger() && p_115308_.isAlive())
            {
                f4 = p_115308_.walkAnimation.speed(p_115310_);
                f5 = p_115308_.walkAnimation.position(p_115310_);

                if (p_115308_.isBaby())
                {
                    f5 *= 3.0F;
                }

                if (f4 > 1.0F)
                {
                    f4 = 1.0F;
                }
            }

            this.model.prepareMobModel(p_115308_, f5, f4, p_115310_);
            this.model.setupAnim(p_115308_, f5, f4, f10, f2, f7);

            if (CustomEntityModels.isActive())
            {
                this.renderLimbSwing = f5;
                this.renderLimbSwingAmount = f4;
                this.renderAgeInTicks = f10;
                this.renderHeadYaw = f2;
                this.renderHeadPitch = f7;
            }

            boolean flag = Config.isShaders();
            Minecraft minecraft = Minecraft.getInstance();
            boolean flag1 = this.isBodyVisible(p_115308_);
            boolean flag2 = !flag1 && !p_115308_.isInvisibleTo(minecraft.player);
            boolean flag3 = minecraft.shouldEntityAppearGlowing(p_115308_);
            RenderType rendertype = this.getRenderType(p_115308_, flag1, flag2, flag3);

            if (rendertype != null)
            {
                VertexConsumer vertexconsumer = p_115312_.getBuffer(rendertype);
                float f6 = this.getWhiteOverlayProgress(p_115308_, p_115310_);

                if (flag)
                {
                    if (p_115308_.hurtTime > 0 || p_115308_.deathTime > 0)
                    {
                        Shaders.setEntityColor(1.0F, 0.0F, 0.0F, 0.3F);
                    }

                    if (f6 > 0.0F)
                    {
                        Shaders.setEntityColor(f6, f6, f6, 0.5F);
                    }
                }

                int i = getOverlayCoords(p_115308_, f6);
                this.model.renderToBuffer(p_115311_, vertexconsumer, p_115313_, i, flag2 ? 654311423 : -1);
            }

            if (!p_115308_.isSpectator())
            {
                for (RenderLayer<T, M> renderlayer : this.layers)
                {
                    if (renderlayer instanceof CustomHeadLayer)
                    {
                        renderItemHead = true;
                    }

                    renderlayer.render(p_115311_, p_115312_, p_115313_, p_115308_, f5, f4, p_115310_, f10, f2, f7);
                    renderItemHead = false;
                }
            }

            if (Config.isShaders())
            {
                Shaders.setEntityColor(0.0F, 0.0F, 0.0F, 0.0F);
            }

            p_115311_.popPose();
            super.render(p_115308_, p_115309_, p_115310_, p_115311_, p_115312_, p_115313_);

            if (Reflector.ForgeEventFactoryClient_onRenderLivingPost.exists())
            {
                Reflector.ForgeEventFactoryClient_onRenderLivingPost.callVoid(p_115308_, this, p_115310_, p_115311_, p_115312_, p_115313_);
            }
        }
    }

    @Nullable
    protected RenderType getRenderType(T p_115322_, boolean p_115323_, boolean p_115324_, boolean p_115325_)
    {
        ResourceLocation resourcelocation = this.getTextureLocation(p_115322_);

        if (this.getLocationTextureCustom() != null)
        {
            resourcelocation = this.getLocationTextureCustom();
        }

        if (p_115324_)
        {
            return RenderType.itemEntityTranslucentCull(resourcelocation);
        }
        else if (p_115323_)
        {
            return this.model.renderType(resourcelocation);
        }
        else if (p_115325_ && !Config.getMinecraft().levelRenderer.shouldShowEntityOutlines())
        {
            return this.model.renderType(resourcelocation);
        }
        else
        {
            return p_115325_ ? RenderType.outline(resourcelocation) : null;
        }
    }

    public static int getOverlayCoords(LivingEntity p_115339_, float p_115340_)
    {
        return OverlayTexture.pack(OverlayTexture.u(p_115340_), OverlayTexture.v(p_115339_.hurtTime > 0 || p_115339_.deathTime > 0));
    }

    protected boolean isBodyVisible(T p_115341_)
    {
        return !p_115341_.isInvisible();
    }

    private static float sleepDirectionToRotation(Direction p_115329_)
    {
        switch (p_115329_)
        {
            case SOUTH:
                return 90.0F;

            case WEST:
                return 0.0F;

            case NORTH:
                return 270.0F;

            case EAST:
                return 180.0F;

            default:
                return 0.0F;
        }
    }

    protected boolean isShaking(T p_115304_)
    {
        return p_115304_.isFullyFrozen();
    }

    protected void setupRotations(T p_115317_, PoseStack p_115318_, float p_115319_, float p_115320_, float p_115321_, float p_334101_)
    {
        if (this.isShaking(p_115317_))
        {
            p_115320_ += (float)(Math.cos((double)p_115317_.tickCount * 3.25) * Math.PI * 0.4F);
        }

        if (!p_115317_.hasPose(Pose.SLEEPING))
        {
            p_115318_.mulPose(Axis.YP.rotationDegrees(180.0F - p_115320_));
        }

        if (p_115317_.deathTime > 0)
        {
            float f = ((float)p_115317_.deathTime + p_115321_ - 1.0F) / 20.0F * 1.6F;
            f = Mth.sqrt(f);

            if (f > 1.0F)
            {
                f = 1.0F;
            }

            p_115318_.mulPose(Axis.ZP.rotationDegrees(f * this.getFlipDegrees(p_115317_)));
        }
        else if (p_115317_.isAutoSpinAttack())
        {
            p_115318_.mulPose(Axis.XP.rotationDegrees(-90.0F - p_115317_.getXRot()));
            p_115318_.mulPose(Axis.YP.rotationDegrees(((float)p_115317_.tickCount + p_115321_) * -75.0F));
        }
        else if (p_115317_.hasPose(Pose.SLEEPING))
        {
            Direction direction = p_115317_.getBedOrientation();
            float f1 = direction != null ? sleepDirectionToRotation(direction) : p_115320_;
            p_115318_.mulPose(Axis.YP.rotationDegrees(f1));
            p_115318_.mulPose(Axis.ZP.rotationDegrees(this.getFlipDegrees(p_115317_)));
            p_115318_.mulPose(Axis.YP.rotationDegrees(270.0F));
        }
        else if (isEntityUpsideDown(p_115317_))
        {
            p_115318_.translate(0.0F, (p_115317_.getBbHeight() + 0.1F) / p_334101_, 0.0F);
            p_115318_.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
    }

    protected float getAttackAnim(T p_115343_, float p_115344_)
    {
        return p_115343_.getAttackAnim(p_115344_);
    }

    protected float getBob(T p_115305_, float p_115306_)
    {
        return (float)p_115305_.tickCount + p_115306_;
    }

    protected float getFlipDegrees(T p_115337_)
    {
        return 90.0F;
    }

    protected float getWhiteOverlayProgress(T p_115334_, float p_115335_)
    {
        return 0.0F;
    }

    protected void scale(T p_115314_, PoseStack p_115315_, float p_115316_)
    {
    }

    protected boolean shouldShowName(T p_115333_)
    {
        double d0 = this.entityRenderDispatcher.distanceToSqr(p_115333_);
        float f = p_115333_.isDiscrete() ? 32.0F : 64.0F;

        if (d0 >= (double)(f * f))
        {
            return false;
        }
        else
        {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer localplayer = minecraft.player;
            boolean flag = !p_115333_.isInvisibleTo(localplayer);

            if (p_115333_ != localplayer)
            {
                Team team = p_115333_.getTeam();
                Team team1 = localplayer.getTeam();

                if (team != null)
                {
                    Team.Visibility team$visibility = team.getNameTagVisibility();

                    switch (team$visibility)
                    {
                        case ALWAYS:
                            return flag;

                        case NEVER:
                            return false;

                        case HIDE_FOR_OTHER_TEAMS:
                            return team1 == null ? flag : team.isAlliedTo(team1) && (team.canSeeFriendlyInvisibles() || flag);

                        case HIDE_FOR_OWN_TEAM:
                            return team1 == null ? flag : !team.isAlliedTo(team1) && flag;

                        default:
                            return true;
                    }
                }
            }

            return Minecraft.renderNames() && p_115333_ != minecraft.getCameraEntity() && flag && !p_115333_.isVehicle();
        }
    }

    public static boolean isEntityUpsideDown(LivingEntity p_194454_)
    {
        if (p_194454_ instanceof Player || p_194454_.hasCustomName())
        {
            String s = ChatFormatting.stripFormatting(p_194454_.getName().getString());

            if ("Dinnerbone".equals(s) || "Grumm".equals(s))
            {
                return !(p_194454_ instanceof Player) || ((Player)p_194454_).isModelPartShown(PlayerModelPart.CAPE);
            }
        }

        return false;
    }

    protected float getShadowRadius(T p_334594_)
    {
        return super.getShadowRadius(p_334594_) * p_334594_.getScale();
    }

    public <T extends RenderLayer> T getLayer(Class<T> cls)
    {
        List<T> list = this.getLayers(cls);
        return list.isEmpty() ? null : list.get(0);
    }

    public <T extends RenderLayer> List<T> getLayers(Class<T> cls)
    {
        List<RenderLayer> list = new ArrayList<>();

        for (RenderLayer renderlayer : this.layers)
        {
            if (cls.isInstance(renderlayer))
            {
                list.add(renderlayer);
            }
        }

        return (List<T>)list;
    }

    public void removeLayers(Class cls)
    {
        Iterator iterator = this.layers.iterator();

        while (iterator.hasNext())
        {
            RenderLayer renderlayer = (RenderLayer)iterator.next();

            if (cls.isInstance(renderlayer))
            {
                iterator.remove();
            }
        }
    }

    public static boolean isRenderItemHead()
    {
        return renderItemHead;
    }
}
