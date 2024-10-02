package net.optifine.shaders;

import net.optifine.texture.ColorBlenderLabPbrSpecular;
import net.optifine.texture.ColorBlenderLinear;
import net.optifine.texture.IColorBlender;

public class TextureFormatLabPbr implements ITextureFormat
{
    private String version;

    public TextureFormatLabPbr(String ver)
    {
        this.version = ver;
    }

    @Override
    public String getMacroName()
    {
        return "LAB_PBR";
    }

    @Override
    public String getMacroVersion()
    {
        return this.version == null ? null : this.version.replace('.', '_');
    }

    @Override
    public IColorBlender getColorBlender(ShadersTextureType typeIn)
    {
        return (IColorBlender)(typeIn == ShadersTextureType.SPECULAR ? new ColorBlenderLabPbrSpecular() : new ColorBlenderLinear());
    }

    @Override
    public boolean isTextureBlend(ShadersTextureType typeIn)
    {
        return typeIn != ShadersTextureType.SPECULAR;
    }
}
