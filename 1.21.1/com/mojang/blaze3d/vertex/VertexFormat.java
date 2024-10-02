package com.mojang.blaze3d.vertex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraftforge.client.extensions.IForgeVertexFormat;

public class VertexFormat implements IForgeVertexFormat
{
    public static final int UNKNOWN_ELEMENT = -1;
    private List<VertexFormatElement> elements;
    private List<String> names;
    private int vertexSize;
    private int elementsMask;
    private int[] offsetsByElement = new int[32];
    @Nullable
    private VertexBuffer immediateDrawVertexBuffer;
    private String name;
    private int positionElementOffset = -1;
    private int normalElementOffset = -1;
    private int colorElementOffset = -1;
    private Int2IntMap uvOffsetsById = new Int2IntArrayMap();
    private ImmutableMap<String, VertexFormatElement> elementMapping;
    private boolean extended;

    VertexFormat(List<VertexFormatElement> p_343616_, List<String> p_345241_, IntList p_345522_, int p_344162_)
    {
        this.elements = p_343616_;
        this.names = p_345241_;
        this.vertexSize = p_344162_;
        this.elementsMask = p_343616_.stream().mapToInt(VertexFormatElement::mask).reduce(0, (val1, val2) -> val1 | val2);
        ImmutableMap.Builder<String, VertexFormatElement> builder = ImmutableMap.builder();

        for (int i = 0; i < p_343616_.size(); i++)
        {
            builder.put(p_345241_.get(i), p_343616_.get(i));
        }

        this.elementMapping = builder.buildOrThrow();

        for (int l = 0; l < this.offsetsByElement.length; l++)
        {
            VertexFormatElement vertexformatelement = VertexFormatElement.byId(l);
            int j = vertexformatelement != null ? p_343616_.indexOf(vertexformatelement) : -1;
            this.offsetsByElement[l] = j != -1 ? p_345522_.getInt(j) : -1;

            if (vertexformatelement != null)
            {
                VertexFormatElement.Usage vertexformatelement$usage = vertexformatelement.usage();
                int k = this.offsetsByElement[l];

                if (vertexformatelement$usage == VertexFormatElement.Usage.POSITION)
                {
                    this.positionElementOffset = k;
                }
                else if (vertexformatelement$usage == VertexFormatElement.Usage.NORMAL)
                {
                    this.normalElementOffset = k;
                }
                else if (vertexformatelement$usage == VertexFormatElement.Usage.COLOR)
                {
                    this.colorElementOffset = k;
                }
                else if (vertexformatelement$usage == VertexFormatElement.Usage.UV)
                {
                    this.uvOffsetsById.put(vertexformatelement.index(), k);
                }
            }
        }
    }

    public static VertexFormat.Builder builder()
    {
        return new VertexFormat.Builder();
    }

    @Override
    public String toString()
    {
        StringBuilder stringbuilder = new StringBuilder("Vertex format: " + this.name + " (").append(this.vertexSize).append(" bytes):\n");

        for (int i = 0; i < this.elements.size(); i++)
        {
            VertexFormatElement vertexformatelement = this.elements.get(i);
            stringbuilder.append(i)
            .append(". ")
            .append(this.names.get(i))
            .append(": ")
            .append(vertexformatelement)
            .append(" @ ")
            .append(this.getOffset(vertexformatelement))
            .append('\n');
        }

        return stringbuilder.toString();
    }

    public int getVertexSize()
    {
        return this.vertexSize;
    }

    public List<VertexFormatElement> getElements()
    {
        return this.elements;
    }

    public List<String> getElementAttributeNames()
    {
        return this.names;
    }

    public int[] getOffsetsByElement()
    {
        return this.offsetsByElement;
    }

    public int getOffset(VertexFormatElement p_342517_)
    {
        return this.offsetsByElement[p_342517_.id()];
    }

    public boolean contains(VertexFormatElement p_345196_)
    {
        return (this.elementsMask & p_345196_.mask()) != 0;
    }

    public int getElementsMask()
    {
        return this.elementsMask;
    }

    public String getElementName(VertexFormatElement p_345336_)
    {
        int i = this.elements.indexOf(p_345336_);

        if (i == -1)
        {
            throw new IllegalArgumentException(p_345336_ + " is not contained in format");
        }
        else
        {
            return this.names.get(i);
        }
    }

