package net.minecraft.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.eventbus.api.Event;
import net.optifine.Config;
import net.optifine.reflect.Reflector;
import org.slf4j.Logger;

public class Screenshot
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String SCREENSHOT_DIR = "screenshots";
    private int rowHeight;
    private final DataOutputStream outputStream;
    private final byte[] bytes;
    private final int width;
    private final int height;
    private File file;

    public static void grab(File p_92290_, RenderTarget p_92293_, Consumer<Component> p_92294_)
    {
        grab(p_92290_, null, p_92293_, p_92294_);
    }

    public static void grab(File p_92296_, @Nullable String p_92297_, RenderTarget p_92300_, Consumer<Component> p_92301_)
    {
        if (!RenderSystem.isOnRenderThread())
        {
            RenderSystem.recordRenderCall(() -> _grab(p_92296_, p_92297_, p_92300_, p_92301_));
        }
        else
        {
            _grab(p_92296_, p_92297_, p_92300_, p_92301_);
        }
    }

    private static void _grab(File p_92306_, @Nullable String p_92307_, RenderTarget p_92310_, Consumer<Component> p_92311_)
    {
        Minecraft minecraft = Config.getMinecraft();
        Window window = minecraft.getWindow();
        Options options = Config.getGameSettings();
        int i = window.getWidth();
        int j = window.getHeight();
        int k = options.guiScale().get();
        int l = window.calculateScale(minecraft.options.guiScale().get(), minecraft.options.forceUnicodeFont().get());
        int i1 = Config.getScreenshotSize();
        boolean flag = GLX.isUsingFBOs() && i1 > 1;

        if (flag)
        {
            options.guiScale().set(l * i1);

            try
            {
                window.resizeFramebuffer(i * i1, j * i1);
            }
            catch (Exception exception)
            {
                LOGGER.warn("Couldn't save screenshot", (Throwable)exception);
                p_92311_.accept(Component.translatable("screenshot.failure", exception.getMessage()));
            }

            GlStateManager.clear(16640);
            minecraft.getMainRenderTarget().bindWrite(true);
            GlStateManager.enableTexture();
            RenderSystem.getModelViewStack().pushMatrix();
            minecraft.gameRenderer.render(minecraft.getTimer(), true);
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
        }

        NativeImage nativeimage = takeScreenshot(p_92310_);

        if (flag)
        {
            minecraft.getMainRenderTarget().unbindWrite();
            Config.getGameSettings().guiScale().set(k);
            window.resizeFramebuffer(i, j);
        }

        File file1 = new File(p_92306_, "screenshots");
        file1.mkdir();
        File file2;

        if (p_92307_ == null)
        {
            file2 = getFile(file1);
        }
        else
        {
            file2 = new File(file1, p_92307_);
        }

        Event event = null;

        if (Reflector.ForgeEventFactoryClient_onScreenshot.exists())
        {
            event = (Event)Reflector.call(Reflector.ForgeEventFactoryClient_onScreenshot, nativeimage, file2);

            if (event.isCanceled())
            {
                Component component = (Component)Reflector.call(event, Reflector.ScreenshotEvent_getCancelMessage);
                p_92311_.accept(component);
                return;
            }

            file2 = (File)Reflector.call(event, Reflector.ScreenshotEvent_getScreenshotFile);
        }

        File file3 = file2;
        Object object = event;
        Util.ioPool()
        .execute(
            () ->
        {
            try {
                nativeimage.writeToFile(file3);
                Component component1 = Component.literal(file3.getName())
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(styleIn -> styleIn.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file3.getAbsolutePath())));

                if (object != null && Reflector.call(object, Reflector.ScreenshotEvent_getResultMessage) != null)
                {
                    p_92311_.accept((Component)Reflector.call(object, Reflector.ScreenshotEvent_getResultMessage));
                }
                else {
                    p_92311_.accept(Component.translatable("screenshot.success", component1));
                }
            }
            catch (Exception exception1)
            {
                LOGGER.warn("Couldn't save screenshot", (Throwable)exception1);
                p_92311_.accept(Component.translatable("screenshot.failure", exception1.getMessage()));
            }
            finally {
                nativeimage.close();
            }
        }
        );
    }

    public static NativeImage takeScreenshot(RenderTarget p_92282_)
    {
        if (!GLX.isUsingFBOs())
        {
            NativeImage nativeimage1 = new NativeImage(p_92282_.width, p_92282_.height, false);
            nativeimage1.downloadFromFramebuffer();
            nativeimage1.flipY();
            return nativeimage1;
        }
        else
        {
            int i = p_92282_.width;
            int j = p_92282_.height;
            NativeImage nativeimage = new NativeImage(i, j, false);
            RenderSystem.bindTexture(p_92282_.getColorTextureId());
            nativeimage.downloadTexture(0, true);
            nativeimage.flipY();
            return nativeimage;
        }
    }

    private static File getFile(File p_92288_)
    {
        String s = Util.getFilenameFormattedDateTime();
        int i = 1;

        while (true)
        {
            File file1 = new File(p_92288_, s + (i == 1 ? "" : "_" + i) + ".png");

            if (!file1.exists())
            {
                return file1;
            }

            i++;
        }
    }

    public Screenshot(File p_168601_, int p_168602_, int p_168603_, int p_168604_) throws IOException
    {
        this.width = p_168602_;
        this.height = p_168603_;
        this.rowHeight = p_168604_;
        File file1 = new File(p_168601_, "screenshots");
        file1.mkdir();
        String s = "huge_" + Util.getFilenameFormattedDateTime();
        int i = 1;

        while ((this.file = new File(file1, s + (i == 1 ? "" : "_" + i) + ".tga")).exists())
        {
            i++;
        }

        byte[] abyte = new byte[18];
        abyte[2] = 2;
        abyte[12] = (byte)(p_168602_ % 256);
        abyte[13] = (byte)(p_168602_ / 256);
        abyte[14] = (byte)(p_168603_ % 256);
        abyte[15] = (byte)(p_168603_ / 256);
        abyte[16] = 24;
        this.bytes = new byte[p_168602_ * p_168604_ * 3];
        this.outputStream = new DataOutputStream(new FileOutputStream(this.file));
        this.outputStream.write(abyte);
    }

    public void addRegion(ByteBuffer p_168610_, int p_168611_, int p_168612_, int p_168613_, int p_168614_)
    {
        int i = p_168613_;
        int j = p_168614_;

        if (p_168613_ > this.width - p_168611_)
        {
            i = this.width - p_168611_;
        }

        if (p_168614_ > this.height - p_168612_)
        {
            j = this.height - p_168612_;
        }

        this.rowHeight = j;

        for (int k = 0; k < j; k++)
        {
            p_168610_.position((p_168614_ - j) * p_168613_ * 3 + k * p_168613_ * 3);
            int l = (p_168611_ + k * this.width) * 3;
            p_168610_.get(this.bytes, l, i * 3);
        }
    }

    public void saveRow() throws IOException
    {
        this.outputStream.write(this.bytes, 0, this.width * 3 * this.rowHeight);
    }

    public File close() throws IOException
    {
        this.outputStream.close();
        return this.file;
    }
}
