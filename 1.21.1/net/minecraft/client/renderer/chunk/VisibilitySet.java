package net.minecraft.client.renderer.chunk;

import java.util.Set;
import net.minecraft.core.Direction;

public class VisibilitySet
{
    private static final int FACINGS = Direction.values().length;
    private long bits;

    public void add(Set<Direction> p_112991_)
    {
        for (Direction direction : p_112991_)
        {
            for (Direction direction1 : p_112991_)
            {
                this.set(direction, direction1, true);
            }
        }
    }

    public void set(Direction p_112987_, Direction p_112988_, boolean p_112989_)
    {
        this.setBit(p_112987_.ordinal() + p_112988_.ordinal() * FACINGS, p_112989_);
        this.setBit(p_112988_.ordinal() + p_112987_.ordinal() * FACINGS, p_112989_);
    }

    public void setAll(boolean p_112993_)
    {
        if (p_112993_)
        {
            this.bits = -1L;
        }
        else
        {
            this.bits = 0L;
        }
    }

    public boolean visibilityBetween(Direction p_112984_, Direction p_112985_)
    {
        return this.getBit(p_112984_.ordinal() + p_112985_.ordinal() * FACINGS);
    }

    @Override
    public String toString()
    {
        StringBuilder stringbuilder = new StringBuilder();
        stringbuilder.append(' ');

        for (Direction direction : Direction.values())
        {
            stringbuilder.append(' ').append(direction.toString().toUpperCase().charAt(0));
        }

        stringbuilder.append('\n');

        for (Direction direction2 : Direction.values())
        {
            stringbuilder.append(direction2.toString().toUpperCase().charAt(0));

            for (Direction direction1 : Direction.values())
            {
                if (direction2 == direction1)
                {
                    stringbuilder.append("  ");
                }
                else
                {
                    boolean flag = this.visibilityBetween(direction2, direction1);
                    stringbuilder.append(' ').append((char)(flag ? 'Y' : 'n'));
                }
            }

            stringbuilder.append('\n');
        }

        return stringbuilder.toString();
    }

    private boolean getBit(int i)
    {
        return (this.bits & 1L << i) != 0L;
    }

    private void setBit(int i, boolean on)
    {
        if (on)
        {
            this.setBit(i);
        }
        else
        {
            this.clearBit(i);
        }
    }

    private void setBit(int i)
    {
        this.bits |= 1L << i;
    }

    private void clearBit(int i)
    {
        this.bits &= ~(1L << i);
    }
}
