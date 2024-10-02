package net.minecraft.nbt;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class LongArrayTag extends CollectionTag<LongTag>
{
    private static final int SELF_SIZE_IN_BYTES = 24;
    public static final TagType<LongArrayTag> TYPE = new TagType.VariableSize<LongArrayTag>()
    {
        public LongArrayTag load(DataInput p_128865_, NbtAccounter p_128867_) throws IOException
        {
            return new LongArrayTag(readAccounted(p_128865_, p_128867_));
        }
        @Override
        public StreamTagVisitor.ValueResult parse(DataInput p_197501_, StreamTagVisitor p_197502_, NbtAccounter p_301749_) throws IOException
        {
            return p_197502_.visit(readAccounted(p_197501_, p_301749_));
        }
        private static long[] readAccounted(DataInput p_301699_, NbtAccounter p_301773_) throws IOException
        {
            p_301773_.accountBytes(24L);
            int i = p_301699_.readInt();
            p_301773_.accountBytes(8L, (long)i);
            long[] along = new long[i];

            for (int j = 0; j < i; j++)
            {
                along[j] = p_301699_.readLong();
            }

            return along;
        }
        @Override
        public void skip(DataInput p_197499_, NbtAccounter p_301708_) throws IOException
        {
            p_197499_.skipBytes(p_197499_.readInt() * 8);
        }
        @Override
        public String getName()
        {
            return "LONG[]";
        }
        @Override
        public String getPrettyName()
        {
            return "TAG_Long_Array";
        }
    };
    private long[] data;

    public LongArrayTag(long[] p_128808_)
    {
        this.data = p_128808_;
    }

    public LongArrayTag(LongSet p_128804_)
    {
        this.data = p_128804_.toLongArray();
    }

    public LongArrayTag(List<Long> p_128806_)
    {
        this(toArray(p_128806_));
    }

    private static long[] toArray(List<Long> p_128824_)
    {
        long[] along = new long[p_128824_.size()];

        for (int i = 0; i < p_128824_.size(); i++)
        {
            Long olong = p_128824_.get(i);
            along[i] = olong == null ? 0L : olong;
        }

        return along;
    }

    @Override
    public void write(DataOutput p_128819_) throws IOException
    {
        p_128819_.writeInt(this.data.length);

        for (long i : this.data)
        {
            p_128819_.writeLong(i);
        }
    }

    @Override
    public int sizeInBytes()
    {
        return 24 + 8 * this.data.length;
    }

    @Override
    public byte getId()
    {
        return 12;
    }

    @Override
    public TagType<LongArrayTag> getType()
    {
        return TYPE;
    }

    @Override
    public String toString()
    {
        return this.getAsString();
    }

    public LongArrayTag copy()
    {
        long[] along = new long[this.data.length];
        System.arraycopy(this.data, 0, along, 0, this.data.length);
        return new LongArrayTag(along);
    }

    @Override
    public boolean equals(Object p_128850_)
    {
        return this == p_128850_ ? true : p_128850_ instanceof LongArrayTag && Arrays.equals(this.data, ((LongArrayTag)p_128850_).data);
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(this.data);
    }

    @Override
    public void accept(TagVisitor p_177995_)
    {
        p_177995_.visitLongArray(this);
    }

    public long[] getAsLongArray()
    {
        return this.data;
    }

    @Override
    public int size()
    {
        return this.data.length;
    }

    public LongTag get(int p_128811_)
    {
        return LongTag.valueOf(this.data[p_128811_]);
    }

    public LongTag set(int p_128813_, LongTag p_128814_)
    {
        long i = this.data[p_128813_];
        this.data[p_128813_] = p_128814_.getAsLong();
        return LongTag.valueOf(i);
    }

    public void add(int p_128832_, LongTag p_128833_)
    {
        this.data = ArrayUtils.add(this.data, p_128832_, p_128833_.getAsLong());
    }

    @Override
    public boolean setTag(int p_128816_, Tag p_128817_)
    {
        if (p_128817_ instanceof NumericTag)
        {
            this.data[p_128816_] = ((NumericTag)p_128817_).getAsLong();
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean addTag(int p_128835_, Tag p_128836_)
    {
        if (p_128836_ instanceof NumericTag)
        {
            this.data = ArrayUtils.add(this.data, p_128835_, ((NumericTag)p_128836_).getAsLong());
            return true;
        }
        else
        {
            return false;
        }
    }

    public LongTag remove(int p_128830_)
    {
        long i = this.data[p_128830_];
        this.data = ArrayUtils.remove(this.data, p_128830_);
        return LongTag.valueOf(i);
    }

    @Override
    public byte getElementType()
    {
        return 4;
    }

    @Override
    public void clear()
    {
        this.data = new long[0];
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197497_)
    {
        return p_197497_.visit(this.data);
    }
}
