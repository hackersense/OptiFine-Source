package net.minecraft.client.renderer.texture;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraftforge.client.textures.ForgeTextureMetadata;
import net.optifine.SmartAnimations;
import net.optifine.texture.ColorBlenderKeepAlpha;
import net.optifine.texture.IColorBlender;
import net.optifine.util.TextureUtils;
import org.slf4j.Logger;

public class SpriteContents implements Stitcher.Entry, AutoCloseable
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ResourceLocation name;
    int width;
    int height;
    private NativeImage originalImage;
    NativeImage[] byMipLevel;
    @Nullable
    private final SpriteContents.AnimatedTexture animatedTexture;
    private final ResourceMetadata metadata;
    private double scaleFactor = 1.0;
    private TextureAtlasSprite sprite;
    public final ForgeTextureMetadata forgeMeta;

    public SpriteContents(ResourceLocation p_249787_, FrameSize p_251031_, NativeImage p_252131_, ResourceMetadata p_299427_)
    {
        this(p_249787_, p_251031_, p_252131_, p_299427_, null);
    }

    public SpriteContents(ResourceLocation nameIn, FrameSize sizeIn, NativeImage imageIn, ResourceMetadata metadataIn, ForgeTextureMetadata forgeMeta)
    {
        this.name = nameIn;
        this.width = sizeIn.width();
        this.height = sizeIn.height();
        this.metadata = metadataIn;
        AnimationMetadataSection animationmetadatasection = metadataIn.getSection(AnimationMetadataSection.SERIALIZER).orElse(AnimationMetadataSection.EMPTY);
        this.animatedTexture = this.createAnimatedTexture(sizeIn, imageIn.getWidth(), imageIn.getHeight(), animationmetadatasection);
        this.originalImage = imageIn;
        this.byMipLevel = new NativeImage[] {this.originalImage};
        this.forgeMeta = forgeMeta;
    }

    public void increaseMipLevel(int p_248864_)
    {
        IColorBlender icolorblender = null;

        if (this.sprite != null)
        {
            icolorblender = this.sprite.getTextureAtlas().getShadersColorBlender(this.sprite.spriteShadersType);

            if (this.sprite.spriteShadersType == null)
            {
                if (!this.name.getPath().endsWith("_leaves"))
                {
                    TextureAtlasSprite.fixTransparentColor(this.originalImage);
                    this.byMipLevel[0] = this.originalImage;
                }

                if (icolorblender == null && this.name.getPath().endsWith("glass_pane_top"))
                {
                    icolorblender = new ColorBlenderKeepAlpha();
                }
            }
        }

        try
        {
            this.byMipLevel = MipmapGenerator.generateMipLevels(this.byMipLevel, p_248864_, icolorblender);
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Generating mipmaps for frame");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Sprite being mipmapped");
            crashreportcategory.setDetail("First frame", () ->
            {
                StringBuilder stringbuilder = new StringBuilder();

                if (stringbuilder.length() > 0)
                {
                    stringbuilder.append(", ");
                }

                stringbuilder.append(this.originalImage.getWidth()).append("x").append(this.originalImage.getHeight());
                return stringbuilder.toString();
            });
            CrashReportCategory crashreportcategory1 = crashreport.addCategory("Frame being iterated");
            crashreportcategory1.setDetail("Sprite name", this.name);
            crashreportcategory1.setDetail("Sprite size", () -> this.width + " x " + this.height);
            crashreportcategory1.setDetail("Sprite frames", () -> this.getFrameCount() + " frames");
            crashreportcategory1.setDetail("Mipmap levels", p_248864_);
            throw new ReportedException(crashreport);
        }
    }

    private int getFrameCount()
    {
        return this.animatedTexture != null ? this.animatedTexture.frames.size() : 1;
    }

    @Nullable
    private SpriteContents.AnimatedTexture createAnimatedTexture(FrameSize p_250817_, int p_249792_, int p_252353_, AnimationMetadataSection p_250947_)
    {
        int i = p_249792_ / p_250817_.width();
        int j = p_252353_ / p_250817_.height();
        int k = i * j;
        List<SpriteContents.FrameInfo> list = new ArrayList<>();
        p_250947_.forEachFrame((indexIn, timeIn) -> list.add(new SpriteContents.FrameInfo(indexIn, timeIn)));

        if (list.isEmpty())
        {
            for (int l = 0; l < k; l++)
            {
                list.add(new SpriteContents.FrameInfo(l, p_250947_.getDefaultFrameTime()));
            }
        }
        else
        {
            int i1 = 0;
            IntSet intset = new IntOpenHashSet();

            for (Iterator<SpriteContents.FrameInfo> iterator = list.iterator(); iterator.hasNext(); i1++)
            {
                SpriteContents.FrameInfo spritecontents$frameinfo = iterator.next();
                boolean flag = true;

                if (spritecontents$frameinfo.time <= 0)
                {
                    LOGGER.warn("Invalid frame duration on sprite {} frame {}: {}", this.name, i1, spritecontents$frameinfo.time);
                    flag = false;
                }

                if (spritecontents$frameinfo.index < 0 || spritecontents$frameinfo.index >= k)
                {
                    LOGGER.warn("Invalid frame index on sprite {} frame {}: {}", this.name, i1, spritecontents$frameinfo.index);
                    flag = false;
                }

                if (flag)
                {
                    intset.add(spritecontents$frameinfo.index);
                }
                else
                {
                    iterator.remove();
                }
            }

            int[] aint = IntStream.range(0, k).filter(indexIn -> !intset.contains(indexIn)).toArray();

            if (aint.length > 0)
            {
                LOGGER.warn("Unused frames in sprite {}: {}", this.name, Arrays.toString(aint));
            }
        }

        return list.size() <= 1 ? null : new SpriteContents.AnimatedTexture(ImmutableList.copyOf(list), i, p_250947_.isInterpolatedFrames());
    }

    void upload(int p_248895_, int p_250245_, int p_250458_, int p_251337_, NativeImage[] p_248825_)
    {
        for (int i = 0; i < this.byMipLevel.length && this.width >> i > 0 && this.height >> i > 0; i++)
        {
            p_248825_[i]
            .upload(
                i,
                p_248895_ >> i,
                p_250245_ >> i,
                p_250458_ >> i,
                p_251337_ >> i,
                this.width >> i,
                this.height >> i,
                this.byMipLevel.length > 1,
                false
            );
        }
    }

    @Override
    public int width()
    {
        return this.width;
    }

    @Override
    public int height()
    {
        return this.height;
    }

    @Override
    public ResourceLocation name()
    {
        return this.name;
    }

    public IntStream getUniqueFrames()
    {
        return this.animatedTexture != null ? this.animatedTexture.getUniqueFrames() : IntStream.of(1);
    }

    @Nullable
    public SpriteTicker createTicker()
    {
        return this.animatedTexture != null ? this.animatedTexture.createTicker() : null;
    }

    public ResourceMetadata metadata()
    {
        return this.metadata;
    }

    @Override
    public void close()
    {
        for (NativeImage nativeimage : this.byMipLevel)
        {
            nativeimage.close();
        }
    }

    @Override
    public String toString()
    {
        return "SpriteContents{name=" + this.name + ", frameCount=" + this.getFrameCount() + ", height=" + this.height + ", width=" + this.width + "}";
    }

    public boolean isTransparent(int p_250374_, int p_250934_, int p_249573_)
    {
        int i = p_250934_;
        int j = p_249573_;

        if (this.animatedTexture != null)
        {
            i = p_250934_ + this.animatedTexture.getFrameX(p_250374_) * this.width;
            j = p_249573_ + this.animatedTexture.getFrameY(p_250374_) * this.height;
        }

        return (this.originalImage.getPixelRGBA(i, j) >> 24 & 0xFF) == 0;
    }

    public void uploadFirstFrame(int p_252315_, int p_248634_)
    {
        if (this.animatedTexture != null)
        {
            this.animatedTexture.uploadFirstFrame(p_252315_, p_248634_);
        }
        else
        {
            this.upload(p_252315_, p_248634_, 0, 0, this.byMipLevel);
        }
    }

    public int getSpriteWidth()
    {
        return this.width;
    }

    public int getSpriteHeight()
    {
        return this.height;
    }

    public ResourceLocation getSpriteLocation()
    {
        return this.name;
    }

    public void setSpriteWidth(int spriteWidth)
    {
        this.width = spriteWidth;
    }

    public void setSpriteHeight(int spriteHeight)
    {
        this.height = spriteHeight;
    }

    public double getScaleFactor()
    {
        return this.scaleFactor;
    }

    public void setScaleFactor(double scaleFactor)
    {
        this.scaleFactor = scaleFactor;
    }

    public void rescale()
    {
        if (this.scaleFactor > 1.0)
        {
            int i = (int)Math.round((double)this.originalImage.getWidth() * this.scaleFactor);
            NativeImage nativeimage = TextureUtils.scaleImage(this.originalImage, i);

            if (nativeimage != this.originalImage)
            {
                this.originalImage.close();
                this.originalImage = nativeimage;
                this.byMipLevel[0] = this.originalImage;
            }
        }
    }

    public SpriteContents.AnimatedTexture getAnimatedTexture()
    {
        return this.animatedTexture;
    }

    public NativeImage getOriginalImage()
    {
        return this.originalImage;
    }

    public TextureAtlasSprite getSprite()
    {
        return this.sprite;
    }

    public void setSprite(TextureAtlasSprite sprite)
    {
        this.sprite = sprite;
    }

    class AnimatedTexture
    {
        final List<SpriteContents.FrameInfo> frames;
        final int frameRowSize;
        final boolean interpolateFrames;

        AnimatedTexture(final List<SpriteContents.FrameInfo> p_250968_, final int p_251686_, final boolean p_251832_)
        {
            this.frames = p_250968_;
            this.frameRowSize = p_251686_;
            this.interpolateFrames = p_251832_;
        }

        int getFrameX(int p_249475_)
        {
            return p_249475_ % this.frameRowSize;
        }

        int getFrameY(int p_251327_)
        {
            return p_251327_ / this.frameRowSize;
        }

        void uploadFrame(int p_250449_, int p_248877_, int p_249060_)
        {
            int i = this.getFrameX(p_249060_) * SpriteContents.this.width;
            int j = this.getFrameY(p_249060_) * SpriteContents.this.height;
            SpriteContents.this.upload(p_250449_, p_248877_, i, j, SpriteContents.this.byMipLevel);
        }

        public SpriteTicker createTicker()
        {
            return SpriteContents.this.new Ticker(this, this.interpolateFrames ? SpriteContents.this.new InterpolationData() : null);
        }

        public void uploadFirstFrame(int p_251807_, int p_248676_)
        {
            this.uploadFrame(p_251807_, p_248676_, this.frames.get(0).index);
        }

        public IntStream getUniqueFrames()
        {
            return this.frames.stream().mapToInt(infoIn -> infoIn.index).distinct();
        }
    }

    static class FrameInfo
    {
        final int index;
        final int time;

        FrameInfo(int p_248909_, int p_250552_)
        {
            this.index = p_248909_;
            this.time = p_250552_;
        }
    }

    final class InterpolationData implements AutoCloseable
    {
        private final NativeImage[] activeFrame = new NativeImage[SpriteContents.this.byMipLevel.length];

        InterpolationData()
        {
            for (int i = 0; i < this.activeFrame.length; i++)
            {
                int j = SpriteContents.this.width >> i;
                int k = SpriteContents.this.height >> i;
                j = Math.max(1, j);
                k = Math.max(1, k);
                this.activeFrame[i] = new NativeImage(j, k, false);
            }
        }

        void uploadInterpolatedFrame(int p_250513_, int p_251644_, SpriteContents.Ticker p_248626_)
        {
            SpriteContents.AnimatedTexture spritecontents$animatedtexture = p_248626_.animationInfo;
            List<SpriteContents.FrameInfo> list = spritecontents$animatedtexture.frames;
            SpriteContents.FrameInfo spritecontents$frameinfo = list.get(p_248626_.frame);
            double d0 = 1.0 - (double)p_248626_.subFrame / (double)spritecontents$frameinfo.time;
            int i = spritecontents$frameinfo.index;
            int j = list.get((p_248626_.frame + 1) % list.size()).index;

            if (i != j)
            {
                for (int k = 0; k < this.activeFrame.length; k++)
                {
                    int l = SpriteContents.this.width >> k;
                    int i1 = SpriteContents.this.height >> k;

                    if (l >= 1 && i1 >= 1)
                    {
                        for (int j1 = 0; j1 < i1; j1++)
                        {
                            for (int k1 = 0; k1 < l; k1++)
                            {
                                int l1 = this.getPixel(spritecontents$animatedtexture, i, k, k1, j1);
                                int i2 = this.getPixel(spritecontents$animatedtexture, j, k, k1, j1);
                                int j2 = this.mix(d0, l1 >> 16 & 0xFF, i2 >> 16 & 0xFF);
                                int k2 = this.mix(d0, l1 >> 8 & 0xFF, i2 >> 8 & 0xFF);
                                int l2 = this.mix(d0, l1 & 0xFF, i2 & 0xFF);
                                this.activeFrame[k].setPixelRGBA(k1, j1, l1 & 0xFF000000 | j2 << 16 | k2 << 8 | l2);
                            }
                        }
                    }
                }

                SpriteContents.this.upload(p_250513_, p_251644_, 0, 0, this.activeFrame);
            }
        }

        private int getPixel(SpriteContents.AnimatedTexture p_251976_, int p_250761_, int p_250049_, int p_250004_, int p_251489_)
        {
            return SpriteContents.this.byMipLevel[p_250049_]
                   .getPixelRGBA(
                       p_250004_ + (p_251976_.getFrameX(p_250761_) * SpriteContents.this.width >> p_250049_),
                       p_251489_ + (p_251976_.getFrameY(p_250761_) * SpriteContents.this.height >> p_250049_)
                   );
        }

        private int mix(double p_250974_, int p_252151_, int p_249832_)
        {
            return (int)(p_250974_ * (double)p_252151_ + (1.0 - p_250974_) * (double)p_249832_);
        }

        @Override
        public void close()
        {
            for (NativeImage nativeimage : this.activeFrame)
            {
                nativeimage.close();
            }
        }
    }

    class Ticker implements SpriteTicker
    {
        int frame;
        int subFrame;
        final SpriteContents.AnimatedTexture animationInfo;
        @Nullable
        private final SpriteContents.InterpolationData interpolationData;
        protected boolean animationActive = false;
        protected TextureAtlasSprite sprite;

        Ticker(@Nullable final SpriteContents.AnimatedTexture p_249618_, final SpriteContents.InterpolationData p_251097_)
        {
            this.animationInfo = p_249618_;
            this.interpolationData = p_251097_;
        }

        @Override
        public void tickAndUpload(int p_249105_, int p_249676_)
        {
            this.animationActive = SmartAnimations.isActive() ? SmartAnimations.isSpriteRendered(this.sprite) : true;

            if (this.animationInfo.frames.size() <= 1)
            {
                this.animationActive = false;
            }

            this.subFrame++;
            SpriteContents.FrameInfo spritecontents$frameinfo = this.animationInfo.frames.get(this.frame);

            if (this.subFrame >= spritecontents$frameinfo.time)
            {
                int i = spritecontents$frameinfo.index;
                this.frame = (this.frame + 1) % this.animationInfo.frames.size();
                this.subFrame = 0;
                int j = this.animationInfo.frames.get(this.frame).index;

                if (!this.animationActive)
                {
                    return;
                }

                if (i != j)
                {
                    this.animationInfo.uploadFrame(p_249105_, p_249676_, j);
                }
            }
            else if (this.interpolationData != null)
            {
                if (!this.animationActive)
                {
                    return;
                }

                if (!RenderSystem.isOnRenderThread())
                {
                    RenderSystem.recordRenderCall(() -> this.interpolationData.uploadInterpolatedFrame(p_249105_, p_249676_, this));
                }
                else
                {
                    this.interpolationData.uploadInterpolatedFrame(p_249105_, p_249676_, this);
                }
            }
        }

        @Override
        public void close()
        {
            if (this.interpolationData != null)
            {
                this.interpolationData.close();
            }
        }

        @Override
        public TextureAtlasSprite getSprite()
        {
            return this.sprite;
        }

        @Override
        public void setSprite(TextureAtlasSprite sprite)
        {
            this.sprite = sprite;
        }

        @Override
        public String toString()
        {
            return "animation:" + SpriteContents.this.toString();
        }
    }
}
