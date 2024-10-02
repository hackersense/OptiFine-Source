package net.optifine.render;

import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.vertex.BufferBuilder;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

public class RenderCache implements IBufferSourceListener
{
    private long cacheTimeMs;
    private long updateTimeMs;
    private Map<RenderType, ByteBuffer> renderTypeBuffers = new LinkedHashMap<>();
    private Deque<ByteBuffer> freeBuffers = new ArrayDeque<>();

    public RenderCache(long cacheTimeMs)
    {
        this.cacheTimeMs = cacheTimeMs;
    }

    public boolean drawCached(GuiGraphics graphicsIn)
    {
        if (System.currentTimeMillis() > this.updateTimeMs)
        {
            graphicsIn.flush();

            for (ByteBuffer bytebuffer1 : this.renderTypeBuffers.values())
            {
                this.freeBuffers.add(bytebuffer1);
            }

            this.renderTypeBuffers.clear();
            this.updateTimeMs = System.currentTimeMillis() + this.cacheTimeMs;
            return false;
        }
        else
        {
            for (RenderType rendertype : this.renderTypeBuffers.keySet())
            {
                ByteBuffer bytebuffer = this.renderTypeBuffers.get(rendertype);
                graphicsIn.putBulkData(rendertype, bytebuffer);
                bytebuffer.rewind();
            }

            graphicsIn.flush();
            return true;
        }
    }

    public void startRender(GuiGraphics graphicsIn)
    {
        graphicsIn.bufferSource().addListener(this);
    }

    public void stopRender(GuiGraphics graphicsIn)
    {
        graphicsIn.flush();
        graphicsIn.bufferSource().removeListener(this);
    }

    @Override
    public void finish(RenderType renderTypeIn, BufferBuilder bufferIn)
    {
        ByteBuffer bytebuffer = this.renderTypeBuffers.get(renderTypeIn);

        if (bytebuffer == null)
        {
            bytebuffer = this.allocateByteBuffer(524288);
            this.renderTypeBuffers.put(renderTypeIn, bytebuffer);
        }

        bytebuffer.position(bytebuffer.limit());
        bytebuffer.limit(bytebuffer.capacity());
        bufferIn.getBulkData(bytebuffer);
        bytebuffer.flip();
    }

    private ByteBuffer allocateByteBuffer(int size)
    {
        ByteBuffer bytebuffer = this.freeBuffers.pollLast();

        if (bytebuffer == null)
        {
            bytebuffer = GlUtil.allocateMemory(size);
        }

        bytebuffer.position(0);
        bytebuffer.limit(0);
        return bytebuffer;
    }
}
