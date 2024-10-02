package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.optifine.Config;
import net.optifine.SmartAnimations;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.Shaders;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;

public class ScreenEffectRenderer
{
    private static final ResourceLocation UNDERWATER_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");

    public static void renderScreenEffect(Minecraft p_110719_, PoseStack p_110720_)
    {
        Player player = p_110719_.player;

        if (!player.noPhysics)
        {
            if (Reflector.ForgeHooksClient_renderBlockOverlay.exists() && Reflector.ForgeBlockModelShapes_getTexture3.exists())
            {
                Pair<BlockState, BlockPos> pair = getOverlayBlock(player);

                if (pair != null)
                {
                    Object object = Reflector.getFieldValue(Reflector.RenderBlockScreenEffectEvent_OverlayType_BLOCK);

                    if (!Reflector.ForgeHooksClient_renderBlockOverlay.callBoolean(player, p_110720_, object, pair.getLeft(), pair.getRight()))
                    {
                        TextureAtlasSprite textureatlassprite = (TextureAtlasSprite)Reflector.call(
                                p_110719_.getBlockRenderer().getBlockModelShaper(), Reflector.ForgeBlockModelShapes_getTexture3, pair.getLeft(), p_110719_.level, pair.getRight()
                                                                );
                        renderTex(textureatlassprite, p_110720_);
                    }
                }
            }
            else
            {
                BlockState blockstate = getViewBlockingState(player);

                if (blockstate != null)
                {
                    renderTex(p_110719_.getBlockRenderer().getBlockModelShaper().getParticleIcon(blockstate), p_110720_);
                }
            }
        }

        if (!p_110719_.player.isSpectator())
        {
            if (p_110719_.player.isEyeInFluid(FluidTags.WATER))
            {
                if (!Reflector.ForgeHooksClient_renderWaterOverlay.callBoolean(player, p_110720_))
                {
                    renderWater(p_110719_, p_110720_);
                }
            }
            else if (Reflector.IForgeEntity_getEyeInFluidType.exists())
            {
                FluidType fluidtype = (FluidType)Reflector.call(player, Reflector.IForgeEntity_getEyeInFluidType);

                if (!fluidtype.isAir())
                {
                    IClientFluidTypeExtensions.of(fluidtype).renderOverlay(p_110719_, p_110720_);
                }
            }

            if (p_110719_.player.isOnFire() && !Reflector.ForgeHooksClient_renderFireOverlay.callBoolean(player, p_110720_))
            {
                renderFire(p_110719_, p_110720_);
            }
        }
    }

    @Nullable
    private static BlockState getViewBlockingState(Player p_110717_)
    {
        Pair<BlockState, BlockPos> pair = getOverlayBlock(p_110717_);
        return pair == null ? null : pair.getLeft();
    }

    private static Pair<BlockState, BlockPos> getOverlayBlock(Player playerIn)
    {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < 8; i++)
        {
            double d0 = playerIn.getX() + (double)(((float)((i >> 0) % 2) - 0.5F) * playerIn.getBbWidth() * 0.8F);
            double d1 = playerIn.getEyeY() + (double)(((float)((i >> 1) % 2) - 0.5F) * 0.1F * playerIn.getScale());
            double d2 = playerIn.getZ() + (double)(((float)((i >> 2) % 2) - 0.5F) * playerIn.getBbWidth() * 0.8F);
            blockpos$mutableblockpos.set(d0, d1, d2);
            BlockState blockstate = playerIn.level().getBlockState(blockpos$mutableblockpos);

            if (blockstate.getRenderShape() != RenderShape.INVISIBLE && blockstate.isViewBlocking(playerIn.level(), blockpos$mutableblockpos))
            {
                return Pair.of(blockstate, blockpos$mutableblockpos.immutable());
            }
        }

