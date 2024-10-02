package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.optifine.SmartAnimations;
import net.optifine.render.IBufferSourceListener;
import net.optifine.render.VertexBuilderDummy;
import net.optifine.util.TextureUtils;

public interface MultiBufferSource
{
    static MultiBufferSource.BufferSource immediate(ByteBufferBuilder p_344614_)
    {
        return immediateWithBuffers(Object2ObjectSortedMaps.<RenderType, ByteBufferBuilder>emptyMap(), p_344614_);
    }

    static MultiBufferSource.BufferSource immediateWithBuffers(SequencedMap<RenderType, ByteBufferBuilder> p_342750_, ByteBufferBuilder p_344601_)
    {
        return new MultiBufferSource.BufferSource(p_344601_, p_342750_);
    }

    VertexConsumer getBuffer(RenderType p_109903_);

default void flushRenderBuffers()
    {
    }

default void flushCache()
    {
    }

    public static class BufferSource implements MultiBufferSource
    {
        protected final ByteBufferBuilder sharedBuffer;
        protected final SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers;
        protected final Map<RenderType, BufferBuilder> startedBuilders = new HashMap<>();
        @Nullable
        protected RenderType lastSharedType;
        private final VertexConsumer DUMMY_BUFFER = new VertexBuilderDummy(this);
        private List<IBufferSourceListener> listeners = new ArrayList<>(4);
        private int maxCachedBuffers = 0;
        private Object2ObjectLinkedOpenHashMap<RenderType, BufferBuilder> cachedBuffers = new Object2ObjectLinkedOpenHashMap<>();
        private Deque<BufferBuilder> freeBufferBuilders = new ArrayDeque<>();

        protected BufferSource(ByteBufferBuilder p_344223_, SequencedMap<RenderType, ByteBufferBuilder> p_344104_)
        {
            this.sharedBuffer = p_344223_;
            this.fixedBuffers = new Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder>(p_344104_);
        }

        @Override
        public VertexConsumer getBuffer(RenderType p_109919_)
        {
            this.addCachedBuffer(p_109919_);
            BufferBuilder bufferbuilder = this.startedBuilders.get(p_109919_);

            if (bufferbuilder != null && !p_109919_.canConsolidateConsecutiveGeometry())
            {
                this.endBatch(p_109919_, bufferbuilder);
                bufferbuilder = null;
            }

            if (bufferbuilder != null)
            {
                return (VertexConsumer)(p_109919_.getTextureLocation() == TextureUtils.LOCATION_TEXTURE_EMPTY ? this.DUMMY_BUFFER : bufferbuilder);
            }
            else
            {
                ByteBufferBuilder bytebufferbuilder = (ByteBufferBuilder)this.fixedBuffers.get(p_109919_);

                if (bytebufferbuilder != null)
                {
                    bufferbuilder = new BufferBuilder(bytebufferbuilder, p_109919_.mode(), p_109919_.format(), p_109919_);
                }
                else
                {
                    if (this.lastSharedType != null)
                    {
                        this.endBatch(this.lastSharedType);
                    }

                    bufferbuilder = new BufferBuilder(this.sharedBuffer, p_109919_.mode(), p_109919_.format(), p_109919_);
                    this.lastSharedType = p_109919_;
                }

                this.startedBuilders.put(p_109919_, bufferbuilder);
                bufferbuilder.setRenderTypeBuffer(this);
                return (VertexConsumer)(p_109919_.getTextureLocation() == TextureUtils.LOCATION_TEXTURE_EMPTY ? this.DUMMY_BUFFER : bufferbuilder);
            }
        }

        public void endLastBatch()
        {
            if (this.lastSharedType != null)
            {
                this.endBatch(this.lastSharedType);
                this.lastSharedType = null;
            }
        }

        public void endBatch()
        {
            if (!this.startedBuilders.isEmpty())
            {
                this.endLastBatch();

                if (!this.startedBuilders.isEmpty())
                {
                    for (RenderType rendertype : this.fixedBuffers.keySet())
                    {
                        this.endBatch(rendertype);

                        if (this.startedBuilders.isEmpty())
                        {
                            break;
                        }
                    }
                }
            }
        }

        public void endBatch(RenderType p_109913_)
        {
            BufferBuilder bufferbuilder = this.startedBuilders.remove(p_109913_);

            if (bufferbuilder != null)
            {
                this.endBatch(p_109913_, bufferbuilder);
            }
        }

        private void endBatch(RenderType p_345497_, BufferBuilder p_344480_)
        {
            this.fireFinish(p_345497_, p_344480_);
            MeshData meshdata = p_344480_.build();

            if (meshdata != null)
            {
                if (p_345497_.sortOnUpload())
                {
                    ByteBufferBuilder bytebufferbuilder = (ByteBufferBuilder)this.fixedBuffers.getOrDefault(p_345497_, this.sharedBuffer);
                    meshdata.sortQuads(bytebufferbuilder, RenderSystem.getVertexSorting());
                }

                if (p_344480_.animatedSprites != null)
                {
                    SmartAnimations.spritesRendered(p_344480_.animatedSprites);
                }

                p_345497_.draw(meshdata);
            }

            if (p_345497_.equals(this.lastSharedType))
            {
                this.lastSharedType = null;
            }
        }

