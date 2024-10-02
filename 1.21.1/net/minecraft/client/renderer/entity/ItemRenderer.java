package net.minecraft.client.renderer.entity;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.MatrixUtil;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.CustomItems;
import net.optifine.EmissiveTextures;
import net.optifine.reflect.Reflector;
import net.optifine.render.VertexBuilderWrapper;
import net.optifine.shaders.Shaders;
import net.optifine.util.SingleIterable;

public class ItemRenderer implements ResourceManagerReloadListener
{
    public static final ResourceLocation ENCHANTED_GLINT_ENTITY = ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_entity.png");
    public static final ResourceLocation ENCHANTED_GLINT_ITEM = ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_item.png");
    private static final Set<Item> IGNORED = Sets.newHashSet(Items.AIR);
    public static final int GUI_SLOT_CENTER_X = 8;
    public static final int GUI_SLOT_CENTER_Y = 8;
    public static final int ITEM_COUNT_BLIT_OFFSET = 200;
    public static final float COMPASS_FOIL_UI_SCALE = 0.5F;
    public static final float COMPASS_FOIL_FIRST_PERSON_SCALE = 0.75F;
    public static final float COMPASS_FOIL_TEXTURE_SCALE = 0.0078125F;
    private static final ModelResourceLocation TRIDENT_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("trident"));
    public static final ModelResourceLocation TRIDENT_IN_HAND_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("trident_in_hand"));
    private static final ModelResourceLocation SPYGLASS_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("spyglass"));
    public static final ModelResourceLocation SPYGLASS_IN_HAND_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("spyglass_in_hand"));
    private final Minecraft minecraft;
    private final ItemModelShaper itemModelShaper;
    private final TextureManager textureManager;
    private final ItemColors itemColors;
    private final BlockEntityWithoutLevelRenderer blockEntityRenderer;
    public ModelManager modelManager = null;
    private static boolean renderItemGui = false;

    public ItemRenderer(Minecraft p_266926_, TextureManager p_266774_, ModelManager p_266850_, ItemColors p_267016_, BlockEntityWithoutLevelRenderer p_267049_)
    {
        this.minecraft = p_266926_;
        this.textureManager = p_266774_;
        this.modelManager = p_266850_;

        if (Reflector.ForgeItemModelShaper_Constructor.exists())
        {
            this.itemModelShaper = (ItemModelShaper)Reflector.newInstance(Reflector.ForgeItemModelShaper_Constructor, this.modelManager);
        }
        else
        {
            this.itemModelShaper = new ItemModelShaper(p_266850_);
        }

        this.blockEntityRenderer = p_267049_;

        for (Item item : BuiltInRegistries.ITEM)
        {
            if (!IGNORED.contains(item))
            {
                this.itemModelShaper.register(item, ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item)));
            }
        }

        this.itemColors = p_267016_;
    }

    public ItemModelShaper getItemModelShaper()
    {
        return this.itemModelShaper;
    }

    public void renderModelLists(BakedModel p_115190_, ItemStack p_115191_, int p_115192_, int p_115193_, PoseStack p_115194_, VertexConsumer p_115195_)
    {
        RandomSource randomsource = RandomSource.create();
        long i = 42L;

        for (Direction direction : Direction.VALUES)
        {
            randomsource.setSeed(42L);
            this.renderQuadList(p_115194_, p_115195_, p_115190_.getQuads(null, direction, randomsource), p_115191_, p_115192_, p_115193_);
        }

        randomsource.setSeed(42L);
        this.renderQuadList(p_115194_, p_115195_, p_115190_.getQuads(null, null, randomsource), p_115191_, p_115192_, p_115193_);
    }

    public void render(
        ItemStack p_115144_,
        ItemDisplayContext p_270188_,
        boolean p_115146_,
        PoseStack p_115147_,
        MultiBufferSource p_115148_,
        int p_115149_,
        int p_115150_,
        BakedModel p_115151_
    )
    {
        if (!p_115144_.isEmpty())
        {
            p_115147_.pushPose();
            boolean flag = p_270188_ == ItemDisplayContext.GUI || p_270188_ == ItemDisplayContext.GROUND || p_270188_ == ItemDisplayContext.FIXED;

            if (flag)
            {
                if (p_115144_.is(Items.TRIDENT))
                {
                    p_115151_ = this.itemModelShaper.getModelManager().getModel(TRIDENT_MODEL);
                }
                else if (p_115144_.is(Items.SPYGLASS))
                {
                    p_115151_ = this.itemModelShaper.getModelManager().getModel(SPYGLASS_MODEL);
                }
            }

            if (Reflector.ForgeHooksClient.exists())
            {
                p_115151_ = p_115151_.applyTransform(p_270188_, p_115147_, p_115146_);
            }
            else
            {
                p_115151_.getTransforms().getTransform(p_270188_).apply(p_115146_, p_115147_);
            }

            p_115147_.translate(-0.5F, -0.5F, -0.5F);

            if (!p_115151_.isCustomRenderer() && (!p_115144_.is(Items.TRIDENT) || flag))
            {
                boolean flag1;

                if (p_270188_ != ItemDisplayContext.GUI && !p_270188_.firstPerson() && p_115144_.getItem() instanceof BlockItem blockitem)
                {
                    Block block = blockitem.getBlock();
                    flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
                }
                else
                {
                    flag1 = true;
                }

                boolean flag2 = Reflector.ForgeHooksClient.exists();
                Iterable<BakedModel> iterable1 = (Iterable<BakedModel>)(flag2 ? p_115151_.getRenderPasses(p_115144_, flag1) : new SingleIterable<>(p_115151_));
                Iterable<RenderType> iterable = (Iterable<RenderType>)(flag2
                                                ? p_115151_.getRenderTypes(p_115144_, flag1)
                                                : new SingleIterable<>(ItemBlockRenderTypes.getRenderType(p_115144_, flag1)));

                for (BakedModel bakedmodel : iterable1)
                {
                    p_115151_ = bakedmodel;

                    for (RenderType rendertype : iterable)
                    {
                        VertexConsumer vertexconsumer;

                        if (hasAnimatedTexture(p_115144_) && p_115144_.hasFoil())
                        {
                            PoseStack.Pose posestack$pose = p_115147_.last().copy();

                            if (p_270188_ == ItemDisplayContext.GUI)
                            {
                                MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.5F);
                            }
                            else if (p_270188_.firstPerson())
                            {
                                MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.75F);
                            }

                            vertexconsumer = getCompassFoilBuffer(p_115148_, rendertype, posestack$pose);
                        }
                        else if (flag1)
                        {
                            vertexconsumer = getFoilBufferDirect(p_115148_, rendertype, true, p_115144_.hasFoil());
                        }
                        else
                        {
                            vertexconsumer = getFoilBuffer(p_115148_, rendertype, true, p_115144_.hasFoil());
                        }

                        if (Config.isCustomItems())
                        {
                            p_115151_ = CustomItems.getCustomItemModel(p_115144_, p_115151_, ItemOverrides.lastModelLocation, false);
                            ItemOverrides.lastModelLocation = null;
                        }

                        if (EmissiveTextures.isActive())
                        {
                            EmissiveTextures.beginRender();
                        }

                        this.renderModelLists(p_115151_, p_115144_, p_115149_, p_115150_, p_115147_, vertexconsumer);

                        if (EmissiveTextures.isActive())
                        {
                            if (EmissiveTextures.hasEmissive())
                            {
                                EmissiveTextures.beginRenderEmissive();
                                VertexConsumer vertexconsumer1 = vertexconsumer instanceof VertexBuilderWrapper
                                                                 ? ((VertexBuilderWrapper)vertexconsumer).getVertexBuilder()
                                                                 : vertexconsumer;
                                this.renderModelLists(p_115151_, p_115144_, LightTexture.MAX_BRIGHTNESS, p_115150_, p_115147_, vertexconsumer1);
                                EmissiveTextures.endRenderEmissive();
                            }

                            EmissiveTextures.endRender();
                        }
                    }
                }
            }
            else if (Reflector.MinecraftForge.exists())
            {
                IClientItemExtensions.of(p_115144_).getCustomRenderer().renderByItem(p_115144_, p_270188_, p_115147_, p_115148_, p_115149_, p_115150_);
            }
            else
            {
                this.blockEntityRenderer.renderByItem(p_115144_, p_270188_, p_115147_, p_115148_, p_115149_, p_115150_);
            }

            p_115147_.popPose();
        }
    }

    private static boolean hasAnimatedTexture(ItemStack p_286353_)
    {
        return p_286353_.is(ItemTags.COMPASSES) || p_286353_.is(Items.CLOCK);
    }

    public static VertexConsumer getArmorFoilBuffer(MultiBufferSource p_115185_, RenderType p_115186_, boolean p_115187_)
    {
        if (Shaders.isShadowPass)
        {
            p_115187_ = false;
        }

        if (EmissiveTextures.isRenderEmissive())
        {
            p_115187_ = false;
        }

        return p_115187_ ? VertexMultiConsumer.create(p_115185_.getBuffer(RenderType.armorEntityGlint()), p_115185_.getBuffer(p_115186_)) : p_115185_.getBuffer(p_115186_);
    }

    public static VertexConsumer getCompassFoilBuffer(MultiBufferSource p_115181_, RenderType p_115182_, PoseStack.Pose p_115183_)
    {
        return VertexMultiConsumer.create(
                   new SheetedDecalTextureGenerator(p_115181_.getBuffer(RenderType.glint()), p_115183_, 0.0078125F), p_115181_.getBuffer(p_115182_)
               );
    }

    public static VertexConsumer getFoilBuffer(MultiBufferSource p_115212_, RenderType p_115213_, boolean p_115214_, boolean p_115215_)
    {
        if (Shaders.isShadowPass)
        {
            p_115215_ = false;
        }

        if (EmissiveTextures.isRenderEmissive())
        {
            p_115215_ = false;
        }

        if (!p_115215_)
        {
            return p_115212_.getBuffer(p_115213_);
        }
        else
        {
            return Minecraft.useShaderTransparency() && p_115213_ == Sheets.translucentItemSheet()
                   ? VertexMultiConsumer.create(p_115212_.getBuffer(RenderType.glintTranslucent()), p_115212_.getBuffer(p_115213_))
                   : VertexMultiConsumer.create(p_115212_.getBuffer(p_115214_ ? RenderType.glint() : RenderType.entityGlint()), p_115212_.getBuffer(p_115213_));
        }
    }

    public static VertexConsumer getFoilBufferDirect(MultiBufferSource p_115223_, RenderType p_115224_, boolean p_115225_, boolean p_115226_)
    {
        if (Shaders.isShadowPass)
        {
            p_115226_ = false;
        }

        if (EmissiveTextures.isRenderEmissive())
        {
            p_115226_ = false;
        }

        return p_115226_
               ? VertexMultiConsumer.create(p_115223_.getBuffer(p_115225_ ? RenderType.glint() : RenderType.entityGlintDirect()), p_115223_.getBuffer(p_115224_))
               : p_115223_.getBuffer(p_115224_);
    }

    private void renderQuadList(PoseStack p_115163_, VertexConsumer p_115164_, List<BakedQuad> p_115165_, ItemStack p_115166_, int p_115167_, int p_115168_)
    {
        boolean flag = !p_115166_.isEmpty();
        PoseStack.Pose posestack$pose = p_115163_.last();
        boolean flag1 = EmissiveTextures.isActive();
        int i = p_115165_.size();
        int j = i > 0 && Config.isCustomColors() ? CustomColors.getColorFromItemStack(p_115166_, -1, -1) : -1;

        for (int k = 0; k < i; k++)
        {
            BakedQuad bakedquad = p_115165_.get(k);

            if (flag1)
            {
                bakedquad = EmissiveTextures.getEmissiveQuad(bakedquad);

                if (bakedquad == null)
                {
                    continue;
                }
            }

            int l = j;

            if (flag && bakedquad.isTinted())
            {
                l = this.itemColors.getColor(p_115166_, bakedquad.getTintIndex());

                if (Config.isCustomColors())
                {
                    l = CustomColors.getColorFromItemStack(p_115166_, bakedquad.getTintIndex(), l);
                }
            }

            float f = (float)FastColor.ARGB32.alpha(l) / 255.0F;
            float f1 = (float)FastColor.ARGB32.red(l) / 255.0F;
            float f2 = (float)FastColor.ARGB32.green(l) / 255.0F;
            float f3 = (float)FastColor.ARGB32.blue(l) / 255.0F;

            if (Reflector.ForgeHooksClient.exists())
            {
                p_115164_.putBulkData(posestack$pose, bakedquad, f1, f2, f3, f, p_115167_, p_115168_, true);
            }
            else
            {
                p_115164_.putBulkData(posestack$pose, bakedquad, f1, f2, f3, f, p_115167_, p_115168_);
            }
        }
    }

    public BakedModel getModel(ItemStack p_174265_, @Nullable Level p_174266_, @Nullable LivingEntity p_174267_, int p_174268_)
    {
        BakedModel bakedmodel;

        if (p_174265_.is(Items.TRIDENT))
        {
            bakedmodel = this.itemModelShaper.getModelManager().getModel(TRIDENT_IN_HAND_MODEL);
        }
        else if (p_174265_.is(Items.SPYGLASS))
        {
            bakedmodel = this.itemModelShaper.getModelManager().getModel(SPYGLASS_IN_HAND_MODEL);
        }
        else
        {
            bakedmodel = this.itemModelShaper.getItemModel(p_174265_);
        }

        ClientLevel clientlevel = p_174266_ instanceof ClientLevel ? (ClientLevel)p_174266_ : null;
        ItemOverrides.lastModelLocation = null;
        BakedModel bakedmodel1 = bakedmodel.getOverrides().resolve(bakedmodel, p_174265_, clientlevel, p_174267_, p_174268_);

        if (Config.isCustomItems())
        {
            bakedmodel1 = CustomItems.getCustomItemModel(p_174265_, bakedmodel1, ItemOverrides.lastModelLocation, true);
        }

        return bakedmodel1 == null ? this.itemModelShaper.getModelManager().getMissingModel() : bakedmodel1;
    }

    public void renderStatic(
        ItemStack p_270761_,
        ItemDisplayContext p_270648_,
        int p_270410_,
        int p_270894_,
        PoseStack p_270430_,
        MultiBufferSource p_270457_,
        @Nullable Level p_270149_,
        int p_270509_
    )
    {
        this.renderStatic(null, p_270761_, p_270648_, false, p_270430_, p_270457_, p_270149_, p_270410_, p_270894_, p_270509_);
    }

    public void renderStatic(
        @Nullable LivingEntity p_270101_,
        ItemStack p_270637_,
        ItemDisplayContext p_270437_,
        boolean p_270434_,
        PoseStack p_270230_,
        MultiBufferSource p_270411_,
        @Nullable Level p_270641_,
        int p_270595_,
        int p_270927_,
        int p_270845_
    )
    {
        if (!p_270637_.isEmpty())
        {
            BakedModel bakedmodel = this.getModel(p_270637_, p_270641_, p_270101_, p_270845_);
            this.render(p_270637_, p_270437_, p_270434_, p_270230_, p_270411_, p_270595_, p_270927_, bakedmodel);
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager p_115105_)
    {
        this.itemModelShaper.rebuildCache();
    }

    public static boolean isRenderItemGui()
    {
        return renderItemGui;
    }

    public static void setRenderItemGui(boolean renderItemGui)
    {
        ItemRenderer.renderItemGui = renderItemGui;
    }

    public BlockEntityWithoutLevelRenderer getBlockEntityRenderer()
    {
        return this.blockEntityRenderer;
    }
}
