package net.minecraft.world.level.storage;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.FileUtil;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtFormatException;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.nbt.visitors.SkipFields;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.DirectoryLock;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import net.minecraft.world.level.validation.PathAllowList;
import org.slf4j.Logger;

public class LevelStorageSource
{
    static final Logger LOGGER = LogUtils.getLogger();
    static final DateTimeFormatter FORMATTER = FileNameDateFormatter.create();
    private static final String TAG_DATA = "Data";
    private static final PathMatcher NO_SYMLINKS_ALLOWED = p_296993_ -> false;
    public static final String ALLOWED_SYMLINKS_CONFIG_NAME = "allowed_symlinks.txt";
    private static final int UNCOMPRESSED_NBT_QUOTA = 104857600;
    private static final int DISK_SPACE_WARNING_THRESHOLD = 67108864;
    private final Path baseDir;
    private final Path backupDir;
    final DataFixer fixerUpper;
    private final DirectoryValidator worldDirValidator;

    public LevelStorageSource(Path p_289985_, Path p_289978_, DirectoryValidator p_289922_, DataFixer p_289940_)
    {
        this.fixerUpper = p_289940_;

        try
        {
            FileUtil.createDirectoriesSafe(p_289985_);
        }
        catch (IOException ioexception)
        {
            throw new UncheckedIOException(ioexception);
        }

        this.baseDir = p_289985_;
        this.backupDir = p_289978_;
        this.worldDirValidator = p_289922_;
    }

    public static DirectoryValidator parseValidator(Path p_289968_)
    {
        if (Files.exists(p_289968_))
        {
            try
            {
                DirectoryValidator directoryvalidator;

                try (BufferedReader bufferedreader = Files.newBufferedReader(p_289968_))
                {
                    directoryvalidator = new DirectoryValidator(PathAllowList.readPlain(bufferedreader));
                }

                return directoryvalidator;
            }
            catch (Exception exception)
            {
                LOGGER.error("Failed to parse {}, disallowing all symbolic links", "allowed_symlinks.txt", exception);
            }
        }

        return new DirectoryValidator(NO_SYMLINKS_ALLOWED);
    }

    public static LevelStorageSource createDefault(Path p_78243_)
    {
        DirectoryValidator directoryvalidator = parseValidator(p_78243_.resolve("allowed_symlinks.txt"));
        return new LevelStorageSource(p_78243_, p_78243_.resolve("../backups"), directoryvalidator, DataFixers.getDataFixer());
    }

    public static WorldDataConfiguration readDataConfig(Dynamic<?> p_250884_)
    {
        return WorldDataConfiguration.CODEC.parse(p_250884_).resultOrPartial(LOGGER::error).orElse(WorldDataConfiguration.DEFAULT);
    }

    public static WorldLoader.PackConfig getPackConfig(Dynamic<?> p_312675_, PackRepository p_309764_, boolean p_310223_)
    {
        return new WorldLoader.PackConfig(p_309764_, readDataConfig(p_312675_), p_310223_, false);
    }

    public static LevelDataAndDimensions getLevelDataAndDimensions(
        Dynamic<?> p_311362_, WorldDataConfiguration p_311014_, Registry<LevelStem> p_311619_, RegistryAccess.Frozen p_313214_
    )
    {
        Dynamic<?> dynamic = RegistryOps.injectRegistryContext(p_311362_, p_313214_);
        Dynamic<?> dynamic1 = dynamic.get("WorldGenSettings").orElseEmptyMap();
        WorldGenSettings worldgensettings = WorldGenSettings.CODEC.parse(dynamic1).getOrThrow();
        LevelSettings levelsettings = LevelSettings.parse(dynamic, p_311014_);
        WorldDimensions.Complete worlddimensions$complete = worldgensettings.dimensions().bake(p_311619_);
        Lifecycle lifecycle = worlddimensions$complete.lifecycle().add(p_313214_.allRegistriesLifecycle());
        PrimaryLevelData primaryleveldata = PrimaryLevelData.parse(
                                                dynamic, levelsettings, worlddimensions$complete.specialWorldProperty(), worldgensettings.options(), lifecycle
                                            );
        return new LevelDataAndDimensions(primaryleveldata, worlddimensions$complete);
    }

