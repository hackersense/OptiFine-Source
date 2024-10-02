package net.minecraft.world.level.storage;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.DataResult.Error;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.timers.TimerCallbacks;
import net.minecraft.world.level.timers.TimerQueue;
import org.slf4j.Logger;

public class PrimaryLevelData implements ServerLevelData, WorldData
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String LEVEL_NAME = "LevelName";
    protected static final String PLAYER = "Player";
    protected static final String WORLD_GEN_SETTINGS = "WorldGenSettings";
    private LevelSettings settings;
    private final WorldOptions worldOptions;
    private final PrimaryLevelData.SpecialWorldProperty specialWorldProperty;
    private final Lifecycle worldGenSettingsLifecycle;
    private BlockPos spawnPos;
    private float spawnAngle;
    private long gameTime;
    private long dayTime;
    @Nullable
    private final CompoundTag loadedPlayerTag;
    private final int version;
    private int clearWeatherTime;
    private boolean raining;
    private int rainTime;
    private boolean thundering;
    private int thunderTime;
    private boolean initialized;
    private boolean difficultyLocked;
    private WorldBorder.Settings worldBorder;
    private EndDragonFight.Data endDragonFightData;
    @Nullable
    private CompoundTag customBossEvents;
    private int wanderingTraderSpawnDelay;
    private int wanderingTraderSpawnChance;
    @Nullable
    private UUID wanderingTraderId;
    private final Set<String> knownServerBrands;
    private boolean wasModded;
    private final Set<String> removedFeatureFlags;
    private final TimerQueue<MinecraftServer> scheduledEvents;

    private PrimaryLevelData(
        @Nullable CompoundTag p_277888_,
        boolean p_278109_,
        BlockPos p_328735_,
        float p_277542_,
        long p_277414_,
        long p_277635_,
        int p_277672_,
        int p_277714_,
        int p_278088_,
        boolean p_277943_,
        int p_278037_,
        boolean p_277644_,
        boolean p_277749_,
        boolean p_278004_,
        WorldBorder.Settings p_277729_,
        int p_277595_,
        int p_277794_,
        @Nullable UUID p_277341_,
        Set<String> p_277989_,
        Set<String> p_277399_,
        TimerQueue<MinecraftServer> p_277860_,
        @Nullable CompoundTag p_277936_,
        EndDragonFight.Data p_289764_,
        LevelSettings p_278064_,
        WorldOptions p_278072_,
        PrimaryLevelData.SpecialWorldProperty p_277548_,
        Lifecycle p_277915_
    )
    {
        this.wasModded = p_278109_;
        this.spawnPos = p_328735_;
        this.spawnAngle = p_277542_;
        this.gameTime = p_277414_;
        this.dayTime = p_277635_;
        this.version = p_277672_;
        this.clearWeatherTime = p_277714_;
        this.rainTime = p_278088_;
        this.raining = p_277943_;
        this.thunderTime = p_278037_;
        this.thundering = p_277644_;
        this.initialized = p_277749_;
        this.difficultyLocked = p_278004_;
        this.worldBorder = p_277729_;
        this.wanderingTraderSpawnDelay = p_277595_;
        this.wanderingTraderSpawnChance = p_277794_;
        this.wanderingTraderId = p_277341_;
        this.knownServerBrands = p_277989_;
        this.removedFeatureFlags = p_277399_;
        this.loadedPlayerTag = p_277888_;
        this.scheduledEvents = p_277860_;
        this.customBossEvents = p_277936_;
        this.endDragonFightData = p_289764_;
        this.settings = p_278064_;
        this.worldOptions = p_278072_;
        this.specialWorldProperty = p_277548_;
        this.worldGenSettingsLifecycle = p_277915_;
    }

    public PrimaryLevelData(LevelSettings p_251081_, WorldOptions p_251666_, PrimaryLevelData.SpecialWorldProperty p_252268_, Lifecycle p_251714_)
    {
        this(
            null,
            false,
            BlockPos.ZERO,
            0.0F,
            0L,
            0L,
            19133,
            0,
            0,
            false,
            0,
            false,
            false,
            false,
            WorldBorder.DEFAULT_SETTINGS,
            0,
            0,
            null,
            Sets.newLinkedHashSet(),
            new HashSet<>(),
            new TimerQueue<>(TimerCallbacks.SERVER_CALLBACKS),
            null,
            EndDragonFight.Data.DEFAULT,
            p_251081_.copy(),
            p_251666_,
            p_252268_,
            p_251714_
        );
    }

    public static <T> PrimaryLevelData parse(
        Dynamic<T> p_78531_, LevelSettings p_78535_, PrimaryLevelData.SpecialWorldProperty p_250651_, WorldOptions p_251864_, Lifecycle p_78538_
    )
    {
        long i = p_78531_.get("Time").asLong(0L);
        return new PrimaryLevelData(
                   p_78531_.get("Player").flatMap(CompoundTag.CODEC::parse).result().orElse(null),
                   p_78531_.get("WasModded").asBoolean(false),
                   new BlockPos(p_78531_.get("SpawnX").asInt(0), p_78531_.get("SpawnY").asInt(0), p_78531_.get("SpawnZ").asInt(0)),
                   p_78531_.get("SpawnAngle").asFloat(0.0F),
                   i,
                   p_78531_.get("DayTime").asLong(i),
                   LevelVersion.parse(p_78531_).levelDataVersion(),
                   p_78531_.get("clearWeatherTime").asInt(0),
                   p_78531_.get("rainTime").asInt(0),
                   p_78531_.get("raining").asBoolean(false),
                   p_78531_.get("thunderTime").asInt(0),
                   p_78531_.get("thundering").asBoolean(false),
                   p_78531_.get("initialized").asBoolean(true),
                   p_78531_.get("DifficultyLocked").asBoolean(false),
                   WorldBorder.Settings.read(p_78531_, WorldBorder.DEFAULT_SETTINGS),
                   p_78531_.get("WanderingTraderSpawnDelay").asInt(0),
                   p_78531_.get("WanderingTraderSpawnChance").asInt(0),
                   p_78531_.get("WanderingTraderId").read(UUIDUtil.CODEC).result().orElse(null),
                   p_78531_.get("ServerBrands")
                   .asStream()
                   .flatMap(p_327546_ -> p_327546_.asString().result().stream())
                   .collect(Collectors.toCollection(Sets::newLinkedHashSet)),
                   p_78531_.get("removed_features").asStream().flatMap(p_327544_ -> p_327544_.asString().result().stream()).collect(Collectors.toSet()),
                   new TimerQueue<>(TimerCallbacks.SERVER_CALLBACKS, p_78531_.get("ScheduledEvents").asStream()),
                   (CompoundTag)p_78531_.get("CustomBossEvents").orElseEmptyMap().getValue(),
                   p_78531_.get("DragonFight").read(EndDragonFight.Data.CODEC).resultOrPartial(LOGGER::error).orElse(EndDragonFight.Data.DEFAULT),
                   p_78535_,
                   p_251864_,
                   p_250651_,
                   p_78538_
               );
    }

    @Override
    public CompoundTag createTag(RegistryAccess p_78543_, @Nullable CompoundTag p_78544_)
    {
        if (p_78544_ == null)
        {
            p_78544_ = this.loadedPlayerTag;
        }

        CompoundTag compoundtag = new CompoundTag();
        this.setTagData(p_78543_, compoundtag, p_78544_);
        return compoundtag;
    }

    private void setTagData(RegistryAccess p_78546_, CompoundTag p_78547_, @Nullable CompoundTag p_78548_)
    {
        p_78547_.put("ServerBrands", stringCollectionToTag(this.knownServerBrands));
        p_78547_.putBoolean("WasModded", this.wasModded);

        if (!this.removedFeatureFlags.isEmpty())
        {
            p_78547_.put("removed_features", stringCollectionToTag(this.removedFeatureFlags));
        }

        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("Name", SharedConstants.getCurrentVersion().getName());
        compoundtag.putInt("Id", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        compoundtag.putBoolean("Snapshot", !SharedConstants.getCurrentVersion().isStable());
        compoundtag.putString("Series", SharedConstants.getCurrentVersion().getDataVersion().getSeries());
        p_78547_.put("Version", compoundtag);
        NbtUtils.addCurrentDataVersion(p_78547_);
        DynamicOps<Tag> dynamicops = p_78546_.createSerializationContext(NbtOps.INSTANCE);
        WorldGenSettings.encode(dynamicops, this.worldOptions, p_78546_)
        .resultOrPartial(Util.prefix("WorldGenSettings: ", LOGGER::error))
        .ifPresent(p_78574_ -> p_78547_.put("WorldGenSettings", p_78574_));
        p_78547_.putInt("GameType", this.settings.gameType().getId());
        p_78547_.putInt("SpawnX", this.spawnPos.getX());
        p_78547_.putInt("SpawnY", this.spawnPos.getY());
        p_78547_.putInt("SpawnZ", this.spawnPos.getZ());
        p_78547_.putFloat("SpawnAngle", this.spawnAngle);
        p_78547_.putLong("Time", this.gameTime);
        p_78547_.putLong("DayTime", this.dayTime);
        p_78547_.putLong("LastPlayed", Util.getEpochMillis());
        p_78547_.putString("LevelName", this.settings.levelName());
        p_78547_.putInt("version", 19133);
        p_78547_.putInt("clearWeatherTime", this.clearWeatherTime);
        p_78547_.putInt("rainTime", this.rainTime);
        p_78547_.putBoolean("raining", this.raining);
        p_78547_.putInt("thunderTime", this.thunderTime);
        p_78547_.putBoolean("thundering", this.thundering);
        p_78547_.putBoolean("hardcore", this.settings.hardcore());
        p_78547_.putBoolean("allowCommands", this.settings.allowCommands());
        p_78547_.putBoolean("initialized", this.initialized);
        this.worldBorder.write(p_78547_);
        p_78547_.putByte("Difficulty", (byte)this.settings.difficulty().getId());
        p_78547_.putBoolean("DifficultyLocked", this.difficultyLocked);
        p_78547_.put("GameRules", this.settings.gameRules().createTag());
        p_78547_.put("DragonFight", EndDragonFight.Data.CODEC.encodeStart(NbtOps.INSTANCE, this.endDragonFightData).getOrThrow());

        if (p_78548_ != null)
        {
            p_78547_.put("Player", p_78548_);
        }

        WorldDataConfiguration.CODEC
        .encodeStart(NbtOps.INSTANCE, this.settings.getDataConfiguration())
        .ifSuccess(p_248505_ -> p_78547_.merge((CompoundTag)p_248505_))
        .ifError(p_327545_ -> LOGGER.warn("Failed to encode configuration {}", p_327545_.message()));

        if (this.customBossEvents != null)
        {
            p_78547_.put("CustomBossEvents", this.customBossEvents);
        }

        p_78547_.put("ScheduledEvents", this.scheduledEvents.store());
        p_78547_.putInt("WanderingTraderSpawnDelay", this.wanderingTraderSpawnDelay);
        p_78547_.putInt("WanderingTraderSpawnChance", this.wanderingTraderSpawnChance);

        if (this.wanderingTraderId != null)
        {
            p_78547_.putUUID("WanderingTraderId", this.wanderingTraderId);
        }
    }

    private static ListTag stringCollectionToTag(Set<String> p_277880_)
    {
        ListTag listtag = new ListTag();
        p_277880_.stream().map(StringTag::valueOf).forEach(listtag::add);
        return listtag;
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

    @Nullable
    @Override
    public CompoundTag getLoadedPlayerTag()
    {
        return this.loadedPlayerTag;
    }

    @Override
    public void setGameTime(long p_78519_)
    {
        this.gameTime = p_78519_;
    }

    @Override
    public void setDayTime(long p_78567_)
    {
        this.dayTime = p_78567_;
    }

    @Override
    public void setSpawn(BlockPos p_78540_, float p_78541_)
    {
        this.spawnPos = p_78540_.immutable();
        this.spawnAngle = p_78541_;
    }

    @Override
    public String getLevelName()
    {
        return this.settings.levelName();
    }

    @Override
    public int getVersion()
    {
        return this.version;
    }

    @Override
    public int getClearWeatherTime()
    {
        return this.clearWeatherTime;
    }

    @Override
    public void setClearWeatherTime(int p_78517_)
    {
        this.clearWeatherTime = p_78517_;
    }

    @Override
    public boolean isThundering()
    {
        return this.thundering;
    }

    @Override
    public void setThundering(boolean p_78562_)
    {
        this.thundering = p_78562_;
    }

    @Override
    public int getThunderTime()
    {
        return this.thunderTime;
    }

    @Override
    public void setThunderTime(int p_78589_)
    {
        this.thunderTime = p_78589_;
    }

    @Override
    public boolean isRaining()
    {
        return this.raining;
    }

    @Override
    public void setRaining(boolean p_78576_)
    {
        this.raining = p_78576_;
    }

    @Override
    public int getRainTime()
    {
        return this.rainTime;
    }

    @Override
    public void setRainTime(int p_78592_)
    {
        this.rainTime = p_78592_;
    }

    @Override
    public GameType getGameType()
    {
        return this.settings.gameType();
    }

    @Override
    public void setGameType(GameType p_78525_)
    {
        this.settings = this.settings.withGameType(p_78525_);
    }

    @Override
    public boolean isHardcore()
    {
        return this.settings.hardcore();
    }

    @Override
    public boolean isAllowCommands()
    {
        return this.settings.allowCommands();
    }

    @Override
    public boolean isInitialized()
    {
        return this.initialized;
    }

    @Override
    public void setInitialized(boolean p_78581_)
    {
        this.initialized = p_78581_;
    }

    @Override
    public GameRules getGameRules()
    {
        return this.settings.gameRules();
    }

    @Override
    public WorldBorder.Settings getWorldBorder()
    {
        return this.worldBorder;
    }

    @Override
    public void setWorldBorder(WorldBorder.Settings p_78527_)
    {
        this.worldBorder = p_78527_;
    }

    @Override
    public Difficulty getDifficulty()
    {
        return this.settings.difficulty();
    }

    @Override
    public void setDifficulty(Difficulty p_78521_)
    {
        this.settings = this.settings.withDifficulty(p_78521_);
    }

    @Override
    public boolean isDifficultyLocked()
    {
        return this.difficultyLocked;
    }

    @Override
    public void setDifficultyLocked(boolean p_78586_)
    {
        this.difficultyLocked = p_78586_;
    }

    @Override
    public TimerQueue<MinecraftServer> getScheduledEvents()
    {
        return this.scheduledEvents;
    }

    @Override
    public void fillCrashReportCategory(CrashReportCategory p_164972_, LevelHeightAccessor p_164973_)
    {
        ServerLevelData.super.fillCrashReportCategory(p_164972_, p_164973_);
        WorldData.super.fillCrashReportCategory(p_164972_);
    }

    @Override
    public WorldOptions worldGenOptions()
    {
        return this.worldOptions;
    }

    @Override
    public boolean isFlatWorld()
    {
        return this.specialWorldProperty == PrimaryLevelData.SpecialWorldProperty.FLAT;
    }

    @Override
    public boolean isDebugWorld()
    {
        return this.specialWorldProperty == PrimaryLevelData.SpecialWorldProperty.DEBUG;
    }

    @Override
    public Lifecycle worldGenSettingsLifecycle()
    {
        return this.worldGenSettingsLifecycle;
    }

    @Override
    public EndDragonFight.Data endDragonFightData()
    {
        return this.endDragonFightData;
    }

    @Override
    public void setEndDragonFightData(EndDragonFight.Data p_289770_)
    {
        this.endDragonFightData = p_289770_;
    }

    @Override
    public WorldDataConfiguration getDataConfiguration()
    {
        return this.settings.getDataConfiguration();
    }

    @Override
    public void setDataConfiguration(WorldDataConfiguration p_252328_)
    {
        this.settings = this.settings.withDataConfiguration(p_252328_);
    }

    @Nullable
    @Override
    public CompoundTag getCustomBossEvents()
    {
        return this.customBossEvents;
    }

    @Override
    public void setCustomBossEvents(@Nullable CompoundTag p_78571_)
    {
        this.customBossEvents = p_78571_;
    }

    @Override
    public int getWanderingTraderSpawnDelay()
    {
        return this.wanderingTraderSpawnDelay;
    }

    @Override
    public void setWanderingTraderSpawnDelay(int p_78595_)
    {
        this.wanderingTraderSpawnDelay = p_78595_;
    }

    @Override
    public int getWanderingTraderSpawnChance()
    {
        return this.wanderingTraderSpawnChance;
    }

    @Override
    public void setWanderingTraderSpawnChance(int p_78598_)
    {
        this.wanderingTraderSpawnChance = p_78598_;
    }

    @Nullable
    @Override
    public UUID getWanderingTraderId()
    {
        return this.wanderingTraderId;
    }

    @Override
    public void setWanderingTraderId(UUID p_78553_)
    {
        this.wanderingTraderId = p_78553_;
    }

    @Override
    public void setModdedInfo(String p_78550_, boolean p_78551_)
    {
        this.knownServerBrands.add(p_78550_);
        this.wasModded |= p_78551_;
    }

    @Override
    public boolean wasModded()
    {
        return this.wasModded;
    }

    @Override
    public Set<String> getKnownServerBrands()
    {
        return ImmutableSet.copyOf(this.knownServerBrands);
    }

    @Override
    public Set<String> getRemovedFeatureFlags()
    {
        return Set.copyOf(this.removedFeatureFlags);
    }

    @Override
    public ServerLevelData overworldData()
    {
        return this;
    }

    @Override
    public LevelSettings getLevelSettings()
    {
        return this.settings.copy();
    }

    @Deprecated
    public static enum SpecialWorldProperty
    {
        NONE,
        FLAT,
        DEBUG;
    }
}
