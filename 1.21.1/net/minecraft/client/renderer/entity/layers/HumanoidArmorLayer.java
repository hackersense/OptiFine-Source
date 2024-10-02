package net.minecraft.client.renderer.entity.layers;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.component.DyedItemColor;
import net.optifine.Config;
import net.optifine.CustomItems;
import net.optifine.reflect.Reflector;
import net.optifine.util.TextureUtils;

public class HumanoidArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M>
{
    private static final Map<String, ResourceLocation> ARMOR_LOCATION_CACHE = Maps.newHashMap();
    private final A innerModel;
    private final A outerModel;
    private final TextureAtlas armorTrimAtlas;

    public HumanoidArmorLayer(RenderLayerParent<T, M> p_267286_, A p_267110_, A p_267150_, ModelManager p_267238_)
    {
        super(p_267286_);
        this.innerModel = p_267110_;
        this.outerModel = p_267150_;
        this.armorTrimAtlas = p_267238_.getAtlas(Sheets.ARMOR_TRIMS_SHEET);
    }

    public void render(
        PoseStack p_117096_,
        MultiBufferSource p_117097_,
        int p_117098_,
        T p_117099_,
        float p_117100_,
        float p_117101_,
        float p_117102_,
        float p_117103_,
        float p_117104_,
        float p_117105_
    )
    {
        this.renderArmorPiece(p_117096_, p_117097_, p_117099_, EquipmentSlot.CHEST, p_117098_, this.getArmorModel(EquipmentSlot.CHEST));
        this.renderArmorPiece(p_117096_, p_117097_, p_117099_, EquipmentSlot.LEGS, p_117098_, this.getArmorModel(EquipmentSlot.LEGS));
        this.renderArmorPiece(p_117096_, p_117097_, p_117099_, EquipmentSlot.FEET, p_117098_, this.getArmorModel(EquipmentSlot.FEET));
        this.renderArmorPiece(p_117096_, p_117097_, p_117099_, EquipmentSlot.HEAD, p_117098_, this.getArmorModel(EquipmentSlot.HEAD));
    }

