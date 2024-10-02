package net.minecraft.util.worldupdate;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Reference2FloatMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatMaps;
import it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RecreatingChunkStorage;
import net.minecraft.world.level.chunk.storage.RecreatingSimpleRegionStorage;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;

public class WorldUpgrader
{
    static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder().setDaemon(true).build();
    private static final String NEW_DIRECTORY_PREFIX = "new_";
    static final MutableComponent STATUS_UPGRADING_POI = Component.translatable("optimizeWorld.stage.upgrading.poi");
    static final MutableComponent STATUS_FINISHED_POI = Component.translatable("optimizeWorld.stage.finished.poi");
    static final MutableComponent STATUS_UPGRADING_ENTITIES = Component.translatable("optimizeWorld.stage.upgrading.entities");
    static final MutableComponent STATUS_FINISHED_ENTITIES = Component.translatable("optimizeWorld.stage.finished.entities");
    static final MutableComponent STATUS_UPGRADING_CHUNKS = Component.translatable("optimizeWorld.stage.upgrading.chunks");
    static final MutableComponent STATUS_FINISHED_CHUNKS = Component.translatable("optimizeWorld.stage.finished.chunks");
    final Registry<LevelStem> dimensions;
    final Set<ResourceKey<Level>> levels;
    final boolean eraseCache;
    final boolean recreateRegionFiles;
    final LevelStorageSource.LevelStorageAccess levelStorage;
    private final Thread thread;
    final DataFixer dataFixer;
    volatile boolean running = true;
    private volatile boolean finished;
    volatile float progress;
    volatile int totalChunks;
    volatile int totalFiles;
    volatile int converted;
    volatile int skipped;
    final Reference2FloatMap<ResourceKey<Level>> progressMap = Reference2FloatMaps.synchronize(new Reference2FloatOpenHashMap<>());
    volatile Component status = Component.translatable("optimizeWorld.stage.counting");
    static final Pattern REGEX = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");
    final DimensionDataStorage overworldDataStorage;

    public WorldUpgrader(LevelStorageSource.LevelStorageAccess p_249922_, DataFixer p_250273_, RegistryAccess p_334652_, boolean p_250738_, boolean p_335488_)
    {
        this.dimensions = p_334652_.registryOrThrow(Registries.LEVEL_STEM);
        this.levels = this.dimensions.registryKeySet().stream().map(Registries::levelStemToLevel).collect(Collectors.toUnmodifiableSet());
        this.eraseCache = p_250738_;
        this.dataFixer = p_250273_;
        this.levelStorage = p_249922_;
        this.overworldDataStorage = new DimensionDataStorage(this.levelStorage.getDimensionPath(Level.OVERWORLD).resolve("data").toFile(), p_250273_, p_334652_);
        this.recreateRegionFiles = p_335488_;
        this.thread = THREAD_FACTORY.newThread(this::work);
        this.thread.setUncaughtExceptionHandler((p_18825_, p_18826_) ->
        {
            LOGGER.error("Error upgrading world", p_18826_);
            this.status = Component.translatable("optimizeWorld.stage.failed");
            this.finished = true;
        });
        this.thread.start();
    }

    public void cancel()
    {
        this.running = false;

        try
        {
            this.thread.join();
        }
        catch (InterruptedException interruptedexception)
        {
        }
    }

    private void work()
    {
        long i = Util.getMillis();
        LOGGER.info("Upgrading entities");
        new WorldUpgrader.EntityUpgrader().upgrade();
        LOGGER.info("Upgrading POIs");
        new WorldUpgrader.PoiUpgrader().upgrade();
        LOGGER.info("Upgrading blocks");
        new WorldUpgrader.ChunkUpgrader().upgrade();
        this.overworldDataStorage.save();
        i = Util.getMillis() - i;
        LOGGER.info("World optimizaton finished after {} seconds", i / 1000L);
        this.finished = true;
    }

    public boolean isFinished()
    {
        return this.finished;
    }

