package net.optifine.render;

import com.mojang.blaze3d.vertex.VertexBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

public class ClearVertexBuffersTask implements Runnable
{
    List<VertexBuffer> listBuffers;

    public ClearVertexBuffersTask(List<VertexBuffer> listBuffers)
    {
        this.listBuffers = listBuffers;
    }

    @Override
    public void run()
    {
        for (int i = 0; i < this.listBuffers.size(); i++)
        {
            VertexBuffer vertexbuffer = this.listBuffers.get(i);
            vertexbuffer.clearData();
        }
    }

    @Override
    public String toString()
    {
        return this.listBuffers + "";
    }

    public static ClearVertexBuffersTask make(Set<RenderType> renderedLayers, SectionRenderDispatcher.RenderSection renderChunk)
    {
        List<VertexBuffer> list = null;

        for (RenderType rendertype : SectionRenderDispatcher.BLOCK_RENDER_LAYERS)
        {
            VertexBuffer vertexbuffer = renderChunk.getBuffer(rendertype);

            if (vertexbuffer != null && !vertexbuffer.isEmpty() && (renderedLayers == null || !renderedLayers.contains(rendertype)))
            {
                if (list == null)
                {
                    list = new ArrayList<>();
                }

                list.add(vertexbuffer);
            }
        }

        return list == null ? null : new ClearVertexBuffersTask(list);
    }

    public static CompletableFuture<Void> makeFuture(Set<RenderType> renderedLayers, SectionRenderDispatcher.RenderSection renderChunk, Executor executor)
    {
        ClearVertexBuffersTask clearvertexbufferstask = make(renderedLayers, renderChunk);
        return clearvertexbufferstask == null ? null : CompletableFuture.runAsync(() -> clearvertexbufferstask.run(), executor);
    }
}
