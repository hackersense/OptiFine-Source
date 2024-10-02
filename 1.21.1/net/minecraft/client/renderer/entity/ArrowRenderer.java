package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.optifine.entity.model.ArrowModel;

public abstract class ArrowRenderer<T extends AbstractArrow> extends EntityRenderer<T>
{
    public ArrowModel model;

    public ArrowRenderer(EntityRendererProvider.Context p_173917_)
    {
        super(p_173917_);
    }

    public void render(T p_113839_, float p_113840_, float p_113841_, PoseStack p_113842_, MultiBufferSource p_113843_, int p_113844_)
    {
        p_113842_.pushPose();
        p_113842_.mulPose(Axis.YP.rotationDegrees(Mth.lerp(p_113841_, p_113839_.yRotO, p_113839_.getYRot()) - 90.0F));
        p_113842_.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(p_113841_, p_113839_.xRotO, p_113839_.getXRot())));
        int i = 0;
        float f = 0.0F;
        float f1 = 0.5F;
        float f2 = 0.0F;
        float f3 = 0.15625F;
        float f4 = 0.0F;
        float f5 = 0.15625F;
        float f6 = 0.15625F;
        float f7 = 0.3125F;
        float f8 = 0.05625F;
        float f9 = (float)p_113839_.shakeTime - p_113841_;

        if (f9 > 0.0F)
        {
            float f10 = -Mth.sin(f9 * 3.0F) * f9;
            p_113842_.mulPose(Axis.ZP.rotationDegrees(f10));
        }

        p_113842_.mulPose(Axis.XP.rotationDegrees(45.0F));
        p_113842_.scale(0.05625F, 0.05625F, 0.05625F);
        p_113842_.translate(-4.0F, 0.0F, 0.0F);
        RenderType rendertype = RenderType.entityCutout(this.getTextureLocation(p_113839_));

        if (this.model != null)
        {
            rendertype = this.model.renderType(this.getTextureLocation(p_113839_));
        }

        VertexConsumer vertexconsumer = p_113843_.getBuffer(rendertype);
        PoseStack.Pose posestack$pose = p_113842_.last();

        if (this.model != null)
        {
            p_113842_.scale(16.0F, 16.0F, 16.0F);
            this.model.renderToBuffer(p_113842_, vertexconsumer, p_113844_, OverlayTexture.NO_OVERLAY, -1);
        }
        else
        {
            this.vertex(posestack$pose, vertexconsumer, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0, p_113844_);
            this.vertex(posestack$pose, vertexconsumer, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, p_113844_);
            this.vertex(posestack$pose, vertexconsumer, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, p_113844_);
            this.vertex(posestack$pose, vertexconsumer, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0, p_113844_);
            this.vertex(posestack$pose, vertexconsumer, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0, p_113844_);
            this.vertex(posestack$pose, vertexconsumer, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, p_113844_);
            this.vertex(posestack$pose, vertexconsumer, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, p_113844_);
            this.vertex(posestack$pose, vertexconsumer, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0, p_113844_);

            for (int j = 0; j < 4; j++)
            {
                p_113842_.mulPose(Axis.XP.rotationDegrees(90.0F));
                this.vertex(posestack$pose, vertexconsumer, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, p_113844_);
                this.vertex(posestack$pose, vertexconsumer, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, p_113844_);
                this.vertex(posestack$pose, vertexconsumer, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, p_113844_);
                this.vertex(posestack$pose, vertexconsumer, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, p_113844_);
            }
        }

        p_113842_.popPose();
        super.render(p_113839_, p_113840_, p_113841_, p_113842_, p_113843_, p_113844_);
    }

    public void vertex(
        PoseStack.Pose p_327779_,
        VertexConsumer p_253902_,
        int p_254058_,
        int p_254338_,
        int p_254196_,
        float p_254003_,
        float p_254165_,
        int p_253982_,
        int p_254037_,
        int p_254038_,
        int p_254271_
    )
    {
        p_253902_.addVertex(p_327779_, (float)p_254058_, (float)p_254338_, (float)p_254196_)
        .setColor(-1)
        .setUv(p_254003_, p_254165_)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(p_254271_)
        .setNormal(p_327779_, (float)p_253982_, (float)p_254038_, (float)p_254037_);
    }
}
