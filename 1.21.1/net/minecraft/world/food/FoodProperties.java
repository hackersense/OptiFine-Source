package net.minecraft.world.food;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record FoodProperties(
    int nutrition, float saturation, boolean canAlwaysEat, float eatSeconds, Optional<ItemStack> usingConvertsTo, List<FoodProperties.PossibleEffect> effects
)
{
    private static final float DEFAULT_EAT_SECONDS = 1.6F;
    public static final Codec<FoodProperties> DIRECT_CODEC = RecordCodecBuilder.create(
                p_341498_ -> p_341498_.group(
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("nutrition").forGetter(FoodProperties::nutrition),
                    Codec.FLOAT.fieldOf("saturation").forGetter(FoodProperties::saturation),
                    Codec.BOOL.optionalFieldOf("can_always_eat", Boolean.valueOf(false)).forGetter(FoodProperties::canAlwaysEat),
                    ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("eat_seconds", 1.6F).forGetter(FoodProperties::eatSeconds),
                    ItemStack.SINGLE_ITEM_CODEC.optionalFieldOf("using_converts_to").forGetter(FoodProperties::usingConvertsTo),
                    FoodProperties.PossibleEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(FoodProperties::effects)
                )
                .apply(p_341498_, FoodProperties::new)
            );
    public static final StreamCodec<RegistryFriendlyByteBuf, FoodProperties> DIRECT_STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                FoodProperties::nutrition,
                ByteBufCodecs.FLOAT,
                FoodProperties::saturation,
                ByteBufCodecs.BOOL,
                FoodProperties::canAlwaysEat,
                ByteBufCodecs.FLOAT,
                FoodProperties::eatSeconds,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs::optional),
                FoodProperties::usingConvertsTo,
                FoodProperties.PossibleEffect.STREAM_CODEC.apply(ByteBufCodecs.list()),
                FoodProperties::effects,
                FoodProperties::new
            );
    public int eatDurationTicks()
    {
        return (int)(this.eatSeconds * 20.0F);
    }
    public static class Builder
    {
        private int nutrition;
        private float saturationModifier;
        private boolean canAlwaysEat;
        private float eatSeconds = 1.6F;
        private Optional<ItemStack> usingConvertsTo = Optional.empty();
        private final ImmutableList.Builder<FoodProperties.PossibleEffect> effects = ImmutableList.builder();

        public FoodProperties.Builder nutrition(int p_38761_)
        {
            this.nutrition = p_38761_;
            return this;
        }

        public FoodProperties.Builder saturationModifier(float p_38759_)
        {
            this.saturationModifier = p_38759_;
            return this;
        }

        public FoodProperties.Builder alwaysEdible()
        {
            this.canAlwaysEat = true;
            return this;
        }

        public FoodProperties.Builder fast()
        {
            this.eatSeconds = 0.8F;
            return this;
        }

        public FoodProperties.Builder effect(MobEffectInstance p_38763_, float p_38764_)
        {
            this.effects.add(new FoodProperties.PossibleEffect(p_38763_, p_38764_));
            return this;
        }

        public FoodProperties.Builder usingConvertsTo(ItemLike p_343021_)
        {
            this.usingConvertsTo = Optional.of(new ItemStack(p_343021_));
            return this;
        }

        public FoodProperties build()
        {
            float f = FoodConstants.saturationByModifier(this.nutrition, this.saturationModifier);
            return new FoodProperties(this.nutrition, f, this.canAlwaysEat, this.eatSeconds, this.usingConvertsTo, this.effects.build());
        }
    }
    public static record PossibleEffect(MobEffectInstance effect, float probability)
    {
        public static final Codec<FoodProperties.PossibleEffect> CODEC = RecordCodecBuilder.create(
                    p_333209_ -> p_333209_.group(
                        MobEffectInstance.CODEC.fieldOf("effect").forGetter(FoodProperties.PossibleEffect::effect),
                        Codec.floatRange(0.0F, 1.0F).optionalFieldOf("probability", 1.0F).forGetter(FoodProperties.PossibleEffect::probability)
                    )
                    .apply(p_333209_, FoodProperties.PossibleEffect::new)
                );
        public static final StreamCodec<RegistryFriendlyByteBuf, FoodProperties.PossibleEffect> STREAM_CODEC = StreamCodec.composite(
                    MobEffectInstance.STREAM_CODEC,
                    FoodProperties.PossibleEffect::effect,
                    ByteBufCodecs.FLOAT,
                    FoodProperties.PossibleEffect::probability,
                    FoodProperties.PossibleEffect::new
                );
        public MobEffectInstance effect()
        {
            return new MobEffectInstance(this.effect);
        }
    }
}
