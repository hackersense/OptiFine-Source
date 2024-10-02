package net.optifine.texture;

public class ColorBlenderSeparate implements IColorBlender
{
    private IBlender blenderR;
    private IBlender blenderG;
    private IBlender blenderB;
    private IBlender blenderA;

    public ColorBlenderSeparate(IBlender blenderR, IBlender blenderG, IBlender blenderB, IBlender blenderA)
    {
        this.blenderR = blenderR;
        this.blenderG = blenderG;
        this.blenderB = blenderB;
        this.blenderA = blenderA;
    }

    @Override
    public int blend(int c1, int c2, int c3, int c4)
    {
        int i = c1 >> 24 & 0xFF;
        int j = c1 >> 16 & 0xFF;
        int k = c1 >> 8 & 0xFF;
        int l = c1 & 0xFF;
        int i1 = c2 >> 24 & 0xFF;
        int j1 = c2 >> 16 & 0xFF;
        int k1 = c2 >> 8 & 0xFF;
        int l1 = c2 & 0xFF;
        int i2 = c3 >> 24 & 0xFF;
        int j2 = c3 >> 16 & 0xFF;
        int k2 = c3 >> 8 & 0xFF;
        int l2 = c3 & 0xFF;
        int i3 = c4 >> 24 & 0xFF;
        int j3 = c4 >> 16 & 0xFF;
        int k3 = c4 >> 8 & 0xFF;
        int l3 = c4 & 0xFF;
        int i4 = this.blenderA.blend(i, i1, i2, i3);
        int j4 = this.blenderR.blend(j, j1, j2, j3);
        int k4 = this.blenderG.blend(k, k1, k2, k3);
        int l4 = this.blenderB.blend(l, l1, l2, l3);
        return i4 << 24 | j4 << 16 | k4 << 8 | l4;
    }
}
