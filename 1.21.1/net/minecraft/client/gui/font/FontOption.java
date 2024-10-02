package net.minecraft.client.gui.font;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.util.StringRepresentable;

public enum FontOption implements StringRepresentable
{
    UNIFORM("uniform"),
    JAPANESE_VARIANTS("jp");

    public static final Codec<FontOption> CODEC = StringRepresentable.fromEnum(FontOption::values);
    private final String name;

    private FontOption(final String p_334824_)
    {
        this.name = p_334824_;
    }

    @Override
    public String getSerializedName()
    {
        return this.name;
    }

    public static class Filter {
        private final Map<FontOption, Boolean> values;
        public static final Codec<FontOption.Filter> CODEC = Codec.unboundedMap(FontOption.CODEC, Codec.BOOL)
        .xmap(FontOption.Filter::new, p_329501_ -> p_329501_.values);
        public static final FontOption.Filter ALWAYS_PASS = new FontOption.Filter(Map.of());

        public Filter(Map<FontOption, Boolean> p_332258_)
        {
            this.values = p_332258_;
        }

        public boolean apply(Set<FontOption> p_334823_)
        {
            for (Entry<FontOption, Boolean> entry : this.values.entrySet())
            {
                if (p_334823_.contains(entry.getKey()) != entry.getValue())
                {
                    return false;
                }
            }

            return true;
        }

        public FontOption.Filter merge(FontOption.Filter p_331605_)
        {
            Map<FontOption, Boolean> map = new HashMap<>(p_331605_.values);
            map.putAll(this.values);
            return new FontOption.Filter(Map.copyOf(map));
        }
    }
}
