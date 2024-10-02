package net.optifine.shaders.gui;

import net.optifine.gui.GuiButtonOF;
import net.optifine.shaders.config.ShaderOption;
import net.optifine.shaders.config.ShaderOptionScreen;

public class GuiButtonShaderOption extends GuiButtonOF
{
    private ShaderOption shaderOption = null;

    public GuiButtonShaderOption(int buttonId, int x, int y, int widthIn, int heightIn, ShaderOption shaderOption, String text)
    {
        super(buttonId, x, y, widthIn, heightIn, text);
        this.shaderOption = shaderOption;
    }

    @Override
    protected boolean isValidClickButton(int p_isValidClickButton_1_)
    {
        return this.shaderOption instanceof ShaderOptionScreen ? p_isValidClickButton_1_ == 0 : true;
    }

    public ShaderOption getShaderOption()
    {
        return this.shaderOption;
    }

    public void valueChanged()
    {
    }

    public boolean isSwitchable()
    {
        return true;
    }
}
