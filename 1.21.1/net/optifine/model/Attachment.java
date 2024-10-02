package net.optifine.model;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.optifine.util.ArrayUtils;
import net.optifine.util.Json;

public class Attachment
{
    private AttachmentType type;
    private float[] translate;

    public Attachment(AttachmentType type, float[] translate)
    {
        this.type = type;
        this.translate = translate;
    }

    public AttachmentType getType()
    {
        return this.type;
    }

    public float[] getTranslate()
    {
        return this.translate;
    }

    public void applyTransform(PoseStack matrixStackIn)
    {
        if (this.translate[0] != 0.0F || this.translate[1] != 0.0F || this.translate[2] != 0.0F)
        {
            matrixStackIn.translate(this.translate[0], this.translate[1], this.translate[2]);
        }
    }

    @Override
    public String toString()
    {
        return this.type + ", translate: " + ArrayUtils.arrayToString(this.translate);
    }

    public static Attachment parse(JsonObject jo, AttachmentType type)
    {
        if (jo == null)
        {
            return null;
        }
        else if (type == null)
        {
            return null;
        }
        else
        {
            float[] afloat = Json.parseFloatArray(jo.get(type.getName()), 3, null);
            return afloat == null ? null : new Attachment(type, afloat);
        }
    }
}
