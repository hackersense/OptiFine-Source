package com.mojang.blaze3d.vertex;

import com.google.common.collect.Queues;
import com.mojang.math.Axis;
import com.mojang.math.MatrixUtil;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.Util;
import net.minecraftforge.client.extensions.IForgePoseStack;
import net.optifine.util.MathUtils;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PoseStack implements IForgePoseStack
{
    Deque<PoseStack.Pose> freeEntries = new ArrayDeque<>();
    private final Deque<PoseStack.Pose> poseStack = Util.make(Queues.newArrayDeque(), dequeIn ->
    {
        Matrix4f matrix4f = new Matrix4f();
        Matrix3f matrix3f = new Matrix3f();
        dequeIn.add(new PoseStack.Pose(matrix4f, matrix3f));
    });

    public void translate(double p_85838_, double p_85839_, double p_85840_)
    {
        this.translate((float)p_85838_, (float)p_85839_, (float)p_85840_);
    }

    public void translate(float p_254202_, float p_253782_, float p_254238_)
    {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.translate(p_254202_, p_253782_, p_254238_);
    }

    public void scale(float p_85842_, float p_85843_, float p_85844_)
    {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.scale(p_85842_, p_85843_, p_85844_);

        if (Math.abs(p_85842_) != Math.abs(p_85843_) || Math.abs(p_85843_) != Math.abs(p_85844_))
        {
            posestack$pose.normal.scale(1.0F / p_85842_, 1.0F / p_85843_, 1.0F / p_85844_);
            posestack$pose.trustedNormals = false;
        }
        else if (p_85842_ < 0.0F || p_85843_ < 0.0F || p_85844_ < 0.0F)
        {
            posestack$pose.normal.scale(Math.signum(p_85842_), Math.signum(p_85843_), Math.signum(p_85844_));
        }
    }

    public void mulPose(Quaternionf p_254385_)
    {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.rotate(p_254385_);
        posestack$pose.normal.rotate(p_254385_);
    }

    public void rotateAround(Quaternionf p_272904_, float p_273581_, float p_272655_, float p_273275_)
    {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.rotateAround(p_272904_, p_273581_, p_272655_, p_273275_);
        posestack$pose.normal.rotate(p_272904_);
    }

    public void pushPose()
    {
        PoseStack.Pose posestack$pose = this.freeEntries.pollLast();

        if (posestack$pose != null)
        {
            PoseStack.Pose posestack$pose1 = this.poseStack.getLast();
            posestack$pose.pose.set(posestack$pose1.pose);
            posestack$pose.normal.set(posestack$pose1.normal);
            posestack$pose.trustedNormals = posestack$pose1.trustedNormals;
            this.poseStack.addLast(posestack$pose);
        }
        else
        {
            this.poseStack.addLast(new PoseStack.Pose(this.poseStack.getLast()));
        }
    }

    public void popPose()
    {
        PoseStack.Pose posestack$pose = this.poseStack.removeLast();

        if (posestack$pose != null)
        {
            this.freeEntries.add(posestack$pose);
        }
    }

    public PoseStack.Pose last()
    {
        return this.poseStack.getLast();
    }

    public boolean clear()
    {
        return this.poseStack.size() == 1;
    }

    public void rotateDegXp(float angle)
    {
        this.mulPose(Axis.XP.rotationDegrees(angle));
    }

    public void rotateDegXn(float angle)
    {
        this.mulPose(Axis.XN.rotationDegrees(angle));
    }

    public void rotateDegYp(float angle)
    {
        this.mulPose(Axis.YP.rotationDegrees(angle));
    }

    public void rotateDegYn(float angle)
    {
        this.mulPose(Axis.YN.rotationDegrees(angle));
    }

    public void rotateDegZp(float angle)
    {
        this.mulPose(Axis.ZP.rotationDegrees(angle));
    }

    public void rotateDegZn(float angle)
    {
        this.mulPose(Axis.ZN.rotationDegrees(angle));
    }

    public void rotateDeg(float angle, float x, float y, float z)
    {
        Vector3f vector3f = new Vector3f(x, y, z);
        Quaternionf quaternionf = MathUtils.rotationDegrees(vector3f, angle);
        this.mulPose(quaternionf);
    }

    public int size()
    {
        return this.poseStack.size();
    }

    @Override
    public String toString()
    {
        return this.last().toString() + "Depth: " + this.poseStack.size();
    }

    public void setIdentity()
    {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.identity();
        posestack$pose.normal.identity();
        posestack$pose.trustedNormals = true;
    }

    public void mulPose(Matrix4f p_332918_)
    {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.mul(p_332918_);

        if (!MatrixUtil.isPureTranslation(p_332918_))
        {
            if (MatrixUtil.isOrthonormal(p_332918_))
            {
                posestack$pose.normal.mul(new Matrix3f(p_332918_));
            }
            else
            {
                posestack$pose.computeNormalMatrix();
            }
        }
    }

    public static final class Pose
    {
        final Matrix4f pose;
        final Matrix3f normal;
        boolean trustedNormals = true;

        Pose(Matrix4f p_254509_, Matrix3f p_254348_)
        {
            this.pose = p_254509_;
            this.normal = p_254348_;
        }

        Pose(PoseStack.Pose p_328466_)
        {
            this.pose = new Matrix4f(p_328466_.pose);
            this.normal = new Matrix3f(p_328466_.normal);
            this.trustedNormals = p_328466_.trustedNormals;
        }

        void computeNormalMatrix()
        {
            this.normal.set(this.pose).invert().transpose();
            this.trustedNormals = false;
        }

        public Matrix4f pose()
        {
            return this.pose;
        }

        public Matrix3f normal()
        {
            return this.normal;
        }

        public Vector3f transformNormal(Vector3f p_332767_, Vector3f p_333196_)
        {
            return this.transformNormal(p_332767_.x, p_332767_.y, p_332767_.z, p_333196_);
        }

        public Vector3f transformNormal(float p_333912_, float p_334796_, float p_329732_, Vector3f p_328781_)
        {
            Vector3f vector3f = this.normal.transform(p_333912_, p_334796_, p_329732_, p_328781_);
            return this.trustedNormals ? vector3f : vector3f.normalize();
        }

        public PoseStack.Pose copy()
        {
            return new PoseStack.Pose(this);
        }

        @Override
        public String toString()
        {
            return this.pose.toString() + this.normal.toString();
        }
    }
}