    public String getName()
    {
        return "Anvil";
    }

    public LevelStorageSource.LevelCandidates findLevelCandidates() throws LevelStorageException
    {
        if (!Files.isDirectory(this.baseDir))
        {
            throw new LevelStorageException(Component.translatable("selectWorld.load_folder_access"));
        }
        else
        {
            try
            {
                LevelStorageSource.LevelCandidates levelstoragesource$levelcandidates;

                try (Stream<Path> stream = Files.list(this.baseDir))
                {
                    List<LevelStorageSource.LevelDirectory> list = stream.filter(p_230839_ -> Files.isDirectory(p_230839_))
                            .map(LevelStorageSource.LevelDirectory::new)
                            .filter(p_230835_ -> Files.isRegularFile(p_230835_.dataFile()) || Files.isRegularFile(p_230835_.oldDataFile()))
                            .toList();
                    levelstoragesource$levelcandidates = new LevelStorageSource.LevelCandidates(list);
                }

                return levelstoragesource$levelcandidates;
            }
            catch (IOException ioexception)
            {
                throw new LevelStorageException(Component.translatable("selectWorld.load_folder_access"));
            }
        }
    }

    public CompletableFuture<List<LevelSummary>> loadLevelSummaries(LevelStorageSource.LevelCandidates p_230814_)
    {
        List<CompletableFuture<LevelSummary>> list = new ArrayList<>(p_230814_.levels.size());

        for (LevelStorageSource.LevelDirectory levelstoragesource$leveldirectory : p_230814_.levels)
        {
            list.add(CompletableFuture.supplyAsync(() ->
            {
                boolean flag;

                try {
                    flag = DirectoryLock.isLocked(levelstoragesource$leveldirectory.path());
                }
                catch (Exception exception)
                {
                    LOGGER.warn("Failed to read {} lock", levelstoragesource$leveldirectory.path(), exception);
                    return null;
                }

                try {
                    return this.readLevelSummary(levelstoragesource$leveldirectory, flag);
                }
                catch (OutOfMemoryError outofmemoryerror1)
                {
                    MemoryReserve.release();
                    System.gc();
                    String s = "Ran out of memory trying to read summary of world folder \"" + levelstoragesource$leveldirectory.directoryName() + "\"";
                    LOGGER.error(LogUtils.FATAL_MARKER, s);
                    OutOfMemoryError outofmemoryerror = new OutOfMemoryError("Ran out of memory reading level data");
                    outofmemoryerror.initCause(outofmemoryerror1);
                    CrashReport crashreport = CrashReport.forThrowable(outofmemoryerror, s);
                    CrashReportCategory crashreportcategory = crashreport.addCategory("World details");
                    crashreportcategory.setDetail("Folder Name", levelstoragesource$leveldirectory.directoryName());

                    try
                    {
                        long i = Files.size(levelstoragesource$leveldirectory.dataFile());
                        crashreportcategory.setDetail("level.dat size", i);
                    }
                    catch (IOException ioexception)
                    {
                        crashreportcategory.setDetailError("level.dat size", ioexception);
                    }

                    throw new ReportedException(crashreport);
                }
            }, Util.backgroundExecutor()));
        }

        return Util.sequenceFailFastAndCancel(list).thenApply(p_230832_ -> p_230832_.stream().filter(Objects::nonNull).sorted().toList());
    }

    private int getStorageVersion()
    {
        return 19133;
    }

    static CompoundTag readLevelDataTagRaw(Path p_312857_) throws IOException
    {
        return NbtIo.readCompressed(p_312857_, NbtAccounter.create(104857600L));
    }

    static Dynamic<?> readLevelDataTagFixed(Path p_309458_, DataFixer p_312702_) throws IOException
    {
        CompoundTag compoundtag = readLevelDataTagRaw(p_309458_);
        CompoundTag compoundtag1 = compoundtag.getCompound("Data");
        int i = NbtUtils.getDataVersion(compoundtag1, -1);
        Dynamic<?> dynamic = DataFixTypes.LEVEL.updateToCurrentVersion(p_312702_, new Dynamic<>(NbtOps.INSTANCE, compoundtag1), i);
        dynamic = dynamic.update("Player", p_327540_ -> DataFixTypes.PLAYER.updateToCurrentVersion(p_312702_, p_327540_, i));
        return dynamic.update("WorldGenSettings", p_327543_ -> DataFixTypes.WORLD_GEN_SETTINGS.updateToCurrentVersion(p_312702_, p_327543_, i));
    }

