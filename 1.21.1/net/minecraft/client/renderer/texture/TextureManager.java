package net.minecraft.client.renderer.texture;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.optifine.Config;
import net.optifine.EmissiveTextures;
import net.optifine.RandomEntities;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.ShadersTex;
import org.slf4j.Logger;

public class TextureManager implements PreparableReloadListener, Tickable, AutoCloseable
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation INTENTIONAL_MISSING_TEXTURE = ResourceLocation.withDefaultNamespace("");
    private final Map<ResourceLocation, AbstractTexture> byPath = Maps.newHashMap();
    private final Set<Tickable> tickableTextures = Sets.newHashSet();
    private final Map<String, Integer> prefixRegister = Maps.newHashMap();
    private final ResourceManager resourceManager;
    private Int2ObjectMap<AbstractTexture> mapTexturesById = new Int2ObjectOpenHashMap<>();
    private AbstractTexture mojangLogoTexture;

    public TextureManager(ResourceManager p_118474_)
    {
        this.resourceManager = p_118474_;
    }

    public void bindForSetup(ResourceLocation p_174785_)
    {
        if (!RenderSystem.isOnRenderThread())
        {
            RenderSystem.recordRenderCall(() -> this._bind(p_174785_));
        }
        else
        {
            this._bind(p_174785_);
        }
    }

    private void _bind(ResourceLocation p_118520_)
    {
        AbstractTexture abstracttexture = this.byPath.get(p_118520_);

        if (abstracttexture == null)
        {
            abstracttexture = new SimpleTexture(p_118520_);
            this.register(p_118520_, abstracttexture);
        }

        if (Config.isShaders())
        {
            ShadersTex.bindTexture(abstracttexture);
        }
        else
        {
            abstracttexture.bind();
        }
    }

    public void register(ResourceLocation p_118496_, AbstractTexture p_118497_)
    {
        if (Reflector.MinecraftForge.exists() && this.mojangLogoTexture == null && p_118496_.equals(LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION))
        {
            LOGGER.info("Keep logo texture for ForgeLoadingOverlay: " + p_118497_);
            this.mojangLogoTexture = p_118497_;
        }

        p_118497_ = this.loadTexture(p_118496_, p_118497_);
        AbstractTexture abstracttexture = this.byPath.put(p_118496_, p_118497_);

        if (abstracttexture != p_118497_)
        {
            if (abstracttexture != null && abstracttexture != MissingTextureAtlasSprite.getTexture() && abstracttexture != this.mojangLogoTexture)
            {
                this.safeClose(p_118496_, abstracttexture);
            }

            if (p_118497_ instanceof Tickable)
            {
                this.tickableTextures.add((Tickable)p_118497_);
            }
        }

        int i = p_118497_.getId();

        if (i > 0)
        {
            this.mapTexturesById.put(i, p_118497_);
        }
    }

    private void safeClose(ResourceLocation p_118509_, AbstractTexture p_118510_)
    {
        if (p_118510_ != MissingTextureAtlasSprite.getTexture())
        {
            this.tickableTextures.remove(p_118510_);

            try
            {
                p_118510_.close();
            }
            catch (Exception exception)
            {
                LOGGER.warn("Failed to close texture {}", p_118509_, exception);
            }
        }

        p_118510_.releaseId();
    }

    private AbstractTexture loadTexture(ResourceLocation p_118516_, AbstractTexture p_118517_)
    {
        try
        {
            p_118517_.load(this.resourceManager);
            return p_118517_;
        }
        catch (IOException ioexception)
        {
            if (p_118516_ != INTENTIONAL_MISSING_TEXTURE)
            {
                LOGGER.warn("Failed to load texture: {}", p_118516_);
                LOGGER.warn(ioexception.getClass().getName() + ": " + ioexception.getMessage());
            }

            return MissingTextureAtlasSprite.getTexture();
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Registering texture");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Resource location being registered");
            crashreportcategory.setDetail("Resource location", p_118516_);
            crashreportcategory.setDetail("Texture object class", () -> p_118517_.getClass().getName());
            throw new ReportedException(crashreport);
        }
    }

    public AbstractTexture getTexture(ResourceLocation p_118507_)
    {
        AbstractTexture abstracttexture = this.byPath.get(p_118507_);

        if (abstracttexture == null)
        {
            abstracttexture = new SimpleTexture(p_118507_);
            this.register(p_118507_, abstracttexture);
        }

        return abstracttexture;
    }

    public AbstractTexture getTexture(ResourceLocation p_174787_, AbstractTexture p_174788_)
    {
        return this.byPath.getOrDefault(p_174787_, p_174788_);
    }

    public ResourceLocation register(String p_118491_, DynamicTexture p_118492_)
    {
        Integer integer = this.prefixRegister.get(p_118491_);

        if (integer == null)
        {
            integer = 1;
        }
        else
        {
            integer = integer + 1;
        }

        this.prefixRegister.put(p_118491_, integer);
        ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace(String.format(Locale.ROOT, "dynamic/%s_%d", p_118491_, integer));
        this.register(resourcelocation, p_118492_);
        return resourcelocation;
    }

    public CompletableFuture<Void> preload(ResourceLocation p_118502_, Executor p_118503_)
    {
        if (!this.byPath.containsKey(p_118502_))
        {
            PreloadedTexture preloadedtexture = new PreloadedTexture(this.resourceManager, p_118502_, p_118503_);
            this.byPath.put(p_118502_, preloadedtexture);
            return preloadedtexture.getFuture().thenRunAsync(() -> this.register(p_118502_, preloadedtexture), TextureManager::execute);
        }
        else
        {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static void execute(Runnable p_118489_)
    {
        Minecraft.getInstance().execute(() -> RenderSystem.recordRenderCall(p_118489_::run));
    }

    @Override
    public void tick()
    {
        for (Tickable tickable : this.tickableTextures)
        {
            tickable.tick();
        }
    }

    public void release(ResourceLocation p_118514_)
    {
        AbstractTexture abstracttexture = this.byPath.remove(p_118514_);

        if (abstracttexture != null)
        {
            this.safeClose(p_118514_, abstracttexture);
        }
    }

    @Override
    public void close()
    {
        this.byPath.forEach(this::safeClose);
        this.byPath.clear();
        this.tickableTextures.clear();
        this.prefixRegister.clear();
        this.mapTexturesById.clear();
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparableReloadListener.PreparationBarrier p_118476_,
        ResourceManager p_118477_,
        ProfilerFiller p_118478_,
        ProfilerFiller p_118479_,
        Executor p_118480_,
        Executor p_118481_
    )
    {
        Config.dbg("*** Reloading textures ***");
        Config.log("Resource packs: " + Config.getResourcePackNames());
        Iterator iterator = this.byPath.keySet().iterator();

        while (iterator.hasNext())
        {
            ResourceLocation resourcelocation = (ResourceLocation)iterator.next();
            String s = resourcelocation.getPath();

            if (s.startsWith("optifine/") || EmissiveTextures.isEmissive(resourcelocation))
            {
                AbstractTexture abstracttexture = this.byPath.get(resourcelocation);

                if (abstracttexture instanceof AbstractTexture)
                {
                    abstracttexture.releaseId();
                }

                iterator.remove();
            }
        }

        RandomEntities.update();
        EmissiveTextures.update();
        CompletableFuture<Void> completablefuture = new CompletableFuture<>();
        TitleScreen.preloadResources(this, p_118480_).thenCompose(p_118476_::wait).thenAcceptAsync(voidIn ->
        {
            MissingTextureAtlasSprite.getTexture();
            AddRealmPopupScreen.updateCarouselImages(this.resourceManager);
            Set<Entry<ResourceLocation, AbstractTexture>> set = new HashSet<>(this.byPath.entrySet());
            Iterator<Entry<ResourceLocation, AbstractTexture>> iterator1 = set.iterator();

            while (iterator1.hasNext())
            {
                Entry<ResourceLocation, AbstractTexture> entry = iterator1.next();
                ResourceLocation resourcelocation1 = entry.getKey();
                AbstractTexture abstracttexture1 = entry.getValue();
                abstracttexture1.resetBlurMipmap();

                if (abstracttexture1 == MissingTextureAtlasSprite.getTexture() && !resourcelocation1.equals(MissingTextureAtlasSprite.getLocation()))
                {
                    iterator1.remove();
                }
                else
                {
                    abstracttexture1.reset(this, p_118477_, resourcelocation1, p_118481_);
                }
            }

            Minecraft.getInstance().tell(() -> completablefuture.complete(null));
        }, runnableIn -> RenderSystem.recordRenderCall(runnableIn::run));
        return completablefuture;
    }

    public void dumpAllSheets(Path p_276129_)
    {
        if (!RenderSystem.isOnRenderThread())
        {
            RenderSystem.recordRenderCall(() -> this._dumpAllSheets(p_276129_));
        }
        else
        {
            this._dumpAllSheets(p_276129_);
        }
    }

    private void _dumpAllSheets(Path p_276128_)
    {
        try
        {
            Files.createDirectories(p_276128_);
        }
        catch (IOException ioexception)
        {
            LOGGER.error("Failed to create directory {}", p_276128_, ioexception);
            return;
        }

        this.byPath.forEach((locIn, texIn) ->
        {
            if (texIn instanceof Dumpable dumpable)
            {
                try
                {
                    dumpable.dumpContents(locIn, p_276128_);
                }
                catch (IOException ioexception1)
                {
                    LOGGER.error("Failed to dump texture {}", locIn, ioexception1);
                }
            }
        });
    }

    public AbstractTexture getTextureById(int id)
    {
        AbstractTexture abstracttexture = this.mapTexturesById.get(id);

        if (abstracttexture != null && abstracttexture.getId() != id)
        {
            this.mapTexturesById.remove(id);
            this.mapTexturesById.put(abstracttexture.getId(), abstracttexture);
            abstracttexture = null;
        }

        return abstracttexture;
    }

    public Collection<AbstractTexture> getTextures()
    {
        return this.byPath.values();
    }

    public Collection<ResourceLocation> getTextureLocations()
    {
        return this.byPath.keySet();
    }
}
