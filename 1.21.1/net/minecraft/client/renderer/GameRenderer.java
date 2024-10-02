package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import net.optifine.Config;
import net.optifine.GlErrors;
import net.optifine.Lagometer;
import net.optifine.RandomEntities;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.gui.GuiChatOF;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorForge;
import net.optifine.reflect.ReflectorResolver;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import net.optifine.util.MemoryMonitor;
import net.optifine.util.TimedEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

public class GameRenderer implements AutoCloseable
{
    private static final ResourceLocation NAUSEA_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/nausea.png");
    private static final ResourceLocation BLUR_LOCATION = ResourceLocation.withDefaultNamespace("shaders/post/blur.json");
    public static final int MAX_BLUR_RADIUS = 10;
    static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEPTH_BUFFER_DEBUG = false;
    public static final float PROJECTION_Z_NEAR = 0.05F;
    private static final float GUI_Z_NEAR = 1000.0F;
    final Minecraft minecraft;
    private final ResourceManager resourceManager;
    private final RandomSource random = RandomSource.create();
    private float renderDistance;
    public final ItemInHandRenderer itemInHandRenderer;
    private final MapRenderer mapRenderer;
    private final RenderBuffers renderBuffers;
    private int confusionAnimationTick;
    private float fov;
    private float oldFov;
    private float darkenWorldAmount;
    private float darkenWorldAmountO;
    private boolean renderHand = true;
    private boolean renderBlockOutline = true;
    private long lastScreenshotAttempt;
    private boolean hasWorldScreenshot;
    private long lastActiveTime = Util.getMillis();
    private final LightTexture lightTexture;
    private final OverlayTexture overlayTexture = new OverlayTexture();
    private boolean panoramicMode;
    private float zoom = 1.0F;
    private float zoomX;
    private float zoomY;
    public static final int ITEM_ACTIVATION_ANIMATION_LENGTH = 40;
    @Nullable
    private ItemStack itemActivationItem;
    private int itemActivationTicks;
    private float itemActivationOffX;
    private float itemActivationOffY;
    @Nullable
    PostChain postEffect;
    @Nullable
    private PostChain blurEffect;
    private boolean effectActive;
    private final Camera mainCamera = new Camera();
    @Nullable
    public ShaderInstance blitShader;
    private final Map<String, ShaderInstance> shaders = Maps.newHashMap();
    @Nullable
    private static ShaderInstance positionShader;
    @Nullable
    private static ShaderInstance positionColorShader;
    @Nullable
    private static ShaderInstance positionTexShader;
    @Nullable
    private static ShaderInstance positionTexColorShader;
    @Nullable
    private static ShaderInstance particleShader;
    @Nullable
    private static ShaderInstance positionColorLightmapShader;
    @Nullable
    private static ShaderInstance positionColorTexLightmapShader;
    @Nullable
    private static ShaderInstance rendertypeSolidShader;
    @Nullable
    private static ShaderInstance rendertypeCutoutMippedShader;
    @Nullable
    private static ShaderInstance rendertypeCutoutShader;
    @Nullable
    private static ShaderInstance rendertypeTranslucentShader;
    @Nullable
    private static ShaderInstance rendertypeTranslucentMovingBlockShader;
    @Nullable
    private static ShaderInstance rendertypeArmorCutoutNoCullShader;
    @Nullable
    private static ShaderInstance rendertypeEntitySolidShader;
    @Nullable
    private static ShaderInstance rendertypeEntityCutoutShader;
    @Nullable
    private static ShaderInstance rendertypeEntityCutoutNoCullShader;
    @Nullable
    private static ShaderInstance rendertypeEntityCutoutNoCullZOffsetShader;
    @Nullable
    private static ShaderInstance rendertypeItemEntityTranslucentCullShader;
    @Nullable
    private static ShaderInstance rendertypeEntityTranslucentCullShader;
    @Nullable
    private static ShaderInstance rendertypeEntityTranslucentShader;
    @Nullable
    private static ShaderInstance rendertypeEntityTranslucentEmissiveShader;
    @Nullable
    private static ShaderInstance rendertypeEntitySmoothCutoutShader;
    @Nullable
    private static ShaderInstance rendertypeBeaconBeamShader;
    @Nullable
    private static ShaderInstance rendertypeEntityDecalShader;
    @Nullable
    private static ShaderInstance rendertypeEntityNoOutlineShader;
    @Nullable
    private static ShaderInstance rendertypeEntityShadowShader;
    @Nullable
    private static ShaderInstance rendertypeEntityAlphaShader;
    @Nullable
    private static ShaderInstance rendertypeEyesShader;
    @Nullable
    private static ShaderInstance rendertypeEnergySwirlShader;
    @Nullable
    private static ShaderInstance rendertypeBreezeWindShader;
    @Nullable
    private static ShaderInstance rendertypeLeashShader;
    @Nullable
    private static ShaderInstance rendertypeWaterMaskShader;
    @Nullable
    private static ShaderInstance rendertypeOutlineShader;
    @Nullable
    private static ShaderInstance rendertypeArmorGlintShader;
    @Nullable
    private static ShaderInstance rendertypeArmorEntityGlintShader;
    @Nullable
    private static ShaderInstance rendertypeGlintTranslucentShader;
    @Nullable
    private static ShaderInstance rendertypeGlintShader;
    @Nullable
    private static ShaderInstance rendertypeGlintDirectShader;
    @Nullable
    private static ShaderInstance rendertypeEntityGlintShader;
    @Nullable
    private static ShaderInstance rendertypeEntityGlintDirectShader;
    @Nullable
    private static ShaderInstance rendertypeTextShader;
    @Nullable
    private static ShaderInstance rendertypeTextBackgroundShader;
    @Nullable
    private static ShaderInstance rendertypeTextIntensityShader;
    @Nullable
    private static ShaderInstance rendertypeTextSeeThroughShader;
    @Nullable
    private static ShaderInstance rendertypeTextBackgroundSeeThroughShader;
    @Nullable
    private static ShaderInstance rendertypeTextIntensitySeeThroughShader;
    @Nullable
    private static ShaderInstance rendertypeLightningShader;
    @Nullable
    private static ShaderInstance rendertypeTripwireShader;
    @Nullable
    private static ShaderInstance rendertypeEndPortalShader;
    @Nullable
    private static ShaderInstance rendertypeEndGatewayShader;
    @Nullable
    private static ShaderInstance rendertypeCloudsShader;
    @Nullable
    private static ShaderInstance rendertypeLinesShader;
    @Nullable
    private static ShaderInstance rendertypeCrumblingShader;
    @Nullable
    private static ShaderInstance rendertypeGuiShader;
    @Nullable
    private static ShaderInstance rendertypeGuiOverlayShader;
    @Nullable
    private static ShaderInstance rendertypeGuiTextHighlightShader;
    @Nullable
    private static ShaderInstance rendertypeGuiGhostRecipeOverlayShader;
    private boolean initialized = false;
    private Level updatedWorld = null;
    private int frameCount = 0;
    private float clipDistance = 128.0F;
    private long lastServerTime = 0L;
    private int lastServerTicks = 0;
    private int serverWaitTime = 0;
    private int serverWaitTimeCurrent = 0;
    private float avgServerTimeDiff = 0.0F;
    private float avgServerTickDiff = 0.0F;
    private PostChain[] fxaaShaders = new PostChain[10];
    private boolean guiLoadingVisible = false;

    public GameRenderer(Minecraft p_234219_, ItemInHandRenderer p_234220_, ResourceManager p_234221_, RenderBuffers p_234222_)
    {
        this.minecraft = p_234219_;
        this.resourceManager = p_234221_;
        this.itemInHandRenderer = p_234220_;
        this.mapRenderer = new MapRenderer(p_234219_.getTextureManager(), p_234219_.getMapDecorationTextures());
        this.lightTexture = new LightTexture(this, p_234219_);
        this.renderBuffers = p_234222_;
        this.postEffect = null;
    }

    @Override
    public void close()
    {
        this.lightTexture.close();
        this.mapRenderer.close();
        this.overlayTexture.close();
        this.shutdownEffect();
        this.shutdownShaders();

        if (this.blurEffect != null)
        {
            this.blurEffect.close();
        }

        if (this.blitShader != null)
        {
            this.blitShader.close();
        }
    }

    public void setRenderHand(boolean p_172737_)
    {
        this.renderHand = p_172737_;
    }

    public void setRenderBlockOutline(boolean p_172776_)
    {
        this.renderBlockOutline = p_172776_;
    }

    public void setPanoramicMode(boolean p_172780_)
    {
        this.panoramicMode = p_172780_;
    }

    public boolean isPanoramicMode()
    {
        return this.panoramicMode;
    }

    public void shutdownEffect()
    {
        if (this.postEffect != null)
        {
            this.postEffect.close();
        }

        this.postEffect = null;
    }

