package net.minecraft.world.level.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.util.BitStorage;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.util.ZeroBitStorage;

public class PalettedContainer<T> implements PaletteResize<T>, PalettedContainerRO<T>
{
    private static final int MIN_PALETTE_BITS = 0;
    private final PaletteResize<T> dummyPaletteResize = (p_198182_0_, p_198182_1_) -> 0;
    private final IdMap<T> registry;
    private volatile PalettedContainer.Data<T> data;
    private final PalettedContainer.Strategy strategy;
    private final ThreadingDetector threadingDetector = new ThreadingDetector("PalettedContainer");

    public void acquire()
    {
        this.threadingDetector.checkAndLock();
    }

    public void release()
    {
        this.threadingDetector.checkAndUnlock();
    }

    public static <T> Codec<PalettedContainer<T>> codecRW(IdMap<T> p_238372_, Codec<T> p_238373_, PalettedContainer.Strategy p_238374_, T p_238375_)
    {
        PalettedContainerRO.Unpacker<T, PalettedContainer<T>> unpacker = PalettedContainer::unpack;
        return codec(p_238372_, p_238373_, p_238374_, p_238375_, unpacker);
    }

    public static <T> Codec<PalettedContainerRO<T>> codecRO(IdMap<T> p_238419_, Codec<T> p_238420_, PalettedContainer.Strategy p_238421_, T p_238422_)
    {
        PalettedContainerRO.Unpacker<T, PalettedContainerRO<T>> unpacker = (p_318427_0_, p_318427_1_, p_318427_2_) -> unpack(
                    p_318427_0_, p_318427_1_, p_318427_2_
                )
                .map(p_200428_0_ -> (PalettedContainerRO<T>)p_200428_0_);
        return codec(p_238419_, p_238420_, p_238421_, p_238422_, unpacker);
    }

    private static <T, C extends PalettedContainerRO<T>> Codec<C> codec(
        IdMap<T> p_238428_, Codec<T> p_238429_, PalettedContainer.Strategy p_238430_, T p_238431_, PalettedContainerRO.Unpacker<T, C> p_238432_
    )
    {
        return RecordCodecBuilder.<PalettedContainerRO.PackedData>create(
                   p_318428_2_ -> p_318428_2_.group(
                       p_238429_.mapResult(ExtraCodecs.orElsePartial(p_238431_))
                       .listOf()
                       .fieldOf("palette")
                       .forGetter(PalettedContainerRO.PackedData::paletteEntries),
                       Codec.LONG_STREAM.lenientOptionalFieldOf("data").forGetter(PalettedContainerRO.PackedData::storage)
                   )
                   .apply(p_318428_2_, PalettedContainerRO.PackedData::new)
               )
               .comapFlatMap(
                   p_188078_3_ -> p_238432_.read(p_238428_, p_238430_, (PalettedContainerRO.PackedData<T>)p_188078_3_),
                   p_188071_2_ -> p_188071_2_.pack(p_238428_, p_238430_)
               );
    }

    public PalettedContainer(
        IdMap<T> p_188035_, PalettedContainer.Strategy p_188036_, PalettedContainer.Configuration<T> p_188037_, BitStorage p_188038_, List<T> p_188039_
    )
    {
        this.registry = p_188035_;
        this.strategy = p_188036_;
        this.data = new PalettedContainer.Data<>(p_188037_, p_188038_, p_188037_.factory().create(p_188037_.bits(), p_188035_, this, p_188039_));
    }

    private PalettedContainer(IdMap<T> p_199928_, PalettedContainer.Strategy p_199929_, PalettedContainer.Data<T> p_199930_)
    {
        this.registry = p_199928_;
        this.strategy = p_199929_;
        this.data = p_199930_;
    }

    public PalettedContainer(IdMap<T> p_188041_, T p_188042_, PalettedContainer.Strategy p_188043_)
    {
        this.strategy = p_188043_;
        this.registry = p_188041_;
        this.data = this.createOrReuseData(null, 0);
        this.data.palette.idFor(p_188042_);
    }