    public Set<ResourceKey<Level>> levels()
    {
        return this.levels;
    }

    public float dimensionProgress(ResourceKey<Level> p_18828_)
    {
        return this.progressMap.getFloat(p_18828_);
    }

    public float getProgress()
    {
        return this.progress;
    }

    public int getTotalChunks()
    {
        return this.totalChunks;
    }

    public int getConverted()
    {
        return this.converted;
    }

    public int getSkipped()
    {
        return this.skipped;
    }

    public Component getStatus()
    {
        return this.status;
    }

    static Path resolveRecreateDirectory(Path p_330107_)
    {
        return p_330107_.resolveSibling("new_" + p_330107_.getFileName().toString());
    }

    abstract class AbstractUpgrader<T extends AutoCloseable>
    {
        private final MutableComponent upgradingStatus;
        private final MutableComponent finishedStatus;
        private final String type;
        private final String folderName;
        @Nullable
        protected CompletableFuture<Void> previousWriteFuture;
        protected final DataFixTypes dataFixType;

        AbstractUpgrader(
            final DataFixTypes p_332379_, final String p_334432_, final String p_334138_, final MutableComponent p_332782_, final MutableComponent p_331966_
        )
        {
            this.dataFixType = p_332379_;
            this.type = p_334432_;
            this.folderName = p_334138_;
            this.upgradingStatus = p_332782_;
            this.finishedStatus = p_331966_;
        }

        public void upgrade()
        {
            WorldUpgrader.this.totalFiles = 0;
            WorldUpgrader.this.totalChunks = 0;
            WorldUpgrader.this.converted = 0;
            WorldUpgrader.this.skipped = 0;
            List<WorldUpgrader.DimensionToUpgrade<T>> list = this.getDimensionsToUpgrade();

            if (WorldUpgrader.this.totalChunks != 0)
            {
                float f = (float)WorldUpgrader.this.totalFiles;
                WorldUpgrader.this.status = this.upgradingStatus;

                while (WorldUpgrader.this.running)
                {
                    boolean flag = false;
                    float f1 = 0.0F;

                    for (WorldUpgrader.DimensionToUpgrade<T> dimensiontoupgrade : list)
                    {
                        ResourceKey<Level> resourcekey = dimensiontoupgrade.dimensionKey;
                        ListIterator<WorldUpgrader.FileToUpgrade> listiterator = dimensiontoupgrade.files;
                        T t = dimensiontoupgrade.storage;

                        if (listiterator.hasNext())
                        {
                            WorldUpgrader.FileToUpgrade worldupgrader$filetoupgrade = listiterator.next();
                            boolean flag1 = true;

                            for (ChunkPos chunkpos : worldupgrader$filetoupgrade.chunksToUpgrade)
                            {
                                flag1 = flag1 && this.processOnePosition(resourcekey, t, chunkpos);
                                flag = true;
                            }

                            if (WorldUpgrader.this.recreateRegionFiles)
                            {
                                if (flag1)
                                {
                                    this.onFileFinished(worldupgrader$filetoupgrade.file);
                                }
                                else
                                {
                                    WorldUpgrader.LOGGER.error("Failed to convert region file {}", worldupgrader$filetoupgrade.file.getPath());
                                }
                            }
                        }

                        float f2 = (float)listiterator.nextIndex() / f;
                        WorldUpgrader.this.progressMap.put(resourcekey, f2);
                        f1 += f2;
                    }

                    WorldUpgrader.this.progress = f1;

                    if (!flag)
                    {
                        break;
                    }
                }

                WorldUpgrader.this.status = this.finishedStatus;

                for (WorldUpgrader.DimensionToUpgrade<T> dimensiontoupgrade1 : list)
                {
                    try
                    {
                        dimensiontoupgrade1.storage.close();
                    }
                    catch (Exception exception)
                    {
                        WorldUpgrader.LOGGER.error("Error upgrading chunk", (Throwable)exception);
                    }
                }
            }
        }

