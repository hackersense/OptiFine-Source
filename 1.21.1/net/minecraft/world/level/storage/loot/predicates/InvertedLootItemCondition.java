package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public record InvertedLootItemCondition(LootItemCondition term) implements LootItemCondition
{
    public static final MapCodec<InvertedLootItemCondition> CODEC = RecordCodecBuilder.mapCodec(
        p_342026_ -> p_342026_.group(LootItemCondition.DIRECT_CODEC.fieldOf("term").forGetter(InvertedLootItemCondition::term))
        .apply(p_342026_, InvertedLootItemCondition::new)
    );

    @Override
    public LootItemConditionType getType()
    {
        return LootItemConditions.INVERTED;
    }

    public boolean test(LootContext p_81689_)
    {
        return !this.term.test(p_81689_);
    }

    @Override
    public Set < LootContextParam<? >> getReferencedContextParams()
    {
        return this.term.getReferencedContextParams();
    }

    @Override
    public void validate(ValidationContext p_81691_)
    {
        LootItemCondition.super.validate(p_81691_);
        this.term.validate(p_81691_);
    }

    public static LootItemCondition.Builder invert(LootItemCondition.Builder p_81695_)
    {
        InvertedLootItemCondition invertedlootitemcondition = new InvertedLootItemCondition(p_81695_.build());
        return () -> invertedlootitemcondition;
    }
}
