package net.minecraft.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.Shaders;
import net.optifine.util.TextureUtils;
import org.joml.Vector3f;

public class LightTexture implements AutoCloseable
{
    public static final int FULL_BRIGHT = 15728880;
    public static final int FULL_SKY = 15728640;
    public static final int FULL_BLOCK = 240;
    private final DynamicTexture lightTexture;
    private final NativeImage lightPixels;
    private final ResourceLocation lightTextureLocation;
    private boolean updateLightTexture;
    private float blockLightRedFlicker;
    private final GameRenderer renderer;
    private final Minecraft minecraft;
    private boolean allowed = true;
    private boolean custom = false;
    private Vector3f tempVector = new Vector3f();
    public static final int MAX_BRIGHTNESS = pack(15, 15);
    public static final int VANILLA_EMISSIVE_BRIGHTNESS = 15794417;

    public LightTexture(GameRenderer p_109878_, Minecraft p_109879_)
    {
        this.renderer = p_109878_;
        this.minecraft = p_109879_;
        this.lightTexture = new DynamicTexture(16, 16, false);
        this.lightTextureLocation = this.minecraft.getTextureManager().register("light_map", this.lightTexture);
        this.lightPixels = this.lightTexture.getPixels();

        for (int i = 0; i < 16; i++)
        {
            for (int j = 0; j < 16; j++)
            {
                this.lightPixels.setPixelRGBA(j, i, -1);
            }
        }

        this.lightTexture.upload();
    }

    @Override
    public void close()
    {
        this.lightTexture.close();
    }

    public void tick()
    {
        this.blockLightRedFlicker = this.blockLightRedFlicker + (float)((Math.random() - Math.random()) * Math.random() * Math.random() * 0.1);
        this.blockLightRedFlicker *= 0.9F;
        this.updateLightTexture = true;
    }

    public void turnOffLightLayer()
    {
        RenderSystem.setShaderTexture(2, 0);

        if (Config.isShaders())
        {
            Shaders.disableLightmap();
        }
    }

    public void turnOnLightLayer()
    {
        if (!this.allowed)
        {
            RenderSystem.setShaderTexture(2, TextureUtils.WHITE_TEXTURE_LOCATION);
            this.minecraft.getTextureManager().bindForSetup(TextureUtils.WHITE_TEXTURE_LOCATION);
        }
        else
        {
            RenderSystem.setShaderTexture(2, this.lightTextureLocation);
            this.minecraft.getTextureManager().bindForSetup(this.lightTextureLocation);
        }

        RenderSystem.texParameter(3553, 10241, 9729);
        RenderSystem.texParameter(3553, 10240, 9729);

        if (Config.isShaders())
        {
            Shaders.enableLightmap();
        }
    }

    private float getDarknessGamma(float p_234320_)
    {
        MobEffectInstance mobeffectinstance = this.minecraft.player.getEffect(MobEffects.DARKNESS);
        return mobeffectinstance != null ? mobeffectinstance.getBlendFactor(this.minecraft.player, p_234320_) : 0.0F;
    }

    private float calculateDarknessScale(LivingEntity p_234313_, float p_234314_, float p_234315_)
    {
        float f = 0.45F * p_234314_;
        return Math.max(0.0F, Mth.cos(((float)p_234313_.tickCount - p_234315_) * (float) Math.PI * 0.025F) * f);
    }

