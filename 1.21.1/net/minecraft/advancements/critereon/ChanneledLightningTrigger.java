package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class ChanneledLightningTrigger extends SimpleCriterionTrigger<ChanneledLightningTrigger.TriggerInstance>
{
    @Override
    public Codec<ChanneledLightningTrigger.TriggerInstance> codec()
    {
        return ChanneledLightningTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer p_21722_, Collection <? extends Entity > p_21723_)
    {
        List<LootContext> list = p_21723_.stream().map(p_21720_ -> EntityPredicate.createContext(p_21722_, p_21720_)).collect(Collectors.toList());
        this.trigger(p_21722_, p_21730_ -> p_21730_.matches(list));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, List<ContextAwarePredicate> victims)
    implements SimpleCriterionTrigger.SimpleInstance
    {
        public static final Codec<ChanneledLightningTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_325195_ -> p_325195_.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ChanneledLightningTrigger.TriggerInstance::player),
                EntityPredicate.ADVANCEMENT_CODEC.listOf().optionalFieldOf("victims", List.of()).forGetter(ChanneledLightningTrigger.TriggerInstance::victims)
            )
            .apply(p_325195_, ChanneledLightningTrigger.TriggerInstance::new)
        );

        public static Criterion<ChanneledLightningTrigger.TriggerInstance> channeledLightning(EntityPredicate.Builder... p_299370_)
        {
            return CriteriaTriggers.CHANNELED_LIGHTNING.createCriterion(new ChanneledLightningTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_299370_)));
        }

        public boolean matches(Collection <? extends LootContext > p_21745_)
        {
            for (ContextAwarePredicate contextawarepredicate : this.victims)
            {
                boolean flag = false;

                for (LootContext lootcontext : p_21745_)
                {
                    if (contextawarepredicate.matches(lootcontext))
                    {
                        flag = true;
                        break;
                    }
                }

                if (!flag)
                {
                    return false;
                }
            }

            return true;
        }

        @Override
        public void validate(CriterionValidator p_312774_)
        {
            SimpleCriterionTrigger.SimpleInstance.super.validate(p_312774_);
            p_312774_.validateEntities(this.victims, ".victims");
        }

        @Override
        public Optional<ContextAwarePredicate> player()
        {
            return this.player;
        }
    }
}
