package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;

public class BufferUploader
{
    @Nullable
    private static VertexBuffer lastImmediateBuffer;

    public static void reset()
    {
        if (lastImmediateBuffer != null)
        {
            invalidate();
            VertexBuffer.unbind();
        }
    }

    public static void invalidate()
    {
        lastImmediateBuffer = null;
    }

    public static void drawWithShader(MeshData p_344650_)
    {
        if (!RenderSystem.isOnRenderThreadOrInit())
        {
            RenderSystem.recordRenderCall(() -> _drawWithShader(p_344650_));
        }
        else
        {
            _drawWithShader(p_344650_);
        }
    }

    private static void _drawWithShader(MeshData p_343117_)
    {
        VertexBuffer vertexbuffer = upload(p_343117_);
        vertexbuffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
    }

    public static void draw(MeshData p_342146_)
    {
        VertexBuffer vertexbuffer = upload(p_342146_);
        vertexbuffer.draw();
    }

    private static VertexBuffer upload(MeshData p_342083_)
    {
        RenderSystem.assertOnRenderThread();
        VertexBuffer vertexbuffer = bindImmediateBuffer(p_342083_.drawState().format());
        vertexbuffer.upload(p_342083_);
        return vertexbuffer;
    }

    private static VertexBuffer bindImmediateBuffer(VertexFormat p_231207_)
    {
        VertexBuffer vertexbuffer = p_231207_.getImmediateDrawVertexBuffer();
        bindImmediateBuffer(vertexbuffer);
        return vertexbuffer;
    }

    private static void bindImmediateBuffer(VertexBuffer p_231205_)
    {
        if (p_231205_ != lastImmediateBuffer)
        {
            p_231205_.bind();
            lastImmediateBuffer = p_231205_;
        }
    }
}