    public void togglePostEffect()
    {
        this.effectActive = !this.effectActive;
    }

    public void checkEntityPostEffect(@Nullable Entity p_109107_)
    {
        if (this.postEffect != null)
        {
            this.postEffect.close();
        }

        this.postEffect = null;

        if (p_109107_ instanceof Creeper)
        {
            this.loadEffect(ResourceLocation.withDefaultNamespace("shaders/post/creeper.json"));
        }
        else if (p_109107_ instanceof Spider)
        {
            this.loadEffect(ResourceLocation.withDefaultNamespace("shaders/post/spider.json"));
        }
        else if (p_109107_ instanceof EnderMan)
        {
            this.loadEffect(ResourceLocation.withDefaultNamespace("shaders/post/invert.json"));
        }
        else if (Reflector.ForgeHooksClient_loadEntityShader.exists())
        {
            Reflector.call(Reflector.ForgeHooksClient_loadEntityShader, p_109107_, this);
        }
    }

    private void loadEffect(ResourceLocation p_109129_)
    {
        if (GLX.isUsingFBOs())
        {
            if (this.postEffect != null)
            {
                this.postEffect.close();
            }

            try
            {
                this.postEffect = new PostChain(this.minecraft.getTextureManager(), this.resourceManager, this.minecraft.getMainRenderTarget(), p_109129_);
                this.postEffect.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
                this.effectActive = true;
            }
            catch (IOException ioexception)
            {
                LOGGER.warn("Failed to load shader: {}", p_109129_, ioexception);
                this.effectActive = false;
            }
            catch (JsonSyntaxException jsonsyntaxexception)
            {
                LOGGER.warn("Failed to parse shader: {}", p_109129_, jsonsyntaxexception);
                this.effectActive = false;
            }
        }
    }

    private void loadBlurEffect(ResourceProvider p_330023_)
    {
        if (this.blurEffect != null)
        {
            this.blurEffect.close();
        }

        try
        {
            this.blurEffect = new PostChain(this.minecraft.getTextureManager(), p_330023_, this.minecraft.getMainRenderTarget(), BLUR_LOCATION);
            this.blurEffect.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
        }
        catch (IOException ioexception)
        {
            LOGGER.warn("Failed to load shader: {}", BLUR_LOCATION, ioexception);
        }
        catch (JsonSyntaxException jsonsyntaxexception)
        {
            LOGGER.warn("Failed to parse shader: {}", BLUR_LOCATION, jsonsyntaxexception);
        }
    }

    public void processBlurEffect(float p_333718_)
    {
        if (GLX.isUsingFBOs())
        {
            RenderSystem.disableDepthTest();
            float f = (float)this.minecraft.options.getMenuBackgroundBlurriness();

            if (this.blurEffect != null && f >= 1.0F)
            {
                this.blurEffect.setUniform("Radius", f);
                this.blurEffect.process(p_333718_);
            }
        }
    }

    public PreparableReloadListener createReloadListener()
    {
        return new SimplePreparableReloadListener<GameRenderer.ResourceCache>()
        {
            protected GameRenderer.ResourceCache prepare(ResourceManager p_251213_, ProfilerFiller p_251006_)
            {
                Map<ResourceLocation, Resource> map = p_251213_.listResources(
                        "shaders",
                        locIn ->
                {
                    String s = locIn.getPath();
                    return s.endsWith(".json")
                    || s.endsWith(Program.Type.FRAGMENT.getExtension())
                    || s.endsWith(Program.Type.VERTEX.getExtension())
                    || s.endsWith(".glsl");
                }
                                                      );
                Map<ResourceLocation, Resource> map1 = new HashMap<>();
                map.forEach((locIn, resIn) ->
                {
                    try (InputStream inputstream = resIn.open())
                    {
                        byte[] abyte = inputstream.readAllBytes();
                        map1.put(locIn, new Resource(resIn.source(), () -> new ByteArrayInputStream(abyte)));
                    }
                    catch (Exception exception1)
                    {
                        GameRenderer.LOGGER.warn("Failed to read resource {}", locIn, exception1);
                    }
                });
                return new GameRenderer.ResourceCache(p_251213_, map1);
            }
            protected void apply(GameRenderer.ResourceCache p_251168_, ResourceManager p_248902_, ProfilerFiller p_251909_)
            {
                GameRenderer.this.reloadShaders(p_251168_);

                if (GameRenderer.this.postEffect != null)
                {
                    GameRenderer.this.postEffect.close();
                }

                GameRenderer.this.postEffect = null;
                GameRenderer.this.checkEntityPostEffect(GameRenderer.this.minecraft.getCameraEntity());
            }
            @Override
            public String getName()
            {
                return "Shader Loader";
            }
        };
    }

