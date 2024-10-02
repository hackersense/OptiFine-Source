package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.optifine.util.Either;

public class VirtualEntityRenderer implements IEntityRenderer
{
    private Model model;
    private Either<EntityType, BlockEntityType> type;
    private ResourceLocation locationTextureCustom;

    public VirtualEntityRenderer(Model model)
    {
        this.model = model;
    }

    public Model getModel()
    {
        return this.model;
    }

    @Override
    public Either<EntityType, BlockEntityType> getType()
    {
        return this.type;
    }

    @Override
    public void setType(Either<EntityType, BlockEntityType> type)
    {
        this.type = type;
    }

    @Override
    public ResourceLocation getLocationTextureCustom()
    {
        return this.locationTextureCustom;
    }

    @Override
    public void setLocationTextureCustom(ResourceLocation locationTextureCustom)
    {
        this.locationTextureCustom = locationTextureCustom;
    }
}
