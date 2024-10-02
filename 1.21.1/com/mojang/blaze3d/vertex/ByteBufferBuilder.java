package com.mojang.blaze3d.vertex;

import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.annotation.Nullable;
import net.optifine.render.BufferBuilderCache;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.MemoryUtil.MemoryAllocator;
import org.slf4j.Logger;

public class ByteBufferBuilder implements AutoCloseable
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);
    private static final int MAX_GROWTH_SIZE = 2097152;
    private static final int BUFFER_FREED_GENERATION = -1;
    long pointer;
    private int capacity;
    private int writeOffset;
    private int nextResultOffset;
    private int resultCount;
    private int generation;
    private BufferBuilderCache bufferBuilderCache;
    private ByteBuffer byteBuffer;
    private IntBuffer intBuffer;
    private FloatBuffer floatBuffer;

    public ByteBufferBuilder(int p_344576_)
    {
        this.capacity = p_344576_;
        this.pointer = ALLOCATOR.malloc((long)p_344576_);

        if (this.pointer == 0L)
        {
            throw new OutOfMemoryError("Failed to allocate " + p_344576_ + " bytes");
        }
        else
        {
            this.byteBuffer = MemoryUtil.memByteBuffer(this.pointer, this.capacity);
            this.intBuffer = this.byteBuffer.asIntBuffer();
            this.floatBuffer = this.byteBuffer.asFloatBuffer();
        }
    }

    public long reserve(int p_342985_)
    {
        int i = this.writeOffset;
        int j = i + p_342985_;
        this.ensureCapacity(j);
        this.writeOffset = j;
        return this.pointer + (long)i;
    }

    private void ensureCapacity(int p_343904_)
    {
        if (p_343904_ > this.capacity)
        {
            int i = Math.min(this.capacity, 2097152);
            int j = Math.max(this.capacity + i, p_343904_);
            this.resize(j);
        }
    }

    private void resize(int p_344270_)
    {
        this.pointer = ALLOCATOR.realloc(this.pointer, (long)p_344270_);
        LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", this.capacity, p_344270_);

        if (this.pointer == 0L)
        {
            throw new OutOfMemoryError("Failed to resize buffer from " + this.capacity + " bytes to " + p_344270_ + " bytes");
        }
        else
        {
            this.capacity = p_344270_;
            this.byteBuffer = MemoryUtil.memByteBuffer(this.pointer, this.capacity);
            this.intBuffer = this.byteBuffer.asIntBuffer();
            this.floatBuffer = this.byteBuffer.asFloatBuffer();
        }
    }

    @Nullable
    public ByteBufferBuilder.Result build()
    {
        this.checkOpen();
        int i = this.nextResultOffset;
        int j = this.writeOffset - i;

        if (j == 0)
        {
            return null;
        }
        else
        {
            this.nextResultOffset = this.writeOffset;
            this.resultCount++;
            return new ByteBufferBuilder.Result(i, j, this.generation);
        }
    }

    public void clear()
    {
        if (this.resultCount > 0)
        {
            LOGGER.warn("Clearing BufferBuilder with unused batches");
        }

        this.discard();
    }

    public void discard()
    {
        this.checkOpen();

        if (this.resultCount > 0)
        {
            this.discardResults();
            this.resultCount = 0;
        }
    }

    boolean isValid(int p_344177_)
    {
        return p_344177_ == this.generation;
    }

    void freeResult()
    {
        if (--this.resultCount <= 0)
        {
            this.discardResults();
        }
    }

    private void discardResults()
    {
        int i = this.writeOffset - this.nextResultOffset;

        if (i > 0)
        {
            MemoryUtil.memCopy(this.pointer + (long)this.nextResultOffset, this.pointer, (long)i);
        }

        this.writeOffset = i;
        this.nextResultOffset = 0;
        this.generation++;
    }

    @Override
    public void close()
    {
        if (this.pointer != 0L)
        {
            ALLOCATOR.free(this.pointer);
            this.pointer = 0L;
            this.generation = -1;
            this.byteBuffer = null;
            this.intBuffer = null;
            this.floatBuffer = null;
        }
    }

    private void checkOpen()
    {
        if (this.pointer == 0L)
        {
            throw new IllegalStateException("Buffer has been freed");
        }
    }

    public int getCapacity()
    {
        return this.capacity;
    }

    public ByteBuffer getByteBuffer()
    {
        return this.byteBuffer;
    }

    public IntBuffer getIntBuffer()
    {
        return this.intBuffer;
    }

    public FloatBuffer getFloatBuffer()
    {
        return this.floatBuffer;
    }

    public int getNextResultOffset()
    {
        return this.nextResultOffset;
    }

    public int getWriteOffset()
    {
        return this.writeOffset;
    }

    public BufferBuilderCache getBufferBuilderCache()
    {
        if (this.bufferBuilderCache == null)
        {
            this.bufferBuilderCache = new BufferBuilderCache();
        }

        return this.bufferBuilderCache;
    }

    @Override
    public String toString()
    {
        return "resultOffset: " + this.nextResultOffset + ", writeOffset: " + this.writeOffset + ", capacity: " + this.capacity;
    }

    public class Result implements AutoCloseable
    {
        private final int offset;
        private final int capacity;
        private final int generation;
        private boolean closed;

        Result(final int p_343613_, final int p_344565_, final int p_344170_)
        {
            this.offset = p_343613_;
            this.capacity = p_344565_;
            this.generation = p_344170_;
        }

        public ByteBuffer byteBuffer()
        {
            if (!ByteBufferBuilder.this.isValid(this.generation))
            {
                throw new IllegalStateException("Buffer is no longer valid");
            }
            else
            {
                return MemoryUtil.memByteBuffer(ByteBufferBuilder.this.pointer + (long)this.offset, this.capacity);
            }
        }

        @Override
        public void close()
        {
            if (!this.closed)
            {
                this.closed = true;

                if (ByteBufferBuilder.this.isValid(this.generation))
                {
                    ByteBufferBuilder.this.freeResult();
                }
            }
        }

        @Override
        public String toString()
        {
            return "offset: " + this.offset + ", capacity: " + this.capacity + ", generation: " + this.generation + ", closed: " + this.closed;
        }
    }
}
