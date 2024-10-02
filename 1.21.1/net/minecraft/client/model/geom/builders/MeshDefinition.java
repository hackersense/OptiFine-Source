package net.minecraft.client.model.geom.builders;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.geom.PartPose;

public class MeshDefinition
{
    private final PartDefinition root = new PartDefinition(ImmutableList.of(), PartPose.ZERO);

    public PartDefinition getRoot()
    {
        this.root.setName("root");
        return this.root;
    }
}