        private List<WorldUpgrader.DimensionToUpgrade<T>> getDimensionsToUpgrade()
        {
            List<WorldUpgrader.DimensionToUpgrade<T>> list = Lists.newArrayList();

            for (ResourceKey<Level> resourcekey : WorldUpgrader.this.levels)
            {
                RegionStorageInfo regionstorageinfo = new RegionStorageInfo(WorldUpgrader.this.levelStorage.getLevelId(), resourcekey, this.type);
                Path path = WorldUpgrader.this.levelStorage.getDimensionPath(resourcekey).resolve(this.folderName);
                T t = this.createStorage(regionstorageinfo, path);
                ListIterator<WorldUpgrader.FileToUpgrade> listiterator = this.getFilesToProcess(regionstorageinfo, path);
                list.add(new WorldUpgrader.DimensionToUpgrade<>(resourcekey, t, listiterator));
            }

            return list;
        }

        protected abstract T createStorage(RegionStorageInfo p_328836_, Path p_332071_);

        private ListIterator<WorldUpgrader.FileToUpgrade> getFilesToProcess(RegionStorageInfo p_332870_, Path p_331013_)
        {
            List<WorldUpgrader.FileToUpgrade> list = getAllChunkPositions(p_332870_, p_331013_);
            WorldUpgrader.this.totalFiles = WorldUpgrader.this.totalFiles + list.size();
            WorldUpgrader.this.totalChunks = WorldUpgrader.this.totalChunks + list.stream().mapToInt(p_328536_ -> p_328536_.chunksToUpgrade.size()).sum();
            return list.listIterator();
        }

        private static List<WorldUpgrader.FileToUpgrade> getAllChunkPositions(RegionStorageInfo p_330333_, Path p_330743_)
        {
            File[] afile = p_330743_.toFile().listFiles((p_336334_, p_329184_) -> p_329184_.endsWith(".mca"));

            if (afile == null)
            {
                return List.of();
            }
            else
            {
                List<WorldUpgrader.FileToUpgrade> list = Lists.newArrayList();

                for (File file1 : afile)
                {
                    Matcher matcher = WorldUpgrader.REGEX.matcher(file1.getName());

                    if (matcher.matches())
                    {
                        int i = Integer.parseInt(matcher.group(1)) << 5;
                        int j = Integer.parseInt(matcher.group(2)) << 5;
                        List<ChunkPos> list1 = Lists.newArrayList();

                        try (RegionFile regionfile = new RegionFile(p_330333_, file1.toPath(), p_330743_, true))
                        {
                            for (int k = 0; k < 32; k++)
                            {
                                for (int l = 0; l < 32; l++)
                                {
                                    ChunkPos chunkpos = new ChunkPos(k + i, l + j);

                                    if (regionfile.doesChunkExist(chunkpos))
                                    {
                                        list1.add(chunkpos);
                                    }
                                }
                            }

                            if (!list1.isEmpty())
                            {
                                list.add(new WorldUpgrader.FileToUpgrade(regionfile, list1));
                            }
                        }
                        catch (Throwable throwable)
                        {
                            WorldUpgrader.LOGGER.error("Failed to read chunks from region file {}", file1.toPath(), throwable);
                        }
                    }
                }

                return list;
            }
        }

        private boolean processOnePosition(ResourceKey<Level> p_328452_, T p_333889_, ChunkPos p_332028_)
        {
            boolean flag = false;

            try
            {
                flag = this.tryProcessOnePosition(p_333889_, p_332028_, p_328452_);
            }
            catch (CompletionException | ReportedException reportedexception)
            {
                Throwable throwable = reportedexception.getCause();

                if (!(throwable instanceof IOException))
                {
                    throw reportedexception;
                }

                WorldUpgrader.LOGGER.error("Error upgrading chunk {}", p_332028_, throwable);
            }

            if (flag)
            {
                WorldUpgrader.this.converted++;
            }
            else
            {
                WorldUpgrader.this.skipped++;
            }

            return flag;
        }

