package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.optifine.Mipmaps;
import net.optifine.texture.IColorBlender;

public class MipmapGenerator
{
    private static final int ALPHA_CUTOUT_CUTOFF = 96;
    private static final float[] POW22 = Util.make(new float[256], floatsIn ->
    {
        for (int i = 0; i < floatsIn.length; i++)
        {
            floatsIn[i] = (float)Math.pow((double)((float)i / 255.0F), 2.2);
        }
    });

    private MipmapGenerator()
    {
    }

    public static NativeImage[] generateMipLevels(NativeImage[] p_251300_, int p_252326_)
    {
        return generateMipLevels(p_251300_, p_252326_, null);
    }

    public static NativeImage[] generateMipLevels(NativeImage[] imageIn, int mipmapLevelsIn, IColorBlender colorBlender)
    {
        if (mipmapLevelsIn + 1 <= imageIn.length)
        {
            return imageIn;
        }
        else
        {
            NativeImage[] anativeimage = new NativeImage[mipmapLevelsIn + 1];
            anativeimage[0] = imageIn[0];
            boolean flag = false;

            for (int i = 1; i <= mipmapLevelsIn; i++)
            {
                if (i < imageIn.length)
                {
                    anativeimage[i] = imageIn[i];
                }
                else
                {
                    NativeImage nativeimage = anativeimage[i - 1];
                    int j = Math.max(nativeimage.getWidth() >> 1, 1);
                    int k = Math.max(nativeimage.getHeight() >> 1, 1);
                    NativeImage nativeimage1 = new NativeImage(j, k, false);
                    int l = nativeimage1.getWidth();
                    int i1 = nativeimage1.getHeight();

                    for (int j1 = 0; j1 < l; j1++)
                    {
                        for (int k1 = 0; k1 < i1; k1++)
                        {
                            if (colorBlender != null)
                            {
                                nativeimage1.setPixelRGBA(
                                    j1,
                                    k1,
                                    colorBlender.blend(
                                        nativeimage.getPixelRGBA(j1 * 2 + 0, k1 * 2 + 0),
                                        nativeimage.getPixelRGBA(j1 * 2 + 1, k1 * 2 + 0),
                                        nativeimage.getPixelRGBA(j1 * 2 + 0, k1 * 2 + 1),
                                        nativeimage.getPixelRGBA(j1 * 2 + 1, k1 * 2 + 1)
                                    )
                                );
                            }
                            else
                            {
                                nativeimage1.setPixelRGBA(
                                    j1,
                                    k1,
                                    alphaBlend(
                                        nativeimage.getPixelRGBA(j1 * 2 + 0, k1 * 2 + 0),
                                        nativeimage.getPixelRGBA(j1 * 2 + 1, k1 * 2 + 0),
                                        nativeimage.getPixelRGBA(j1 * 2 + 0, k1 * 2 + 1),
                                        nativeimage.getPixelRGBA(j1 * 2 + 1, k1 * 2 + 1),
                                        flag
                                    )
                                );
                            }
                        }
                    }

                    anativeimage[i] = nativeimage1;
                }
            }

            return anativeimage;
        }
    }

    private static boolean hasTransparentPixel(NativeImage p_252279_)
    {
        for (int i = 0; i < p_252279_.getWidth(); i++)
        {
            for (int j = 0; j < p_252279_.getHeight(); j++)
            {
                if (p_252279_.getPixelRGBA(i, j) >> 24 == 0)
                {
                    return true;
                }
            }
        }

        return false;
    }

    private static int alphaBlend(int p_118049_, int p_118050_, int p_118051_, int p_118052_, boolean p_118053_)
    {
        return Mipmaps.alphaBlend(p_118049_, p_118050_, p_118051_, p_118052_);
    }

    private static int gammaBlend(int p_118043_, int p_118044_, int p_118045_, int p_118046_, int p_118047_)
    {
        float f = getPow22(p_118043_ >> p_118047_);
        float f1 = getPow22(p_118044_ >> p_118047_);
        float f2 = getPow22(p_118045_ >> p_118047_);
        float f3 = getPow22(p_118046_ >> p_118047_);
        float f4 = (float)((double)((float)Math.pow((double)(f + f1 + f2 + f3) * 0.25, 0.45454545454545453)));
        return (int)((double)f4 * 255.0);
    }

    private static float getPow22(int p_118041_)
    {
        return POW22[p_118041_ & 0xFF];
    }
}
