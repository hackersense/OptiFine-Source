package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.optifine.Config;
import net.optifine.EmissiveTextures;
import net.optifine.shaders.ShadersTex;
import org.slf4j.Logger;

public class SimpleTexture extends AbstractTexture
{
    static final Logger LOGGER = LogUtils.getLogger();
    protected final ResourceLocation location;
    private ResourceManager resourceManager;
    public ResourceLocation locationEmissive;
    public boolean isEmissive;
    public long size;

    public SimpleTexture(ResourceLocation p_118133_)
    {
        this.location = p_118133_;
    }

    @Override
    public void load(ResourceManager p_118135_) throws IOException
    {
        this.resourceManager = p_118135_;
        SimpleTexture.TextureImage simpletexture$textureimage = this.getTextureImage(p_118135_);
        simpletexture$textureimage.throwIfError();
        TextureMetadataSection texturemetadatasection = simpletexture$textureimage.getTextureMetadata();
        boolean flag;
        boolean flag1;

        if (texturemetadatasection != null)
        {
            flag = texturemetadatasection.isBlur();
            flag1 = texturemetadatasection.isClamp();
        }
        else
        {
            flag = false;
            flag1 = false;
        }

        NativeImage nativeimage = simpletexture$textureimage.getImage();

        if (!RenderSystem.isOnRenderThreadOrInit())
        {
            RenderSystem.recordRenderCall(() -> this.doLoad(nativeimage, flag, flag1));
        }
        else
        {
            this.doLoad(nativeimage, flag, flag1);
        }
    }

    private void doLoad(NativeImage p_118137_, boolean p_118138_, boolean p_118139_)
    {
        TextureUtil.prepareImage(this.getId(), 0, p_118137_.getWidth(), p_118137_.getHeight());
        p_118137_.upload(0, 0, 0, 0, 0, p_118137_.getWidth(), p_118137_.getHeight(), p_118138_, p_118139_, false, true);

        if (Config.isShaders())
        {
            ShadersTex.loadSimpleTextureNS(this.getId(), p_118137_, p_118138_, p_118139_, this.resourceManager, this.location, this.getMultiTexID());
        }

        if (EmissiveTextures.isActive())
        {
            EmissiveTextures.loadTexture(this.location, this);
        }

        this.size = p_118137_.getSize();
    }

    protected SimpleTexture.TextureImage getTextureImage(ResourceManager p_118140_)
    {
        return SimpleTexture.TextureImage.load(p_118140_, this.location);
    }

    protected static class TextureImage implements Closeable
    {
        @Nullable
        private final TextureMetadataSection metadata;
        @Nullable
        private final NativeImage image;
        @Nullable
        private final IOException exception;

        public TextureImage(IOException p_118153_)
        {
            this.exception = p_118153_;
            this.metadata = null;
            this.image = null;
        }

        public TextureImage(@Nullable TextureMetadataSection p_118150_, NativeImage p_118151_)
        {
            this.exception = null;
            this.metadata = p_118150_;
            this.image = p_118151_;
        }

        public static SimpleTexture.TextureImage load(ResourceManager p_118156_, ResourceLocation p_118157_)
        {
            try
            {
                Resource resource = p_118156_.getResourceOrThrow(p_118157_);
                NativeImage nativeimage;

                try (InputStream inputstream = resource.open())
                {
                    nativeimage = NativeImage.read(inputstream);
                }

                TextureMetadataSection texturemetadatasection = null;

                try
                {
                    texturemetadatasection = resource.metadata().getSection(TextureMetadataSection.SERIALIZER).orElse(null);
                }
                catch (RuntimeException runtimeexception)
                {
                    SimpleTexture.LOGGER.warn("Failed reading metadata of: {}", p_118157_, runtimeexception);
                }

                return new SimpleTexture.TextureImage(texturemetadatasection, nativeimage);
            }
            catch (IOException ioexception1)
            {
                return new SimpleTexture.TextureImage(ioexception1);
            }
        }

        @Nullable
        public TextureMetadataSection getTextureMetadata()
        {
            return this.metadata;
        }

        public NativeImage getImage() throws IOException
        {
            if (this.exception != null)
            {
                throw this.exception;
            }
            else
            {
                return this.image;
            }
        }

        @Override
        public void close()
        {
            if (this.image != null)
            {
                this.image.close();
            }
        }

        public void throwIfError() throws IOException
        {
            if (this.exception != null)
            {
                throw this.exception;
            }
        }
    }
}
