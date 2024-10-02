package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class RandomBooleanFeatureConfiguration implements FeatureConfiguration
{
    public static final Codec<RandomBooleanFeatureConfiguration> CODEC = RecordCodecBuilder.create(
                p_67877_ -> p_67877_.group(
                    PlacedFeature.CODEC.fieldOf("feature_true").forGetter(p_204809_ -> p_204809_.featureTrue),
                    PlacedFeature.CODEC.fieldOf("feature_false").forGetter(p_204807_ -> p_204807_.featureFalse)
                )
                .apply(p_67877_, RandomBooleanFeatureConfiguration::new)
            );
    public final Holder<PlacedFeature> featureTrue;
    public final Holder<PlacedFeature> featureFalse;

    public RandomBooleanFeatureConfiguration(Holder<PlacedFeature> p_204804_, Holder<PlacedFeature> p_204805_)
    {
        this.featureTrue = p_204804_;
        this.featureFalse = p_204805_;
    }

    @Override
    public Stream < ConfiguredFeature <? , ? >> getFeatures()
    {
        return Stream.concat(this.featureTrue.value().getFeatures(), this.featureFalse.value().getFeatures());
    }
}