    private LevelSummary readLevelSummary(LevelStorageSource.LevelDirectory p_313112_, boolean p_312081_)
    {
        Path path = p_313112_.dataFile();

        if (Files.exists(path))
        {
            try
            {
                if (Files.isSymbolicLink(path))
                {
                    List<ForbiddenSymlinkInfo> list = this.worldDirValidator.validateSymlink(path);

                    if (!list.isEmpty())
                    {
                        LOGGER.warn("{}", ContentValidationException.getMessage(path, list));
                        return new LevelSummary.SymlinkLevelSummary(p_313112_.directoryName(), p_313112_.iconFile());
                    }
                }

                if (readLightweightData(path) instanceof CompoundTag compoundtag)
                {
                    CompoundTag compoundtag1 = compoundtag.getCompound("Data");
                    int i = NbtUtils.getDataVersion(compoundtag1, -1);
                    Dynamic<?> dynamic = DataFixTypes.LEVEL.updateToCurrentVersion(this.fixerUpper, new Dynamic<>(NbtOps.INSTANCE, compoundtag1), i);
                    return this.makeLevelSummary(dynamic, p_313112_, p_312081_);
                }

                LOGGER.warn("Invalid root tag in {}", path);
            }
            catch (Exception exception)
            {
                LOGGER.error("Exception reading {}", path, exception);
            }
        }

        return new LevelSummary.CorruptedLevelSummary(p_313112_.directoryName(), p_313112_.iconFile(), getFileModificationTime(p_313112_));
    }

    private static long getFileModificationTime(LevelStorageSource.LevelDirectory p_311230_)
    {
        Instant instant = getFileModificationTime(p_311230_.dataFile());

        if (instant == null)
        {
            instant = getFileModificationTime(p_311230_.oldDataFile());
        }

        return instant == null ? -1L : instant.toEpochMilli();
    }

    @Nullable
    static Instant getFileModificationTime(Path p_313101_)
    {
        try
        {
            return Files.getLastModifiedTime(p_313101_).toInstant();
        }
        catch (IOException ioexception)
        {
            return null;
        }
    }

    LevelSummary makeLevelSummary(Dynamic<?> p_310955_, LevelStorageSource.LevelDirectory p_309842_, boolean p_310644_)
    {
        LevelVersion levelversion = LevelVersion.parse(p_310955_);
        int i = levelversion.levelDataVersion();

        if (i != 19132 && i != 19133)
        {
            throw new NbtFormatException("Unknown data version: " + Integer.toHexString(i));
        }
        else
        {
            boolean flag = i != this.getStorageVersion();
            Path path = p_309842_.iconFile();
            WorldDataConfiguration worlddataconfiguration = readDataConfig(p_310955_);
            LevelSettings levelsettings = LevelSettings.parse(p_310955_, worlddataconfiguration);
            FeatureFlagSet featureflagset = parseFeatureFlagsFromSummary(p_310955_);
            boolean flag1 = FeatureFlags.isExperimental(featureflagset);
            return new LevelSummary(levelsettings, levelversion, p_309842_.directoryName(), flag, p_310644_, flag1, path);
        }
    }

    private static FeatureFlagSet parseFeatureFlagsFromSummary(Dynamic<?> p_249466_)
    {
        Set<ResourceLocation> set = p_249466_.get("enabled_features")
                                    .asStream()
                                    .flatMap(p_327537_ -> p_327537_.asString().result().map(ResourceLocation::tryParse).stream())
                                    .collect(Collectors.toSet());
        return FeatureFlags.REGISTRY.fromNames(set, p_248503_ ->
        {
        });
    }

