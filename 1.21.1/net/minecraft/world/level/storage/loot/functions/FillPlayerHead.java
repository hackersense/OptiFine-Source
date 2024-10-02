package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class FillPlayerHead extends LootItemConditionalFunction
{
    public static final MapCodec<FillPlayerHead> CODEC = RecordCodecBuilder.mapCodec(
                p_297099_ -> commonFields(p_297099_)
                .and(LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(p_297096_ -> p_297096_.entityTarget))
                .apply(p_297099_, FillPlayerHead::new)
            );
    private final LootContext.EntityTarget entityTarget;

    public FillPlayerHead(List<LootItemCondition> p_301112_, LootContext.EntityTarget p_80605_)
    {
        super(p_301112_);
        this.entityTarget = p_80605_;
    }

    @Override
    public LootItemFunctionType<FillPlayerHead> getType()
    {
        return LootItemFunctions.FILL_PLAYER_HEAD;
    }

    @Override
    public Set < LootContextParam<? >> getReferencedContextParams()
    {
        return ImmutableSet.of(this.entityTarget.getParam());
    }

    @Override
    public ItemStack run(ItemStack p_80608_, LootContext p_80609_)
    {
        if (p_80608_.is(Items.PLAYER_HEAD) && p_80609_.getParamOrNull(this.entityTarget.getParam()) instanceof Player player)
        {
            p_80608_.set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()));
        }

        return p_80608_;
    }

    public static LootItemConditionalFunction.Builder<?> fillPlayerHead(LootContext.EntityTarget p_165208_)
    {
        return simpleBuilder(p_297098_ -> new FillPlayerHead(p_297098_, p_165208_));
    }
}
