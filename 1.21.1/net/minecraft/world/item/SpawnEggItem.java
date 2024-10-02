package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SpawnEggItem extends Item
{
    private static final Map < EntityType <? extends Mob > , SpawnEggItem > BY_ID = Maps.newIdentityHashMap();
    private static final MapCodec < EntityType<? >> ENTITY_TYPE_FIELD_CODEC = BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("id");
    private final int backgroundColor;
    private final int highlightColor;
    private final EntityType<?> defaultType;

    public SpawnEggItem(EntityType <? extends Mob > p_43207_, int p_43208_, int p_43209_, Item.Properties p_43210_)
    {
        super(p_43210_);
        this.defaultType = p_43207_;
        this.backgroundColor = p_43208_;
        this.highlightColor = p_43209_;
        BY_ID.put(p_43207_, this);
    }

    @Override
    public InteractionResult useOn(UseOnContext p_43223_)
    {
        Level level = p_43223_.getLevel();

        if (!(level instanceof ServerLevel))
        {
            return InteractionResult.SUCCESS;
        }
        else
        {
            ItemStack itemstack = p_43223_.getItemInHand();
            BlockPos blockpos = p_43223_.getClickedPos();
            Direction direction = p_43223_.getClickedFace();
            BlockState blockstate = level.getBlockState(blockpos);

            if (level.getBlockEntity(blockpos) instanceof Spawner spawner)
            {
                EntityType<?> entitytype1 = this.getType(itemstack);
                spawner.setEntityId(entitytype1, level.getRandom());
                level.sendBlockUpdated(blockpos, blockstate, blockstate, 3);
                level.gameEvent(p_43223_.getPlayer(), GameEvent.BLOCK_CHANGE, blockpos);
                itemstack.shrink(1);
                return InteractionResult.CONSUME;
            }
            else
            {
                BlockPos blockpos1;

                if (blockstate.getCollisionShape(level, blockpos).isEmpty())
                {
                    blockpos1 = blockpos;
                }
                else
                {
                    blockpos1 = blockpos.relative(direction);
                }

                EntityType<?> entitytype = this.getType(itemstack);

                if (entitytype.spawn(
                            (ServerLevel)level,
                            itemstack,
                            p_43223_.getPlayer(),
                            blockpos1,
                            MobSpawnType.SPAWN_EGG,
                            true,
                            !Objects.equals(blockpos, blockpos1) && direction == Direction.UP
                        )
                        != null)
                {
                    itemstack.shrink(1);
                    level.gameEvent(p_43223_.getPlayer(), GameEvent.ENTITY_PLACE, blockpos);
                }

                return InteractionResult.CONSUME;
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level p_43225_, Player p_43226_, InteractionHand p_43227_)
    {
        ItemStack itemstack = p_43226_.getItemInHand(p_43227_);
        BlockHitResult blockhitresult = getPlayerPOVHitResult(p_43225_, p_43226_, ClipContext.Fluid.SOURCE_ONLY);

        if (blockhitresult.getType() != HitResult.Type.BLOCK)
        {
            return InteractionResultHolder.pass(itemstack);
        }
        else if (!(p_43225_ instanceof ServerLevel))
        {
            return InteractionResultHolder.success(itemstack);
        }
        else
        {
            BlockPos blockpos = blockhitresult.getBlockPos();

            if (!(p_43225_.getBlockState(blockpos).getBlock() instanceof LiquidBlock))
            {
                return InteractionResultHolder.pass(itemstack);
            }
            else if (p_43225_.mayInteract(p_43226_, blockpos) && p_43226_.mayUseItemAt(blockpos, blockhitresult.getDirection(), itemstack))
            {
                EntityType<?> entitytype = this.getType(itemstack);
                Entity entity = entitytype.spawn((ServerLevel)p_43225_, itemstack, p_43226_, blockpos, MobSpawnType.SPAWN_EGG, false, false);

                if (entity == null)
                {
                    return InteractionResultHolder.pass(itemstack);
                }
                else
                {
                    itemstack.consume(1, p_43226_);
                    p_43226_.awardStat(Stats.ITEM_USED.get(this));
                    p_43225_.gameEvent(p_43226_, GameEvent.ENTITY_PLACE, entity.position());
                    return InteractionResultHolder.consume(itemstack);
                }
            }
            else
            {
                return InteractionResultHolder.fail(itemstack);
            }
        }
    }

    public boolean spawnsEntity(ItemStack p_331553_, EntityType<?> p_43232_)
    {
        return Objects.equals(this.getType(p_331553_), p_43232_);
    }

    public int getColor(int p_43212_)
    {
        return p_43212_ == 0 ? this.backgroundColor : this.highlightColor;
    }

    @Nullable
    public static SpawnEggItem byId(@Nullable EntityType<?> p_43214_)
    {
        return BY_ID.get(p_43214_);
    }

    public static Iterable<SpawnEggItem> eggs()
    {
        return Iterables.unmodifiableIterable(BY_ID.values());
    }

    public EntityType<?> getType(ItemStack p_334231_)
    {
        CustomData customdata = p_334231_.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
        return !customdata.isEmpty() ? customdata.read(ENTITY_TYPE_FIELD_CODEC).result().orElse(this.defaultType) : this.defaultType;
    }

    @Override
    public FeatureFlagSet requiredFeatures()
    {
        return this.defaultType.requiredFeatures();
    }

    public Optional<Mob> spawnOffspringFromSpawnEgg(Player p_43216_, Mob p_43217_, EntityType <? extends Mob > p_43218_, ServerLevel p_43219_, Vec3 p_43220_, ItemStack p_43221_)
    {
        if (!this.spawnsEntity(p_43221_, p_43218_))
        {
            return Optional.empty();
        }
        else
        {
            Mob mob;

            if (p_43217_ instanceof AgeableMob)
            {
                mob = ((AgeableMob)p_43217_).getBreedOffspring(p_43219_, (AgeableMob)p_43217_);
            }
            else
            {
                mob = p_43218_.create(p_43219_);
            }

            if (mob == null)
            {
                return Optional.empty();
            }
            else
            {
                mob.setBaby(true);

                if (!mob.isBaby())
                {
                    return Optional.empty();
                }
                else
                {
                    mob.moveTo(p_43220_.x(), p_43220_.y(), p_43220_.z(), 0.0F, 0.0F);
                    p_43219_.addFreshEntityWithPassengers(mob);
                    mob.setCustomName(p_43221_.get(DataComponents.CUSTOM_NAME));
                    p_43221_.consume(1, p_43216_);
                    return Optional.of(mob);
                }
            }
        }
    }
}
