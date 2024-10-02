package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public class HorseArmorLayer extends RenderLayer<Horse, HorseModel<Horse>>
{
    public HorseModel<Horse> model;
    public ResourceLocation customTextureLocation;

    public HorseArmorLayer(RenderLayerParent<Horse, HorseModel<Horse>> p_174496_, EntityModelSet p_174497_)
    {
        super(p_174496_);
        this.model = new HorseModel<>(p_174497_.bakeLayer(ModelLayers.HORSE_ARMOR));
    }

    public void render(
        PoseStack p_117032_,
        MultiBufferSource p_117033_,
        int p_117034_,
        Horse p_117035_,
        float p_117036_,
        float p_117037_,
        float p_117038_,
        float p_117039_,
        float p_117040_,
        float p_117041_
    )
    {
        ItemStack itemstack = p_117035_.getBodyArmorItem();

        if (itemstack.getItem() instanceof AnimalArmorItem animalarmoritem && animalarmoritem.getBodyType() == AnimalArmorItem.BodyType.EQUESTRIAN)
        {
            this.getParentModel().copyPropertiesTo(this.model);
            this.model.prepareMobModel(p_117035_, p_117036_, p_117037_, p_117038_);
            this.model.setupAnim(p_117035_, p_117036_, p_117037_, p_117039_, p_117040_, p_117041_);
            int i;

            if (itemstack.is(ItemTags.DYEABLE))
            {
                i = FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(itemstack, -6265536));
            }
            else
            {
                i = -1;
            }

            ResourceLocation resourcelocation = this.customTextureLocation != null ? this.customTextureLocation : animalarmoritem.getTexture();
            VertexConsumer vertexconsumer = p_117033_.getBuffer(RenderType.entityCutoutNoCull(resourcelocation));
            this.model.renderToBuffer(p_117032_, vertexconsumer, p_117034_, OverlayTexture.NO_OVERLAY, i);
            return;
        }
    }
}
