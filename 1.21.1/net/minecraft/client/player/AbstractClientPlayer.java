package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.ShoulderRidingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.optifine.Config;
import net.optifine.RandomEntities;
import net.optifine.player.CapeUtils;
import net.optifine.player.PlayerConfigurations;
import net.optifine.reflect.Reflector;

public abstract class AbstractClientPlayer extends Player
{
    @Nullable
    private PlayerInfo playerInfo;
    protected Vec3 deltaMovementOnPreviousTick = Vec3.ZERO;
    public float elytraRotX;
    public float elytraRotY;
    public float elytraRotZ;
    public final ClientLevel clientLevel;
    private ResourceLocation locationOfCape = null;
    private long reloadCapeTimeMs = 0L;
    private boolean elytraOfCape = false;
    private String nameClear = null;
    public ShoulderRidingEntity entityShoulderLeft;
    public ShoulderRidingEntity entityShoulderRight;
    public ShoulderRidingEntity lastAttachedEntity;
    public float capeRotateX;
    public float capeRotateY;
    public float capeRotateZ;
    private static final ResourceLocation TEXTURE_ELYTRA = new ResourceLocation("textures/entity/elytra.png");

    public AbstractClientPlayer(ClientLevel p_250460_, GameProfile p_249912_)
    {
        super(p_250460_, p_250460_.getSharedSpawnPos(), p_250460_.getSharedSpawnAngle(), p_249912_);
        this.clientLevel = p_250460_;
        this.nameClear = p_249912_.getName();

        if (this.nameClear != null && !this.nameClear.isEmpty())
        {
            this.nameClear = StringUtil.stripColor(this.nameClear);
        }

        CapeUtils.downloadCape(this);
        PlayerConfigurations.getPlayerConfiguration(this);
    }

    @Override
    public boolean isSpectator()
    {
        PlayerInfo playerinfo = this.getPlayerInfo();
        return playerinfo != null && playerinfo.getGameMode() == GameType.SPECTATOR;
    }

    @Override
    public boolean isCreative()
    {
        PlayerInfo playerinfo = this.getPlayerInfo();
        return playerinfo != null && playerinfo.getGameMode() == GameType.CREATIVE;
    }

    @Nullable
    protected PlayerInfo getPlayerInfo()
    {
        if (this.playerInfo == null)
        {
            this.playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(this.getUUID());
        }

        return this.playerInfo;
    }

    @Override
    public void tick()
    {
        this.deltaMovementOnPreviousTick = this.getDeltaMovement();
        super.tick();

        if (this.lastAttachedEntity != null)
        {
            RandomEntities.checkEntityShoulder(this.lastAttachedEntity, true);
            this.lastAttachedEntity = null;
        }
    }

    public Vec3 getDeltaMovementLerped(float p_272943_)
    {
        return this.deltaMovementOnPreviousTick.lerp(this.getDeltaMovement(), (double)p_272943_);
    }

    public PlayerSkin getSkin()
    {
        PlayerInfo playerinfo = this.getPlayerInfo();
        return playerinfo == null ? DefaultPlayerSkin.get(this.getUUID()) : playerinfo.getSkin();
    }

    public float getFieldOfViewModifier()
    {
        float f = 1.0F;

        if (this.getAbilities().flying)
        {
            f *= 1.1F;
        }

        f *= ((float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) / this.getAbilities().getWalkingSpeed() + 1.0F) / 2.0F;

        if (this.getAbilities().getWalkingSpeed() == 0.0F || Float.isNaN(f) || Float.isInfinite(f))
        {
            f = 1.0F;
        }

        ItemStack itemstack = this.getUseItem();

        if (this.isUsingItem())
        {
            if (itemstack.getItem() instanceof BowItem)
            {
                int i = this.getTicksUsingItem();
                float f1 = (float)i / 20.0F;

                if (f1 > 1.0F)
                {
                    f1 = 1.0F;
                }
                else
                {
                    f1 *= f1;
                }

                f *= 1.0F - f1 * 0.15F;
            }
            else if (Minecraft.getInstance().options.getCameraType().isFirstPerson() && this.isScoping())
            {
                return 0.1F;
            }
        }

        return Reflector.ForgeHooksClient_getFieldOfViewModifier.exists()
               ? Reflector.callFloat(Reflector.ForgeHooksClient_getFieldOfViewModifier, this, f)
               : Mth.lerp(Minecraft.getInstance().options.fovEffectScale().get().floatValue(), 1.0F, f);
    }

    public String getNameClear()
    {
        return this.nameClear;
    }

    public ResourceLocation getLocationOfCape()
    {
        return this.locationOfCape;
    }

    public void setLocationOfCape(ResourceLocation locationOfCape)
    {
        this.locationOfCape = locationOfCape;
    }

    public boolean hasElytraCape()
    {
        ResourceLocation resourcelocation = this.getLocationCape();

        if (resourcelocation == null)
        {
            return false;
        }
        else
        {
            return resourcelocation == this.locationOfCape ? this.elytraOfCape : true;
        }
    }

    public void setElytraOfCape(boolean elytraOfCape)
    {
        this.elytraOfCape = elytraOfCape;
    }

    public boolean isElytraOfCape()
    {
        return this.elytraOfCape;
    }

    public long getReloadCapeTimeMs()
    {
        return this.reloadCapeTimeMs;
    }

    public void setReloadCapeTimeMs(long reloadCapeTimeMs)
    {
        this.reloadCapeTimeMs = reloadCapeTimeMs;
    }

    @Nullable
    public ResourceLocation getLocationCape()
    {
        if (!Config.isShowCapes())
        {
            return null;
        }
        else
        {
            if (this.reloadCapeTimeMs != 0L && System.currentTimeMillis() > this.reloadCapeTimeMs)
            {
                CapeUtils.reloadCape(this);
                this.reloadCapeTimeMs = 0L;
                PlayerConfigurations.setPlayerConfiguration(this.getNameClear(), null);
            }

            return this.locationOfCape != null ? this.locationOfCape : this.getSkin().capeTexture();
        }
    }

    public ResourceLocation getLocationElytra()
    {
        return this.hasElytraCape() ? this.locationOfCape : this.getSkin().elytraTexture();
    }

    public ResourceLocation getSkinTextureLocation()
    {
        PlayerInfo playerinfo = this.getPlayerInfo();
        return playerinfo == null ? DefaultPlayerSkin.get(this.getUUID()).texture() : playerinfo.getSkin().texture();
    }
}
