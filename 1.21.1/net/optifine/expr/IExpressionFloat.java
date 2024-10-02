package net.optifine.expr;

public interface IExpressionFloat extends IExpression
{
    float eval();

    @Override

default ExpressionType getExpressionType()
    {
        return ExpressionType.FLOAT;
    }
}
