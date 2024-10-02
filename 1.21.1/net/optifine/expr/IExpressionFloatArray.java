package net.optifine.expr;

public interface IExpressionFloatArray extends IExpression
{
    float[] eval();

    @Override

default ExpressionType getExpressionType()
    {
        return ExpressionType.FLOAT_ARRAY;
    }
}