        protected abstract boolean tryProcessOnePosition(T p_329483_, ChunkPos p_327751_, ResourceKey<Level> p_335733_);

        private void onFileFinished(RegionFile p_332836_)
        {
            if (WorldUpgrader.this.recreateRegionFiles)
            {
                if (this.previousWriteFuture != null)
                {
                    this.previousWriteFuture.join();
                }

                Path path = p_332836_.getPath();
                Path path1 = path.getParent();
                Path path2 = WorldUpgrader.resolveRecreateDirectory(path1).resolve(path.getFileName().toString());

                try
                {
                    if (path2.toFile().exists())
                    {
                        Files.delete(path);
                        Files.move(path2, path);
                    }
                    else
                    {
                        WorldUpgrader.LOGGER.error("Failed to replace an old region file. New file {} does not exist.", path2);
                    }
                }
                catch (IOException ioexception)
                {
                    WorldUpgrader.LOGGER.error("Failed to replace an old region file", (Throwable)ioexception);
                }
            }
        }
    }

    class ChunkUpgrader extends WorldUpgrader.AbstractUpgrader<ChunkStorage>
    {
        ChunkUpgrader()
        {
            super(DataFixTypes.CHUNK, "chunk", "region", WorldUpgrader.STATUS_UPGRADING_CHUNKS, WorldUpgrader.STATUS_FINISHED_CHUNKS);
        }

        protected boolean tryProcessOnePosition(ChunkStorage p_330540_, ChunkPos p_331086_, ResourceKey<Level> p_327850_)
        {
            CompoundTag compoundtag = p_330540_.read(p_331086_).join().orElse(null);

            if (compoundtag != null)
            {
                int i = ChunkStorage.getVersion(compoundtag);
                ChunkGenerator chunkgenerator = WorldUpgrader.this.dimensions.getOrThrow(Registries.levelToLevelStem(p_327850_)).generator();
                CompoundTag compoundtag1 = p_330540_.upgradeChunkTag(p_327850_, () -> WorldUpgrader.this.overworldDataStorage, compoundtag, chunkgenerator.getTypeNameForDataFixer());
                ChunkPos chunkpos = new ChunkPos(compoundtag1.getInt("xPos"), compoundtag1.getInt("zPos"));

                if (!chunkpos.equals(p_331086_))
                {
                    WorldUpgrader.LOGGER.warn("Chunk {} has invalid position {}", p_331086_, chunkpos);
                }

                boolean flag = i < SharedConstants.getCurrentVersion().getDataVersion().getVersion();

                if (WorldUpgrader.this.eraseCache)
                {
                    flag = flag || compoundtag1.contains("Heightmaps");
                    compoundtag1.remove("Heightmaps");
                    flag = flag || compoundtag1.contains("isLightOn");
                    compoundtag1.remove("isLightOn");
                    ListTag listtag = compoundtag1.getList("sections", 10);

                    for (int j = 0; j < listtag.size(); j++)
                    {
                        CompoundTag compoundtag2 = listtag.getCompound(j);
                        flag = flag || compoundtag2.contains("BlockLight");
                        compoundtag2.remove("BlockLight");
                        flag = flag || compoundtag2.contains("SkyLight");
                        compoundtag2.remove("SkyLight");
                    }
                }

                if (flag || WorldUpgrader.this.recreateRegionFiles)
                {
                    if (this.previousWriteFuture != null)
                    {
                        this.previousWriteFuture.join();
                    }

                    this.previousWriteFuture = p_330540_.write(p_331086_, compoundtag1);
                    return true;
                }
            }

            return false;
        }

