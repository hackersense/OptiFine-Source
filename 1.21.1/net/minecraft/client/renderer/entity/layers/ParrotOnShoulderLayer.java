package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Optional;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.ShoulderRidingEntity;
import net.minecraft.world.entity.player.Player;
import net.optifine.Config;
import net.optifine.shaders.Shaders;

public class ParrotOnShoulderLayer<T extends Player> extends RenderLayer<T, PlayerModel<T>>
{
    private final ParrotModel model;
    public static ParrotModel customParrotModel;

    public ParrotOnShoulderLayer(RenderLayerParent<T, PlayerModel<T>> p_174511_, EntityModelSet p_174512_)
    {
        super(p_174511_);
        this.model = new ParrotModel(p_174512_.bakeLayer(ModelLayers.PARROT));
    }

    public void render(
        PoseStack p_117307_,
        MultiBufferSource p_117308_,
        int p_117309_,
        T p_117310_,
        float p_117311_,
        float p_117312_,
        float p_117313_,
        float p_117314_,
        float p_117315_,
        float p_117316_
    )
    {
        this.render(p_117307_, p_117308_, p_117309_, p_117310_, p_117311_, p_117312_, p_117315_, p_117316_, true);
        this.render(p_117307_, p_117308_, p_117309_, p_117310_, p_117311_, p_117312_, p_117315_, p_117316_, false);
    }

    private void render(
        PoseStack p_117318_,
        MultiBufferSource p_117319_,
        int p_117320_,
        T p_117321_,
        float p_117322_,
        float p_117323_,
        float p_117324_,
        float p_117325_,
        boolean p_117326_
    )
    {
        CompoundTag compoundtag = p_117326_ ? p_117321_.getShoulderEntityLeft() : p_117321_.getShoulderEntityRight();
        EntityType.byString(compoundtag.getString("id"))
        .filter(entityTypeIn -> entityTypeIn == EntityType.PARROT)
        .ifPresent(
            entityTypeIn ->
        {
            Entity entity = Config.getEntityRenderDispatcher().getRenderedEntity();

            if (p_117321_ instanceof AbstractClientPlayer abstractclientplayer)
            {
                Entity entity1 = p_117326_ ? abstractclientplayer.entityShoulderLeft : abstractclientplayer.entityShoulderRight;

                if (entity1 == null)
                {
                    entity1 = this.makeEntity(compoundtag, p_117321_);

                    if (entity1 instanceof ShoulderRidingEntity)
                    {
                        if (p_117326_)
                        {
                            abstractclientplayer.entityShoulderLeft = (ShoulderRidingEntity)entity1;
                        }
                        else
                        {
                            abstractclientplayer.entityShoulderRight = (ShoulderRidingEntity)entity1;
                        }
                    }
                }

                if (entity1 != null)
                {
                    entity1.xo = entity.xo;
                    entity1.yo = entity.yo;
                    entity1.zo = entity.zo;
                    entity1.setPosRaw(entity.getX(), entity.getY(), entity.getZ());
                    entity1.xRotO = entity.xRotO;
                    entity1.yRotO = entity.yRotO;
                    entity1.setXRot(entity.getXRot());
                    entity1.setYRot(entity.getYRot());

                    if (entity1 instanceof LivingEntity && entity instanceof LivingEntity)
                    {
                        ((LivingEntity)entity1).yBodyRotO = ((LivingEntity)entity).yBodyRotO;
                        ((LivingEntity)entity1).yBodyRot = ((LivingEntity)entity).yBodyRot;
                    }

                    Config.getEntityRenderDispatcher().setRenderedEntity(entity1);

                    if (Config.isShaders())
                    {
                        Shaders.nextEntity(entity1);
                    }
                }
            }

            p_117318_.pushPose();
            p_117318_.translate(p_117326_ ? 0.4F : -0.4F, p_117321_.isCrouching() ? -1.3F : -1.5F, 0.0F);
            Parrot.Variant parrot$variant = Parrot.Variant.byId(compoundtag.getInt("Variant"));
            VertexConsumer vertexconsumer = p_117319_.getBuffer(this.model.renderType(ParrotRenderer.getVariantTexture(parrot$variant)));
            this.getParrotModel()
            .renderOnShoulder(
                p_117318_, vertexconsumer, p_117320_, OverlayTexture.NO_OVERLAY, p_117322_, p_117323_, p_117324_, p_117325_, p_117321_.tickCount
            );
            p_117318_.popPose();
            Config.getEntityRenderDispatcher().setRenderedEntity(entity);

            if (Config.isShaders())
            {
                Shaders.nextEntity(entity);
            }
        }
        );
    }

    private Entity makeEntity(CompoundTag compoundtag, Player player)
    {
        Optional < EntityType<? >> optional = EntityType.by(compoundtag);

        if (!optional.isPresent())
        {
            return null;
        }
        else
        {
            Entity entity = optional.get().create(player.level());

            if (entity == null)
            {
                return null;
            }
            else
            {
                entity.load(compoundtag);
                SynchedEntityData synchedentitydata = entity.getEntityData();

                if (synchedentitydata != null)
                {
                    synchedentitydata.spawnPosition = player.blockPosition();
                    synchedentitydata.spawnBiome = player.level().getBiome(synchedentitydata.spawnPosition).value();
                }

                return entity;
            }
        }
    }

    private ParrotModel getParrotModel()
    {
        return customParrotModel != null ? customParrotModel : this.model;
    }
}
