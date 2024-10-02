package net.optifine.config;

import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.optifine.Lang;

public class IterableOptionBool extends IteratableOptionOF implements IPersitentOption
{
    private String resourceKey;
    private ToBooleanFunction<Options> getter;
    private ObjBooleanConsumer<Options> setter;
    private String saveKey;

    public IterableOptionBool(String resourceKey, ToBooleanFunction<Options> getter, ObjBooleanConsumer<Options> setter, String saveKey)
    {
        super(resourceKey);
        this.resourceKey = resourceKey;
        this.getter = getter;
        this.setter = setter;
        this.saveKey = saveKey;
    }

    @Override
    public void nextOptionValue(int dirIn)
    {
        Options options = this.getOptions();
        boolean flag = this.getter.applyAsBool(options);
        flag = !flag;
        this.setter.accept(options, flag);
    }

    @Override
    public Component getOptionText()
    {
        Options options = this.getOptions();
        String s = Lang.get(this.resourceKey) + ": ";
        boolean flag = this.getter.applyAsBool(options);
        String s1 = flag ? Lang.getOn() : Lang.getOff();
        String s2 = s + s1;
        Component component = Component.literal(s2);
        return component;
    }

    @Override
    public String getSaveKey()
    {
        return this.saveKey;
    }

    @Override
    public void loadValue(Options opts, String s)
    {
        boolean flag = Boolean.valueOf(s);
        this.setter.accept(opts, flag);
    }

    @Override
    public String getSaveText(Options opts)
    {
        boolean flag = this.getter.applyAsBool(opts);
        return Boolean.toString(flag);
    }
}
