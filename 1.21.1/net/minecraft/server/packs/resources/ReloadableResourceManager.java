package net.minecraft.server.packs.resources;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Unit;
import net.optifine.util.TextureUtils;
import org.slf4j.Logger;

public class ReloadableResourceManager implements ResourceManager, AutoCloseable
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private CloseableResourceManager resources;
    private final List<PreparableReloadListener> listeners = Lists.newArrayList();
    private final PackType type;

    public ReloadableResourceManager(PackType p_203820_)
    {
        this.type = p_203820_;
        this.resources = new MultiPackResourceManager(p_203820_, List.of());
    }

    @Override
    public void close()
    {
        this.resources.close();
    }

    public void registerReloadListener(PreparableReloadListener p_10714_)
    {
        this.listeners.add(p_10714_);
    }

    public ReloadInstance createReload(Executor p_143930_, Executor p_143931_, CompletableFuture<Unit> p_143932_, List<PackResources> p_143933_)
    {
        LOGGER.info("Reloading ResourceManager: {}", LogUtils.defer(() -> p_143933_.stream().map(PackResources::packId).collect(Collectors.joining(", "))));
        this.resources.close();
        this.resources = new MultiPackResourceManager(this.type, p_143933_);

        if (Minecraft.getInstance().getResourceManager() == this)
        {
            TextureUtils.resourcesPreReload(this);
        }

        return SimpleReloadInstance.create(this.resources, this.listeners, p_143930_, p_143931_, p_143932_, LOGGER.isDebugEnabled());
    }

    @Override
    public Optional<Resource> getResource(ResourceLocation p_215494_)
    {
        return this.resources.getResource(p_215494_);
    }

    @Override
    public Set<String> getNamespaces()
    {
        return this.resources.getNamespaces();
    }

    @Override
    public List<Resource> getResourceStack(ResourceLocation p_215486_)
    {
        return this.resources.getResourceStack(p_215486_);
    }

    @Override
    public Map<ResourceLocation, Resource> listResources(String p_215488_, Predicate<ResourceLocation> p_215489_)
    {
        return this.resources.listResources(p_215488_, p_215489_);
    }

    @Override
    public Map<ResourceLocation, List<Resource>> listResourceStacks(String p_215491_, Predicate<ResourceLocation> p_215492_)
    {
        return this.resources.listResourceStacks(p_215491_, p_215492_);
    }

    @Override
    public Stream<PackResources> listPacks()
    {
        return this.resources.listPacks();
    }

    public void registerReloadListenerIfNotPresent(PreparableReloadListener listener)
    {
        if (!this.listeners.contains(listener))
        {
            this.registerReloadListener(listener);
        }
    }
}