    @Nullable
    private static Tag readLightweightData(Path p_230837_) throws IOException
    {
        SkipFields skipfields = new SkipFields(
            new FieldSelector("Data", CompoundTag.TYPE, "Player"), new FieldSelector("Data", CompoundTag.TYPE, "WorldGenSettings")
        );
        NbtIo.parseCompressed(p_230837_, skipfields, NbtAccounter.create(104857600L));
        return skipfields.getResult();
    }

    public boolean isNewLevelIdAcceptable(String p_78241_)
    {
        try
        {
            Path path = this.getLevelPath(p_78241_);
            Files.createDirectory(path);
            Files.deleteIfExists(path);
            return true;
        }
        catch (IOException ioexception)
        {
            return false;
        }
    }

    public boolean levelExists(String p_78256_)
    {
        try
        {
            return Files.isDirectory(this.getLevelPath(p_78256_));
        }
        catch (InvalidPathException invalidpathexception)
        {
            return false;
        }
    }

    public Path getLevelPath(String p_289974_)
    {
        return this.baseDir.resolve(p_289974_);
    }

    public Path getBaseDir()
    {
        return this.baseDir;
    }

    public Path getBackupPath()
    {
        return this.backupDir;
    }

    public LevelStorageSource.LevelStorageAccess validateAndCreateAccess(String p_289980_) throws IOException, ContentValidationException
    {
        Path path = this.getLevelPath(p_289980_);
        List<ForbiddenSymlinkInfo> list = this.worldDirValidator.validateDirectory(path, true);

        if (!list.isEmpty())
        {
            throw new ContentValidationException(path, list);
        }
        else
        {
            return new LevelStorageSource.LevelStorageAccess(p_289980_, path);
        }
    }

    public LevelStorageSource.LevelStorageAccess createAccess(String p_78261_) throws IOException
    {
        Path path = this.getLevelPath(p_78261_);
        return new LevelStorageSource.LevelStorageAccess(p_78261_, path);
    }

    public DirectoryValidator getWorldDirValidator()
    {
        return this.worldDirValidator;
    }

    public static record LevelCandidates(List<LevelStorageSource.LevelDirectory> levels) implements Iterable<LevelStorageSource.LevelDirectory>
    {
        public boolean isEmpty()
        {
            return this.levels.isEmpty();
        }

        @Override
        public Iterator<LevelStorageSource.LevelDirectory> iterator()
        {
            return this.levels.iterator();
        }
    }

    public static record LevelDirectory(Path path)
    {
        public String directoryName()
        {
            return this.path.getFileName().toString();
        }
        public Path dataFile()
        {
            return this.resourcePath(LevelResource.LEVEL_DATA_FILE);
        }
        public Path oldDataFile()
        {
            return this.resourcePath(LevelResource.OLD_LEVEL_DATA_FILE);
        }
        public Path corruptedDataFile(LocalDateTime p_230857_)
        {
            return this.path.resolve(LevelResource.LEVEL_DATA_FILE.getId() + "_corrupted_" + p_230857_.format(LevelStorageSource.FORMATTER));
        }
        public Path rawDataFile(LocalDateTime p_310027_)
        {
            return this.path.resolve(LevelResource.LEVEL_DATA_FILE.getId() + "_raw_" + p_310027_.format(LevelStorageSource.FORMATTER));
        }
        public Path iconFile()
        {
            return this.resourcePath(LevelResource.ICON_FILE);
        }
        public Path lockFile()
        {
            return this.resourcePath(LevelResource.LOCK_FILE);
        }
        public Path resourcePath(LevelResource p_230855_)
        {
            return this.path.resolve(p_230855_.getId());
        }
    }

    public class LevelStorageAccess implements AutoCloseable
    {
        final DirectoryLock lock;
        final LevelStorageSource.LevelDirectory levelDirectory;
        private final String levelId;
        private final Map<LevelResource, Path> resources = Maps.newHashMap();

        LevelStorageAccess(final String p_289967_, final Path p_289988_) throws IOException
        {
            this.levelId = p_289967_;
            this.levelDirectory = new LevelStorageSource.LevelDirectory(p_289988_);
            this.lock = DirectoryLock.create(p_289988_);
        }

        public long estimateDiskSpace()
        {
            try
            {
                return Files.getFileStore(this.levelDirectory.path).getUsableSpace();
            }
            catch (Exception exception)
            {
                return Long.MAX_VALUE;
            }
        }