    private PalettedContainer.Data<T> createOrReuseData(@Nullable PalettedContainer.Data<T> p_188052_, int p_188053_)
    {
        PalettedContainer.Configuration<T> configuration = this.strategy.getConfiguration(this.registry, p_188053_);
        return p_188052_ != null && configuration.equals(p_188052_.configuration())
               ? p_188052_
               : configuration.createData(this.registry, this, this.strategy.size());
    }

    @Override
    public int onResize(int p_63142_, T p_63143_)
    {
        PalettedContainer.Data<T> data = this.data;
        PalettedContainer.Data<T> data1 = this.createOrReuseData(data, p_63142_);
        data1.copyFrom(data.palette, data.storage);
        this.data = data1;
        return data1.palette.idFor(p_63143_);
    }

    public T getAndSet(int p_63092_, int p_63093_, int p_63094_, T p_63095_)
    {
        this.acquire();
        Object object;

        try
        {
            object = this.getAndSet(this.strategy.getIndex(p_63092_, p_63093_, p_63094_), p_63095_);
        }
        finally
        {
            this.release();
        }

        return (T)object;
    }

    public T getAndSetUnchecked(int p_63128_, int p_63129_, int p_63130_, T p_63131_)
    {
        return this.getAndSet(this.strategy.getIndex(p_63128_, p_63129_, p_63130_), p_63131_);
    }

    private T getAndSet(int p_63097_, T p_63098_)
    {
        int i = this.data.palette.idFor(p_63098_);
        int j = this.data.storage.getAndSet(p_63097_, i);
        return this.data.palette.valueFor(j);
    }

    public void set(int p_156471_, int p_156472_, int p_156473_, T p_156474_)
    {
        this.acquire();

        try
        {
            this.set(this.strategy.getIndex(p_156471_, p_156472_, p_156473_), p_156474_);
        }
        finally
        {
            this.release();
        }
    }

    private void set(int p_63133_, T p_63134_)
    {
        int i = this.data.palette.idFor(p_63134_);
        this.data.storage.set(p_63133_, i);
    }

    @Override
    public T get(int p_63088_, int p_63089_, int p_63090_)
    {
        return this.get(this.strategy.getIndex(p_63088_, p_63089_, p_63090_));
    }

    protected T get(int p_63086_)
    {
        PalettedContainer.Data<T> data = this.data;
        return data.palette.valueFor(data.storage.get(p_63086_));
    }

    @Override
    public void getAll(Consumer<T> p_196880_)
    {
        Palette<T> palette = this.data.palette();
        IntSet intset = new IntArraySet();
        this.data.storage.getAll(intset::add);
        intset.forEach(p_196885_2_ -> p_196880_.accept(palette.valueFor(p_196885_2_)));
    }

    public void read(FriendlyByteBuf p_63119_)
    {
        this.acquire();

        try
        {
            int i = p_63119_.readByte();
            PalettedContainer.Data<T> data = this.createOrReuseData(this.data, i);
            data.palette.read(p_63119_);
            p_63119_.readLongArray(data.storage.getRaw());
            this.data = data;
        }
        finally
        {
            this.release();
        }
    }

    @Override
    public void write(FriendlyByteBuf p_63136_)
    {
        this.acquire();

        try
        {
            this.data.write(p_63136_);
        }
        finally
        {
            this.release();
        }
    }

    private static <T> DataResult<PalettedContainer<T>> unpack(
        IdMap<T> p_188068_, PalettedContainer.Strategy p_188069_, PalettedContainerRO.PackedData<T> p_238258_
    )
    {
        List<T> list = p_238258_.paletteEntries();
        int i = p_188069_.size();
        int j = p_188069_.calculateBitsForSerialization(p_188068_, list.size());
        PalettedContainer.Configuration<T> configuration = p_188069_.getConfiguration(p_188068_, j);
        BitStorage bitstorage;

        if (j == 0)
        {
            bitstorage = new ZeroBitStorage(i);
        }
        else
        {
            Optional<LongStream> optional = p_238258_.storage();

            if (optional.isEmpty())
            {
                return DataResult.error(() -> "Missing values for non-zero storage");
            }

            long[] along = optional.get().toArray();

            try
            {
                if (configuration.factory() == PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY)
                {
                    Palette<T> palette = new HashMapPalette<>(p_188068_, j, (p_238277_0_, p_238277_1_) -> 0, list);
                    SimpleBitStorage simplebitstorage = new SimpleBitStorage(j, i, along);
                    int[] aint = new int[i];
                    simplebitstorage.unpack(aint);
                    swapPalette(aint, p_238280_2_ -> p_188068_.getId(palette.valueFor(p_238280_2_)));
                    bitstorage = new SimpleBitStorage(configuration.bits(), i, aint);
                }
                else
                {
                    bitstorage = new SimpleBitStorage(configuration.bits(), i, along);
                }
            }
            catch (SimpleBitStorage.InitializationException simplebitstorage$initializationexception1)
            {
                return DataResult.error(() -> "Failed to read PalettedContainer: " + simplebitstorage$initializationexception1.getMessage());
            }
        }

        return DataResult.success(new PalettedContainer<>(p_188068_, p_188069_, configuration, bitstorage, list));
    }

