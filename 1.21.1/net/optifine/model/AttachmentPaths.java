package net.optifine.model;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;

public class AttachmentPaths
{
    private List<List<AttachmentPath>> pathsByType = new ArrayList<>();

    public void addPath(AttachmentPath ap)
    {
        AttachmentType attachmenttype = ap.getAttachment().getType();

        while (this.pathsByType.size() <= attachmenttype.ordinal())
        {
            this.pathsByType.add(null);
        }

        List<AttachmentPath> list = this.pathsByType.get(attachmenttype.ordinal());

        if (list == null)
        {
            list = new ArrayList<>();
            this.pathsByType.set(attachmenttype.ordinal(), list);
        }

        list.add(ap);
    }

    public void addPaths(List<ModelPart> parents, Attachment[] attachments)
    {
        ModelPart[] amodelpart = parents.toArray(new ModelPart[parents.size()]);

        for (int i = 0; i < attachments.length; i++)
        {
            Attachment attachment = attachments[i];
            AttachmentPath attachmentpath = new AttachmentPath(attachment, amodelpart);
            this.addPath(attachmentpath);
        }
    }

    public boolean isEmpty()
    {
        return this.pathsByType.isEmpty();
    }

    public AttachmentPath getVisiblePath(AttachmentType typeIn)
    {
        if (this.pathsByType.size() <= typeIn.ordinal())
        {
            return null;
        }
        else
        {
            List<AttachmentPath> list = this.pathsByType.get(typeIn.ordinal());

            if (list == null)
            {
                return null;
            }
            else
            {
                for (AttachmentPath attachmentpath : list)
                {
                    if (attachmentpath.isVisible())
                    {
                        return attachmentpath;
                    }
                }

                return null;
            }
        }
    }

    @Override
    public String toString()
    {
        int i = 0;

        for (List list : this.pathsByType)
        {
            if (list != null)
            {
                i++;
            }
        }

        return "types: " + i;
    }
}
