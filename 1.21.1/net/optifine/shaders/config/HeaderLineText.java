package net.optifine.shaders.config;

public class HeaderLineText extends HeaderLine
{
    private String text;

    public HeaderLineText(String text)
    {
        this.text = text;
    }

    @Override
    public String getText()
    {
        return this.text;
    }

    @Override
    public boolean matches(String line)
    {
        return line.equals(this.text);
    }

    @Override
    public String removeFrom(String line)
    {
        return null;
    }
}
