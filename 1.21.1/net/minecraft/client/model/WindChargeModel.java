package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;

public class WindChargeModel extends HierarchicalModel<AbstractWindCharge>
{
    private static final int ROTATION_SPEED = 16;
    private final ModelPart bone;
    private final ModelPart windCharge;
    private final ModelPart wind;

    public WindChargeModel(ModelPart p_309708_)
    {
        super(RenderType::entityTranslucent);
        this.bone = p_309708_.getChild("bone");
        this.wind = this.bone.getChild("wind");
        this.windCharge = this.bone.getChild("wind_charge");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        partdefinition1.addOrReplaceChild(
            "wind",
            CubeListBuilder.create()
            .texOffs(15, 20)
            .addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(0, 9)
            .addBox(-3.0F, -2.0F, -3.0F, 6.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F)
        );
        partdefinition1.addOrReplaceChild(
            "wind_charge",
            CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    public void setupAnim(AbstractWindCharge p_328328_, float p_309974_, float p_310519_, float p_311366_, float p_312503_, float p_311700_)
    {
        this.windCharge.yRot = -p_311366_ * 16.0F * (float)(Math.PI / 180.0);
        this.wind.yRot = p_311366_ * 16.0F * (float)(Math.PI / 180.0);
    }

    @Override
    public ModelPart root()
    {
        return this.bone;
    }
}
