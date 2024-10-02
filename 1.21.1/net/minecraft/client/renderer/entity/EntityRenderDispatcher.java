package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.optifine.Config;
import net.optifine.DynamicLights;
import net.optifine.EmissiveTextures;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.player.PlayerItemsLayer;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.Shaders;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class EntityRenderDispatcher implements ResourceManagerReloadListener
{
    private static final RenderType SHADOW_RENDER_TYPE = RenderType.entityShadow(ResourceLocation.withDefaultNamespace("textures/misc/shadow.png"));
    private static final float MAX_SHADOW_RADIUS = 32.0F;
    private static final float SHADOW_POWER_FALLOFF_Y = 0.5F;
    private Map<EntityType<?>, EntityRenderer<?>> renderers = ImmutableMap.of();
    private Map<PlayerSkin.Model, EntityRenderer<? extends Player>> playerRenderers = Map.of();
    public final TextureManager textureManager;
    private Level level;
    public Camera camera;
    private Quaternionf cameraOrientation;
    public Entity crosshairPickEntity;
    private final ItemRenderer itemRenderer;
    private final BlockRenderDispatcher blockRenderDispatcher;
    private final ItemInHandRenderer itemInHandRenderer;
    private final Font font;
    public final Options options;
    private final EntityModelSet entityModels;
    private boolean shouldRenderShadow = true;
    private boolean renderHitBoxes;
    private EntityRenderer entityRenderer = null;
    private Entity renderedEntity = null;
    private EntityRendererProvider.Context context = null;

    public <E extends Entity> int getPackedLightCoords(E p_114395_, float p_114396_)
    {
        int i = this.getRenderer(p_114395_).getPackedLightCoords(p_114395_, p_114396_);

        if (Config.isDynamicLights())
        {
            i = DynamicLights.getCombinedLight(p_114395_, i);
        }

        return i;
    }

    public EntityRenderDispatcher(
        Minecraft p_234579_,
        TextureManager p_234580_,
        ItemRenderer p_234581_,
        BlockRenderDispatcher p_234582_,
        Font p_234583_,
        Options p_234584_,
        EntityModelSet p_234585_
    )
    {
        this.textureManager = p_234580_;
        this.itemRenderer = p_234581_;
        this.itemInHandRenderer = new ItemInHandRenderer(p_234579_, this, p_234581_);
        this.blockRenderDispatcher = p_234582_;
        this.font = p_234583_;
        this.options = p_234584_;
        this.entityModels = p_234585_;
    }

    public <T extends Entity> EntityRenderer <? super T > getRenderer(T p_114383_)
    {
        if (p_114383_ instanceof AbstractClientPlayer abstractclientplayer)
        {
            PlayerSkin.Model playerskin$model = abstractclientplayer.getSkin().model();
            EntityRenderer <? extends Player > entityrenderer = this.playerRenderers.get(playerskin$model);
            return (EntityRenderer <? super T >)(entityrenderer != null ? entityrenderer : this.playerRenderers.get(PlayerSkin.Model.WIDE));
        }
        else
        {
            return (EntityRenderer <? super T >)this.renderers.get(p_114383_.getType());
        }
    }

    public void prepare(Level p_114409_, Camera p_114410_, Entity p_114411_)
    {
        this.level = p_114409_;
        this.camera = p_114410_;
        this.cameraOrientation = p_114410_.rotation();
        this.crosshairPickEntity = p_114411_;
    }

    public void overrideCameraOrientation(Quaternionf p_254264_)
    {
        this.cameraOrientation = p_254264_;
    }

    public void setRenderShadow(boolean p_114469_)
    {
        this.shouldRenderShadow = p_114469_;
    }

    public void setRenderHitBoxes(boolean p_114474_)
    {
        this.renderHitBoxes = p_114474_;
    }

    public boolean shouldRenderHitBoxes()
    {
        return this.renderHitBoxes;
    }

    public <E extends Entity> boolean shouldRender(E p_114398_, Frustum p_114399_, double p_114400_, double p_114401_, double p_114402_)
    {
        EntityRenderer <? super E > entityrenderer = this.getRenderer(p_114398_);
        return entityrenderer.shouldRender(p_114398_, p_114399_, p_114400_, p_114401_, p_114402_);
    }

    public <E extends Entity> void render(
        E p_114385_,
        double p_114386_,
        double p_114387_,
        double p_114388_,
        float p_114389_,
        float p_114390_,
        PoseStack p_114391_,
        MultiBufferSource p_114392_,
        int p_114393_
    )
    {
        if (this.camera != null)
        {
            EntityRenderer <? super E > entityrenderer = this.getRenderer(p_114385_);

            try
            {
                Vec3 vec3 = entityrenderer.getRenderOffset(p_114385_, p_114390_);
                double d2 = p_114386_ + vec3.x();
                double d3 = p_114387_ + vec3.y();
                double d0 = p_114388_ + vec3.z();
                p_114391_.pushPose();
                p_114391_.translate(d2, d3, d0);
                EntityRenderer entityrenderer1 = this.entityRenderer;
                Entity entity = this.renderedEntity;
                entityrenderer = CustomEntityModels.getEntityRenderer(p_114385_, entityrenderer);
                this.entityRenderer = entityrenderer;
                this.renderedEntity = p_114385_;

                if (EmissiveTextures.isActive())
                {
                    EmissiveTextures.beginRender();
                }

                entityrenderer.render(p_114385_, p_114389_, p_114390_, p_114391_, p_114392_, p_114393_);

                if (EmissiveTextures.isActive())
                {
                    if (EmissiveTextures.hasEmissive())
                    {
                        EmissiveTextures.beginRenderEmissive();
                        entityrenderer.render(p_114385_, p_114389_, p_114390_, p_114391_, p_114392_, LightTexture.MAX_BRIGHTNESS);
                        EmissiveTextures.endRenderEmissive();
                    }

                    EmissiveTextures.endRender();
                }

                this.entityRenderer = entityrenderer1;
                this.renderedEntity = entity;

                if (p_114385_.displayFireAnimation())
                {
                    this.renderFlame(p_114391_, p_114392_, p_114385_, Mth.rotationAroundAxis(Mth.Y_AXIS, this.cameraOrientation, new Quaternionf()));
                }

                p_114391_.translate(-vec3.x(), -vec3.y(), -vec3.z());

                if (this.options.entityShadows().get() && this.shouldRenderShadow && !p_114385_.isInvisible())
                {
                    float f = entityrenderer.getShadowRadius(p_114385_);

                    if (f > 0.0F)
                    {
                        boolean flag = CustomEntityModels.isActive() && entityrenderer.shadowOffsetX != 0.0F && entityrenderer.shadowOffsetZ != 0.0F;

                        if (flag)
                        {
                            p_114391_.translate(entityrenderer.shadowOffsetX, 0.0F, entityrenderer.shadowOffsetZ);
                        }

                        double d1 = this.distanceToSqr(p_114385_.getX(), p_114385_.getY(), p_114385_.getZ());
                        float f1 = (float)((1.0 - d1 / 256.0) * (double)entityrenderer.shadowStrength);

                        if (f1 > 0.0F)
                        {
                            renderShadow(p_114391_, p_114392_, p_114385_, f1, p_114390_, this.level, Math.min(f, 32.0F));
                        }

                        if (flag)
                        {
                            p_114391_.translate(-entityrenderer.shadowOffsetX, 0.0F, -entityrenderer.shadowOffsetZ);
                        }
                    }
                }

                if (this.renderHitBoxes && !p_114385_.isInvisible() && !Minecraft.getInstance().showOnlyReducedInfo())
                {
                    renderHitbox(p_114391_, p_114392_.getBuffer(RenderType.lines()), p_114385_, p_114390_, 1.0F, 1.0F, 1.0F);
                }

                p_114391_.popPose();
            }
            catch (Throwable throwable1)
            {
                CrashReport crashreport = CrashReport.forThrowable(throwable1, "Rendering entity in world");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being rendered");
                p_114385_.fillCrashReportCategory(crashreportcategory);
                CrashReportCategory crashreportcategory1 = crashreport.addCategory("Renderer details");
                crashreportcategory1.setDetail("Assigned renderer", entityrenderer);
                crashreportcategory1.setDetail("Location", CrashReportCategory.formatLocation(this.level, p_114386_, p_114387_, p_114388_));
                crashreportcategory1.setDetail("Rotation", p_114389_);
                crashreportcategory1.setDetail("Delta", p_114390_);
                throw new ReportedException(crashreport);
            }
        }
    }

    private static void renderServerSideHitbox(PoseStack p_343043_, Entity p_343997_, MultiBufferSource p_342784_)
    {
        Entity entity = getServerSideEntity(p_343997_);

        if (entity == null)
        {
            DebugRenderer.renderFloatingText(p_343043_, p_342784_, "Missing", p_343997_.getX(), p_343997_.getBoundingBox().maxY + 1.5, p_343997_.getZ(), -65536);
        }
        else
        {
            p_343043_.pushPose();
            p_343043_.translate(entity.getX() - p_343997_.getX(), entity.getY() - p_343997_.getY(), entity.getZ() - p_343997_.getZ());
            renderHitbox(p_343043_, p_342784_.getBuffer(RenderType.lines()), entity, 1.0F, 0.0F, 1.0F, 0.0F);
            renderVector(p_343043_, p_342784_.getBuffer(RenderType.lines()), new Vector3f(), entity.getDeltaMovement(), -256);
            p_343043_.popPose();
        }
    }

    @Nullable
    private static Entity getServerSideEntity(Entity p_343454_)
    {
        IntegratedServer integratedserver = Minecraft.getInstance().getSingleplayerServer();

        if (integratedserver != null)
        {
            ServerLevel serverlevel = integratedserver.getLevel(p_343454_.level().dimension());

            if (serverlevel != null)
            {
                return serverlevel.getEntity(p_343454_.getId());
            }
        }

        return null;
    }

    private static void renderHitbox(
        PoseStack p_114442_, VertexConsumer p_114443_, Entity p_114444_, float p_114445_, float p_343193_, float p_342304_, float p_342638_
    )
    {
        if (!Shaders.isShadowPass)
        {
            AABB aabb = p_114444_.getBoundingBox().move(-p_114444_.getX(), -p_114444_.getY(), -p_114444_.getZ());
            LevelRenderer.renderLineBox(p_114442_, p_114443_, aabb, p_343193_, p_342304_, p_342638_, 1.0F);
            boolean flag = p_114444_ instanceof EnderDragon;

            if (Reflector.IForgeEntity_isMultipartEntity.exists() && Reflector.IForgeEntity_getParts.exists())
            {
                flag = Reflector.callBoolean(p_114444_, Reflector.IForgeEntity_isMultipartEntity);
            }

            if (flag)
            {
                double d0 = -Mth.lerp((double)p_114445_, p_114444_.xOld, p_114444_.getX());
                double d1 = -Mth.lerp((double)p_114445_, p_114444_.yOld, p_114444_.getY());
                double d2 = -Mth.lerp((double)p_114445_, p_114444_.zOld, p_114444_.getZ());
                Entity[] aentity = (Entity[])(Reflector.IForgeEntity_getParts.exists()
                                              ? (Entity[])Reflector.call(p_114444_, Reflector.IForgeEntity_getParts)
                                              : ((EnderDragon)p_114444_).getSubEntities());

                for (Entity entity : aentity)
                {
                    p_114442_.pushPose();
                    double d3 = d0 + Mth.lerp((double)p_114445_, entity.xOld, entity.getX());
                    double d4 = d1 + Mth.lerp((double)p_114445_, entity.yOld, entity.getY());
                    double d5 = d2 + Mth.lerp((double)p_114445_, entity.zOld, entity.getZ());
                    p_114442_.translate(d3, d4, d5);
                    LevelRenderer.renderLineBox(
                        p_114442_, p_114443_, entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ()), 0.25F, 1.0F, 0.0F, 1.0F
                    );
                    p_114442_.popPose();
                }
            }

            if (p_114444_ instanceof LivingEntity)
            {
                float f1 = 0.01F;
                LevelRenderer.renderLineBox(
                    p_114442_,
                    p_114443_,
                    aabb.minX,
                    (double)(p_114444_.getEyeHeight() - 0.01F),
                    aabb.minZ,
                    aabb.maxX,
                    (double)(p_114444_.getEyeHeight() + 0.01F),
                    aabb.maxZ,
                    1.0F,
                    0.0F,
                    0.0F,
                    1.0F
                );
            }

            Entity entity1 = p_114444_.getVehicle();

            if (entity1 != null)
            {
                float f = Math.min(entity1.getBbWidth(), p_114444_.getBbWidth()) / 2.0F;
                float f2 = 0.0625F;
                Vec3 vec3 = entity1.getPassengerRidingPosition(p_114444_).subtract(p_114444_.position());
                LevelRenderer.renderLineBox(
                    p_114442_,
                    p_114443_,
                    vec3.x - (double)f,
                    vec3.y,
                    vec3.z - (double)f,
                    vec3.x + (double)f,
                    vec3.y + 0.0625,
                    vec3.z + (double)f,
                    1.0F,
                    1.0F,
                    0.0F,
                    1.0F
                );
            }

            renderVector(p_114442_, p_114443_, new Vector3f(0.0F, p_114444_.getEyeHeight(), 0.0F), p_114444_.getViewVector(p_114445_).scale(2.0), -16776961);
        }
    }

    private static void renderVector(PoseStack p_344314_, VertexConsumer p_344851_, Vector3f p_343382_, Vec3 p_343924_, int p_342889_)
    {
        PoseStack.Pose posestack$pose = p_344314_.last();
        p_344851_.addVertex(posestack$pose, p_343382_)
        .setColor(p_342889_)
        .setNormal(posestack$pose, (float)p_343924_.x, (float)p_343924_.y, (float)p_343924_.z);
        p_344851_.addVertex(
            posestack$pose,
            (float)((double)p_343382_.x() + p_343924_.x),
            (float)((double)p_343382_.y() + p_343924_.y),
            (float)((double)p_343382_.z() + p_343924_.z)
        )
        .setColor(p_342889_)
        .setNormal(posestack$pose, (float)p_343924_.x, (float)p_343924_.y, (float)p_343924_.z);
    }

    private void renderFlame(PoseStack p_114454_, MultiBufferSource p_114455_, Entity p_114456_, Quaternionf p_312342_)
    {
        TextureAtlasSprite textureatlassprite = ModelBakery.FIRE_0.sprite();
        TextureAtlasSprite textureatlassprite1 = ModelBakery.FIRE_1.sprite();
        p_114454_.pushPose();
        float f = p_114456_.getBbWidth() * 1.4F;
        p_114454_.scale(f, f, f);
        float f1 = 0.5F;
        float f2 = 0.0F;
        float f3 = p_114456_.getBbHeight() / f;
        float f4 = 0.0F;
        p_114454_.mulPose(p_312342_);
        p_114454_.translate(0.0F, 0.0F, 0.3F - (float)((int)f3) * 0.02F);
        float f5 = 0.0F;
        int i = 0;
        VertexConsumer vertexconsumer = p_114455_.getBuffer(Sheets.cutoutBlockSheet());

        for (PoseStack.Pose posestack$pose = p_114454_.last(); f3 > 0.0F; i++)
        {
            TextureAtlasSprite textureatlassprite2 = i % 2 == 0 ? textureatlassprite : textureatlassprite1;
            vertexconsumer.setSprite(textureatlassprite2);
            float f6 = textureatlassprite2.getU0();
            float f7 = textureatlassprite2.getV0();
            float f8 = textureatlassprite2.getU1();
            float f9 = textureatlassprite2.getV1();

            if (i / 2 % 2 == 0)
            {
                float f10 = f8;
                f8 = f6;
                f6 = f10;
            }

            fireVertex(posestack$pose, vertexconsumer, -f1 - 0.0F, 0.0F - f4, f5, f8, f9);
            fireVertex(posestack$pose, vertexconsumer, f1 - 0.0F, 0.0F - f4, f5, f6, f9);
            fireVertex(posestack$pose, vertexconsumer, f1 - 0.0F, 1.4F - f4, f5, f6, f7);
            fireVertex(posestack$pose, vertexconsumer, -f1 - 0.0F, 1.4F - f4, f5, f8, f7);
            f3 -= 0.45F;
            f4 -= 0.45F;
            f1 *= 0.9F;
            f5 -= 0.03F;
        }

        vertexconsumer.setSprite(null);
        p_114454_.popPose();
    }

    private static void fireVertex(
        PoseStack.Pose p_114415_, VertexConsumer p_114416_, float p_114417_, float p_114418_, float p_114419_, float p_114420_, float p_114421_
    )
    {
        p_114416_.addVertex(p_114415_, p_114417_, p_114418_, p_114419_)
        .setColor(-1)
        .setUv(p_114420_, p_114421_)
        .setUv1(0, 10)
        .setLight(240)
        .setNormal(p_114415_, 0.0F, 1.0F, 0.0F);
    }

    private static void renderShadow(
        PoseStack p_114458_, MultiBufferSource p_114459_, Entity p_114460_, float p_114461_, float p_114462_, LevelReader p_114463_, float p_114464_
    )
    {
        if (!Config.isShaders() || !Shaders.shouldSkipDefaultShadow)
        {
            double d0 = Mth.lerp((double)p_114462_, p_114460_.xOld, p_114460_.getX());
            double d1 = Mth.lerp((double)p_114462_, p_114460_.yOld, p_114460_.getY());
            double d2 = Mth.lerp((double)p_114462_, p_114460_.zOld, p_114460_.getZ());
            float f = Math.min(p_114461_ / 0.5F, p_114464_);
            int i = Mth.floor(d0 - (double)p_114464_);
            int j = Mth.floor(d0 + (double)p_114464_);
            int k = Mth.floor(d1 - (double)f);
            int l = Mth.floor(d1);
            int i1 = Mth.floor(d2 - (double)p_114464_);
            int j1 = Mth.floor(d2 + (double)p_114464_);
            PoseStack.Pose posestack$pose = p_114458_.last();
            VertexConsumer vertexconsumer = p_114459_.getBuffer(SHADOW_RENDER_TYPE);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (int k1 = i1; k1 <= j1; k1++)
            {
                for (int l1 = i; l1 <= j; l1++)
                {
                    blockpos$mutableblockpos.set(l1, 0, k1);
                    ChunkAccess chunkaccess = p_114463_.getChunk(blockpos$mutableblockpos);

                    for (int i2 = k; i2 <= l; i2++)
                    {
                        blockpos$mutableblockpos.setY(i2);
                        float f1 = p_114461_ - (float)(d1 - (double)blockpos$mutableblockpos.getY()) * 0.5F;
                        renderBlockShadow(posestack$pose, vertexconsumer, chunkaccess, p_114463_, blockpos$mutableblockpos, d0, d1, d2, p_114464_, f1);
                    }
                }
            }
        }
    }

    private static void renderBlockShadow(
        PoseStack.Pose p_277956_,
        VertexConsumer p_277533_,
        ChunkAccess p_277501_,
        LevelReader p_277622_,
        BlockPos p_277911_,
        double p_277682_,
        double p_278099_,
        double p_277806_,
        float p_277844_,
        float p_277496_
    )
    {
        BlockPos blockpos = p_277911_.below();
        BlockState blockstate = p_277501_.getBlockState(blockpos);

        if (blockstate.getRenderShape() != RenderShape.INVISIBLE && p_277622_.getMaxLocalRawBrightness(p_277911_) > 3 && blockstate.isCollisionShapeFullBlock(p_277501_, blockpos))
        {
            VoxelShape voxelshape = blockstate.getShape(p_277501_, blockpos);

            if (!voxelshape.isEmpty())
            {
                float f = LightTexture.getBrightness(p_277622_.dimensionType(), p_277622_.getMaxLocalRawBrightness(p_277911_));
                float f1 = p_277496_ * 0.5F * f;

                if (f1 >= 0.0F)
                {
                    if (f1 > 1.0F)
                    {
                        f1 = 1.0F;
                    }

                    int i = FastColor.ARGB32.color(Mth.floor(f1 * 255.0F), 255, 255, 255);
                    AABB aabb = voxelshape.bounds();
                    double d0 = (double)p_277911_.getX() + aabb.minX;
                    double d1 = (double)p_277911_.getX() + aabb.maxX;
                    double d2 = (double)p_277911_.getY() + aabb.minY;
                    double d3 = (double)p_277911_.getZ() + aabb.minZ;
                    double d4 = (double)p_277911_.getZ() + aabb.maxZ;
                    float f2 = (float)(d0 - p_277682_);
                    float f3 = (float)(d1 - p_277682_);
                    float f4 = (float)(d2 - p_278099_);
                    float f5 = (float)(d3 - p_277806_);
                    float f6 = (float)(d4 - p_277806_);
                    float f7 = -f2 / 2.0F / p_277844_ + 0.5F;
                    float f8 = -f3 / 2.0F / p_277844_ + 0.5F;
                    float f9 = -f5 / 2.0F / p_277844_ + 0.5F;
                    float f10 = -f6 / 2.0F / p_277844_ + 0.5F;
                    shadowVertex(p_277956_, p_277533_, i, f2, f4, f5, f7, f9);
                    shadowVertex(p_277956_, p_277533_, i, f2, f4, f6, f7, f10);
                    shadowVertex(p_277956_, p_277533_, i, f3, f4, f6, f8, f10);
                    shadowVertex(p_277956_, p_277533_, i, f3, f4, f5, f8, f9);
                }
            }
        }
    }

    private static void shadowVertex(
        PoseStack.Pose p_114423_, VertexConsumer p_114424_, int p_343218_, float p_114425_, float p_114426_, float p_114427_, float p_114428_, float p_114429_
    )
    {
        Vector3f vector3f = p_114423_.pose().transformPosition(p_114425_, p_114426_, p_114427_, new Vector3f());
        p_114424_.addVertex(vector3f.x(), vector3f.y(), vector3f.z(), p_343218_, p_114428_, p_114429_, OverlayTexture.NO_OVERLAY, 15728880, 0.0F, 1.0F, 0.0F);
    }

    public void setLevel(@Nullable Level p_114407_)
    {
        this.level = p_114407_;

        if (p_114407_ == null)
        {
            this.camera = null;
        }
        else
        {
            this.camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            this.cameraOrientation = this.camera.rotation();
        }
    }

    public double distanceToSqr(Entity p_114472_)
    {
        return this.camera.getPosition().distanceToSqr(p_114472_.position());
    }

    public double distanceToSqr(double p_114379_, double p_114380_, double p_114381_)
    {
        return this.camera.getPosition().distanceToSqr(p_114379_, p_114380_, p_114381_);
    }

    public Quaternionf cameraOrientation()
    {
        return this.cameraOrientation;
    }

    public ItemInHandRenderer getItemInHandRenderer()
    {
        return this.itemInHandRenderer;
    }

    @Override
    public void onResourceManagerReload(ResourceManager p_174004_)
    {
        EntityRendererProvider.Context entityrendererprovider$context = new EntityRendererProvider.Context(
            this, this.itemRenderer, this.blockRenderDispatcher, this.itemInHandRenderer, p_174004_, this.entityModels, this.font
        );
        this.context = entityrendererprovider$context;
        this.renderers = EntityRenderers.createEntityRenderers(entityrendererprovider$context);
        this.playerRenderers = EntityRenderers.createPlayerRenderers(entityrendererprovider$context);
        registerPlayerItems(this.playerRenderers);

        if (Reflector.ForgeEventFactoryClient_onGatherLayers.exists())
        {
            Reflector.ForgeEventFactoryClient_onGatherLayers.call(this.renderers, this.playerRenderers, entityrendererprovider$context);
        }
    }

    private static void registerPlayerItems(Map<PlayerSkin.Model, EntityRenderer<? extends Player>> renderPlayerMap)
    {
        boolean flag = false;

        for (EntityRenderer<? extends Player> entityrenderer : renderPlayerMap.values())
        {
            if (entityrenderer instanceof PlayerRenderer playerrenderer)
            {
                playerrenderer.removeLayers(PlayerItemsLayer.class);
                playerrenderer.addLayer(new PlayerItemsLayer(playerrenderer));
                flag = true;
            }
        }

        if (!flag)
        {
            Config.warn("PlayerItemsLayer not registered");
        }
    }

    public Map<EntityType<?>, EntityRenderer<?>> getEntityRenderMap()
    {
        if (this.renderers instanceof ImmutableMap)
        {
            this.renderers = new HashMap<>(this.renderers);
        }

        return this.renderers;
    }

    public EntityRendererProvider.Context getContext()
    {
        return this.context;
    }

    public Entity getRenderedEntity()
    {
        return this.renderedEntity;
    }

    public EntityRenderer getEntityRenderer()
    {
        return this.entityRenderer;
    }

    public void setRenderedEntity(Entity renderedEntity)
    {
        this.renderedEntity = renderedEntity;
    }

    public Map<PlayerSkin.Model, EntityRenderer> getSkinMap()
    {
        return Collections.unmodifiableMap(this.playerRenderers);
    }
}
