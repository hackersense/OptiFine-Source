package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraftforge.client.model.data.ModelDataManager;
import net.minecraftforge.client.model.lighting.QuadLighter;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.eventbus.api.Event;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.CustomGuis;
import net.optifine.DynamicLights;
import net.optifine.RandomEntities;
import net.optifine.Vec3M;
import net.optifine.override.PlayerControllerOF;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorForge;
import net.optifine.shaders.Shaders;
import org.slf4j.Logger;

public class ClientLevel extends Level
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double FLUID_PARTICLE_SPAWN_OFFSET = 0.05;
    private static final int NORMAL_LIGHT_UPDATES_PER_FRAME = 10;
    private static final int LIGHT_UPDATE_QUEUE_SIZE_THRESHOLD = 1000;
    final EntityTickList tickingEntities = new EntityTickList();
    private final TransientEntitySectionManager<Entity> entityStorage = new TransientEntitySectionManager<>(Entity.class, new ClientLevel.EntityCallbacks());
    private final ClientPacketListener connection;
    private final LevelRenderer levelRenderer;
    private final ClientLevel.ClientLevelData clientLevelData;
    private final DimensionSpecialEffects effects;
    private final TickRateManager tickRateManager;
    private final Minecraft minecraft = Minecraft.getInstance();
    final List<AbstractClientPlayer> players = Lists.newArrayList();
    private final Map<MapId, MapItemSavedData> mapData = Maps.newHashMap();
    private static final long CLOUD_COLOR = 16777215L;
    private int skyFlashTime;
    private final Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches = Util.make(new Object2ObjectArrayMap<>(3), mapIn ->
    {
        mapIn.put(BiomeColors.GRASS_COLOR_RESOLVER, new BlockTintCache(posIn -> this.calculateBlockTint(posIn, BiomeColors.GRASS_COLOR_RESOLVER)));
        mapIn.put(BiomeColors.FOLIAGE_COLOR_RESOLVER, new BlockTintCache(posIn -> this.calculateBlockTint(posIn, BiomeColors.FOLIAGE_COLOR_RESOLVER)));
        mapIn.put(BiomeColors.WATER_COLOR_RESOLVER, new BlockTintCache(posIn -> this.calculateBlockTint(posIn, BiomeColors.WATER_COLOR_RESOLVER)));
        Reflector.ColorResolverManager_registerBlockTintCaches.call(this, mapIn);
    });
    private final ClientChunkCache chunkSource;
    private final Deque<Runnable> lightUpdateQueue = Queues.newArrayDeque();
    private int serverSimulationDistance;
    private final BlockStatePredictionHandler blockStatePredictionHandler = new BlockStatePredictionHandler();
    private static final Set<Item> MARKER_PARTICLE_ITEMS = Set.of(Items.BARRIER, Items.LIGHT);
    private final Int2ObjectMap < PartEntity<? >> partEntities = new Int2ObjectOpenHashMap<>();
    private final ModelDataManager modelDataManager = new ModelDataManager(this);
    private boolean playerUpdate = false;

    public void handleBlockChangedAck(int p_233652_)
    {
        this.blockStatePredictionHandler.endPredictionsUpTo(p_233652_, this);
    }

    public void setServerVerifiedBlockState(BlockPos p_233654_, BlockState p_233655_, int p_233656_)
    {
        if (!this.blockStatePredictionHandler.updateKnownServerState(p_233654_, p_233655_))
        {
            super.setBlock(p_233654_, p_233655_, p_233656_, 512);
        }
    }

    public void syncBlockState(BlockPos p_233648_, BlockState p_233649_, Vec3 p_233650_)
    {
        BlockState blockstate = this.getBlockState(p_233648_);

        if (blockstate != p_233649_)
        {
            this.setBlock(p_233648_, p_233649_, 19);
            Player player = this.minecraft.player;

            if (this == player.level() && player.isColliding(p_233648_, p_233649_))
            {
                player.absMoveTo(p_233650_.x, p_233650_.y, p_233650_.z);
            }
        }
    }

    BlockStatePredictionHandler getBlockStatePredictionHandler()
    {
        return this.blockStatePredictionHandler;
    }

    @Override
    public boolean setBlock(BlockPos p_233643_, BlockState p_233644_, int p_233645_, int p_233646_)
    {
        if (this.blockStatePredictionHandler.isPredicting())
        {
            BlockState blockstate = this.getBlockState(p_233643_);
            boolean flag = super.setBlock(p_233643_, p_233644_, p_233645_, p_233646_);

            if (flag)
            {
                this.blockStatePredictionHandler.retainKnownServerState(p_233643_, blockstate, this.minecraft.player);
            }

            return flag;
        }
        else
        {
            return super.setBlock(p_233643_, p_233644_, p_233645_, p_233646_);
        }
    }

    public ClientLevel(
        ClientPacketListener p_205505_,
        ClientLevel.ClientLevelData p_205506_,
        ResourceKey<Level> p_205507_,
        Holder<DimensionType> p_205508_,
        int p_205509_,
        int p_205510_,
        Supplier<ProfilerFiller> p_205511_,
        LevelRenderer p_205512_,
        boolean p_205513_,
        long p_205514_
    )
    {
        super(p_205506_, p_205507_, p_205505_.registryAccess(), p_205508_, p_205511_, true, p_205513_, p_205514_, 1000000);
        this.connection = p_205505_;
        this.chunkSource = new ClientChunkCache(this, p_205509_);
        this.tickRateManager = new TickRateManager();
        this.clientLevelData = p_205506_;
        this.levelRenderer = p_205512_;
        this.effects = DimensionSpecialEffects.forType(p_205508_.value());
        this.setDefaultSpawnPos(new BlockPos(8, 64, 8), 0.0F);
        this.serverSimulationDistance = p_205510_;
        this.updateSkyBrightness();
        this.prepareWeather();

        if (Reflector.CapabilityProvider_gatherCapabilities.exists() && Reflector.CapabilityProvider.getTargetClass().isAssignableFrom(this.getClass()))
        {
            Reflector.call(this, Reflector.CapabilityProvider_gatherCapabilities);
        }

        Reflector.ForgeEventFactory_onLevelLoad.call(this);

        if (this.minecraft.gameMode != null && this.minecraft.gameMode.getClass() == MultiPlayerGameMode.class)
        {
            this.minecraft.gameMode = new PlayerControllerOF(this.minecraft, this.connection);
            CustomGuis.setPlayerControllerOF((PlayerControllerOF)this.minecraft.gameMode);
        }
    }

    public void queueLightUpdate(Runnable p_194172_)
    {
        this.lightUpdateQueue.add(p_194172_);
    }

    public void pollLightUpdates()
    {
        int i = this.lightUpdateQueue.size();
        int j = i < 1000 ? Math.max(10, i / 10) : i;

        for (int k = 0; k < j; k++)
        {
            Runnable runnable = this.lightUpdateQueue.poll();

            if (runnable == null)
            {
                break;
            }

            runnable.run();
        }
    }

    public boolean isLightUpdateQueueEmpty()
    {
        return this.lightUpdateQueue.isEmpty();
    }

    public DimensionSpecialEffects effects()
    {
        return this.effects;
    }

    public void tick(BooleanSupplier p_104727_)
    {
        this.getWorldBorder().tick();

        if (this.tickRateManager().runsNormally())
        {
            this.tickTime();
        }

        if (this.skyFlashTime > 0)
        {
            this.setSkyFlashTime(this.skyFlashTime - 1);
        }

        this.getProfiler().push("blocks");
        this.chunkSource.tick(p_104727_, true);
        this.getProfiler().pop();
    }

    private void tickTime()
    {
        this.setGameTime(this.levelData.getGameTime() + 1L);

        if (this.levelData.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT))
        {
            this.setDayTime(this.levelData.getDayTime() + 1L);
        }
    }

    public void setGameTime(long p_104638_)
    {
        this.clientLevelData.setGameTime(p_104638_);
    }

    public void setDayTime(long p_104747_)
    {
        if (p_104747_ < 0L)
        {
            p_104747_ = -p_104747_;
            this.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        }
        else
        {
            this.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, null);
        }

        this.clientLevelData.setDayTime(p_104747_);
    }

    public Iterable<Entity> entitiesForRendering()
    {
        return this.getEntities().getAll();
    }

    public void tickEntities()
    {
        ProfilerFiller profilerfiller = this.getProfiler();
        profilerfiller.push("entities");
        this.tickingEntities.forEach(entityIn ->
        {
            if (!entityIn.isRemoved() && !entityIn.isPassenger() && !this.tickRateManager.isEntityFrozen(entityIn))
            {
                this.guardEntityTick(this::tickNonPassenger, entityIn);
            }
        });
        profilerfiller.pop();
        this.tickBlockEntities();
    }

    @Override
    public boolean shouldTickDeath(Entity p_194185_)
    {
        return p_194185_.chunkPosition().getChessboardDistance(this.minecraft.player.chunkPosition()) <= this.serverSimulationDistance;
    }

    public void tickNonPassenger(Entity p_104640_)
    {
        p_104640_.setOldPosAndRot();
        p_104640_.tickCount++;
        this.getProfiler().push(() -> BuiltInRegistries.ENTITY_TYPE.getKey(p_104640_.getType()).toString());

        if (ReflectorForge.canUpdate(p_104640_))
        {
            p_104640_.tick();
        }

        if (p_104640_.isRemoved())
        {
            this.onEntityRemoved(p_104640_);
        }

        this.getProfiler().pop();

        for (Entity entity : p_104640_.getPassengers())
        {
            this.tickPassenger(p_104640_, entity);
        }
    }

    private void tickPassenger(Entity p_104642_, Entity p_104643_)
    {
        if (!p_104643_.isRemoved() && p_104643_.getVehicle() == p_104642_)
        {
            if (p_104643_ instanceof Player || this.tickingEntities.contains(p_104643_))
            {
                p_104643_.setOldPosAndRot();
                p_104643_.tickCount++;
                p_104643_.rideTick();

                for (Entity entity : p_104643_.getPassengers())
                {
                    this.tickPassenger(p_104643_, entity);
                }
            }
        }
        else
        {
            p_104643_.stopRiding();
        }
    }

    public void unload(LevelChunk p_104666_)
    {
        p_104666_.clearAllBlockEntities();
        this.chunkSource.getLightEngine().setLightEnabled(p_104666_.getPos(), false);
        this.entityStorage.stopTicking(p_104666_.getPos());
    }

    public void onChunkLoaded(ChunkPos p_171650_)
    {
        this.tintCaches.forEach((resolverIn, cacheIn) -> cacheIn.invalidateForChunk(p_171650_.x, p_171650_.z));
        this.entityStorage.startTicking(p_171650_);
        this.levelRenderer.onChunkLoaded(p_171650_);
    }

    public void clearTintCaches()
    {
        this.tintCaches.forEach((resolverIn, cacheIn) -> cacheIn.invalidateAll());
    }

    @Override
    public boolean hasChunk(int p_104737_, int p_104738_)
    {
        return true;
    }

    public int getEntityCount()
    {
        return this.entityStorage.count();
    }

    public void addEntity(Entity p_104741_)
    {
        if (!Reflector.ForgeEventFactory_onEntityJoinLevel.exists() || !Reflector.ForgeEventFactory_onEntityJoinLevel.callBoolean(p_104741_, this))
        {
            this.removeEntity(p_104741_.getId(), Entity.RemovalReason.DISCARDED);
            this.entityStorage.addEntity(p_104741_);

            if (Reflector.IForgeEntity_onAddedToWorld.exists())
            {
                Reflector.call(p_104741_, Reflector.IForgeEntity_onAddedToWorld);
            }

            this.onEntityAdded(p_104741_);
        }
    }

    public void removeEntity(int p_171643_, Entity.RemovalReason p_171644_)
    {
        Entity entity = this.getEntities().get(p_171643_);

        if (entity != null)
        {
            entity.setRemoved(p_171644_);
            entity.onClientRemoval();
        }
    }

    @Nullable
    @Override
    public Entity getEntity(int p_104609_)
    {
        return this.getEntities().get(p_104609_);
    }

    @Override
    public void disconnect()
    {
        this.connection.getConnection().disconnect(Component.translatable("multiplayer.status.quitting"));
    }

    public void animateTick(int p_104785_, int p_104786_, int p_104787_)
    {
        int i = 32;
        RandomSource randomsource = RandomSource.create();
        Block block = this.getMarkerParticleTarget();
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int j = 0; j < 667; j++)
        {
            this.doAnimateTick(p_104785_, p_104786_, p_104787_, 16, randomsource, block, blockpos$mutableblockpos);
            this.doAnimateTick(p_104785_, p_104786_, p_104787_, 32, randomsource, block, blockpos$mutableblockpos);
        }
    }

    @Nullable
    private Block getMarkerParticleTarget()
    {
        if (this.minecraft.gameMode.getPlayerMode() == GameType.CREATIVE)
        {
            ItemStack itemstack = this.minecraft.player.getMainHandItem();
            Item item = itemstack.getItem();

            if (MARKER_PARTICLE_ITEMS.contains(item) && item instanceof BlockItem blockitem)
            {
                return blockitem.getBlock();
            }
        }

        return null;
    }

    public void doAnimateTick(
        int p_233613_, int p_233614_, int p_233615_, int p_233616_, RandomSource p_233617_, @Nullable Block p_233618_, BlockPos.MutableBlockPos p_233619_
    )
    {
        int i = p_233613_ + this.random.nextInt(p_233616_) - this.random.nextInt(p_233616_);
        int j = p_233614_ + this.random.nextInt(p_233616_) - this.random.nextInt(p_233616_);
        int k = p_233615_ + this.random.nextInt(p_233616_) - this.random.nextInt(p_233616_);
        p_233619_.set(i, j, k);
        BlockState blockstate = this.getBlockState(p_233619_);
        blockstate.getBlock().animateTick(blockstate, this, p_233619_, p_233617_);
        FluidState fluidstate = this.getFluidState(p_233619_);

        if (!fluidstate.isEmpty())
        {
            fluidstate.animateTick(this, p_233619_, p_233617_);
            ParticleOptions particleoptions = fluidstate.getDripParticle();

            if (particleoptions != null && this.random.nextInt(10) == 0)
            {
                boolean flag = blockstate.isFaceSturdy(this, p_233619_, Direction.DOWN);
                BlockPos blockpos = p_233619_.below();
                this.trySpawnDripParticles(blockpos, this.getBlockState(blockpos), particleoptions, flag);
            }
        }

        if (p_233618_ == blockstate.getBlock())
        {
            this.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, blockstate), (double)i + 0.5, (double)j + 0.5, (double)k + 0.5, 0.0, 0.0, 0.0);
        }

        if (!blockstate.isCollisionShapeFullBlock(this, p_233619_))
        {
            this.getBiome(p_233619_)
            .value()
            .getAmbientParticle()
            .ifPresent(
                settingsIn ->
            {
                if (settingsIn.canSpawn(this.random))
                {
                    this.addParticle(
                        settingsIn.getOptions(),
                        (double)p_233619_.getX() + this.random.nextDouble(),
                        (double)p_233619_.getY() + this.random.nextDouble(),
                        (double)p_233619_.getZ() + this.random.nextDouble(),
                        0.0,
                        0.0,
                        0.0
                    );
                }
            }
            );
        }
    }

    private void trySpawnDripParticles(BlockPos p_104690_, BlockState p_104691_, ParticleOptions p_104692_, boolean p_104693_)
    {
        if (p_104691_.getFluidState().isEmpty())
        {
            VoxelShape voxelshape = p_104691_.getCollisionShape(this, p_104690_);
            double d0 = voxelshape.max(Direction.Axis.Y);

            if (d0 < 1.0)
            {
                if (p_104693_)
                {
                    this.spawnFluidParticle(
                        (double)p_104690_.getX(),
                        (double)(p_104690_.getX() + 1),
                        (double)p_104690_.getZ(),
                        (double)(p_104690_.getZ() + 1),
                        (double)(p_104690_.getY() + 1) - 0.05,
                        p_104692_
                    );
                }
            }
            else if (!p_104691_.is(BlockTags.IMPERMEABLE))
            {
                double d1 = voxelshape.min(Direction.Axis.Y);

                if (d1 > 0.0)
                {
                    this.spawnParticle(p_104690_, p_104692_, voxelshape, (double)p_104690_.getY() + d1 - 0.05);
                }
                else
                {
                    BlockPos blockpos = p_104690_.below();
                    BlockState blockstate = this.getBlockState(blockpos);
                    VoxelShape voxelshape1 = blockstate.getCollisionShape(this, blockpos);
                    double d2 = voxelshape1.max(Direction.Axis.Y);

                    if (d2 < 1.0 && blockstate.getFluidState().isEmpty())
                    {
                        this.spawnParticle(p_104690_, p_104692_, voxelshape, (double)p_104690_.getY() - 0.05);
                    }
                }
            }
        }
    }

    private void spawnParticle(BlockPos p_104695_, ParticleOptions p_104696_, VoxelShape p_104697_, double p_104698_)
    {
        this.spawnFluidParticle(
            (double)p_104695_.getX() + p_104697_.min(Direction.Axis.X),
            (double)p_104695_.getX() + p_104697_.max(Direction.Axis.X),
            (double)p_104695_.getZ() + p_104697_.min(Direction.Axis.Z),
            (double)p_104695_.getZ() + p_104697_.max(Direction.Axis.Z),
            p_104698_,
            p_104696_
        );
    }

    private void spawnFluidParticle(double p_104593_, double p_104594_, double p_104595_, double p_104596_, double p_104597_, ParticleOptions p_104598_)
    {
        this.addParticle(
            p_104598_,
            Mth.lerp(this.random.nextDouble(), p_104593_, p_104594_),
            p_104597_,
            Mth.lerp(this.random.nextDouble(), p_104595_, p_104596_),
            0.0,
            0.0,
            0.0
        );
    }

    @Override
    public CrashReportCategory fillReportDetails(CrashReport p_104729_)
    {
        CrashReportCategory crashreportcategory = super.fillReportDetails(p_104729_);
        crashreportcategory.setDetail("Server brand", () -> this.minecraft.player.connection.serverBrand());
        crashreportcategory.setDetail(
            "Server type", () -> this.minecraft.getSingleplayerServer() == null ? "Non-integrated multiplayer server" : "Integrated singleplayer server"
        );
        crashreportcategory.setDetail("Tracked entity count", () -> String.valueOf(this.getEntityCount()));
        return crashreportcategory;
    }

    @Override
    public void playSeededSound(
        @Nullable Player p_263381_,
        double p_263372_,
        double p_263404_,
        double p_263365_,
        Holder<SoundEvent> p_263335_,
        SoundSource p_263417_,
        float p_263416_,
        float p_263349_,
        long p_263408_
    )
    {
        if (Reflector.ForgeEventFactory_onPlaySoundAtPosition.exists())
        {
            Event event = (Event)Reflector.ForgeEventFactory_onPlaySoundAtPosition
                          .call(this, p_263372_, p_263404_, p_263365_, p_263335_, p_263417_, p_263416_, p_263349_);

            if (event.isCanceled() || Reflector.call(event, Reflector.PlayLevelSoundEvent_getSound) == null)
            {
                return;
            }

            p_263335_ = (Holder<SoundEvent>)Reflector.call(event, Reflector.PlayLevelSoundEvent_getSound);
            p_263417_ = (SoundSource)Reflector.call(event, Reflector.PlayLevelSoundEvent_getSource);
            p_263416_ = Reflector.callFloat(event, Reflector.PlayLevelSoundEvent_getNewVolume);
            p_263349_ = Reflector.callFloat(event, Reflector.PlayLevelSoundEvent_getNewPitch);
        }

        if (p_263381_ == this.minecraft.player)
        {
            this.playSound(p_263372_, p_263404_, p_263365_, p_263335_.value(), p_263417_, p_263416_, p_263349_, false, p_263408_);
        }
    }

    @Override
    public void playSeededSound(
        @Nullable Player p_263514_, Entity p_263536_, Holder<SoundEvent> p_263518_, SoundSource p_263487_, float p_263538_, float p_263524_, long p_263509_
    )
    {
        if (Reflector.ForgeEventFactory_onPlaySoundAtEntity.exists())
        {
            Event event = (Event)Reflector.ForgeEventFactory_onPlaySoundAtEntity.call(p_263536_, p_263518_, p_263487_, p_263538_, p_263524_);

            if (event.isCanceled() || Reflector.call(event, Reflector.PlayLevelSoundEvent_getSound) == null)
            {
                return;
            }

            p_263518_ = (Holder<SoundEvent>)Reflector.call(event, Reflector.PlayLevelSoundEvent_getSound);
            p_263487_ = (SoundSource)Reflector.call(event, Reflector.PlayLevelSoundEvent_getSource);
            p_263538_ = Reflector.callFloat(event, Reflector.PlayLevelSoundEvent_getNewVolume);
            p_263524_ = Reflector.callFloat(event, Reflector.PlayLevelSoundEvent_getNewPitch);
        }

        if (p_263514_ == this.minecraft.player)
        {
            this.minecraft.getSoundManager().play(new EntityBoundSoundInstance(p_263518_.value(), p_263487_, p_263538_, p_263524_, p_263536_, p_263509_));
        }
    }

    @Override
    public void playLocalSound(Entity p_311985_, SoundEvent p_309564_, SoundSource p_310311_, float p_309468_, float p_312364_)
    {
        this.minecraft.getSoundManager().play(new EntityBoundSoundInstance(p_309564_, p_310311_, p_309468_, p_312364_, p_311985_, this.random.nextLong()));
    }

    @Override
    public void playLocalSound(
        double p_104600_, double p_104601_, double p_104602_, SoundEvent p_104603_, SoundSource p_104604_, float p_104605_, float p_104606_, boolean p_104607_
    )
    {
        this.playSound(p_104600_, p_104601_, p_104602_, p_104603_, p_104604_, p_104605_, p_104606_, p_104607_, this.random.nextLong());
    }

    private void playSound(
        double p_233603_,
        double p_233604_,
        double p_233605_,
        SoundEvent p_233606_,
        SoundSource p_233607_,
        float p_233608_,
        float p_233609_,
        boolean p_233610_,
        long p_233611_
    )
    {
        double d0 = this.minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(p_233603_, p_233604_, p_233605_);
        SimpleSoundInstance simplesoundinstance = new SimpleSoundInstance(
            p_233606_, p_233607_, p_233608_, p_233609_, RandomSource.create(p_233611_), p_233603_, p_233604_, p_233605_
        );

        if (p_233610_ && d0 > 100.0)
        {
            double d1 = Math.sqrt(d0) / 40.0;
            this.minecraft.getSoundManager().playDelayed(simplesoundinstance, (int)(d1 * 20.0));
        }
        else
        {
            this.minecraft.getSoundManager().play(simplesoundinstance);
        }
    }

    @Override
    public void createFireworks(
        double p_104585_, double p_104586_, double p_104587_, double p_104588_, double p_104589_, double p_104590_, List<FireworkExplosion> p_330286_
    )
    {
        if (p_330286_.isEmpty())
        {
            for (int i = 0; i < this.random.nextInt(3) + 2; i++)
            {
                this.addParticle(
                    ParticleTypes.POOF, p_104585_, p_104586_, p_104587_, this.random.nextGaussian() * 0.05, 0.005, this.random.nextGaussian() * 0.05
                );
            }
        }
        else
        {
            this.minecraft
            .particleEngine
            .add(
                new FireworkParticles.Starter(this, p_104585_, p_104586_, p_104587_, p_104588_, p_104589_, p_104590_, this.minecraft.particleEngine, p_330286_)
            );
        }
    }

    @Override
    public void sendPacketToServer(Packet<?> p_104734_)
    {
        this.connection.send(p_104734_);
    }

    @Override
    public RecipeManager getRecipeManager()
    {
        return this.connection.getRecipeManager();
    }

    @Override
    public TickRateManager tickRateManager()
    {
        return this.tickRateManager;
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks()
    {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks()
    {
        return BlackholeTickAccess.emptyLevelList();
    }

    public ClientChunkCache getChunkSource()
    {
        return this.chunkSource;
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState newState, int flags)
    {
        this.playerUpdate = this.isPlayerActing();
        boolean flag = super.setBlock(pos, newState, flags);
        this.playerUpdate = false;
        return flag;
    }

    private boolean isPlayerActing()
    {
        return this.minecraft.gameMode instanceof PlayerControllerOF playercontrollerof ? playercontrollerof.isActing() : false;
    }

    public boolean isPlayerUpdate()
    {
        return this.playerUpdate;
    }

    public void onEntityAdded(Entity entityIn)
    {
        RandomEntities.entityLoaded(entityIn, this);

        if (Config.isDynamicLights())
        {
            DynamicLights.entityAdded(entityIn, Config.getRenderGlobal());
        }
    }

    public void onEntityRemoved(Entity entityIn)
    {
        RandomEntities.entityUnloaded(entityIn, this);

        if (Config.isDynamicLights())
        {
            DynamicLights.entityRemoved(entityIn, Config.getRenderGlobal());
        }
    }

    @Nullable
    @Override
    public MapItemSavedData getMapData(MapId p_334091_)
    {
        return this.mapData.get(p_334091_);
    }

    public void overrideMapData(MapId p_328178_, MapItemSavedData p_259308_)
    {
        this.mapData.put(p_328178_, p_259308_);
    }

    @Override
    public void setMapData(MapId p_329658_, MapItemSavedData p_171671_)
    {
    }

    @Override
    public MapId getFreeMapId()
    {
        return new MapId(0);
    }

    @Override
    public Scoreboard getScoreboard()
    {
        return this.connection.scoreboard();
    }

    @Override
    public void sendBlockUpdated(BlockPos p_104685_, BlockState p_104686_, BlockState p_104687_, int p_104688_)
    {
        this.levelRenderer.blockChanged(this, p_104685_, p_104686_, p_104687_, p_104688_);
    }

    @Override
    public void setBlocksDirty(BlockPos p_104759_, BlockState p_104760_, BlockState p_104761_)
    {
        this.levelRenderer.setBlockDirty(p_104759_, p_104760_, p_104761_);
    }

    public void setSectionDirtyWithNeighbors(int p_104794_, int p_104795_, int p_104796_)
    {
        this.levelRenderer.setSectionDirtyWithNeighbors(p_104794_, p_104795_, p_104796_);
    }

    @Override
    public void destroyBlockProgress(int p_104634_, BlockPos p_104635_, int p_104636_)
    {
        this.levelRenderer.destroyBlockProgress(p_104634_, p_104635_, p_104636_);
    }

    @Override
    public void globalLevelEvent(int p_104743_, BlockPos p_104744_, int p_104745_)
    {
        this.levelRenderer.globalLevelEvent(p_104743_, p_104744_, p_104745_);
    }

    @Override
    public void levelEvent(@Nullable Player p_104654_, int p_104655_, BlockPos p_104656_, int p_104657_)
    {
        try
        {
            this.levelRenderer.levelEvent(p_104655_, p_104656_, p_104657_);
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Playing level event");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Level event being played");
            crashreportcategory.setDetail("Block coordinates", CrashReportCategory.formatLocation(this, p_104656_));
            crashreportcategory.setDetail("Event source", p_104654_);
            crashreportcategory.setDetail("Event type", p_104655_);
            crashreportcategory.setDetail("Event data", p_104657_);
            throw new ReportedException(crashreport);
        }
    }

    @Override
    public void addParticle(ParticleOptions p_104706_, double p_104707_, double p_104708_, double p_104709_, double p_104710_, double p_104711_, double p_104712_)
    {
        this.levelRenderer.addParticle(p_104706_, p_104706_.getType().getOverrideLimiter(), p_104707_, p_104708_, p_104709_, p_104710_, p_104711_, p_104712_);
    }

    @Override
    public void addParticle(
        ParticleOptions p_104714_,
        boolean p_104715_,
        double p_104716_,
        double p_104717_,
        double p_104718_,
        double p_104719_,
        double p_104720_,
        double p_104721_
    )
    {
        this.levelRenderer.addParticle(p_104714_, p_104714_.getType().getOverrideLimiter() || p_104715_, p_104716_, p_104717_, p_104718_, p_104719_, p_104720_, p_104721_);
    }

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions p_104766_, double p_104767_, double p_104768_, double p_104769_, double p_104770_, double p_104771_, double p_104772_)
    {
        this.levelRenderer.addParticle(p_104766_, false, true, p_104767_, p_104768_, p_104769_, p_104770_, p_104771_, p_104772_);
    }

    @Override
    public void addAlwaysVisibleParticle(
        ParticleOptions p_104774_,
        boolean p_104775_,
        double p_104776_,
        double p_104777_,
        double p_104778_,
        double p_104779_,
        double p_104780_,
        double p_104781_
    )
    {
        this.levelRenderer
        .addParticle(p_104774_, p_104774_.getType().getOverrideLimiter() || p_104775_, true, p_104776_, p_104777_, p_104778_, p_104779_, p_104780_, p_104781_);
    }

    @Override
    public List<AbstractClientPlayer> players()
    {
        return this.players;
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int p_205516_, int p_205517_, int p_205518_)
    {
        return this.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
    }

    public float getSkyDarken(float p_104806_)
    {
        float f = this.getTimeOfDay(p_104806_);
        float f1 = 1.0F - (Mth.cos(f * (float)(Math.PI * 2)) * 2.0F + 0.2F);
        f1 = Mth.clamp(f1, 0.0F, 1.0F);
        f1 = 1.0F - f1;
        f1 *= 1.0F - this.getRainLevel(p_104806_) * 5.0F / 16.0F;
        f1 *= 1.0F - this.getThunderLevel(p_104806_) * 5.0F / 16.0F;
        return f1 * 0.8F + 0.2F;
    }

    public Vec3 getSkyColor(Vec3 p_171661_, float p_171662_)
    {
        float f = this.getTimeOfDay(p_171662_);
        Vec3 vec3 = p_171661_.subtract(2.0, 2.0, 2.0).scale(0.25);
        BiomeManager biomemanager = this.getBiomeManager();
        Vec3M vec3m = new Vec3M(0.0, 0.0, 0.0);
        Vec3 vec31 = CubicSampler.sampleM(vec3, (xIn, yIn, zIn) -> vec3m.fromRgbM(biomemanager.getNoiseBiomeAtQuart(xIn, yIn, zIn).value().getSkyColor()));
        float f1 = Mth.cos(f * (float)(Math.PI * 2)) * 2.0F + 0.5F;
        f1 = Mth.clamp(f1, 0.0F, 1.0F);
        float f2 = (float)vec31.x * f1;
        float f3 = (float)vec31.y * f1;
        float f4 = (float)vec31.z * f1;
        float f5 = this.getRainLevel(p_171662_);

        if (f5 > 0.0F)
        {
            float f6 = (f2 * 0.3F + f3 * 0.59F + f4 * 0.11F) * 0.6F;
            float f7 = 1.0F - f5 * 0.75F;
            f2 = f2 * f7 + f6 * (1.0F - f7);
            f3 = f3 * f7 + f6 * (1.0F - f7);
            f4 = f4 * f7 + f6 * (1.0F - f7);
        }

        float f9 = this.getThunderLevel(p_171662_);

        if (f9 > 0.0F)
        {
            float f10 = (f2 * 0.3F + f3 * 0.59F + f4 * 0.11F) * 0.2F;
            float f8 = 1.0F - f9 * 0.75F;
            f2 = f2 * f8 + f10 * (1.0F - f8);
            f3 = f3 * f8 + f10 * (1.0F - f8);
            f4 = f4 * f8 + f10 * (1.0F - f8);
        }

        int i = this.getSkyFlashTime();

        if (i > 0)
        {
            float f11 = (float)i - p_171662_;

            if (f11 > 1.0F)
            {
                f11 = 1.0F;
            }

            f11 *= 0.45F;
            f2 = f2 * (1.0F - f11) + 0.8F * f11;
            f3 = f3 * (1.0F - f11) + 0.8F * f11;
            f4 = f4 * (1.0F - f11) + 1.0F * f11;
        }

        return new Vec3((double)f2, (double)f3, (double)f4);
    }

    public Vec3 getCloudColor(float p_104809_)
    {
        float f = this.getTimeOfDay(p_104809_);
        float f1 = Mth.cos(f * (float)(Math.PI * 2)) * 2.0F + 0.5F;
        f1 = Mth.clamp(f1, 0.0F, 1.0F);
        float f2 = 1.0F;
        float f3 = 1.0F;
        float f4 = 1.0F;
        float f5 = this.getRainLevel(p_104809_);

        if (f5 > 0.0F)
        {
            float f6 = (f2 * 0.3F + f3 * 0.59F + f4 * 0.11F) * 0.6F;
            float f7 = 1.0F - f5 * 0.95F;
            f2 = f2 * f7 + f6 * (1.0F - f7);
            f3 = f3 * f7 + f6 * (1.0F - f7);
            f4 = f4 * f7 + f6 * (1.0F - f7);
        }

        f2 *= f1 * 0.9F + 0.1F;
        f3 *= f1 * 0.9F + 0.1F;
        f4 *= f1 * 0.85F + 0.15F;
        float f9 = this.getThunderLevel(p_104809_);

        if (f9 > 0.0F)
        {
            float f10 = (f2 * 0.3F + f3 * 0.59F + f4 * 0.11F) * 0.2F;
            float f8 = 1.0F - f9 * 0.95F;
            f2 = f2 * f8 + f10 * (1.0F - f8);
            f3 = f3 * f8 + f10 * (1.0F - f8);
            f4 = f4 * f8 + f10 * (1.0F - f8);
        }

        return new Vec3((double)f2, (double)f3, (double)f4);
    }

    public float getStarBrightness(float p_104812_)
    {
        float f = this.getTimeOfDay(p_104812_);
        float f1 = 1.0F - (Mth.cos(f * (float)(Math.PI * 2)) * 2.0F + 0.25F);
        f1 = Mth.clamp(f1, 0.0F, 1.0F);
        return f1 * f1 * 0.5F;
    }

    public int getSkyFlashTime()
    {
        return this.minecraft.options.hideLightningFlash().get() ? 0 : this.skyFlashTime;
    }

    @Override
    public void setSkyFlashTime(int p_104783_)
    {
        this.skyFlashTime = p_104783_;
    }

    @Override
    public float getShade(Direction p_104703_, boolean p_104704_)
    {
        boolean flag = this.effects().constantAmbientLight();
        boolean flag1 = Config.isShaders();

        if (!p_104704_)
        {
            return flag ? 0.9F : 1.0F;
        }
        else
        {
            switch (p_104703_)
            {
                case DOWN:
                    return flag ? 0.9F : (flag1 ? Shaders.blockLightLevel05 : 0.5F);

                case UP:
                    return flag ? 0.9F : 1.0F;

                case NORTH:
                case SOUTH:
                    if (Config.isShaders())
                    {
                        return Shaders.blockLightLevel08;
                    }

                    return 0.8F;

                case WEST:
                case EAST:
                    if (Config.isShaders())
                    {
                        return Shaders.blockLightLevel06;
                    }

                    return 0.6F;

                default:
                    return 1.0F;
            }
        }
    }

    @Override
    public int getBlockTint(BlockPos p_104700_, ColorResolver p_104701_)
    {
        BlockTintCache blocktintcache = this.tintCaches.get(p_104701_);
        return blocktintcache.getColor(p_104700_);
    }

    public int calculateBlockTint(BlockPos p_104763_, ColorResolver p_104764_)
    {
        int i = Minecraft.getInstance().options.biomeBlendRadius().get();

        if (i == 0)
        {
            return p_104764_.getColor(
                       CustomColors.fixBiome(this.getBiome(p_104763_).value()), (double)p_104763_.getX(), (double)p_104763_.getZ()
                   );
        }
        else
        {
            int j = (i * 2 + 1) * (i * 2 + 1);
            int k = 0;
            int l = 0;
            int i1 = 0;
            Cursor3D cursor3d = new Cursor3D(
                p_104763_.getX() - i,
                p_104763_.getY(),
                p_104763_.getZ() - i,
                p_104763_.getX() + i,
                p_104763_.getY(),
                p_104763_.getZ() + i
            );
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            while (cursor3d.advance())
            {
                blockpos$mutableblockpos.set(cursor3d.nextX(), cursor3d.nextY(), cursor3d.nextZ());
                int j1 = p_104764_.getColor(
                             CustomColors.fixBiome(this.getBiome(blockpos$mutableblockpos).value()),
                             (double)blockpos$mutableblockpos.getX(),
                             (double)blockpos$mutableblockpos.getZ()
                         );
                k += (j1 & 0xFF0000) >> 16;
                l += (j1 & 0xFF00) >> 8;
                i1 += j1 & 0xFF;
            }

            return (k / j & 0xFF) << 16 | (l / j & 0xFF) << 8 | i1 / j & 0xFF;
        }
    }

    public void setDefaultSpawnPos(BlockPos p_104753_, float p_104754_)
    {
        this.levelData.setSpawn(p_104753_, p_104754_);
    }

    @Override
    public String toString()
    {
        return "ClientLevel";
    }

    public ClientLevel.ClientLevelData getLevelData()
    {
        return this.clientLevelData;
    }

    @Override
    public void gameEvent(Holder<GameEvent> p_333083_, Vec3 p_233640_, GameEvent.Context p_233641_)
    {
    }

    protected Map<MapId, MapItemSavedData> getAllMapData()
    {
        return ImmutableMap.copyOf(this.mapData);
    }

    protected void addMapData(Map<MapId, MapItemSavedData> p_171673_)
    {
        this.mapData.putAll(p_171673_);
    }

    @Override
    protected LevelEntityGetter<Entity> getEntities()
    {
        return this.entityStorage.getEntityGetter();
    }

    @Override
    public String gatherChunkSourceStats()
    {
        return "Chunks[C] W: " + this.chunkSource.gatherStats() + " E: " + this.entityStorage.gatherStats();
    }

    @Override
    public void addDestroyBlockEffect(BlockPos p_171667_, BlockState p_171668_)
    {
        this.minecraft.particleEngine.destroy(p_171667_, p_171668_);
    }

    public void setServerSimulationDistance(int p_194175_)
    {
        this.serverSimulationDistance = p_194175_;
    }

    public int getServerSimulationDistance()
    {
        return this.serverSimulationDistance;
    }

    @Override
    public FeatureFlagSet enabledFeatures()
    {
        return this.connection.enabledFeatures();
    }

    @Override
    public PotionBrewing potionBrewing()
    {
        return this.connection.potionBrewing();
    }

    public TransientEntitySectionManager getEntityStorage()
    {
        return this.entityStorage;
    }

    public EntitySectionStorage getSectionStorage()
    {
        return EntitySection.getSectionStorage(this.entityStorage);
    }

    public Collection < PartEntity<? >> getPartEntities()
    {
        return this.partEntities.values();
    }

    public ModelDataManager getModelDataManager()
    {
        return this.modelDataManager;
    }

    public float getShade(float normalX, float normalY, float normalZ, boolean shade)
    {
        boolean flag = this.effects().constantAmbientLight();

        if (!shade)
        {
            return flag ? 0.9F : 1.0F;
        }
        else
        {
            return QuadLighter.calculateShade(normalX, normalY, normalZ, flag);
        }
    }

    public static class ClientLevelData implements WritableLevelData
    {
        private final boolean hardcore;
        private final GameRules gameRules;
        private final boolean isFlat;
        private BlockPos spawnPos;
        private float spawnAngle;
        private long gameTime;
        private long dayTime;
        private boolean raining;
        private Difficulty difficulty;
        private boolean difficultyLocked;

        public ClientLevelData(Difficulty p_104843_, boolean p_104844_, boolean p_104845_)
        {
            this.difficulty = p_104843_;
            this.hardcore = p_104844_;
            this.isFlat = p_104845_;
            this.gameRules = new GameRules();
        }

        @Override
        public BlockPos getSpawnPos()
        {
            return this.spawnPos;
        }

        @Override
        public float getSpawnAngle()
        {
            return this.spawnAngle;
        }

        @Override
        public long getGameTime()
        {
            return this.gameTime;
        }

        @Override
        public long getDayTime()
        {
            return this.dayTime;
        }

        public void setGameTime(long p_104850_)
        {
            this.gameTime = p_104850_;
        }

        public void setDayTime(long p_104864_)
        {
            this.dayTime = p_104864_;
        }

        @Override
        public void setSpawn(BlockPos p_104854_, float p_104855_)
        {
            this.spawnPos = p_104854_.immutable();
            this.spawnAngle = p_104855_;
        }

        @Override
        public boolean isThundering()
        {
            return false;
        }

        @Override
        public boolean isRaining()
        {
            return this.raining;
        }

        @Override
        public void setRaining(boolean p_104866_)
        {
            this.raining = p_104866_;
        }

        @Override
        public boolean isHardcore()
        {
            return this.hardcore;
        }

        @Override
        public GameRules getGameRules()
        {
            return this.gameRules;
        }

        @Override
        public Difficulty getDifficulty()
        {
            return this.difficulty;
        }

        @Override
        public boolean isDifficultyLocked()
        {
            return this.difficultyLocked;
        }

        @Override
        public void fillCrashReportCategory(CrashReportCategory p_171690_, LevelHeightAccessor p_171691_)
        {
            WritableLevelData.super.fillCrashReportCategory(p_171690_, p_171691_);
        }

        public void setDifficulty(Difficulty p_104852_)
        {
            Reflector.ForgeEventFactory_onDifficultyChange.callVoid(p_104852_, this.difficulty);
            this.difficulty = p_104852_;
        }

        public void setDifficultyLocked(boolean p_104859_)
        {
            this.difficultyLocked = p_104859_;
        }

        public double getHorizonHeight(LevelHeightAccessor p_171688_)
        {
            return this.isFlat ? (double)p_171688_.getMinBuildHeight() : 63.0;
        }

        public float getClearColorScale()
        {
            return this.isFlat ? 1.0F : 0.03125F;
        }
    }

    final class EntityCallbacks implements LevelCallback<Entity>
    {
        public void onCreated(Entity p_171696_)
        {
        }

        public void onDestroyed(Entity p_171700_)
        {
        }

        public void onTickingStart(Entity p_171704_)
        {
            ClientLevel.this.tickingEntities.add(p_171704_);
        }

        public void onTickingEnd(Entity p_171708_)
        {
            ClientLevel.this.tickingEntities.remove(p_171708_);
        }

        public void onTrackingStart(Entity p_171712_)
        {
            if (p_171712_ instanceof AbstractClientPlayer)
            {
                ClientLevel.this.players.add((AbstractClientPlayer)p_171712_);
            }

            if (Reflector.IForgeEntity_isMultipartEntity.exists() && Reflector.IForgeEntity_getParts.exists())
            {
                boolean flag = Reflector.callBoolean(p_171712_, Reflector.IForgeEntity_isMultipartEntity);

                if (flag)
                {
                    PartEntity[] apartentity = (PartEntity[])Reflector.call(p_171712_, Reflector.IForgeEntity_getParts);

                    for (PartEntity partentity : apartentity)
                    {
                        ClientLevel.this.partEntities.put(partentity.getId(), partentity);
                    }
                }
            }
        }

        public void onTrackingEnd(Entity p_171716_)
        {
            p_171716_.unRide();
            ClientLevel.this.players.remove(p_171716_);

            if (Reflector.IForgeEntity_onRemovedFromWorld.exists())
            {
                Reflector.call(p_171716_, Reflector.IForgeEntity_onRemovedFromWorld);
            }

            if (Reflector.ForgeEventFactory_onEntityLeaveLevel.exists())
            {
                Reflector.ForgeEventFactory_onEntityLeaveLevel.callVoid(p_171716_, ClientLevel.this);
            }

            if (Reflector.IForgeEntity_isMultipartEntity.exists() && Reflector.IForgeEntity_getParts.exists())
            {
                boolean flag = Reflector.callBoolean(p_171716_, Reflector.IForgeEntity_isMultipartEntity);

                if (flag)
                {
                    PartEntity[] apartentity = (PartEntity[])Reflector.call(p_171716_, Reflector.IForgeEntity_getParts);

                    for (PartEntity partentity : apartentity)
                    {
                        ClientLevel.this.partEntities.remove(partentity.getId(), partentity);
                    }
                }
            }

            ClientLevel.this.onEntityRemoved(p_171716_);
        }

        public void onSectionChange(Entity p_233660_)
        {
        }
    }
}
