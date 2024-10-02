package net.optifine.render;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.client.renderer.RenderType;

public class ChunkLayerSet implements Set<RenderType>
{
    private boolean[] layers = new boolean[RenderType.CHUNK_RENDER_TYPES.length];
    private boolean empty = true;

    public boolean add(RenderType renderType)
    {
        this.layers[renderType.ordinal()] = true;
        this.empty = false;
        return false;
    }

    public boolean contains(RenderType renderType)
    {
        return this.layers[renderType.ordinal()];
    }

    @Override
    public boolean contains(Object obj)
    {
        return obj instanceof RenderType ? this.contains((RenderType)obj) : false;
    }

    @Override
    public boolean isEmpty()
    {
        return this.empty;
    }

    @Override
    public int size()
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Iterator<RenderType> iterator()
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Object[] toArray()
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean remove(Object o)
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean addAll(Collection <? extends RenderType > c)
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Not supported");
    }
}
