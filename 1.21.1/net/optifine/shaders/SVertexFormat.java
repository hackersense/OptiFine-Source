package net.optifine.shaders;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class SVertexFormat
{
    public static final int vertexSizeBlock = 18;
    public static final int offsetMidBlock = 8;
    public static final int offsetMidTexCoord = 9;
    public static final int offsetTangent = 11;
    public static final int offsetEntity = 13;
    public static final int offsetVelocity = 15;
    public static final VertexFormatElement SHADERS_MIDBLOCK_3B = makeElement(
                "SHADERS_MIDOFFSET_3B", 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.PADDING, 3
            );
    public static final VertexFormatElement PADDING_1B = makeElement("PADDING_1B", 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.PADDING, 1);
    public static final VertexFormatElement SHADERS_MIDTEXCOORD_2F = makeElement(
                "SHADERS_MIDTEXCOORD_2F", 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.PADDING, 2
            );
    public static final VertexFormatElement SHADERS_TANGENT_4S = makeElement(
                "SHADERS_TANGENT_4S", 0, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.PADDING, 4
            );
    public static final VertexFormatElement SHADERS_MC_ENTITY_4S = makeElement(
                "SHADERS_MC_ENTITY_4S", 0, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.PADDING, 4
            );
    public static final VertexFormatElement SHADERS_VELOCITY_3F = makeElement(
                "SHADERS_VELOCITY_3F", 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.PADDING, 3
            );

    public static VertexFormat makeExtendedFormatBlock(VertexFormat blockVanilla)
    {
        VertexFormat.Builder vertexformat$builder = new VertexFormat.Builder();
        vertexformat$builder.addAll(blockVanilla);
        vertexformat$builder.add("MidOffset", SHADERS_MIDBLOCK_3B);
        vertexformat$builder.add("PaddingMO", PADDING_1B);
        vertexformat$builder.add("MidTexCoord", SHADERS_MIDTEXCOORD_2F);
        vertexformat$builder.add("Tangent", SHADERS_TANGENT_4S);
        vertexformat$builder.add("McEntity", SHADERS_MC_ENTITY_4S);
        vertexformat$builder.add("Velocity", SHADERS_VELOCITY_3F);
        VertexFormat vertexformat = vertexformat$builder.build();
        vertexformat.setExtended(true);
        return vertexformat;
    }

    public static VertexFormat makeExtendedFormatEntity(VertexFormat entityVanilla)
    {
        VertexFormat.Builder vertexformat$builder = new VertexFormat.Builder();
        vertexformat$builder.addAll(entityVanilla);
        vertexformat$builder.add("MidTexCoord", SHADERS_MIDTEXCOORD_2F);
        vertexformat$builder.add("Tangent", SHADERS_TANGENT_4S);
        vertexformat$builder.add("McEntity", SHADERS_MC_ENTITY_4S);
        vertexformat$builder.add("Velocity", SHADERS_VELOCITY_3F);
        VertexFormat vertexformat = vertexformat$builder.build();
        vertexformat.setExtended(true);
        return vertexformat;
    }

    private static VertexFormatElement makeElement(String name, int indexIn, VertexFormatElement.Type typeIn, VertexFormatElement.Usage usageIn, int count)
    {
        return VertexFormatElement.register(VertexFormatElement.getElementsCount(), indexIn, typeIn, usageIn, count, name, -1);
    }

    public static int removeExtendedElements(int maskElements)
    {
        int i = (VertexFormatElement.NORMAL.mask() << 1) - 1;
        return maskElements & i;
    }
}
