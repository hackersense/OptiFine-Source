package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.optifine.Config;
import net.optifine.render.MultiTextureData;
import net.optifine.render.MultiTextureRenderer;
import net.optifine.render.VboRange;
import net.optifine.render.VboRegion;
import net.optifine.shaders.SVertexBuilder;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import net.optifine.util.GpuMemory;
import org.joml.Matrix4f;

public class VertexBuffer implements AutoCloseable
{
    private final VertexBuffer.Usage usage;
    private int vertexBufferId;
    private int indexBufferId;
    private int arrayObjectId;
    @Nullable
    private VertexFormat format;
    @Nullable
    private RenderSystem.AutoStorageIndexBuffer sequentialIndices;
    private VertexFormat.IndexType indexType;
    private int indexCount;
    private VertexFormat.Mode mode;
    private VboRegion vboRegion;
    private VboRange vboRange;
    private MultiTextureData multiTextureData;
    private static ByteBuffer emptyBuffer = GlUtil.allocateMemory(0);

    public VertexBuffer(VertexBuffer.Usage p_286252_)
    {
        this.usage = p_286252_;
        RenderSystem.assertOnRenderThread();
        this.vertexBufferId = GlStateManager._glGenBuffers();
        this.indexBufferId = GlStateManager._glGenBuffers();
        this.arrayObjectId = GlStateManager._glGenVertexArrays();
    }

    public void upload(MeshData p_345178_)
    {
        MeshData meshdata = p_345178_;
        label48:
        {
            try
            {
                if (this.isInvalid())
                {
                    break label48;
                }

                RenderSystem.assertOnRenderThread();
                GpuMemory.bufferFreed((long)this.getVertexBufferSize());
                GpuMemory.bufferFreed((long)this.getIndexBufferSize());
                MeshData.DrawState meshdata$drawstate = p_345178_.drawState();
                this.format = this.uploadVertexBuffer(meshdata$drawstate, p_345178_.vertexBuffer());
                this.sequentialIndices = this.uploadIndexBuffer(meshdata$drawstate, p_345178_.indexBuffer());
                this.indexCount = meshdata$drawstate.indexCount();
                this.indexType = meshdata$drawstate.indexType();
                this.mode = meshdata$drawstate.mode();
                GpuMemory.bufferAllocated((long)this.getVertexBufferSize());
                GpuMemory.bufferAllocated((long)this.getIndexBufferSize());

                if (this.vboRegion != null)
                {
                    ByteBuffer bytebuffer = p_345178_.vertexBuffer();
                    bytebuffer.position(0);
                    bytebuffer.limit(meshdata$drawstate.getVertexBufferSize());
                    this.vboRegion.bufferData(bytebuffer, this.vboRange);
                    bytebuffer.position(0);
                    bytebuffer.limit(meshdata$drawstate.getVertexBufferSize());
                }

                this.multiTextureData = p_345178_.getMultiTextureData();
                this.updateMultiTextureData();
            }
            catch (Throwable throwable11)
            {
                if (p_345178_ != null)
                {
                    try
                    {
                        meshdata.close();
                    }
                    catch (Throwable throwable)
                    {
                        throwable11.addSuppressed(throwable);
                    }
                }

                throw throwable11;
            }

            if (p_345178_ != null)
            {
                p_345178_.close();
            }

            return;
        }

        if (p_345178_ != null)
        {
            p_345178_.close();
        }
    }

