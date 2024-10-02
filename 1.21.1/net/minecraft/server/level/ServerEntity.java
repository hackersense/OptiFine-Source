package net.minecraft.server.level;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundProjectilePowerPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ServerEntity
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TOLERANCE_LEVEL_ROTATION = 1;
    private static final double TOLERANCE_LEVEL_POSITION = 7.6293945E-6F;
    public static final int FORCED_POS_UPDATE_PERIOD = 60;
    private static final int FORCED_TELEPORT_PERIOD = 400;
    private final ServerLevel level;
    private final Entity entity;
    private final int updateInterval;
    private final boolean trackDelta;
    private final Consumer < Packet<? >> broadcast;
    private final VecDeltaCodec positionCodec = new VecDeltaCodec();
    private int lastSentYRot;
    private int lastSentXRot;
    private int lastSentYHeadRot;
    private Vec3 lastSentMovement;
    private int tickCount;
    private int teleportDelay;
    private List<Entity> lastPassengers = Collections.emptyList();
    private boolean wasRiding;
    private boolean wasOnGround;
    @Nullable
    private List < SynchedEntityData.DataValue<? >> trackedDataValues;

    public ServerEntity(ServerLevel p_8528_, Entity p_8529_, int p_8530_, boolean p_8531_, Consumer < Packet<? >> p_8532_)
    {
        this.level = p_8528_;
        this.broadcast = p_8532_;
        this.entity = p_8529_;
        this.updateInterval = p_8530_;
        this.trackDelta = p_8531_;
        this.positionCodec.setBase(p_8529_.trackingPosition());
        this.lastSentMovement = p_8529_.getDeltaMovement();
        this.lastSentYRot = Mth.floor(p_8529_.getYRot() * 256.0F / 360.0F);
        this.lastSentXRot = Mth.floor(p_8529_.getXRot() * 256.0F / 360.0F);
        this.lastSentYHeadRot = Mth.floor(p_8529_.getYHeadRot() * 256.0F / 360.0F);
        this.wasOnGround = p_8529_.onGround();
        this.trackedDataValues = p_8529_.getEntityData().getNonDefaultValues();
    }

    public void sendChanges()
    {
        List<Entity> list = this.entity.getPassengers();

        if (!list.equals(this.lastPassengers))
        {
            this.broadcast.accept(new ClientboundSetPassengersPacket(this.entity));
            removedPassengers(list, this.lastPassengers)
            .forEach(
                p_341234_ ->
            {
                if (p_341234_ instanceof ServerPlayer serverplayer1)
                {
                    serverplayer1.connection
                    .teleport(
                        serverplayer1.getX(),
                        serverplayer1.getY(),
                        serverplayer1.getZ(),
                        serverplayer1.getYRot(),
                        serverplayer1.getXRot()
                    );
                }
            }
            );
            this.lastPassengers = list;
        }

        if (this.entity instanceof ItemFrame itemframe && this.tickCount % 10 == 0)
        {
            ItemStack itemstack = itemframe.getItem();

            if (itemstack.getItem() instanceof MapItem)
            {
                MapId mapid = itemstack.get(DataComponents.MAP_ID);
                MapItemSavedData mapitemsaveddata = MapItem.getSavedData(mapid, this.level);

                if (mapitemsaveddata != null)
                {
                    for (ServerPlayer serverplayer : this.level.players())
                    {
                        mapitemsaveddata.tickCarriedBy(serverplayer, itemstack);
                        Packet<?> packet = mapitemsaveddata.getUpdatePacket(mapid, serverplayer);

                        if (packet != null)
                        {
                            serverplayer.connection.send(packet);
                        }
                    }
                }
            }

            this.sendDirtyEntityData();
        }

        if (this.tickCount % this.updateInterval == 0 || this.entity.hasImpulse || this.entity.getEntityData().isDirty())
        {
            if (this.entity.isPassenger())
            {
                int i1 = Mth.floor(this.entity.getYRot() * 256.0F / 360.0F);
                int l1 = Mth.floor(this.entity.getXRot() * 256.0F / 360.0F);
                boolean flag3 = Math.abs(i1 - this.lastSentYRot) >= 1 || Math.abs(l1 - this.lastSentXRot) >= 1;

                if (flag3)
                {
                    this.broadcast.accept(new ClientboundMoveEntityPacket.Rot(this.entity.getId(), (byte)i1, (byte)l1, this.entity.onGround()));
                    this.lastSentYRot = i1;
                    this.lastSentXRot = l1;
                }

                this.positionCodec.setBase(this.entity.trackingPosition());
                this.sendDirtyEntityData();
                this.wasRiding = true;
            }
            else
            {
                this.teleportDelay++;
                int l = Mth.floor(this.entity.getYRot() * 256.0F / 360.0F);
                int k1 = Mth.floor(this.entity.getXRot() * 256.0F / 360.0F);
                Vec3 vec31 = this.entity.trackingPosition();
                boolean flag4 = this.positionCodec.delta(vec31).lengthSqr() >= 7.6293945E-6F;
                Packet<?> packet1 = null;
                boolean flag5 = flag4 || this.tickCount % 60 == 0;
                boolean flag6 = Math.abs(l - this.lastSentYRot) >= 1 || Math.abs(k1 - this.lastSentXRot) >= 1;
                boolean flag = false;
                boolean flag1 = false;
                long i = this.positionCodec.encodeX(vec31);
                long j = this.positionCodec.encodeY(vec31);
                long k = this.positionCodec.encodeZ(vec31);
                boolean flag2 = i < -32768L || i > 32767L || j < -32768L || j > 32767L || k < -32768L || k > 32767L;

                if (flag2 || this.teleportDelay > 400 || this.wasRiding || this.wasOnGround != this.entity.onGround())
                {
                    this.wasOnGround = this.entity.onGround();
                    this.teleportDelay = 0;
                    packet1 = new ClientboundTeleportEntityPacket(this.entity);
                    flag = true;
                    flag1 = true;
                }
                else if ((!flag5 || !flag6) && !(this.entity instanceof AbstractArrow))
                {
                    if (flag5)
                    {
                        packet1 = new ClientboundMoveEntityPacket.Pos(
                            this.entity.getId(), (short)((int)i), (short)((int)j), (short)((int)k), this.entity.onGround()
                        );
                        flag = true;
                    }
                    else if (flag6)
                    {
                        packet1 = new ClientboundMoveEntityPacket.Rot(this.entity.getId(), (byte)l, (byte)k1, this.entity.onGround());
                        flag1 = true;
                    }
                }
                else
                {
                    packet1 = new ClientboundMoveEntityPacket.PosRot(
                        this.entity.getId(), (short)((int)i), (short)((int)j), (short)((int)k), (byte)l, (byte)k1, this.entity.onGround()
                    );
                    flag = true;
                    flag1 = true;
                }

                if ((this.trackDelta || this.entity.hasImpulse || this.entity instanceof LivingEntity && ((LivingEntity)this.entity).isFallFlying())
                        && this.tickCount > 0)
                {
                    Vec3 vec3 = this.entity.getDeltaMovement();
                    double d0 = vec3.distanceToSqr(this.lastSentMovement);

                    if (d0 > 1.0E-7 || d0 > 0.0 && vec3.lengthSqr() == 0.0)
                    {
                        this.lastSentMovement = vec3;

                        if (this.entity instanceof AbstractHurtingProjectile abstracthurtingprojectile)
                        {
                            this.broadcast
                            .accept(
                                new ClientboundBundlePacket(
                                    List.of(
                                        new ClientboundSetEntityMotionPacket(this.entity.getId(), this.lastSentMovement),
                                        new ClientboundProjectilePowerPacket(abstracthurtingprojectile.getId(), abstracthurtingprojectile.accelerationPower)
                                    )
                                )
                            );
                        }
                        else
                        {
                            this.broadcast.accept(new ClientboundSetEntityMotionPacket(this.entity.getId(), this.lastSentMovement));
                        }
                    }
                }

                if (packet1 != null)
                {
                    this.broadcast.accept(packet1);
                }

                this.sendDirtyEntityData();

                if (flag)
                {
                    this.positionCodec.setBase(vec31);
                }

                if (flag1)
                {
                    this.lastSentYRot = l;
                    this.lastSentXRot = k1;
                }

                this.wasRiding = false;
            }

            int j1 = Mth.floor(this.entity.getYHeadRot() * 256.0F / 360.0F);

            if (Math.abs(j1 - this.lastSentYHeadRot) >= 1)
            {
                this.broadcast.accept(new ClientboundRotateHeadPacket(this.entity, (byte)j1));
                this.lastSentYHeadRot = j1;
            }

            this.entity.hasImpulse = false;
        }

        this.tickCount++;

        if (this.entity.hurtMarked)
        {
            this.entity.hurtMarked = false;
            this.broadcastAndSend(new ClientboundSetEntityMotionPacket(this.entity));
        }
    }

    private static Stream<Entity> removedPassengers(List<Entity> p_277592_, List<Entity> p_277658_)
    {
        return p_277658_.stream().filter(p_275361_ -> !p_277592_.contains(p_275361_));
    }

    public void removePairing(ServerPlayer p_8535_)
    {
        this.entity.stopSeenByPlayer(p_8535_);
        p_8535_.connection.send(new ClientboundRemoveEntitiesPacket(this.entity.getId()));
    }

    public void addPairing(ServerPlayer p_8542_)
    {
        List < Packet <? super ClientGamePacketListener >> list = new ArrayList<>();
        this.sendPairingData(p_8542_, list::add);
        p_8542_.connection.send(new ClientboundBundlePacket(list));
        this.entity.startSeenByPlayer(p_8542_);
    }

    public void sendPairingData(ServerPlayer p_289562_, Consumer<Packet<ClientGamePacketListener>> p_289563_)
    {
        if (this.entity.isRemoved())
        {
            LOGGER.warn("Fetching packet for removed entity {}", this.entity);
        }

        Packet<ClientGamePacketListener> packet = this.entity.getAddEntityPacket(this);
        p_289563_.accept(packet);

        if (this.trackedDataValues != null)
        {
            p_289563_.accept(new ClientboundSetEntityDataPacket(this.entity.getId(), this.trackedDataValues));
        }

        boolean flag = this.trackDelta;

        if (this.entity instanceof LivingEntity)
        {
            Collection<AttributeInstance> collection = ((LivingEntity)this.entity).getAttributes().getSyncableAttributes();

            if (!collection.isEmpty())
            {
                p_289563_.accept(new ClientboundUpdateAttributesPacket(this.entity.getId(), collection));
            }

            if (((LivingEntity)this.entity).isFallFlying())
            {
                flag = true;
            }
        }

        if (flag && !(this.entity instanceof LivingEntity))
        {
            p_289563_.accept(new ClientboundSetEntityMotionPacket(this.entity.getId(), this.lastSentMovement));
        }

        if (this.entity instanceof LivingEntity)
        {
            List<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayList();

            for (EquipmentSlot equipmentslot : EquipmentSlot.values())
            {
                ItemStack itemstack = ((LivingEntity)this.entity).getItemBySlot(equipmentslot);

                if (!itemstack.isEmpty())
                {
                    list.add(Pair.of(equipmentslot, itemstack.copy()));
                }
            }

            if (!list.isEmpty())
            {
                p_289563_.accept(new ClientboundSetEquipmentPacket(this.entity.getId(), list));
            }
        }

        if (!this.entity.getPassengers().isEmpty())
        {
            p_289563_.accept(new ClientboundSetPassengersPacket(this.entity));
        }

        if (this.entity.isPassenger())
        {
            p_289563_.accept(new ClientboundSetPassengersPacket(this.entity.getVehicle()));
        }

        if (this.entity instanceof Leashable leashable && leashable.isLeashed())
        {
            p_289563_.accept(new ClientboundSetEntityLinkPacket(this.entity, leashable.getLeashHolder()));
        }
    }

    public Vec3 getPositionBase()
    {
        return this.positionCodec.getBase();
    }

    public Vec3 getLastSentMovement()
    {
        return this.lastSentMovement;
    }

    public float getLastSentXRot()
    {
        return (float)(this.lastSentXRot * 360) / 256.0F;
    }

    public float getLastSentYRot()
    {
        return (float)(this.lastSentYRot * 360) / 256.0F;
    }

    public float getLastSentYHeadRot()
    {
        return (float)(this.lastSentYHeadRot * 360) / 256.0F;
    }

    private void sendDirtyEntityData()
    {
        SynchedEntityData synchedentitydata = this.entity.getEntityData();
        List < SynchedEntityData.DataValue<? >> list = synchedentitydata.packDirty();

        if (list != null)
        {
            this.trackedDataValues = synchedentitydata.getNonDefaultValues();
            this.broadcastAndSend(new ClientboundSetEntityDataPacket(this.entity.getId(), list));
        }

        if (this.entity instanceof LivingEntity)
        {
            Set<AttributeInstance> set = ((LivingEntity)this.entity).getAttributes().getAttributesToSync();

            if (!set.isEmpty())
            {
                this.broadcastAndSend(new ClientboundUpdateAttributesPacket(this.entity.getId(), set));
            }

            set.clear();
        }
    }

    private void broadcastAndSend(Packet<?> p_8539_)
    {
        this.broadcast.accept(p_8539_);

        if (this.entity instanceof ServerPlayer)
        {
            ((ServerPlayer)this.entity).connection.send(p_8539_);
        }
    }
}
