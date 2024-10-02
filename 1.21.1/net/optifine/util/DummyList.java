package net.optifine.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class DummyList<E> implements List<E>
{
    public static final DummyList INSTANCE = new DummyList();

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public boolean contains(Object o)
    {
        return false;
    }

    @Override
    public Iterator<E> iterator()
    {
        return null;
    }

    @Override
    public Object[] toArray()
    {
        return null;
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        return null;
    }

    @Override
    public boolean add(E e)
    {
        return false;
    }

    @Override
    public boolean remove(Object o)
    {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return false;
    }

    @Override
    public boolean addAll(Collection <? extends E > c)
    {
        return false;
    }

    @Override
    public boolean addAll(int index, Collection <? extends E > c)
    {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        return false;
    }

    @Override
    public void clear()
    {
    }

    @Override
    public E get(int index)
    {
        return null;
    }

    @Override
    public E set(int index, E element)
    {
        return null;
    }

    @Override
    public void add(int index, E element)
    {
    }

    @Override
    public E remove(int index)
    {
        return null;
    }

    @Override
    public int indexOf(Object o)
    {
        return -1;
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return -1;
    }

    @Override
    public ListIterator<E> listIterator()
    {
        return null;
    }

    @Override
    public ListIterator<E> listIterator(int index)
    {
        return null;
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex)
    {
        return this;
    }

    public class DummyIterator<E> implements Iterator<E>
    {
        @Override
        public boolean hasNext()
        {
            return false;
        }

        @Override
        public E next()
        {
            return null;
        }
    }

    public class DummyListIterator<E> implements ListIterator<E>
    {
        @Override
        public boolean hasNext()
        {
            return false;
        }

        @Override
        public E next()
        {
            return null;
        }

        @Override
        public boolean hasPrevious()
        {
            return false;
        }

        @Override
        public E previous()
        {
            return null;
        }

        @Override
        public int nextIndex()
        {
            return 0;
        }

        @Override
        public int previousIndex()
        {
            return 0;
        }

        @Override
        public void remove()
        {
        }

        @Override
        public void set(E e)
        {
        }

        @Override
        public void add(E e)
        {
        }
    }
}