    public void uploadIndexBuffer(ByteBufferBuilder.Result p_343348_)
    {
        ByteBufferBuilder.Result bytebufferbuilder$result = p_343348_;
        label42:
        {
            try
            {
                if (this.isInvalid())
                {
                    break label42;
                }

                RenderSystem.assertOnRenderThread();
                GlStateManager._glBindBuffer(34963, this.indexBufferId);
                RenderSystem.glBufferData(34963, p_343348_.byteBuffer(), this.usage.id);
                this.sequentialIndices = null;
                this.updateMultiTextureData();
            }
            catch (Throwable throwable1)
            {
                if (p_343348_ != null)
                {
                    try
                    {
                        bytebufferbuilder$result.close();
                    }
                    catch (Throwable throwable)
                    {
                        throwable1.addSuppressed(throwable);
                    }
                }

                throw throwable1;
            }

            if (p_343348_ != null)
            {
                p_343348_.close();
            }

            return;
        }

        if (p_343348_ != null)
        {
            p_343348_.close();
        }
    }

    private VertexFormat uploadVertexBuffer(MeshData.DrawState p_342212_, @Nullable ByteBuffer p_231220_)
    {
        if (this.vboRegion != null)
        {
            return p_342212_.format();
        }
        else
        {
            boolean flag = false;

            if (!p_342212_.format().equals(this.format))
            {
                if (this.format != null)
                {
                    this.format.clearBufferState();
                }

                GlStateManager._glBindBuffer(34962, this.vertexBufferId);
                p_342212_.format().setupBufferState();

                if (Config.isShaders())
                {
                    ShadersRender.setupArrayPointersVbo();
                }

                flag = true;
            }

            if (p_231220_ != null)
            {
                if (!flag)
                {
                    GlStateManager._glBindBuffer(34962, this.vertexBufferId);
                }

                RenderSystem.glBufferData(34962, p_231220_, this.usage.id);
            }

            return p_342212_.format();
        }
    }

    @Nullable
    private RenderSystem.AutoStorageIndexBuffer uploadIndexBuffer(MeshData.DrawState p_345013_, @Nullable ByteBuffer p_231225_)
    {
        if (p_231225_ != null)
        {
            if (this.vboRegion != null)
            {
                return null;
            }
            else
            {
                GlStateManager._glBindBuffer(34963, this.indexBufferId);
                RenderSystem.glBufferData(34963, p_231225_, this.usage.id);
                return null;
            }
        }
        else
        {
            RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(p_345013_.mode());
            int i = p_345013_.indexCount();

            if (this.vboRegion != null && p_345013_.mode() == VertexFormat.Mode.QUADS)
            {
                i = 65536;
            }

            if (rendersystem$autostorageindexbuffer != this.sequentialIndices || !rendersystem$autostorageindexbuffer.hasStorage(i))
            {
                rendersystem$autostorageindexbuffer.bind(i);
            }

            return rendersystem$autostorageindexbuffer;
        }
    }

    public void bind()
    {
        BufferUploader.invalidate();

        if (this.arrayObjectId >= 0)
        {
            GlStateManager._glBindVertexArray(this.arrayObjectId);
        }
    }

    public static void unbind()
    {
        BufferUploader.invalidate();
        GlStateManager._glBindVertexArray(0);
    }

    public void draw()
    {
        if (this.vboRegion != null)
        {
            this.vboRegion.drawArrays(VertexFormat.Mode.QUADS, this.vboRange);
        }
        else if (this.multiTextureData != null)
        {
            MultiTextureRenderer.draw(this.mode, this.getIndexType().asGLType, this.multiTextureData);
        }
        else
        {
            RenderSystem.drawElements(this.mode.asGLMode, this.indexCount, this.getIndexType().asGLType);
        }
    }

    private VertexFormat.IndexType getIndexType()
    {
        RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = this.sequentialIndices;
        return rendersystem$autostorageindexbuffer != null ? rendersystem$autostorageindexbuffer.type() : this.indexType;
    }

    public void drawWithShader(Matrix4f p_254480_, Matrix4f p_254555_, ShaderInstance p_253993_)
    {
        if (!RenderSystem.isOnRenderThread())
        {
            RenderSystem.recordRenderCall(() -> this._drawWithShader(new Matrix4f(p_254480_), new Matrix4f(p_254555_), p_253993_));
        }
        else
        {
            this._drawWithShader(p_254480_, p_254555_, p_253993_);
        }
    }