        public VertexConsumer getBuffer(ResourceLocation textureLocation, VertexConsumer bufferIn)
        {
            RenderType rendertype = bufferIn.getRenderType();

            if (!(rendertype instanceof RenderType.CompositeRenderType))
            {
                return bufferIn;
            }
            else
            {
                textureLocation = RenderType.getCustomTexture(textureLocation);
                RenderType.CompositeRenderType rendertype$compositerendertype = (RenderType.CompositeRenderType)rendertype;
                RenderType.CompositeRenderType rendertype$compositerendertype1 = rendertype$compositerendertype.getTextured(textureLocation);
                return this.getBuffer(rendertype$compositerendertype1);
            }
        }

        public RenderType getLastRenderType()
        {
            return this.lastSharedType;
        }

        public BufferBuilder getStartedBuffer(RenderType renderType)
        {
            return this.startedBuilders.get(renderType);
        }

        @Override
        public void flushRenderBuffers()
        {
            RenderType rendertype = this.lastSharedType;
            BufferBuilder bufferbuilder = this.startedBuilders.get(rendertype);
            this.endBatch();
            this.restoreRenderState(rendertype, bufferbuilder);
        }

        public void restoreRenderState(RenderType renderTypeIn, BufferBuilder bufferBuilderIn)
        {
            if (renderTypeIn != null)
            {
                this.endLastBatch();
                this.lastSharedType = renderTypeIn;

                if (bufferBuilderIn != null)
                {
                    this.startedBuilders.put(renderTypeIn, bufferBuilderIn);
                }
            }
        }

        public void addListener(IBufferSourceListener bsl)
        {
            this.listeners.add(bsl);
        }

        public boolean removeListener(IBufferSourceListener bsl)
        {
            return this.listeners.remove(bsl);
        }

        private void fireFinish(RenderType renderTypeIn, BufferBuilder bufferIn)
        {
            for (int i = 0; i < this.listeners.size(); i++)
            {
                IBufferSourceListener ibuffersourcelistener = this.listeners.get(i);
                ibuffersourcelistener.finish(renderTypeIn, bufferIn);
            }
        }

        public VertexConsumer getDummyBuffer()
        {
            return this.DUMMY_BUFFER;
        }

        public void enableCache()
        {
        }

        @Override
        public void flushCache()
        {
            int i = this.maxCachedBuffers;
            this.setMaxCachedBuffers(0);
            this.setMaxCachedBuffers(i);
        }

        public void disableCache()
        {
            this.setMaxCachedBuffers(0);
        }

        private void setMaxCachedBuffers(int maxCachedBuffers)
        {
            this.maxCachedBuffers = Math.max(maxCachedBuffers, 0);
            this.trimCachedBuffers();
        }

        private void addCachedBuffer(RenderType rt)
        {
            if (this.maxCachedBuffers > 0)
            {
                this.cachedBuffers.getAndMoveToLast(rt);

                if (!this.fixedBuffers.containsKey(rt))
                {
                    if (this.shouldCache(rt))
                    {
                        this.trimCachedBuffers();
                        BufferBuilder bufferbuilder = this.freeBufferBuilders.pollLast();
                        this.cachedBuffers.put(rt, bufferbuilder);
                    }
                }
            }
        }

        private boolean shouldCache(RenderType rt)
        {
            ResourceLocation resourcelocation = rt.getTextureLocation();

            if (resourcelocation == null)
            {
                return false;
            }
            else if (!rt.canConsolidateConsecutiveGeometry())
            {
                return false;
            }
            else
            {
                String s = resourcelocation.getPath();

                if (s.startsWith("skins/"))
                {
                    return false;
                }
                else if (s.startsWith("capes/"))
                {
                    return false;
                }
                else if (s.startsWith("capeof/"))
                {
                    return false;
                }
                else if (s.startsWith("textures/entity/horse/"))
                {
                    return false;
                }
                else
                {
                    return s.startsWith("textures/entity/villager/") ? false : !s.startsWith("textures/entity/warden/");
                }
            }
        }

        private void trimCachedBuffers()
        {
            while (this.cachedBuffers.size() > this.maxCachedBuffers)
            {
                RenderType rendertype = this.cachedBuffers.firstKey();

                if (rendertype == this.lastSharedType)
                {
                    return;
                }

                this.removeCachedBuffer(rendertype);
            }
        }

        private void removeCachedBuffer(RenderType rt)
        {
        }
    }
}
