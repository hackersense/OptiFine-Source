package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.optifine.entity.model.IEntityRenderer;
import net.optifine.util.Either;

public interface BlockEntityRenderer<T extends BlockEntity> extends IEntityRenderer
{
    IdentityHashMap<BlockEntityRenderer, BlockEntityType> CACHED_TYPES = new IdentityHashMap<>();

    void render(T p_112307_, float p_112308_, PoseStack p_112309_, MultiBufferSource p_112310_, int p_112311_, int p_112312_);

default boolean shouldRenderOffScreen(T p_112306_)
    {
        return false;
    }

default int getViewDistance()
    {
        return 64;
    }

default boolean shouldRender(T p_173568_, Vec3 p_173569_)
    {
        return Vec3.atCenterOf(p_173568_.getBlockPos()).closerThan(p_173569_, (double)this.getViewDistance());
    }

    @Override

default Either<EntityType, BlockEntityType> getType()
    {
        BlockEntityType blockentitytype = CACHED_TYPES.get(this);
        return blockentitytype == null ? null : Either.makeRight(blockentitytype);
    }

    @Override

default void setType(Either<EntityType, BlockEntityType> type)
    {
        CACHED_TYPES.put(this, type.getRight().get());
    }

    @Override

default ResourceLocation getLocationTextureCustom()
    {
        return null;
    }

    @Override

default void setLocationTextureCustom(ResourceLocation locationTextureCustom)
    {
    }
}
