package net.optifine.config;

import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.optifine.Config;
import net.optifine.Lang;

public class IterableOptionInt extends IteratableOptionOF implements IPersitentOption
{
    private String resourceKey;
    private OptionValueInt[] values;
    private ToIntFunction<Options> getter;
    private ObjIntConsumer<Options> setter;
    private String saveKey;

    public IterableOptionInt(String resourceKey, OptionValueInt[] values, ToIntFunction<Options> getter, ObjIntConsumer<Options> setter, String saveKey)
    {
        super(resourceKey);
        this.resourceKey = resourceKey;
        this.values = values;
        this.getter = getter;
        this.setter = setter;
        this.saveKey = saveKey;
    }

    @Override
    public void nextOptionValue(int dirIn)
    {
        Options options = this.getOptions();
        int i = this.getter.applyAsInt(options);
        int j = this.getValueIndex(i);
        int k = j + dirIn;

        if (k < this.getIndexMin() || k > this.getIndexMax())
        {
            k = dirIn > 0 ? this.getIndexMin() : this.getIndexMax();
        }

        int l = this.values[k].getValue();
        this.setter.accept(options, l);
    }

    @Override
    public Component getOptionText()
    {
        Options options = this.getOptions();
        String s = Lang.get(this.resourceKey) + ": ";
        int i = this.getter.applyAsInt(options);
        OptionValueInt optionvalueint = this.getOptionValue(i);

        if (optionvalueint == null)
        {
            return Component.literal(s + "???");
        }
        else
        {
            String s1 = Lang.get(optionvalueint.getResourceKey());
            String s2 = s + s1;
            Component component = Component.literal(s2);
            return component;
        }
    }

    @Override
    public String getSaveKey()
    {
        return this.saveKey;
    }

    @Override
    public void loadValue(Options opts, String s)
    {
        int i = Config.parseInt(s, -1);

        if (this.getOptionValue(i) == null)
        {
            i = this.values[0].getValue();
        }

        this.setter.accept(opts, i);
    }

    @Override
    public String getSaveText(Options opts)
    {
        int i = this.getter.applyAsInt(opts);
        return Integer.toString(i);
    }

    private OptionValueInt getOptionValue(int value)
    {
        int i = this.getValueIndex(value);
        return i < 0 ? null : this.values[i];
    }

    private int getValueIndex(int value)
    {
        for (int i = 0; i < this.values.length; i++)
        {
            OptionValueInt optionvalueint = this.values[i];

            if (optionvalueint.getValue() == value)
            {
                return i;
            }
        }

        return -1;
    }

    private int getIndexMin()
    {
        return 0;
    }

    private int getIndexMax()
    {
        return this.values.length - 1;
    }
}
