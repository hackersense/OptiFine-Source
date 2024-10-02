package net.optifine.shaders;

import java.io.InputStream;
import net.optifine.Config;

public class ShaderPackDefault implements IShaderPack
{
    @Override
    public void close()
    {
    }

    @Override
    public InputStream getResourceAsStream(String resName)
    {
        return Config.getOptiFineResourceStream(resName);
    }

    @Override
    public String getName()
    {
        return "(internal)";
    }

    @Override
    public boolean hasDirectory(String name)
    {
        return false;
    }
}