    private void renderArmorPiece(PoseStack p_117119_, MultiBufferSource p_117120_, T p_117121_, EquipmentSlot p_117122_, int p_117123_, A p_117124_)
    {
        ItemStack itemstack = p_117121_.getItemBySlot(p_117122_);

        if (itemstack.getItem() instanceof ArmorItem armoritem && armoritem.getEquipmentSlot() == p_117122_)
        {
            this.getParentModel().copyPropertiesTo(p_117124_);
            this.setPartVisibility(p_117124_, p_117122_);
            Model model = this.getArmorModelHook(p_117121_, itemstack, p_117122_, p_117124_);
            boolean flag = this.usesInnerModel(p_117122_);
            ArmorMaterial armormaterial = armoritem.getMaterial().value();
            int i = itemstack.is(ItemTags.DYEABLE) ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(itemstack, -6265536)) : -1;

            for (ArmorMaterial.Layer armormaterial$layer : armormaterial.layers())
            {
                int j = armormaterial$layer.dyeable() ? i : -1;
                ResourceLocation resourcelocation = armormaterial$layer.texture(flag);

                if (Reflector.ForgeHooksClient_getArmorTexture.exists())
                {
                    resourcelocation = (ResourceLocation)Reflector.ForgeHooksClient_getArmorTexture
                                       .call(p_117121_, itemstack, p_117122_, armormaterial$layer, flag);
                }

                if (Config.isCustomItems())
                {
                    resourcelocation = CustomItems.getCustomArmorTexture(itemstack, p_117122_, armormaterial$layer.getSuffix(), resourcelocation);
                }

                this.renderModel(p_117119_, p_117120_, p_117123_, model, j, resourcelocation);
            }

            ArmorTrim armortrim = itemstack.get(DataComponents.TRIM);

            if (armortrim != null)
            {
                this.renderTrim(armoritem.getMaterial(), p_117119_, p_117120_, p_117123_, armortrim, model, flag);
            }

            if (itemstack.hasFoil())
            {
                this.renderGlint(p_117119_, p_117120_, p_117123_, model);
            }
        }
    }

    protected void setPartVisibility(A p_117126_, EquipmentSlot p_117127_)
    {
        p_117126_.setAllVisible(false);

        switch (p_117127_)
        {
            case HEAD:
                p_117126_.head.visible = true;
                p_117126_.hat.visible = true;
                break;

            case CHEST:
                p_117126_.body.visible = true;
                p_117126_.rightArm.visible = true;
                p_117126_.leftArm.visible = true;
                break;

            case LEGS:
                p_117126_.body.visible = true;
                p_117126_.rightLeg.visible = true;
                p_117126_.leftLeg.visible = true;
                break;

            case FEET:
                p_117126_.rightLeg.visible = true;
                p_117126_.leftLeg.visible = true;
        }
    }

    private void renderModel(PoseStack p_289664_, MultiBufferSource p_289689_, int p_289681_, A p_289658_, int p_342257_, ResourceLocation p_328978_)
    {
        this.renderModel(p_289664_, p_289689_, p_289681_, p_289658_, p_342257_, p_328978_);
    }

    private void renderModel(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, Model bipedModelIn, int colorIn, ResourceLocation suffixIn)
    {
        VertexConsumer vertexconsumer = bufferIn.getBuffer(RenderType.armorCutoutNoCull(suffixIn));
        bipedModelIn.renderToBuffer(matrixStackIn, vertexconsumer, packedLightIn, OverlayTexture.NO_OVERLAY, colorIn);
    }

    private void renderTrim(
        Holder<ArmorMaterial> p_331988_, PoseStack p_289687_, MultiBufferSource p_289643_, int p_289683_, ArmorTrim p_289692_, A p_289663_, boolean p_289651_
    )
    {
        this.renderTrim(p_331988_, p_289687_, p_289643_, p_289683_, p_289692_, p_289663_, p_289651_);
    }

    private void renderTrim(
        Holder<ArmorMaterial> armorMaterialIn,
        PoseStack matrixStackIn,
        MultiBufferSource bufferIn,
        int packedLightIn,
        ArmorTrim trimIn,
        Model bipedModelIn,
        boolean isLegSlot
    )
    {
        TextureAtlasSprite textureatlassprite = this.armorTrimAtlas.getSprite(isLegSlot ? trimIn.innerTexture(armorMaterialIn) : trimIn.outerTexture(armorMaterialIn));
        textureatlassprite = TextureUtils.getCustomSprite(textureatlassprite);
        VertexConsumer vertexconsumer = textureatlassprite.wrap(bufferIn.getBuffer(Sheets.armorTrimsSheet(trimIn.pattern().value().decal())));
        bipedModelIn.renderToBuffer(matrixStackIn, vertexconsumer, packedLightIn, OverlayTexture.NO_OVERLAY);
    }

    private void renderGlint(PoseStack p_289673_, MultiBufferSource p_289654_, int p_289649_, A p_289659_)
    {
        this.renderGlint(p_289673_, p_289654_, p_289649_, p_289659_);
    }

    private void renderGlint(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, Model bipedModelIn)
    {
        bipedModelIn.renderToBuffer(matrixStackIn, bufferIn.getBuffer(RenderType.armorEntityGlint()), packedLightIn, OverlayTexture.NO_OVERLAY);
    }

    private A getArmorModel(EquipmentSlot p_117079_)
    {
        return this.usesInnerModel(p_117079_) ? this.innerModel : this.outerModel;
    }

    private boolean usesInnerModel(EquipmentSlot p_117129_)
    {
        return p_117129_ == EquipmentSlot.LEGS;
    }

    protected Model getArmorModelHook(T entity, ItemStack itemStack, EquipmentSlot slot, A model)
    {
        return (Model)(Reflector.ForgeHooksClient_getArmorModel.exists()
                       ? (Model)Reflector.ForgeHooksClient_getArmorModel.call(entity, itemStack, slot, model)
                       : model);
    }
}
