package net.minecraft.server.level;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ComplexItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ServerItemCooldowns;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;

public class ServerPlayer extends Player
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_XZ = 32;
    private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_Y = 10;
    private static final int FLY_STAT_RECORDING_SPEED = 25;
    public static final double INTERACTION_DISTANCE_VERIFICATION_BUFFER = 1.0;
    private static final AttributeModifier CREATIVE_BLOCK_INTERACTION_RANGE_MODIFIER = new AttributeModifier(
        ResourceLocation.withDefaultNamespace("creative_mode_block_range"), 0.5, AttributeModifier.Operation.ADD_VALUE
    );
    private static final AttributeModifier CREATIVE_ENTITY_INTERACTION_RANGE_MODIFIER = new AttributeModifier(
        ResourceLocation.withDefaultNamespace("creative_mode_entity_range"), 2.0, AttributeModifier.Operation.ADD_VALUE
    );
    public ServerGamePacketListenerImpl connection;
    public final MinecraftServer server;
    public final ServerPlayerGameMode gameMode;
    private final PlayerAdvancements advancements;
    private final ServerStatsCounter stats;
    private float lastRecordedHealthAndAbsorption = Float.MIN_VALUE;
    private int lastRecordedFoodLevel = Integer.MIN_VALUE;
    private int lastRecordedAirLevel = Integer.MIN_VALUE;
    private int lastRecordedArmor = Integer.MIN_VALUE;
    private int lastRecordedLevel = Integer.MIN_VALUE;
    private int lastRecordedExperience = Integer.MIN_VALUE;
    private float lastSentHealth = -1.0E8F;
    private int lastSentFood = -99999999;
    private boolean lastFoodSaturationZero = true;
    private int lastSentExp = -99999999;
    private int spawnInvulnerableTime = 60;
    private ChatVisiblity chatVisibility = ChatVisiblity.FULL;
    private boolean canChatColor = true;
    private long lastActionTime = Util.getMillis();
    @Nullable
    private Entity camera;
    private boolean isChangingDimension;
    public boolean seenCredits;
    private final ServerRecipeBook recipeBook = new ServerRecipeBook();
    @Nullable
    private Vec3 levitationStartPos;
    private int levitationStartTime;
    private boolean disconnected;
    private int requestedViewDistance = 2;
    private String language = "en_us";
    @Nullable
    private Vec3 startingToFallPosition;
    @Nullable
    private Vec3 enteredNetherPosition;
    @Nullable
    private Vec3 enteredLavaOnVehiclePosition;
    private SectionPos lastSectionPos = SectionPos.of(0, 0, 0);
    private ChunkTrackingView chunkTrackingView = ChunkTrackingView.EMPTY;
    private ResourceKey<Level> respawnDimension = Level.OVERWORLD;
    @Nullable
    private BlockPos respawnPosition;
    private boolean respawnForced;
    private float respawnAngle;
    private final TextFilter textFilter;
    private boolean textFilteringEnabled;
    private boolean allowsListing;
    private boolean spawnExtraParticlesOnFall;
    private WardenSpawnTracker wardenSpawnTracker = new WardenSpawnTracker(0, 0, 0);
    @Nullable
    private BlockPos raidOmenPosition;
    private Vec3 lastKnownClientMovement = Vec3.ZERO;
    private final ContainerSynchronizer containerSynchronizer = new ContainerSynchronizer()
    {
        @Override
        public void sendInitialData(AbstractContainerMenu p_143448_, NonNullList<ItemStack> p_143449_, ItemStack p_143450_, int[] p_143451_)
        {
            ServerPlayer.this.connection.send(new ClientboundContainerSetContentPacket(p_143448_.containerId, p_143448_.incrementStateId(), p_143449_, p_143450_));

            for (int i = 0; i < p_143451_.length; i++)
            {
                this.broadcastDataValue(p_143448_, i, p_143451_[i]);
            }
        }
        @Override
        public void sendSlotChange(AbstractContainerMenu p_143441_, int p_143442_, ItemStack p_143443_)
        {
            ServerPlayer.this.connection.send(new ClientboundContainerSetSlotPacket(p_143441_.containerId, p_143441_.incrementStateId(), p_143442_, p_143443_));
        }
        @Override
        public void sendCarriedChange(AbstractContainerMenu p_143445_, ItemStack p_143446_)
        {
            ServerPlayer.this.connection.send(new ClientboundContainerSetSlotPacket(-1, p_143445_.incrementStateId(), -1, p_143446_));
        }
        @Override
        public void sendDataChange(AbstractContainerMenu p_143437_, int p_143438_, int p_143439_)
        {
            this.broadcastDataValue(p_143437_, p_143438_, p_143439_);
        }
        private void broadcastDataValue(AbstractContainerMenu p_143455_, int p_143456_, int p_143457_)
        {
            ServerPlayer.this.connection.send(new ClientboundContainerSetDataPacket(p_143455_.containerId, p_143456_, p_143457_));
        }
    };
    private final ContainerListener containerListener = new ContainerListener()
    {
        @Override
        public void slotChanged(AbstractContainerMenu p_143466_, int p_143467_, ItemStack p_143468_)
        {
            Slot slot = p_143466_.getSlot(p_143467_);

            if (!(slot instanceof ResultSlot))
            {
                if (slot.container == ServerPlayer.this.getInventory())
                {
                    CriteriaTriggers.INVENTORY_CHANGED.trigger(ServerPlayer.this, ServerPlayer.this.getInventory(), p_143468_);
                }
            }
        }
        @Override
        public void dataChanged(AbstractContainerMenu p_143462_, int p_143463_, int p_143464_)
        {
        }
    };
    @Nullable
    private RemoteChatSession chatSession;
    @Nullable
    public final Object object;
    private int containerCounter;
    public boolean wonGame;

    public ServerPlayer(MinecraftServer p_254143_, ServerLevel p_254435_, GameProfile p_253651_, ClientInformation p_299301_)
    {
        super(p_254435_, p_254435_.getSharedSpawnPos(), p_254435_.getSharedSpawnAngle(), p_253651_);
        this.textFilter = p_254143_.createTextFilterForPlayer(this);
        this.gameMode = p_254143_.createGameModeForPlayer(this);
        this.server = p_254143_;
        this.stats = p_254143_.getPlayerList().getPlayerStats(this);
        this.advancements = p_254143_.getPlayerList().getPlayerAdvancements(this);
        this.moveTo(this.adjustSpawnLocation(p_254435_, p_254435_.getSharedSpawnPos()).getBottomCenter(), 0.0F, 0.0F);
        this.updateOptions(p_299301_);
        this.object = null;
    }

    @Override
    public BlockPos adjustSpawnLocation(ServerLevel p_343805_, BlockPos p_344752_)
    {
        AABB aabb = this.getDimensions(Pose.STANDING).makeBoundingBox(Vec3.ZERO);
        BlockPos blockpos = p_344752_;

        if (p_343805_.dimensionType().hasSkyLight() && p_343805_.getServer().getWorldData().getGameType() != GameType.ADVENTURE)
        {
            int i = Math.max(0, this.server.getSpawnRadius(p_343805_));
            int j = Mth.floor(p_343805_.getWorldBorder().getDistanceToBorder((double)p_344752_.getX(), (double)p_344752_.getZ()));

            if (j < i)
            {
                i = j;
            }

            if (j <= 1)
            {
                i = 1;
            }

            long k = (long)(i * 2 + 1);
            long l = k * k;
            int i1 = l > 2147483647L ? Integer.MAX_VALUE : (int)l;
            int j1 = this.getCoprime(i1);
            int k1 = RandomSource.create().nextInt(i1);

            for (int l1 = 0; l1 < i1; l1++)
            {
                int i2 = (k1 + j1 * l1) % i1;
                int j2 = i2 % (i * 2 + 1);
                int k2 = i2 / (i * 2 + 1);
                blockpos = PlayerRespawnLogic.getOverworldRespawnPos(p_343805_, p_344752_.getX() + j2 - i, p_344752_.getZ() + k2 - i);

                if (blockpos != null && p_343805_.noCollision(this, aabb.move(blockpos.getBottomCenter())))
                {
                    return blockpos;
                }
            }

            blockpos = p_344752_;
        }

        while (!p_343805_.noCollision(this, aabb.move(blockpos.getBottomCenter())) && blockpos.getY() < p_343805_.getMaxBuildHeight() - 1)
        {
            blockpos = blockpos.above();
        }

        while (p_343805_.noCollision(this, aabb.move(blockpos.below().getBottomCenter())) && blockpos.getY() > p_343805_.getMinBuildHeight() + 1)
        {
            blockpos = blockpos.below();
        }

        return blockpos;
    }

    private int getCoprime(int p_9238_)
    {
        return p_9238_ <= 16 ? p_9238_ - 1 : 17;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag p_9131_)
    {
        super.readAdditionalSaveData(p_9131_);

        if (p_9131_.contains("warden_spawn_tracker", 10))
        {
            WardenSpawnTracker.CODEC
            .parse(new Dynamic<>(NbtOps.INSTANCE, p_9131_.get("warden_spawn_tracker")))
            .resultOrPartial(LOGGER::error)
            .ifPresent(p_248205_ -> this.wardenSpawnTracker = p_248205_);
        }

        if (p_9131_.contains("enteredNetherPosition", 10))
        {
            CompoundTag compoundtag = p_9131_.getCompound("enteredNetherPosition");
            this.enteredNetherPosition = new Vec3(compoundtag.getDouble("x"), compoundtag.getDouble("y"), compoundtag.getDouble("z"));
        }

        this.seenCredits = p_9131_.getBoolean("seenCredits");

        if (p_9131_.contains("recipeBook", 10))
        {
            this.recipeBook.fromNbt(p_9131_.getCompound("recipeBook"), this.server.getRecipeManager());
        }

        if (this.isSleeping())
        {
            this.stopSleeping();
        }

        if (p_9131_.contains("SpawnX", 99) && p_9131_.contains("SpawnY", 99) && p_9131_.contains("SpawnZ", 99))
        {
            this.respawnPosition = new BlockPos(p_9131_.getInt("SpawnX"), p_9131_.getInt("SpawnY"), p_9131_.getInt("SpawnZ"));
            this.respawnForced = p_9131_.getBoolean("SpawnForced");
            this.respawnAngle = p_9131_.getFloat("SpawnAngle");

            if (p_9131_.contains("SpawnDimension"))
            {
                this.respawnDimension = Level.RESOURCE_KEY_CODEC
                               .parse(NbtOps.INSTANCE, p_9131_.get("SpawnDimension"))
                               .resultOrPartial(LOGGER::error)
                               .orElse(Level.OVERWORLD);
            }
        }

        this.spawnExtraParticlesOnFall = p_9131_.getBoolean("spawn_extra_particles_on_fall");
        Tag tag = p_9131_.get("raid_omen_position");

        if (tag != null)
        {
            BlockPos.CODEC.parse(NbtOps.INSTANCE, tag).resultOrPartial(LOGGER::error).ifPresent(p_326431_ -> this.raidOmenPosition = p_326431_);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag p_9197_)
    {
        super.addAdditionalSaveData(p_9197_);
        WardenSpawnTracker.CODEC
        .encodeStart(NbtOps.INSTANCE, this.wardenSpawnTracker)
        .resultOrPartial(LOGGER::error)
        .ifPresent(p_9134_ -> p_9197_.put("warden_spawn_tracker", p_9134_));
        this.storeGameTypes(p_9197_);
        p_9197_.putBoolean("seenCredits", this.seenCredits);

        if (this.enteredNetherPosition != null)
        {
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putDouble("x", this.enteredNetherPosition.x);
            compoundtag.putDouble("y", this.enteredNetherPosition.y);
            compoundtag.putDouble("z", this.enteredNetherPosition.z);
            p_9197_.put("enteredNetherPosition", compoundtag);
        }

        Entity entity1 = this.getRootVehicle();
        Entity entity = this.getVehicle();

        if (entity != null && entity1 != this && entity1.hasExactlyOnePlayerPassenger())
        {
            CompoundTag compoundtag1 = new CompoundTag();
            CompoundTag compoundtag2 = new CompoundTag();
            entity1.save(compoundtag2);
            compoundtag1.putUUID("Attach", entity.getUUID());
            compoundtag1.put("Entity", compoundtag2);
            p_9197_.put("RootVehicle", compoundtag1);
        }

        p_9197_.put("recipeBook", this.recipeBook.toNbt());
        p_9197_.putString("Dimension", this.level().dimension().location().toString());

        if (this.respawnPosition != null)
        {
            p_9197_.putInt("SpawnX", this.respawnPosition.getX());
            p_9197_.putInt("SpawnY", this.respawnPosition.getY());
            p_9197_.putInt("SpawnZ", this.respawnPosition.getZ());
            p_9197_.putBoolean("SpawnForced", this.respawnForced);
            p_9197_.putFloat("SpawnAngle", this.respawnAngle);
            ResourceLocation.CODEC
            .encodeStart(NbtOps.INSTANCE, this.respawnDimension.location())
            .resultOrPartial(LOGGER::error)
            .ifPresent(p_248207_ -> p_9197_.put("SpawnDimension", p_248207_));
        }

        p_9197_.putBoolean("spawn_extra_particles_on_fall", this.spawnExtraParticlesOnFall);

        if (this.raidOmenPosition != null)
        {
            BlockPos.CODEC
            .encodeStart(NbtOps.INSTANCE, this.raidOmenPosition)
            .resultOrPartial(LOGGER::error)
            .ifPresent(p_326433_ -> p_9197_.put("raid_omen_position", p_326433_));
        }
    }

    public void setExperiencePoints(int p_8986_)
    {
        float f = (float)this.getXpNeededForNextLevel();
        float f1 = (f - 1.0F) / f;
        this.experienceProgress = Mth.clamp((float)p_8986_ / f, 0.0F, f1);
        this.lastSentExp = -1;
    }

    public void setExperienceLevels(int p_9175_)
    {
        this.experienceLevel = p_9175_;
        this.lastSentExp = -1;
    }

    @Override
    public void giveExperienceLevels(int p_9200_)
    {
        super.giveExperienceLevels(p_9200_);
        this.lastSentExp = -1;
    }

    @Override
    public void onEnchantmentPerformed(ItemStack p_9079_, int p_9080_)
    {
        super.onEnchantmentPerformed(p_9079_, p_9080_);
        this.lastSentExp = -1;
    }

    private void initMenu(AbstractContainerMenu p_143400_)
    {
        p_143400_.addSlotListener(this.containerListener);
        p_143400_.setSynchronizer(this.containerSynchronizer);
    }

    public void initInventoryMenu()
    {
        this.initMenu(this.inventoryMenu);
    }

    @Override
    public void onEnterCombat()
    {
        super.onEnterCombat();
        this.connection.send(ClientboundPlayerCombatEnterPacket.INSTANCE);
    }

    @Override
    public void onLeaveCombat()
    {
        super.onLeaveCombat();
        this.connection.send(new ClientboundPlayerCombatEndPacket(this.getCombatTracker()));
    }

    @Override
    public void onInsideBlock(BlockState p_9103_)
    {
        CriteriaTriggers.ENTER_BLOCK.trigger(this, p_9103_);
    }

    @Override
    protected ItemCooldowns createItemCooldowns()
    {
        return new ServerItemCooldowns(this);
    }

    @Override
    public void tick()
    {
        this.gameMode.tick();
        this.wardenSpawnTracker.tick();
        this.spawnInvulnerableTime--;

        if (this.invulnerableTime > 0)
        {
            this.invulnerableTime--;
        }

        this.containerMenu.broadcastChanges();

        if (!this.level().isClientSide && !this.containerMenu.stillValid(this))
        {
            this.closeContainer();
            this.containerMenu = this.inventoryMenu;
        }

        Entity entity = this.getCamera();

        if (entity != this)
        {
            if (entity.isAlive())
            {
                this.absMoveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                this.serverLevel().getChunkSource().move(this);

                if (this.wantsToStopRiding())
                {
                    this.setCamera(this);
                }
            }
            else
            {
                this.setCamera(this);
            }
        }

        CriteriaTriggers.TICK.trigger(this);

        if (this.levitationStartPos != null)
        {
            CriteriaTriggers.LEVITATION.trigger(this, this.levitationStartPos, this.tickCount - this.levitationStartTime);
        }

        this.trackStartFallingPosition();
        this.trackEnteredOrExitedLavaOnVehicle();
        this.updatePlayerAttributes();
        this.advancements.flushDirty(this);
    }

    private void updatePlayerAttributes()
    {
        AttributeInstance attributeinstance = this.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);

        if (attributeinstance != null)
        {
            if (this.isCreative())
            {
                attributeinstance.addOrUpdateTransientModifier(CREATIVE_BLOCK_INTERACTION_RANGE_MODIFIER);
            }
            else
            {
                attributeinstance.removeModifier(CREATIVE_BLOCK_INTERACTION_RANGE_MODIFIER);
            }
        }

        AttributeInstance attributeinstance1 = this.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);

        if (attributeinstance1 != null)
        {
            if (this.isCreative())
            {
                attributeinstance1.addOrUpdateTransientModifier(CREATIVE_ENTITY_INTERACTION_RANGE_MODIFIER);
            }
            else
            {
                attributeinstance1.removeModifier(CREATIVE_ENTITY_INTERACTION_RANGE_MODIFIER);
            }
        }
    }

    public void doTick()
    {
        try
        {
            if (!this.isSpectator() || !this.touchingUnloadedChunk())
            {
                super.tick();
            }

            for (int i = 0; i < this.getInventory().getContainerSize(); i++)
            {
                ItemStack itemstack = this.getInventory().getItem(i);

                if (itemstack.getItem().isComplex())
                {
                    Packet<?> packet = ((ComplexItem)itemstack.getItem()).getUpdatePacket(itemstack, this.level(), this);

                    if (packet != null)
                    {
                        this.connection.send(packet);
                    }
                }
            }

            if (this.getHealth() != this.lastSentHealth || this.lastSentFood != this.foodData.getFoodLevel() || this.foodData.getSaturationLevel() == 0.0F != this.lastFoodSaturationZero)
            {
                this.connection.send(new ClientboundSetHealthPacket(this.getHealth(), this.foodData.getFoodLevel(), this.foodData.getSaturationLevel()));
                this.lastSentHealth = this.getHealth();
                this.lastSentFood = this.foodData.getFoodLevel();
                this.lastFoodSaturationZero = this.foodData.getSaturationLevel() == 0.0F;
            }

            if (this.getHealth() + this.getAbsorptionAmount() != this.lastRecordedHealthAndAbsorption)
            {
                this.lastRecordedHealthAndAbsorption = this.getHealth() + this.getAbsorptionAmount();
                this.updateScoreForCriteria(ObjectiveCriteria.HEALTH, Mth.ceil(this.lastRecordedHealthAndAbsorption));
            }

            if (this.foodData.getFoodLevel() != this.lastRecordedFoodLevel)
            {
                this.lastRecordedFoodLevel = this.foodData.getFoodLevel();
                this.updateScoreForCriteria(ObjectiveCriteria.FOOD, Mth.ceil((float)this.lastRecordedFoodLevel));
            }

            if (this.getAirSupply() != this.lastRecordedAirLevel)
            {
                this.lastRecordedAirLevel = this.getAirSupply();
                this.updateScoreForCriteria(ObjectiveCriteria.AIR, Mth.ceil((float)this.lastRecordedAirLevel));
            }

            if (this.getArmorValue() != this.lastRecordedArmor)
            {
                this.lastRecordedArmor = this.getArmorValue();
                this.updateScoreForCriteria(ObjectiveCriteria.ARMOR, Mth.ceil((float)this.lastRecordedArmor));
            }

            if (this.totalExperience != this.lastRecordedExperience)
            {
                this.lastRecordedExperience = this.totalExperience;
                this.updateScoreForCriteria(ObjectiveCriteria.EXPERIENCE, Mth.ceil((float)this.lastRecordedExperience));
            }

            if (this.experienceLevel != this.lastRecordedLevel)
            {
                this.lastRecordedLevel = this.experienceLevel;
                this.updateScoreForCriteria(ObjectiveCriteria.LEVEL, Mth.ceil((float)this.lastRecordedLevel));
            }

            if (this.totalExperience != this.lastSentExp)
            {
                this.lastSentExp = this.totalExperience;
                this.connection.send(new ClientboundSetExperiencePacket(this.experienceProgress, this.totalExperience, this.experienceLevel));
            }

            if (this.tickCount % 20 == 0)
            {
                CriteriaTriggers.LOCATION.trigger(this);
            }
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking player");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Player being ticked");
            this.fillCrashReportCategory(crashreportcategory);
            throw new ReportedException(crashreport);
        }
    }

    @Override
    public void resetFallDistance()
    {
        if (this.getHealth() > 0.0F && this.startingToFallPosition != null)
        {
            CriteriaTriggers.FALL_FROM_HEIGHT.trigger(this, this.startingToFallPosition);
        }

        this.startingToFallPosition = null;
        super.resetFallDistance();
    }

    public void trackStartFallingPosition()
    {
        if (this.fallDistance > 0.0F && this.startingToFallPosition == null)
        {
            this.startingToFallPosition = this.position();

            if (this.currentImpulseImpactPos != null && this.currentImpulseImpactPos.y <= this.startingToFallPosition.y)
            {
                CriteriaTriggers.FALL_AFTER_EXPLOSION.trigger(this, this.currentImpulseImpactPos, this.currentExplosionCause);
            }
        }
    }

    public void trackEnteredOrExitedLavaOnVehicle()
    {
        if (this.getVehicle() != null && this.getVehicle().isInLava())
        {
            if (this.enteredLavaOnVehiclePosition == null)
            {
                this.enteredLavaOnVehiclePosition = this.position();
            }
            else
            {
                CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER.trigger(this, this.enteredLavaOnVehiclePosition);
            }
        }

        if (this.enteredLavaOnVehiclePosition != null && (this.getVehicle() == null || !this.getVehicle().isInLava()))
        {
            this.enteredLavaOnVehiclePosition = null;
        }
    }

    private void updateScoreForCriteria(ObjectiveCriteria p_9105_, int p_9106_)
    {
        this.getScoreboard().forAllObjectives(p_9105_, this, p_308949_ -> p_308949_.set(p_9106_));
    }

    @Override
    public void die(DamageSource p_9035_)
    {
        this.gameEvent(GameEvent.ENTITY_DIE);
        boolean flag = this.level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);

        if (flag)
        {
            Component component = this.getCombatTracker().getDeathMessage();
            this.connection
            .send(
                new ClientboundPlayerCombatKillPacket(this.getId(), component),
                PacketSendListener.exceptionallySend(
                    () ->
            {
                int i = 256;
                String s = component.getString(256);
                Component component1 = Component.translatable("death.attack.message_too_long", Component.literal(s).withStyle(ChatFormatting.YELLOW));
                Component component2 = Component.translatable("death.attack.even_more_magic", this.getDisplayName())
                .withStyle(p_143420_ -> p_143420_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component1)));
                return new ClientboundPlayerCombatKillPacket(this.getId(), component2);
            }
                )
            );
            Team team = this.getTeam();

            if (team == null || team.getDeathMessageVisibility() == Team.Visibility.ALWAYS)
            {
                this.server.getPlayerList().broadcastSystemMessage(component, false);
            }
            else if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OTHER_TEAMS)
            {
                this.server.getPlayerList().broadcastSystemToTeam(this, component);
            }
            else if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OWN_TEAM)
            {
                this.server.getPlayerList().broadcastSystemToAllExceptTeam(this, component);
            }
        }
        else
        {
            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getId(), CommonComponents.EMPTY));
        }

        this.removeEntitiesOnShoulder();

        if (this.level().getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS))
        {
            this.tellNeutralMobsThatIDied();
        }

        if (!this.isSpectator())
        {
            this.dropAllDeathLoot(this.serverLevel(), p_9035_);
        }

        this.getScoreboard().forAllObjectives(ObjectiveCriteria.DEATH_COUNT, this, ScoreAccess::increment);
        LivingEntity livingentity = this.getKillCredit();

        if (livingentity != null)
        {
            this.awardStat(Stats.ENTITY_KILLED_BY.get(livingentity.getType()));
            livingentity.awardKillScore(this, this.deathScore, p_9035_);
            this.createWitherRose(livingentity);
        }

        this.level().broadcastEntityEvent(this, (byte)3);
        this.awardStat(Stats.DEATHS);
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        this.clearFire();
        this.setTicksFrozen(0);
        this.setSharedFlagOnFire(false);
        this.getCombatTracker().recheckStatus();
        this.setLastDeathLocation(Optional.of(GlobalPos.of(this.level().dimension(), this.blockPosition())));
    }

    private void tellNeutralMobsThatIDied()
    {
        AABB aabb = new AABB(this.blockPosition()).inflate(32.0, 10.0, 32.0);
        this.level()
        .getEntitiesOfClass(Mob.class, aabb, EntitySelector.NO_SPECTATORS)
        .stream()
        .filter(p_9188_ -> p_9188_ instanceof NeutralMob)
        .forEach(p_9057_ -> ((NeutralMob)p_9057_).playerDied(this));
    }

    @Override
    public void awardKillScore(Entity p_9050_, int p_9051_, DamageSource p_9052_)
    {
        if (p_9050_ != this)
        {
            super.awardKillScore(p_9050_, p_9051_, p_9052_);
            this.increaseScore(p_9051_);
            this.getScoreboard().forAllObjectives(ObjectiveCriteria.KILL_COUNT_ALL, this, ScoreAccess::increment);

            if (p_9050_ instanceof Player)
            {
                this.awardStat(Stats.PLAYER_KILLS);
                this.getScoreboard().forAllObjectives(ObjectiveCriteria.KILL_COUNT_PLAYERS, this, ScoreAccess::increment);
            }
            else
            {
                this.awardStat(Stats.MOB_KILLS);
            }

            this.handleTeamKill(this, p_9050_, ObjectiveCriteria.TEAM_KILL);
            this.handleTeamKill(p_9050_, this, ObjectiveCriteria.KILLED_BY_TEAM);
            CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(this, p_9050_, p_9052_);
        }
    }

    private void handleTeamKill(ScoreHolder p_312242_, ScoreHolder p_312349_, ObjectiveCriteria[] p_9127_)
    {
        PlayerTeam playerteam = this.getScoreboard().getPlayersTeam(p_312349_.getScoreboardName());

        if (playerteam != null)
        {
            int i = playerteam.getColor().getId();

            if (i >= 0 && i < p_9127_.length)
            {
                this.getScoreboard().forAllObjectives(p_9127_[i], p_312242_, ScoreAccess::increment);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource p_9037_, float p_9038_)
    {
        if (this.isInvulnerableTo(p_9037_))
        {
            return false;
        }
        else
        {
            boolean flag = this.server.isDedicatedServer() && this.isPvpAllowed() && p_9037_.is(DamageTypeTags.IS_FALL);

            if (!flag && this.spawnInvulnerableTime > 0 && !p_9037_.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            {
                return false;
            }
            else
            {
                Entity entity = p_9037_.getEntity();

                if (entity instanceof Player player && !this.canHarmPlayer(player))
                {
                    return false;
                }

                if (entity instanceof AbstractArrow abstractarrow && abstractarrow.getOwner() instanceof Player player1 && !this.canHarmPlayer(player1))
                {
                    return false;
                }

                return super.hurt(p_9037_, p_9038_);
            }
        }
    }

    @Override
    public boolean canHarmPlayer(Player p_9064_)
    {
        return !this.isPvpAllowed() ? false : super.canHarmPlayer(p_9064_);
    }

    private boolean isPvpAllowed()
    {
        return this.server.isPvpAllowed();
    }

    public DimensionTransition findRespawnPositionAndUseSpawnBlock(boolean p_342433_, DimensionTransition.PostDimensionTransition p_343860_)
    {
        BlockPos blockpos = this.getRespawnPosition();
        float f = this.getRespawnAngle();
        boolean flag = this.isRespawnForced();
        ServerLevel serverlevel = this.server.getLevel(this.getRespawnDimension());

        if (serverlevel != null && blockpos != null)
        {
            Optional<ServerPlayer.RespawnPosAngle> optional = findRespawnAndUseSpawnBlock(serverlevel, blockpos, f, flag, p_342433_);

            if (optional.isPresent())
            {
                ServerPlayer.RespawnPosAngle serverplayer$respawnposangle = optional.get();
                return new DimensionTransition(
                           serverlevel, serverplayer$respawnposangle.position(), Vec3.ZERO, serverplayer$respawnposangle.yaw(), 0.0F, p_343860_
                       );
            }
            else
            {
                return DimensionTransition.missingRespawnBlock(this.server.overworld(), this, p_343860_);
            }
        }
        else
        {
            return new DimensionTransition(this.server.overworld(), this, p_343860_);
        }
    }

    private static Optional<ServerPlayer.RespawnPosAngle> findRespawnAndUseSpawnBlock(
        ServerLevel p_343173_, BlockPos p_345429_, float p_342316_, boolean p_345318_, boolean p_343357_
    )
    {
        BlockState blockstate = p_343173_.getBlockState(p_345429_);
        Block block = blockstate.getBlock();

        if (block instanceof RespawnAnchorBlock
                && (p_345318_ || blockstate.getValue(RespawnAnchorBlock.CHARGE) > 0)
                && RespawnAnchorBlock.canSetSpawn(p_343173_))
        {
            Optional<Vec3> optional = RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, p_343173_, p_345429_);

            if (!p_345318_ && !p_343357_ && optional.isPresent())
            {
                p_343173_.setBlock(
                    p_345429_, blockstate.setValue(RespawnAnchorBlock.CHARGE, Integer.valueOf(blockstate.getValue(RespawnAnchorBlock.CHARGE) - 1)), 3
                );
            }

            return optional.map(p_341237_ -> ServerPlayer.RespawnPosAngle.of(p_341237_, p_345429_));
        }
        else if (block instanceof BedBlock && BedBlock.canSetSpawn(p_343173_))
        {
            return BedBlock.findStandUpPosition(EntityType.PLAYER, p_343173_, p_345429_, blockstate.getValue(BedBlock.FACING), p_342316_)
                   .map(p_341240_ -> ServerPlayer.RespawnPosAngle.of(p_341240_, p_345429_));
        }
        else if (!p_345318_)
        {
            return Optional.empty();
        }
        else
        {
            boolean flag = block.isPossibleToRespawnInThis(blockstate);
            BlockState blockstate1 = p_343173_.getBlockState(p_345429_.above());
            boolean flag1 = blockstate1.getBlock().isPossibleToRespawnInThis(blockstate1);
            return flag && flag1
                   ? Optional.of(
                       new ServerPlayer.RespawnPosAngle(
                           new Vec3((double)p_345429_.getX() + 0.5, (double)p_345429_.getY() + 0.1, (double)p_345429_.getZ() + 0.5), p_342316_
                       )
                   )
                   : Optional.empty();
        }
    }

    public void showEndCredits()
    {
        this.unRide();
        this.serverLevel().removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);

        if (!this.wonGame)
        {
            this.wonGame = true;
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0.0F));
            this.seenCredits = true;
        }
    }

    @Nullable
    @Override
    public Entity changeDimension(DimensionTransition p_344993_)
    {
        if (this.isRemoved())
        {
            return null;
        }
        else
        {
            if (p_344993_.missingRespawnBlock())
            {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
            }

            ServerLevel serverlevel = p_344993_.newLevel();
            ServerLevel serverlevel1 = this.serverLevel();
            ResourceKey<Level> resourcekey = serverlevel1.dimension();

            if (serverlevel.dimension() == resourcekey)
            {
                this.connection
                .teleport(
                    p_344993_.pos().x,
                    p_344993_.pos().y,
                    p_344993_.pos().z,
                    p_344993_.yRot(),
                    p_344993_.xRot()
                );
                this.connection.resetPosition();
                p_344993_.postDimensionTransition().onTransition(this);
                return this;
            }
            else
            {
                this.isChangingDimension = true;
                LevelData leveldata = serverlevel.getLevelData();
                this.connection.send(new ClientboundRespawnPacket(this.createCommonSpawnInfo(serverlevel), (byte)3));
                this.connection.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
                PlayerList playerlist = this.server.getPlayerList();
                playerlist.sendPlayerPermissionLevel(this);
                serverlevel1.removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
                this.unsetRemoved();
                serverlevel1.getProfiler().push("moving");

                if (resourcekey == Level.OVERWORLD && serverlevel.dimension() == Level.NETHER)
                {
                    this.enteredNetherPosition = this.position();
                }

                serverlevel1.getProfiler().pop();
                serverlevel1.getProfiler().push("placing");
                this.setServerLevel(serverlevel);
                this.connection
                .teleport(
                    p_344993_.pos().x,
                    p_344993_.pos().y,
                    p_344993_.pos().z,
                    p_344993_.yRot(),
                    p_344993_.xRot()
                );
                this.connection.resetPosition();
                serverlevel.addDuringTeleport(this);
                serverlevel1.getProfiler().pop();
                this.triggerDimensionChangeTriggers(serverlevel1);
                this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
                playerlist.sendLevelInfo(this, serverlevel);
                playerlist.sendAllPlayerInfo(this);
                playerlist.sendActivePlayerEffects(this);
                p_344993_.postDimensionTransition().onTransition(this);
                this.lastSentExp = -1;
                this.lastSentHealth = -1.0F;
                this.lastSentFood = -1;
                return this;
            }
        }
    }

    private void triggerDimensionChangeTriggers(ServerLevel p_9210_)
    {
        ResourceKey<Level> resourcekey = p_9210_.dimension();
        ResourceKey<Level> resourcekey1 = this.level().dimension();
        CriteriaTriggers.CHANGED_DIMENSION.trigger(this, resourcekey, resourcekey1);

        if (resourcekey == Level.NETHER && resourcekey1 == Level.OVERWORLD && this.enteredNetherPosition != null)
        {
            CriteriaTriggers.NETHER_TRAVEL.trigger(this, this.enteredNetherPosition);
        }

        if (resourcekey1 != Level.NETHER)
        {
            this.enteredNetherPosition = null;
        }
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer p_9014_)
    {
        if (p_9014_.isSpectator())
        {
            return this.getCamera() == this;
        }
        else
        {
            return this.isSpectator() ? false : super.broadcastToPlayer(p_9014_);
        }
    }

    @Override
    public void take(Entity p_9047_, int p_9048_)
    {
        super.take(p_9047_, p_9048_);
        this.containerMenu.broadcastChanges();
    }

    @Override
    public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos p_9115_)
    {
        Direction direction = this.level().getBlockState(p_9115_).getValue(HorizontalDirectionalBlock.FACING);

        if (this.isSleeping() || !this.isAlive())
        {
            return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
        else if (!this.level().dimensionType().natural())
        {
            return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
        }
        else if (!this.bedInRange(p_9115_, direction))
        {
            return Either.left(Player.BedSleepingProblem.TOO_FAR_AWAY);
        }
        else if (this.bedBlocked(p_9115_, direction))
        {
            return Either.left(Player.BedSleepingProblem.OBSTRUCTED);
        }
        else
        {
            this.setRespawnPosition(this.level().dimension(), p_9115_, this.getYRot(), false, true);

            if (this.level().isDay())
            {
                return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            }
            else
            {
                if (!this.isCreative())
                {
                    double d0 = 8.0;
                    double d1 = 5.0;
                    Vec3 vec3 = Vec3.atBottomCenterOf(p_9115_);
                    List<Monster> list = this.level()
                                         .getEntitiesOfClass(
                                             Monster.class,
                                             new AABB(
                                                 vec3.x() - 8.0,
                                                 vec3.y() - 5.0,
                                                 vec3.z() - 8.0,
                                                 vec3.x() + 8.0,
                                                 vec3.y() + 5.0,
                                                 vec3.z() + 8.0
                                             ),
                                             p_9062_ -> p_9062_.isPreventingPlayerRest(this)
                                         );

                    if (!list.isEmpty())
                    {
                        return Either.left(Player.BedSleepingProblem.NOT_SAFE);
                    }
                }

                Either<Player.BedSleepingProblem, Unit> either = super.startSleepInBed(p_9115_).ifRight(p_9029_ ->
                {
                    this.awardStat(Stats.SLEEP_IN_BED);
                    CriteriaTriggers.SLEPT_IN_BED.trigger(this);
                });

                if (!this.serverLevel().canSleepThroughNights())
                {
                    this.displayClientMessage(Component.translatable("sleep.not_possible"), true);
                }

                ((ServerLevel)this.level()).updateSleepingPlayerList();
                return either;
            }
        }
    }

    @Override
    public void startSleeping(BlockPos p_9190_)
    {
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        super.startSleeping(p_9190_);
    }

    private boolean bedInRange(BlockPos p_9117_, Direction p_9118_)
    {
        return this.isReachableBedBlock(p_9117_) || this.isReachableBedBlock(p_9117_.relative(p_9118_.getOpposite()));
    }

    private boolean isReachableBedBlock(BlockPos p_9223_)
    {
        Vec3 vec3 = Vec3.atBottomCenterOf(p_9223_);
        return Math.abs(this.getX() - vec3.x()) <= 3.0
               && Math.abs(this.getY() - vec3.y()) <= 2.0
               && Math.abs(this.getZ() - vec3.z()) <= 3.0;
    }

    private boolean bedBlocked(BlockPos p_9192_, Direction p_9193_)
    {
        BlockPos blockpos = p_9192_.above();
        return !this.freeAt(blockpos) || !this.freeAt(blockpos.relative(p_9193_.getOpposite()));
    }

    @Override
    public void stopSleepInBed(boolean p_9165_, boolean p_9166_)
    {
        if (this.isSleeping())
        {
            this.serverLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(this, 2));
        }

        super.stopSleepInBed(p_9165_, p_9166_);

        if (this.connection != null)
        {
            this.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        }
    }

    @Override
    public void dismountTo(double p_143389_, double p_143390_, double p_143391_)
    {
        this.removeVehicle();
        this.setPos(p_143389_, p_143390_, p_143391_);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource p_9182_)
    {
        return super.isInvulnerableTo(p_9182_) || this.isChangingDimension();
    }

    @Override
    protected void checkFallDamage(double p_8976_, boolean p_8977_, BlockState p_8978_, BlockPos p_8979_)
    {
    }

    @Override
    protected void onChangedBlock(ServerLevel p_345082_, BlockPos p_9206_)
    {
        if (!this.isSpectator())
        {
            super.onChangedBlock(p_345082_, p_9206_);
        }
    }

    public void doCheckFallDamage(double p_289676_, double p_289671_, double p_289665_, boolean p_289696_)
    {
        if (!this.touchingUnloadedChunk())
        {
            this.checkSupportingBlock(p_289696_, new Vec3(p_289676_, p_289671_, p_289665_));
            BlockPos blockpos = this.getOnPosLegacy();
            BlockState blockstate = this.level().getBlockState(blockpos);

            if (this.spawnExtraParticlesOnFall && p_289696_ && this.fallDistance > 0.0F)
            {
                Vec3 vec3 = blockpos.getCenter().add(0.0, 0.5, 0.0);
                int i = (int)Mth.clamp(50.0F * this.fallDistance, 0.0F, 200.0F);
                this.serverLevel()
                .sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, blockstate), vec3.x, vec3.y, vec3.z, i, 0.3F, 0.3F, 0.3F, 0.15F
                );
                this.spawnExtraParticlesOnFall = false;
            }

            super.checkFallDamage(p_289671_, p_289696_, blockstate, blockpos);
        }
    }

    @Override
    public void onExplosionHit(@Nullable Entity p_328773_)
    {
        super.onExplosionHit(p_328773_);
        this.currentImpulseImpactPos = this.position();
        this.currentExplosionCause = p_328773_;
        this.setIgnoreFallDamageFromCurrentImpulse(p_328773_ != null && p_328773_.getType() == EntityType.WIND_CHARGE);
    }

    @Override
    protected void pushEntities()
    {
        if (this.level().tickRateManager().runsNormally())
        {
            super.pushEntities();
        }
    }

    @Override
    public void openTextEdit(SignBlockEntity p_277909_, boolean p_277495_)
    {
        this.connection.send(new ClientboundBlockUpdatePacket(this.level(), p_277909_.getBlockPos()));
        this.connection.send(new ClientboundOpenSignEditorPacket(p_277909_.getBlockPos(), p_277495_));
    }

    private void nextContainerCounter()
    {
        this.containerCounter = this.containerCounter % 100 + 1;
    }

    @Override
    public OptionalInt openMenu(@Nullable MenuProvider p_9033_)
    {
        if (p_9033_ == null)
        {
            return OptionalInt.empty();
        }
        else
        {
            if (this.containerMenu != this.inventoryMenu)
            {
                this.closeContainer();
            }

            this.nextContainerCounter();
            AbstractContainerMenu abstractcontainermenu = p_9033_.createMenu(this.containerCounter, this.getInventory(), this);

            if (abstractcontainermenu == null)
            {
                if (this.isSpectator())
                {
                    this.displayClientMessage(Component.translatable("container.spectatorCantOpen").withStyle(ChatFormatting.RED), true);
                }

                return OptionalInt.empty();
            }
            else
            {
                this.connection.send(new ClientboundOpenScreenPacket(abstractcontainermenu.containerId, abstractcontainermenu.getType(), p_9033_.getDisplayName()));
                this.initMenu(abstractcontainermenu);
                this.containerMenu = abstractcontainermenu;
                return OptionalInt.of(this.containerCounter);
            }
        }
    }

    @Override
    public void sendMerchantOffers(int p_8988_, MerchantOffers p_8989_, int p_8990_, int p_8991_, boolean p_8992_, boolean p_8993_)
    {
        this.connection.send(new ClientboundMerchantOffersPacket(p_8988_, p_8989_, p_8990_, p_8991_, p_8992_, p_8993_));
    }

    @Override
    public void openHorseInventory(AbstractHorse p_9059_, Container p_9060_)
    {
        if (this.containerMenu != this.inventoryMenu)
        {
            this.closeContainer();
        }

        this.nextContainerCounter();
        int i = p_9059_.getInventoryColumns();
        this.connection.send(new ClientboundHorseScreenOpenPacket(this.containerCounter, i, p_9059_.getId()));
        this.containerMenu = new HorseInventoryMenu(this.containerCounter, this.getInventory(), p_9060_, p_9059_, i);
        this.initMenu(this.containerMenu);
    }

    @Override
    public void openItemGui(ItemStack p_9082_, InteractionHand p_9083_)
    {
        if (p_9082_.is(Items.WRITTEN_BOOK))
        {
            if (WrittenBookItem.resolveBookComponents(p_9082_, this.createCommandSourceStack(), this))
            {
                this.containerMenu.broadcastChanges();
            }

            this.connection.send(new ClientboundOpenBookPacket(p_9083_));
        }
    }

    @Override
    public void openCommandBlock(CommandBlockEntity p_9099_)
    {
        this.connection.send(ClientboundBlockEntityDataPacket.create(p_9099_, BlockEntity::saveCustomOnly));
    }

    @Override
    public void closeContainer()
    {
        this.connection.send(new ClientboundContainerClosePacket(this.containerMenu.containerId));
        this.doCloseContainer();
    }

    @Override
    public void doCloseContainer()
    {
        this.containerMenu.removed(this);
        this.inventoryMenu.transferState(this.containerMenu);
        this.containerMenu = this.inventoryMenu;
    }

    public void setPlayerInput(float p_8981_, float p_8982_, boolean p_8983_, boolean p_8984_)
    {
        if (this.isPassenger())
        {
            if (p_8981_ >= -1.0F && p_8981_ <= 1.0F)
            {
                this.xxa = p_8981_;
            }

            if (p_8982_ >= -1.0F && p_8982_ <= 1.0F)
            {
                this.zza = p_8982_;
            }

            this.jumping = p_8983_;
            this.setShiftKeyDown(p_8984_);
        }
    }

    @Override
    public void travel(Vec3 p_312746_)
    {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.travel(p_312746_);
        this.checkMovementStatistics(this.getX() - d0, this.getY() - d1, this.getZ() - d2);
    }

    @Override
    public void rideTick()
    {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.rideTick();
        this.checkRidingStatistics(this.getX() - d0, this.getY() - d1, this.getZ() - d2);
    }

    public void checkMovementStatistics(double p_310268_, double p_310728_, double p_313145_)
    {
        if (!this.isPassenger() && !didNotMove(p_310268_, p_310728_, p_313145_))
        {
            if (this.isSwimming())
            {
                int i = Math.round((float)Math.sqrt(p_310268_ * p_310268_ + p_310728_ * p_310728_ + p_313145_ * p_313145_) * 100.0F);

                if (i > 0)
                {
                    this.awardStat(Stats.SWIM_ONE_CM, i);
                    this.causeFoodExhaustion(0.01F * (float)i * 0.01F);
                }
            }
            else if (this.isEyeInFluid(FluidTags.WATER))
            {
                int j = Math.round((float)Math.sqrt(p_310268_ * p_310268_ + p_310728_ * p_310728_ + p_313145_ * p_313145_) * 100.0F);

                if (j > 0)
                {
                    this.awardStat(Stats.WALK_UNDER_WATER_ONE_CM, j);
                    this.causeFoodExhaustion(0.01F * (float)j * 0.01F);
                }
            }
            else if (this.isInWater())
            {
                int k = Math.round((float)Math.sqrt(p_310268_ * p_310268_ + p_313145_ * p_313145_) * 100.0F);

                if (k > 0)
                {
                    this.awardStat(Stats.WALK_ON_WATER_ONE_CM, k);
                    this.causeFoodExhaustion(0.01F * (float)k * 0.01F);
                }
            }
            else if (this.onClimbable())
            {
                if (p_310728_ > 0.0)
                {
                    this.awardStat(Stats.CLIMB_ONE_CM, (int)Math.round(p_310728_ * 100.0));
                }
            }
            else if (this.onGround())
            {
                int l = Math.round((float)Math.sqrt(p_310268_ * p_310268_ + p_313145_ * p_313145_) * 100.0F);

                if (l > 0)
                {
                    if (this.isSprinting())
                    {
                        this.awardStat(Stats.SPRINT_ONE_CM, l);
                        this.causeFoodExhaustion(0.1F * (float)l * 0.01F);
                    }
                    else if (this.isCrouching())
                    {
                        this.awardStat(Stats.CROUCH_ONE_CM, l);
                        this.causeFoodExhaustion(0.0F * (float)l * 0.01F);
                    }
                    else
                    {
                        this.awardStat(Stats.WALK_ONE_CM, l);
                        this.causeFoodExhaustion(0.0F * (float)l * 0.01F);
                    }
                }
            }
            else if (this.isFallFlying())
            {
                int i1 = Math.round((float)Math.sqrt(p_310268_ * p_310268_ + p_310728_ * p_310728_ + p_313145_ * p_313145_) * 100.0F);
                this.awardStat(Stats.AVIATE_ONE_CM, i1);
            }
            else
            {
                int j1 = Math.round((float)Math.sqrt(p_310268_ * p_310268_ + p_313145_ * p_313145_) * 100.0F);

                if (j1 > 25)
                {
                    this.awardStat(Stats.FLY_ONE_CM, j1);
                }
            }
        }
    }

    private void checkRidingStatistics(double p_310768_, double p_312944_, double p_309791_)
    {
        if (this.isPassenger() && !didNotMove(p_310768_, p_312944_, p_309791_))
        {
            int i = Math.round((float)Math.sqrt(p_310768_ * p_310768_ + p_312944_ * p_312944_ + p_309791_ * p_309791_) * 100.0F);
            Entity entity = this.getVehicle();

            if (entity instanceof AbstractMinecart)
            {
                this.awardStat(Stats.MINECART_ONE_CM, i);
            }
            else if (entity instanceof Boat)
            {
                this.awardStat(Stats.BOAT_ONE_CM, i);
            }
            else if (entity instanceof Pig)
            {
                this.awardStat(Stats.PIG_ONE_CM, i);
            }
            else if (entity instanceof AbstractHorse)
            {
                this.awardStat(Stats.HORSE_ONE_CM, i);
            }
            else if (entity instanceof Strider)
            {
                this.awardStat(Stats.STRIDER_ONE_CM, i);
            }
        }
    }

    private static boolean didNotMove(double p_310773_, double p_310271_, double p_312126_)
    {
        return p_310773_ == 0.0 && p_310271_ == 0.0 && p_312126_ == 0.0;
    }

    @Override
    public void awardStat(Stat<?> p_9026_, int p_9027_)
    {
        this.stats.increment(this, p_9026_, p_9027_);
        this.getScoreboard().forAllObjectives(p_9026_, this, p_308946_ -> p_308946_.add(p_9027_));
    }

    @Override
    public void resetStat(Stat<?> p_9024_)
    {
        this.stats.setValue(this, p_9024_, 0);
        this.getScoreboard().forAllObjectives(p_9024_, this, ScoreAccess::reset);
    }

    @Override
    public int awardRecipes(Collection < RecipeHolder<? >> p_9129_)
    {
        return this.recipeBook.addRecipes(p_9129_, this);
    }

    @Override
    public void triggerRecipeCrafted(RecipeHolder<?> p_299743_, List<ItemStack> p_282336_)
    {
        CriteriaTriggers.RECIPE_CRAFTED.trigger(this, p_299743_.id(), p_282336_);
    }

    @Override
    public void awardRecipesByKey(List<ResourceLocation> p_312871_)
    {
        List < RecipeHolder<? >> list = p_312871_.stream()
                                        .flatMap(p_308947_ -> this.server.getRecipeManager().byKey(p_308947_).stream())
                                        .collect(Collectors.toList());
        this.awardRecipes(list);
    }

    @Override
    public int resetRecipes(Collection < RecipeHolder<? >> p_9195_)
    {
        return this.recipeBook.removeRecipes(p_9195_, this);
    }

    @Override
    public void giveExperiencePoints(int p_9208_)
    {
        super.giveExperiencePoints(p_9208_);
        this.lastSentExp = -1;
    }

    public void disconnect()
    {
        this.disconnected = true;
        this.ejectPassengers();

        if (this.isSleeping())
        {
            this.stopSleepInBed(true, false);
        }
    }

    public boolean hasDisconnected()
    {
        return this.disconnected;
    }

    public void resetSentInfo()
    {
        this.lastSentHealth = -1.0E8F;
    }

    @Override
    public void displayClientMessage(Component p_9154_, boolean p_9155_)
    {
        this.sendSystemMessage(p_9154_, p_9155_);
    }

    @Override
    protected void completeUsingItem()
    {
        if (!this.useItem.isEmpty() && this.isUsingItem())
        {
            this.connection.send(new ClientboundEntityEventPacket(this, (byte)9));
            super.completeUsingItem();
        }
    }

    @Override
    public void lookAt(EntityAnchorArgument.Anchor p_9112_, Vec3 p_9113_)
    {
        super.lookAt(p_9112_, p_9113_);
        this.connection.send(new ClientboundPlayerLookAtPacket(p_9112_, p_9113_.x, p_9113_.y, p_9113_.z));
    }

    public void lookAt(EntityAnchorArgument.Anchor p_9108_, Entity p_9109_, EntityAnchorArgument.Anchor p_9110_)
    {
        Vec3 vec3 = p_9110_.apply(p_9109_);
        super.lookAt(p_9108_, vec3);
        this.connection.send(new ClientboundPlayerLookAtPacket(p_9108_, p_9109_, p_9110_));
    }

    public void restoreFrom(ServerPlayer p_9016_, boolean p_9017_)
    {
        this.wardenSpawnTracker = p_9016_.wardenSpawnTracker;
        this.chatSession = p_9016_.chatSession;
        this.gameMode.setGameModeForPlayer(p_9016_.gameMode.getGameModeForPlayer(), p_9016_.gameMode.getPreviousGameModeForPlayer());
        this.onUpdateAbilities();
        this.getAttributes().assignBaseValues(p_9016_.getAttributes());
        this.setHealth(this.getMaxHealth());

        if (p_9017_)
        {
            this.getInventory().replaceWith(p_9016_.getInventory());
            this.setHealth(p_9016_.getHealth());
            this.foodData = p_9016_.foodData;

            for (MobEffectInstance mobeffectinstance : p_9016_.getActiveEffects())
            {
                this.addEffect(new MobEffectInstance(mobeffectinstance));
            }

            this.experienceLevel = p_9016_.experienceLevel;
            this.totalExperience = p_9016_.totalExperience;
            this.experienceProgress = p_9016_.experienceProgress;
            this.setScore(p_9016_.getScore());
            this.portalProcess = p_9016_.portalProcess;
        }
        else if (this.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || p_9016_.isSpectator())
        {
            this.getInventory().replaceWith(p_9016_.getInventory());
            this.experienceLevel = p_9016_.experienceLevel;
            this.totalExperience = p_9016_.totalExperience;
            this.experienceProgress = p_9016_.experienceProgress;
            this.setScore(p_9016_.getScore());
        }

        this.enchantmentSeed = p_9016_.enchantmentSeed;
        this.enderChestInventory = p_9016_.enderChestInventory;
        this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, p_9016_.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
        this.lastSentExp = -1;
        this.lastSentHealth = -1.0F;
        this.lastSentFood = -1;
        this.recipeBook.copyOverData(p_9016_.recipeBook);
        this.seenCredits = p_9016_.seenCredits;
        this.enteredNetherPosition = p_9016_.enteredNetherPosition;
        this.chunkTrackingView = p_9016_.chunkTrackingView;
        this.setShoulderEntityLeft(p_9016_.getShoulderEntityLeft());
        this.setShoulderEntityRight(p_9016_.getShoulderEntityRight());
        this.setLastDeathLocation(p_9016_.getLastDeathLocation());
    }

    @Override
    protected void onEffectAdded(MobEffectInstance p_143393_, @Nullable Entity p_143394_)
    {
        super.onEffectAdded(p_143393_, p_143394_);
        this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), p_143393_, true));

        if (p_143393_.is(MobEffects.LEVITATION))
        {
            this.levitationStartTime = this.tickCount;
            this.levitationStartPos = this.position();
        }

        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, p_143394_);
    }

    @Override
    protected void onEffectUpdated(MobEffectInstance p_143396_, boolean p_143397_, @Nullable Entity p_143398_)
    {
        super.onEffectUpdated(p_143396_, p_143397_, p_143398_);
        this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), p_143396_, false));
        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, p_143398_);
    }

    @Override
    protected void onEffectRemoved(MobEffectInstance p_9184_)
    {
        super.onEffectRemoved(p_9184_);
        this.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), p_9184_.getEffect()));

        if (p_9184_.is(MobEffects.LEVITATION))
        {
            this.levitationStartPos = null;
        }

        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, null);
    }

    @Override
    public void teleportTo(double p_8969_, double p_8970_, double p_8971_)
    {
        this.connection.teleport(p_8969_, p_8970_, p_8971_, this.getYRot(), this.getXRot(), RelativeMovement.ROTATION);
    }

    @Override
    public void teleportRelative(double p_251611_, double p_248861_, double p_252266_)
    {
        this.connection
        .teleport(
            this.getX() + p_251611_,
            this.getY() + p_248861_,
            this.getZ() + p_252266_,
            this.getYRot(),
            this.getXRot(),
            RelativeMovement.ALL
        );
    }

    @Override
    public boolean teleportTo(
        ServerLevel p_265564_, double p_265424_, double p_265680_, double p_265312_, Set<RelativeMovement> p_265192_, float p_265059_, float p_265266_
    )
    {
        ChunkPos chunkpos = new ChunkPos(BlockPos.containing(p_265424_, p_265680_, p_265312_));
        p_265564_.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkpos, 1, this.getId());
        this.stopRiding();

        if (this.isSleeping())
        {
            this.stopSleepInBed(true, true);
        }

        if (p_265564_ == this.level())
        {
            this.connection.teleport(p_265424_, p_265680_, p_265312_, p_265059_, p_265266_, p_265192_);
        }
        else
        {
            this.teleportTo(p_265564_, p_265424_, p_265680_, p_265312_, p_265059_, p_265266_);
        }

        this.setYHeadRot(p_265059_);
        return true;
    }

    @Override
    public void moveTo(double p_9171_, double p_9172_, double p_9173_)
    {
        super.moveTo(p_9171_, p_9172_, p_9173_);
        this.connection.resetPosition();
    }

    @Override
    public void crit(Entity p_9045_)
    {
        this.serverLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(p_9045_, 4));
    }

    @Override
    public void magicCrit(Entity p_9186_)
    {
        this.serverLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(p_9186_, 5));
    }

    @Override
    public void onUpdateAbilities()
    {
        if (this.connection != null)
        {
            this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
            this.updateInvisibilityStatus();
        }
    }

    public ServerLevel serverLevel()
    {
        return (ServerLevel)this.level();
    }

    public boolean setGameMode(GameType p_143404_)
    {
        boolean flag = this.isSpectator();

        if (!this.gameMode.changeGameModeForPlayer(p_143404_))
        {
            return false;
        }
        else
        {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, (float)p_143404_.getId()));

            if (p_143404_ == GameType.SPECTATOR)
            {
                this.removeEntitiesOnShoulder();
                this.stopRiding();
                EnchantmentHelper.stopLocationBasedEffects(this);
            }
            else
            {
                this.setCamera(this);

                if (flag)
                {
                    EnchantmentHelper.runLocationChangedEffects(this.serverLevel(), this);
                }
            }

            this.onUpdateAbilities();
            this.updateEffectVisibility();
            return true;
        }
    }

    @Override
    public boolean isSpectator()
    {
        return this.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
    }

    @Override
    public boolean isCreative()
    {
        return this.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
    }

    @Override
    public void sendSystemMessage(Component p_215097_)
    {
        this.sendSystemMessage(p_215097_, false);
    }

    public void sendSystemMessage(Component p_240560_, boolean p_240545_)
    {
        if (this.acceptsSystemMessages(p_240545_))
        {
            this.connection
            .send(
                new ClientboundSystemChatPacket(p_240560_, p_240545_),
                PacketSendListener.exceptionallySend(
                    () ->
            {
                if (this.acceptsSystemMessages(false))
                {
                    int i = 256;
                    String s = p_240560_.getString(256);
                    Component component = Component.literal(s).withStyle(ChatFormatting.YELLOW);
                    return new ClientboundSystemChatPacket(
                        Component.translatable("multiplayer.message_not_delivered", component).withStyle(ChatFormatting.RED), false
                    );
                }
                else {
                    return null;
                }
            }
                )
            );
        }
    }

    public void sendChatMessage(OutgoingChatMessage p_249852_, boolean p_250110_, ChatType.Bound p_252108_)
    {
        if (this.acceptsChatMessages())
        {
            p_249852_.sendToPlayer(this, p_250110_, p_252108_);
        }
    }

    public String getIpAddress()
    {
        return this.connection.getRemoteAddress() instanceof InetSocketAddress inetsocketaddress
               ? InetAddresses.toAddrString(inetsocketaddress.getAddress())
               : "<unknown>";
    }

    public void updateOptions(ClientInformation p_297843_)
    {
        this.language = p_297843_.language();
        this.requestedViewDistance = p_297843_.viewDistance();
        this.chatVisibility = p_297843_.chatVisibility();
        this.canChatColor = p_297843_.chatColors();
        this.textFilteringEnabled = p_297843_.textFilteringEnabled();
        this.allowsListing = p_297843_.allowsListing();
        this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte)p_297843_.modelCustomisation());
        this.getEntityData().set(DATA_PLAYER_MAIN_HAND, (byte)p_297843_.mainHand().getId());
    }

    public ClientInformation clientInformation()
    {
        int i = this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION);
        HumanoidArm humanoidarm = HumanoidArm.BY_ID.apply(this.getEntityData().get(DATA_PLAYER_MAIN_HAND));
        return new ClientInformation(this.language, this.requestedViewDistance, this.chatVisibility, this.canChatColor, i, humanoidarm, this.textFilteringEnabled, this.allowsListing);
    }

    public boolean canChatInColor()
    {
        return this.canChatColor;
    }

    public ChatVisiblity getChatVisibility()
    {
        return this.chatVisibility;
    }

    private boolean acceptsSystemMessages(boolean p_240568_)
    {
        return this.chatVisibility == ChatVisiblity.HIDDEN ? p_240568_ : true;
    }

    private boolean acceptsChatMessages()
    {
        return this.chatVisibility == ChatVisiblity.FULL;
    }

    public int requestedViewDistance()
    {
        return this.requestedViewDistance;
    }

    public void sendServerStatus(ServerStatus p_215110_)
    {
        this.connection.send(new ClientboundServerDataPacket(p_215110_.description(), p_215110_.favicon().map(ServerStatus.Favicon::iconBytes)));
    }

    @Override
    protected int getPermissionLevel()
    {
        return this.server.getProfilePermissions(this.getGameProfile());
    }

    public void resetLastActionTime()
    {
        this.lastActionTime = Util.getMillis();
    }

    public ServerStatsCounter getStats()
    {
        return this.stats;
    }

    public ServerRecipeBook getRecipeBook()
    {
        return this.recipeBook;
    }

    @Override
    protected void updateInvisibilityStatus()
    {
        if (this.isSpectator())
        {
            this.removeEffectParticles();
            this.setInvisible(true);
        }
        else
        {
            super.updateInvisibilityStatus();
        }
    }

    public Entity getCamera()
    {
        return (Entity)(this.camera == null ? this : this.camera);
    }

    public void setCamera(@Nullable Entity p_9214_)
    {
        Entity entity = this.getCamera();
        this.camera = (Entity)(p_9214_ == null ? this : p_9214_);

        if (entity != this.camera)
        {
            if (this.camera.level() instanceof ServerLevel serverlevel)
            {
                this.teleportTo(
                    serverlevel, this.camera.getX(), this.camera.getY(), this.camera.getZ(), Set.of(), this.getYRot(), this.getXRot()
                );
            }

            if (p_9214_ != null)
            {
                this.serverLevel().getChunkSource().move(this);
            }

            this.connection.send(new ClientboundSetCameraPacket(this.camera));
            this.connection.resetPosition();
        }
    }

    @Override
    protected void processPortalCooldown()
    {
        if (!this.isChangingDimension)
        {
            super.processPortalCooldown();
        }
    }

    @Override
    public void attack(Entity p_9220_)
    {
        if (this.gameMode.getGameModeForPlayer() == GameType.SPECTATOR)
        {
            this.setCamera(p_9220_);
        }
        else
        {
            super.attack(p_9220_);
        }
    }

    public long getLastActionTime()
    {
        return this.lastActionTime;
    }

    @Nullable
    public Component getTabListDisplayName()
    {
        return null;
    }

    @Override
    public void swing(InteractionHand p_9031_)
    {
        super.swing(p_9031_);
        this.resetAttackStrengthTicker();
    }

    public boolean isChangingDimension()
    {
        return this.isChangingDimension;
    }

    public void hasChangedDimension()
    {
        this.isChangingDimension = false;
    }

    public PlayerAdvancements getAdvancements()
    {
        return this.advancements;
    }

    public void teleportTo(ServerLevel p_9000_, double p_9001_, double p_9002_, double p_9003_, float p_9004_, float p_9005_)
    {
        this.setCamera(this);
        this.stopRiding();

        if (p_9000_ == this.level())
        {
            this.connection.teleport(p_9001_, p_9002_, p_9003_, p_9004_, p_9005_);
        }
        else
        {
            this.changeDimension(new DimensionTransition(p_9000_, new Vec3(p_9001_, p_9002_, p_9003_), Vec3.ZERO, p_9004_, p_9005_, DimensionTransition.DO_NOTHING));
        }
    }

    @Nullable
    public BlockPos getRespawnPosition()
    {
        return this.respawnPosition;
    }

    public float getRespawnAngle()
    {
        return this.respawnAngle;
    }

    public ResourceKey<Level> getRespawnDimension()
    {
        return this.respawnDimension;
    }

    public boolean isRespawnForced()
    {
        return this.respawnForced;
    }

    public void copyRespawnPosition(ServerPlayer p_344968_)
    {
        this.setRespawnPosition(p_344968_.getRespawnDimension(), p_344968_.getRespawnPosition(), p_344968_.getRespawnAngle(), p_344968_.isRespawnForced(), false);
    }

    public void setRespawnPosition(ResourceKey<Level> p_9159_, @Nullable BlockPos p_9160_, float p_9161_, boolean p_9162_, boolean p_9163_)
    {
        if (p_9160_ != null)
        {
            boolean flag = p_9160_.equals(this.respawnPosition) && p_9159_.equals(this.respawnDimension);

            if (p_9163_ && !flag)
            {
                this.sendSystemMessage(Component.translatable("block.minecraft.set_spawn"));
            }

            this.respawnPosition = p_9160_;
            this.respawnDimension = p_9159_;
            this.respawnAngle = p_9161_;
            this.respawnForced = p_9162_;
        }
        else
        {
            this.respawnPosition = null;
            this.respawnDimension = Level.OVERWORLD;
            this.respawnAngle = 0.0F;
            this.respawnForced = false;
        }
    }

    public SectionPos getLastSectionPos()
    {
        return this.lastSectionPos;
    }

    public void setLastSectionPos(SectionPos p_9120_)
    {
        this.lastSectionPos = p_9120_;
    }

    public ChunkTrackingView getChunkTrackingView()
    {
        return this.chunkTrackingView;
    }

    public void setChunkTrackingView(ChunkTrackingView p_300205_)
    {
        this.chunkTrackingView = p_300205_;
    }

    @Override
    public void playNotifySound(SoundEvent p_9019_, SoundSource p_9020_, float p_9021_, float p_9022_)
    {
        this.connection
        .send(
            new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(p_9019_),
                p_9020_,
                this.getX(),
                this.getY(),
                this.getZ(),
                p_9021_,
                p_9022_,
                this.random.nextLong()
            )
        );
    }

    @Override
    public ItemEntity drop(ItemStack p_9085_, boolean p_9086_, boolean p_9087_)
    {
        ItemEntity itementity = super.drop(p_9085_, p_9086_, p_9087_);

        if (itementity == null)
        {
            return null;
        }
        else
        {
            this.level().addFreshEntity(itementity);
            ItemStack itemstack = itementity.getItem();

            if (p_9087_)
            {
                if (!itemstack.isEmpty())
                {
                    this.awardStat(Stats.ITEM_DROPPED.get(itemstack.getItem()), p_9085_.getCount());
                }

                this.awardStat(Stats.DROP);
            }

            return itementity;
        }
    }

    public TextFilter getTextFilter()
    {
        return this.textFilter;
    }

    public void setServerLevel(ServerLevel p_284971_)
    {
        this.setLevel(p_284971_);
        this.gameMode.setLevel(p_284971_);
    }

    @Nullable
    private static GameType readPlayerMode(@Nullable CompoundTag p_143414_, String p_143415_)
    {
        return p_143414_ != null && p_143414_.contains(p_143415_, 99) ? GameType.byId(p_143414_.getInt(p_143415_)) : null;
    }

    private GameType calculateGameModeForNewPlayer(@Nullable GameType p_143424_)
    {
        GameType gametype = this.server.getForcedGameType();

        if (gametype != null)
        {
            return gametype;
        }
        else
        {
            return p_143424_ != null ? p_143424_ : this.server.getDefaultGameType();
        }
    }

    public void loadGameTypes(@Nullable CompoundTag p_143428_)
    {
        this.gameMode.setGameModeForPlayer(this.calculateGameModeForNewPlayer(readPlayerMode(p_143428_, "playerGameType")), readPlayerMode(p_143428_, "previousPlayerGameType"));
    }

    private void storeGameTypes(CompoundTag p_143431_)
    {
        p_143431_.putInt("playerGameType", this.gameMode.getGameModeForPlayer().getId());
        GameType gametype = this.gameMode.getPreviousGameModeForPlayer();

        if (gametype != null)
        {
            p_143431_.putInt("previousPlayerGameType", gametype.getId());
        }
    }

    @Override
    public boolean isTextFilteringEnabled()
    {
        return this.textFilteringEnabled;
    }

    public boolean shouldFilterMessageTo(ServerPlayer p_143422_)
    {
        return p_143422_ == this ? false : this.textFilteringEnabled || p_143422_.textFilteringEnabled;
    }

    @Override
    public boolean mayInteract(Level p_143406_, BlockPos p_143407_)
    {
        return super.mayInteract(p_143406_, p_143407_) && p_143406_.mayInteract(this, p_143407_);
    }

    @Override
    protected void updateUsingItem(ItemStack p_143402_)
    {
        CriteriaTriggers.USING_ITEM.trigger(this, p_143402_);
        super.updateUsingItem(p_143402_);
    }

    public boolean drop(boolean p_182295_)
    {
        Inventory inventory = this.getInventory();
        ItemStack itemstack = inventory.removeFromSelected(p_182295_);
        this.containerMenu.findSlot(inventory, inventory.selected).ifPresent(p_287377_ -> this.containerMenu.setRemoteSlot(p_287377_, inventory.getSelected()));
        return this.drop(itemstack, false, true) != null;
    }

    public boolean allowsListing()
    {
        return this.allowsListing;
    }

    @Override
    public Optional<WardenSpawnTracker> getWardenSpawnTracker()
    {
        return Optional.of(this.wardenSpawnTracker);
    }

    public void setSpawnExtraParticlesOnFall(boolean p_332664_)
    {
        this.spawnExtraParticlesOnFall = p_332664_;
    }

    @Override
    public void onItemPickup(ItemEntity p_215095_)
    {
        super.onItemPickup(p_215095_);
        Entity entity = p_215095_.getOwner();

        if (entity != null)
        {
            CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_PLAYER.trigger(this, p_215095_.getItem(), entity);
        }
    }

    public void setChatSession(RemoteChatSession p_254468_)
    {
        this.chatSession = p_254468_;
    }

    @Nullable
    public RemoteChatSession getChatSession()
    {
        return this.chatSession != null && this.chatSession.hasExpired() ? null : this.chatSession;
    }

    @Override
    public void indicateDamage(double p_270621_, double p_270478_)
    {
        this.hurtDir = (float)(Mth.atan2(p_270478_, p_270621_) * 180.0F / (float)Math.PI - (double)this.getYRot());
        this.connection.send(new ClientboundHurtAnimationPacket(this));
    }

    @Override
    public boolean startRiding(Entity p_277395_, boolean p_278062_)
    {
        if (super.startRiding(p_277395_, p_278062_))
        {
            this.setKnownMovement(Vec3.ZERO);
            p_277395_.positionRider(this);
            this.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());

            if (p_277395_ instanceof LivingEntity livingentity)
            {
                this.server.getPlayerList().sendActiveEffects(livingentity, this.connection);
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public void stopRiding()
    {
        Entity entity = this.getVehicle();
        super.stopRiding();

        if (entity instanceof LivingEntity livingentity)
        {
            for (MobEffectInstance mobeffectinstance : livingentity.getActiveEffects())
            {
                this.connection.send(new ClientboundRemoveMobEffectPacket(entity.getId(), mobeffectinstance.getEffect()));
            }
        }
    }

    public CommonPlayerSpawnInfo createCommonSpawnInfo(ServerLevel p_301182_)
    {
        return new CommonPlayerSpawnInfo(
                   p_301182_.dimensionTypeRegistration(),
                   p_301182_.dimension(),
                   BiomeManager.obfuscateSeed(p_301182_.getSeed()),
                   this.gameMode.getGameModeForPlayer(),
                   this.gameMode.getPreviousGameModeForPlayer(),
                   p_301182_.isDebug(),
                   p_301182_.isFlat(),
                   this.getLastDeathLocation(),
                   this.getPortalCooldown()
               );
    }

    public void setRaidOmenPosition(BlockPos p_335605_)
    {
        this.raidOmenPosition = p_335605_;
    }

    public void clearRaidOmenPosition()
    {
        this.raidOmenPosition = null;
    }

    @Nullable
    public BlockPos getRaidOmenPosition()
    {
        return this.raidOmenPosition;
    }

    @Override
    public Vec3 getKnownMovement()
    {
        Entity entity = this.getVehicle();
        return entity != null && entity.getControllingPassenger() != this ? entity.getKnownMovement() : this.lastKnownClientMovement;
    }

    public void setKnownMovement(Vec3 p_342348_)
    {
        this.lastKnownClientMovement = p_342348_;
    }

    @Override
    protected float getEnchantedDamage(Entity p_344113_, float p_344852_, DamageSource p_343579_)
    {
        return EnchantmentHelper.modifyDamage(this.serverLevel(), this.getWeaponItem(), p_344113_, p_343579_, p_344852_);
    }

    @Override
    public void onEquippedItemBroken(Item p_344553_, EquipmentSlot p_343482_)
    {
        super.onEquippedItemBroken(p_344553_, p_343482_);
        this.awardStat(Stats.ITEM_BROKEN.get(p_344553_));
    }

    static record RespawnPosAngle(Vec3 position, float yaw)
    {
        public static ServerPlayer.RespawnPosAngle of(Vec3 p_342971_, BlockPos p_343580_)
        {
            return new ServerPlayer.RespawnPosAngle(p_342971_, calculateLookAtYaw(p_342971_, p_343580_));
        }
        private static float calculateLookAtYaw(Vec3 p_344384_, BlockPos p_344719_)
        {
            Vec3 vec3 = Vec3.atBottomCenterOf(p_344719_).subtract(p_344384_).normalize();
            return (float)Mth.wrapDegrees(Mth.atan2(vec3.z, vec3.x) * 180.0F / (float)Math.PI - 90.0);
        }
    }
}
