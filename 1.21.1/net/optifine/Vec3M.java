package net.optifine;

import java.util.EnumSet;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class Vec3M extends Vec3 implements Position
{
    public Vec3M()
    {
        super(0.0, 0.0, 0.0);
    }

    public Vec3M(double xIn, double yIn, double zIn)
    {
        super(xIn, yIn, zIn);
    }

    public Vec3M(Vector3f vecIn)
    {
        super(vecIn);
    }

    public Vec3M set(double x, double y, double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vec3M set(Vec3 vec)
    {
        return this.set(vec.x, vec.y, vec.z);
    }

    public Vec3M subtractReverse(Vec3 vec)
    {
        return this.set(vec.x - this.x, vec.y - this.y, vec.z - this.z);
    }

    public Vec3M normalize()
    {
        double d0 = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        return d0 < 1.0E-4 ? this.set(0.0, 0.0, 0.0) : this.set(this.x / d0, this.y / d0, this.z / d0);
    }

    @Override
    public double dot(Vec3 vec)
    {
        return this.x * vec.x + this.y * vec.y + this.z * vec.z;
    }

    public Vec3M crossProduct(Vec3 vec)
    {
        return this.set(
                   this.y * vec.z - this.z * vec.y,
                   this.z * vec.x - this.x * vec.z,
                   this.x * vec.y - this.y * vec.x
               );
    }

    public Vec3M subtract(Vec3 vec)
    {
        return this.subtract(vec.x, vec.y, vec.z);
    }

    public Vec3M subtract(double x, double y, double z)
    {
        return this.add(-x, -y, -z);
    }

    public Vec3M add(Vec3 vec)
    {
        return this.add(vec.x, vec.y, vec.z);
    }

    public Vec3M add(double x, double y, double z)
    {
        return this.set(this.x + x, this.y + y, this.z + z);
    }

    public Vec3M scale(double factor)
    {
        return this.mul(factor, factor, factor);
    }

    public Vec3M inverse()
    {
        return this.scale(-1.0);
    }

    public Vec3M mul(Vec3 vec)
    {
        return this.mul(vec.x, vec.y, vec.z);
    }

    public Vec3M mul(double factorX, double factorY, double factorZ)
    {
        return this.set(this.x * factorX, this.y * factorY, this.z * factorZ);
    }

    public Vec3M lerp(Vec3M vec, double factor)
    {
        return this.set(
                   Mth.lerp(factor, this.x, vec.x),
                   Mth.lerp(factor, this.y, vec.y),
                   Mth.lerp(factor, this.z, vec.z)
               );
    }

    public Vec3M rotatePitch(float pitch)
    {
        float f = Mth.cos(pitch);
        float f1 = Mth.sin(pitch);
        double d0 = this.x;
        double d1 = this.y * (double)f + this.z * (double)f1;
        double d2 = this.z * (double)f - this.y * (double)f1;
        return this.set(d0, d1, d2);
    }

    public Vec3M rotateYaw(float yaw)
    {
        float f = Mth.cos(yaw);
        float f1 = Mth.sin(yaw);
        double d0 = this.x * (double)f + this.z * (double)f1;
        double d1 = this.y;
        double d2 = this.z * (double)f - this.x * (double)f1;
        return this.set(d0, d1, d2);
    }

    public Vec3M zRot(float angle)
    {
        float f = Mth.cos(angle);
        float f1 = Mth.sin(angle);
        double d0 = this.x * (double)f + this.y * (double)f1;
        double d1 = this.y * (double)f - this.x * (double)f1;
        double d2 = this.z;
        return this.set(d0, d1, d2);
    }

    public Vec3M align(EnumSet<Direction.Axis> axes)
    {
        double d0 = axes.contains(Direction.Axis.X) ? (double)Mth.floor(this.x) : this.x;
        double d1 = axes.contains(Direction.Axis.Y) ? (double)Mth.floor(this.y) : this.y;
        double d2 = axes.contains(Direction.Axis.Z) ? (double)Mth.floor(this.z) : this.z;
        return this.set(d0, d1, d2);
    }

    @Override
    public double get(Direction.Axis axis)
    {
        return axis.choose(this.x, this.y, this.z);
    }

    public Vec3M with(Direction.Axis axis, double distance)
    {
        double d0 = axis == Direction.Axis.X ? distance : this.x;
        double d1 = axis == Direction.Axis.Y ? distance : this.y;
        double d2 = axis == Direction.Axis.Z ? distance : this.z;
        return this.set(d0, d1, d2);
    }

    public void setRgb(int rgb)
    {
        double d0 = (double)(rgb >> 16 & 0xFF) / 255.0;
        double d1 = (double)(rgb >> 8 & 0xFF) / 255.0;
        double d2 = (double)(rgb & 0xFF) / 255.0;
        this.set(d0, d1, d2);
    }

    public Vec3M fromRgbM(int rgb)
    {
        this.setRgb(rgb);
        return this;
    }
}