    public void preloadUiShader(ResourceProvider p_172723_)
    {
        if (this.blitShader != null)
        {
            throw new RuntimeException("Blit shader already preloaded");
        }
        else
        {
            try
            {
                this.blitShader = new ShaderInstance(p_172723_, "blit_screen", DefaultVertexFormat.BLIT_SCREEN);
            }
            catch (IOException ioexception)
            {
                throw new RuntimeException("could not preload blit shader", ioexception);
            }

            rendertypeGuiShader = this.preloadShader(p_172723_, "rendertype_gui", DefaultVertexFormat.POSITION_COLOR);
            rendertypeGuiOverlayShader = this.preloadShader(p_172723_, "rendertype_gui_overlay", DefaultVertexFormat.POSITION_COLOR);
            positionShader = this.preloadShader(p_172723_, "position", DefaultVertexFormat.POSITION);
            positionColorShader = this.preloadShader(p_172723_, "position_color", DefaultVertexFormat.POSITION_COLOR);
            positionTexShader = this.preloadShader(p_172723_, "position_tex", DefaultVertexFormat.POSITION_TEX);
            positionTexColorShader = this.preloadShader(p_172723_, "position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR);
            rendertypeTextShader = this.preloadShader(p_172723_, "rendertype_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
        }
    }

    private ShaderInstance preloadShader(ResourceProvider p_172725_, String p_172726_, VertexFormat p_172727_)
    {
        try
        {
            ShaderInstance shaderinstance = new ShaderInstance(p_172725_, p_172726_, p_172727_);
            this.shaders.put(p_172726_, shaderinstance);
            return shaderinstance;
        }
        catch (Exception exception1)
        {
            throw new IllegalStateException("could not preload shader " + p_172726_, exception1);
        }
    }

    void reloadShaders(ResourceProvider p_250719_)
    {
        RenderSystem.assertOnRenderThread();
        List<Program> list = Lists.newArrayList();
        list.addAll(Program.Type.FRAGMENT.getPrograms().values());
        list.addAll(Program.Type.VERTEX.getPrograms().values());
        list.forEach(Program::close);
        List<Pair<ShaderInstance, Consumer<ShaderInstance>>> list1 = Lists.newArrayListWithCapacity(this.shaders.size());

        try
        {
            list1.add(Pair.of(new ShaderInstance(p_250719_, "particle", DefaultVertexFormat.PARTICLE), p_172713_0_ -> particleShader = p_172713_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "position", DefaultVertexFormat.POSITION), p_172710_0_ -> positionShader = p_172710_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "position_color", DefaultVertexFormat.POSITION_COLOR), p_172707_0_ -> positionColorShader = p_172707_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "position_color_lightmap", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP), p_172704_0_ -> positionColorLightmapShader = p_172704_0_));
            list1.add(
                Pair.of(new ShaderInstance(p_250719_, "position_color_tex_lightmap", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), p_172698_0_ -> positionColorTexLightmapShader = p_172698_0_)
            );
            list1.add(Pair.of(new ShaderInstance(p_250719_, "position_tex", DefaultVertexFormat.POSITION_TEX), p_172695_0_ -> positionTexShader = p_172695_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR), p_172692_0_ -> positionTexColorShader = p_172692_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_solid", DefaultVertexFormat.BLOCK), p_172683_0_ -> rendertypeSolidShader = p_172683_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_cutout_mipped", DefaultVertexFormat.BLOCK), p_172680_0_ -> rendertypeCutoutMippedShader = p_172680_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_cutout", DefaultVertexFormat.BLOCK), p_172677_0_ -> rendertypeCutoutShader = p_172677_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_translucent", DefaultVertexFormat.BLOCK), p_172674_0_ -> rendertypeTranslucentShader = p_172674_0_));
            list1.add(
                Pair.of(
                    new ShaderInstance(p_250719_, "rendertype_translucent_moving_block", DefaultVertexFormat.BLOCK), p_172671_0_ -> rendertypeTranslucentMovingBlockShader = p_172671_0_
                )
            );
            list1.add(
                Pair.of(new ShaderInstance(p_250719_, "rendertype_armor_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY), p_172665_0_ -> rendertypeArmorCutoutNoCullShader = p_172665_0_)
            );
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_entity_solid", DefaultVertexFormat.NEW_ENTITY), p_172662_0_ -> rendertypeEntitySolidShader = p_172662_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_entity_cutout", DefaultVertexFormat.NEW_ENTITY), p_172659_0_ -> rendertypeEntityCutoutShader = p_172659_0_));
            list1.add(
                Pair.of(new ShaderInstance(p_250719_, "rendertype_entity_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY), p_172656_0_ -> rendertypeEntityCutoutNoCullShader = p_172656_0_)
            );
            list1.add(
                Pair.of(
                    new ShaderInstance(p_250719_, "rendertype_entity_cutout_no_cull_z_offset", DefaultVertexFormat.NEW_ENTITY),
                    p_172653_0_ -> rendertypeEntityCutoutNoCullZOffsetShader = p_172653_0_
                )
            );
            list1.add(
                Pair.of(
                    new ShaderInstance(p_250719_, "rendertype_item_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY),
                    p_172650_0_ -> rendertypeItemEntityTranslucentCullShader = p_172650_0_
                )
            );
            list1.add(
                Pair.of(
                    new ShaderInstance(p_250719_, "rendertype_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY), p_172647_0_ -> rendertypeEntityTranslucentCullShader = p_172647_0_
                )
            );
            list1.add(
                Pair.of(new ShaderInstance(p_250719_, "rendertype_entity_translucent", DefaultVertexFormat.NEW_ENTITY), p_172644_0_ -> rendertypeEntityTranslucentShader = p_172644_0_)
            );
            list1.add(
                Pair.of(
                    new ShaderInstance(p_250719_, "rendertype_entity_translucent_emissive", DefaultVertexFormat.NEW_ENTITY),
                    p_172641_0_ -> rendertypeEntityTranslucentEmissiveShader = p_172641_0_
                )
            );
            list1.add(
                Pair.of(new ShaderInstance(p_250719_, "rendertype_entity_smooth_cutout", DefaultVertexFormat.NEW_ENTITY), p_172638_0_ -> rendertypeEntitySmoothCutoutShader = p_172638_0_)
            );
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_beacon_beam", DefaultVertexFormat.BLOCK), p_172839_0_ -> rendertypeBeaconBeamShader = p_172839_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_entity_decal", DefaultVertexFormat.NEW_ENTITY), p_172836_0_ -> rendertypeEntityDecalShader = p_172836_0_));
            list1.add(
                Pair.of(new ShaderInstance(p_250719_, "rendertype_entity_no_outline", DefaultVertexFormat.NEW_ENTITY), p_172833_0_ -> rendertypeEntityNoOutlineShader = p_172833_0_)
            );
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_entity_shadow", DefaultVertexFormat.NEW_ENTITY), p_172830_0_ -> rendertypeEntityShadowShader = p_172830_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_entity_alpha", DefaultVertexFormat.NEW_ENTITY), p_172827_0_ -> rendertypeEntityAlphaShader = p_172827_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_eyes", DefaultVertexFormat.NEW_ENTITY), p_172824_0_ -> rendertypeEyesShader = p_172824_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_energy_swirl", DefaultVertexFormat.NEW_ENTITY), p_172821_0_ -> rendertypeEnergySwirlShader = p_172821_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_leash", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP), p_172818_0_ -> rendertypeLeashShader = p_172818_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_water_mask", DefaultVertexFormat.POSITION), p_172815_0_ -> rendertypeWaterMaskShader = p_172815_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_outline", DefaultVertexFormat.POSITION_TEX_COLOR), p_172812_0_ -> rendertypeOutlineShader = p_172812_0_));
            list1.add(
                Pair.of(new ShaderInstance(p_250719_, "rendertype_armor_entity_glint", DefaultVertexFormat.POSITION_TEX), p_172806_0_ -> rendertypeArmorEntityGlintShader = p_172806_0_)
            );
            list1.add(
                Pair.of(new ShaderInstance(p_250719_, "rendertype_glint_translucent", DefaultVertexFormat.POSITION_TEX), p_172804_0_ -> rendertypeGlintTranslucentShader = p_172804_0_)
            );
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_glint", DefaultVertexFormat.POSITION_TEX), p_172802_0_ -> rendertypeGlintShader = p_172802_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_entity_glint", DefaultVertexFormat.POSITION_TEX), p_172798_0_ -> rendertypeEntityGlintShader = p_172798_0_));
            list1.add(
                Pair.of(new ShaderInstance(p_250719_, "rendertype_entity_glint_direct", DefaultVertexFormat.POSITION_TEX), p_172795_0_ -> rendertypeEntityGlintDirectShader = p_172795_0_)
            );
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), p_172793_0_ -> rendertypeTextShader = p_172793_0_));
            list1.add(
                Pair.of(new ShaderInstance(p_250719_, "rendertype_text_background", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP), p_268793_0_ -> rendertypeTextBackgroundShader = p_268793_0_)
            );
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_text_intensity", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), p_172791_0_ -> rendertypeTextIntensityShader = p_172791_0_));
            list1.add(
                Pair.of(new ShaderInstance(p_250719_, "rendertype_text_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), p_172788_0_ -> rendertypeTextSeeThroughShader = p_172788_0_)
            );
            list1.add(
                Pair.of(
                    new ShaderInstance(p_250719_, "rendertype_text_background_see_through", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
                    p_268792_0_ -> rendertypeTextBackgroundSeeThroughShader = p_268792_0_
                )
            );
            list1.add(
                Pair.of(
                    new ShaderInstance(p_250719_, "rendertype_text_intensity_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
                    p_172786_0_ -> rendertypeTextIntensitySeeThroughShader = p_172786_0_
                )
            );
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_lightning", DefaultVertexFormat.POSITION_COLOR), p_172784_0_ -> rendertypeLightningShader = p_172784_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_tripwire", DefaultVertexFormat.BLOCK), p_172781_0_ -> rendertypeTripwireShader = p_172781_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_end_portal", DefaultVertexFormat.POSITION), p_172777_0_ -> rendertypeEndPortalShader = p_172777_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_end_gateway", DefaultVertexFormat.POSITION), p_172773_0_ -> rendertypeEndGatewayShader = p_172773_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_clouds", DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL), p_317418_0_ -> rendertypeCloudsShader = p_317418_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL), p_172732_0_ -> rendertypeLinesShader = p_172732_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_crumbling", DefaultVertexFormat.BLOCK), p_234229_0_ -> rendertypeCrumblingShader = p_234229_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_gui", DefaultVertexFormat.POSITION_COLOR), p_285677_0_ -> rendertypeGuiShader = p_285677_0_));
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_gui_overlay", DefaultVertexFormat.POSITION_COLOR), p_285675_0_ -> rendertypeGuiOverlayShader = p_285675_0_));
            list1.add(
                Pair.of(new ShaderInstance(p_250719_, "rendertype_gui_text_highlight", DefaultVertexFormat.POSITION_COLOR), p_285674_0_ -> rendertypeGuiTextHighlightShader = p_285674_0_)
            );
            list1.add(
                Pair.of(
                    new ShaderInstance(p_250719_, "rendertype_gui_ghost_recipe_overlay", DefaultVertexFormat.POSITION_COLOR), p_285676_0_ -> rendertypeGuiGhostRecipeOverlayShader = p_285676_0_
                )
            );
            list1.add(Pair.of(new ShaderInstance(p_250719_, "rendertype_breeze_wind", DefaultVertexFormat.NEW_ENTITY), p_304052_0_ -> rendertypeBreezeWindShader = p_304052_0_));
            Reflector.ForgeEventFactoryClient_onRegisterShaders.callVoid(p_250719_, list1);
            this.loadBlurEffect(p_250719_);
        }
        catch (IOException ioexception)
        {
            list1.forEach(pairIn -> pairIn.getFirst().close());
            throw new RuntimeException("could not reload shaders", ioexception);
        }

        this.shutdownShaders();
        list1.forEach(pairIn ->
        {
            ShaderInstance shaderinstance = pairIn.getFirst();
            this.shaders.put(shaderinstance.getName(), shaderinstance);
            pairIn.getSecond().accept(shaderinstance);
        });
    }

    private void shutdownShaders()
    {
        RenderSystem.assertOnRenderThread();
        this.shaders.values().forEach(ShaderInstance::close);
        this.shaders.clear();
    }

    @Nullable
    public ShaderInstance getShader(@Nullable String p_172735_)
    {
        return p_172735_ == null ? null : this.shaders.get(p_172735_);
    }

    public void tick()
    {
        this.tickFov();
        this.lightTexture.tick();

        if (this.minecraft.getCameraEntity() == null)
        {
            this.minecraft.setCameraEntity(this.minecraft.player);
        }

        this.mainCamera.tick();
        this.itemInHandRenderer.tick();
        this.confusionAnimationTick++;

        if (this.minecraft.level.tickRateManager().runsNormally())
        {
            this.minecraft.levelRenderer.tickRain(this.mainCamera);
            this.darkenWorldAmountO = this.darkenWorldAmount;

            if (this.minecraft.gui.getBossOverlay().shouldDarkenScreen())
            {
                this.darkenWorldAmount += 0.05F;

                if (this.darkenWorldAmount > 1.0F)
                {
                    this.darkenWorldAmount = 1.0F;
                }
            }
            else if (this.darkenWorldAmount > 0.0F)
            {
                this.darkenWorldAmount -= 0.0125F;
            }

            if (this.itemActivationTicks > 0)
            {
                this.itemActivationTicks--;

                if (this.itemActivationTicks == 0)
                {
                    this.itemActivationItem = null;
                }
            }
        }
    }

    @Nullable
    public PostChain currentEffect()
    {
        return this.postEffect;
    }

    public void resize(int p_109098_, int p_109099_)
    {
        if (this.postEffect != null)
        {
            this.postEffect.resize(p_109098_, p_109099_);
        }

        if (this.blurEffect != null)
        {
            this.blurEffect.resize(p_109098_, p_109099_);
        }

        this.minecraft.levelRenderer.resize(p_109098_, p_109099_);
    }

    public void pick(float p_109088_)
    {
        Entity entity = this.minecraft.getCameraEntity();

        if (entity != null && this.minecraft.level != null && this.minecraft.player != null)
        {
            this.minecraft.getProfiler().push("pick");
            double d0 = this.minecraft.player.blockInteractionRange();
            double d1 = this.minecraft.player.entityInteractionRange();
            HitResult hitresult = this.pick(entity, d0, d1, p_109088_);
            this.minecraft.hitResult = hitresult;
            this.minecraft.crosshairPickEntity = hitresult instanceof EntityHitResult entityhitresult ? entityhitresult.getEntity() : null;
            this.minecraft.getProfiler().pop();
        }
    }

    private HitResult pick(Entity p_328026_, double p_328198_, double p_332188_, float p_336363_)
    {
        double d0 = Math.max(p_328198_, p_332188_);
        double d1 = Mth.square(d0);
        Vec3 vec3 = p_328026_.getEyePosition(p_336363_);
        HitResult hitresult = p_328026_.pick(d0, p_336363_, false);
        double d2 = hitresult.getLocation().distanceToSqr(vec3);

        if (hitresult.getType() != HitResult.Type.MISS)
        {
            d1 = d2;
            d0 = Math.sqrt(d2);
        }

        Vec3 vec31 = p_328026_.getViewVector(p_336363_);
        Vec3 vec32 = vec3.add(vec31.x * d0, vec31.y * d0, vec31.z * d0);
        float f = 1.0F;
        AABB aabb = p_328026_.getBoundingBox().expandTowards(vec31.scale(d0)).inflate(1.0, 1.0, 1.0);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(p_328026_, vec3, vec32, aabb, entityIn -> !entityIn.isSpectator() && entityIn.isPickable(), d1);
        return entityhitresult != null && entityhitresult.getLocation().distanceToSqr(vec3) < d2
               ? filterHitResult(entityhitresult, vec3, p_332188_)
               : filterHitResult(hitresult, vec3, p_328198_);
    }

    private static HitResult filterHitResult(HitResult p_333216_, Vec3 p_336005_, double p_334240_)
    {
        Vec3 vec3 = p_333216_.getLocation();

        if (!vec3.closerThan(p_336005_, p_334240_))
        {
            Vec3 vec31 = p_333216_.getLocation();
            Direction direction = Direction.getNearest(
                                      vec31.x - p_336005_.x, vec31.y - p_336005_.y, vec31.z - p_336005_.z
                                  );
            return BlockHitResult.miss(vec31, direction, BlockPos.containing(vec31));
        }
        else
        {
            return p_333216_;
        }
    }

    private void tickFov()
    {
        float f = 1.0F;

        if (this.minecraft.getCameraEntity() instanceof AbstractClientPlayer abstractclientplayer)
        {
            f = abstractclientplayer.getFieldOfViewModifier();
        }

        this.oldFov = this.fov;
        this.fov = this.fov + (f - this.fov) * 0.5F;

        if (this.fov > 1.5F)
        {
            this.fov = 1.5F;
        }

        if (this.fov < 0.1F)
        {
            this.fov = 0.1F;
        }
    }

    public double getFov(Camera p_109142_, float p_109143_, boolean p_109144_)
    {
        if (this.panoramicMode)
        {
            return 90.0;
        }
        else
        {
            double d0 = 70.0;

            if (p_109144_)
            {
                d0 = (double)this.minecraft.options.fov().get().intValue();
                boolean flag = this.minecraft.getCameraEntity() instanceof AbstractClientPlayer && ((AbstractClientPlayer)this.minecraft.getCameraEntity()).isScoping();

                if (Config.isDynamicFov() || flag)
                {
                    d0 *= (double)Mth.lerp(p_109143_, this.oldFov, this.fov);
                }
            }

            boolean flag1 = false;

            if (this.minecraft.screen == null)
            {
                flag1 = this.minecraft.options.ofKeyBindZoom.isDown();
            }

            if (flag1)
            {
                if (!Config.zoomMode)
                {
                    Config.zoomMode = true;
                    Config.zoomSmoothCamera = this.minecraft.options.smoothCamera;
                    this.minecraft.options.smoothCamera = true;
                    this.minecraft.levelRenderer.needsUpdate();
                }

                if (Config.zoomMode)
                {
                    d0 /= 4.0;
                }
            }
            else if (Config.zoomMode)
            {
                Config.zoomMode = false;
                this.minecraft.options.smoothCamera = Config.zoomSmoothCamera;
                this.minecraft.levelRenderer.needsUpdate();
            }

            if (p_109142_.getEntity() instanceof LivingEntity && ((LivingEntity)p_109142_.getEntity()).isDeadOrDying())
            {
                float f = Math.min((float)((LivingEntity)p_109142_.getEntity()).deathTime + p_109143_, 20.0F);
                d0 /= (double)((1.0F - 500.0F / (f + 500.0F)) * 2.0F + 1.0F);
            }

            FogType fogtype = p_109142_.getFluidInCamera();

            if (fogtype == FogType.LAVA || fogtype == FogType.WATER)
            {
                d0 *= Mth.lerp(this.minecraft.options.fovEffectScale().get(), 1.0, 0.85714287F);
            }

            if (Reflector.ForgeEventFactoryClient_fireComputeFov.exists())
            {
                ViewportEvent.ComputeFov viewportevent$computefov = (ViewportEvent.ComputeFov)Reflector.ForgeEventFactoryClient_fireComputeFov
                        .call(this, p_109142_, p_109143_, d0, p_109144_);

                if (viewportevent$computefov != null)
                {
                    return viewportevent$computefov.getFOV();
                }
            }

            return d0;
        }
    }

    private void bobHurt(PoseStack p_109118_, float p_109119_)
    {
        if (this.minecraft.getCameraEntity() instanceof LivingEntity livingentity)
        {
            float f2 = (float)livingentity.hurtTime - p_109119_;

            if (livingentity.isDeadOrDying())
            {
                float f = Math.min((float)livingentity.deathTime + p_109119_, 20.0F);
                p_109118_.mulPose(Axis.ZP.rotationDegrees(40.0F - 8000.0F / (f + 200.0F)));
            }

            if (f2 < 0.0F)
            {
                return;
            }

            f2 /= (float)livingentity.hurtDuration;
            f2 = Mth.sin(f2 * f2 * f2 * f2 * (float) Math.PI);
            float f3 = livingentity.getHurtDir();
            p_109118_.mulPose(Axis.YP.rotationDegrees(-f3));
            float f1 = (float)((double)(-f2) * 14.0 * this.minecraft.options.damageTiltStrength().get());
            p_109118_.mulPose(Axis.ZP.rotationDegrees(f1));
            p_109118_.mulPose(Axis.YP.rotationDegrees(f3));
        }
    }

    private void bobView(PoseStack p_109139_, float p_109140_)
    {
        if (this.minecraft.getCameraEntity() instanceof Player)
        {
            Player player = (Player)this.minecraft.getCameraEntity();
            float f = player.walkDist - player.walkDistO;
            float f1 = -(player.walkDist + f * p_109140_);
            float f2 = Mth.lerp(p_109140_, player.oBob, player.bob);
            p_109139_.translate(Mth.sin(f1 * (float) Math.PI) * f2 * 0.5F, -Math.abs(Mth.cos(f1 * (float) Math.PI) * f2), 0.0F);
            p_109139_.mulPose(Axis.ZP.rotationDegrees(Mth.sin(f1 * (float) Math.PI) * f2 * 3.0F));
            p_109139_.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F));
        }
    }

    public void renderZoomed(float p_172719_, float p_172720_, float p_172721_)
    {
        this.zoom = p_172719_;
        this.zoomX = p_172720_;
        this.zoomY = p_172721_;
        this.setRenderBlockOutline(false);
        this.setRenderHand(false);
        this.renderLevel(DeltaTracker.ZERO);
        this.zoom = 1.0F;
    }

    private void renderItemInHand(Camera p_109122_, float p_109123_, Matrix4f p_331664_)
    {
        this.renderHand(p_109122_, p_109123_, p_331664_, true, true, false);
    }

    public void renderHand(
        Camera activeRenderInfoIn, float partialTicks, Matrix4f matrixStackIn, boolean renderItem, boolean renderOverlay, boolean renderTranslucent
    )
    {
        if (!this.panoramicMode)
        {
            Shaders.beginRenderFirstPersonHand(renderTranslucent);
            this.resetProjectionMatrix(this.getProjectionMatrix(this.getFov(activeRenderInfoIn, partialTicks, false)));
            PoseStack posestack = new PoseStack();
            boolean flag = false;

            if (renderItem)
            {
                posestack.pushPose();
                posestack.mulPose(matrixStackIn.invert(new Matrix4f()));
                Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
                matrix4fstack.pushMatrix().set(matrixStackIn);
                RenderSystem.applyModelViewMatrix();
                this.bobHurt(posestack, partialTicks);

                if (this.minecraft.options.bobView().get())
                {
                    this.bobView(posestack, partialTicks);
                }

                flag = this.minecraft.getCameraEntity() instanceof LivingEntity && ((LivingEntity)this.minecraft.getCameraEntity()).isSleeping();

                if (this.minecraft.options.getCameraType().isFirstPerson()
                        && !flag
                        && !this.minecraft.options.hideGui
                        && this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR)
                {
                    this.lightTexture.turnOnLightLayer();

                    if (Config.isShaders())
                    {
                        ShadersRender.renderItemFP(
                            this.itemInHandRenderer,
                            partialTicks,
                            posestack,
                            this.renderBuffers.bufferSource(),
                            this.minecraft.player,
                            this.minecraft.getEntityRenderDispatcher().getPackedLightCoords(this.minecraft.player, partialTicks),
                            renderTranslucent
                        );
                    }
                    else
                    {
                        this.itemInHandRenderer
                        .renderHandsWithItems(
                            partialTicks,
                            posestack,
                            this.renderBuffers.bufferSource(),
                            this.minecraft.player,
                            this.minecraft.getEntityRenderDispatcher().getPackedLightCoords(this.minecraft.player, partialTicks)
                        );
                    }

                    this.lightTexture.turnOffLightLayer();
                }

                matrix4fstack.popMatrix();
                RenderSystem.applyModelViewMatrix();
                posestack.popPose();
            }

            Shaders.endRenderFirstPersonHand();

            if (!renderOverlay)
            {
                return;
            }

            this.lightTexture.turnOffLightLayer();

            if (this.minecraft.options.getCameraType().isFirstPerson() && !flag)
            {
                ScreenEffectRenderer.renderScreenEffect(this.minecraft, posestack);
            }
        }
    }

    public void resetProjectionMatrix(Matrix4f p_253668_)
    {
        RenderSystem.setProjectionMatrix(p_253668_, VertexSorting.DISTANCE_TO_ORIGIN);
    }

    public Matrix4f getProjectionMatrix(double p_254507_)
    {
        Matrix4f matrix4f = new Matrix4f();

        if (Config.isShaders() && Shaders.isRenderingFirstPersonHand())
        {
            Shaders.applyHandDepth(matrix4f);
        }

        this.clipDistance = this.renderDistance + 1024.0F;

        if (this.zoom != 1.0F)
        {
            matrix4f.translate(this.zoomX, -this.zoomY, 0.0F);
            matrix4f.scale(this.zoom, this.zoom, 1.0F);
        }

        return matrix4f.perspective(
                   (float)(p_254507_ * (float)(Math.PI / 180.0)),
                   (float)this.minecraft.getWindow().getWidth() / (float)this.minecraft.getWindow().getHeight(),
                   0.05F,
                   this.clipDistance
               );
    }

    public float getDepthFar()
    {
        return this.renderDistance * 4.0F;
    }

    public static float getNightVisionScale(LivingEntity p_109109_, float p_109110_)
    {
        MobEffectInstance mobeffectinstance = p_109109_.getEffect(MobEffects.NIGHT_VISION);
        return !mobeffectinstance.endsWithin(200)
               ? 1.0F
               : 0.7F + Mth.sin(((float)mobeffectinstance.getDuration() - p_109110_) * (float) Math.PI * 0.2F) * 0.3F;
    }

    public void render(DeltaTracker p_343467_, boolean p_109096_)
    {
        this.frameInit();

        if (!this.minecraft.isWindowActive()
                && this.minecraft.options.pauseOnLostFocus
                && (!this.minecraft.options.touchscreen().get() || !this.minecraft.mouseHandler.isRightPressed()))
        {
            if (Util.getMillis() - this.lastActiveTime > 500L)
            {
                this.minecraft.pauseGame(false);
            }
        }
        else
        {
            this.lastActiveTime = Util.getMillis();
        }

        if (!this.minecraft.noRender)
        {
            boolean flag = this.minecraft.isGameLoadFinished();
            int i = (int)(this.minecraft.mouseHandler.xpos() * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth());
            int j = (int)(this.minecraft.mouseHandler.ypos() * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight());

            if (flag && p_109096_ && this.minecraft.level != null && !Config.isReloadingResources())
            {
                this.minecraft.getProfiler().push("level");
                this.renderLevel(p_343467_);
                this.tryTakeScreenshotIfNeeded();
                this.minecraft.levelRenderer.doEntityOutline();

                if (this.postEffect != null && this.effectActive)
                {
                    RenderSystem.disableBlend();
                    RenderSystem.disableDepthTest();
                    RenderSystem.resetTextureMatrix();
                    this.postEffect.process(p_343467_.getGameTimeDeltaTicks());
                    RenderSystem.enableTexture();
                }

                this.minecraft.getMainRenderTarget().bindWrite(true);
            }
            else
            {
                RenderSystem.viewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
            }

            Window window = this.minecraft.getWindow();
            RenderSystem.clear(256, Minecraft.ON_OSX);
            float f = Reflector.ForgeHooksClient_getGuiFarPlane.exists() ? Reflector.ForgeHooksClient_getGuiFarPlane.callFloat() : 21000.0F;
            Matrix4f matrix4f = new Matrix4f()
            .setOrtho(
                0.0F, (float)((double)window.getWidth() / window.getGuiScale()), (float)((double)window.getHeight() / window.getGuiScale()), 0.0F, 1000.0F, f
            );
            RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
            Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
            matrix4fstack.pushMatrix();
            float f1 = Reflector.ForgeHooksClient_getGuiFarPlane.exists() ? 1000.0F - f : -11000.0F;
            matrix4fstack.translation(0.0F, 0.0F, f1);
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
            GuiGraphics guigraphics = new GuiGraphics(this.minecraft, this.renderBuffers.bufferSource());

            if (this.lightTexture.isCustom())
            {
                this.lightTexture.setAllowed(false);
            }

            if (flag && p_109096_ && this.minecraft.level != null)
            {
                this.minecraft.getProfiler().popPush("gui");

                if (this.minecraft.player != null)
                {
                    float f2 = Mth.lerp(p_343467_.getGameTimeDeltaPartialTick(false), this.minecraft.player.oSpinningEffectIntensity, this.minecraft.player.spinningEffectIntensity);
                    float f3 = this.minecraft.options.screenEffectScale().get().floatValue();

                    if (f2 > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONFUSION) && f3 < 1.0F)
                    {
                        this.renderConfusionOverlay(guigraphics, f2 * (1.0F - f3));
                    }
                }

                if (!this.minecraft.options.hideGui)
                {
                    this.renderItemActivationAnimation(guigraphics, p_343467_.getGameTimeDeltaPartialTick(false));
                }

                this.minecraft.gui.render(guigraphics, p_343467_);
                RenderSystem.clear(256, Minecraft.ON_OSX);
                this.minecraft.getProfiler().pop();
            }

            if (this.guiLoadingVisible != (this.minecraft.getOverlay() != null))
            {
                if (this.minecraft.getOverlay() != null)
                {
                    LoadingOverlay.registerTextures(this.minecraft);

                    if (this.minecraft.getOverlay() instanceof LoadingOverlay)
                    {
                        LoadingOverlay loadingoverlay = (LoadingOverlay)this.minecraft.getOverlay();
                        loadingoverlay.update();
                    }
                }

                this.guiLoadingVisible = this.minecraft.getOverlay() != null;
            }

            if (this.minecraft.getOverlay() != null)
            {
                try
                {
                    this.minecraft.getOverlay().render(guigraphics, i, j, p_343467_.getRealtimeDeltaTicks());
                }
                catch (Throwable throwable1)
                {
                    CrashReport crashreport2 = CrashReport.forThrowable(throwable1, "Rendering overlay");
                    CrashReportCategory crashreportcategory2 = crashreport2.addCategory("Overlay render details");
                    crashreportcategory2.setDetail("Overlay name", () -> this.minecraft.getOverlay().getClass().getCanonicalName());
                    throw new ReportedException(crashreport2);
                }
            }
            else if (flag && this.minecraft.screen != null)
            {
                try
                {
                    if (Config.isCustomEntityModels())
                    {
                        CustomEntityModels.onRenderScreen(this.minecraft.screen);
                    }

                    if (Reflector.ForgeHooksClient_drawScreen.exists())
                    {
                        Reflector.callVoid(Reflector.ForgeHooksClient_drawScreen, this.minecraft.screen, guigraphics, i, j, p_343467_.getRealtimeDeltaTicks());
                    }
                    else
                    {
                        this.minecraft.screen.renderWithTooltip(guigraphics, i, j, p_343467_.getRealtimeDeltaTicks());
                    }
                }
                catch (Throwable throwable2)
                {
                    CrashReport crashreport = CrashReport.forThrowable(throwable2, "Rendering screen");
                    CrashReportCategory crashreportcategory = crashreport.addCategory("Screen render details");
                    crashreportcategory.setDetail("Screen name", () -> this.minecraft.screen.getClass().getCanonicalName());
                    crashreportcategory.setDetail(
                        "Mouse location",
                        () -> String.format(
                            Locale.ROOT,
                            "Scaled: (%d, %d). Absolute: (%f, %f)",
                            i,
                            j,
                            this.minecraft.mouseHandler.xpos(),
                            this.minecraft.mouseHandler.ypos()
                        )
                    );
                    crashreportcategory.setDetail(
                        "Screen size",
                        () -> String.format(
                            Locale.ROOT,
                            "Scaled: (%d, %d). Absolute: (%d, %d). Scale factor of %f",
                            this.minecraft.getWindow().getGuiScaledWidth(),
                            this.minecraft.getWindow().getGuiScaledHeight(),
                            this.minecraft.getWindow().getWidth(),
                            this.minecraft.getWindow().getHeight(),
                            this.minecraft.getWindow().getGuiScale()
                        )
                    );
                    throw new ReportedException(crashreport);
                }

                try
                {
                    if (this.minecraft.screen != null)
                    {
                        this.minecraft.screen.handleDelayedNarration();
                    }
                }
                catch (Throwable throwable1)
                {
                    CrashReport crashreport1 = CrashReport.forThrowable(throwable1, "Narrating screen");
                    CrashReportCategory crashreportcategory1 = crashreport1.addCategory("Screen details");
                    crashreportcategory1.setDetail("Screen name", () -> this.minecraft.screen.getClass().getCanonicalName());
                    throw new ReportedException(crashreport1);
                }
            }

            if (flag && p_109096_ && this.minecraft.level != null)
            {
                this.minecraft.gui.renderSavingIndicator(guigraphics, p_343467_);
            }

            if (flag)
            {
                this.minecraft.getProfiler().push("toasts");
                this.minecraft.getToasts().render(guigraphics);
                this.minecraft.getProfiler().pop();
            }

            guigraphics.flush();
            matrix4fstack.popMatrix();
            RenderSystem.applyModelViewMatrix();
            this.lightTexture.setAllowed(true);
        }

        this.frameFinish();
        this.waitForServerThread();
        MemoryMonitor.update();
        Lagometer.updateLagometer();
    }

    private void tryTakeScreenshotIfNeeded()
    {
        if (!this.hasWorldScreenshot && this.minecraft.isLocalServer())
        {
            long i = Util.getMillis();

            if (i - this.lastScreenshotAttempt >= 1000L)
            {
                this.lastScreenshotAttempt = i;
                IntegratedServer integratedserver = this.minecraft.getSingleplayerServer();

                if (integratedserver != null && !integratedserver.isStopped())
                {
                    integratedserver.getWorldScreenshotFile().ifPresent(pathIn ->
                    {
                        if (Files.isRegularFile(pathIn))
                        {
                            this.hasWorldScreenshot = true;
                        }
                        else {
                            this.takeAutoScreenshot(pathIn);
                        }
                    });
                }
            }
        }
    }

    private void takeAutoScreenshot(Path p_182643_)
    {
        if (this.minecraft.levelRenderer.countRenderedSections() > 10 && this.minecraft.levelRenderer.hasRenderedAllSections())
        {
            NativeImage nativeimage = Screenshot.takeScreenshot(this.minecraft.getMainRenderTarget());
            Util.ioPool().execute(() ->
            {
                int i = nativeimage.getWidth();
                int j = nativeimage.getHeight();
                int k = 0;
                int l = 0;

                if (i > j)
                {
                    k = (i - j) / 2;
                    i = j;
                }
                else {
                    l = (j - i) / 2;
                    j = i;
                }

                try (NativeImage nativeimage1 = new NativeImage(64, 64, false))
                {
                    nativeimage.resizeSubRectTo(k, l, i, j, nativeimage1);
                    nativeimage1.writeToFile(p_182643_);
                }
                catch (IOException ioexception1)
                {
                    LOGGER.warn("Couldn't save auto screenshot", (Throwable)ioexception1);
                }
                finally {
                    nativeimage.close();
                }
            });
        }
    }

    private boolean shouldRenderBlockOutline()
    {
        if (!this.renderBlockOutline)
        {
            return false;
        }
        else
        {
            Entity entity = this.minecraft.getCameraEntity();
            boolean flag = entity instanceof Player && !this.minecraft.options.hideGui;

            if (flag && !((Player)entity).getAbilities().mayBuild)
            {
                ItemStack itemstack = ((LivingEntity)entity).getMainHandItem();
                HitResult hitresult = this.minecraft.hitResult;

                if (hitresult != null && hitresult.getType() == HitResult.Type.BLOCK)
                {
                    BlockPos blockpos = ((BlockHitResult)hitresult).getBlockPos();
                    BlockState blockstate = this.minecraft.level.getBlockState(blockpos);

                    if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR)
                    {
                        flag = blockstate.getMenuProvider(this.minecraft.level, blockpos) != null;
                    }
                    else
                    {
                        BlockInWorld blockinworld = new BlockInWorld(this.minecraft.level, blockpos, false);
                        Registry<Block> registry = this.minecraft.level.registryAccess().registryOrThrow(Registries.BLOCK);
                        flag = !itemstack.isEmpty() && (itemstack.canBreakBlockInAdventureMode(blockinworld) || itemstack.canPlaceOnBlockInAdventureMode(blockinworld));
                    }
                }
            }

            return flag;
        }
    }

    public void renderLevel(DeltaTracker p_342230_)
    {
        float f = p_342230_.getGameTimeDeltaPartialTick(true);
        this.lightTexture.updateLightTexture(f);

        if (this.minecraft.getCameraEntity() == null)
        {
            this.minecraft.setCameraEntity(this.minecraft.player);
        }

        this.pick(f);

        if (Config.isShaders())
        {
            Shaders.beginRender(this.minecraft, this.mainCamera, f);
        }

        this.minecraft.getProfiler().push("center");
        boolean flag = Config.isShaders();

        if (flag)
        {
            Shaders.beginRenderPass(f);
        }

        boolean flag1 = this.shouldRenderBlockOutline();
        this.minecraft.getProfiler().popPush("camera");
        Camera camera = this.mainCamera;
        Entity entity = (Entity)(this.minecraft.getCameraEntity() == null ? this.minecraft.player : this.minecraft.getCameraEntity());
        float f1 = this.minecraft.level.tickRateManager().isEntityFrozen(entity) ? 1.0F : f;
        camera.setup(this.minecraft.level, entity, !this.minecraft.options.getCameraType().isFirstPerson(), this.minecraft.options.getCameraType().isMirrored(), f1);
        this.renderDistance = (float)(this.minecraft.options.getEffectiveRenderDistance() * 16);
        double d0 = this.getFov(camera, f, true);
        Matrix4f matrix4f = this.getProjectionMatrix(d0);
        Matrix4f matrix4f1 = matrix4f;

        if (Shaders.isEffectsModelView())
        {
            matrix4f = new Matrix4f();
        }

        PoseStack posestack = new PoseStack();
        this.bobHurt(posestack, camera.getPartialTickTime());

        if (this.minecraft.options.bobView().get())
        {
            this.bobView(posestack, camera.getPartialTickTime());
        }

        matrix4f.mul(posestack.last().pose());
        float f2 = this.minecraft.options.screenEffectScale().get().floatValue();
        float f3 = Mth.lerp(f, this.minecraft.player.oSpinningEffectIntensity, this.minecraft.player.spinningEffectIntensity) * f2 * f2;

        if (f3 > 0.0F)
        {
            int i = this.minecraft.player.hasEffect(MobEffects.CONFUSION) ? 7 : 20;
            float f4 = 5.0F / (f3 * f3 + 5.0F) - f3 * 0.04F;
            f4 *= f4;
            Vector3f vector3f = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);
            float f5 = ((float)this.confusionAnimationTick + f) * (float)i * (float)(Math.PI / 180.0);
            matrix4f.rotate(f5, vector3f);
            matrix4f.scale(1.0F / f4, 1.0F, 1.0F);
            matrix4f.rotate(-f5, vector3f);
        }

        Matrix4f matrix4f2 = matrix4f;

        if (Shaders.isEffectsModelView())
        {
            matrix4f = matrix4f1;
        }

        this.resetProjectionMatrix(matrix4f);

        if (Reflector.ForgeEventFactoryClient_fireComputeCameraAngles.exists())
        {
            ViewportEvent.ComputeCameraAngles viewportevent$computecameraangles = (ViewportEvent.ComputeCameraAngles)Reflector.ForgeEventFactoryClient_fireComputeCameraAngles
                    .call(this, camera, f);
            camera.setRotation(
                viewportevent$computecameraangles.getYaw(), viewportevent$computecameraangles.getPitch(), viewportevent$computecameraangles.getRoll()
            );
        }

        Quaternionf quaternionf = camera.rotation().conjugate(new Quaternionf());
        Matrix4f matrix4f3 = new Matrix4f().rotation(quaternionf);

        if (Shaders.isEffectsModelView())
        {
            matrix4f3 = matrix4f2.mul(matrix4f3);
        }

        this.minecraft
        .levelRenderer
        .prepareCullFrustum(camera.getPosition(), matrix4f3, this.getProjectionMatrix(Math.max(d0, (double)this.minecraft.options.fov().get().intValue())));
        this.minecraft.levelRenderer.renderLevel(p_342230_, flag1, camera, this, this.lightTexture, matrix4f3, matrix4f);
        this.minecraft.getProfiler().popPush("forge_render_last");
        ReflectorForge.dispatchRenderStageS(
            Reflector.RenderLevelStageEvent_Stage_AFTER_LEVEL,
            this.minecraft.levelRenderer,
            matrix4f3,
            matrix4f,
            this.minecraft.levelRenderer.getTicks(),
            camera,
            this.minecraft.levelRenderer.getFrustum()
        );
        this.minecraft.getProfiler().popPush("hand");

        if (this.renderHand && !Shaders.isShadowPass)
        {
            if (flag)
            {
                ShadersRender.renderHand1(this, matrix4f3, camera, f);
                Shaders.renderCompositeFinal();
            }

            RenderSystem.clear(256, Minecraft.ON_OSX);

            if (flag)
            {
                ShadersRender.renderFPOverlay(this, matrix4f3, camera, f);
            }
            else
            {
                this.renderItemInHand(camera, f, matrix4f3);
            }
        }

        if (flag)
        {
            Shaders.endRender();
        }

        this.minecraft.getProfiler().pop();
    }

    public void resetData()
    {
        this.itemActivationItem = null;
        this.mapRenderer.resetData();
        this.mainCamera.reset();
        this.hasWorldScreenshot = false;
    }

    public MapRenderer getMapRenderer()
    {
        return this.mapRenderer;
    }

    private void waitForServerThread()
    {
        this.serverWaitTimeCurrent = 0;

        if (!Config.isSmoothWorld() || !Config.isSingleProcessor())
        {
            this.lastServerTime = 0L;
            this.lastServerTicks = 0;
        }
        else if (this.minecraft.isLocalServer())
        {
            IntegratedServer integratedserver = this.minecraft.getSingleplayerServer();

            if (integratedserver != null)
            {
                boolean flag = this.minecraft.isPaused();

                if (!flag && !(this.minecraft.screen instanceof ReceivingLevelScreen))
                {
                    if (this.serverWaitTime > 0)
                    {
                        Lagometer.timerServer.start();
                        Config.sleep((long)this.serverWaitTime);
                        Lagometer.timerServer.end();
                        this.serverWaitTimeCurrent = this.serverWaitTime;
                    }

                    long i = System.nanoTime() / 1000000L;

                    if (this.lastServerTime != 0L && this.lastServerTicks != 0)
                    {
                        long j = i - this.lastServerTime;

                        if (j < 0L)
                        {
                            this.lastServerTime = i;
                            j = 0L;
                        }

                        if (j >= 50L)
                        {
                            this.lastServerTime = i;
                            int k = integratedserver.getTickCount();
                            int l = k - this.lastServerTicks;

                            if (l < 0)
                            {
                                this.lastServerTicks = k;
                                l = 0;
                            }

                            if (l < 1 && this.serverWaitTime < 100)
                            {
                                this.serverWaitTime += 2;
                            }

                            if (l > 1 && this.serverWaitTime > 0)
                            {
                                this.serverWaitTime--;
                            }

                            this.lastServerTicks = k;
                        }
                    }
                    else
                    {
                        this.lastServerTime = i;
                        this.lastServerTicks = integratedserver.getTickCount();
                        this.avgServerTickDiff = 1.0F;
                        this.avgServerTimeDiff = 50.0F;
                    }
                }
                else
                {
                    if (this.minecraft.screen instanceof ReceivingLevelScreen)
                    {
                        Config.sleep(20L);
                    }

                    this.lastServerTime = 0L;
                    this.lastServerTicks = 0;
                }
            }
        }
    }

    private void frameInit()
    {
        this.frameCount++;
        Config.frameStart();
        GlErrors.frameStart();

        if (!this.initialized)
        {
            ReflectorResolver.resolve();

            if (Config.getBitsOs() == 64 && Config.getBitsJre() == 32)
            {
                Config.setNotify64BitJava(true);
            }

            this.initialized = true;
        }

        Level level = this.minecraft.level;

        if (level != null)
        {
            if (Config.getNewRelease() != null)
            {
                String s = "HD_U".replace("HD_U", "HD Ultra").replace("L", "Light");
                String s1 = s + " " + Config.getNewRelease();
                MutableComponent mutablecomponent = Component.literal(
                                                        I18n.get("of.message.newVersion", "\u00ef\u00bf\u00bdn" + s1 + "\u00ef\u00bf\u00bdr")
                                                    );
                mutablecomponent.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://optifine.net/downloads")));
                this.minecraft.gui.getChat().addMessage(mutablecomponent);
                Config.setNewRelease(null);
            }

            if (Config.isNotify64BitJava())
            {
                Config.setNotify64BitJava(false);
                Component component = Component.literal(I18n.get("of.message.java64Bit"));
                this.minecraft.gui.getChat().addMessage(component);
            }
        }

        if (this.updatedWorld != level)
        {
            RandomEntities.worldChanged(this.updatedWorld, level);
            Config.updateThreadPriorities();
            this.lastServerTime = 0L;
            this.lastServerTicks = 0;
            this.updatedWorld = level;
        }

        if (!this.setFxaaShader(Shaders.configAntialiasingLevel))
        {
            Shaders.configAntialiasingLevel = 0;
        }

        if (this.minecraft.screen != null && this.minecraft.screen.getClass() == ChatScreen.class)
        {
            this.minecraft.setScreen(new GuiChatOF((ChatScreen)this.minecraft.screen));
        }
    }

    private void frameFinish()
    {
        if (this.minecraft.level != null && Config.isShowGlErrors() && TimedEvent.isActive("CheckGlErrorFrameFinish", 10000L))
        {
            int i = GlStateManager._getError();

            if (i != 0 && GlErrors.isEnabled(i))
            {
                String s = Config.getGlErrorString(i);
                Component component = Component.literal(I18n.get("of.message.openglError", i, s));
                this.minecraft.gui.getChat().addMessage(component);
            }
        }
    }

    public boolean setFxaaShader(int fxaaLevel)
    {
        if (!GLX.isUsingFBOs())
        {
            return false;
        }
        else if (this.postEffect != null && this.postEffect != this.fxaaShaders[2] && this.postEffect != this.fxaaShaders[4])
        {
            return true;
        }
        else if (fxaaLevel != 2 && fxaaLevel != 4)
        {
            if (this.postEffect == null)
            {
                return true;
            }
            else
            {
                this.postEffect.close();
                this.postEffect = null;
                return true;
            }
        }
        else if (this.postEffect != null && this.postEffect == this.fxaaShaders[fxaaLevel])
        {
            return true;
        }
        else if (this.minecraft.level == null)
        {
            return true;
        }
        else
        {
            this.loadEffect(new ResourceLocation("shaders/post/fxaa_of_" + fxaaLevel + "x.json"));
            this.fxaaShaders[fxaaLevel] = this.postEffect;
            return this.effectActive;
        }
    }

    public static float getRenderPartialTicks()
    {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }

    public int getFrameCount()
    {
        return this.frameCount;
    }

    public void displayItemActivation(ItemStack p_109114_)
    {
        this.itemActivationItem = p_109114_;
        this.itemActivationTicks = 40;
        this.itemActivationOffX = this.random.nextFloat() * 2.0F - 1.0F;
        this.itemActivationOffY = this.random.nextFloat() * 2.0F - 1.0F;
    }

    private void renderItemActivationAnimation(GuiGraphics p_342383_, float p_109103_)
    {
        if (this.itemActivationItem != null && this.itemActivationTicks > 0)
        {
            int i = 40 - this.itemActivationTicks;
            float f = ((float)i + p_109103_) / 40.0F;
            float f1 = f * f;
            float f2 = f * f1;
            float f3 = 10.25F * f2 * f1 - 24.95F * f1 * f1 + 25.5F * f2 - 13.8F * f1 + 4.0F * f;
            float f4 = f3 * (float) Math.PI;
            float f5 = this.itemActivationOffX * (float)(p_342383_.guiWidth() / 4);
            float f6 = this.itemActivationOffY * (float)(p_342383_.guiHeight() / 4);
            PoseStack posestack = new PoseStack();
            posestack.pushPose();
            posestack.translate(
                (float)(p_342383_.guiWidth() / 2) + f5 * Mth.abs(Mth.sin(f4 * 2.0F)),
                (float)(p_342383_.guiHeight() / 2) + f6 * Mth.abs(Mth.sin(f4 * 2.0F)),
                -50.0F
            );
            float f7 = 50.0F + 175.0F * Mth.sin(f4);
            posestack.scale(f7, -f7, f7);
            posestack.mulPose(Axis.YP.rotationDegrees(900.0F * Mth.abs(Mth.sin(f4))));
            posestack.mulPose(Axis.XP.rotationDegrees(6.0F * Mth.cos(f * 8.0F)));
            posestack.mulPose(Axis.ZP.rotationDegrees(6.0F * Mth.cos(f * 8.0F)));
            p_342383_.drawManaged(
                () -> this.minecraft
                .getItemRenderer()
                .renderStatic(
                    this.itemActivationItem,
                    ItemDisplayContext.FIXED,
                    15728880,
                    OverlayTexture.NO_OVERLAY,
                    posestack,
                    p_342383_.bufferSource(),
                    this.minecraft.level,
                    0
                )
            );
            posestack.popPose();
        }
    }

    private void renderConfusionOverlay(GuiGraphics p_282460_, float p_282656_)
    {
        int i = p_282460_.guiWidth();
        int j = p_282460_.guiHeight();
        p_282460_.pose().pushPose();
        float f = Mth.lerp(p_282656_, 2.0F, 1.0F);
        p_282460_.pose().translate((float)i / 2.0F, (float)j / 2.0F, 0.0F);
        p_282460_.pose().scale(f, f, f);
        p_282460_.pose().translate((float)(-i) / 2.0F, (float)(-j) / 2.0F, 0.0F);
        float f1 = 0.2F * p_282656_;
        float f2 = 0.4F * p_282656_;
        float f3 = 0.2F * p_282656_;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE
        );
        p_282460_.setColor(f1, f2, f3, 1.0F);
        p_282460_.blit(NAUSEA_LOCATION, 0, 0, -90, 0.0F, 0.0F, i, j, i, j);
        p_282460_.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        p_282460_.pose().popPose();
    }

    public Minecraft getMinecraft()
    {
        return this.minecraft;
    }

    public float getDarkenWorldAmount(float p_109132_)
    {
        return Mth.lerp(p_109132_, this.darkenWorldAmountO, this.darkenWorldAmount);
    }

    public float getRenderDistance()
    {
        return this.renderDistance;
    }

    public Camera getMainCamera()
    {
        return this.mainCamera;
    }

    public LightTexture lightTexture()
    {
        return this.lightTexture;
    }

    public OverlayTexture overlayTexture()
    {
        return this.overlayTexture;
    }

    @Nullable
    public static ShaderInstance getPositionShader()
    {
        return positionShader;
    }

    @Nullable
    public static ShaderInstance getPositionColorShader()
    {
        return positionColorShader;
    }

    @Nullable
    public static ShaderInstance getPositionTexShader()
    {
        return positionTexShader;
    }

    @Nullable
    public static ShaderInstance getPositionTexColorShader()
    {
        return positionTexColorShader;
    }

    @Nullable
    public static ShaderInstance getParticleShader()
    {
        return particleShader;
    }

    @Nullable
    public static ShaderInstance getPositionColorLightmapShader()
    {
        return positionColorLightmapShader;
    }

    @Nullable
    public static ShaderInstance getPositionColorTexLightmapShader()
    {
        return positionColorTexLightmapShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeSolidShader()
    {
        return rendertypeSolidShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeCutoutMippedShader()
    {
        return rendertypeCutoutMippedShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeCutoutShader()
    {
        return rendertypeCutoutShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeTranslucentShader()
    {
        return rendertypeTranslucentShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeTranslucentMovingBlockShader()
    {
        return rendertypeTranslucentMovingBlockShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeArmorCutoutNoCullShader()
    {
        return rendertypeArmorCutoutNoCullShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEntitySolidShader()
    {
        return rendertypeEntitySolidShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEntityCutoutShader()
    {
        return rendertypeEntityCutoutShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEntityCutoutNoCullShader()
    {
        return rendertypeEntityCutoutNoCullShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEntityCutoutNoCullZOffsetShader()
    {
        return rendertypeEntityCutoutNoCullZOffsetShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeItemEntityTranslucentCullShader()
    {
        return rendertypeItemEntityTranslucentCullShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEntityTranslucentCullShader()
    {
        return rendertypeEntityTranslucentCullShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEntityTranslucentShader()
    {
        return rendertypeEntityTranslucentShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEntityTranslucentEmissiveShader()
    {
        return rendertypeEntityTranslucentEmissiveShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEntitySmoothCutoutShader()
    {
        return rendertypeEntitySmoothCutoutShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeBeaconBeamShader()
    {
        return rendertypeBeaconBeamShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEntityDecalShader()
    {
        return rendertypeEntityDecalShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEntityNoOutlineShader()
    {
        return rendertypeEntityNoOutlineShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEntityShadowShader()
    {
        return rendertypeEntityShadowShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEntityAlphaShader()
    {
        return rendertypeEntityAlphaShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEyesShader()
    {
        return rendertypeEyesShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEnergySwirlShader()
    {
        return rendertypeEnergySwirlShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeBreezeWindShader()
    {
        return rendertypeBreezeWindShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeLeashShader()
    {
        return rendertypeLeashShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeWaterMaskShader()
    {
        return rendertypeWaterMaskShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeOutlineShader()
    {
        return rendertypeOutlineShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeArmorGlintShader()
    {
        return rendertypeArmorGlintShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeArmorEntityGlintShader()
    {
        return rendertypeArmorEntityGlintShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeGlintTranslucentShader()
    {
        return rendertypeGlintTranslucentShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeGlintShader()
    {
        return rendertypeGlintShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeGlintDirectShader()
    {
        return rendertypeGlintDirectShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEntityGlintShader()
    {
        return rendertypeEntityGlintShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEntityGlintDirectShader()
    {
        return rendertypeEntityGlintDirectShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeTextShader()
    {
        return rendertypeTextShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeTextBackgroundShader()
    {
        return rendertypeTextBackgroundShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeTextIntensityShader()
    {
        return rendertypeTextIntensityShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeTextSeeThroughShader()
    {
        return rendertypeTextSeeThroughShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeTextBackgroundSeeThroughShader()
    {
        return rendertypeTextBackgroundSeeThroughShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeTextIntensitySeeThroughShader()
    {
        return rendertypeTextIntensitySeeThroughShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeLightningShader()
    {
        return rendertypeLightningShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeTripwireShader()
    {
        return rendertypeTripwireShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEndPortalShader()
    {
        return rendertypeEndPortalShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeEndGatewayShader()
    {
        return rendertypeEndGatewayShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeCloudsShader()
    {
        return rendertypeCloudsShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeLinesShader()
    {
        return rendertypeLinesShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeCrumblingShader()
    {
        return rendertypeCrumblingShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeGuiShader()
    {
        return rendertypeGuiShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeGuiOverlayShader()
    {
        return rendertypeGuiOverlayShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeGuiTextHighlightShader()
    {
        return rendertypeGuiTextHighlightShader;
    }

    @Nullable
    public static ShaderInstance getRendertypeGuiGhostRecipeOverlayShader()
    {
        return rendertypeGuiGhostRecipeOverlayShader;
    }

    public static record ResourceCache(ResourceProvider original, Map<ResourceLocation, Resource> cache) implements ResourceProvider
    {
        @Override
        public Optional<Resource> getResource(ResourceLocation p_251007_)
        {
            Resource resource = this.cache.get(p_251007_);
            return resource != null ? Optional.of(resource) : this.original.getResource(p_251007_);
        }
    }
}