    @Override
    public PalettedContainerRO.PackedData<T> pack(IdMap<T> p_188065_, PalettedContainer.Strategy p_188066_)
    {
        this.acquire();
        PalettedContainerRO.PackedData palettedcontainerro$packeddata;

        try
        {
            HashMapPalette<T> hashmappalette = new HashMapPalette<>(p_188065_, this.data.storage.getBits(), this.dummyPaletteResize);
            int i = p_188066_.size();
            int[] aint = new int[i];
            this.data.storage.unpack(aint);
            swapPalette(aint, p_198176_2_ -> hashmappalette.idFor(this.data.palette.valueFor(p_198176_2_)));
            int j = p_188066_.calculateBitsForSerialization(p_188065_, hashmappalette.getSize());
            Optional<LongStream> optional;

            if (j != 0)
            {
                SimpleBitStorage simplebitstorage = new SimpleBitStorage(j, i, aint);
                optional = Optional.of(Arrays.stream(simplebitstorage.getRaw()));
            }
            else
            {
                optional = Optional.empty();
            }

            palettedcontainerro$packeddata = new PalettedContainerRO.PackedData<>(hashmappalette.getEntries(), optional);
        }
        finally
        {
            this.release();
        }

        return palettedcontainerro$packeddata;
    }

    private static <T> void swapPalette(int[] p_198190_, IntUnaryOperator p_198191_)
    {
        int i = -1;
        int j = -1;

        for (int k = 0; k < p_198190_.length; k++)
        {
            int l = p_198190_[k];

            if (l != i)
            {
                i = l;
                j = p_198191_.applyAsInt(l);
            }

            p_198190_[k] = j;
        }
    }

    @Override
    public int getSerializedSize()
    {
        return this.data.getSerializedSize();
    }

    @Override
    public boolean maybeHas(Predicate<T> p_63110_)
    {
        return this.data.palette.maybeHas(p_63110_);
    }

    public PalettedContainer<T> copy()
    {
        return new PalettedContainer<>(this.registry, this.strategy, this.data.copy());
    }

    @Override
    public PalettedContainer<T> recreate()
    {
        return new PalettedContainer<>(this.registry, this.data.palette.valueFor(0), this.strategy);
    }

    @Override
    public void count(PalettedContainer.CountConsumer<T> p_63100_)
    {
        if (this.data.palette.getSize() == 1)
        {
            p_63100_.accept(this.data.palette.valueFor(0), this.data.storage.getSize());
        }
        else
        {
            Int2IntOpenHashMap int2intopenhashmap = new Int2IntOpenHashMap();
            this.data.storage.getAll(p_198179_1_ -> int2intopenhashmap.addTo(p_198179_1_, 1));
            int2intopenhashmap.int2IntEntrySet()
            .forEach(p_63138_2_ -> p_63100_.accept(this.data.palette.valueFor(p_63138_2_.getIntKey()), p_63138_2_.getIntValue()));
        }
    }

    public void finish()
    {
        this.data.storage().finish();
    }