    public void updateLightTexture(float p_109882_)
    {
        if (this.updateLightTexture)
        {
            this.updateLightTexture = false;
            this.minecraft.getProfiler().push("lightTex");
            ClientLevel clientlevel = this.minecraft.level;

            if (clientlevel != null)
            {
                this.custom = false;

                if (Config.isCustomColors())
                {
                    boolean flag = this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION) || this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER);
                    float f = this.getDarknessGammaFactor(p_109882_);
                    float f1 = this.getDarknessLightFactor(clientlevel, p_109882_);
                    float f2 = f * 0.25F + f1 * 0.75F;

                    if (CustomColors.updateLightmap(clientlevel, this.blockLightRedFlicker, this.lightPixels, flag, f2, p_109882_))
                    {
                        this.lightTexture.upload();
                        this.updateLightTexture = false;
                        this.minecraft.getProfiler().pop();
                        this.custom = true;
                        return;
                    }
                }

                float f12 = clientlevel.getSkyDarken(1.0F);
                float f13;

                if (clientlevel.getSkyFlashTime() > 0)
                {
                    f13 = 1.0F;
                }
                else
                {
                    f13 = f12 * 0.95F + 0.05F;
                }

                float f14 = this.minecraft.options.darknessEffectScale().get().floatValue();
                float f15 = this.getDarknessGamma(p_109882_) * f14;
                float f3 = this.calculateDarknessScale(this.minecraft.player, f15, p_109882_) * f14;

                if (Config.isShaders())
                {
                    Shaders.setDarknessFactor(f15);
                    Shaders.setDarknessLightFactor(f3);
                }

                float f4 = this.minecraft.player.getWaterVision();
                float f5;

                if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION))
                {
                    f5 = GameRenderer.getNightVisionScale(this.minecraft.player, p_109882_);
                }
                else if (f4 > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER))
                {
                    f5 = f4;
                }
                else
                {
                    f5 = 0.0F;
                }

                Vector3f vector3f = new Vector3f(f12, f12, 1.0F).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
                float f6 = this.blockLightRedFlicker + 1.5F;
                Vector3f vector3f1 = new Vector3f();

                for (int i = 0; i < 16; i++)
                {
                    for (int j = 0; j < 16; j++)
                    {
                        float f7 = getBrightness(clientlevel.dimensionType(), i) * f13;
                        float f8 = getBrightness(clientlevel.dimensionType(), j) * f6;
                        float f9 = f8 * ((f8 * 0.6F + 0.4F) * 0.6F + 0.4F);
                        float f10 = f8 * (f8 * f8 * 0.6F + 0.4F);
                        vector3f1.set(f8, f9, f10);
                        boolean flag1 = clientlevel.effects().forceBrightLightmap();

                        if (flag1)
                        {
                            vector3f1.lerp(this.getTempVector3f(0.99F, 1.12F, 1.0F), 0.25F);
                            clampColor(vector3f1);
                        }
                        else
                        {
                            Vector3f vector3f2 = this.getTempCopy(vector3f).mul(f7);
                            vector3f1.add(vector3f2);
                            vector3f1.lerp(this.getTempVector3f(0.75F, 0.75F, 0.75F), 0.04F);

                            if (this.renderer.getDarkenWorldAmount(p_109882_) > 0.0F)
                            {
                                float f11 = this.renderer.getDarkenWorldAmount(p_109882_);
                                Vector3f vector3f3 = this.getTempCopy(vector3f1).mul(0.7F, 0.6F, 0.6F);
                                vector3f1.lerp(vector3f3, f11);
                            }
                        }

                        if (Reflector.IForgeDimensionSpecialEffects_adjustLightmapColors.exists())
                        {
                            Reflector.call(
                                clientlevel.effects(),
                                Reflector.IForgeDimensionSpecialEffects_adjustLightmapColors,
                                clientlevel,
                                p_109882_,
                                f12,
                                f6,
                                f7,
                                j,
                                i,
                                vector3f1
                            );
                        }

                        if (f5 > 0.0F)
                        {
                            float f16 = Math.max(vector3f1.x(), Math.max(vector3f1.y(), vector3f1.z()));

                            if (f16 < 1.0F)
                            {
                                float f18 = 1.0F / f16;
                                Vector3f vector3f5 = this.getTempCopy(vector3f1).mul(f18);
                                vector3f1.lerp(vector3f5, f5);
                            }
                        }

                        if (!flag1)
                        {
                            if (f3 > 0.0F)
                            {
                                vector3f1.add(-f3, -f3, -f3);
                            }

                            clampColor(vector3f1);
                        }

                        float f17 = this.minecraft.options.gamma().get().floatValue();
                        Vector3f vector3f4 = this.getTempVector3f(this.notGamma(vector3f1.x), this.notGamma(vector3f1.y), this.notGamma(vector3f1.z));
                        vector3f1.lerp(vector3f4, Math.max(0.0F, f17 - f15));
                        vector3f1.lerp(this.getTempVector3f(0.75F, 0.75F, 0.75F), 0.04F);
                        clampColor(vector3f1);
                        vector3f1.mul(255.0F);
                        int j1 = 255;
                        int k = (int)vector3f1.x();
                        int l = (int)vector3f1.y();
                        int i1 = (int)vector3f1.z();
                        this.lightPixels.setPixelRGBA(j, i, 0xFF000000 | i1 << 16 | l << 8 | k);
                    }
                }

                this.lightTexture.upload();
                this.minecraft.getProfiler().pop();
            }
        }
    }

    private static void clampColor(Vector3f p_254122_)
    {
        p_254122_.set(Mth.clamp(p_254122_.x, 0.0F, 1.0F), Mth.clamp(p_254122_.y, 0.0F, 1.0F), Mth.clamp(p_254122_.z, 0.0F, 1.0F));
    }

    private float notGamma(float p_109893_)
    {
        float f = 1.0F - p_109893_;
        return 1.0F - f * f * f * f;
    }

    public static float getBrightness(DimensionType p_234317_, int p_234318_)
    {
        float f = (float)p_234318_ / 15.0F;
        float f1 = f / (4.0F - 3.0F * f);
        return Mth.lerp(p_234317_.ambientLight(), f1, 1.0F);
    }

    public static int pack(int p_109886_, int p_109887_)
    {
        return p_109886_ << 4 | p_109887_ << 20;
    }

    public static int block(int p_109884_)
    {
        return (p_109884_ & 65535) >> 4;
    }

    public static int sky(int p_109895_)
    {
        return p_109895_ >> 20 & 65535;
    }

    private Vector3f getTempVector3f(float x, float y, float z)
    {
        this.tempVector.set(x, y, z);
        return this.tempVector;
    }

    private Vector3f getTempCopy(Vector3f vec)
    {
        this.tempVector.set(vec.x(), vec.y(), vec.z());
        return this.tempVector;
    }

    public boolean isAllowed()
    {
        return this.allowed;
    }

    public void setAllowed(boolean allowed)
    {
        this.allowed = allowed;
    }

    public boolean isCustom()
    {
        return this.custom;
    }

    public float getDarknessGammaFactor(float partialTicks)
    {
        float f = this.minecraft.options.darknessEffectScale().get().floatValue();
        return this.getDarknessGamma(partialTicks) * f;
    }

    public float getDarknessLightFactor(ClientLevel clientLevel, float partialTicks)
    {
        boolean flag = clientLevel.effects().forceBrightLightmap();

        if (flag)
        {
            return 0.0F;
        }
        else
        {
            float f = this.minecraft.options.darknessEffectScale().get().floatValue();
            float f1 = this.getDarknessGamma(partialTicks) * f;
            return this.calculateDarknessScale(this.minecraft.player, f1, partialTicks) * f;
        }
    }
}
