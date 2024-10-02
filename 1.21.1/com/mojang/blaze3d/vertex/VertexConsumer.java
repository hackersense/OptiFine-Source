package com.mojang.blaze3d.vertex;

import java.nio.ByteBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.IForgeVertexConsumer;
import net.optifine.Config;
import net.optifine.IRandomEntity;
import net.optifine.RandomEntities;
import net.optifine.reflect.Reflector;
import net.optifine.render.RenderEnv;
import net.optifine.render.VertexPosition;
import net.optifine.shaders.Shaders;
import net.optifine.util.MathUtils;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public interface VertexConsumer extends IForgeVertexConsumer
{
    ThreadLocal<RenderEnv> RENDER_ENV = ThreadLocal.withInitial(() -> new RenderEnv(Blocks.AIR.defaultBlockState(), new BlockPos(0, 0, 0)));
    boolean FORGE = Reflector.ForgeHooksClient.exists();

default RenderEnv getRenderEnv(BlockState blockState, BlockPos blockPos)
    {
        RenderEnv renderenv = RENDER_ENV.get();
        renderenv.reset(blockState, blockPos);
        return renderenv;
    }

    VertexConsumer addVertex(float p_344294_, float p_342213_, float p_344859_);

    VertexConsumer setColor(int p_342749_, int p_344324_, int p_343336_, int p_342831_);

    VertexConsumer setUv(float p_344155_, float p_345269_);

    VertexConsumer setUv1(int p_344168_, int p_342818_);

    VertexConsumer setUv2(int p_342773_, int p_345341_);

    VertexConsumer setNormal(float p_342733_, float p_342268_, float p_344916_);

default void addVertex(
            float p_342335_,
            float p_342594_,
            float p_342395_,
            int p_344436_,
            float p_344317_,
            float p_344558_,
            int p_344862_,
            int p_343109_,
            float p_343232_,
            float p_342995_,
            float p_343739_
        )
    {
        this.addVertex(p_342335_, p_342594_, p_342395_);
        this.setColor(p_344436_);
        this.setUv(p_344317_, p_344558_);
        this.setOverlay(p_344862_);
        this.setLight(p_343109_);
        this.setNormal(p_343232_, p_342995_, p_343739_);
    }

default VertexConsumer setColor(float p_345344_, float p_343040_, float p_343668_, float p_342740_)
    {
        return this.setColor((int)(p_345344_ * 255.0F), (int)(p_343040_ * 255.0F), (int)(p_343668_ * 255.0F), (int)(p_342740_ * 255.0F));
    }

default VertexConsumer setColor(int p_345390_)
    {
        return this.setColor(
                   FastColor.ARGB32.red(p_345390_),
                   FastColor.ARGB32.green(p_345390_),
                   FastColor.ARGB32.blue(p_345390_),
                   FastColor.ARGB32.alpha(p_345390_)
               );
    }

default VertexConsumer setWhiteAlpha(int p_342254_)
    {
        return this.setColor(FastColor.ARGB32.color(p_342254_, -1));
    }

default VertexConsumer setLight(int p_342385_)
    {
        return this.setUv2(p_342385_ & 65535, p_342385_ >> 16 & 65535);
    }

default VertexConsumer setOverlay(int p_345433_)
    {
        return this.setUv1(p_345433_ & 65535, p_345433_ >> 16 & 65535);
    }

default void putBulkData(
            PoseStack.Pose p_85996_, BakedQuad p_85997_, float p_85999_, float p_86000_, float p_86001_, float p_330684_, int p_86003_, int p_332867_
        )
    {
        this.putBulkData(
            p_85996_,
            p_85997_,
            this.getTempFloat4(1.0F, 1.0F, 1.0F, 1.0F),
            p_85999_,
            p_86000_,
            p_86001_,
            p_330684_,
            this.getTempInt4(p_86003_, p_86003_, p_86003_, p_86003_),
            p_332867_,
            false
        );
    }

    @Override

default void putBulkData(
            PoseStack.Pose matrixEntry,
            BakedQuad bakedQuad,
            float red,
            float green,
            float blue,
            float alpha,
            int packedLight,
            int packedOverlay,
            boolean readExistingColor
        )
    {
        this.putBulkData(
            matrixEntry,
            bakedQuad,
            this.getTempFloat4(1.0F, 1.0F, 1.0F, 1.0F),
            red,
            green,
            blue,
            alpha,
            this.getTempInt4(packedLight, packedLight, packedLight, packedLight),
            packedOverlay,
            readExistingColor
        );
    }

default void putBulkData(
            PoseStack.Pose p_85988_,
            BakedQuad p_85989_,
            float[] p_331915_,
            float p_85990_,
            float p_85991_,
            float p_85992_,
            float p_335371_,
            int[] p_331444_,
            int p_85993_,
            boolean p_329910_
        )
    {
        int[] aint = this.isMultiTexture() ? p_85989_.getVertexDataSingle() : p_85989_.getVertices();
        this.putSprite(p_85989_.getSprite());
        boolean flag = ModelBlockRenderer.isSeparateAoLightValue();
        Vec3i vec3i = p_85989_.getDirection().getNormal();
        Matrix4f matrix4f = p_85988_.pose();
        Vector3f vector3f = p_85988_.transformNormal((float)vec3i.getX(), (float)vec3i.getY(), (float)vec3i.getZ(), this.getTempVec3f());
        float f = vector3f.x;
        float f1 = vector3f.y;
        float f2 = vector3f.z;
        int i = 8;
        int j = DefaultVertexFormat.BLOCK.getIntegerSize();
        int k = aint.length / j;
        int l = (int)(p_335371_ * 255.0F);
        boolean flag1 = Config.isShaders() && Shaders.useVelocityAttrib && Config.isMinecraftThread();

        if (flag1)
        {
            IRandomEntity irandomentity = RandomEntities.getRandomEntityRendered();

            if (irandomentity != null)
            {
                VertexPosition[] avertexposition = p_85989_.getVertexPositions(irandomentity.getId());
                this.setQuadVertexPositions(avertexposition);
            }
        }

        for (int j1 = 0; j1 < k; j1++)
        {
            int k1 = j1 * j;
            float f3 = Float.intBitsToFloat(aint[k1 + 0]);
            float f4 = Float.intBitsToFloat(aint[k1 + 1]);
            float f5 = Float.intBitsToFloat(aint[k1 + 2]);
            float f9 = flag ? 1.0F : p_331915_[j1];
            float f6;
            float f7;
            float f8;

            if (p_329910_)
            {
                int i1 = aint[k1 + 3];
                float f10 = (float)(i1 & 0xFF);
                float f11 = (float)(i1 >> 8 & 0xFF);
                float f12 = (float)(i1 >> 16 & 0xFF);
                f6 = f10 * f9 * p_85990_;
                f7 = f11 * f9 * p_85991_;
                f8 = f12 * f9 * p_85992_;
            }
            else
            {
                f6 = f9 * p_85990_ * 255.0F;
                f7 = f9 * p_85991_ * 255.0F;
                f8 = f9 * p_85992_ * 255.0F;
            }

            if (flag)
            {
                l = (int)(p_331915_[j1] * 255.0F);
            }

            int l1 = FastColor.ARGB32.color(l, (int)f6, (int)f7, (int)f8);
            int i2 = p_331444_[j1];

            if (FORGE)
            {
                i2 = this.applyBakedLighting(p_331444_[j1], aint, k1);
            }

            float f16 = Float.intBitsToFloat(aint[k1 + 4]);
            float f17 = Float.intBitsToFloat(aint[k1 + 5]);
            float f13 = MathUtils.getTransformX(matrix4f, f3, f4, f5);
            float f14 = MathUtils.getTransformY(matrix4f, f3, f4, f5);
            float f15 = MathUtils.getTransformZ(matrix4f, f3, f4, f5);

            if (FORGE)
            {
                Vector3f vector3f1 = this.applyBakedNormals(aint, k1, p_85988_.normal());

                if (vector3f1 != null)
                {
                    f = vector3f1.x();
                    f1 = vector3f1.y();
                    f2 = vector3f1.z();
                }
            }

            this.addVertex(f13, f14, f15, l1, f16, f17, p_85993_, i2, f, f1, f2);
        }
    }

default VertexConsumer addVertex(Vector3f p_343309_)
    {
        return this.addVertex(p_343309_.x(), p_343309_.y(), p_343309_.z());
    }

default VertexConsumer addVertex(PoseStack.Pose p_343718_, Vector3f p_344795_)
    {
        return this.addVertex(p_343718_, p_344795_.x(), p_344795_.y(), p_344795_.z());
    }

default VertexConsumer addVertex(PoseStack.Pose p_343203_, float p_343315_, float p_342573_, float p_344986_)
    {
        return this.addVertex(p_343203_.pose(), p_343315_, p_342573_, p_344986_);
    }

default VertexConsumer addVertex(Matrix4f p_344823_, float p_342636_, float p_342677_, float p_343814_)
    {
        Vector3f vector3f = p_344823_.transformPosition(p_342636_, p_342677_, p_343814_, this.getTempVec3f());
        return this.addVertex(vector3f.x(), vector3f.y(), vector3f.z());
    }

default VertexConsumer setNormal(PoseStack.Pose p_343706_, float p_345121_, float p_344892_, float p_344341_)
    {
        Vector3f vector3f = p_343706_.transformNormal(p_345121_, p_344892_, p_344341_, this.getTempVec3f());
        return this.setNormal(vector3f.x(), vector3f.y(), vector3f.z());
    }

default void putSprite(TextureAtlasSprite sprite)
    {
    }

default void setSprite(TextureAtlasSprite sprite)
    {
    }

default boolean isMultiTexture()
    {
        return false;
    }

default RenderType getRenderType()
    {
        return null;
    }

default Vector3f getTempVec3f()
    {
        return new Vector3f();
    }

default Vector3f getTempVec3f(float x, float y, float z)
    {
        return this.getTempVec3f().set(x, y, z);
    }

default Vector3f getTempVec3f(Vector3f vec)
    {
        return this.getTempVec3f().set(vec);
    }

default float[] getTempFloat4(float f1, float f2, float f3, float f4)
    {
        return new float[] {f1, f2, f3, f4};
    }

default int[] getTempInt4(int i1, int i2, int i3, int i4)
    {
        return new int[] {i1, i2, i3, i4};
    }

default MultiBufferSource.BufferSource getRenderTypeBuffer()
    {
        return null;
    }

default void setQuadVertexPositions(VertexPosition[] vps)
    {
    }

default void setMidBlock(float mbx, float mby, float mbz)
    {
    }

default Vector3f getMidBlock()
    {
        return null;
    }

default VertexConsumer getSecondaryBuilder()
    {
        return null;
    }

default int getVertexCount()
    {
        return 0;
    }

default int applyBakedLighting(int lightmapCoord, int[] data, int pos)
    {
        int i = getLightOffset(0);
        int j = LightTexture.block(data[pos + i]);
        int k = LightTexture.sky(data[pos + i]);

        if (j == 0 && k == 0)
        {
            return lightmapCoord;
        }
        else
        {
            int l = LightTexture.block(lightmapCoord);
            int i1 = LightTexture.sky(lightmapCoord);
            l = Math.max(l, j);
            i1 = Math.max(i1, k);
            return LightTexture.pack(l, i1);
        }
    }

    static int getLightOffset(int v)
    {
        return v * 8 + 6;
    }

default Vector3f applyBakedNormals(int[] data, int pos, Matrix3f normalTransform)
    {
        int i = 7;
        int j = data[pos + i];
        byte b0 = (byte)(j >> 0 & 0xFF);
        byte b1 = (byte)(j >> 8 & 0xFF);
        byte b2 = (byte)(j >> 16 & 0xFF);

        if (b0 == 0 && b1 == 0 && b2 == 0)
        {
            return null;
        }
        else
        {
            Vector3f vector3f = this.getTempVec3f((float)b0 / 127.0F, (float)b1 / 127.0F, (float)b2 / 127.0F);
            MathUtils.transform(vector3f, normalTransform);
            return vector3f;
        }
    }

default void getBulkData(ByteBuffer buffer)
    {
    }

default void putBulkData(ByteBuffer buffer)
    {
    }

default boolean canAddVertexFast()
    {
        return false;
    }

default void addVertexFast(float x, float y, float z, int color, float texU, float texV, int overlayUV, int lightmapUV, int normals)
    {
    }
}
