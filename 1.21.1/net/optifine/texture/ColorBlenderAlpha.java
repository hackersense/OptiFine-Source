package net.optifine.texture;

public class ColorBlenderAlpha implements IColorBlender
{
    private int alphaMul;
    private int alphaDiv;

    public ColorBlenderAlpha()
    {
        this(1, 2);
    }

    public ColorBlenderAlpha(int alphaMul, int alphaDiv)
    {
        this.alphaMul = alphaMul;
        this.alphaDiv = alphaDiv;
    }

    @Override
    public int blend(int col1, int col2, int col3, int col4)
    {
        int i = this.alphaBlend(col1, col2);
        int j = this.alphaBlend(col3, col4);
        return this.alphaBlend(i, j);
    }

    private int alphaBlend(int c1, int c2)
    {
        int i = (c1 & 0xFF000000) >> 24 & 0xFF;
        int j = (c2 & 0xFF000000) >> 24 & 0xFF;
        int k = (i + j) / 2;

        if (i == 0 && j == 0)
        {
            i = 1;
            j = 1;
        }
        else
        {
            if (i == 0)
            {
                c1 = c2;
                k = k * this.alphaMul / this.alphaDiv;
            }

            if (j == 0)
            {
                c2 = c1;
                k = k * this.alphaMul / this.alphaDiv;
            }
        }

        int l = (c1 >> 16 & 0xFF) * i;
        int i1 = (c1 >> 8 & 0xFF) * i;
        int j1 = (c1 & 0xFF) * i;
        int k1 = (c2 >> 16 & 0xFF) * j;
        int l1 = (c2 >> 8 & 0xFF) * j;
        int i2 = (c2 & 0xFF) * j;
        int j2 = (l + k1) / (i + j);
        int k2 = (i1 + l1) / (i + j);
        int l2 = (j1 + i2) / (i + j);
        return k << 24 | j2 << 16 | k2 << 8 | l2;
    }
}