    @Override
    public boolean equals(Object p_86026_)
    {
        if (this == p_86026_)
        {
            return true;
        }
        else
        {
            if (p_86026_ instanceof VertexFormat vertexformat
                    && this.elementsMask == vertexformat.elementsMask
                    && this.vertexSize == vertexformat.vertexSize
                    && this.names.equals(vertexformat.names)
                    && Arrays.equals(this.offsetsByElement, vertexformat.offsetsByElement))
            {
                return true;
            }

            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return this.elementsMask * 31 + Arrays.hashCode(this.offsetsByElement);
    }

    public void setupBufferState()
    {
        if (!RenderSystem.isOnRenderThread())
        {
            RenderSystem.recordRenderCall(this::_setupBufferState);
        }
        else
        {
            this._setupBufferState();
        }
    }

    private void _setupBufferState()
    {
        int i = this.getVertexSize();

        for (int j = 0; j < this.elements.size(); j++)
        {
            VertexFormatElement vertexformatelement = this.elements.get(j);
            int k = vertexformatelement.getAttributeIndex();

            if (k >= 0)
            {
                GlStateManager._enableVertexAttribArray(k);
                vertexformatelement.setupBufferState(k, (long)this.getOffset(vertexformatelement), i);
            }
        }
    }

    public void clearBufferState()
    {
        if (!RenderSystem.isOnRenderThread())
        {
            RenderSystem.recordRenderCall(this::_clearBufferState);
        }
        else
        {
            this._clearBufferState();
        }
    }

    private void _clearBufferState()
    {
        for (int i = 0; i < this.elements.size(); i++)
        {
            VertexFormatElement vertexformatelement = this.elements.get(i);
            int j = vertexformatelement.getAttributeIndex();

            if (j >= 0)
            {
                GlStateManager._disableVertexAttribArray(j);
            }
        }
    }

    public VertexBuffer getImmediateDrawVertexBuffer()
    {
        VertexBuffer vertexbuffer = this.immediateDrawVertexBuffer;

        if (vertexbuffer == null)
        {
            this.immediateDrawVertexBuffer = vertexbuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }

        return vertexbuffer;
    }

    public int getOffset(int index)
    {
        return this.offsetsByElement[index];
    }

    public boolean hasPosition()
    {
        return this.positionElementOffset >= 0;
    }

    public int getPositionOffset()
    {
        return this.positionElementOffset;
    }

    public boolean hasNormal()
    {
        return this.normalElementOffset >= 0;
    }

    public int getNormalOffset()
    {
        return this.normalElementOffset;
    }

    public boolean hasColor()
    {
        return this.colorElementOffset >= 0;
    }

    public int getColorOffset()
    {
        return this.colorElementOffset;
    }

    public boolean hasUV(int id)
    {
        return this.uvOffsetsById.containsKey(id);
    }

    public int getUvOffsetById(int id)
    {
        return this.uvOffsetsById.get(id);
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void copyFrom(VertexFormat vf)
    {
        this.elements = vf.elements;
        this.names = vf.names;
        this.vertexSize = vf.vertexSize;
        this.elementsMask = vf.elementsMask;
        this.offsetsByElement = vf.offsetsByElement;
        this.immediateDrawVertexBuffer = vf.immediateDrawVertexBuffer;
        this.name = vf.name;
        this.positionElementOffset = vf.positionElementOffset;
        this.normalElementOffset = vf.normalElementOffset;
        this.colorElementOffset = vf.colorElementOffset;
        this.uvOffsetsById = vf.uvOffsetsById;
        this.elementMapping = vf.elementMapping;
        this.extended = vf.extended;
    }

    public VertexFormat duplicate()
    {
        VertexFormat.Builder vertexformat$builder = builder();
        vertexformat$builder.addAll(this);
        return vertexformat$builder.build();
    }

    public ImmutableMap<String, VertexFormatElement> getElementMapping()
    {
        return this.elementMapping;
    }

    public int getIntegerSize()
    {
        return this.getVertexSize() / 4;
    }

    public boolean isExtended()
    {
        return this.extended;
    }

    public void setExtended(boolean extended)
    {
        this.extended = extended;
    }

    public static class Builder
    {
        private final ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
        private final IntList offsets = new IntArrayList();
        private int offset;

        public VertexFormat.Builder add(String p_343401_, VertexFormatElement p_345244_)
        {
            this.elements.put(p_343401_, p_345244_);
            this.offsets.add(this.offset);
            this.offset = this.offset + p_345244_.byteSize();
            return this;
        }

        public VertexFormat.Builder padding(int p_345477_)
        {
            this.offset += p_345477_;
            return this;
        }

        public VertexFormat build()
        {
            ImmutableMap<String, VertexFormatElement> immutablemap = this.elements.buildOrThrow();
            ImmutableList<VertexFormatElement> immutablelist = immutablemap.values().asList();
            ImmutableList<String> immutablelist1 = immutablemap.keySet().asList();
            return new VertexFormat(immutablelist, immutablelist1, this.offsets, this.offset);
        }

        public VertexFormat.Builder addAll(VertexFormat vf)
        {
            for (VertexFormatElement vertexformatelement : vf.getElements())
            {
                String s = vf.getElementName(vertexformatelement);
                this.add(s, vertexformatelement);
            }

            while (this.offset < vf.getVertexSize())
            {
                this.padding(1);
            }

            return this;
        }
    }

    public static enum IndexType
    {
        SHORT(5123, 2),
        INT(5125, 4);

        public final int asGLType;
        public final int bytes;

        private IndexType(final int p_166930_, final int p_166931_)
        {
            this.asGLType = p_166930_;
            this.bytes = p_166931_;
        }

        public static VertexFormat.IndexType least(int p_166934_)
        {
            return (p_166934_ & -65536) != 0 ? INT : SHORT;
        }
    }

    public static enum Mode
    {
        LINES(4, 2, 2, false),
        LINE_STRIP(5, 2, 1, true),
        DEBUG_LINES(1, 2, 2, false),
        DEBUG_LINE_STRIP(3, 2, 1, true),
        TRIANGLES(4, 3, 3, false),
        TRIANGLE_STRIP(5, 3, 1, true),
        TRIANGLE_FAN(6, 3, 1, true),
        QUADS(4, 4, 4, false);

        public final int asGLMode;
        public final int primitiveLength;
        public final int primitiveStride;
        public final boolean connectedPrimitives;

        private Mode(final int p_231238_, final int p_231239_, final int p_231240_, final boolean p_231241_)
        {
            this.asGLMode = p_231238_;
            this.primitiveLength = p_231239_;
            this.primitiveStride = p_231240_;
            this.connectedPrimitives = p_231241_;
        }

        public int indexCount(int p_166959_)
        {

            return switch (this)
            {
                case LINES, QUADS -> p_166959_ / 4 * 6;

                case LINE_STRIP, DEBUG_LINES, DEBUG_LINE_STRIP, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> p_166959_;

                default -> 0;
            };
        }
    }
}
