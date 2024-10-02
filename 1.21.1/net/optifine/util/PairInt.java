package net.optifine.util;

public class PairInt
{
    private int left;
    private int right;
    private final int hashCode;

    public PairInt(int left, int right)
    {
        this.left = left;
        this.right = right;
        this.hashCode = left + 37 * right;
    }

    public static PairInt of(int left, int right)
    {
        return new PairInt(left, right);
    }

    public int getLeft()
    {
        return this.left;
    }

    public int getRight()
    {
        return this.right;
    }

    @Override
    public int hashCode()
    {
        return this.hashCode;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        else
        {
            return !(obj instanceof PairInt pairint) ? false : this.left == pairint.left && this.right == pairint.right;
        }
    }

    @Override
    public String toString()
    {
        return "(" + this.left + ", " + this.right + ")";
    }
}
