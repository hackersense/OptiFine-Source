package net.minecraft.client.renderer.culling;

import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import net.optifine.util.MathUtils;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Frustum
{
    public static final int OFFSET_STEP = 4;
    private final FrustumIntersection intersection = new FrustumIntersection();
    private final Matrix4f matrix = new Matrix4f();
    protected Vector4f viewVector;
    private double camX;
    private double camY;
    private double camZ;
    public boolean disabled = false;
    protected boolean usePlanes = false;
    protected final Vector4f[] frustum = new Vector4f[6];

    public Frustum(Matrix4f p_254207_, Matrix4f p_254535_)
    {
        this.calculateFrustum(p_254207_, p_254535_);
    }

    public Frustum(Frustum p_194440_)
    {
        this.intersection.set(p_194440_.matrix);
        this.matrix.set(p_194440_.matrix);
        this.camX = p_194440_.camX;
        this.camY = p_194440_.camY;
        this.camZ = p_194440_.camZ;
        this.viewVector = p_194440_.viewVector;
        this.disabled = p_194440_.disabled;
        this.usePlanes = p_194440_.usePlanes;
        System.arraycopy(p_194440_.frustum, 0, this.frustum, 0, p_194440_.frustum.length);
    }

    public Frustum offsetToFullyIncludeCameraCube(int p_194442_)
    {
        double d0 = Math.floor(this.camX / (double)p_194442_) * (double)p_194442_;
        double d1 = Math.floor(this.camY / (double)p_194442_) * (double)p_194442_;
        double d2 = Math.floor(this.camZ / (double)p_194442_) * (double)p_194442_;
        double d3 = Math.ceil(this.camX / (double)p_194442_) * (double)p_194442_;
        double d4 = Math.ceil(this.camY / (double)p_194442_) * (double)p_194442_;
        int i = 0;

        for (double d5 = Math.ceil(this.camZ / (double)p_194442_) * (double)p_194442_;
                this.intersection
                .intersectAab(
                    (float)(d0 - this.camX),
                    (float)(d1 - this.camY),
                    (float)(d2 - this.camZ),
                    (float)(d3 - this.camX),
                    (float)(d4 - this.camY),
                    (float)(d5 - this.camZ)
                )
                != -2;
                this.camZ = this.camZ - (double)(this.viewVector.z() * 4.0F)
            )
        {
            this.camX = this.camX - (double)(this.viewVector.x() * 4.0F);
            this.camY = this.camY - (double)(this.viewVector.y() * 4.0F);

            if (i++ > 10)
            {
                break;
            }
        }

        return this;
    }

    public void prepare(double p_113003_, double p_113004_, double p_113005_)
    {
        this.camX = p_113003_;
        this.camY = p_113004_;
        this.camZ = p_113005_;
    }

    private void calculateFrustum(Matrix4f p_253909_, Matrix4f p_254521_)
    {
        p_254521_.mul(p_253909_, this.matrix);
        this.intersection.set(this.matrix);
        this.viewVector = this.matrix.transformTranspose(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
        Matrix4f matrix4f = new Matrix4f(this.matrix).transpose();
        this.setFrustumPlane(matrix4f, -1, 0, 0, 0);
        this.setFrustumPlane(matrix4f, 1, 0, 0, 1);
        this.setFrustumPlane(matrix4f, 0, -1, 0, 2);
        this.setFrustumPlane(matrix4f, 0, 1, 0, 3);
        this.setFrustumPlane(matrix4f, 0, 0, -1, 4);
        this.setFrustumPlane(matrix4f, 0, 0, 1, 5);
    }

    public boolean isVisible(AABB p_113030_)
    {
        return p_113030_ == IForgeBlockEntity.INFINITE_EXTENT_AABB
               ? true
               : this.cubeInFrustum(p_113030_.minX, p_113030_.minY, p_113030_.minZ, p_113030_.maxX, p_113030_.maxY, p_113030_.maxZ);
    }

    private boolean cubeInFrustum(double p_113007_, double p_113008_, double p_113009_, double p_113010_, double p_113011_, double p_113012_)
    {
        if (this.disabled)
        {
            return true;
        }
        else
        {
            float f = (float)(p_113007_ - this.camX);
            float f1 = (float)(p_113008_ - this.camY);
            float f2 = (float)(p_113009_ - this.camZ);
            float f3 = (float)(p_113010_ - this.camX);
            float f4 = (float)(p_113011_ - this.camY);
            float f5 = (float)(p_113012_ - this.camZ);
            return this.usePlanes ? this.isBoxInFrustumRaw(f, f1, f2, f3, f4, f5) : this.intersection.testAab(f, f1, f2, f3, f4, f5);
        }
    }

    private boolean isBoxInFrustumRaw(float minX, float minY, float minZ, float maxX, float maxY, float maxZ)
    {
        for (int i = 0; i < 6; i++)
        {
            Vector4f vector4f = this.frustum[i];
            float f = vector4f.x();
            float f1 = vector4f.y();
            float f2 = vector4f.z();
            float f3 = vector4f.w();

            if (f * minX + f1 * minY + f2 * minZ + f3 <= 0.0F
                    && f * maxX + f1 * minY + f2 * minZ + f3 <= 0.0F
                    && f * minX + f1 * maxY + f2 * minZ + f3 <= 0.0F
                    && f * maxX + f1 * maxY + f2 * minZ + f3 <= 0.0F
                    && f * minX + f1 * minY + f2 * maxZ + f3 <= 0.0F
                    && f * maxX + f1 * minY + f2 * maxZ + f3 <= 0.0F
                    && f * minX + f1 * maxY + f2 * maxZ + f3 <= 0.0F
                    && f * maxX + f1 * maxY + f2 * maxZ + f3 <= 0.0F)
            {
                return false;
            }
        }

        return true;
    }

    public boolean isBoxInFrustumFully(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
    {
        if (this.disabled)
        {
            return true;
        }
        else
        {
            float f = (float)minX;
            float f1 = (float)minY;
            float f2 = (float)minZ;
            float f3 = (float)maxX;
            float f4 = (float)maxY;
            float f5 = (float)maxZ;

            for (int i = 0; i < 6; i++)
            {
                Vector4f vector4f = this.frustum[i];
                float f6 = vector4f.x();
                float f7 = vector4f.y();
                float f8 = vector4f.z();
                float f9 = vector4f.w();

                if (i < 4)
                {
                    if (f6 * f + f7 * f1 + f8 * f2 + f9 <= 0.0F
                            || f6 * f3 + f7 * f1 + f8 * f2 + f9 <= 0.0F
                            || f6 * f + f7 * f4 + f8 * f2 + f9 <= 0.0F
                            || f6 * f3 + f7 * f4 + f8 * f2 + f9 <= 0.0F
                            || f6 * f + f7 * f1 + f8 * f5 + f9 <= 0.0F
                            || f6 * f3 + f7 * f1 + f8 * f5 + f9 <= 0.0F
                            || f6 * f + f7 * f4 + f8 * f5 + f9 <= 0.0F
                            || f6 * f3 + f7 * f4 + f8 * f5 + f9 <= 0.0F)
                    {
                        return false;
                    }
                }
                else if (f6 * f + f7 * f1 + f8 * f2 + f9 <= 0.0F
                         && f6 * f3 + f7 * f1 + f8 * f2 + f9 <= 0.0F
                         && f6 * f + f7 * f4 + f8 * f2 + f9 <= 0.0F
                         && f6 * f3 + f7 * f4 + f8 * f2 + f9 <= 0.0F
                         && f6 * f + f7 * f1 + f8 * f5 + f9 <= 0.0F
                         && f6 * f3 + f7 * f1 + f8 * f5 + f9 <= 0.0F
                         && f6 * f + f7 * f4 + f8 * f5 + f9 <= 0.0F
                         && f6 * f3 + f7 * f4 + f8 * f5 + f9 <= 0.0F)
                {
                    return false;
                }
            }

            return true;
        }
    }

    public double getCameraX()
    {
        return this.camX;
    }

    public double getCameraY()
    {
        return this.camY;
    }

    public double getCameraZ()
    {
        return this.camZ;
    }

    private void setFrustumPlane(Matrix4f matrixIn, int xIn, int yIn, int zIn, int wIn)
    {
        Vector4f vector4f = new Vector4f((float)xIn, (float)yIn, (float)zIn, 1.0F);
        MathUtils.transform(vector4f, matrixIn);
        vector4f.normalize();
        this.frustum[wIn] = vector4f;
    }
}
