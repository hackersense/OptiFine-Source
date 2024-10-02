package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;

public interface ArmedModel
{
    void translateToHand(HumanoidArm p_102108_, PoseStack p_102109_);
}
