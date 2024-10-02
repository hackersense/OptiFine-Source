package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public abstract class ListModel<E extends Entity> extends EntityModel<E>
{
    public ListModel()
    {
        this(RenderType::entityCutoutNoCull);
    }

    public ListModel(Function<ResourceLocation, RenderType> p_103011_)
    {
        super(p_103011_);
    }

    @Override
    public void renderToBuffer(PoseStack p_103013_, VertexConsumer p_103014_, int p_103015_, int p_103016_, int p_343152_)
    {
        this.parts().forEach(p_340858_ -> p_340858_.render(p_103013_, p_103014_, p_103015_, p_103016_, p_343152_));
    }

    public abstract Iterable<ModelPart> parts();
}
