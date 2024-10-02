package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import net.optifine.BlockPosM;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.CustomSky;
import net.optifine.DynamicLights;
import net.optifine.EmissiveTextures;
import net.optifine.Lagometer;
import net.optifine.SmartAnimations;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorForge;
import net.optifine.render.ChunkVisibility;
import net.optifine.render.RenderEnv;
import net.optifine.render.RenderStateManager;
import net.optifine.render.RenderUtils;
import net.optifine.render.VboRegion;
import net.optifine.shaders.RenderStage;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import net.optifine.util.BiomeUtils;
import net.optifine.util.GpuMemory;
import net.optifine.util.MathUtils;
import net.optifine.util.PairInt;
import net.optifine.util.RandomUtils;
import net.optifine.util.RenderChunkUtils;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

public class LevelRenderer implements ResourceManagerReloadListener, AutoCloseable
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int SECTION_SIZE = 16;
    public static final int HALF_SECTION_SIZE = 8;
    private static final float SKY_DISC_RADIUS = 512.0F;
    private static final int MIN_FOG_DISTANCE = 32;
    private static final int RAIN_RADIUS = 10;
    private static final int RAIN_DIAMETER = 21;
    private static final int TRANSPARENT_SORT_COUNT = 15;
    private static final ResourceLocation MOON_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
    private static final ResourceLocation SUN_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    protected static final ResourceLocation CLOUDS_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");
    private static final ResourceLocation END_SKY_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/end_sky.png");
    private static final ResourceLocation FORCEFIELD_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/forcefield.png");
    private static final ResourceLocation RAIN_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/rain.png");
    private static final ResourceLocation SNOW_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/snow.png");
    public static final Direction[] DIRECTIONS = Direction.values();
    private final Minecraft minecraft;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    private final RenderBuffers renderBuffers;
    @Nullable
    protected ClientLevel level;
    private final SectionOcclusionGraph sectionOcclusionGraph = new SectionOcclusionGraph();
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList<>(10000);
    private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
    @Nullable
    private ViewArea viewArea;
    @Nullable
    private VertexBuffer starBuffer;
    @Nullable
    private VertexBuffer skyBuffer;
    @Nullable
    private VertexBuffer darkBuffer;
    private boolean generateClouds = true;
    @Nullable
    private VertexBuffer cloudBuffer;
    private final RunningTrimmedMean frameTimes = new RunningTrimmedMean(100);
    private int ticks;
    private final Int2ObjectMap<BlockDestructionProgress> destroyingBlocks = new Int2ObjectOpenHashMap<>();
    private final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress = new Long2ObjectOpenHashMap<>();
    private final Map<BlockPos, SoundInstance> playingJukeboxSongs = Maps.newHashMap();
    @Nullable
    private RenderTarget entityTarget;
    @Nullable
    private PostChain entityEffect;
    @Nullable
    private RenderTarget translucentTarget;
    @Nullable
    private RenderTarget itemEntityTarget;
    @Nullable
    private RenderTarget particlesTarget;
    @Nullable
    private RenderTarget weatherTarget;
    @Nullable
    private RenderTarget cloudsTarget;
    @Nullable
    private PostChain transparencyChain;
    private int lastCameraSectionX = Integer.MIN_VALUE;
    private int lastCameraSectionY = Integer.MIN_VALUE;
    private int lastCameraSectionZ = Integer.MIN_VALUE;
    private double prevCamX = Double.MIN_VALUE;
    private double prevCamY = Double.MIN_VALUE;
    private double prevCamZ = Double.MIN_VALUE;
    private double prevCamRotX = Double.MIN_VALUE;
    private double prevCamRotY = Double.MIN_VALUE;
    private int prevCloudX = Integer.MIN_VALUE;
    private int prevCloudY = Integer.MIN_VALUE;
    private int prevCloudZ = Integer.MIN_VALUE;
    private Vec3 prevCloudColor = Vec3.ZERO;
    @Nullable
    private CloudStatus prevCloudsType;
    @Nullable
    private SectionRenderDispatcher sectionRenderDispatcher;
    private int lastViewDistance = -1;
    private int renderedEntities;
    private int culledEntities;
    private Frustum cullingFrustum;
    private boolean captureFrustum;
    @Nullable
    private Frustum capturedFrustum;
    private final Vector4f[] frustumPoints = new Vector4f[8];
    private final Vector3d frustumPos = new Vector3d(0.0, 0.0, 0.0);
    private double xTransparentOld;
    private double yTransparentOld;
    private double zTransparentOld;
    private int rainSoundTime;
    private final float[] rainSizeX = new float[1024];
    private final float[] rainSizeZ = new float[1024];
    private Set<SectionRenderDispatcher.RenderSection> chunksToResortTransparency = new LinkedHashSet<>();
    private int countChunksToUpdate = 0;
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> renderInfosTerrain = new ObjectArrayList<>(1024);
    private LongOpenHashSet renderInfosEntities = new LongOpenHashSet(1024);
    private List<SectionRenderDispatcher.RenderSection> renderInfosTileEntities = new ArrayList<>(1024);
    private ObjectArrayList renderInfosTerrainNormal = new ObjectArrayList(1024);
    private LongOpenHashSet renderInfosEntitiesNormal = new LongOpenHashSet(1024);
    private List renderInfosTileEntitiesNormal = new ArrayList(1024);
    private ObjectArrayList renderInfosTerrainShadow = new ObjectArrayList(1024);
    private LongOpenHashSet renderInfosEntitiesShadow = new LongOpenHashSet(1024);
    private List renderInfosTileEntitiesShadow = new ArrayList(1024);
    protected int renderDistance = 0;
    protected int renderDistanceSq = 0;
    protected int renderDistanceXZSq = 0;
    private int countTileEntitiesRendered;
    private RenderEnv renderEnv = new RenderEnv(Blocks.AIR.defaultBlockState(), new BlockPos(0, 0, 0));
    public boolean renderOverlayDamaged = false;
    public boolean renderOverlayEyes = false;
    private boolean firstWorldLoad = false;
    private static int renderEntitiesCounter = 0;
    public int loadVisibleChunksCounter = -1;
    public static MessageSignature loadVisibleChunksMessageId = new MessageSignature(RandomUtils.getRandomBytes(256));
    private static boolean ambientOcclusion = false;
    private Map<String, List<Entity>> mapEntityLists = new HashMap<>();
    private Map<RenderType, Map> mapRegionLayers = new LinkedHashMap<>();
    private int frameId;
    private boolean debugFixTerrainFrustumShadow;

    public LevelRenderer(Minecraft p_234245_, EntityRenderDispatcher p_234246_, BlockEntityRenderDispatcher p_234247_, RenderBuffers p_234248_)
    {
        this.minecraft = p_234245_;
        this.entityRenderDispatcher = p_234246_;
        this.blockEntityRenderDispatcher = p_234247_;
        this.renderBuffers = p_234248_;

        for (int i = 0; i < 32; i++)
        {
            for (int j = 0; j < 32; j++)
            {
                float f = (float)(j - 16);
                float f1 = (float)(i - 16);
                float f2 = Mth.sqrt(f * f + f1 * f1);
                this.rainSizeX[i << 5 | j] = -f1 / f2;
                this.rainSizeZ[i << 5 | j] = f / f2;
            }
        }

        this.createStars();
        this.createLightSky();
        this.createDarkSky();
    }

    private void renderSnowAndRain(LightTexture p_109704_, float p_109705_, double p_109706_, double p_109707_, double p_109708_)
    {
        if (!Reflector.IForgeDimensionSpecialEffects_renderSnowAndRain.exists()
                || !Reflector.callBoolean(
                    this.level.effects(),
                    Reflector.IForgeDimensionSpecialEffects_renderSnowAndRain,
                    this.level,
                    this.ticks,
                    p_109705_,
                    p_109704_,
                    p_109706_,
                    p_109707_,
                    p_109708_
                ))
        {
            float f = this.minecraft.level.getRainLevel(p_109705_);

            if (!(f <= 0.0F))
            {
                if (Config.isRainOff())
                {
                    return;
                }

                p_109704_.turnOnLightLayer();
                Level level = this.minecraft.level;
                int i = Mth.floor(p_109706_);
                int j = Mth.floor(p_109707_);
                int k = Mth.floor(p_109708_);
                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder bufferbuilder = null;
                RenderSystem.disableCull();
                RenderSystem.enableBlend();
                RenderSystem.enableDepthTest();
                int l = 5;

                if (Config.isRainFancy())
                {
                    l = 10;
                }

                RenderSystem.depthMask(Minecraft.useShaderTransparency());

                if (Config.isShaders())
                {
                    GlStateManager._depthMask(Shaders.isRainDepth());
                }

                int i1 = -1;
                float f1 = (float)this.ticks + p_109705_;
                RenderSystem.setShader(GameRenderer::getParticleShader);
                BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                for (int j1 = k - l; j1 <= k + l; j1++)
                {
                    for (int k1 = i - l; k1 <= i + l; k1++)
                    {
                        int l1 = (j1 - k + 16) * 32 + k1 - i + 16;
                        double d0 = (double)this.rainSizeX[l1] * 0.5;
                        double d1 = (double)this.rainSizeZ[l1] * 0.5;
                        blockpos$mutableblockpos.set((double)k1, p_109707_, (double)j1);
                        Biome biome = level.getBiome(blockpos$mutableblockpos).value();

                        if (biome.hasPrecipitation())
                        {
                            int i2 = level.getHeight(Heightmap.Types.MOTION_BLOCKING, k1, j1);
                            int j2 = j - l;
                            int k2 = j + l;

                            if (j2 < i2)
                            {
                                j2 = i2;
                            }

                            if (k2 < i2)
                            {
                                k2 = i2;
                            }

                            int l2 = i2;

                            if (i2 < j)
                            {
                                l2 = j;
                            }

                            if (j2 != k2)
                            {
                                RandomSource randomsource = RandomSource.create((long)(k1 * k1 * 3121 + k1 * 45238971 ^ j1 * j1 * 418711 + j1 * 13761));
                                blockpos$mutableblockpos.set(k1, j2, j1);
                                Biome.Precipitation biome$precipitation = biome.getPrecipitationAt(blockpos$mutableblockpos);

                                if (biome$precipitation == Biome.Precipitation.RAIN)
                                {
                                    if (i1 != 0)
                                    {
                                        if (i1 >= 0)
                                        {
                                            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
                                        }

                                        i1 = 0;
                                        RenderSystem.setShaderTexture(0, RAIN_LOCATION);
                                        bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                                    }

                                    int i3 = this.ticks & 131071;
                                    int j3 = k1 * k1 * 3121 + k1 * 45238971 + j1 * j1 * 418711 + j1 * 13761 & 0xFF;
                                    float f2 = 3.0F + randomsource.nextFloat();
                                    float f3 = -((float)(i3 + j3) + p_109705_) / 32.0F * f2;
                                    float f4 = f3 % 32.0F;
                                    double d2 = (double)k1 + 0.5 - p_109706_;
                                    double d3 = (double)j1 + 0.5 - p_109708_;
                                    float f6 = (float)Math.sqrt(d2 * d2 + d3 * d3) / (float)l;
                                    float f7 = ((1.0F - f6 * f6) * 0.5F + 0.5F) * f;
                                    blockpos$mutableblockpos.set(k1, l2, j1);
                                    int k3 = getLightColor(level, blockpos$mutableblockpos);
                                    bufferbuilder.addVertex(
                                        (float)((double)k1 - p_109706_ - d0 + 0.5),
                                        (float)((double)k2 - p_109707_),
                                        (float)((double)j1 - p_109708_ - d1 + 0.5)
                                    )
                                    .setUv(0.0F, (float)j2 * 0.25F + f4)
                                    .setColor(1.0F, 1.0F, 1.0F, f7)
                                    .setLight(k3);
                                    bufferbuilder.addVertex(
                                        (float)((double)k1 - p_109706_ + d0 + 0.5),
                                        (float)((double)k2 - p_109707_),
                                        (float)((double)j1 - p_109708_ + d1 + 0.5)
                                    )
                                    .setUv(1.0F, (float)j2 * 0.25F + f4)
                                    .setColor(1.0F, 1.0F, 1.0F, f7)
                                    .setLight(k3);
                                    bufferbuilder.addVertex(
                                        (float)((double)k1 - p_109706_ + d0 + 0.5),
                                        (float)((double)j2 - p_109707_),
                                        (float)((double)j1 - p_109708_ + d1 + 0.5)
                                    )
                                    .setUv(1.0F, (float)k2 * 0.25F + f4)
                                    .setColor(1.0F, 1.0F, 1.0F, f7)
                                    .setLight(k3);
                                    bufferbuilder.addVertex(
                                        (float)((double)k1 - p_109706_ - d0 + 0.5),
                                        (float)((double)j2 - p_109707_),
                                        (float)((double)j1 - p_109708_ - d1 + 0.5)
                                    )
                                    .setUv(0.0F, (float)k2 * 0.25F + f4)
                                    .setColor(1.0F, 1.0F, 1.0F, f7)
                                    .setLight(k3);
                                }
                                else if (biome$precipitation == Biome.Precipitation.SNOW)
                                {
                                    if (i1 != 1)
                                    {
                                        if (i1 >= 0)
                                        {
                                            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
                                        }

                                        i1 = 1;
                                        RenderSystem.setShaderTexture(0, SNOW_LOCATION);
                                        bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                                    }

                                    float f8 = -((float)(this.ticks & 511) + p_109705_) / 512.0F;
                                    float f9 = (float)(randomsource.nextDouble() + (double)f1 * 0.01 * (double)((float)randomsource.nextGaussian()));
                                    float f10 = (float)(randomsource.nextDouble() + (double)(f1 * (float)randomsource.nextGaussian()) * 0.001);
                                    double d4 = (double)k1 + 0.5 - p_109706_;
                                    double d5 = (double)j1 + 0.5 - p_109708_;
                                    float f11 = (float)Math.sqrt(d4 * d4 + d5 * d5) / (float)l;
                                    float f5 = ((1.0F - f11 * f11) * 0.3F + 0.5F) * f;
                                    blockpos$mutableblockpos.set(k1, l2, j1);
                                    int j4 = getLightColor(level, blockpos$mutableblockpos);
                                    int k4 = j4 >> 16 & 65535;
                                    int l4 = j4 & 65535;
                                    int l3 = (k4 * 3 + 240) / 4;
                                    int i4 = (l4 * 3 + 240) / 4;
                                    bufferbuilder.addVertex(
                                        (float)((double)k1 - p_109706_ - d0 + 0.5),
                                        (float)((double)k2 - p_109707_),
                                        (float)((double)j1 - p_109708_ - d1 + 0.5)
                                    )
                                    .setUv(0.0F + f9, (float)j2 * 0.25F + f8 + f10)
                                    .setColor(1.0F, 1.0F, 1.0F, f5)
                                    .setUv2(i4, l3);
                                    bufferbuilder.addVertex(
                                        (float)((double)k1 - p_109706_ + d0 + 0.5),
                                        (float)((double)k2 - p_109707_),
                                        (float)((double)j1 - p_109708_ + d1 + 0.5)
                                    )
                                    .setUv(1.0F + f9, (float)j2 * 0.25F + f8 + f10)
                                    .setColor(1.0F, 1.0F, 1.0F, f5)
                                    .setUv2(i4, l3);
                                    bufferbuilder.addVertex(
                                        (float)((double)k1 - p_109706_ + d0 + 0.5),
                                        (float)((double)j2 - p_109707_),
                                        (float)((double)j1 - p_109708_ + d1 + 0.5)
                                    )
                                    .setUv(1.0F + f9, (float)k2 * 0.25F + f8 + f10)
                                    .setColor(1.0F, 1.0F, 1.0F, f5)
                                    .setUv2(i4, l3);
                                    bufferbuilder.addVertex(
                                        (float)((double)k1 - p_109706_ - d0 + 0.5),
                                        (float)((double)j2 - p_109707_),
                                        (float)((double)j1 - p_109708_ - d1 + 0.5)
                                    )
                                    .setUv(0.0F + f9, (float)k2 * 0.25F + f8 + f10)
                                    .setColor(1.0F, 1.0F, 1.0F, f5)
                                    .setUv2(i4, l3);
                                }
                            }
                        }
                    }
                }

                if (i1 >= 0)
                {
                    BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
                }

                RenderSystem.enableCull();
                RenderSystem.disableBlend();
                p_109704_.turnOffLightLayer();
            }
        }
    }

    public void tickRain(Camera p_109694_)
    {
        if (!Reflector.IForgeDimensionSpecialEffects_tickRain.exists()
                || !Reflector.callBoolean(this.level.effects(), Reflector.IForgeDimensionSpecialEffects_tickRain, this.level, this.ticks, p_109694_))
        {
            float f = this.minecraft.level.getRainLevel(1.0F) / (Minecraft.useFancyGraphics() ? 1.0F : 2.0F);

            if (!Config.isRainFancy())
            {
                f /= 2.0F;
            }

            if (!(f <= 0.0F) && Config.isRainSplash())
            {
                RandomSource randomsource = RandomSource.create((long)this.ticks * 312987231L);
                LevelReader levelreader = this.minecraft.level;
                BlockPos blockpos = BlockPos.containing(p_109694_.getPosition());
                BlockPos blockpos1 = null;
                int i = (int)(100.0F * f * f) / (this.minecraft.options.particles().get() == ParticleStatus.DECREASED ? 2 : 1);

                for (int j = 0; j < i; j++)
                {
                    int k = randomsource.nextInt(21) - 10;
                    int l = randomsource.nextInt(21) - 10;
                    BlockPos blockpos2 = levelreader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos.offset(k, 0, l));

                    if (blockpos2.getY() > levelreader.getMinBuildHeight()
                            && blockpos2.getY() <= blockpos.getY() + 10
                            && blockpos2.getY() >= blockpos.getY() - 10)
                    {
                        Biome biome = levelreader.getBiome(blockpos2).value();

                        if (biome.getPrecipitationAt(blockpos2) == Biome.Precipitation.RAIN)
                        {
                            blockpos1 = blockpos2.below();

                            if (this.minecraft.options.particles().get() == ParticleStatus.MINIMAL)
                            {
                                break;
                            }

                            double d0 = randomsource.nextDouble();
                            double d1 = randomsource.nextDouble();
                            BlockState blockstate = levelreader.getBlockState(blockpos1);
                            FluidState fluidstate = levelreader.getFluidState(blockpos1);
                            VoxelShape voxelshape = blockstate.getCollisionShape(levelreader, blockpos1);
                            double d2 = voxelshape.max(Direction.Axis.Y, d0, d1);
                            double d3 = (double)fluidstate.getHeight(levelreader, blockpos1);
                            double d4 = Math.max(d2, d3);
                            ParticleOptions particleoptions = !fluidstate.is(FluidTags.LAVA)
                                                              && !blockstate.is(Blocks.MAGMA_BLOCK)
                                                              && !CampfireBlock.isLitCampfire(blockstate)
                                                              ? ParticleTypes.RAIN
                                                              : ParticleTypes.SMOKE;
                            this.minecraft
                            .level
                            .addParticle(
                                particleoptions,
                                (double)blockpos1.getX() + d0,
                                (double)blockpos1.getY() + d4,
                                (double)blockpos1.getZ() + d1,
                                0.0,
                                0.0,
                                0.0
                            );
                        }
                    }
                }

                if (blockpos1 != null && randomsource.nextInt(3) < this.rainSoundTime++)
                {
                    this.rainSoundTime = 0;

                    if (blockpos1.getY() > blockpos.getY() + 1
                            && levelreader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos).getY() > Mth.floor((float)blockpos.getY()))
                    {
                        this.minecraft.level.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F, false);
                    }
                    else
                    {
                        this.minecraft.level.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F, false);
                    }
                }
            }
        }
    }

    @Override
    public void close()
    {
        if (this.entityEffect != null)
        {
            this.entityEffect.close();
        }

        if (this.transparencyChain != null)
        {
            this.transparencyChain.close();
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager p_109513_)
    {
        this.initOutline();

        if (Minecraft.useShaderTransparency())
        {
            this.initTransparency();
        }
    }

    public void initOutline()
    {
        if (this.entityEffect != null)
        {
            this.entityEffect.close();
        }

        ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace("shaders/post/entity_outline.json");

        try
        {
            this.entityEffect = new PostChain(this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getMainRenderTarget(), resourcelocation);
            this.entityEffect.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
            this.entityTarget = this.entityEffect.getTempTarget("final");
        }
        catch (IOException ioexception)
        {
            LOGGER.warn("Failed to load shader: {}", resourcelocation, ioexception);
            this.entityEffect = null;
            this.entityTarget = null;
        }
        catch (JsonSyntaxException jsonsyntaxexception)
        {
            LOGGER.warn("Failed to parse shader: {}", resourcelocation, jsonsyntaxexception);
            this.entityEffect = null;
            this.entityTarget = null;
        }
    }

    private void initTransparency()
    {
        this.deinitTransparency();
        ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace("shaders/post/transparency.json");

        try
        {
            PostChain postchain = new PostChain(this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getMainRenderTarget(), resourcelocation);
            postchain.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
            RenderTarget rendertarget1 = postchain.getTempTarget("translucent");
            RenderTarget rendertarget2 = postchain.getTempTarget("itemEntity");
            RenderTarget rendertarget3 = postchain.getTempTarget("particles");
            RenderTarget rendertarget4 = postchain.getTempTarget("weather");
            RenderTarget rendertarget = postchain.getTempTarget("clouds");
            this.transparencyChain = postchain;
            this.translucentTarget = rendertarget1;
            this.itemEntityTarget = rendertarget2;
            this.particlesTarget = rendertarget3;
            this.weatherTarget = rendertarget4;
            this.cloudsTarget = rendertarget;
        }
        catch (Exception exception1)
        {
            String s = exception1 instanceof JsonSyntaxException ? "parse" : "load";
            String s1 = "Failed to " + s + " shader: " + resourcelocation;
            LevelRenderer.TransparencyShaderException levelrenderer$transparencyshaderexception = new LevelRenderer.TransparencyShaderException(s1, exception1);

            if (this.minecraft.getResourcePackRepository().getSelectedIds().size() > 1)
            {
                Component component = this.minecraft
                                      .getResourceManager()
                                      .listPacks()
                                      .findFirst()
                                      .map(packResourcesIn -> Component.literal(packResourcesIn.packId()))
                                      .orElse(null);
                this.minecraft.options.graphicsMode().set(GraphicsStatus.FANCY);
                this.minecraft.clearResourcePacksOnError(levelrenderer$transparencyshaderexception, component, null);
            }
            else
            {
                this.minecraft.options.graphicsMode().set(GraphicsStatus.FANCY);
                this.minecraft.options.save();
                LOGGER.error(LogUtils.FATAL_MARKER, s1, (Throwable)levelrenderer$transparencyshaderexception);
                this.minecraft.emergencySaveAndCrash(new CrashReport(s1, levelrenderer$transparencyshaderexception));
            }
        }
    }

    private void deinitTransparency()
    {
        if (this.transparencyChain != null)
        {
            this.transparencyChain.close();
            this.translucentTarget.destroyBuffers();
            this.itemEntityTarget.destroyBuffers();
            this.particlesTarget.destroyBuffers();
            this.weatherTarget.destroyBuffers();
            this.cloudsTarget.destroyBuffers();
            this.transparencyChain = null;
            this.translucentTarget = null;
            this.itemEntityTarget = null;
            this.particlesTarget = null;
            this.weatherTarget = null;
            this.cloudsTarget = null;
        }
    }

    public void doEntityOutline()
    {
        if (this.shouldShowEntityOutlines())
        {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ZERO,
                GlStateManager.DestFactor.ONE
            );
            this.entityTarget.blitToScreen(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), false);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    public boolean shouldShowEntityOutlines()
    {
        return !Config.isShaders() && !Config.isAntialiasing()
               ? !this.minecraft.gameRenderer.isPanoramicMode() && this.entityTarget != null && this.entityEffect != null && this.minecraft.player != null
               : false;
    }

    private void createDarkSky()
    {
        if (this.darkBuffer != null)
        {
            this.darkBuffer.close();
        }

        this.darkBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        this.darkBuffer.bind();
        this.darkBuffer.upload(buildSkyDisc(Tesselator.getInstance(), -16.0F));
        VertexBuffer.unbind();
    }

    private void createLightSky()
    {
        if (this.skyBuffer != null)
        {
            this.skyBuffer.close();
        }

        this.skyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        this.skyBuffer.bind();
        this.skyBuffer.upload(buildSkyDisc(Tesselator.getInstance(), 16.0F));
        VertexBuffer.unbind();
    }

    private static MeshData buildSkyDisc(Tesselator p_343330_, float p_234269_)
    {
        float f = Math.signum(p_234269_) * 512.0F;
        float f1 = 512.0F;
        BufferBuilder bufferbuilder = p_343330_.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        bufferbuilder.addVertex(0.0F, p_234269_, 0.0F);

        for (int i = -180; i <= 180; i += 45)
        {
            bufferbuilder.addVertex(
                f * Mth.cos((float)i * (float)(Math.PI / 180.0)), p_234269_, 512.0F * Mth.sin((float)i * (float)(Math.PI / 180.0))
            );
        }

        return bufferbuilder.buildOrThrow();
    }

    private void createStars()
    {
        if (this.starBuffer != null)
        {
            this.starBuffer.close();
        }

        this.starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        this.starBuffer.bind();
        this.starBuffer.upload(this.drawStars(Tesselator.getInstance()));
        VertexBuffer.unbind();
    }

    private MeshData drawStars(Tesselator p_344774_)
    {
        RandomSource randomsource = RandomSource.create(10842L);
        int i = 1500;
        float f = 100.0F;
        BufferBuilder bufferbuilder = p_344774_.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        for (int j = 0; j < 1500; j++)
        {
            float f1 = randomsource.nextFloat() * 2.0F - 1.0F;
            float f2 = randomsource.nextFloat() * 2.0F - 1.0F;
            float f3 = randomsource.nextFloat() * 2.0F - 1.0F;
            float f4 = 0.15F + randomsource.nextFloat() * 0.1F;
            float f5 = Mth.lengthSquared(f1, f2, f3);

            if (!(f5 <= 0.010000001F) && !(f5 >= 1.0F))
            {
                Vector3f vector3f = new Vector3f(f1, f2, f3).normalize(100.0F);
                float f6 = (float)(randomsource.nextDouble() * (float) Math.PI * 2.0);
                Quaternionf quaternionf = new Quaternionf().rotateTo(new Vector3f(0.0F, 0.0F, -1.0F), vector3f).rotateZ(f6);
                bufferbuilder.addVertex(vector3f.add(new Vector3f(f4, -f4, 0.0F).rotate(quaternionf)));
                bufferbuilder.addVertex(vector3f.add(new Vector3f(f4, f4, 0.0F).rotate(quaternionf)));
                bufferbuilder.addVertex(vector3f.add(new Vector3f(-f4, f4, 0.0F).rotate(quaternionf)));
                bufferbuilder.addVertex(vector3f.add(new Vector3f(-f4, -f4, 0.0F).rotate(quaternionf)));
            }
        }

        return bufferbuilder.buildOrThrow();
    }

    public void setLevel(@Nullable ClientLevel p_109702_)
    {
        this.lastCameraSectionX = Integer.MIN_VALUE;
        this.lastCameraSectionY = Integer.MIN_VALUE;
        this.lastCameraSectionZ = Integer.MIN_VALUE;
        this.entityRenderDispatcher.setLevel(p_109702_);
        this.level = p_109702_;

        if (Config.isDynamicLights())
        {
            DynamicLights.clear();
        }

        ChunkVisibility.reset();
        this.renderEnv.reset(null, null);
        BiomeUtils.onWorldChanged(this.level);
        Shaders.checkWorldChanged(this.level);

        if (p_109702_ != null)
        {
            this.allChanged();
        }
        else
        {
            if (this.viewArea != null)
            {
                this.viewArea.releaseAllBuffers();
                this.viewArea = null;
            }

            if (this.sectionRenderDispatcher != null)
            {
                this.sectionRenderDispatcher.dispose();
            }

            this.sectionRenderDispatcher = null;
            this.globalBlockEntities.clear();
            this.sectionOcclusionGraph.waitAndReset(null);
            this.visibleSections.clear();
            this.clearRenderInfos();
        }
    }

    public void graphicsChanged()
    {
        if (Minecraft.useShaderTransparency())
        {
            this.initTransparency();
        }
        else
        {
            this.deinitTransparency();
        }
    }

    public void allChanged()
    {
        if (this.level != null)
        {
            this.graphicsChanged();
            this.level.clearTintCaches();

            if (this.sectionRenderDispatcher == null)
            {
                this.sectionRenderDispatcher = new SectionRenderDispatcher(
                    this.level, this, Util.backgroundExecutor(), this.renderBuffers, this.minecraft.getBlockRenderer(), this.minecraft.getBlockEntityRenderDispatcher()
                );
            }
            else
            {
                this.sectionRenderDispatcher.setLevel(this.level);
            }

            this.generateClouds = true;
            ItemBlockRenderTypes.setFancy(Config.isTreesFancy());
            ModelBlockRenderer.updateAoLightValue();

            if (Config.isDynamicLights())
            {
                DynamicLights.clear();
            }

            SmartAnimations.update();
            ambientOcclusion = Minecraft.useAmbientOcclusion();
            this.lastViewDistance = this.minecraft.options.getEffectiveRenderDistance();
            this.renderDistance = this.lastViewDistance * 16;
            this.renderDistanceSq = this.renderDistance * this.renderDistance;
            double d0 = (double)((this.lastViewDistance + 1) * 16);
            this.renderDistanceXZSq = (int)(d0 * d0);

            if (this.viewArea != null)
            {
                this.viewArea.releaseAllBuffers();
            }

            GpuMemory.bufferFreed(GpuMemory.getBufferAllocated());
            this.sectionRenderDispatcher.blockUntilClear();

            synchronized (this.globalBlockEntities)
            {
                this.globalBlockEntities.clear();
            }

            this.viewArea = new ViewArea(this.sectionRenderDispatcher, this.level, this.minecraft.options.getEffectiveRenderDistance(), this);
            this.sectionOcclusionGraph.waitAndReset(this.viewArea);
            this.visibleSections.clear();
            this.clearRenderInfos();
            this.killFrustum();
            Entity entity = this.minecraft.getCameraEntity();

            if (entity != null)
            {
                this.viewArea.repositionCamera(entity.getX(), entity.getZ());
            }
        }
    }

    public void resize(int p_109488_, int p_109489_)
    {
        this.needsUpdate();

        if (this.entityEffect != null)
        {
            this.entityEffect.resize(p_109488_, p_109489_);
        }

        if (this.transparencyChain != null)
        {
            this.transparencyChain.resize(p_109488_, p_109489_);
        }
    }

    public String getSectionStatistics()
    {
        int i = this.viewArea.sections.length;
        int j = this.countRenderedSections();
        return String.format(
                   Locale.ROOT,
                   "C: %d/%d %sD: %d, %s",
                   j,
                   i,
                   this.minecraft.smartCull ? "(s) " : "",
                   this.lastViewDistance,
                   this.sectionRenderDispatcher == null ? "null" : this.sectionRenderDispatcher.getStats()
               );
    }

    public SectionRenderDispatcher getSectionRenderDispatcher()
    {
        return this.sectionRenderDispatcher;
    }

    public double getTotalSections()
    {
        return (double)this.viewArea.sections.length;
    }

    public double getLastViewDistance()
    {
        return (double)this.lastViewDistance;
    }

    public int countRenderedSections()
    {
        return this.renderInfosTerrain.size();
    }

    public String getEntityStatistics()
    {
        return "E: "
               + this.renderedEntities
               + "/"
               + this.level.getEntityCount()
               + ", B: "
               + this.culledEntities
               + ", SD: "
               + this.level.getServerSimulationDistance()
               + ", "
               + Config.getVersionDebug();
    }

    private void setupRender(Camera p_194339_, Frustum p_194340_, boolean p_194341_, boolean p_194342_)
    {
        Vec3 vec3 = p_194339_.getPosition();

        if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance)
        {
            this.allChanged();
        }

        this.level.getProfiler().push("camera");
        double d0 = this.minecraft.player.getX();
        double d1 = this.minecraft.player.getY();
        double d2 = this.minecraft.player.getZ();
        int i = SectionPos.posToSectionCoord(d0);
        int j = SectionPos.posToSectionCoord(d1);
        int k = SectionPos.posToSectionCoord(d2);

        if (this.lastCameraSectionX != i || this.lastCameraSectionY != j || this.lastCameraSectionZ != k)
        {
            this.lastCameraSectionX = i;
            this.lastCameraSectionY = j;
            this.lastCameraSectionZ = k;
            this.viewArea.repositionCamera(d0, d2);
        }

        if (Config.isDynamicLights())
        {
            DynamicLights.update(this);
        }

        this.sectionRenderDispatcher.setCamera(vec3);
        this.level.getProfiler().popPush("cull");
        this.minecraft.getProfiler().popPush("culling");
        BlockPos blockpos = p_194339_.getBlockPosition();
        double d3 = Math.floor(vec3.x / 8.0);
        double d4 = Math.floor(vec3.y / 8.0);
        double d5 = Math.floor(vec3.z / 8.0);

        if (d3 != this.prevCamX || d4 != this.prevCamY || d5 != this.prevCamZ)
        {
            this.sectionOcclusionGraph.invalidate();
        }

        this.prevCamX = d3;
        this.prevCamY = d4;
        this.prevCamZ = d5;
        this.minecraft.getProfiler().popPush("update");
        Lagometer.timerVisibility.start();

        if (!p_194341_)
        {
            boolean flag = this.minecraft.smartCull;

            if (p_194342_ && this.level.getBlockState(blockpos).isSolidRender(this.level, blockpos))
            {
                flag = false;
            }

            Entity.setViewScale(Mth.clamp((double)this.minecraft.options.getEffectiveRenderDistance() / 8.0, 1.0, 2.5) * this.minecraft.options.entityDistanceScaling().get());
            this.minecraft.getProfiler().push("section_occlusion_graph");
            this.sectionOcclusionGraph.update(flag, p_194339_, p_194340_, this.visibleSections);
            this.minecraft.getProfiler().pop();
            double d6 = Math.floor((double)(p_194339_.getXRot() / 2.0F));
            double d7 = Math.floor((double)(p_194339_.getYRot() / 2.0F));
            boolean flag1 = false;

            if (this.sectionOcclusionGraph.consumeFrustumUpdate() || d6 != this.prevCamRotX || d7 != this.prevCamRotY)
            {
                this.applyFrustum(offsetFrustum(p_194340_));
                this.prevCamRotX = d6;
                this.prevCamRotY = d7;
                flag1 = true;
                ShadersRender.frustumTerrainShadowChanged = true;
            }

            if (this.level.getSectionStorage().resetUpdated() || flag1)
            {
                this.applyFrustumEntities(p_194340_, -1);
                ShadersRender.frustumEntitiesShadowChanged = true;
            }
        }

        Lagometer.timerVisibility.end();
        this.minecraft.getProfiler().pop();
    }

    public static Frustum offsetFrustum(Frustum p_298803_)
    {
        return new Frustum(p_298803_).offsetToFullyIncludeCameraCube(8);
    }

    private void applyFrustum(Frustum p_194355_)
    {
        this.applyFrustum(p_194355_, true, -1);
    }

    public void applyFrustum(Frustum frustumIn, boolean updateRenderInfos, int maxChunkDistance)
    {
        if (!Minecraft.getInstance().isSameThread())
        {
            throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
        }
        else
        {
            this.minecraft.getProfiler().push("apply_frustum");

            if (updateRenderInfos)
            {
                this.visibleSections.clear();
            }

            this.clearRenderInfosTerrain();
            this.sectionOcclusionGraph.addSectionsInFrustum(frustumIn, this.visibleSections, updateRenderInfos, maxChunkDistance);
            this.minecraft.getProfiler().pop();
        }
    }

    public void addRecentlyCompiledSection(SectionRenderDispatcher.RenderSection p_301248_)
    {
        this.sectionOcclusionGraph.onSectionCompiled(p_301248_);
    }

    private void captureFrustum(Matrix4f p_253756_, Matrix4f p_253787_, double p_254187_, double p_253833_, double p_254547_, Frustum p_253954_)
    {
        this.capturedFrustum = p_253954_;
        Matrix4f matrix4f = new Matrix4f(p_253787_);
        matrix4f.mul(p_253756_);
        matrix4f.invert();
        this.frustumPos.x = p_254187_;
        this.frustumPos.y = p_253833_;
        this.frustumPos.z = p_254547_;
        this.frustumPoints[0] = new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F);
        this.frustumPoints[1] = new Vector4f(1.0F, -1.0F, -1.0F, 1.0F);
        this.frustumPoints[2] = new Vector4f(1.0F, 1.0F, -1.0F, 1.0F);
        this.frustumPoints[3] = new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F);
        this.frustumPoints[4] = new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F);
        this.frustumPoints[5] = new Vector4f(1.0F, -1.0F, 1.0F, 1.0F);
        this.frustumPoints[6] = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.frustumPoints[7] = new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F);

        for (int i = 0; i < 8; i++)
        {
            matrix4f.transform(this.frustumPoints[i]);
            this.frustumPoints[i].div(this.frustumPoints[i].w());
        }
    }

    public void prepareCullFrustum(Vec3 p_253766_, Matrix4f p_254341_, Matrix4f p_332544_)
    {
        this.cullingFrustum = new Frustum(p_254341_, p_332544_);
        this.cullingFrustum.prepare(p_253766_.x(), p_253766_.y(), p_253766_.z());

        if (Config.isShaders() && !Shaders.isFrustumCulling())
        {
            this.cullingFrustum.disabled = true;
        }
    }

    public void renderLevel(
        DeltaTracker p_342180_, boolean p_109603_, Camera p_109604_, GameRenderer p_109605_, LightTexture p_109606_, Matrix4f p_254120_, Matrix4f p_330527_
    )
    {
        TickRateManager tickratemanager = this.minecraft.level.tickRateManager();
        float f = p_342180_.getGameTimeDeltaPartialTick(false);
        RenderSystem.setShaderGameTime(this.level.getGameTime(), f);
        this.blockEntityRenderDispatcher.prepare(this.level, p_109604_, this.minecraft.hitResult);
        this.entityRenderDispatcher.prepare(this.level, p_109604_, this.minecraft.crosshairPickEntity);
        ProfilerFiller profilerfiller = this.level.getProfiler();
        profilerfiller.popPush("light_update_queue");
        this.level.pollLightUpdates();
        profilerfiller.popPush("light_updates");
        this.level.getChunkSource().getLightEngine().runLightUpdates();
        Vec3 vec3 = p_109604_.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        profilerfiller.popPush("culling");
        boolean flag = this.capturedFrustum != null;
        Frustum frustum;

        if (flag)
        {
            frustum = this.capturedFrustum;
            frustum.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
        }
        else
        {
            frustum = this.cullingFrustum;
        }

        this.minecraft.getProfiler().popPush("captureFrustum");

        if (this.captureFrustum)
        {
            this.captureFrustum(p_254120_, p_330527_, vec3.x, vec3.y, vec3.z, flag ? new Frustum(p_254120_, p_330527_) : frustum);
            this.captureFrustum = false;
            frustum = this.capturedFrustum;
            frustum.disabled = Config.isShaders() && !Shaders.isFrustumCulling();
            frustum.prepare(vec3.x, vec3.y, vec3.z);
            this.applyFrustum(frustum, false, -1);
            this.applyFrustumEntities(frustum, -1);
        }

        if (this.debugFixTerrainFrustumShadow)
        {
            this.captureFrustum(p_254120_, p_330527_, vec3.x, vec3.y, vec3.z, ShadersRender.makeShadowFrustum(p_109604_, f));
            this.debugFixTerrainFrustumShadow = false;
            frustum = this.capturedFrustum;
            frustum.prepare(vec3.x, vec3.y, vec3.z);
            ShadersRender.frustumTerrainShadowChanged = true;
            ShadersRender.frustumEntitiesShadowChanged = true;
            ShadersRender.applyFrustumShadow(this, frustum);
        }

        profilerfiller.popPush("clear");

        if (Config.isShaders())
        {
            Shaders.setViewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
        }
        else
        {
            RenderSystem.viewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
        }

        FogRenderer.setupColor(p_109604_, f, this.minecraft.level, this.minecraft.options.getEffectiveRenderDistance(), p_109605_.getDarkenWorldAmount(f));
        FogRenderer.levelFogColor();
        RenderSystem.clear(16640, Minecraft.ON_OSX);
        boolean flag1 = Config.isShaders();

        if (flag1)
        {
            Shaders.clearRenderBuffer();
            Shaders.setCamera(p_254120_, p_109604_, f);
            Shaders.renderPrepare();
        }

        float f1 = p_109605_.getRenderDistance();
        boolean flag2 = this.minecraft.level.effects().isFoggyAt(Mth.floor(d0), Mth.floor(d1)) || this.minecraft.gui.getBossOverlay().shouldCreateWorldFog();
        boolean flag3 = this.minecraft.level.effects().isFoggyAt(Mth.floor(d0), Mth.floor(d1));

        if ((Config.isSkyEnabled() || Config.isSunMoonEnabled() || Config.isStarsEnabled()) && !Shaders.isShadowPass)
        {
            profilerfiller.popPush("sky");

            if (flag1)
            {
                Shaders.beginSky();
            }

            RenderSystem.setShader(GameRenderer::getPositionShader);
            this.renderSky(p_254120_, p_330527_, f, p_109604_, flag3, () -> FogRenderer.setupFog(p_109604_, FogRenderer.FogMode.FOG_SKY, f1, flag2, f));

            if (flag1)
            {
                Shaders.endSky();
            }
        }
        else
        {
            GlStateManager._disableBlend();
        }

        ReflectorForge.dispatchRenderStageS(Reflector.RenderLevelStageEvent_Stage_AFTER_SKY, this, p_254120_, p_330527_, this.ticks, p_109604_, frustum);
        profilerfiller.popPush("fog");
        FogRenderer.setupFog(p_109604_, FogRenderer.FogMode.FOG_TERRAIN, Math.max(f1, 32.0F), flag2, f);
        profilerfiller.popPush("terrain_setup");
        this.checkLoadVisibleChunks(p_109604_, frustum, this.minecraft.player.isSpectator());
        this.frameId++;
        this.setupRender(p_109604_, frustum, flag, this.minecraft.player.isSpectator());
        profilerfiller.popPush("compile_sections");
        this.compileSections(p_109604_);
        profilerfiller.popPush("terrain");
        Lagometer.timerTerrain.start();

        if (this.minecraft.options.ofSmoothFps)
        {
            this.minecraft.getProfiler().popPush("finish");
            GL11.glFinish();
            this.minecraft.getProfiler().popPush("terrain");
        }

        if (Config.isFogOff() && FogRenderer.fogStandard)
        {
            RenderSystem.setFogAllowed(false);
        }

        this.renderSectionLayer(RenderType.solid(), d0, d1, d2, p_254120_, p_330527_);
        this.minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).setBlurMipmap(false, this.minecraft.options.mipmapLevels().get() > 0);
        this.renderSectionLayer(RenderType.cutoutMipped(), d0, d1, d2, p_254120_, p_330527_);
        this.minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).restoreLastBlurMipmap();
        this.renderSectionLayer(RenderType.cutout(), d0, d1, d2, p_254120_, p_330527_);

        if (flag1)
        {
            ShadersRender.endTerrain();
        }

        if (this.level.effects().constantAmbientLight())
        {
            Lighting.setupNetherLevel();
        }
        else
        {
            Lighting.setupLevel();
        }

        if (flag1)
        {
            Shaders.beginEntities();
        }

        ItemFrameRenderer.updateItemRenderDistance();
        profilerfiller.popPush("entities");
        renderEntitiesCounter++;
        this.renderedEntities = 0;
        this.culledEntities = 0;
        this.countTileEntitiesRendered = 0;

        if (this.itemEntityTarget != null)
        {
            this.itemEntityTarget.clear(Minecraft.ON_OSX);
            this.itemEntityTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
            this.minecraft.getMainRenderTarget().bindWrite(false);
        }

        if (this.weatherTarget != null)
        {
            this.weatherTarget.clear(Minecraft.ON_OSX);
        }

        if (this.shouldShowEntityOutlines())
        {
            this.entityTarget.clear(Minecraft.ON_OSX);
            this.minecraft.getMainRenderTarget().bindWrite(false);
        }

        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.mul(p_254120_);
        RenderSystem.applyModelViewMatrix();
        boolean flag4 = false;
        PoseStack posestack = new PoseStack();
        MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();

        if (Config.isFastRender())
        {
            RenderStateManager.enableCache();
            multibuffersource$buffersource.enableCache();
        }

        int i = this.level.getMinBuildHeight();
        int j = this.level.getMaxBuildHeight();

        if (Config.isRenderRegions() || Config.isMultiTexture())
        {
            GameRenderer.getPositionShader().apply();
        }

        for (Entity entity : this.level.entitiesForRendering())
        {
            if (this.shouldRenderEntity(entity, i, j))
            {
                boolean flag5 = entity == this.minecraft.player && !this.minecraft.player.isSpectator();

                if (this.entityRenderDispatcher.shouldRender(entity, frustum, d0, d1, d2) || entity.hasIndirectPassenger(this.minecraft.player))
                {
                    BlockPos blockpos = entity.blockPosition();

                    if ((this.level.isOutsideBuildHeight(blockpos.getY()) || this.isSectionCompiled(blockpos))
                            && (
                                entity != p_109604_.getEntity()
                                || p_109604_.isDetached()
                                || p_109604_.getEntity() instanceof LivingEntity && ((LivingEntity)p_109604_.getEntity()).isSleeping()
                            )
                            && (!(entity instanceof LocalPlayer) || p_109604_.getEntity() == entity || flag5))
                    {
                        String s = entity.getClass().getName();
                        List<Entity> list = this.mapEntityLists.get(s);

                        if (list == null)
                        {
                            list = new ArrayList<>();
                            this.mapEntityLists.put(s, list);
                        }

                        list.add(entity);
                    }
                }
            }
        }

        for (List<Entity> list1 : this.mapEntityLists.values())
        {
            for (Entity entity1 : list1)
            {
                this.renderedEntities++;

                if (entity1.tickCount == 0)
                {
                    entity1.xOld = entity1.getX();
                    entity1.yOld = entity1.getY();
                    entity1.zOld = entity1.getZ();
                }

                MultiBufferSource multibuffersource1;

                if (this.shouldShowEntityOutlines() && this.minecraft.shouldEntityAppearGlowing(entity1))
                {
                    flag4 = true;
                    OutlineBufferSource outlinebuffersource = this.renderBuffers.outlineBufferSource();
                    multibuffersource1 = outlinebuffersource;
                    int k = entity1.getTeamColor();
                    outlinebuffersource.setColor(FastColor.ARGB32.red(k), FastColor.ARGB32.green(k), FastColor.ARGB32.blue(k), 255);
                }
                else
                {
                    if (Reflector.IForgeEntity_hasCustomOutlineRendering.exists()
                            && this.shouldShowEntityOutlines()
                            && Reflector.callBoolean(entity1, Reflector.IForgeEntity_hasCustomOutlineRendering, this.minecraft.player))
                    {
                        flag4 = true;
                    }

                    multibuffersource1 = multibuffersource$buffersource;
                }

                if (flag1)
                {
                    Shaders.nextEntity(entity1);
                }

                float f2 = p_342180_.getGameTimeDeltaPartialTick(!tickratemanager.isEntityFrozen(entity1));
                this.renderEntity(entity1, d0, d1, d2, f2, posestack, multibuffersource1);
            }

            list1.clear();
        }

        multibuffersource$buffersource.endLastBatch();
        this.checkPoseStack(posestack);
        multibuffersource$buffersource.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
        multibuffersource$buffersource.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        multibuffersource$buffersource.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        multibuffersource$buffersource.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));

        if (Config.isFastRender())
        {
            multibuffersource$buffersource.flushCache();
            RenderStateManager.flushCache();
        }

        if (flag1)
        {
            Shaders.endEntities();
        }

        ReflectorForge.dispatchRenderStageS(
            Reflector.RenderLevelStageEvent_Stage_AFTER_ENTITIES, this, p_254120_, p_330527_, this.ticks, p_109604_, frustum
        );

        if (flag1)
        {
            Shaders.beginBlockEntities();
        }

        profilerfiller.popPush("blockentities");
        SignRenderer.updateTextRenderDistance();
        boolean flag6 = Reflector.IForgeBlockEntity_getRenderBoundingBox.exists();
        Frustum frustum1 = frustum;

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.renderInfosTileEntities)
        {
            List<BlockEntity> list2 = sectionrenderdispatcher$rendersection.getCompiled().getRenderableBlockEntities();

            if (!list2.isEmpty())
            {
                for (BlockEntity blockentity1 : list2)
                {
                    if (flag6)
                    {
                        AABB aabb = (AABB)Reflector.call(blockentity1, Reflector.IForgeBlockEntity_getRenderBoundingBox);

                        if (aabb != null && !frustum1.isVisible(aabb))
                        {
                            continue;
                        }
                    }

                    if (flag1)
                    {
                        Shaders.nextBlockEntity(blockentity1);
                    }

                    BlockPos blockpos4 = blockentity1.getBlockPos();
                    MultiBufferSource multibuffersource = multibuffersource$buffersource;
                    posestack.pushPose();
                    posestack.translate((double)blockpos4.getX() - d0, (double)blockpos4.getY() - d1, (double)blockpos4.getZ() - d2);
                    SortedSet<BlockDestructionProgress> sortedset = this.destructionProgress.get(blockpos4.asLong());

                    if (sortedset != null && !sortedset.isEmpty())
                    {
                        int l = sortedset.last().getProgress();

                        if (l >= 0)
                        {
                            PoseStack.Pose posestack$pose = posestack.last();
                            VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(
                                this.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(l)), posestack$pose, 1.0F
                            );
                            multibuffersource = renderTypeIn ->
                            {
                                VertexConsumer vertexconsumer3 = multibuffersource$buffersource.getBuffer(renderTypeIn);
                                return renderTypeIn.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, vertexconsumer3) : vertexconsumer3;
                            };
                        }
                    }

                    if (Reflector.IForgeBlockEntity_hasCustomOutlineRendering.exists()
                            && this.shouldShowEntityOutlines()
                            && Reflector.callBoolean(blockentity1, Reflector.IForgeBlockEntity_hasCustomOutlineRendering, this.minecraft.player))
                    {
                        flag4 = true;
                    }

                    this.blockEntityRenderDispatcher.render(blockentity1, f, posestack, multibuffersource);
                    posestack.popPose();
                    this.countTileEntitiesRendered++;
                }
            }
        }

        synchronized (this.globalBlockEntities)
        {
            for (BlockEntity blockentity : this.globalBlockEntities)
            {
                if (flag6)
                {
                    AABB aabb1 = (AABB)Reflector.call(blockentity, Reflector.IForgeBlockEntity_getRenderBoundingBox);

                    if (aabb1 != null && !frustum1.isVisible(aabb1))
                    {
                        continue;
                    }
                }

                if (flag1)
                {
                    Shaders.nextBlockEntity(blockentity);
                }

                BlockPos blockpos3 = blockentity.getBlockPos();
                posestack.pushPose();
                posestack.translate((double)blockpos3.getX() - d0, (double)blockpos3.getY() - d1, (double)blockpos3.getZ() - d2);

                if (Reflector.IForgeBlockEntity_hasCustomOutlineRendering.exists()
                        && this.shouldShowEntityOutlines()
                        && Reflector.callBoolean(blockentity, Reflector.IForgeBlockEntity_hasCustomOutlineRendering, this.minecraft.player))
                {
                    flag4 = true;
                }

                this.blockEntityRenderDispatcher.render(blockentity, f, posestack, multibuffersource$buffersource);
                posestack.popPose();
                this.countTileEntitiesRendered++;
            }
        }

        this.checkPoseStack(posestack);
        multibuffersource$buffersource.endBatch(RenderType.solid());
        multibuffersource$buffersource.endBatch(RenderType.endPortal());
        multibuffersource$buffersource.endBatch(RenderType.endGateway());
        multibuffersource$buffersource.endBatch(Sheets.solidBlockSheet());
        multibuffersource$buffersource.endBatch(Sheets.cutoutBlockSheet());
        multibuffersource$buffersource.endBatch(Sheets.bedSheet());
        multibuffersource$buffersource.endBatch(Sheets.shulkerBoxSheet());
        multibuffersource$buffersource.endBatch(Sheets.signSheet());
        multibuffersource$buffersource.endBatch(Sheets.hangingSignSheet());
        multibuffersource$buffersource.endBatch(Sheets.chestSheet());
        this.renderBuffers.outlineBufferSource().endOutlineBatch();

        if (Config.isFastRender())
        {
            multibuffersource$buffersource.disableCache();
            RenderStateManager.disableCache();
        }

        Lagometer.timerTerrain.end();

        if (flag4)
        {
            this.entityEffect.process(p_342180_.getGameTimeDeltaTicks());
            this.minecraft.getMainRenderTarget().bindWrite(false);
        }

        if (flag1)
        {
            Shaders.endBlockEntities();
        }

        ReflectorForge.dispatchRenderStageS(
            Reflector.RenderLevelStageEvent_Stage_AFTER_BLOCK_ENTITIES, this, p_254120_, p_330527_, this.ticks, p_109604_, frustum
        );
        this.renderOverlayDamaged = true;
        profilerfiller.popPush("destroyProgress");

        for (Entry<SortedSet<BlockDestructionProgress>> entry : this.destructionProgress.long2ObjectEntrySet())
        {
            BlockPos blockpos2 = BlockPos.of(entry.getLongKey());
            double d3 = (double)blockpos2.getX() - d0;
            double d4 = (double)blockpos2.getY() - d1;
            double d5 = (double)blockpos2.getZ() - d2;

            if (!(d3 * d3 + d4 * d4 + d5 * d5 > 1024.0))
            {
                SortedSet<BlockDestructionProgress> sortedset1 = entry.getValue();

                if (sortedset1 != null && !sortedset1.isEmpty())
                {
                    int i1 = sortedset1.last().getProgress();
                    posestack.pushPose();
                    posestack.translate((double)blockpos2.getX() - d0, (double)blockpos2.getY() - d1, (double)blockpos2.getZ() - d2);
                    PoseStack.Pose posestack$pose1 = posestack.last();
                    VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(
                        this.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(i1)), posestack$pose1, 1.0F
                    );
                    ModelData modeldata = this.level.getModelDataManager().getAt(blockpos2);

                    if (modeldata == null)
                    {
                        modeldata = ModelData.EMPTY;
                    }

                    this.minecraft
                    .getBlockRenderer()
                    .renderBreakingTexture(this.level.getBlockState(blockpos2), blockpos2, this.level, posestack, vertexconsumer1, modeldata);
                    posestack.popPose();
                }
            }
        }

        this.renderOverlayDamaged = false;
        RenderUtils.flushRenderBuffers();
        renderEntitiesCounter--;
        this.checkPoseStack(posestack);
        HitResult hitresult = this.minecraft.hitResult;

        if (p_109603_ && hitresult != null && hitresult.getType() == HitResult.Type.BLOCK)
        {
            profilerfiller.popPush("outline");
            BlockPos blockpos1 = ((BlockHitResult)hitresult).getBlockPos();
            BlockState blockstate = this.level.getBlockState(blockpos1);

            if (flag1)
            {
                ShadersRender.beginOutline();
            }

            if (!Reflector.callBoolean(Reflector.ForgeHooksClient_onDrawHighlight, this, p_109604_, hitresult, f, posestack, multibuffersource$buffersource)
                    && !blockstate.isAir()
                    && this.level.getWorldBorder().isWithinBounds(blockpos1))
            {
                VertexConsumer vertexconsumer2 = multibuffersource$buffersource.getBuffer(RenderType.lines());
                this.renderHitOutline(posestack, vertexconsumer2, p_109604_.getEntity(), d0, d1, d2, blockpos1, blockstate);
            }

            if (flag1)
            {
                multibuffersource$buffersource.endBatch(RenderType.lines());
                ShadersRender.endOutline();
            }
        }
        else if (hitresult != null && hitresult.getType() == HitResult.Type.ENTITY)
        {
            Reflector.ForgeHooksClient_onDrawHighlight.call(this, p_109604_, hitresult, f, posestack, multibuffersource$buffersource);
        }

        this.minecraft.debugRenderer.render(posestack, multibuffersource$buffersource, d0, d1, d2);
        multibuffersource$buffersource.endLastBatch();

        if (flag1)
        {
            RenderUtils.finishRenderBuffers();
            ShadersRender.beginDebug();
        }

        multibuffersource$buffersource.endBatch(Sheets.translucentCullBlockSheet());
        multibuffersource$buffersource.endBatch(Sheets.bannerSheet());
        multibuffersource$buffersource.endBatch(Sheets.shieldSheet());
        multibuffersource$buffersource.endBatch(RenderType.armorEntityGlint());
        multibuffersource$buffersource.endBatch(RenderType.glint());
        multibuffersource$buffersource.endBatch(RenderType.glintTranslucent());
        multibuffersource$buffersource.endBatch(RenderType.entityGlint());
        multibuffersource$buffersource.endBatch(RenderType.entityGlintDirect());
        multibuffersource$buffersource.endBatch(RenderType.waterMask());
        this.renderBuffers.crumblingBufferSource().endBatch();

        if (flag1)
        {
            multibuffersource$buffersource.endBatch();
            ShadersRender.endDebug();
            Shaders.preRenderHand();
            Matrix4f matrix4f = MathUtils.copy(RenderSystem.getProjectionMatrix());
            ShadersRender.renderHand0(p_109605_, p_254120_, p_109604_, f);
            RenderSystem.setProjectionMatrix(matrix4f, RenderSystem.getVertexSorting());
            Shaders.preWater();
        }

        if (this.transparencyChain != null)
        {
            multibuffersource$buffersource.endBatch(RenderType.lines());
            multibuffersource$buffersource.endBatch();
            this.translucentTarget.clear(Minecraft.ON_OSX);
            this.translucentTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
            profilerfiller.popPush("translucent");
            this.renderSectionLayer(RenderType.translucent(), d0, d1, d2, p_254120_, p_330527_);
            profilerfiller.popPush("string");
            this.renderSectionLayer(RenderType.tripwire(), d0, d1, d2, p_254120_, p_330527_);
            this.particlesTarget.clear(Minecraft.ON_OSX);
            this.particlesTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
            RenderStateShard.PARTICLES_TARGET.setupRenderState();
            profilerfiller.popPush("particles");
            this.minecraft.particleEngine.render(p_109606_, p_109604_, f, frustum);
            ReflectorForge.dispatchRenderStageS(
                Reflector.RenderLevelStageEvent_Stage_AFTER_PARTICLES, this, p_254120_, p_330527_, this.ticks, p_109604_, frustum
            );
            RenderStateShard.PARTICLES_TARGET.clearRenderState();
        }
        else
        {
            profilerfiller.popPush("translucent");
            Lagometer.timerTerrain.start();

            if (Shaders.isParticlesBeforeDeferred())
            {
                Shaders.beginParticles();
                this.minecraft.particleEngine.render(p_109606_, p_109604_, f, frustum);
                Shaders.endParticles();
                ReflectorForge.dispatchRenderStageS(
                    Reflector.RenderLevelStageEvent_Stage_AFTER_PARTICLES, this, p_254120_, p_330527_, this.ticks, p_109604_, frustum
                );
            }

            if (flag1)
            {
                Shaders.beginWater();
            }

            if (this.translucentTarget != null)
            {
                this.translucentTarget.clear(Minecraft.ON_OSX);
            }

            this.renderSectionLayer(RenderType.translucent(), d0, d1, d2, p_254120_, p_330527_);

            if (flag1)
            {
                Shaders.endWater();
            }

            Lagometer.timerTerrain.end();
            multibuffersource$buffersource.endBatch(RenderType.lines());
            multibuffersource$buffersource.endBatch();
            profilerfiller.popPush("string");
            this.renderSectionLayer(RenderType.tripwire(), d0, d1, d2, p_254120_, p_330527_);
            profilerfiller.popPush("particles");

            if (!Shaders.isParticlesBeforeDeferred())
            {
                if (flag1)
                {
                    Shaders.beginParticles();
                }

                this.minecraft.particleEngine.render(p_109606_, p_109604_, f, frustum);

                if (flag1)
                {
                    Shaders.endParticles();
                }

                ReflectorForge.dispatchRenderStageS(
                    Reflector.RenderLevelStageEvent_Stage_AFTER_PARTICLES, this, p_254120_, p_330527_, this.ticks, p_109604_, frustum
                );
            }
        }

        RenderSystem.setFogAllowed(true);

        if (this.minecraft.options.getCloudsType() != CloudStatus.OFF)
        {
            if (this.transparencyChain != null)
            {
                this.cloudsTarget.clear(Minecraft.ON_OSX);
            }

            profilerfiller.popPush("clouds");
            this.renderClouds(posestack, p_254120_, p_330527_, f, d0, d1, d2);
        }

        if (this.transparencyChain != null)
        {
            RenderStateShard.WEATHER_TARGET.setupRenderState();
            profilerfiller.popPush("weather");
            this.renderSnowAndRain(p_109606_, f, d0, d1, d2);
            ReflectorForge.dispatchRenderStageS(
                Reflector.RenderLevelStageEvent_Stage_AFTER_WEATHER, this, p_254120_, p_330527_, this.ticks, p_109604_, frustum
            );
            this.renderWorldBorder(p_109604_);
            RenderStateShard.WEATHER_TARGET.clearRenderState();
            this.transparencyChain.process(p_342180_.getGameTimeDeltaTicks());
            this.minecraft.getMainRenderTarget().bindWrite(false);
        }
        else
        {
            RenderSystem.depthMask(false);
            profilerfiller.popPush("weather");

            if (flag1)
            {
                Shaders.beginWeather();
            }

            this.renderSnowAndRain(p_109606_, f, d0, d1, d2);

            if (flag1)
            {
                Shaders.endWeather();
            }

            ReflectorForge.dispatchRenderStageS(
                Reflector.RenderLevelStageEvent_Stage_AFTER_WEATHER, this, p_254120_, p_330527_, this.ticks, p_109604_, frustum
            );
            this.renderWorldBorder(p_109604_);
            RenderSystem.depthMask(true);
        }

        this.renderDebug(posestack, multibuffersource$buffersource, p_109604_);
        multibuffersource$buffersource.endLastBatch();
        matrix4fstack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        FogRenderer.setupNoFog();
    }

    public void checkPoseStack(PoseStack p_109589_)
    {
        if (!p_109589_.clear())
        {
            throw new IllegalStateException("Pose stack not empty");
        }
    }

    public void renderEntity(
        Entity p_109518_, double p_109519_, double p_109520_, double p_109521_, float p_109522_, PoseStack p_109523_, MultiBufferSource p_109524_
    )
    {
        double d0 = Mth.lerp((double)p_109522_, p_109518_.xOld, p_109518_.getX());
        double d1 = Mth.lerp((double)p_109522_, p_109518_.yOld, p_109518_.getY());
        double d2 = Mth.lerp((double)p_109522_, p_109518_.zOld, p_109518_.getZ());
        float f = Mth.lerp(p_109522_, p_109518_.yRotO, p_109518_.getYRot());
        this.entityRenderDispatcher
        .render(
            p_109518_, d0 - p_109519_, d1 - p_109520_, d2 - p_109521_, f, p_109522_, p_109523_, p_109524_, this.entityRenderDispatcher.getPackedLightCoords(p_109518_, p_109522_)
        );
    }

    public void renderSectionLayer(RenderType p_298012_, double p_298706_, double p_299730_, double p_298956_, Matrix4f p_297481_, Matrix4f p_333714_)
    {
        RenderSystem.assertOnRenderThread();
        p_298012_.setupRenderState();
        boolean flag = Config.isShaders();

        if (p_298012_ == RenderType.translucent() && !Shaders.isShadowPass)
        {
            this.minecraft.getProfiler().push("translucent_sort");
            double d0 = p_298706_ - this.xTransparentOld;
            double d1 = p_299730_ - this.yTransparentOld;
            double d2 = p_298956_ - this.zTransparentOld;

            if (d0 * d0 + d1 * d1 + d2 * d2 > 1.0)
            {
                int i = SectionPos.posToSectionCoord(p_298706_);
                int j = SectionPos.posToSectionCoord(p_299730_);
                int k = SectionPos.posToSectionCoord(p_298956_);
                boolean flag1 = i != SectionPos.posToSectionCoord(this.xTransparentOld)
                                || k != SectionPos.posToSectionCoord(this.zTransparentOld)
                                || j != SectionPos.posToSectionCoord(this.yTransparentOld);
                this.xTransparentOld = p_298706_;
                this.yTransparentOld = p_299730_;
                this.zTransparentOld = p_298956_;
                int l = 0;
                this.chunksToResortTransparency.clear();

                for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 : this.renderInfosTerrain)
                {
                    if (l < 15
                            && (flag1 || sectionrenderdispatcher$rendersection1.isAxisAlignedWith(i, j, k))
                            && sectionrenderdispatcher$rendersection1.getCompiled().isLayerUsed(p_298012_))
                    {
                        this.chunksToResortTransparency.add(sectionrenderdispatcher$rendersection1);
                        l++;
                    }
                }
            }

            this.minecraft.getProfiler().pop();
        }

        this.minecraft.getProfiler().push("filterempty");
        this.minecraft.getProfiler().popPush(() -> "render_" + p_298012_);
        boolean flag2 = p_298012_ != RenderType.translucent();
        ObjectListIterator<SectionRenderDispatcher.RenderSection> objectlistiterator = this.renderInfosTerrain
                .listIterator(flag2 ? 0 : this.renderInfosTerrain.size());
        ShaderInstance shaderinstance = RenderSystem.getShader();
        shaderinstance.setDefaultUniforms(VertexFormat.Mode.QUADS, p_297481_, p_333714_, this.minecraft.getWindow());
        shaderinstance.apply();
        Uniform uniform = shaderinstance.CHUNK_OFFSET;

        if (flag)
        {
            ShadersRender.preRenderChunkLayer(p_298012_);
            Shaders.setModelViewMatrix(p_297481_);
            Shaders.setProjectionMatrix(p_333714_);
            Shaders.setTextureMatrix(RenderSystem.getTextureMatrix());
            Shaders.setColorModulator(RenderSystem.getShaderColor());
        }

        boolean flag3 = SmartAnimations.isActive();

        if (flag && Shaders.activeProgramID > 0)
        {
            uniform = null;
        }

        if (Config.isRenderRegions() && !p_298012_.isNeedsSorting())
        {
            int i1 = Integer.MIN_VALUE;
            int j1 = Integer.MIN_VALUE;
            VboRegion vboregion2 = null;
            Map<PairInt, Map<VboRegion, List<VertexBuffer>>> map1 = (Map<PairInt, Map<VboRegion, List<VertexBuffer>>>)this.mapRegionLayers
                    .computeIfAbsent(p_298012_, k -> new LinkedHashMap(16));
            Map<VboRegion, List<VertexBuffer>> map2 = null;
            List<VertexBuffer> list1 = null;

            while (flag2 ? objectlistiterator.hasNext() : objectlistiterator.hasPrevious())
            {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection2 = flag2
                        ? objectlistiterator.next()
                        : objectlistiterator.previous();

                if (!sectionrenderdispatcher$rendersection2.getCompiled().isEmpty(p_298012_))
                {
                    VertexBuffer vertexbuffer2 = sectionrenderdispatcher$rendersection2.getBuffer(p_298012_);
                    VboRegion vboregion = vertexbuffer2.getVboRegion();

                    if (sectionrenderdispatcher$rendersection2.regionX != i1 || sectionrenderdispatcher$rendersection2.regionZ != j1)
                    {
                        PairInt pairint = PairInt.of(sectionrenderdispatcher$rendersection2.regionX, sectionrenderdispatcher$rendersection2.regionZ);
                        map2 = map1.computeIfAbsent(pairint, k -> new LinkedHashMap<>(8));
                        i1 = sectionrenderdispatcher$rendersection2.regionX;
                        j1 = sectionrenderdispatcher$rendersection2.regionZ;
                        vboregion2 = null;
                    }

                    if (vboregion != vboregion2)
                    {
                        list1 = map2.computeIfAbsent(vboregion, k -> new ArrayList<>());
                        vboregion2 = vboregion;
                    }

                    list1.add(vertexbuffer2);

                    if (flag3)
                    {
                        BitSet bitset1 = sectionrenderdispatcher$rendersection2.getCompiled().getAnimatedSprites(p_298012_);

                        if (bitset1 != null)
                        {
                            SmartAnimations.spritesRendered(bitset1);
                        }
                    }
                }
            }

            for (java.util.Map.Entry<PairInt, Map<VboRegion, List<VertexBuffer>>> entry1 : map1.entrySet())
            {
                PairInt pairint1 = entry1.getKey();
                Map<VboRegion, List<VertexBuffer>> map = entry1.getValue();

                for (java.util.Map.Entry<VboRegion, List<VertexBuffer>> entry : map.entrySet())
                {
                    VboRegion vboregion1 = entry.getKey();
                    List<VertexBuffer> list = entry.getValue();

                    if (!list.isEmpty())
                    {
                        for (VertexBuffer vertexbuffer : list)
                        {
                            vertexbuffer.draw();
                        }

                        this.drawRegion(pairint1.getLeft(), 0, pairint1.getRight(), p_298706_, p_299730_, p_298956_, vboregion1, uniform, flag);
                        list.clear();
                    }
                }
            }
        }
        else
        {
            while (flag2 ? objectlistiterator.hasNext() : objectlistiterator.hasPrevious())
            {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = flag2 ? objectlistiterator.next() : objectlistiterator.previous();

                if (!sectionrenderdispatcher$rendersection.getCompiled().isEmpty(p_298012_))
                {
                    VertexBuffer vertexbuffer1 = sectionrenderdispatcher$rendersection.getBuffer(p_298012_);
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getOrigin();

                    if (uniform != null)
                    {
                        uniform.set(
                            (float)((double)blockpos.getX() - p_298706_ - (double)sectionrenderdispatcher$rendersection.regionDX),
                            (float)((double)blockpos.getY() - p_299730_ - (double)sectionrenderdispatcher$rendersection.regionDY),
                            (float)((double)blockpos.getZ() - p_298956_ - (double)sectionrenderdispatcher$rendersection.regionDZ)
                        );
                        uniform.upload();
                    }

                    if (flag)
                    {
                        Shaders.uniform_chunkOffset
                        .setValue(
                            (float)((double)blockpos.getX() - p_298706_ - (double)sectionrenderdispatcher$rendersection.regionDX),
                            (float)((double)blockpos.getY() - p_299730_ - (double)sectionrenderdispatcher$rendersection.regionDY),
                            (float)((double)blockpos.getZ() - p_298956_ - (double)sectionrenderdispatcher$rendersection.regionDZ)
                        );
                    }

                    if (flag3)
                    {
                        BitSet bitset = sectionrenderdispatcher$rendersection.getCompiled().getAnimatedSprites(p_298012_);

                        if (bitset != null)
                        {
                            SmartAnimations.spritesRendered(bitset);
                        }
                    }

                    vertexbuffer1.bind();
                    vertexbuffer1.draw();
                }
            }
        }

        if (Config.isMultiTexture())
        {
            this.minecraft.getTextureManager().bindForSetup(TextureAtlas.LOCATION_BLOCKS);
        }

        if (uniform != null)
        {
            uniform.set(0.0F, 0.0F, 0.0F);
        }

        if (flag)
        {
            Shaders.uniform_chunkOffset.setValue(0.0F, 0.0F, 0.0F);
        }

        shaderinstance.clear();
        VertexBuffer.unbind();
        this.minecraft.getProfiler().pop();

        if (flag)
        {
            ShadersRender.postRenderChunkLayer(p_298012_);
        }

        if (Reflector.ForgeHooksClient_dispatchRenderStageRT.exists())
        {
            Reflector.ForgeHooksClient_dispatchRenderStageRT
            .call(p_298012_, this, p_297481_, p_333714_, this.ticks, this.minecraft.gameRenderer.getMainCamera(), this.getFrustum());
        }

        p_298012_.clearRenderState();
    }

    private void drawRegion(int regionX, int regionY, int regionZ, double xIn, double yIn, double zIn, VboRegion vboRegion, Uniform uniform, boolean isShaders)
    {
        if (uniform != null)
        {
            uniform.set((float)((double)regionX - xIn), (float)((double)regionY - yIn), (float)((double)regionZ - zIn));
            uniform.upload();
        }

        if (isShaders)
        {
            Shaders.uniform_chunkOffset.setValue((float)((double)regionX - xIn), (float)((double)regionY - yIn), (float)((double)regionZ - zIn));
        }

        vboRegion.finishDraw();
    }

    private void renderDebug(PoseStack p_271014_, MultiBufferSource p_270107_, Camera p_270483_)
    {
        if (this.minecraft.sectionPath || this.minecraft.sectionVisibility)
        {
            if (Config.isShaders())
            {
                Shaders.pushUseProgram(Shaders.ProgramBasic);
            }

            double d0 = p_270483_.getPosition().x();
            double d1 = p_270483_.getPosition().y();
            double d2 = p_270483_.getPosition().z();

            for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.visibleSections)
            {
                SectionOcclusionGraph.Node sectionocclusiongraph$node = this.sectionOcclusionGraph.getNode(sectionrenderdispatcher$rendersection);

                if (sectionocclusiongraph$node != null)
                {
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getOrigin();
                    p_271014_.pushPose();
                    p_271014_.translate((double)blockpos.getX() - d0, (double)blockpos.getY() - d1, (double)blockpos.getZ() - d2);
                    Matrix4f matrix4f = p_271014_.last().pose();

                    if (this.minecraft.sectionPath)
                    {
                        if (Config.isShaders())
                        {
                            Shaders.beginLines();
                        }

                        VertexConsumer vertexconsumer1 = p_270107_.getBuffer(RenderType.lines());
                        int i = sectionocclusiongraph$node.step == 0 ? 0 : Mth.hsvToRgb((float)sectionocclusiongraph$node.step / 50.0F, 0.9F, 0.9F);
                        int j = i >> 16 & 0xFF;
                        int k = i >> 8 & 0xFF;
                        int l = i & 0xFF;

                        for (int i1 = 0; i1 < DIRECTIONS.length; i1++)
                        {
                            if (sectionocclusiongraph$node.hasSourceDirection(i1))
                            {
                                Direction direction = DIRECTIONS[i1];
                                vertexconsumer1.addVertex(matrix4f, 8.0F, 8.0F, 8.0F)
                                .setColor(j, k, l, 255)
                                .setNormal((float)direction.getStepX(), (float)direction.getStepY(), (float)direction.getStepZ());
                                vertexconsumer1.addVertex(
                                    matrix4f,
                                    (float)(8 - 16 * direction.getStepX()),
                                    (float)(8 - 16 * direction.getStepY()),
                                    (float)(8 - 16 * direction.getStepZ())
                                )
                                .setColor(j, k, l, 255)
                                .setNormal((float)direction.getStepX(), (float)direction.getStepY(), (float)direction.getStepZ());
                            }
                        }

                        if (Config.isShaders())
                        {
                            Shaders.endLines();
                        }
                    }

                    if (this.minecraft.sectionVisibility && !sectionrenderdispatcher$rendersection.getCompiled().hasNoRenderableLayers())
                    {
                        if (Config.isShaders())
                        {
                            Shaders.beginLines();
                        }

                        VertexConsumer vertexconsumer3 = p_270107_.getBuffer(RenderType.lines());
                        int j1 = 0;

                        for (Direction direction2 : DIRECTIONS)
                        {
                            for (Direction direction1 : DIRECTIONS)
                            {
                                boolean flag = sectionrenderdispatcher$rendersection.getCompiled().facesCanSeeEachother(direction2, direction1);

                                if (!flag)
                                {
                                    j1++;
                                    vertexconsumer3.addVertex(
                                        matrix4f,
                                        (float)(8 + 8 * direction2.getStepX()),
                                        (float)(8 + 8 * direction2.getStepY()),
                                        (float)(8 + 8 * direction2.getStepZ())
                                    )
                                    .setColor(255, 0, 0, 255)
                                    .setNormal((float)direction2.getStepX(), (float)direction2.getStepY(), (float)direction2.getStepZ());
                                    vertexconsumer3.addVertex(
                                        matrix4f,
                                        (float)(8 + 8 * direction1.getStepX()),
                                        (float)(8 + 8 * direction1.getStepY()),
                                        (float)(8 + 8 * direction1.getStepZ())
                                    )
                                    .setColor(255, 0, 0, 255)
                                    .setNormal((float)direction1.getStepX(), (float)direction1.getStepY(), (float)direction1.getStepZ());
                                }
                            }
                        }

                        if (Config.isShaders())
                        {
                            Shaders.endLines();
                        }

                        if (j1 > 0)
                        {
                            VertexConsumer vertexconsumer4 = p_270107_.getBuffer(RenderType.debugQuads());
                            float f = 0.5F;
                            float f1 = 0.2F;
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        }
                    }

                    p_271014_.popPose();
                }

                if (Config.isShaders())
                {
                    Shaders.popProgram();
                }
            }
        }

        if (this.capturedFrustum != null)
        {
            if (Config.isShaders())
            {
                Shaders.pushUseProgram(Shaders.ProgramBasic);
            }

            p_271014_.pushPose();
            p_271014_.translate(
                (float)(this.frustumPos.x - p_270483_.getPosition().x),
                (float)(this.frustumPos.y - p_270483_.getPosition().y),
                (float)(this.frustumPos.z - p_270483_.getPosition().z)
            );
            Matrix4f matrix4f1 = p_271014_.last().pose();
            VertexConsumer vertexconsumer = p_270107_.getBuffer(RenderType.debugQuads());
            this.addFrustumQuad(vertexconsumer, matrix4f1, 0, 1, 2, 3, 0, 1, 1);
            this.addFrustumQuad(vertexconsumer, matrix4f1, 4, 5, 6, 7, 1, 0, 0);
            this.addFrustumQuad(vertexconsumer, matrix4f1, 0, 1, 5, 4, 1, 1, 0);
            this.addFrustumQuad(vertexconsumer, matrix4f1, 2, 3, 7, 6, 0, 0, 1);
            this.addFrustumQuad(vertexconsumer, matrix4f1, 0, 4, 7, 3, 0, 1, 0);
            this.addFrustumQuad(vertexconsumer, matrix4f1, 1, 5, 6, 2, 1, 0, 1);

            if (Config.isShaders())
            {
                Shaders.beginLines();
            }

            VertexConsumer vertexconsumer2 = p_270107_.getBuffer(RenderType.lines());
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 0);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 1);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 1);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 2);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 2);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 3);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 3);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 0);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 4);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 5);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 5);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 6);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 6);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 7);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 7);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 4);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 0);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 4);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 1);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 5);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 2);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 6);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 3);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 7);

            if (Config.isShaders())
            {
                Shaders.endLines();
            }

            p_271014_.popPose();

            if (Config.isShaders())
            {
                Shaders.popProgram();
            }
        }
    }

    private void addFrustumVertex(VertexConsumer p_270950_, Matrix4f p_270118_, int p_270865_)
    {
        p_270950_.addVertex(p_270118_, this.frustumPoints[p_270865_].x(), this.frustumPoints[p_270865_].y(), this.frustumPoints[p_270865_].z())
        .setColor(-16777216)
        .setNormal(0.0F, 0.0F, -1.0F);
    }

    private void addFrustumQuad(
        VertexConsumer p_270858_, Matrix4f p_270341_, int p_270702_, int p_270959_, int p_270732_, int p_270363_, int p_270273_, int p_270934_, int p_270916_
    )
    {
        float f = 0.25F;
        p_270858_.addVertex(p_270341_, this.frustumPoints[p_270702_].x(), this.frustumPoints[p_270702_].y(), this.frustumPoints[p_270702_].z())
        .setColor((float)p_270273_, (float)p_270934_, (float)p_270916_, 0.25F);
        p_270858_.addVertex(p_270341_, this.frustumPoints[p_270959_].x(), this.frustumPoints[p_270959_].y(), this.frustumPoints[p_270959_].z())
        .setColor((float)p_270273_, (float)p_270934_, (float)p_270916_, 0.25F);
        p_270858_.addVertex(p_270341_, this.frustumPoints[p_270732_].x(), this.frustumPoints[p_270732_].y(), this.frustumPoints[p_270732_].z())
        .setColor((float)p_270273_, (float)p_270934_, (float)p_270916_, 0.25F);
        p_270858_.addVertex(p_270341_, this.frustumPoints[p_270363_].x(), this.frustumPoints[p_270363_].y(), this.frustumPoints[p_270363_].z())
        .setColor((float)p_270273_, (float)p_270934_, (float)p_270916_, 0.25F);
    }

    public void captureFrustum()
    {
        this.captureFrustum = true;
    }

    public void killFrustum()
    {
        this.capturedFrustum = null;
    }

    public void tick()
    {
        if (this.level.tickRateManager().runsNormally())
        {
            this.ticks++;
        }

        if (this.ticks % 20 == 0)
        {
            Iterator<BlockDestructionProgress> iterator = this.destroyingBlocks.values().iterator();

            while (iterator.hasNext())
            {
                BlockDestructionProgress blockdestructionprogress = iterator.next();
                int i = blockdestructionprogress.getUpdatedRenderTick();

                if (this.ticks - i > 400)
                {
                    iterator.remove();
                    this.removeProgress(blockdestructionprogress);
                }
            }
        }

        if (Config.isRenderRegions() && this.ticks % 20 == 0)
        {
            this.mapRegionLayers.clear();
        }
    }

    private void removeProgress(BlockDestructionProgress p_109766_)
    {
        long i = p_109766_.getPos().asLong();
        Set<BlockDestructionProgress> set = this.destructionProgress.get(i);
        set.remove(p_109766_);

        if (set.isEmpty())
        {
            this.destructionProgress.remove(i);
        }
    }

    private void renderEndSky(PoseStack p_109781_)
    {
        if (Config.isSkyEnabled())
        {
            RenderSystem.enableBlend();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, END_SKY_LOCATION);
            Tesselator tesselator = Tesselator.getInstance();

            for (int i = 0; i < 6; i++)
            {
                p_109781_.pushPose();

                if (i == 1)
                {
                    p_109781_.mulPose(Axis.XP.rotationDegrees(90.0F));
                }

                if (i == 2)
                {
                    p_109781_.mulPose(Axis.XP.rotationDegrees(-90.0F));
                }

                if (i == 3)
                {
                    p_109781_.mulPose(Axis.XP.rotationDegrees(180.0F));
                }

                if (i == 4)
                {
                    p_109781_.mulPose(Axis.ZP.rotationDegrees(90.0F));
                }

                if (i == 5)
                {
                    p_109781_.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                }

                Matrix4f matrix4f = p_109781_.last().pose();
                BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                int j = -14145496;

                if (Config.isCustomColors())
                {
                    int k = FastColor.ARGB32.red(j);
                    int l = FastColor.ARGB32.green(j);
                    int i1 = FastColor.ARGB32.blue(j);
                    Vec3 vec3 = new Vec3((double)k / 255.0, (double)l / 255.0, (double)i1 / 255.0);
                    vec3 = CustomColors.getWorldSkyColor(vec3, this.level, this.minecraft.getCameraEntity(), 0.0F);
                    k = (int)(vec3.x * 255.0);
                    l = (int)(vec3.y * 255.0);
                    i1 = (int)(vec3.z * 255.0);
                    j = FastColor.ARGB32.color(255, k, l, i1);
                }

                bufferbuilder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setUv(0.0F, 0.0F).setColor(j);
                bufferbuilder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setUv(0.0F, 16.0F).setColor(j);
                bufferbuilder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setUv(16.0F, 16.0F).setColor(j);
                bufferbuilder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setUv(16.0F, 0.0F).setColor(j);
                BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
                p_109781_.popPose();
            }

            CustomSky.renderSky(this.level, p_109781_, 0.0F);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    public void renderSky(Matrix4f p_254034_, Matrix4f p_333200_, float p_202426_, Camera p_202427_, boolean p_202428_, Runnable p_202429_)
    {
        p_202429_.run();

        if (!Reflector.IForgeDimensionSpecialEffects_renderSky.exists()
                || !Reflector.callBoolean(
                    this.level.effects(),
                    Reflector.IForgeDimensionSpecialEffects_renderSky,
                    this.level,
                    this.ticks,
                    p_202426_,
                    p_202427_,
                    p_254034_,
                    p_202428_,
                    p_202429_
                ))
        {
            if (!p_202428_)
            {
                FogType fogtype = p_202427_.getFluidInCamera();

                if (fogtype != FogType.POWDER_SNOW && fogtype != FogType.LAVA && !this.doesMobEffectBlockSky(p_202427_))
                {
                    PoseStack posestack = new PoseStack();
                    posestack.mulPose(p_254034_);

                    if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.END)
                    {
                        this.renderEndSky(posestack);
                    }
                    else if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL)
                    {
                        boolean flag = Config.isShaders();

                        if (flag)
                        {
                            Shaders.disableTexture2D();
                        }

                        Vec3 vec3 = this.level.getSkyColor(this.minecraft.gameRenderer.getMainCamera().getPosition(), p_202426_);
                        vec3 = CustomColors.getSkyColor(
                                   vec3,
                                   this.minecraft.level,
                                   this.minecraft.getCameraEntity().getX(),
                                   this.minecraft.getCameraEntity().getY() + 1.0,
                                   this.minecraft.getCameraEntity().getZ()
                               );

                        if (flag)
                        {
                            Shaders.setSkyColor(vec3);
                            RenderSystem.setColorToAttribute(true);
                        }

                        float f = (float)vec3.x;
                        float f1 = (float)vec3.y;
                        float f2 = (float)vec3.z;
                        FogRenderer.levelFogColor();
                        Tesselator tesselator = Tesselator.getInstance();
                        RenderSystem.depthMask(false);

                        if (flag)
                        {
                            Shaders.enableFog();
                        }

                        RenderSystem.setShaderColor(f, f1, f2, 1.0F);

                        if (flag)
                        {
                            Shaders.preSkyList(posestack);
                        }

                        ShaderInstance shaderinstance = RenderSystem.getShader();

                        if (Config.isSkyEnabled())
                        {
                            this.skyBuffer.bind();
                            this.skyBuffer.drawWithShader(posestack.last().pose(), p_333200_, shaderinstance);
                            VertexBuffer.unbind();
                        }

                        if (flag)
                        {
                            Shaders.disableFog();
                        }

                        RenderSystem.enableBlend();
                        float[] afloat = this.level.effects().getSunriseColor(this.level.getTimeOfDay(p_202426_), p_202426_);

                        if (afloat != null && Config.isSunMoonEnabled())
                        {
                            RenderSystem.setShader(GameRenderer::getPositionColorShader);

                            if (flag)
                            {
                                Shaders.disableTexture2D();
                            }

                            if (flag)
                            {
                                Shaders.setRenderStage(RenderStage.SUNSET);
                            }

                            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                            posestack.pushPose();
                            posestack.mulPose(Axis.XP.rotationDegrees(90.0F));
                            float f3 = Mth.sin(this.level.getSunAngle(p_202426_)) < 0.0F ? 180.0F : 0.0F;
                            posestack.mulPose(Axis.ZP.rotationDegrees(f3));
                            posestack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                            float f4 = afloat[0];
                            float f5 = afloat[1];
                            float f6 = afloat[2];
                            Matrix4f matrix4f = posestack.last().pose();
                            BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                            bufferbuilder.addVertex(matrix4f, 0.0F, 100.0F, 0.0F).setColor(f4, f5, f6, afloat[3]);
                            int i = 16;

                            for (int j = 0; j <= 16; j++)
                            {
                                float f7 = (float)j * (float)(Math.PI * 2) / 16.0F;
                                float f8 = Mth.sin(f7);
                                float f9 = Mth.cos(f7);
                                bufferbuilder.addVertex(matrix4f, f8 * 120.0F, f9 * 120.0F, -f9 * 40.0F * afloat[3])
                                .setColor(afloat[0], afloat[1], afloat[2], 0.0F);
                            }

                            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
                            posestack.popPose();
                        }

                        if (flag)
                        {
                            Shaders.enableTexture2D();
                        }

                        RenderSystem.blendFuncSeparate(
                            GlStateManager.SourceFactor.SRC_ALPHA,
                            GlStateManager.DestFactor.ONE,
                            GlStateManager.SourceFactor.ONE,
                            GlStateManager.DestFactor.ZERO
                        );
                        posestack.pushPose();
                        float f10 = 1.0F - this.level.getRainLevel(p_202426_);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, f10);
                        posestack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                        CustomSky.renderSky(this.level, posestack, p_202426_);

                        if (flag)
                        {
                            Shaders.preCelestialRotate(posestack);
                        }

                        posestack.mulPose(Axis.XP.rotationDegrees(this.level.getTimeOfDay(p_202426_) * 360.0F));

                        if (flag)
                        {
                            Shaders.postCelestialRotate(posestack);
                        }

                        Matrix4f matrix4f1 = posestack.last().pose();
                        float f11 = 30.0F;
                        RenderSystem.setShader(GameRenderer::getPositionTexShader);

                        if (Config.isSunTexture())
                        {
                            if (flag)
                            {
                                Shaders.setRenderStage(RenderStage.SUN);
                            }

                            RenderSystem.setShaderTexture(0, SUN_LOCATION);
                            BufferBuilder bufferbuilder1 = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                            bufferbuilder1.addVertex(matrix4f1, -f11, 100.0F, -f11).setUv(0.0F, 0.0F);
                            bufferbuilder1.addVertex(matrix4f1, f11, 100.0F, -f11).setUv(1.0F, 0.0F);
                            bufferbuilder1.addVertex(matrix4f1, f11, 100.0F, f11).setUv(1.0F, 1.0F);
                            bufferbuilder1.addVertex(matrix4f1, -f11, 100.0F, f11).setUv(0.0F, 1.0F);
                            BufferUploader.drawWithShader(bufferbuilder1.buildOrThrow());
                        }

                        f11 = 20.0F;

                        if (Config.isMoonTexture())
                        {
                            if (flag)
                            {
                                Shaders.setRenderStage(RenderStage.MOON);
                            }

                            RenderSystem.setShaderTexture(0, MOON_LOCATION);
                            int k = this.level.getMoonPhase();
                            int l = k % 4;
                            int i1 = k / 4 % 2;
                            float f13 = (float)(l + 0) / 4.0F;
                            float f14 = (float)(i1 + 0) / 2.0F;
                            float f15 = (float)(l + 1) / 4.0F;
                            float f16 = (float)(i1 + 1) / 2.0F;
                            BufferBuilder bufferbuilder2 = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                            bufferbuilder2.addVertex(matrix4f1, -f11, -100.0F, f11).setUv(f15, f16);
                            bufferbuilder2.addVertex(matrix4f1, f11, -100.0F, f11).setUv(f13, f16);
                            bufferbuilder2.addVertex(matrix4f1, f11, -100.0F, -f11).setUv(f13, f14);
                            bufferbuilder2.addVertex(matrix4f1, -f11, -100.0F, -f11).setUv(f15, f14);
                            BufferUploader.drawWithShader(bufferbuilder2.buildOrThrow());
                        }

                        if (flag)
                        {
                            Shaders.disableTexture2D();
                        }

                        float f12 = this.level.getStarBrightness(p_202426_) * f10;

                        if (f12 > 0.0F && Config.isStarsEnabled() && !CustomSky.hasSkyLayers(this.level))
                        {
                            if (flag)
                            {
                                Shaders.setRenderStage(RenderStage.STARS);
                            }

                            RenderSystem.setShaderColor(f12, f12, f12, f12);
                            FogRenderer.setupNoFog();
                            this.starBuffer.bind();
                            this.starBuffer.drawWithShader(posestack.last().pose(), p_333200_, GameRenderer.getPositionShader());
                            VertexBuffer.unbind();
                            p_202429_.run();
                        }

                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();

                        if (flag)
                        {
                            Shaders.enableFog();
                        }

                        posestack.popPose();

                        if (flag)
                        {
                            Shaders.disableTexture2D();
                        }

                        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
                        double d0 = this.minecraft.player.getEyePosition(p_202426_).y - this.level.getLevelData().getHorizonHeight(this.level);
                        boolean flag1 = false;

                        if (d0 < 0.0)
                        {
                            if (flag)
                            {
                                Shaders.setRenderStage(RenderStage.VOID);
                            }

                            posestack.pushPose();
                            posestack.translate(0.0F, 12.0F, 0.0F);
                            this.darkBuffer.bind();
                            this.darkBuffer.drawWithShader(posestack.last().pose(), p_333200_, shaderinstance);
                            VertexBuffer.unbind();
                            posestack.popPose();
                            flag1 = true;
                        }

                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                        if (flag)
                        {
                            RenderSystem.setColorToAttribute(false);
                        }

                        RenderSystem.depthMask(true);
                    }
                }
            }
        }
    }

    private boolean doesMobEffectBlockSky(Camera p_234311_)
    {
        return p_234311_.getEntity() instanceof LivingEntity livingentity
               ? livingentity.hasEffect(MobEffects.BLINDNESS) || livingentity.hasEffect(MobEffects.DARKNESS)
               : false;
    }

    public void renderClouds(PoseStack p_254145_, Matrix4f p_254537_, Matrix4f p_330176_, float p_254364_, double p_253843_, double p_253663_, double p_253795_)
    {
        if (!Config.isCloudsOff())
        {
            if (!Reflector.IForgeDimensionSpecialEffects_renderClouds.exists()
                    || !Reflector.callBoolean(
                        this.level.effects(),
                        Reflector.IForgeDimensionSpecialEffects_renderClouds,
                        this.level,
                        this.ticks,
                        p_254364_,
                        p_254145_,
                        p_253843_,
                        p_253663_,
                        p_253795_,
                        p_254537_
                    ))
            {
                float f = this.level.effects().getCloudHeight();

                if (!Float.isNaN(f))
                {
                    if (Config.isShaders())
                    {
                        Shaders.beginClouds();
                    }

                    float f1 = 12.0F;
                    float f2 = 4.0F;
                    double d0 = 2.0E-4;
                    double d1 = (double)(((float)this.ticks + p_254364_) * 0.03F);
                    double d2 = (p_253843_ + d1) / 12.0;
                    double d3 = (double)(f - (float)p_253663_ + 0.33F);
                    d3 += this.minecraft.options.ofCloudsHeight * 128.0;
                    double d4 = p_253795_ / 12.0 + 0.33F;
                    d2 -= (double)(Mth.floor(d2 / 2048.0) * 2048);
                    d4 -= (double)(Mth.floor(d4 / 2048.0) * 2048);
                    float f3 = (float)(d2 - (double)Mth.floor(d2));
                    float f4 = (float)(d3 / 4.0 - (double)Mth.floor(d3 / 4.0)) * 4.0F;
                    float f5 = (float)(d4 - (double)Mth.floor(d4));
                    Vec3 vec3 = this.level.getCloudColor(p_254364_);
                    int i = (int)Math.floor(d2);
                    int j = (int)Math.floor(d3 / 4.0);
                    int k = (int)Math.floor(d4);

                    if (i != this.prevCloudX
                            || j != this.prevCloudY
                            || k != this.prevCloudZ
                            || this.minecraft.options.getCloudsType() != this.prevCloudsType
                            || this.prevCloudColor.distanceToSqr(vec3) > 2.0E-4)
                    {
                        this.prevCloudX = i;
                        this.prevCloudY = j;
                        this.prevCloudZ = k;
                        this.prevCloudColor = vec3;
                        this.prevCloudsType = this.minecraft.options.getCloudsType();
                        this.generateClouds = true;
                    }

                    if (this.generateClouds)
                    {
                        this.generateClouds = false;

                        if (this.cloudBuffer != null)
                        {
                            this.cloudBuffer.close();
                        }

                        this.cloudBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                        this.cloudBuffer.bind();
                        this.cloudBuffer.upload(this.buildClouds(Tesselator.getInstance(), d2, d3, d4, vec3));
                        VertexBuffer.unbind();
                    }

                    FogRenderer.levelFogColor();
                    p_254145_.pushPose();
                    p_254145_.mulPose(p_254537_);
                    p_254145_.scale(12.0F, 1.0F, 12.0F);
                    p_254145_.translate(-f3, f4, -f5);

                    if (this.cloudBuffer != null)
                    {
                        this.cloudBuffer.bind();
                        int l = this.prevCloudsType == CloudStatus.FANCY ? 0 : 1;

                        for (int i1 = l; i1 < 2; i1++)
                        {
                            RenderType rendertype = i1 == 0 ? RenderType.cloudsDepthOnly() : RenderType.clouds();
                            rendertype.setupRenderState();
                            ShaderInstance shaderinstance = RenderSystem.getShader();
                            this.cloudBuffer.drawWithShader(p_254145_.last().pose(), p_330176_, shaderinstance);
                            rendertype.clearRenderState();
                        }

                        VertexBuffer.unbind();
                    }

                    p_254145_.popPose();

                    if (Config.isShaders())
                    {
                        Shaders.endClouds();
                    }
                }
            }
        }
    }

    private MeshData buildClouds(Tesselator p_343567_, double p_234263_, double p_234264_, double p_234265_, Vec3 p_234266_)
    {
        float f = 4.0F;
        float f1 = 0.00390625F;
        int i = 8;
        int j = 4;
        float f2 = 9.765625E-4F;
        float f3 = (float)Mth.floor(p_234263_) * 0.00390625F;
        float f4 = (float)Mth.floor(p_234265_) * 0.00390625F;
        float f5 = (float)p_234266_.x;
        float f6 = (float)p_234266_.y;
        float f7 = (float)p_234266_.z;
        float f8 = f5 * 0.9F;
        float f9 = f6 * 0.9F;
        float f10 = f7 * 0.9F;
        float f11 = f5 * 0.7F;
        float f12 = f6 * 0.7F;
        float f13 = f7 * 0.7F;
        float f14 = f5 * 0.8F;
        float f15 = f6 * 0.8F;
        float f16 = f7 * 0.8F;
        BufferBuilder bufferbuilder = p_343567_.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
        float f17 = (float)Math.floor(p_234264_ / 4.0) * 4.0F;

        if (Config.isCloudsFancy())
        {
            for (int k = -3; k <= 4; k++)
            {
                for (int l = -3; l <= 4; l++)
                {
                    float f18 = (float)(k * 8);
                    float f19 = (float)(l * 8);

                    if (f17 > -5.0F)
                    {
                        bufferbuilder.addVertex(f18 + 0.0F, f17 + 0.0F, f19 + 8.0F)
                        .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                        .setColor(f11, f12, f13, 0.8F)
                        .setNormal(0.0F, -1.0F, 0.0F);
                        bufferbuilder.addVertex(f18 + 8.0F, f17 + 0.0F, f19 + 8.0F)
                        .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                        .setColor(f11, f12, f13, 0.8F)
                        .setNormal(0.0F, -1.0F, 0.0F);
                        bufferbuilder.addVertex(f18 + 8.0F, f17 + 0.0F, f19 + 0.0F)
                        .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                        .setColor(f11, f12, f13, 0.8F)
                        .setNormal(0.0F, -1.0F, 0.0F);
                        bufferbuilder.addVertex(f18 + 0.0F, f17 + 0.0F, f19 + 0.0F)
                        .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                        .setColor(f11, f12, f13, 0.8F)
                        .setNormal(0.0F, -1.0F, 0.0F);
                    }

                    if (f17 <= 5.0F)
                    {
                        bufferbuilder.addVertex(f18 + 0.0F, f17 + 4.0F - 9.765625E-4F, f19 + 8.0F)
                        .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                        .setColor(f5, f6, f7, 0.8F)
                        .setNormal(0.0F, 1.0F, 0.0F);
                        bufferbuilder.addVertex(f18 + 8.0F, f17 + 4.0F - 9.765625E-4F, f19 + 8.0F)
                        .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                        .setColor(f5, f6, f7, 0.8F)
                        .setNormal(0.0F, 1.0F, 0.0F);
                        bufferbuilder.addVertex(f18 + 8.0F, f17 + 4.0F - 9.765625E-4F, f19 + 0.0F)
                        .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                        .setColor(f5, f6, f7, 0.8F)
                        .setNormal(0.0F, 1.0F, 0.0F);
                        bufferbuilder.addVertex(f18 + 0.0F, f17 + 4.0F - 9.765625E-4F, f19 + 0.0F)
                        .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                        .setColor(f5, f6, f7, 0.8F)
                        .setNormal(0.0F, 1.0F, 0.0F);
                    }

                    if (k > -1)
                    {
                        for (int i1 = 0; i1 < 8; i1++)
                        {
                            bufferbuilder.addVertex(f18 + (float)i1 + 0.0F, f17 + 0.0F, f19 + 8.0F)
                            .setUv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                            .setColor(f8, f9, f10, 0.8F)
                            .setNormal(-1.0F, 0.0F, 0.0F);
                            bufferbuilder.addVertex(f18 + (float)i1 + 0.0F, f17 + 4.0F, f19 + 8.0F)
                            .setUv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                            .setColor(f8, f9, f10, 0.8F)
                            .setNormal(-1.0F, 0.0F, 0.0F);
                            bufferbuilder.addVertex(f18 + (float)i1 + 0.0F, f17 + 4.0F, f19 + 0.0F)
                            .setUv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                            .setColor(f8, f9, f10, 0.8F)
                            .setNormal(-1.0F, 0.0F, 0.0F);
                            bufferbuilder.addVertex(f18 + (float)i1 + 0.0F, f17 + 0.0F, f19 + 0.0F)
                            .setUv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                            .setColor(f8, f9, f10, 0.8F)
                            .setNormal(-1.0F, 0.0F, 0.0F);
                        }
                    }

                    if (k <= 1)
                    {
                        for (int j2 = 0; j2 < 8; j2++)
                        {
                            bufferbuilder.addVertex(f18 + (float)j2 + 1.0F - 9.765625E-4F, f17 + 0.0F, f19 + 8.0F)
                            .setUv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                            .setColor(f8, f9, f10, 0.8F)
                            .setNormal(1.0F, 0.0F, 0.0F);
                            bufferbuilder.addVertex(f18 + (float)j2 + 1.0F - 9.765625E-4F, f17 + 4.0F, f19 + 8.0F)
                            .setUv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                            .setColor(f8, f9, f10, 0.8F)
                            .setNormal(1.0F, 0.0F, 0.0F);
                            bufferbuilder.addVertex(f18 + (float)j2 + 1.0F - 9.765625E-4F, f17 + 4.0F, f19 + 0.0F)
                            .setUv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                            .setColor(f8, f9, f10, 0.8F)
                            .setNormal(1.0F, 0.0F, 0.0F);
                            bufferbuilder.addVertex(f18 + (float)j2 + 1.0F - 9.765625E-4F, f17 + 0.0F, f19 + 0.0F)
                            .setUv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                            .setColor(f8, f9, f10, 0.8F)
                            .setNormal(1.0F, 0.0F, 0.0F);
                        }
                    }

                    if (l > -1)
                    {
                        for (int k2 = 0; k2 < 8; k2++)
                        {
                            bufferbuilder.addVertex(f18 + 0.0F, f17 + 4.0F, f19 + (float)k2 + 0.0F)
                            .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4)
                            .setColor(f14, f15, f16, 0.8F)
                            .setNormal(0.0F, 0.0F, -1.0F);
                            bufferbuilder.addVertex(f18 + 8.0F, f17 + 4.0F, f19 + (float)k2 + 0.0F)
                            .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4)
                            .setColor(f14, f15, f16, 0.8F)
                            .setNormal(0.0F, 0.0F, -1.0F);
                            bufferbuilder.addVertex(f18 + 8.0F, f17 + 0.0F, f19 + (float)k2 + 0.0F)
                            .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4)
                            .setColor(f14, f15, f16, 0.8F)
                            .setNormal(0.0F, 0.0F, -1.0F);
                            bufferbuilder.addVertex(f18 + 0.0F, f17 + 0.0F, f19 + (float)k2 + 0.0F)
                            .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4)
                            .setColor(f14, f15, f16, 0.8F)
                            .setNormal(0.0F, 0.0F, -1.0F);
                        }
                    }

                    if (l <= 1)
                    {
                        for (int l2 = 0; l2 < 8; l2++)
                        {
                            bufferbuilder.addVertex(f18 + 0.0F, f17 + 4.0F, f19 + (float)l2 + 1.0F - 9.765625E-4F)
                            .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4)
                            .setColor(f14, f15, f16, 0.8F)
                            .setNormal(0.0F, 0.0F, 1.0F);
                            bufferbuilder.addVertex(f18 + 8.0F, f17 + 4.0F, f19 + (float)l2 + 1.0F - 9.765625E-4F)
                            .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4)
                            .setColor(f14, f15, f16, 0.8F)
                            .setNormal(0.0F, 0.0F, 1.0F);
                            bufferbuilder.addVertex(f18 + 8.0F, f17 + 0.0F, f19 + (float)l2 + 1.0F - 9.765625E-4F)
                            .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4)
                            .setColor(f14, f15, f16, 0.8F)
                            .setNormal(0.0F, 0.0F, 1.0F);
                            bufferbuilder.addVertex(f18 + 0.0F, f17 + 0.0F, f19 + (float)l2 + 1.0F - 9.765625E-4F)
                            .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4)
                            .setColor(f14, f15, f16, 0.8F)
                            .setNormal(0.0F, 0.0F, 1.0F);
                        }
                    }
                }
            }
        }
        else
        {
            int j1 = 1;
            int k1 = 32;

            for (int l1 = -32; l1 < 32; l1 += 32)
            {
                for (int i2 = -32; i2 < 32; i2 += 32)
                {
                    bufferbuilder.addVertex((float)(l1 + 0), f17, (float)(i2 + 32))
                    .setUv((float)(l1 + 0) * 0.00390625F + f3, (float)(i2 + 32) * 0.00390625F + f4)
                    .setColor(f5, f6, f7, 0.8F)
                    .setNormal(0.0F, -1.0F, 0.0F);
                    bufferbuilder.addVertex((float)(l1 + 32), f17, (float)(i2 + 32))
                    .setUv((float)(l1 + 32) * 0.00390625F + f3, (float)(i2 + 32) * 0.00390625F + f4)
                    .setColor(f5, f6, f7, 0.8F)
                    .setNormal(0.0F, -1.0F, 0.0F);
                    bufferbuilder.addVertex((float)(l1 + 32), f17, (float)(i2 + 0))
                    .setUv((float)(l1 + 32) * 0.00390625F + f3, (float)(i2 + 0) * 0.00390625F + f4)
                    .setColor(f5, f6, f7, 0.8F)
                    .setNormal(0.0F, -1.0F, 0.0F);
                    bufferbuilder.addVertex((float)(l1 + 0), f17, (float)(i2 + 0))
                    .setUv((float)(l1 + 0) * 0.00390625F + f3, (float)(i2 + 0) * 0.00390625F + f4)
                    .setColor(f5, f6, f7, 0.8F)
                    .setNormal(0.0F, -1.0F, 0.0F);
                }
            }
        }

        return bufferbuilder.buildOrThrow();
    }

    private void compileSections(Camera p_194371_)
    {
        this.minecraft.getProfiler().push("populate_sections_to_compile");
        LevelLightEngine levellightengine = this.level.getLightEngine();
        RenderRegionCache renderregioncache = new RenderRegionCache();
        renderregioncache.compileStarted();
        BlockPos blockpos = p_194371_.getBlockPosition();
        List<SectionRenderDispatcher.RenderSection> list = Lists.newArrayList();
        Lagometer.timerChunkUpdate.start();

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.visibleSections)
        {
            SectionPos sectionpos = sectionrenderdispatcher$rendersection.getSectionPosition();

            if (sectionrenderdispatcher$rendersection.isDirty() && levellightengine.lightOnInSection(sectionpos))
            {
                if (sectionrenderdispatcher$rendersection.needsBackgroundPriorityUpdate())
                {
                    list.add(sectionrenderdispatcher$rendersection);
                }
                else
                {
                    boolean flag = false;

                    if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.NEARBY)
                    {
                        BlockPos blockpos1 = sectionrenderdispatcher$rendersection.getOrigin().offset(8, 8, 8);
                        flag = blockpos1.distSqr(blockpos) < 768.0 || sectionrenderdispatcher$rendersection.isDirtyFromPlayer();
                    }
                    else if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.PLAYER_AFFECTED)
                    {
                        flag = sectionrenderdispatcher$rendersection.isDirtyFromPlayer();
                    }

                    if (flag)
                    {
                        this.minecraft.getProfiler().push("build_near_sync");
                        this.sectionRenderDispatcher.rebuildSectionSync(sectionrenderdispatcher$rendersection, renderregioncache);
                        sectionrenderdispatcher$rendersection.setNotDirty();
                        this.minecraft.getProfiler().pop();
                    }
                    else
                    {
                        list.add(sectionrenderdispatcher$rendersection);
                    }
                }
            }
        }

        Lagometer.timerChunkUpdate.end();
        Lagometer.timerChunkUpload.start();
        this.minecraft.getProfiler().popPush("upload");
        this.sectionRenderDispatcher.uploadAllPendingUploads();
        this.viewArea.clearUnusedVbos();
        this.minecraft.getProfiler().popPush("schedule_async_compile");

        if (this.chunksToResortTransparency.size() > 0)
        {
            Iterator<SectionRenderDispatcher.RenderSection> iterator = this.chunksToResortTransparency.iterator();

            if (iterator.hasNext())
            {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = iterator.next();

                if (this.sectionRenderDispatcher.updateTransparencyLater(sectionrenderdispatcher$rendersection1))
                {
                    iterator.remove();
                }
            }
        }

        double d1 = 0.0;
        int i = Config.getUpdatesPerFrame();
        this.countChunksToUpdate = list.size();

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection2 : list)
        {
            boolean flag1 = sectionrenderdispatcher$rendersection2.isChunkRegionEmpty();
            boolean flag2 = sectionrenderdispatcher$rendersection2.needsBackgroundPriorityUpdate();

            if (sectionrenderdispatcher$rendersection2.isDirty())
            {
                sectionrenderdispatcher$rendersection2.rebuildSectionAsync(this.sectionRenderDispatcher, renderregioncache);
                sectionrenderdispatcher$rendersection2.setNotDirty();

                if (!flag1 && !flag2)
                {
                    double d0 = 2.0 * RenderChunkUtils.getRelativeBufferSize(sectionrenderdispatcher$rendersection2);
                    d1 += d0;

                    if (d1 > (double)i)
                    {
                        break;
                    }
                }
            }
        }

        renderregioncache.compileFinished();
        Lagometer.timerChunkUpload.end();
        this.minecraft.getProfiler().pop();
    }

    private void renderWorldBorder(Camera p_173013_)
    {
        WorldBorder worldborder = this.level.getWorldBorder();
        double d0 = (double)(this.minecraft.options.getEffectiveRenderDistance() * 16);

        if (!(p_173013_.getPosition().x < worldborder.getMaxX() - d0)
                || !(p_173013_.getPosition().x > worldborder.getMinX() + d0)
                || !(p_173013_.getPosition().z < worldborder.getMaxZ() - d0)
                || !(p_173013_.getPosition().z > worldborder.getMinZ() + d0))
        {
            if (Config.isShaders())
            {
                Shaders.pushProgram();
                Shaders.useProgram(Shaders.ProgramTexturedLit);
                Shaders.setRenderStage(RenderStage.WORLD_BORDER);
            }

            double d1 = 1.0 - worldborder.getDistanceToBorder(p_173013_.getPosition().x, p_173013_.getPosition().z) / d0;
            d1 = Math.pow(d1, 4.0);
            d1 = Mth.clamp(d1, 0.0, 1.0);
            double d2 = p_173013_.getPosition().x;
            double d3 = p_173013_.getPosition().z;
            double d4 = (double)this.minecraft.gameRenderer.getDepthFar();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
            RenderSystem.setShaderTexture(0, FORCEFIELD_LOCATION);
            RenderSystem.depthMask(Minecraft.useShaderTransparency());
            int i = worldborder.getStatus().getColor();
            float f = (float)(i >> 16 & 0xFF) / 255.0F;
            float f1 = (float)(i >> 8 & 0xFF) / 255.0F;
            float f2 = (float)(i & 0xFF) / 255.0F;
            RenderSystem.setShaderColor(f, f1, f2, (float)d1);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.polygonOffset(-3.0F, -3.0F);
            RenderSystem.enablePolygonOffset();
            RenderSystem.disableCull();
            float f3 = (float)(Util.getMillis() % 3000L) / 3000.0F;
            float f4 = (float)(-Mth.frac(p_173013_.getPosition().y * 0.5));
            float f5 = f4 + (float)d4;
            BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            double d5 = Math.max((double)Mth.floor(d3 - d0), worldborder.getMinZ());
            double d6 = Math.min((double)Mth.ceil(d3 + d0), worldborder.getMaxZ());
            float f6 = (float)(Mth.floor(d5) & 1) * 0.5F;

            if (d2 > worldborder.getMaxX() - d0)
            {
                float f7 = f6;

                for (double d7 = d5; d7 < d6; f7 += 0.5F)
                {
                    double d8 = Math.min(1.0, d6 - d7);
                    float f8 = (float)d8 * 0.5F;
                    bufferbuilder.addVertex((float)(worldborder.getMaxX() - d2), (float)(-d4), (float)(d7 - d3)).setUv(f3 - f7, f3 + f5);
                    bufferbuilder.addVertex((float)(worldborder.getMaxX() - d2), (float)(-d4), (float)(d7 + d8 - d3)).setUv(f3 - (f8 + f7), f3 + f5);
                    bufferbuilder.addVertex((float)(worldborder.getMaxX() - d2), (float)d4, (float)(d7 + d8 - d3)).setUv(f3 - (f8 + f7), f3 + f4);
                    bufferbuilder.addVertex((float)(worldborder.getMaxX() - d2), (float)d4, (float)(d7 - d3)).setUv(f3 - f7, f3 + f4);
                    d7++;
                }
            }

            if (d2 < worldborder.getMinX() + d0)
            {
                float f9 = f6;

                for (double d9 = d5; d9 < d6; f9 += 0.5F)
                {
                    double d12 = Math.min(1.0, d6 - d9);
                    float f12 = (float)d12 * 0.5F;
                    bufferbuilder.addVertex((float)(worldborder.getMinX() - d2), (float)(-d4), (float)(d9 - d3)).setUv(f3 + f9, f3 + f5);
                    bufferbuilder.addVertex((float)(worldborder.getMinX() - d2), (float)(-d4), (float)(d9 + d12 - d3)).setUv(f3 + f12 + f9, f3 + f5);
                    bufferbuilder.addVertex((float)(worldborder.getMinX() - d2), (float)d4, (float)(d9 + d12 - d3)).setUv(f3 + f12 + f9, f3 + f4);
                    bufferbuilder.addVertex((float)(worldborder.getMinX() - d2), (float)d4, (float)(d9 - d3)).setUv(f3 + f9, f3 + f4);
                    d9++;
                }
            }

            d5 = Math.max((double)Mth.floor(d2 - d0), worldborder.getMinX());
            d6 = Math.min((double)Mth.ceil(d2 + d0), worldborder.getMaxX());
            f6 = (float)(Mth.floor(d5) & 1) * 0.5F;

            if (d3 > worldborder.getMaxZ() - d0)
            {
                float f10 = f6;

                for (double d10 = d5; d10 < d6; f10 += 0.5F)
                {
                    double d13 = Math.min(1.0, d6 - d10);
                    float f13 = (float)d13 * 0.5F;
                    bufferbuilder.addVertex((float)(d10 - d2), (float)(-d4), (float)(worldborder.getMaxZ() - d3)).setUv(f3 + f10, f3 + f5);
                    bufferbuilder.addVertex((float)(d10 + d13 - d2), (float)(-d4), (float)(worldborder.getMaxZ() - d3)).setUv(f3 + f13 + f10, f3 + f5);
                    bufferbuilder.addVertex((float)(d10 + d13 - d2), (float)d4, (float)(worldborder.getMaxZ() - d3)).setUv(f3 + f13 + f10, f3 + f4);
                    bufferbuilder.addVertex((float)(d10 - d2), (float)d4, (float)(worldborder.getMaxZ() - d3)).setUv(f3 + f10, f3 + f4);
                    d10++;
                }
            }

            if (d3 < worldborder.getMinZ() + d0)
            {
                float f11 = f6;

                for (double d11 = d5; d11 < d6; f11 += 0.5F)
                {
                    double d14 = Math.min(1.0, d6 - d11);
                    float f14 = (float)d14 * 0.5F;
                    bufferbuilder.addVertex((float)(d11 - d2), (float)(-d4), (float)(worldborder.getMinZ() - d3)).setUv(f3 - f11, f3 + f5);
                    bufferbuilder.addVertex((float)(d11 + d14 - d2), (float)(-d4), (float)(worldborder.getMinZ() - d3)).setUv(f3 - (f14 + f11), f3 + f5);
                    bufferbuilder.addVertex((float)(d11 + d14 - d2), (float)d4, (float)(worldborder.getMinZ() - d3)).setUv(f3 - (f14 + f11), f3 + f4);
                    bufferbuilder.addVertex((float)(d11 - d2), (float)d4, (float)(worldborder.getMinZ() - d3)).setUv(f3 - f11, f3 + f4);
                    d11++;
                }
            }

            MeshData meshdata = bufferbuilder.build();

            if (meshdata != null)
            {
                BufferUploader.drawWithShader(meshdata);
            }

            RenderSystem.enableCull();
            RenderSystem.polygonOffset(0.0F, 0.0F);
            RenderSystem.disablePolygonOffset();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
            );
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);

            if (Config.isShaders())
            {
                Shaders.popProgram();
                Shaders.setRenderStage(RenderStage.NONE);
            }
        }
    }

    private void renderHitOutline(
        PoseStack p_109638_,
        VertexConsumer p_109639_,
        Entity p_109640_,
        double p_109641_,
        double p_109642_,
        double p_109643_,
        BlockPos p_109644_,
        BlockState p_109645_
    )
    {
        if (!Config.isCustomEntityModels() || !CustomEntityModels.isCustomModel(p_109645_))
        {
            renderShape(
                p_109638_,
                p_109639_,
                p_109645_.getShape(this.level, p_109644_, CollisionContext.of(p_109640_)),
                (double)p_109644_.getX() - p_109641_,
                (double)p_109644_.getY() - p_109642_,
                (double)p_109644_.getZ() - p_109643_,
                0.0F,
                0.0F,
                0.0F,
                0.4F
            );
        }
    }

    private static Vec3 mixColor(float p_286899_)
    {
        float f = 5.99999F;
        int i = (int)(Mth.clamp(p_286899_, 0.0F, 1.0F) * 5.99999F);
        float f1 = p_286899_ * 5.99999F - (float)i;

        return switch (i)
        {
            case 0 -> new Vec3(1.0, (double)f1, 0.0);

            case 1 -> new Vec3((double)(1.0F - f1), 1.0, 0.0);

            case 2 -> new Vec3(0.0, 1.0, (double)f1);

            case 3 -> new Vec3(0.0, 1.0 - (double)f1, 1.0);

            case 4 -> new Vec3((double)f1, 0.0, 1.0);

            case 5 -> new Vec3(1.0, 0.0, 1.0 - (double)f1);

            default -> throw new IllegalStateException("Unexpected value: " + i);
        };
    }

    private static Vec3 shiftHue(float p_286907_, float p_286536_, float p_286318_, float p_286590_)
    {
        Vec3 vec3 = mixColor(p_286590_).scale((double)p_286907_);
        Vec3 vec31 = mixColor((p_286590_ + 0.33333334F) % 1.0F).scale((double)p_286536_);
        Vec3 vec32 = mixColor((p_286590_ + 0.6666667F) % 1.0F).scale((double)p_286318_);
        Vec3 vec33 = vec3.add(vec31).add(vec32);
        double d0 = Math.max(Math.max(1.0, vec33.x), Math.max(vec33.y, vec33.z));
        return new Vec3(vec33.x / d0, vec33.y / d0, vec33.z / d0);
    }

    public static void renderVoxelShape(
        PoseStack p_286791_,
        VertexConsumer p_286416_,
        VoxelShape p_286863_,
        double p_286432_,
        double p_286836_,
        double p_286774_,
        float p_286612_,
        float p_286516_,
        float p_286787_,
        float p_286300_,
        boolean p_286443_
    )
    {
        List<AABB> list = p_286863_.toAabbs();

        if (!list.isEmpty())
        {
            int i = p_286443_ ? list.size() : list.size() * 8;
            renderShape(p_286791_, p_286416_, Shapes.create(list.get(0)), p_286432_, p_286836_, p_286774_, p_286612_, p_286516_, p_286787_, p_286300_);

            for (int j = 1; j < list.size(); j++)
            {
                AABB aabb = list.get(j);
                float f = (float)j / (float)i;
                Vec3 vec3 = shiftHue(p_286612_, p_286516_, p_286787_, f);
                renderShape(
                    p_286791_,
                    p_286416_,
                    Shapes.create(aabb),
                    p_286432_,
                    p_286836_,
                    p_286774_,
                    (float)vec3.x,
                    (float)vec3.y,
                    (float)vec3.z,
                    p_286300_
                );
            }
        }
    }

    private static void renderShape(
        PoseStack p_109783_,
        VertexConsumer p_109784_,
        VoxelShape p_109785_,
        double p_109786_,
        double p_109787_,
        double p_109788_,
        float p_109789_,
        float p_109790_,
        float p_109791_,
        float p_109792_
    )
    {
        PoseStack.Pose posestack$pose = p_109783_.last();
        p_109785_.forAllEdges(
            (x0, y0, z0, x1, y1, z1) ->
        {
            float f = (float)(x1 - x0);
            float f1 = (float)(y1 - y0);
            float f2 = (float)(z1 - z0);
            float f3 = Mth.sqrt(f * f + f1 * f1 + f2 * f2);
            f /= f3;
            f1 /= f3;
            f2 /= f3;
            p_109784_.addVertex(posestack$pose, (float)(x0 + p_109786_), (float)(y0 + p_109787_), (float)(z0 + p_109788_))
            .setColor(p_109789_, p_109790_, p_109791_, p_109792_)
            .setNormal(posestack$pose, f, f1, f2);
            p_109784_.addVertex(posestack$pose, (float)(x1 + p_109786_), (float)(y1 + p_109787_), (float)(z1 + p_109788_))
            .setColor(p_109789_, p_109790_, p_109791_, p_109792_)
            .setNormal(posestack$pose, f, f1, f2);
        }
        );
    }

    public static void renderLineBox(
        VertexConsumer p_172966_,
        double p_172967_,
        double p_172968_,
        double p_172969_,
        double p_172970_,
        double p_172971_,
        double p_172972_,
        float p_172973_,
        float p_172974_,
        float p_172975_,
        float p_172976_
    )
    {
        renderLineBox(
            new PoseStack(),
            p_172966_,
            p_172967_,
            p_172968_,
            p_172969_,
            p_172970_,
            p_172971_,
            p_172972_,
            p_172973_,
            p_172974_,
            p_172975_,
            p_172976_,
            p_172973_,
            p_172974_,
            p_172975_
        );
    }

    public static void renderLineBox(
        PoseStack p_109647_, VertexConsumer p_109648_, AABB p_109649_, float p_109650_, float p_109651_, float p_109652_, float p_109653_
    )
    {
        renderLineBox(
            p_109647_,
            p_109648_,
            p_109649_.minX,
            p_109649_.minY,
            p_109649_.minZ,
            p_109649_.maxX,
            p_109649_.maxY,
            p_109649_.maxZ,
            p_109650_,
            p_109651_,
            p_109652_,
            p_109653_,
            p_109650_,
            p_109651_,
            p_109652_
        );
    }

    public static void renderLineBox(
        PoseStack p_109609_,
        VertexConsumer p_109610_,
        double p_109611_,
        double p_109612_,
        double p_109613_,
        double p_109614_,
        double p_109615_,
        double p_109616_,
        float p_109617_,
        float p_109618_,
        float p_109619_,
        float p_109620_
    )
    {
        renderLineBox(
            p_109609_,
            p_109610_,
            p_109611_,
            p_109612_,
            p_109613_,
            p_109614_,
            p_109615_,
            p_109616_,
            p_109617_,
            p_109618_,
            p_109619_,
            p_109620_,
            p_109617_,
            p_109618_,
            p_109619_
        );
    }

    public static void renderLineBox(
        PoseStack p_109622_,
        VertexConsumer p_109623_,
        double p_109624_,
        double p_109625_,
        double p_109626_,
        double p_109627_,
        double p_109628_,
        double p_109629_,
        float p_109630_,
        float p_109631_,
        float p_109632_,
        float p_109633_,
        float p_109634_,
        float p_109635_,
        float p_109636_
    )
    {
        PoseStack.Pose posestack$pose = p_109622_.last();
        float f = (float)p_109624_;
        float f1 = (float)p_109625_;
        float f2 = (float)p_109626_;
        float f3 = (float)p_109627_;
        float f4 = (float)p_109628_;
        float f5 = (float)p_109629_;
        p_109623_.addVertex(posestack$pose, f, f1, f2).setColor(p_109630_, p_109635_, p_109636_, p_109633_).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f3, f1, f2).setColor(p_109630_, p_109635_, p_109636_, p_109633_).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f, f1, f2).setColor(p_109634_, p_109631_, p_109636_, p_109633_).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f, f4, f2).setColor(p_109634_, p_109631_, p_109636_, p_109633_).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f, f1, f2).setColor(p_109634_, p_109635_, p_109632_, p_109633_).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        p_109623_.addVertex(posestack$pose, f, f1, f5).setColor(p_109634_, p_109635_, p_109632_, p_109633_).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        p_109623_.addVertex(posestack$pose, f3, f1, f2).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f3, f4, f2).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f3, f4, f2).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, -1.0F, 0.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f, f4, f2).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, -1.0F, 0.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f, f4, f2).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        p_109623_.addVertex(posestack$pose, f, f4, f5).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        p_109623_.addVertex(posestack$pose, f, f4, f5).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 0.0F, -1.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f, f1, f5).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 0.0F, -1.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f, f1, f5).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f3, f1, f5).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f3, f1, f5).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 0.0F, 0.0F, -1.0F);
        p_109623_.addVertex(posestack$pose, f3, f1, f2).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 0.0F, 0.0F, -1.0F);
        p_109623_.addVertex(posestack$pose, f, f4, f5).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f3, f4, f5).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f3, f1, f5).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f3, f4, f5).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        p_109623_.addVertex(posestack$pose, f3, f4, f2).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        p_109623_.addVertex(posestack$pose, f3, f4, f5).setColor(p_109630_, p_109631_, p_109632_, p_109633_).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
    }

    public static void addChainedFilledBoxVertices(
        PoseStack p_270343_,
        VertexConsumer p_270926_,
        double p_270503_,
        double p_270353_,
        double p_270642_,
        double p_270676_,
        double p_270711_,
        double p_270601_,
        float p_270523_,
        float p_270382_,
        float p_270089_,
        float p_270335_
    )
    {
        addChainedFilledBoxVertices(
            p_270343_,
            p_270926_,
            (float)p_270503_,
            (float)p_270353_,
            (float)p_270642_,
            (float)p_270676_,
            (float)p_270711_,
            (float)p_270601_,
            p_270523_,
            p_270382_,
            p_270089_,
            p_270335_
        );
    }

    public static void addChainedFilledBoxVertices(
        PoseStack p_270352_,
        VertexConsumer p_271015_,
        float p_270144_,
        float p_270901_,
        float p_270546_,
        float p_270102_,
        float p_270605_,
        float p_271006_,
        float p_270864_,
        float p_270181_,
        float p_270220_,
        float p_270293_
    )
    {
        Matrix4f matrix4f = p_270352_.last().pose();
        p_271015_.addVertex(matrix4f, p_270144_, p_270901_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270144_, p_270901_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270144_, p_270901_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270144_, p_270901_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270144_, p_270605_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270144_, p_270605_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270144_, p_270605_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270144_, p_270901_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270605_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270901_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270901_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270901_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270605_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270605_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270605_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270901_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270144_, p_270605_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270144_, p_270901_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270144_, p_270901_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270901_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270144_, p_270901_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270901_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270901_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270144_, p_270605_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270144_, p_270605_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270144_, p_270605_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270605_, p_270546_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270605_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270605_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
        p_271015_.addVertex(matrix4f, p_270102_, p_270605_, p_271006_).setColor(p_270864_, p_270181_, p_270220_, p_270293_);
    }

    public static void renderFace(
        PoseStack p_343129_,
        VertexConsumer p_342366_,
        Direction p_345284_,
        float p_342273_,
        float p_344240_,
        float p_344233_,
        float p_344077_,
        float p_344098_,
        float p_343162_,
        float p_344811_,
        float p_344617_,
        float p_345180_,
        float p_342832_
    )
    {
        Matrix4f matrix4f = p_343129_.last().pose();

        switch (p_345284_)
        {
            case DOWN:
                p_342366_.addVertex(matrix4f, p_342273_, p_344240_, p_344233_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_344077_, p_344240_, p_344233_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_344077_, p_344240_, p_343162_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_342273_, p_344240_, p_343162_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                break;

            case UP:
                p_342366_.addVertex(matrix4f, p_342273_, p_344098_, p_344233_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_342273_, p_344098_, p_343162_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_344077_, p_344098_, p_343162_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_344077_, p_344098_, p_344233_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                break;

            case NORTH:
                p_342366_.addVertex(matrix4f, p_342273_, p_344240_, p_344233_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_342273_, p_344098_, p_344233_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_344077_, p_344098_, p_344233_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_344077_, p_344240_, p_344233_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                break;

            case SOUTH:
                p_342366_.addVertex(matrix4f, p_342273_, p_344240_, p_343162_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_344077_, p_344240_, p_343162_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_344077_, p_344098_, p_343162_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_342273_, p_344098_, p_343162_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                break;

            case WEST:
                p_342366_.addVertex(matrix4f, p_342273_, p_344240_, p_344233_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_342273_, p_344240_, p_343162_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_342273_, p_344098_, p_343162_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_342273_, p_344098_, p_344233_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                break;

            case EAST:
                p_342366_.addVertex(matrix4f, p_344077_, p_344240_, p_344233_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_344077_, p_344098_, p_344233_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_344077_, p_344098_, p_343162_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
                p_342366_.addVertex(matrix4f, p_344077_, p_344240_, p_343162_).setColor(p_344811_, p_344617_, p_345180_, p_342832_);
        }
    }

    public void blockChanged(BlockGetter p_109545_, BlockPos p_109546_, BlockState p_109547_, BlockState p_109548_, int p_109549_)
    {
        this.setBlockDirty(p_109546_, (p_109549_ & 8) != 0);
    }

    private void setBlockDirty(BlockPos p_109733_, boolean p_109734_)
    {
        for (int i = p_109733_.getZ() - 1; i <= p_109733_.getZ() + 1; i++)
        {
            for (int j = p_109733_.getX() - 1; j <= p_109733_.getX() + 1; j++)
            {
                for (int k = p_109733_.getY() - 1; k <= p_109733_.getY() + 1; k++)
                {
                    this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i), p_109734_);
                }
            }
        }
    }

    public void setBlocksDirty(int p_109495_, int p_109496_, int p_109497_, int p_109498_, int p_109499_, int p_109500_)
    {
        for (int i = p_109497_ - 1; i <= p_109500_ + 1; i++)
        {
            for (int j = p_109495_ - 1; j <= p_109498_ + 1; j++)
            {
                for (int k = p_109496_ - 1; k <= p_109499_ + 1; k++)
                {
                    this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i));
                }
            }
        }
    }

    public void setBlockDirty(BlockPos p_109722_, BlockState p_109723_, BlockState p_109724_)
    {
        if (this.minecraft.getModelManager().requiresRender(p_109723_, p_109724_))
        {
            this.setBlocksDirty(
                p_109722_.getX(), p_109722_.getY(), p_109722_.getZ(), p_109722_.getX(), p_109722_.getY(), p_109722_.getZ()
            );
        }
    }

    public void setSectionDirtyWithNeighbors(int p_109491_, int p_109492_, int p_109493_)
    {
        for (int i = p_109493_ - 1; i <= p_109493_ + 1; i++)
        {
            for (int j = p_109491_ - 1; j <= p_109491_ + 1; j++)
            {
                for (int k = p_109492_ - 1; k <= p_109492_ + 1; k++)
                {
                    this.setSectionDirty(j, k, i);
                }
            }
        }
    }

    public void setSectionDirty(int p_109771_, int p_109772_, int p_109773_)
    {
        this.setSectionDirty(p_109771_, p_109772_, p_109773_, false);
    }

    private void setSectionDirty(int p_109502_, int p_109503_, int p_109504_, boolean p_109505_)
    {
        this.viewArea.setDirty(p_109502_, p_109503_, p_109504_, p_109505_);
    }

    public void playJukeboxSong(Holder<JukeboxSong> p_343944_, BlockPos p_342259_)
    {
        if (this.level != null)
        {
            this.stopJukeboxSong(p_342259_);
            JukeboxSong jukeboxsong = p_343944_.value();
            SoundEvent soundevent = jukeboxsong.soundEvent().value();
            SoundInstance soundinstance = SimpleSoundInstance.forJukeboxSong(soundevent, Vec3.atCenterOf(p_342259_));
            this.playingJukeboxSongs.put(p_342259_, soundinstance);
            this.minecraft.getSoundManager().play(soundinstance);
            this.minecraft.gui.setNowPlaying(jukeboxsong.description());
            this.notifyNearbyEntities(this.level, p_342259_, true);
        }
    }

    private void stopJukeboxSong(BlockPos p_343323_)
    {
        SoundInstance soundinstance = this.playingJukeboxSongs.remove(p_343323_);

        if (soundinstance != null)
        {
            this.minecraft.getSoundManager().stop(soundinstance);
        }
    }

    public void stopJukeboxSongAndNotifyNearby(BlockPos p_343954_)
    {
        this.stopJukeboxSong(p_343954_);

        if (this.level != null)
        {
            this.notifyNearbyEntities(this.level, p_343954_, false);
        }
    }

    private void notifyNearbyEntities(Level p_109551_, BlockPos p_109552_, boolean p_109553_)
    {
        for (LivingEntity livingentity : p_109551_.getEntitiesOfClass(LivingEntity.class, new AABB(p_109552_).inflate(3.0)))
        {
            livingentity.setRecordPlayingNearby(p_109552_, p_109553_);
        }
    }

    public void addParticle(
        ParticleOptions p_109744_,
        boolean p_109745_,
        double p_109746_,
        double p_109747_,
        double p_109748_,
        double p_109749_,
        double p_109750_,
        double p_109751_
    )
    {
        this.addParticle(p_109744_, p_109745_, false, p_109746_, p_109747_, p_109748_, p_109749_, p_109750_, p_109751_);
    }

    public void addParticle(
        ParticleOptions p_109753_,
        boolean p_109754_,
        boolean p_109755_,
        double p_109756_,
        double p_109757_,
        double p_109758_,
        double p_109759_,
        double p_109760_,
        double p_109761_
    )
    {
        try
        {
            this.addParticleInternal(p_109753_, p_109754_, p_109755_, p_109756_, p_109757_, p_109758_, p_109759_, p_109760_, p_109761_);
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception while adding particle");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being added");
            crashreportcategory.setDetail("ID", BuiltInRegistries.PARTICLE_TYPE.getKey(p_109753_.getType()));
            crashreportcategory.setDetail(
                "Parameters", () -> ParticleTypes.CODEC.encodeStart(this.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), p_109753_).toString()
            );
            crashreportcategory.setDetail("Position", () -> CrashReportCategory.formatLocation(this.level, p_109756_, p_109757_, p_109758_));
            throw new ReportedException(crashreport);
        }
    }

    private <T extends ParticleOptions> void addParticle(
        T p_109736_, double p_109737_, double p_109738_, double p_109739_, double p_109740_, double p_109741_, double p_109742_
    )
    {
        this.addParticle(p_109736_, p_109736_.getType().getOverrideLimiter(), p_109737_, p_109738_, p_109739_, p_109740_, p_109741_, p_109742_);
    }

    @Nullable
    private Particle addParticleInternal(
        ParticleOptions p_109796_,
        boolean p_109797_,
        double p_109798_,
        double p_109799_,
        double p_109800_,
        double p_109801_,
        double p_109802_,
        double p_109803_
    )
    {
        return this.addParticleInternal(p_109796_, p_109797_, false, p_109798_, p_109799_, p_109800_, p_109801_, p_109802_, p_109803_);
    }

    @Nullable
    private Particle addParticleInternal(
        ParticleOptions p_109805_,
        boolean p_109806_,
        boolean p_109807_,
        double p_109808_,
        double p_109809_,
        double p_109810_,
        double p_109811_,
        double p_109812_,
        double p_109813_
    )
    {
        Camera camera = this.minecraft.gameRenderer.getMainCamera();
        ParticleStatus particlestatus = this.calculateParticleLevel(p_109807_);

        if (p_109805_ == ParticleTypes.EXPLOSION_EMITTER && !Config.isAnimatedExplosion())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.EXPLOSION && !Config.isAnimatedExplosion())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.POOF && !Config.isAnimatedExplosion())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.UNDERWATER && !Config.isWaterParticles())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.SMOKE && !Config.isAnimatedSmoke())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.LARGE_SMOKE && !Config.isAnimatedSmoke())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.ENTITY_EFFECT && !Config.isPotionParticles())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.EFFECT && !Config.isPotionParticles())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.INSTANT_EFFECT && !Config.isPotionParticles())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.WITCH && !Config.isPotionParticles())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.PORTAL && !Config.isPortalParticles())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.FLAME && !Config.isAnimatedFlame())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.SOUL_FIRE_FLAME && !Config.isAnimatedFlame())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.DUST && !Config.isAnimatedRedstone())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.DRIPPING_WATER && !Config.isDrippingWaterLava())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.DRIPPING_LAVA && !Config.isDrippingWaterLava())
        {
            return null;
        }
        else if (p_109805_ == ParticleTypes.FIREWORK && !Config.isFireworkParticles())
        {
            return null;
        }
        else
        {
            if (!p_109806_)
            {
                double d0 = 1024.0;

                if (p_109805_ == ParticleTypes.CRIT)
                {
                    d0 = 38416.0;
                }

                if (camera.getPosition().distanceToSqr(p_109808_, p_109809_, p_109810_) > d0)
                {
                    return null;
                }

                if (particlestatus == ParticleStatus.MINIMAL)
                {
                    return null;
                }
            }

            Particle particle = this.minecraft.particleEngine.createParticle(p_109805_, p_109808_, p_109809_, p_109810_, p_109811_, p_109812_, p_109813_);

            if (p_109805_ == ParticleTypes.BUBBLE)
            {
                CustomColors.updateWaterFX(particle, this.level, p_109808_, p_109809_, p_109810_, this.renderEnv);
            }

            if (p_109805_ == ParticleTypes.SPLASH)
            {
                CustomColors.updateWaterFX(particle, this.level, p_109808_, p_109809_, p_109810_, this.renderEnv);
            }

            if (p_109805_ == ParticleTypes.RAIN)
            {
                CustomColors.updateWaterFX(particle, this.level, p_109808_, p_109809_, p_109810_, this.renderEnv);
            }

            if (p_109805_ == ParticleTypes.MYCELIUM)
            {
                CustomColors.updateMyceliumFX(particle);
            }

            if (p_109805_ == ParticleTypes.PORTAL)
            {
                CustomColors.updatePortalFX(particle);
            }

            if (p_109805_ == ParticleTypes.DUST)
            {
                CustomColors.updateReddustFX(particle, this.level, p_109808_, p_109809_, p_109810_);
            }

            if (p_109805_ == ParticleTypes.LAVA)
            {
                CustomColors.updateLavaFX(particle);
            }

            return particle;
        }
    }

    private ParticleStatus calculateParticleLevel(boolean p_109768_)
    {
        ParticleStatus particlestatus = this.minecraft.options.particles().get();

        if (p_109768_ && particlestatus == ParticleStatus.MINIMAL && this.level.random.nextInt(10) == 0)
        {
            particlestatus = ParticleStatus.DECREASED;
        }

        if (particlestatus == ParticleStatus.DECREASED && this.level.random.nextInt(3) == 0)
        {
            particlestatus = ParticleStatus.MINIMAL;
        }

        return particlestatus;
    }

    public void clear()
    {
    }

    public void globalLevelEvent(int p_109507_, BlockPos p_109508_, int p_109509_)
    {
        switch (p_109507_)
        {
            case 1023:
            case 1028:
            case 1038:
                Camera camera = this.minecraft.gameRenderer.getMainCamera();

                if (camera.isInitialized())
                {
                    double d0 = (double)p_109508_.getX() - camera.getPosition().x;
                    double d1 = (double)p_109508_.getY() - camera.getPosition().y;
                    double d2 = (double)p_109508_.getZ() - camera.getPosition().z;
                    double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                    double d4 = camera.getPosition().x;
                    double d5 = camera.getPosition().y;
                    double d6 = camera.getPosition().z;

                    if (d3 > 0.0)
                    {
                        d4 += d0 / d3 * 2.0;
                        d5 += d1 / d3 * 2.0;
                        d6 += d2 / d3 * 2.0;
                    }

                    if (p_109507_ == 1023)
                    {
                        this.level.playLocalSound(d4, d5, d6, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
                    }
                    else if (p_109507_ == 1038)
                    {
                        this.level.playLocalSound(d4, d5, d6, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
                    }
                    else
                    {
                        this.level.playLocalSound(d4, d5, d6, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 5.0F, 1.0F, false);
                    }
                }
        }
    }

    public void levelEvent(int p_234305_, BlockPos p_234306_, int p_234307_)
    {
        RandomSource randomsource = this.level.random;

        switch (p_234305_)
        {
            case 1000:
                this.level.playLocalSound(p_234306_, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;

            case 1001:
                this.level.playLocalSound(p_234306_, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.2F, false);
                break;

            case 1002:
                this.level.playLocalSound(p_234306_, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.2F, false);
                break;

            case 1004:
                this.level.playLocalSound(p_234306_, SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
                break;

            case 1009:
                if (p_234307_ == 0)
                {
                    this.level
                    .playLocalSound(
                        p_234306_,
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS,
                        0.5F,
                        2.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.8F,
                        false
                    );
                }
                else if (p_234307_ == 1)
                {
                    this.level
                    .playLocalSound(
                        p_234306_,
                        SoundEvents.GENERIC_EXTINGUISH_FIRE,
                        SoundSource.BLOCKS,
                        0.7F,
                        1.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.4F,
                        false
                    );
                }

                break;

            case 1010:
                this.level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG).getHolder(p_234307_).ifPresent(songIn -> this.playJukeboxSong(songIn, p_234306_));
                break;

            case 1011:
                this.stopJukeboxSongAndNotifyNearby(p_234306_);
                break;

            case 1015:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 10.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1016:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 10.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1017:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 10.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1018:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1019:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1020:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1021:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1022:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1024:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1025:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 0.05F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1026:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1027:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1029:
                this.level.playLocalSound(p_234306_, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1030:
                this.level.playLocalSound(p_234306_, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1031:
                this.level.playLocalSound(p_234306_, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1032:
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRAVEL, randomsource.nextFloat() * 0.4F + 0.8F, 0.25F));
                break;

            case 1033:
                this.level.playLocalSound(p_234306_, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;

            case 1034:
                this.level.playLocalSound(p_234306_, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;

            case 1035:
                this.level.playLocalSound(p_234306_, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;

            case 1039:
                this.level.playLocalSound(p_234306_, SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1040:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1041:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.HUSK_CONVERTED_TO_ZOMBIE, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1042:
                this.level.playLocalSound(p_234306_, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1043:
                this.level.playLocalSound(p_234306_, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1044:
                this.level.playLocalSound(p_234306_, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1045:
                this.level.playLocalSound(p_234306_, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1046:
                this.level.playLocalSound(p_234306_, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1047:
                this.level.playLocalSound(p_234306_, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1048:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.SKELETON_CONVERTED_TO_STRAY, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                );
                break;

            case 1049:
                this.level.playLocalSound(p_234306_, SoundEvents.CRAFTER_CRAFT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;

            case 1050:
                this.level.playLocalSound(p_234306_, SoundEvents.CRAFTER_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;

            case 1051:
                this.level
                .playLocalSound(p_234306_, SoundEvents.WIND_CHARGE_THROW, SoundSource.BLOCKS, 0.5F, 0.4F / (this.level.getRandom().nextFloat() * 0.4F + 0.8F), false);

            case 2010:
                this.shootParticles(p_234307_, p_234306_, randomsource, ParticleTypes.WHITE_SMOKE);
                break;

            case 1500:
                ComposterBlock.handleFill(this.level, p_234306_, p_234307_ > 0);
                break;

            case 1501:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.8F, false
                );

                for (int k1 = 0; k1 < 8; k1++)
                {
                    this.level
                    .addParticle(
                        ParticleTypes.LARGE_SMOKE,
                        (double)p_234306_.getX() + randomsource.nextDouble(),
                        (double)p_234306_.getY() + 1.2,
                        (double)p_234306_.getZ() + randomsource.nextDouble(),
                        0.0,
                        0.0,
                        0.0
                    );
                }

                break;

            case 1502:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.5F, 2.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.8F, false
                );

                for (int j1 = 0; j1 < 5; j1++)
                {
                    double d11 = (double)p_234306_.getX() + randomsource.nextDouble() * 0.6 + 0.2;
                    double d13 = (double)p_234306_.getY() + randomsource.nextDouble() * 0.6 + 0.2;
                    double d15 = (double)p_234306_.getZ() + randomsource.nextDouble() * 0.6 + 0.2;
                    this.level.addParticle(ParticleTypes.SMOKE, d11, d13, d15, 0.0, 0.0, 0.0);
                }

                break;

            case 1503:
                this.level.playLocalSound(p_234306_, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);

                for (int i1 = 0; i1 < 16; i1++)
                {
                    double d10 = (double)p_234306_.getX() + (5.0 + randomsource.nextDouble() * 6.0) / 16.0;
                    double d12 = (double)p_234306_.getY() + 0.8125;
                    double d14 = (double)p_234306_.getZ() + (5.0 + randomsource.nextDouble() * 6.0) / 16.0;
                    this.level.addParticle(ParticleTypes.SMOKE, d10, d12, d14, 0.0, 0.0, 0.0);
                }

                break;

            case 1504:
                PointedDripstoneBlock.spawnDripParticle(this.level, p_234306_, this.level.getBlockState(p_234306_));
                break;

            case 1505:
                BoneMealItem.addGrowthParticles(this.level, p_234306_, p_234307_);
                this.level.playLocalSound(p_234306_, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;

            case 2000:
                this.shootParticles(p_234307_, p_234306_, randomsource, ParticleTypes.SMOKE);
                break;

            case 2001:
                BlockState blockstate = Block.stateById(p_234307_);

                if (!blockstate.isAir())
                {
                    SoundType soundtype = blockstate.getSoundType();

                    if (Reflector.IForgeBlockState_getSoundType3.exists())
                    {
                        soundtype = (SoundType)Reflector.call(blockstate, Reflector.IForgeBlockState_getSoundType3, this.level, p_234306_, null);
                    }

                    this.level
                    .playLocalSound(
                        p_234306_, soundtype.getBreakSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F, false
                    );
                }

                this.level.addDestroyBlockEffect(p_234306_, blockstate);
                break;

            case 2002:
            case 2007:
                Vec3 vec3 = Vec3.atBottomCenterOf(p_234306_);

                for (int i = 0; i < 8; i++)
                {
                    this.addParticle(
                        new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SPLASH_POTION)),
                        vec3.x,
                        vec3.y,
                        vec3.z,
                        randomsource.nextGaussian() * 0.15,
                        randomsource.nextDouble() * 0.2,
                        randomsource.nextGaussian() * 0.15
                    );
                }

                float f2 = (float)(p_234307_ >> 16 & 0xFF) / 255.0F;
                float f = (float)(p_234307_ >> 8 & 0xFF) / 255.0F;
                float f1 = (float)(p_234307_ >> 0 & 0xFF) / 255.0F;
                ParticleOptions particleoptions = p_234305_ == 2007 ? ParticleTypes.INSTANT_EFFECT : ParticleTypes.EFFECT;

                for (int l1 = 0; l1 < 100; l1++)
                {
                    double d1 = randomsource.nextDouble() * 4.0;
                    double d3 = randomsource.nextDouble() * Math.PI * 2.0;
                    double d5 = Math.cos(d3) * d1;
                    double d18 = 0.01 + randomsource.nextDouble() * 0.5;
                    double d21 = Math.sin(d3) * d1;
                    Particle particle = this.addParticleInternal(
                                            particleoptions,
                                            particleoptions.getType().getOverrideLimiter(),
                                            vec3.x + d5 * 0.1,
                                            vec3.y + 0.3,
                                            vec3.z + d21 * 0.1,
                                            d5,
                                            d18,
                                            d21
                                        );

                    if (particle != null)
                    {
                        float f11 = 0.75F + randomsource.nextFloat() * 0.25F;
                        particle.setColor(f2 * f11, f * f11, f1 * f11);
                        particle.setPower((float)d1);
                    }
                }

                this.level.playLocalSound(p_234306_, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 2003:
                double d0 = (double)p_234306_.getX() + 0.5;
                double d2 = (double)p_234306_.getY();
                double d4 = (double)p_234306_.getZ() + 0.5;

                for (int l2 = 0; l2 < 8; l2++)
                {
                    this.addParticle(
                        new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ENDER_EYE)),
                        d0,
                        d2,
                        d4,
                        randomsource.nextGaussian() * 0.15,
                        randomsource.nextDouble() * 0.2,
                        randomsource.nextGaussian() * 0.15
                    );
                }

                for (double d16 = 0.0; d16 < Math.PI * 2; d16 += Math.PI / 20)
                {
                    this.addParticle(
                        ParticleTypes.PORTAL, d0 + Math.cos(d16) * 5.0, d2 - 0.4, d4 + Math.sin(d16) * 5.0, Math.cos(d16) * -5.0, 0.0, Math.sin(d16) * -5.0
                    );
                    this.addParticle(
                        ParticleTypes.PORTAL, d0 + Math.cos(d16) * 5.0, d2 - 0.4, d4 + Math.sin(d16) * 5.0, Math.cos(d16) * -7.0, 0.0, Math.sin(d16) * -7.0
                    );
                }

                break;

            case 2004:
                for (int k2 = 0; k2 < 20; k2++)
                {
                    double d17 = (double)p_234306_.getX() + 0.5 + (randomsource.nextDouble() - 0.5) * 2.0;
                    double d20 = (double)p_234306_.getY() + 0.5 + (randomsource.nextDouble() - 0.5) * 2.0;
                    double d23 = (double)p_234306_.getZ() + 0.5 + (randomsource.nextDouble() - 0.5) * 2.0;
                    this.level.addParticle(ParticleTypes.SMOKE, d17, d20, d23, 0.0, 0.0, 0.0);
                    this.level.addParticle(ParticleTypes.FLAME, d17, d20, d23, 0.0, 0.0, 0.0);
                }

                break;

            case 2006:
                for (int j2 = 0; j2 < 200; j2++)
                {
                    float f4 = randomsource.nextFloat() * 4.0F;
                    float f6 = randomsource.nextFloat() * (float)(Math.PI * 2);
                    double d19 = (double)(Mth.cos(f6) * f4);
                    double d22 = 0.01 + randomsource.nextDouble() * 0.5;
                    double d24 = (double)(Mth.sin(f6) * f4);
                    Particle particle1 = this.addParticleInternal(
                                             ParticleTypes.DRAGON_BREATH,
                                             false,
                                             (double)p_234306_.getX() + d19 * 0.1,
                                             (double)p_234306_.getY() + 0.3,
                                             (double)p_234306_.getZ() + d24 * 0.1,
                                             d19,
                                             d22,
                                             d24
                                         );

                    if (particle1 != null)
                    {
                        particle1.setPower(f4);
                    }
                }

                if (p_234307_ == 1)
                {
                    this.level.playLocalSound(p_234306_, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
                }

                break;

            case 2008:
                this.level
                .addParticle(
                    ParticleTypes.EXPLOSION,
                    (double)p_234306_.getX() + 0.5,
                    (double)p_234306_.getY() + 0.5,
                    (double)p_234306_.getZ() + 0.5,
                    0.0,
                    0.0,
                    0.0
                );
                break;

            case 2009:
                for (int i2 = 0; i2 < 8; i2++)
                {
                    this.level
                    .addParticle(
                        ParticleTypes.CLOUD,
                        (double)p_234306_.getX() + randomsource.nextDouble(),
                        (double)p_234306_.getY() + 1.2,
                        (double)p_234306_.getZ() + randomsource.nextDouble(),
                        0.0,
                        0.0,
                        0.0
                    );
                }

                break;

            case 2011:
                ParticleUtils.spawnParticleInBlock(this.level, p_234306_, p_234307_, ParticleTypes.HAPPY_VILLAGER);
                break;

            case 2012:
                ParticleUtils.spawnParticleInBlock(this.level, p_234306_, p_234307_, ParticleTypes.HAPPY_VILLAGER);
                break;

            case 2013:
                ParticleUtils.spawnSmashAttackParticles(this.level, p_234306_, p_234307_);
                break;

            case 3000:
                this.level
                .addParticle(
                    ParticleTypes.EXPLOSION_EMITTER,
                    true,
                    (double)p_234306_.getX() + 0.5,
                    (double)p_234306_.getY() + 0.5,
                    (double)p_234306_.getZ() + 0.5,
                    0.0,
                    0.0,
                    0.0
                );
                this.level
                .playLocalSound(
                    p_234306_,
                    SoundEvents.END_GATEWAY_SPAWN,
                    SoundSource.BLOCKS,
                    10.0F,
                    (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F,
                    false
                );
                break;

            case 3001:
                this.level.playLocalSound(p_234306_, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 64.0F, 0.8F + this.level.random.nextFloat() * 0.3F, false);
                break;

            case 3002:
                if (p_234307_ >= 0 && p_234307_ < Direction.Axis.VALUES.length)
                {
                    ParticleUtils.spawnParticlesAlongAxis(
                        Direction.Axis.VALUES[p_234307_], this.level, p_234306_, 0.125, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(10, 19)
                    );
                }
                else
                {
                    ParticleUtils.spawnParticlesOnBlockFaces(this.level, p_234306_, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(3, 5));
                }

                break;

            case 3003:
                ParticleUtils.spawnParticlesOnBlockFaces(this.level, p_234306_, ParticleTypes.WAX_ON, UniformInt.of(3, 5));
                this.level.playLocalSound(p_234306_, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;

            case 3004:
                ParticleUtils.spawnParticlesOnBlockFaces(this.level, p_234306_, ParticleTypes.WAX_OFF, UniformInt.of(3, 5));
                break;

            case 3005:
                ParticleUtils.spawnParticlesOnBlockFaces(this.level, p_234306_, ParticleTypes.SCRAPE, UniformInt.of(3, 5));
                break;

            case 3006:
                int j = p_234307_ >> 6;

                if (j > 0)
                {
                    if (randomsource.nextFloat() < 0.3F + (float)j * 0.1F)
                    {
                        float f3 = 0.15F + 0.02F * (float)j * (float)j * randomsource.nextFloat();
                        float f5 = 0.4F + 0.3F * (float)j * randomsource.nextFloat();
                        this.level.playLocalSound(p_234306_, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, f3, f5, false);
                    }

                    byte b0 = (byte)(p_234307_ & 63);
                    IntProvider intprovider = UniformInt.of(0, j);
                    float f7 = 0.005F;
                    Supplier<Vec3> supplier = () -> new Vec3(
                                                  Mth.nextDouble(randomsource, -0.005F, 0.005F),
                                                  Mth.nextDouble(randomsource, -0.005F, 0.005F),
                                                  Mth.nextDouble(randomsource, -0.005F, 0.005F)
                                              );

                    if (b0 == 0)
                    {
                        for (Direction direction : Direction.values())
                        {
                            float f15 = direction == Direction.DOWN ? (float) Math.PI : 0.0F;
                            double d9 = direction.getAxis() == Direction.Axis.Y ? 0.65 : 0.57;
                            ParticleUtils.spawnParticlesOnBlockFace(this.level, p_234306_, new SculkChargeParticleOptions(f15), intprovider, direction, supplier, d9);
                        }
                    }
                    else
                    {
                        for (Direction direction1 : MultifaceBlock.unpack(b0))
                        {
                            float f12 = direction1 == Direction.UP ? (float) Math.PI : 0.0F;
                            double d25 = 0.35;
                            ParticleUtils.spawnParticlesOnBlockFace(this.level, p_234306_, new SculkChargeParticleOptions(f12), intprovider, direction1, supplier, 0.35);
                        }
                    }
                }
                else
                {
                    this.level.playLocalSound(p_234306_, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                    boolean flag1 = this.level.getBlockState(p_234306_).isCollisionShapeFullBlock(this.level, p_234306_);
                    int i3 = flag1 ? 40 : 20;
                    float f8 = flag1 ? 0.45F : 0.25F;
                    float f9 = 0.07F;

                    for (int j3 = 0; j3 < i3; j3++)
                    {
                        float f10 = 2.0F * randomsource.nextFloat() - 1.0F;
                        float f13 = 2.0F * randomsource.nextFloat() - 1.0F;
                        float f14 = 2.0F * randomsource.nextFloat() - 1.0F;
                        this.level
                        .addParticle(
                            ParticleTypes.SCULK_CHARGE_POP,
                            (double)p_234306_.getX() + 0.5 + (double)(f10 * f8),
                            (double)p_234306_.getY() + 0.5 + (double)(f13 * f8),
                            (double)p_234306_.getZ() + 0.5 + (double)(f14 * f8),
                            (double)(f10 * 0.07F),
                            (double)(f13 * 0.07F),
                            (double)(f14 * 0.07F)
                        );
                    }
                }

                break;

            case 3007:
                for (int k = 0; k < 10; k++)
                {
                    this.level
                    .addParticle(
                        new ShriekParticleOption(k * 5),
                        false,
                        (double)p_234306_.getX() + 0.5,
                        (double)p_234306_.getY() + SculkShriekerBlock.TOP_Y,
                        (double)p_234306_.getZ() + 0.5,
                        0.0,
                        0.0,
                        0.0
                    );
                }

                BlockState blockstate2 = this.level.getBlockState(p_234306_);
                boolean flag = blockstate2.hasProperty(BlockStateProperties.WATERLOGGED) && blockstate2.getValue(BlockStateProperties.WATERLOGGED);

                if (!flag)
                {
                    this.level
                    .playLocalSound(
                        (double)p_234306_.getX() + 0.5,
                        (double)p_234306_.getY() + SculkShriekerBlock.TOP_Y,
                        (double)p_234306_.getZ() + 0.5,
                        SoundEvents.SCULK_SHRIEKER_SHRIEK,
                        SoundSource.BLOCKS,
                        2.0F,
                        0.6F + this.level.random.nextFloat() * 0.4F,
                        false
                    );
                }

                break;

            case 3008:
                BlockState blockstate1 = Block.stateById(p_234307_);

                if (blockstate1.getBlock() instanceof BrushableBlock brushableblock)
                {
                    this.level.playLocalSound(p_234306_, brushableblock.getBrushCompletedSound(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
                }

                this.level.addDestroyBlockEffect(p_234306_, blockstate1);
                break;

            case 3009:
                ParticleUtils.spawnParticlesOnBlockFaces(this.level, p_234306_, ParticleTypes.EGG_CRACK, UniformInt.of(3, 6));
                break;

            case 3011:
                TrialSpawner.addSpawnParticles(this.level, p_234306_, randomsource, TrialSpawner.FlameParticle.decode(p_234307_).particleType);
                break;

            case 3012:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.TRIAL_SPAWNER_SPAWN_MOB, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                );
                TrialSpawner.addSpawnParticles(this.level, p_234306_, randomsource, TrialSpawner.FlameParticle.decode(p_234307_).particleType);
                break;

            case 3013:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                );
                TrialSpawner.addDetectPlayerParticles(this.level, p_234306_, randomsource, p_234307_, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER);
                break;

            case 3014:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.TRIAL_SPAWNER_EJECT_ITEM, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                );
                TrialSpawner.addEjectItemParticles(this.level, p_234306_, randomsource);
                break;

            case 3015:
                if (this.level.getBlockEntity(p_234306_) instanceof VaultBlockEntity vaultblockentity)
                {
                    VaultBlockEntity.Client.emitActivationParticles(
                        this.level,
                        vaultblockentity.getBlockPos(),
                        vaultblockentity.getBlockState(),
                        vaultblockentity.getSharedData(),
                        p_234307_ == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME
                    );
                    this.level
                    .playLocalSound(
                        p_234306_,
                        SoundEvents.VAULT_ACTIVATE,
                        SoundSource.BLOCKS,
                        1.0F,
                        (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                        true
                    );
                }

                break;

            case 3016:
                VaultBlockEntity.Client.emitDeactivationParticles(this.level, p_234306_, p_234307_ == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME);
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.VAULT_DEACTIVATE, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                );
                break;

            case 3017:
                TrialSpawner.addEjectItemParticles(this.level, p_234306_, randomsource);
                break;

            case 3018:
                for (int l = 0; l < 10; l++)
                {
                    double d6 = randomsource.nextGaussian() * 0.02;
                    double d7 = randomsource.nextGaussian() * 0.02;
                    double d8 = randomsource.nextGaussian() * 0.02;
                    this.level
                    .addParticle(
                        ParticleTypes.POOF,
                        (double)p_234306_.getX() + randomsource.nextDouble(),
                        (double)p_234306_.getY() + randomsource.nextDouble(),
                        (double)p_234306_.getZ() + randomsource.nextDouble(),
                        d6,
                        d7,
                        d8
                    );
                }

                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.COBWEB_PLACE, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                );
                break;

            case 3019:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                );
                TrialSpawner.addDetectPlayerParticles(this.level, p_234306_, randomsource, p_234307_, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS);
                break;

            case 3020:
                this.level
                .playLocalSound(
                    p_234306_,
                    SoundEvents.TRIAL_SPAWNER_OMINOUS_ACTIVATE,
                    SoundSource.BLOCKS,
                    p_234307_ == 0 ? 0.3F : 1.0F,
                    (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                    true
                );
                TrialSpawner.addDetectPlayerParticles(this.level, p_234306_, randomsource, 0, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS);
                TrialSpawner.addBecomeOminousParticles(this.level, p_234306_, randomsource);
                break;

            case 3021:
                this.level
                .playLocalSound(
                    p_234306_, SoundEvents.TRIAL_SPAWNER_SPAWN_ITEM, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                );
                TrialSpawner.addSpawnParticles(this.level, p_234306_, randomsource, TrialSpawner.FlameParticle.decode(p_234307_).particleType);
        }
    }

    public void destroyBlockProgress(int p_109775_, BlockPos p_109776_, int p_109777_)
    {
        if (p_109777_ >= 0 && p_109777_ < 10)
        {
            BlockDestructionProgress blockdestructionprogress1 = this.destroyingBlocks.get(p_109775_);

            if (blockdestructionprogress1 != null)
            {
                this.removeProgress(blockdestructionprogress1);
            }

            if (blockdestructionprogress1 == null
                    || blockdestructionprogress1.getPos().getX() != p_109776_.getX()
                    || blockdestructionprogress1.getPos().getY() != p_109776_.getY()
                    || blockdestructionprogress1.getPos().getZ() != p_109776_.getZ())
            {
                blockdestructionprogress1 = new BlockDestructionProgress(p_109775_, p_109776_);
                this.destroyingBlocks.put(p_109775_, blockdestructionprogress1);
            }

            blockdestructionprogress1.setProgress(p_109777_);
            blockdestructionprogress1.updateTick(this.ticks);
            this.destructionProgress.computeIfAbsent(blockdestructionprogress1.getPos().asLong(), keyIn -> Sets.newTreeSet()).add(blockdestructionprogress1);
        }
        else
        {
            BlockDestructionProgress blockdestructionprogress = this.destroyingBlocks.remove(p_109775_);

            if (blockdestructionprogress != null)
            {
                this.removeProgress(blockdestructionprogress);
            }
        }
    }

    public boolean hasRenderedAllSections()
    {
        return this.sectionRenderDispatcher.isQueueEmpty();
    }

    public void onChunkLoaded(ChunkPos p_300326_)
    {
        this.sectionOcclusionGraph.onChunkLoaded(p_300326_);
    }

    public void needsUpdate()
    {
        this.sectionOcclusionGraph.invalidate();
        this.generateClouds = true;
    }

    public int getCountRenderers()
    {
        return this.viewArea.sections.length;
    }

    public int getCountEntitiesRendered()
    {
        return this.renderedEntities;
    }

    public int getCountTileEntitiesRendered()
    {
        return this.countTileEntitiesRendered;
    }

    public int getCountLoadedChunks()
    {
        if (this.level == null)
        {
            return 0;
        }
        else
        {
            ClientChunkCache clientchunkcache = this.level.getChunkSource();
            return clientchunkcache == null ? 0 : clientchunkcache.getLoadedChunksCount();
        }
    }

    public int getCountChunksToUpdate()
    {
        return this.countChunksToUpdate;
    }

    public SectionRenderDispatcher.RenderSection getRenderChunk(BlockPos pos)
    {
        return this.viewArea.getRenderSectionAt(pos);
    }

    public ClientLevel getWorld()
    {
        return this.level;
    }

    private void clearRenderInfos()
    {
        this.clearRenderInfosTerrain();
        this.clearRenderInfosEntities();
    }

    private void clearRenderInfosTerrain()
    {
        if (renderEntitiesCounter > 0)
        {
            this.renderInfosTerrain = new ObjectArrayList<>(this.renderInfosTerrain.size() + 16);
            this.renderInfosTileEntities = new ArrayList<>(this.renderInfosTileEntities.size() + 16);
        }
        else
        {
            this.renderInfosTerrain.clear();
            this.renderInfosTileEntities.clear();
        }
    }

    private void clearRenderInfosEntities()
    {
        if (renderEntitiesCounter > 0)
        {
            this.renderInfosEntities = new LongOpenHashSet(this.renderInfosEntities.size() + 16);
        }
        else
        {
            this.renderInfosEntities.clear();
        }
    }

    public void onPlayerPositionSet()
    {
        if (this.firstWorldLoad)
        {
            this.allChanged();
            this.firstWorldLoad = false;
        }
    }

    public void pauseChunkUpdates()
    {
        if (this.sectionRenderDispatcher != null)
        {
            this.sectionRenderDispatcher.pauseChunkUpdates();
        }
    }

    public void resumeChunkUpdates()
    {
        if (this.sectionRenderDispatcher != null)
        {
            this.sectionRenderDispatcher.resumeChunkUpdates();
        }
    }

    public int getFrameCount()
    {
        return this.frameId;
    }

    public RenderBuffers getRenderTypeTextures()
    {
        return this.renderBuffers;
    }

    public LongOpenHashSet getRenderChunksEntities()
    {
        return this.renderInfosEntities;
    }

    private void addEntitySection(LongOpenHashSet set, EntitySectionStorage storage, BlockPos pos)
    {
        long i = SectionPos.asLong(pos);
        EntitySection entitysection = storage.getSection(i);

        if (entitysection != null)
        {
            set.add(i);
        }
    }

    private boolean hasEntitySection(EntitySectionStorage storage, BlockPos pos)
    {
        long i = SectionPos.asLong(pos);
        EntitySection entitysection = storage.getSection(i);
        return entitysection != null;
    }

    public List<SectionRenderDispatcher.RenderSection> getRenderInfos()
    {
        return this.visibleSections;
    }

    public List<SectionRenderDispatcher.RenderSection> getRenderInfosTerrain()
    {
        return this.renderInfosTerrain;
    }

    public List<SectionRenderDispatcher.RenderSection> getRenderInfosTileEntities()
    {
        return this.renderInfosTileEntities;
    }

    private void checkLoadVisibleChunks(Camera activeRenderInfo, Frustum icamera, boolean spectator)
    {
        if (this.loadVisibleChunksCounter == 0)
        {
            this.loadAllVisibleChunks(activeRenderInfo, icamera, spectator);
            this.minecraft.gui.getChat().deleteMessage(loadVisibleChunksMessageId);
        }

        if (this.loadVisibleChunksCounter >= 0)
        {
            this.loadVisibleChunksCounter--;
        }
    }

    private void loadAllVisibleChunks(Camera activeRenderInfo, Frustum icamera, boolean spectator)
    {
        int i = this.minecraft.options.ofChunkUpdates;
        boolean flag = this.minecraft.options.ofLazyChunkLoading;

        try
        {
            this.minecraft.options.ofChunkUpdates = 1000;
            this.minecraft.options.ofLazyChunkLoading = false;
            LevelRenderer levelrenderer = Config.getRenderGlobal();
            int j = levelrenderer.getCountLoadedChunks();
            long k = System.currentTimeMillis();
            Config.dbg("Loading visible chunks");
            long l = System.currentTimeMillis() + 5000L;
            int i1 = 0;
            boolean flag1 = false;

            do
            {
                flag1 = false;

                for (int j1 = 0; j1 < 100; j1++)
                {
                    levelrenderer.needsUpdate();
                    levelrenderer.setupRender(activeRenderInfo, icamera, false, spectator);
                    Config.sleep(1L);
                    this.compileSections(activeRenderInfo);

                    if (levelrenderer.getCountChunksToUpdate() > 0)
                    {
                        flag1 = true;
                    }

                    if (!levelrenderer.hasRenderedAllSections())
                    {
                        flag1 = true;
                    }

                    i1 += levelrenderer.getCountChunksToUpdate();

                    while (!levelrenderer.hasRenderedAllSections())
                    {
                        int k1 = levelrenderer.getCountChunksToUpdate();
                        this.compileSections(activeRenderInfo);

                        if (k1 == levelrenderer.getCountChunksToUpdate())
                        {
                            break;
                        }
                    }

                    i1 -= levelrenderer.getCountChunksToUpdate();

                    if (!flag1)
                    {
                        break;
                    }
                }

                if (levelrenderer.getCountLoadedChunks() != j)
                {
                    flag1 = true;
                    j = levelrenderer.getCountLoadedChunks();
                }

                if (System.currentTimeMillis() > l)
                {
                    Config.log("Chunks loaded: " + i1);
                    l = System.currentTimeMillis() + 5000L;
                }
            }
            while (flag1);

            Config.log("Chunks loaded: " + i1);
            Config.log("Finished loading visible chunks");
            SectionRenderDispatcher.renderChunksUpdated = 0;
        }
        finally
        {
            this.minecraft.options.ofChunkUpdates = i;
            this.minecraft.options.ofLazyChunkLoading = flag;
        }
    }

    public void applyFrustumEntities(Frustum camera, int maxChunkDistance)
    {
        this.renderInfosEntities.clear();
        int i = (int)camera.getCameraX() >> 4 << 4;
        int j = (int)camera.getCameraY() >> 4 << 4;
        int k = (int)camera.getCameraZ() >> 4 << 4;
        int l = maxChunkDistance * maxChunkDistance;
        EntitySectionStorage<?> entitysectionstorage = this.level.getSectionStorage();
        BlockPosM blockposm = new BlockPosM();
        LongSet longset = entitysectionstorage.getSectionKeys();
        LongIterator longiterator = longset.iterator();

        while (longiterator.hasNext())
        {
            long i1 = longiterator.nextLong();
            blockposm.setXyz(
                SectionPos.sectionToBlockCoord(SectionPos.x(i1)), SectionPos.sectionToBlockCoord(SectionPos.y(i1)), SectionPos.sectionToBlockCoord(SectionPos.z(i1))
            );
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSectionAt(blockposm);

            if (sectionrenderdispatcher$rendersection != null && camera.isVisible(sectionrenderdispatcher$rendersection.getBoundingBox()))
            {
                if (maxChunkDistance > 0)
                {
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getOrigin();
                    int j1 = i - blockpos.getX();
                    int k1 = j - blockpos.getY();
                    int l1 = k - blockpos.getZ();
                    int i2 = j1 * j1 + k1 * k1 + l1 * l1;

                    if (i2 > l)
                    {
                        continue;
                    }
                }

                this.renderInfosEntities.add(i1);
            }
        }
    }

    public void setShadowRenderInfos(boolean shadowInfos)
    {
        if (shadowInfos)
        {
            this.renderInfosTerrain = this.renderInfosTerrainShadow;
            this.renderInfosEntities = this.renderInfosEntitiesShadow;
            this.renderInfosTileEntities = this.renderInfosTileEntitiesShadow;
        }
        else
        {
            this.renderInfosTerrain = this.renderInfosTerrainNormal;
            this.renderInfosEntities = this.renderInfosEntitiesNormal;
            this.renderInfosTileEntities = this.renderInfosTileEntitiesNormal;
        }
    }

    public int getRenderedChunksShadow()
    {
        return !Config.isShadersShadows() ? -1 : this.renderInfosTerrainShadow.size();
    }

    public int getCountEntitiesRenderedShadow()
    {
        return !Config.isShadersShadows() ? -1 : ShadersRender.countEntitiesRenderedShadow;
    }

    public int getCountTileEntitiesRenderedShadow()
    {
        if (!Config.isShaders())
        {
            return -1;
        }
        else
        {
            return !Shaders.hasShadowMap ? -1 : ShadersRender.countTileEntitiesRenderedShadow;
        }
    }

    public void captureFrustumShadow()
    {
        this.debugFixTerrainFrustumShadow = true;
    }

    public boolean isDebugFrustum()
    {
        return this.capturedFrustum != null;
    }

    public void onChunkRenderNeedsUpdate(SectionRenderDispatcher.RenderSection renderChunk)
    {
        if (!renderChunk.getCompiled().hasTerrainBlockEntities())
        {
            ;
        }
    }

    public boolean needsFrustumUpdate()
    {
        return this.sectionOcclusionGraph.needsFrustumUpdate();
    }

    public boolean shouldRenderEntity(Entity entity, int minWorldY, int maxWorldY)
    {
        if (entity instanceof Display)
        {
            return true;
        }
        else
        {
            if (entity instanceof LivingEntity livingentity && livingentity.getScale() > 1.0F)
            {
                return true;
            }

            BlockPos blockpos = entity.blockPosition();
            return this.renderInfosEntities.contains(SectionPos.asLong(blockpos)) || blockpos.getY() <= minWorldY || blockpos.getY() >= maxWorldY;
        }
    }

    public Frustum getFrustum()
    {
        return this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum;
    }

    public int getTicks()
    {
        return this.ticks;
    }

    public void updateGlobalBlockEntities(Collection<BlockEntity> p_109763_, Collection<BlockEntity> p_109764_)
    {
        synchronized (this.globalBlockEntities)
        {
            this.globalBlockEntities.removeAll(p_109763_);
            this.globalBlockEntities.addAll(p_109764_);
        }
    }

    public static int getLightColor(BlockAndTintGetter p_109542_, BlockPos p_109543_)
    {
        return getLightColor(p_109542_, p_109542_.getBlockState(p_109543_), p_109543_);
    }

    public static int getLightColor(BlockAndTintGetter p_109538_, BlockState p_109539_, BlockPos p_109540_)
    {
        return getPackedLightmapCoords(p_109538_, p_109539_, p_109540_, ambientOcclusion);
    }

    public static int getPackedLightmapCoords(BlockAndTintGetter lightReaderIn, BlockState blockStateIn, BlockPos blockPosIn, boolean ambientOcclusionIn)
    {
        if (EmissiveTextures.isRenderEmissive() && Config.isMinecraftThread())
        {
            return LightTexture.MAX_BRIGHTNESS;
        }
        else if (blockStateIn.emissiveRendering(lightReaderIn, blockPosIn))
        {
            return 15794417;
        }
        else
        {
            int i = lightReaderIn.getBrightness(LightLayer.SKY, blockPosIn);
            int j = lightReaderIn.getBrightness(LightLayer.BLOCK, blockPosIn);
            int k = blockStateIn.getLightValue(lightReaderIn, blockPosIn);

            if (j < k)
            {
                j = k;
            }

            int l = i << 20 | j << 4;

            if (Config.isDynamicLights() && lightReaderIn instanceof BlockGetter && (!ambientOcclusionIn || !blockStateIn.isSolidRender(lightReaderIn, blockPosIn)))
            {
                l = DynamicLights.getCombinedLight(blockPosIn, l);
            }

            return l;
        }
    }

    public boolean isSectionCompiled(BlockPos p_300380_)
    {
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSectionAt(p_300380_);
        return sectionrenderdispatcher$rendersection != null
               && sectionrenderdispatcher$rendersection.compiled.get() != SectionRenderDispatcher.CompiledSection.UNCOMPILED;
    }

    @Nullable
    public RenderTarget entityTarget()
    {
        return this.entityTarget;
    }

    @Nullable
    public RenderTarget getTranslucentTarget()
    {
        return this.translucentTarget;
    }

    @Nullable
    public RenderTarget getItemEntityTarget()
    {
        return this.itemEntityTarget;
    }

    @Nullable
    public RenderTarget getParticlesTarget()
    {
        return this.particlesTarget;
    }

    @Nullable
    public RenderTarget getWeatherTarget()
    {
        return this.weatherTarget;
    }

    @Nullable
    public RenderTarget getCloudsTarget()
    {
        return this.cloudsTarget;
    }

    private void shootParticles(int p_313147_, BlockPos p_313126_, RandomSource p_311422_, SimpleParticleType p_309823_)
    {
        Direction direction = Direction.from3DDataValue(p_313147_);
        int i = direction.getStepX();
        int j = direction.getStepY();
        int k = direction.getStepZ();
        double d0 = (double)p_313126_.getX() + (double)i * 0.6 + 0.5;
        double d1 = (double)p_313126_.getY() + (double)j * 0.6 + 0.5;
        double d2 = (double)p_313126_.getZ() + (double)k * 0.6 + 0.5;

        for (int l = 0; l < 10; l++)
        {
            double d3 = p_311422_.nextDouble() * 0.2 + 0.01;
            double d4 = d0 + (double)i * 0.01 + (p_311422_.nextDouble() - 0.5) * (double)k * 0.5;
            double d5 = d1 + (double)j * 0.01 + (p_311422_.nextDouble() - 0.5) * (double)j * 0.5;
            double d6 = d2 + (double)k * 0.01 + (p_311422_.nextDouble() - 0.5) * (double)i * 0.5;
            double d7 = (double)i * d3 + p_311422_.nextGaussian() * 0.01;
            double d8 = (double)j * d3 + p_311422_.nextGaussian() * 0.01;
            double d9 = (double)k * d3 + p_311422_.nextGaussian() * 0.01;
            this.addParticle(p_309823_, d4, d5, d6, d7, d8, d9);
        }
    }

    public static class TransparencyShaderException extends RuntimeException
    {
        public TransparencyShaderException(String p_109868_, Throwable p_109869_)
        {
            super(p_109868_, p_109869_);
        }
    }
}