        protected ChunkStorage createStorage(RegionStorageInfo p_333791_, Path p_332463_)
        {
            return (ChunkStorage)(WorldUpgrader.this.recreateRegionFiles
                                  ? new RecreatingChunkStorage(
                                      p_333791_.withTypeSuffix("source"),
                                      p_332463_,
                                      p_333791_.withTypeSuffix("target"),
                                      WorldUpgrader.resolveRecreateDirectory(p_332463_),
                                      WorldUpgrader.this.dataFixer,
                                      true
                                  )
                                  : new ChunkStorage(p_333791_, p_332463_, WorldUpgrader.this.dataFixer, true));
        }
    }

    static record DimensionToUpgrade<T>(ResourceKey<Level> dimensionKey, T storage, ListIterator<WorldUpgrader.FileToUpgrade> files)
    {
    }

    class EntityUpgrader extends WorldUpgrader.SimpleRegionStorageUpgrader
    {
        EntityUpgrader()
        {
            super(DataFixTypes.ENTITY_CHUNK, "entities", WorldUpgrader.STATUS_UPGRADING_ENTITIES, WorldUpgrader.STATUS_FINISHED_ENTITIES);
        }

        @Override
        protected CompoundTag upgradeTag(SimpleRegionStorage p_334286_, CompoundTag p_335346_)
        {
            return p_334286_.upgradeChunkTag(p_335346_, -1);
        }
    }

    static record FileToUpgrade(RegionFile file, List<ChunkPos> chunksToUpgrade)
    {
    }

    class PoiUpgrader extends WorldUpgrader.SimpleRegionStorageUpgrader
    {
        PoiUpgrader()
        {
            super(DataFixTypes.POI_CHUNK, "poi", WorldUpgrader.STATUS_UPGRADING_POI, WorldUpgrader.STATUS_FINISHED_POI);
        }

        @Override
        protected CompoundTag upgradeTag(SimpleRegionStorage p_329642_, CompoundTag p_336180_)
        {
            return p_329642_.upgradeChunkTag(p_336180_, 1945);
        }
    }

    abstract class SimpleRegionStorageUpgrader extends WorldUpgrader.AbstractUpgrader<SimpleRegionStorage>
    {
        SimpleRegionStorageUpgrader(final DataFixTypes p_332054_, final String p_328150_, final MutableComponent p_336376_, final MutableComponent p_335930_)
        {
            super(p_332054_, p_328150_, p_328150_, p_336376_, p_335930_);
        }

        protected SimpleRegionStorage createStorage(RegionStorageInfo p_328549_, Path p_333111_)
        {
            return (SimpleRegionStorage)(WorldUpgrader.this.recreateRegionFiles
                                         ? new RecreatingSimpleRegionStorage(
                                             p_328549_.withTypeSuffix("source"),
                                             p_333111_,
                                             p_328549_.withTypeSuffix("target"),
                                             WorldUpgrader.resolveRecreateDirectory(p_333111_),
                                             WorldUpgrader.this.dataFixer,
                                             true,
                                             this.dataFixType
                                         )
                                         : new SimpleRegionStorage(p_328549_, p_333111_, WorldUpgrader.this.dataFixer, true, this.dataFixType));
        }

        protected boolean tryProcessOnePosition(SimpleRegionStorage p_327888_, ChunkPos p_328250_, ResourceKey<Level> p_329996_)
        {
            CompoundTag compoundtag = p_327888_.read(p_328250_).join().orElse(null);

            if (compoundtag != null)
            {
                int i = ChunkStorage.getVersion(compoundtag);
                CompoundTag compoundtag1 = this.upgradeTag(p_327888_, compoundtag);
                boolean flag = i < SharedConstants.getCurrentVersion().getDataVersion().getVersion();

                if (flag || WorldUpgrader.this.recreateRegionFiles)
                {
                    if (this.previousWriteFuture != null)
                    {
                        this.previousWriteFuture.join();
                    }

                    this.previousWriteFuture = p_327888_.write(p_328250_, compoundtag1);
                    return true;
                }
            }

            return false;
        }

        protected abstract CompoundTag upgradeTag(SimpleRegionStorage p_328302_, CompoundTag p_330493_);
    }
}