    private void _drawWithShader(Matrix4f p_253705_, Matrix4f p_253737_, ShaderInstance p_166879_)
    {
        p_166879_.setDefaultUniforms(this.mode, p_253705_, p_253737_, Minecraft.getInstance().getWindow());
        p_166879_.apply();
        boolean flag = Config.isShaders() && Shaders.isRenderingWorld;
        boolean flag1 = flag && SVertexBuilder.preDrawArrays(this.format);

        if (flag)
        {
            Shaders.setModelViewMatrix(p_253705_);
            Shaders.setProjectionMatrix(p_253737_);
            Shaders.setTextureMatrix(RenderSystem.getTextureMatrix());
            Shaders.setColorModulator(RenderSystem.getShaderColor());
        }

        this.draw();

        if (flag1)
        {
            SVertexBuilder.postDrawArrays();
        }

        p_166879_.clear();
    }

    @Override
    public void close()
    {
        if (this.vertexBufferId >= 0)
        {
            RenderSystem.glDeleteBuffers(this.vertexBufferId);
            this.vertexBufferId = -1;
            GpuMemory.bufferFreed((long)this.getVertexBufferSize());
        }

        if (this.indexBufferId >= 0)
        {
            RenderSystem.glDeleteBuffers(this.indexBufferId);
            this.indexBufferId = -1;
            GpuMemory.bufferFreed((long)this.getIndexBufferSize());
        }

        if (this.arrayObjectId >= 0)
        {
            RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
            this.arrayObjectId = -1;
        }

        this.indexCount = 0;
    }

    public VertexFormat getFormat()
    {
        return this.format;
    }

    public boolean isInvalid()
    {
        return this.vboRegion != null ? false : this.arrayObjectId == -1;
    }

    public void setVboRegion(VboRegion vboRegion)
    {
        if (vboRegion != null)
        {
            this.close();
            this.vboRegion = vboRegion;
            this.vboRange = new VboRange();
        }
    }

    public VboRegion getVboRegion()
    {
        return this.vboRegion;
    }

    public boolean isEmpty()
    {
        return this.indexCount <= 0;
    }

    public void clearData()
    {
        if (this.indexCount > 0)
        {
            if (this.vboRegion != null)
            {
                this.vboRegion.bufferData(emptyBuffer, this.vboRange);
                this.indexCount = 0;
            }
            else
            {
                this.bind();

                if (this.vertexBufferId >= 0)
                {
                    GlStateManager._glBindBuffer(34962, this.vertexBufferId);
                    GlStateManager._glBufferData(34962, 0L, this.usage.id);
                    GpuMemory.bufferFreed((long)this.getVertexBufferSize());
                }

                if (this.indexBufferId >= 0 && this.sequentialIndices == null)
                {
                    GlStateManager._glBindBuffer(34963, this.indexBufferId);
                    GlStateManager._glBufferData(34963, 0L, this.usage.id);
                    GpuMemory.bufferFreed((long)this.getIndexBufferSize());
                }

                unbind();
                this.indexCount = 0;
            }
        }
    }

    public int getIndexCount()
    {
        return this.indexCount;
    }

    private int getVertexBufferSize()
    {
        return this.format == null ? 0 : this.indexCount * this.format.getVertexSize();
    }

    private int getIndexBufferSize()
    {
        if (this.sequentialIndices != null)
        {
            return 0;
        }
        else
        {
            return this.indexType == null ? 0 : this.indexCount * this.indexType.bytes;
        }
    }

    public void updateMultiTextureData()
    {
        if (this.multiTextureData != null)
        {
            this.multiTextureData.applySort();
        }
    }

    public static enum Usage
    {
        STATIC(35044),
        DYNAMIC(35048);

        final int id;

        private Usage(final int p_286680_)
        {
            this.id = p_286680_;
        }
    }
}
