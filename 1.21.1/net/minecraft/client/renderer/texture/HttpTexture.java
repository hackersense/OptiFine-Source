package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.Proxy.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.optifine.Config;
import net.optifine.http.HttpPipeline;
import net.optifine.http.HttpRequest;
import net.optifine.http.HttpResponse;
import net.optifine.player.CapeImageBuffer;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

public class HttpTexture extends SimpleTexture
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SKIN_WIDTH = 64;
    private static final int SKIN_HEIGHT = 64;
    private static final int LEGACY_SKIN_HEIGHT = 32;
    @Nullable
    private final File file;
    private final String urlString;
    private final boolean processLegacySkin;
    @Nullable
    private final Runnable onDownloaded;
    @Nullable
    private CompletableFuture<?> future;
    private boolean uploaded;
    public Boolean imageFound = null;
    public boolean pipeline = false;
    private boolean uploadPending = false;

    public HttpTexture(@Nullable File p_118002_, String p_118003_, ResourceLocation p_118004_, boolean p_118005_, @Nullable Runnable p_118006_)
    {
        super(p_118004_);
        this.file = p_118002_;
        this.urlString = p_118003_;
        this.processLegacySkin = p_118005_;
        this.onDownloaded = p_118006_;
    }

    private void loadCallback(NativeImage p_118011_)
    {
        if (this.onDownloaded instanceof CapeImageBuffer capeimagebuffer)
        {
            p_118011_ = capeimagebuffer.parseUserSkin(p_118011_);
            capeimagebuffer.skinAvailable();
        }

        this.setImageImpl(p_118011_);
    }

    private void setImageImpl(NativeImage nativeImageIn)
    {
        if (this.onDownloaded != null)
        {
            this.onDownloaded.run();
        }

        Minecraft.getInstance().execute(() ->
        {
            this.uploaded = true;

            if (!RenderSystem.isOnRenderThread())
            {
                RenderSystem.recordRenderCall(() -> this.upload(nativeImageIn));
            }
            else {
                this.upload(nativeImageIn);
            }
        });
    }

    private void upload(NativeImage p_118021_)
    {
        TextureUtil.prepareImage(this.getId(), p_118021_.getWidth(), p_118021_.getHeight());
        p_118021_.upload(0, 0, 0, true);
        this.imageFound = p_118021_ != null;
        this.size = p_118021_.getSize();
    }

    @Override
    public void load(ResourceManager p_118009_) throws IOException
    {
        Minecraft.getInstance().execute(() ->
        {
            if (!this.uploaded)
            {
                try
                {
                    super.load(p_118009_);
                }
                catch (IOException ioexception)
                {
                    LOGGER.warn("Failed to load texture: {}", this.location, ioexception);
                }

                this.uploaded = true;
            }
        });

        if (this.future == null)
        {
            NativeImage nativeimage;

            if (this.file != null && this.file.isFile())
            {
                LOGGER.debug("Loading http texture from local cache ({})", this.file);
                FileInputStream fileinputstream = new FileInputStream(this.file);
                nativeimage = this.load(fileinputstream);
            }
            else
            {
                nativeimage = null;
            }

            if (nativeimage != null)
            {
                this.loadCallback(nativeimage);
                this.loadingFinished();
            }
            else
            {
                this.future = CompletableFuture.runAsync(() ->
                {
                    HttpURLConnection httpurlconnection = null;
                    LOGGER.debug("Downloading http texture from {} to {}", this.urlString, this.file);

                    if (this.shouldPipeline())
                    {
                        this.loadPipelined();
                    }
                    else {
                        try {
                            httpurlconnection = (HttpURLConnection)new URL(this.urlString).openConnection(Minecraft.getInstance().getProxy());
                            httpurlconnection.setDoInput(true);
                            httpurlconnection.setDoOutput(false);
                            httpurlconnection.connect();

                            if (httpurlconnection.getResponseCode() / 100 != 2)
                            {
                                if (httpurlconnection.getErrorStream() != null)
                                {
                                    Config.readAll(httpurlconnection.getErrorStream());
                                }

                                return;
                            }

                            InputStream inputstream;

                            if (this.file != null)
                            {
                                FileUtils.copyInputStreamToFile(httpurlconnection.getInputStream(), this.file);
                                inputstream = new FileInputStream(this.file);
                            }
                            else {
                                inputstream = httpurlconnection.getInputStream();
                            }

                            Minecraft.getInstance().execute(() -> {
                                NativeImage nativeimage1 = this.load(inputstream);

                                if (nativeimage1 != null)
                                {
                                    this.loadCallback(nativeimage1);
                                    this.loadingFinished();
                                }
                            });
                            this.uploadPending = true;
                        }
                        catch (Exception exception1)
                        {
                            LOGGER.error("Couldn't download http texture", (Throwable)exception1);
                            return;
                        }
                        finally {
                            if (httpurlconnection != null)
                            {
                                httpurlconnection.disconnect();
                            }

                            this.loadingFinished();
                        }
                    }
                }, this.getExecutor());
            }
        }
    }

    @Nullable
    private NativeImage load(InputStream p_118019_)
    {
        NativeImage nativeimage = null;

        try
        {
            nativeimage = NativeImage.read(p_118019_);

            if (this.processLegacySkin)
            {
                nativeimage = this.processLegacySkin(nativeimage);
            }
        }
        catch (Exception exception)
        {
            LOGGER.warn("Error while loading the skin texture", (Throwable)exception);
        }

        return nativeimage;
    }

    private boolean shouldPipeline()
    {
        if (!this.pipeline)
        {
            return false;
        }
        else
        {
            Proxy proxy = Minecraft.getInstance().getProxy();
            return proxy.type() != Type.DIRECT && proxy.type() != Type.SOCKS ? false : this.urlString.startsWith("http://");
        }
    }

    private void loadPipelined()
    {
        try
        {
            HttpRequest httprequest = HttpPipeline.makeRequest(this.urlString, Minecraft.getInstance().getProxy());
            HttpResponse httpresponse = HttpPipeline.executeRequest(httprequest);

            if (httpresponse.getStatus() / 100 == 2)
            {
                byte[] abyte = httpresponse.getBody();
                ByteArrayInputStream bytearrayinputstream = new ByteArrayInputStream(abyte);
                NativeImage nativeimage;

                if (this.file != null)
                {
                    FileUtils.copyInputStreamToFile(bytearrayinputstream, this.file);
                    nativeimage = NativeImage.read(new FileInputStream(this.file));
                }
                else
                {
                    nativeimage = NativeImage.read(bytearrayinputstream);
                }

                this.loadCallback(nativeimage);
                this.uploadPending = true;
                return;
            }
        }
        catch (Exception exception)
        {
            LOGGER.error("Couldn't download http texture: " + exception.getClass().getName() + ": " + exception.getMessage());
            return;
        }
        finally
        {
            this.loadingFinished();
        }
    }

    private void loadingFinished()
    {
        if (!this.uploadPending)
        {
            if (this.onDownloaded instanceof CapeImageBuffer capeimagebuffer)
            {
                capeimagebuffer.cleanup();
            }
        }
    }

    public Runnable getProcessTask()
    {
        return this.onDownloaded;
    }

    private Executor getExecutor()
    {
        return this.urlString.startsWith("http://s.optifine.net") ? Util.getCapeExecutor() : Util.backgroundExecutor();
    }

    @Nullable
    private NativeImage processLegacySkin(NativeImage p_118033_)
    {
        int i = p_118033_.getHeight();
        int j = p_118033_.getWidth();

        if (j == 64 && (i == 32 || i == 64))
        {
            boolean flag = i == 32;

            if (flag)
            {
                NativeImage nativeimage = new NativeImage(64, 64, true);
                nativeimage.copyFrom(p_118033_);
                p_118033_.close();
                p_118033_ = nativeimage;
                nativeimage.fillRect(0, 32, 64, 32, 0);
                nativeimage.copyRect(4, 16, 16, 32, 4, 4, true, false);
                nativeimage.copyRect(8, 16, 16, 32, 4, 4, true, false);
                nativeimage.copyRect(0, 20, 24, 32, 4, 12, true, false);
                nativeimage.copyRect(4, 20, 16, 32, 4, 12, true, false);
                nativeimage.copyRect(8, 20, 8, 32, 4, 12, true, false);
                nativeimage.copyRect(12, 20, 16, 32, 4, 12, true, false);
                nativeimage.copyRect(44, 16, -8, 32, 4, 4, true, false);
                nativeimage.copyRect(48, 16, -8, 32, 4, 4, true, false);
                nativeimage.copyRect(40, 20, 0, 32, 4, 12, true, false);
                nativeimage.copyRect(44, 20, -8, 32, 4, 12, true, false);
                nativeimage.copyRect(48, 20, -16, 32, 4, 12, true, false);
                nativeimage.copyRect(52, 20, -8, 32, 4, 12, true, false);
            }

            setNoAlpha(p_118033_, 0, 0, 32, 16);

            if (flag)
            {
                doNotchTransparencyHack(p_118033_, 32, 0, 64, 32);
            }

            setNoAlpha(p_118033_, 0, 16, 64, 32);
            setNoAlpha(p_118033_, 16, 48, 48, 64);
            return p_118033_;
        }
        else
        {
            p_118033_.close();
            LOGGER.warn("Discarding incorrectly sized ({}x{}) skin texture from {}", j, i, this.urlString);
            return null;
        }
    }

    private static void doNotchTransparencyHack(NativeImage p_118013_, int p_118014_, int p_118015_, int p_118016_, int p_118017_)
    {
        for (int i = p_118014_; i < p_118016_; i++)
        {
            for (int j = p_118015_; j < p_118017_; j++)
            {
                int k = p_118013_.getPixelRGBA(i, j);

                if ((k >> 24 & 0xFF) < 128)
                {
                    return;
                }
            }
        }

        for (int l = p_118014_; l < p_118016_; l++)
        {
            for (int i1 = p_118015_; i1 < p_118017_; i1++)
            {
                p_118013_.setPixelRGBA(l, i1, p_118013_.getPixelRGBA(l, i1) & 16777215);
            }
        }
    }

    private static void setNoAlpha(NativeImage p_118023_, int p_118024_, int p_118025_, int p_118026_, int p_118027_)
    {
        for (int i = p_118024_; i < p_118026_; i++)
        {
            for (int j = p_118025_; j < p_118027_; j++)
            {
                p_118023_.setPixelRGBA(i, j, p_118023_.getPixelRGBA(i, j) | 0xFF000000);
            }
        }
    }
}
