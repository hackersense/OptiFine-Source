package net.optifine.render;

import net.optifine.shaders.Shaders;
import net.optifine.util.MathUtils;
import org.joml.Matrix4f;

public class VertexPosition
{
    private int frameId;
    private float posX;
    private float posY;
    private float posZ;
    private float velocityX;
    private float velocityY;
    private float velocityZ;
    private boolean velocityValid;

    public void setPosition(int frameId, float x, float y, float z)
    {
        if (!Shaders.isShadowPass)
        {
            if (frameId != this.frameId)
            {
                Matrix4f matrix4f = Shaders.getLastModelView();
                float f = MathUtils.getTransformX(matrix4f, x, y, z);
                float f1 = MathUtils.getTransformY(matrix4f, x, y, z);
                float f2 = MathUtils.getTransformZ(matrix4f, x, y, z);

                if (this.frameId != 0)
                {
                    this.velocityX = f - this.posX;
                    this.velocityY = f1 - this.posY;
                    this.velocityZ = f2 - this.posZ;
                    this.velocityValid = frameId - this.frameId <= 3 && !Shaders.pointOfViewChanged;
                }

                this.frameId = frameId;
                this.posX = f;
                this.posY = f1;
                this.posZ = f2;
            }
        }
    }

    public boolean isVelocityValid()
    {
        return this.velocityValid;
    }

    public float getVelocityX()
    {
        return this.velocityX;
    }

    public float getVelocityY()
    {
        return this.velocityY;
    }

    public float getVelocityZ()
    {
        return this.velocityZ;
    }

    public int getFrameId()
    {
        return this.frameId;
    }
}
