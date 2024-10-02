package net.minecraft.core;

import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.VisibleForTesting;

public interface HolderSet<T> extends Iterable<Holder<T>>
{
    Stream<Holder<T>> stream();

    int size();

    Either<TagKey<T>, List<Holder<T>>> unwrap();

    Optional<Holder<T>> getRandomElement(RandomSource p_235712_);

    Holder<T> get(int p_205798_);

    boolean contains(Holder<T> p_205799_);

    boolean canSerializeIn(HolderOwner<T> p_255749_);

    Optional<TagKey<T>> unwrapKey();

    @Deprecated
    @VisibleForTesting
    static <T> HolderSet.Named<T> emptyNamed(HolderOwner<T> p_255858_, TagKey<T> p_256459_)
    {
        return new HolderSet.Named<T>(p_255858_, p_256459_)
        {
            @Override
            protected List<Holder<T>> contents()
            {
                throw new UnsupportedOperationException("Tag " + this.key() + " can't be dereferenced during construction");
            }
        };
    }

    static <T> HolderSet<T> empty()
    {
        return (HolderSet<T>)HolderSet.Direct.EMPTY;
    }

    @SafeVarargs
    static <T> HolderSet.Direct<T> direct(Holder<T>... p_205810_)
    {
        return new HolderSet.Direct<>(List.of(p_205810_));
    }

    static <T> HolderSet.Direct<T> direct(List <? extends Holder<T >> p_205801_)
    {
        return new HolderSet.Direct<>(List.copyOf(p_205801_));
    }

    @SafeVarargs
    static <E, T> HolderSet.Direct<T> direct(Function<E, Holder<T>> p_205807_, E... p_205808_)
    {
        return direct(Stream.of(p_205808_).map(p_205807_).toList());
    }

    static <E, T> HolderSet.Direct<T> direct(Function<E, Holder<T>> p_205804_, Collection<E> p_298882_)
    {
        return direct(p_298882_.stream().map(p_205804_).toList());
    }

    public static final class Direct<T> extends HolderSet.ListBacked<T>
    {
        static final HolderSet.Direct<?> EMPTY = new HolderSet.Direct(List.of());
        private final List<Holder<T>> contents;
        @Nullable
        private Set<Holder<T>> contentsSet;

        Direct(List<Holder<T>> p_205814_)
        {
            this.contents = p_205814_;
        }

        @Override
        protected List<Holder<T>> contents()
        {
            return this.contents;
        }

        @Override
        public Either<TagKey<T>, List<Holder<T>>> unwrap()
        {
            return Either.right(this.contents);
        }

        @Override
        public Optional<TagKey<T>> unwrapKey()
        {
            return Optional.empty();
        }

        @Override
        public boolean contains(Holder<T> p_205816_)
        {
            if (this.contentsSet == null)
            {
                this.contentsSet = Set.copyOf(this.contents);
            }

            return this.contentsSet.contains(p_205816_);
        }

        @Override
        public String toString()
        {
            return "DirectSet[" + this.contents + "]";
        }

        @Override
        public boolean equals(Object p_335031_)
        {
            if (this == p_335031_)
            {
                return true;
            }
            else
            {
                if (p_335031_ instanceof HolderSet.Direct<?> direct && this.contents.equals(direct.contents))
                {
                    return true;
                }

                return false;
            }
        }

        @Override
        public int hashCode()
        {
            return this.contents.hashCode();
        }
    }

    public abstract static class ListBacked<T> implements HolderSet<T>
    {
        protected abstract List<Holder<T>> contents();

        @Override
        public int size()
        {
            return this.contents().size();
        }

        @Override
        public Spliterator<Holder<T>> spliterator()
        {
            return this.contents().spliterator();
        }

        @Override
        public Iterator<Holder<T>> iterator()
        {
            return this.contents().iterator();
        }

        @Override
        public Stream<Holder<T>> stream()
        {
            return this.contents().stream();
        }

        @Override
        public Optional<Holder<T>> getRandomElement(RandomSource p_235714_)
        {
            return Util.getRandomSafe(this.contents(), p_235714_);
        }

        @Override
        public Holder<T> get(int p_205823_)
        {
            return this.contents().get(p_205823_);
        }

        @Override
        public boolean canSerializeIn(HolderOwner<T> p_255876_)
        {
            return true;
        }
    }

    public static class Named<T> extends HolderSet.ListBacked<T>
    {
        private final HolderOwner<T> owner;
        private final TagKey<T> key;
        private List<Holder<T>> contents = List.of();

        Named(HolderOwner<T> p_256118_, TagKey<T> p_256597_)
        {
            this.owner = p_256118_;
            this.key = p_256597_;
        }

        void bind(List<Holder<T>> p_205836_)
        {
            this.contents = List.copyOf(p_205836_);
        }

        public TagKey<T> key()
        {
            return this.key;
        }

        @Override
        protected List<Holder<T>> contents()
        {
            return this.contents;
        }

        @Override
        public Either<TagKey<T>, List<Holder<T>>> unwrap()
        {
            return Either.left(this.key);
        }

        @Override
        public Optional<TagKey<T>> unwrapKey()
        {
            return Optional.of(this.key);
        }

        @Override
        public boolean contains(Holder<T> p_205834_)
        {
            return p_205834_.is(this.key);
        }

        @Override
        public String toString()
        {
            return "NamedSet(" + this.key + ")[" + this.contents + "]";
        }

        @Override
        public boolean canSerializeIn(HolderOwner<T> p_256542_)
        {
            return this.owner.canSerializeIn(p_256542_);
        }
    }
}