        public boolean checkForLowDiskSpace()
        {
            return this.estimateDiskSpace() < 67108864L;
        }

        public void safeClose()
        {
            try
            {
                this.close();
            }
            catch (IOException ioexception)
            {
                LevelStorageSource.LOGGER.warn("Failed to unlock access to level {}", this.getLevelId(), ioexception);
            }
        }

        public LevelStorageSource parent()
        {
            return LevelStorageSource.this;
        }

        public LevelStorageSource.LevelDirectory getLevelDirectory()
        {
            return this.levelDirectory;
        }

        public String getLevelId()
        {
            return this.levelId;
        }

        public Path getLevelPath(LevelResource p_78284_)
        {
            return this.resources.computeIfAbsent(p_78284_, this.levelDirectory::resourcePath);
        }

        public Path getDimensionPath(ResourceKey<Level> p_197395_)
        {
            return DimensionType.getStorageFolder(p_197395_, this.levelDirectory.path());
        }

        private void checkLock()
        {
            if (!this.lock.isValid())
            {
                throw new IllegalStateException("Lock is no longer valid");
            }
        }

        public PlayerDataStorage createPlayerStorage()
        {
            this.checkLock();
            return new PlayerDataStorage(this, LevelStorageSource.this.fixerUpper);
        }

        public LevelSummary getSummary(Dynamic<?> p_310283_)
        {
            this.checkLock();
            return LevelStorageSource.this.makeLevelSummary(p_310283_, this.levelDirectory, false);
        }

        public Dynamic<?> getDataTag() throws IOException
        {
            return this.getDataTag(false);
        }

        public Dynamic<?> getDataTagFallback() throws IOException
        {
            return this.getDataTag(true);
        }

        private Dynamic<?> getDataTag(boolean p_310699_) throws IOException
        {
            this.checkLock();
            return LevelStorageSource.readLevelDataTagFixed(p_310699_ ? this.levelDirectory.oldDataFile() : this.levelDirectory.dataFile(), LevelStorageSource.this.fixerUpper);
        }

        public void saveDataTag(RegistryAccess p_78288_, WorldData p_78289_)
        {
            this.saveDataTag(p_78288_, p_78289_, null);
        }

        public void saveDataTag(RegistryAccess p_78291_, WorldData p_78292_, @Nullable CompoundTag p_78293_)
        {
            CompoundTag compoundtag = p_78292_.createTag(p_78291_, p_78293_);
            CompoundTag compoundtag1 = new CompoundTag();
            compoundtag1.put("Data", compoundtag);
            this.saveLevelData(compoundtag1);
        }

        private void saveLevelData(CompoundTag p_312575_)
        {
            Path path = this.levelDirectory.path();

            try
            {
                Path path1 = Files.createTempFile(path, "level", ".dat");
                NbtIo.writeCompressed(p_312575_, path1);
                Path path2 = this.levelDirectory.oldDataFile();
                Path path3 = this.levelDirectory.dataFile();
                Util.safeReplaceFile(path3, path1, path2);
            }
            catch (Exception exception)
            {
                LevelStorageSource.LOGGER.error("Failed to save level {}", path, exception);
            }
        }

        public Optional<Path> getIconFile()
        {
            return !this.lock.isValid() ? Optional.empty() : Optional.of(this.levelDirectory.iconFile());
        }

