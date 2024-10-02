package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;

public abstract class ColorableHierarchicalModel<E extends Entity> extends HierarchicalModel<E>
{
    private int color = -1;

    public void setColor(int p_344057_)
    {
        this.color = p_344057_;
    }

    @Override
    public void renderToBuffer(PoseStack p_170506_, VertexConsumer p_170507_, int p_170508_, int p_170509_, int p_342498_)
    {
        super.renderToBuffer(p_170506_, p_170507_, p_170508_, p_170509_, FastColor.ARGB32.multiply(p_342498_, this.color));
    }
}
