package net.minecraft.server.packs;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult.Error;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.optifine.reflect.ReflectorForge;
import org.slf4j.Logger;

public class VanillaPackResources implements PackResources
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackLocationInfo location;
    private final BuiltInMetadata metadata;
    private final Set<String> namespaces;
    private final List<Path> rootPaths;
    private final Map<PackType, List<Path>> pathsForType;

    VanillaPackResources(
        PackLocationInfo p_331390_, BuiltInMetadata p_249743_, Set<String> p_250468_, List<Path> p_248798_, Map<PackType, List<Path>> p_251106_
    )
    {
        this.location = p_331390_;
        this.metadata = p_249743_;
        this.namespaces = p_250468_;
        this.rootPaths = p_248798_;
        this.pathsForType = p_251106_;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... p_250530_)
    {
        FileUtil.validatePath(p_250530_);
        List<String> list = List.of(p_250530_);

        for (Path path : this.rootPaths)
        {
            Path path1 = FileUtil.resolvePath(path, list);

            if (Files.exists(path1) && PathPackResources.validatePath(path1))
            {
                return IoSupplier.create(path1);
            }
        }

        return null;
    }

    public void listRawPaths(PackType p_252103_, ResourceLocation p_250441_, Consumer<Path> p_251968_)
    {
        FileUtil.decomposePath(p_250441_.getPath()).ifSuccess(partsIn ->
        {
            String s = p_250441_.getNamespace();

            for (Path path : this.pathsForType.get(p_252103_))
            {
                Path path1 = path.resolve(s);
                p_251968_.accept(FileUtil.resolvePath(path1, (List<String>)partsIn));
            }
        }).ifError(errorIn -> LOGGER.error("Invalid path {}: {}", p_250441_, errorIn.message()));
    }

    @Override
    public void listResources(PackType p_248974_, String p_248703_, String p_250848_, PackResources.ResourceOutput p_249668_)
    {
        PackResources.ResourceOutput packresources$resourceoutput = (locIn, suppIn) ->
        {
            if (locIn.getPath().startsWith("models/block/template_glass_pane"))
            {
                IoSupplier<InputStream> iosupplier = this.getResourceOF(p_248974_, locIn);

                if (iosupplier != null)
                {
                    suppIn = iosupplier;
                }
            }

            p_249668_.accept(locIn, suppIn);
        };
        FileUtil.decomposePath(p_250848_).ifSuccess(partsIn ->
        {
            List<Path> list = this.pathsForType.get(p_248974_);
            int i = list.size();

            if (i == 1)
            {
                getResources(packresources$resourceoutput, p_248703_, list.get(0), (List<String>)partsIn);
            }
            else if (i > 1)
            {
                Map<ResourceLocation, IoSupplier<InputStream>> map = new HashMap<>();

                for (int j = 0; j < i - 1; j++)
                {
                    getResources(map::putIfAbsent, p_248703_, list.get(j), (List<String>)partsIn);
                }

                Path path = list.get(i - 1);

                if (map.isEmpty())
                {
                    getResources(packresources$resourceoutput, p_248703_, path, (List<String>)partsIn);
                }
                else
                {
                    getResources(map::putIfAbsent, p_248703_, path, (List<String>)partsIn);
                    map.forEach(packresources$resourceoutput);
                }
            }
        }).ifError(errorIn -> LOGGER.error("Invalid path {}: {}", p_250848_, errorIn.message()));
    }

    private static void getResources(PackResources.ResourceOutput p_249662_, String p_251249_, Path p_251290_, List<String> p_250451_)
    {
        Path path = p_251290_.resolve(p_251249_);
        PathPackResources.listPath(p_251249_, path, p_250451_, p_249662_);
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType p_250512_, ResourceLocation p_251554_)
    {
        IoSupplier<InputStream> iosupplier = this.getResourcesImpl(p_250512_, p_251554_);
        return iosupplier != null ? iosupplier : this.getResourceOF(p_250512_, p_251554_);
    }

    @Nullable
    public IoSupplier<InputStream> getResourcesImpl(PackType type, ResourceLocation namespaceIn)
    {
        return FileUtil.decomposePath(namespaceIn.getPath()).mapOrElse(partsIn ->
        {
            String s = namespaceIn.getNamespace();

            for (Path path : this.pathsForType.get(type))
            {
                Path path1 = FileUtil.resolvePath(path.resolve(s), (List<String>)partsIn);

                if (Files.exists(path1) && PathPackResources.validatePath(path1))
                {
                    return IoSupplier.create(path1);
                }
            }

            return null;
        }, errorIn ->
        {
            LOGGER.error("Invalid path {}: {}", namespaceIn, errorIn.message());
            return null;
        });
    }

    @Override
    public Set<String> getNamespaces(PackType p_10322_)
    {
        return this.namespaces;
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> p_10333_)
    {
        IoSupplier<InputStream> iosupplier = this.getRootResource("pack.mcmeta");

        if (iosupplier != null)
        {
            try
            {
                Object object;

                try (InputStream inputstream = iosupplier.get())
                {
                    T t = AbstractPackResources.getMetadataFromStream(p_10333_, inputstream);

                    if (t == null)
                    {
                        return this.metadata.get(p_10333_);
                    }

                    object = t;
                }

                return (T)object;
            }
            catch (IOException ioexception)
            {
            }
        }

        return this.metadata.get(p_10333_);
    }

    @Override
    public PackLocationInfo location()
    {
        return this.location;
    }

    @Override
    public void close()
    {
    }

    public ResourceProvider asProvider()
    {
        return locIn -> Optional.ofNullable(this.getResource(PackType.CLIENT_RESOURCES, locIn))
               .map(supplierIn -> new Resource(this, (IoSupplier<InputStream>)supplierIn));
    }

    public IoSupplier<InputStream> getResourceOF(PackType type, ResourceLocation locationIn)
    {
        String s = "/" + type.getDirectory() + "/" + locationIn.getNamespace() + "/" + locationIn.getPath();
        InputStream inputstream = ReflectorForge.getOptiFineResourceStream(s);

        if (inputstream != null)
        {
            return () -> inputstream;
        }
        else
        {
            URL url = VanillaPackResources.class.getResource(s);
            return url != null ? () -> url.openStream() : null;
        }
    }
}