        public void deleteLevel() throws IOException
        {
            this.checkLock();
            final Path path = this.levelDirectory.lockFile();
            LevelStorageSource.LOGGER.info("Deleting level {}", this.levelId);

            for (int i = 1; i <= 5; i++)
            {
                LevelStorageSource.LOGGER.info("Attempt {}...", i);

                try
                {
                    Files.walkFileTree(this.levelDirectory.path(), new SimpleFileVisitor<Path>()
                    {
                        public FileVisitResult visitFile(Path p_78323_, BasicFileAttributes p_78324_) throws IOException
                        {
                            if (!p_78323_.equals(path))
                            {
                                LevelStorageSource.LOGGER.debug("Deleting {}", p_78323_);
                                Files.delete(p_78323_);
                            }

                            return FileVisitResult.CONTINUE;
                        }
                        public FileVisitResult postVisitDirectory(Path p_78320_, @Nullable IOException p_78321_) throws IOException
                        {
                            if (p_78321_ != null)
                            {
                                throw p_78321_;
                            }
                            else
                            {
                                if (p_78320_.equals(LevelStorageAccess.this.levelDirectory.path()))
                                {
                                    LevelStorageAccess.this.lock.close();
                                    Files.deleteIfExists(path);
                                }

                                Files.delete(p_78320_);
                                return FileVisitResult.CONTINUE;
                            }
                        }
                    });
                    break;
                }
                catch (IOException ioexception)
                {
                    if (i >= 5)
                    {
                        throw ioexception;
                    }

                    LevelStorageSource.LOGGER.warn("Failed to delete {}", this.levelDirectory.path(), ioexception);

                    try
                    {
                        Thread.sleep(500L);
                    }
                    catch (InterruptedException interruptedexception)
                    {
                    }
                }
            }
        }

        public void renameLevel(String p_78298_) throws IOException
        {
            this.modifyLevelDataWithoutDatafix(p_313219_ -> p_313219_.putString("LevelName", p_78298_.trim()));
        }

        public void renameAndDropPlayer(String p_309798_) throws IOException
        {
            this.modifyLevelDataWithoutDatafix(p_312160_ ->
            {
                p_312160_.putString("LevelName", p_309798_.trim());
                p_312160_.remove("Player");
            });
        }

        private void modifyLevelDataWithoutDatafix(Consumer<CompoundTag> p_310066_) throws IOException
        {
            this.checkLock();
            CompoundTag compoundtag = LevelStorageSource.readLevelDataTagRaw(this.levelDirectory.dataFile());
            p_310066_.accept(compoundtag.getCompound("Data"));
            this.saveLevelData(compoundtag);
        }

        public long makeWorldBackup() throws IOException
        {
            this.checkLock();
            String s = LocalDateTime.now().format(LevelStorageSource.FORMATTER) + "_" + this.levelId;
            Path path = LevelStorageSource.this.getBackupPath();

            try
            {
                FileUtil.createDirectoriesSafe(path);
            }
            catch (IOException ioexception)
            {
                throw new RuntimeException(ioexception);
            }

            Path path1 = path.resolve(FileUtil.findAvailableName(path, s, ".zip"));

            try (final ZipOutputStream zipoutputstream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(path1))))
            {
                final Path path2 = Paths.get(this.levelId);
                Files.walkFileTree(this.levelDirectory.path(), new SimpleFileVisitor<Path>()
                {
                    public FileVisitResult visitFile(Path p_78339_, BasicFileAttributes p_78340_) throws IOException
                    {
                        if (p_78339_.endsWith("session.lock"))
                        {
                            return FileVisitResult.CONTINUE;
                        }
                        else
                        {
                            String s1 = path2.resolve(LevelStorageAccess.this.levelDirectory.path().relativize(p_78339_)).toString().replace('\\', '/');
                            ZipEntry zipentry = new ZipEntry(s1);
                            zipoutputstream.putNextEntry(zipentry);
                            com.google.common.io.Files.asByteSource(p_78339_.toFile()).copyTo(zipoutputstream);
                            zipoutputstream.closeEntry();
                            return FileVisitResult.CONTINUE;
                        }
                    }
                });
            }

            return Files.size(path1);
        }

        public boolean hasWorldData()
        {
            return Files.exists(this.levelDirectory.dataFile()) || Files.exists(this.levelDirectory.oldDataFile());
        }

        @Override
        public void close() throws IOException
        {
            this.lock.close();
        }

        public boolean restoreLevelDataFromOld()
        {
            return Util.safeReplaceOrMoveFile(this.levelDirectory.dataFile(), this.levelDirectory.oldDataFile(), this.levelDirectory.corruptedDataFile(LocalDateTime.now()), true);
        }

        @Nullable
        public Instant getFileModificationTime(boolean p_311251_)
        {
            return LevelStorageSource.getFileModificationTime(p_311251_ ? this.levelDirectory.oldDataFile() : this.levelDirectory.dataFile());
        }
    }
}