        return null;
    }

    private static void renderTex(TextureAtlasSprite p_173297_, PoseStack p_173298_)
    {
        if (SmartAnimations.isActive())
        {
            SmartAnimations.spriteRendered(p_173297_);
        }

        RenderSystem.setShaderTexture(0, p_173297_.atlasLocation());
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        float f = 0.1F;
        float f1 = -1.0F;
        float f2 = 1.0F;
        float f3 = -1.0F;
        float f4 = 1.0F;
        float f5 = -0.5F;
        float f6 = p_173297_.getU0();
        float f7 = p_173297_.getU1();
        float f8 = p_173297_.getV0();
        float f9 = p_173297_.getV1();
        Matrix4f matrix4f = p_173298_.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferbuilder.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(f7, f9).setColor(0.1F, 0.1F, 0.1F, 1.0F);
        bufferbuilder.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(f6, f9).setColor(0.1F, 0.1F, 0.1F, 1.0F);
        bufferbuilder.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(f6, f8).setColor(0.1F, 0.1F, 0.1F, 1.0F);
        bufferbuilder.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(f7, f8).setColor(0.1F, 0.1F, 0.1F, 1.0F);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
    }

    private static void renderWater(Minecraft p_110726_, PoseStack p_110727_)
    {
        renderFluid(p_110726_, p_110727_, UNDERWATER_LOCATION);
    }

    public static void renderFluid(Minecraft minecraftIn, PoseStack matrixStackIn, ResourceLocation textureIn)
    {
        if (!Config.isShaders() || Shaders.isUnderwaterOverlay())
        {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, textureIn);

            if (SmartAnimations.isActive())
            {
                SmartAnimations.textureRendered(minecraftIn.getTextureManager().getTexture(UNDERWATER_LOCATION).getId());
            }

            BlockPos blockpos = BlockPos.containing(minecraftIn.player.getX(), minecraftIn.player.getEyeY(), minecraftIn.player.getZ());
            float f = LightTexture.getBrightness(minecraftIn.player.level().dimensionType(), minecraftIn.player.level().getMaxLocalRawBrightness(blockpos));
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(f, f, f, 0.1F);
            float f1 = 4.0F;
            float f2 = -1.0F;
            float f3 = 1.0F;
            float f4 = -1.0F;
            float f5 = 1.0F;
            float f6 = -0.5F;
            float f7 = -minecraftIn.player.getYRot() / 64.0F;
            float f8 = minecraftIn.player.getXRot() / 64.0F;
            Matrix4f matrix4f = matrixStackIn.last().pose();
            BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferbuilder.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(4.0F + f7, 4.0F + f8);
            bufferbuilder.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(0.0F + f7, 4.0F + f8);
            bufferbuilder.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(0.0F + f7, 0.0F + f8);
            bufferbuilder.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(4.0F + f7, 0.0F + f8);
            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    private static void renderFire(Minecraft p_110729_, PoseStack p_110730_)
    {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.depthFunc(519);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        TextureAtlasSprite textureatlassprite = ModelBakery.FIRE_1.sprite();

        if (SmartAnimations.isActive())
        {
            SmartAnimations.spriteRendered(textureatlassprite);
        }

        RenderSystem.setShaderTexture(0, textureatlassprite.atlasLocation());
        float f = textureatlassprite.getU0();
        float f1 = textureatlassprite.getU1();
        float f2 = (f + f1) / 2.0F;
        float f3 = textureatlassprite.getV0();
        float f4 = textureatlassprite.getV1();
        float f5 = (f3 + f4) / 2.0F;
        float f6 = textureatlassprite.uvShrinkRatio();
        float f7 = Mth.lerp(f6, f, f2);
        float f8 = Mth.lerp(f6, f1, f2);
        float f9 = Mth.lerp(f6, f3, f5);
        float f10 = Mth.lerp(f6, f4, f5);
        float f11 = 1.0F;

        for (int i = 0; i < 2; i++)
        {
            p_110730_.pushPose();
            float f12 = -0.5F;
            float f13 = 0.5F;
            float f14 = -0.5F;
            float f15 = 0.5F;
            float f16 = -0.5F;
            p_110730_.translate((float)(-(i * 2 - 1)) * 0.24F, -0.3F, 0.0F);
            p_110730_.mulPose(Axis.YP.rotationDegrees((float)(i * 2 - 1) * 10.0F));
            Matrix4f matrix4f = p_110730_.last().pose();
            BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            bufferbuilder.addVertex(matrix4f, -0.5F, -0.5F, -0.5F).setUv(f8, f10).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            bufferbuilder.addVertex(matrix4f, 0.5F, -0.5F, -0.5F).setUv(f7, f10).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            bufferbuilder.addVertex(matrix4f, 0.5F, 0.5F, -0.5F).setUv(f7, f9).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            bufferbuilder.addVertex(matrix4f, -0.5F, 0.5F, -0.5F).setUv(f8, f9).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
            p_110730_.popPose();
        }

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(515);
    }
}
