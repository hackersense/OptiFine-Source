package net.optifine.model;

import net.optifine.Config;

public enum AttachmentType
{
    LEFT_HANDHELD_ITEM("left_handheld_item"),
    RIGHT_HANDHELD_ITEM("right_handheld_item"),
    HANDHELD_ITEM("handheld_item"),
    HEAD("head"),
    LEAD("lead");

    private String name;

    private AttachmentType(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    @Override
    public String toString()
    {
        return this.name;
    }

    public static AttachmentType parse(String str)
    {
        if (str == null)
        {
            return null;
        }
        else
        {
            for (AttachmentType attachmenttype : values())
            {
                if (Config.equals(attachmenttype.getName(), str))
                {
                    return attachmenttype;
                }
            }

            return null;
        }
    }
}
