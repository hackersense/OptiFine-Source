package net.optifine.entity.model.anim;

import net.minecraft.client.model.geom.ModelPart;
import net.optifine.expr.IExpressionBool;

public class ModelVariableBool implements IExpressionBool, IModelVariableBool, IModelRendererVariable
{
    private String name;
    private ModelPart modelRenderer;
    private ModelVariableType enumModelVariable;

    public ModelVariableBool(String name, ModelPart modelRenderer, ModelVariableType enumModelVariable)
    {
        this.name = name;
        this.modelRenderer = modelRenderer;
        this.enumModelVariable = enumModelVariable;
    }

    @Override
    public ModelPart getModelRenderer()
    {
        return this.modelRenderer;
    }

    @Override
    public boolean eval()
    {
        return this.getValue();
    }

    @Override
    public boolean getValue()
    {
        return this.enumModelVariable.getBool(this.modelRenderer);
    }

    @Override
    public void setValue(boolean value)
    {
        this.enumModelVariable.setBool(this.modelRenderer, value);
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}