    static record Configuration<T>(Palette.Factory factory, int bits)
    {
        public PalettedContainer.Data<T> createData(IdMap<T> p_188092_, PaletteResize<T> p_188093_, int p_188094_)
        {
            BitStorage bitstorage = (BitStorage)(this.bits == 0 ? new ZeroBitStorage(p_188094_) : new SimpleBitStorage(this.bits, p_188094_));
            Palette<T> palette = this.factory.create(this.bits, p_188092_, p_188093_, List.of());
            return new PalettedContainer.Data<>(this, bitstorage, palette);
        }
    }

    @FunctionalInterface
    public interface CountConsumer<T>
    {
        void accept(T p_63145_, int p_63146_);
    }

    static record Data<T>(PalettedContainer.Configuration<T> configuration, BitStorage storage, Palette<T> palette)
    {
        public void copyFrom(Palette<T> p_188112_, BitStorage p_188113_)
        {
            for (int i = 0; i < p_188113_.getSize(); i++)
            {
                T t = p_188112_.valueFor(p_188113_.get(i));
                this.storage.set(i, this.palette.idFor(t));
            }
        }
        public int getSerializedSize()
        {
            return 1 + this.palette.getSerializedSize() + VarInt.getByteSize(this.storage.getRaw().length) + this.storage.getRaw().length * 8;
        }
        public void write(FriendlyByteBuf p_188115_)
        {
            p_188115_.writeByte(this.storage.getBits());
            this.palette.write(p_188115_);
            p_188115_.writeLongArray(this.storage.getRaw());
        }
        public PalettedContainer.Data<T> copy()
        {
            return new PalettedContainer.Data<>(this.configuration, this.storage.copy(), this.palette.copy());
        }
    }

    public abstract static class Strategy
    {
        public static final Palette.Factory SINGLE_VALUE_PALETTE_FACTORY = SingleValuePalette::create;
        public static final Palette.Factory LINEAR_PALETTE_FACTORY = LinearPalette::create;
        public static final Palette.Factory HASHMAP_PALETTE_FACTORY = HashMapPalette::create;
        static final Palette.Factory GLOBAL_PALETTE_FACTORY = GlobalPalette::create;
        public static final PalettedContainer.Strategy SECTION_STATES = new PalettedContainer.Strategy(4)
        {
            @Override
            public <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> p_188157_, int p_188158_)
            {

                return switch (p_188158_)
                {
                    case 0 -> new PalettedContainer.Configuration(SINGLE_VALUE_PALETTE_FACTORY, p_188158_);

                    case 1, 2, 3, 4 -> new PalettedContainer.Configuration(LINEAR_PALETTE_FACTORY, 4);

                    case 5, 6, 7, 8 -> new PalettedContainer.Configuration(HASHMAP_PALETTE_FACTORY, p_188158_);

                    default -> new PalettedContainer.Configuration(PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY, Mth.ceillog2(p_188157_.size()));
                };
            }
        };
        public static final PalettedContainer.Strategy SECTION_BIOMES = new PalettedContainer.Strategy(2)
        {
            @Override
            public <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> p_188162_, int p_188163_)
            {

                return switch (p_188163_)
                {
                    case 0 -> new PalettedContainer.Configuration(SINGLE_VALUE_PALETTE_FACTORY, p_188163_);

                    case 1, 2, 3 -> new PalettedContainer.Configuration(LINEAR_PALETTE_FACTORY, p_188163_);

                    default -> new PalettedContainer.Configuration(PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY, Mth.ceillog2(p_188162_.size()));
                };
            }
        };
        private final int sizeBits;

        Strategy(int p_188143_)
        {
            this.sizeBits = p_188143_;
        }

        public int size()
        {
            return 1 << this.sizeBits * 3;
        }

        public int getIndex(int p_188146_, int p_188147_, int p_188148_)
        {
            return (p_188147_ << this.sizeBits | p_188148_) << this.sizeBits | p_188146_;
        }

        public abstract <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> p_188149_, int p_188150_);

        <A> int calculateBitsForSerialization(IdMap<A> p_188152_, int p_188153_)
        {
            int i = Mth.ceillog2(p_188153_);
            PalettedContainer.Configuration<A> configuration = this.getConfiguration(p_188152_, i);
            return configuration.factory() == GLOBAL_PALETTE_FACTORY ? i : configuration.bits();
        }
    }
}
