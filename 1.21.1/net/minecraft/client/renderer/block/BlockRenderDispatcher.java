package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.model.data.ModelData;
import net.optifine.reflect.Reflector;

public class BlockRenderDispatcher implements ResourceManagerReloadListener
{
    private final BlockModelShaper blockModelShaper;
    private final ModelBlockRenderer modelRenderer;
    private final BlockEntityWithoutLevelRenderer blockEntityRenderer;
    private final LiquidBlockRenderer liquidBlockRenderer;
    private final RandomSource random = RandomSource.create();
    private final BlockColors blockColors;

    public BlockRenderDispatcher(BlockModelShaper p_173399_, BlockEntityWithoutLevelRenderer p_173400_, BlockColors p_173401_)
    {
        this.blockModelShaper = p_173399_;
        this.blockEntityRenderer = p_173400_;
        this.blockColors = p_173401_;

        if (Reflector.ForgeModelBlockRenderer_Constructor.exists())
        {
            this.modelRenderer = (ModelBlockRenderer)Reflector.newInstance(Reflector.ForgeModelBlockRenderer_Constructor, this.blockColors);
        }
        else
        {
            this.modelRenderer = new ModelBlockRenderer(this.blockColors);
        }

        this.liquidBlockRenderer = new LiquidBlockRenderer();
    }

    public BlockModelShaper getBlockModelShaper()
    {
        return this.blockModelShaper;
    }

    public void renderBreakingTexture(BlockState p_110919_, BlockPos p_110920_, BlockAndTintGetter p_110921_, PoseStack p_110922_, VertexConsumer p_110923_)
    {
        this.renderBreakingTexture(p_110919_, p_110920_, p_110921_, p_110922_, p_110923_, ModelData.EMPTY);
    }

    public void renderBreakingTexture(
        BlockState blockStateIn, BlockPos posIn, BlockAndTintGetter lightReaderIn, PoseStack matrixStackIn, VertexConsumer vertexBuilderIn, ModelData modelData
    )
    {
        if (blockStateIn.getRenderShape() == RenderShape.MODEL)
        {
            BakedModel bakedmodel = this.blockModelShaper.getBlockModel(blockStateIn);
            long i = blockStateIn.getSeed(posIn);
            this.modelRenderer
            .tesselateBlock(
                lightReaderIn,
                bakedmodel,
                blockStateIn,
                posIn,
                matrixStackIn,
                vertexBuilderIn,
                true,
                this.random,
                i,
                OverlayTexture.NO_OVERLAY,
                modelData,
                null
            );
        }
    }

    public void renderBatched(
        BlockState p_234356_,
        BlockPos p_234357_,
        BlockAndTintGetter p_234358_,
        PoseStack p_234359_,
        VertexConsumer p_234360_,
        boolean p_234361_,
        RandomSource p_234362_
    )
    {
        this.renderBatched(p_234356_, p_234357_, p_234358_, p_234359_, p_234360_, p_234361_, p_234362_, ModelData.EMPTY, null);
    }

    public void renderBatched(
        BlockState blockStateIn,
        BlockPos posIn,
        BlockAndTintGetter lightReaderIn,
        PoseStack matrixStackIn,
        VertexConsumer vertexBuilderIn,
        boolean checkSides,
        RandomSource rand,
        ModelData modelData,
        RenderType renderType
    )
    {
        try
        {
            this.modelRenderer
            .tesselateBlock(
                lightReaderIn,
                this.getBlockModel(blockStateIn),
                blockStateIn,
                posIn,
                matrixStackIn,
                vertexBuilderIn,
                checkSides,
                rand,
                blockStateIn.getSeed(posIn),
                OverlayTexture.NO_OVERLAY,
                modelData,
                renderType
            );
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, lightReaderIn, posIn, blockStateIn);
            throw new ReportedException(crashreport);
        }
    }

    public void renderLiquid(BlockPos p_234364_, BlockAndTintGetter p_234365_, VertexConsumer p_234366_, BlockState p_234367_, FluidState p_234368_)
    {
        try
        {
            this.liquidBlockRenderer.tesselate(p_234365_, p_234364_, p_234366_, p_234367_, p_234368_);
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating liquid in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, p_234365_, p_234364_, null);
            throw new ReportedException(crashreport);
        }
    }

    public ModelBlockRenderer getModelRenderer()
    {
        return this.modelRenderer;
    }

    public BakedModel getBlockModel(BlockState p_110911_)
    {
        return this.blockModelShaper.getBlockModel(p_110911_);
    }

    public void renderSingleBlock(BlockState p_110913_, PoseStack p_110914_, MultiBufferSource p_110915_, int p_110916_, int p_110917_)
    {
        this.renderSingleBlock(p_110913_, p_110914_, p_110915_, p_110916_, p_110917_, ModelData.EMPTY, null);
    }

    public void renderSingleBlock(
        BlockState blockStateIn,
        PoseStack matrixStackIn,
        MultiBufferSource bufferTypeIn,
        int combinedLightIn,
        int combinedOverlayIn,
        ModelData modelData,
        RenderType renderType
    )
    {
        RenderShape rendershape = blockStateIn.getRenderShape();

        if (rendershape != RenderShape.INVISIBLE)
        {
            switch (rendershape)
            {
                case MODEL:
                    BakedModel bakedmodel = this.getBlockModel(blockStateIn);
                    int i = this.blockColors.getColor(blockStateIn, null, null, 0);
                    float f = (float)(i >> 16 & 0xFF) / 255.0F;
                    float f1 = (float)(i >> 8 & 0xFF) / 255.0F;
                    float f2 = (float)(i & 0xFF) / 255.0F;

                    if (Reflector.ForgeHooksClient.exists())
                    {
                        for (RenderType rendertype : bakedmodel.getRenderTypes(blockStateIn, RandomSource.create(42L), modelData))
                        {
                            this.modelRenderer
                            .renderModel(
                                matrixStackIn.last(),
                                bufferTypeIn.getBuffer(renderType != null ? renderType : RenderTypeHelper.getEntityRenderType(rendertype, false)),
                                blockStateIn,
                                bakedmodel,
                                f,
                                f1,
                                f2,
                                combinedLightIn,
                                combinedOverlayIn,
                                modelData,
                                rendertype
                            );
                        }
                    }
                    else
                    {
                        this.modelRenderer
                        .renderModel(
                            matrixStackIn.last(),
                            bufferTypeIn.getBuffer(ItemBlockRenderTypes.getRenderType(blockStateIn, false)),
                            blockStateIn,
                            bakedmodel,
                            f,
                            f1,
                            f2,
                            combinedLightIn,
                            combinedOverlayIn
                        );
                    }

                    break;

                case ENTITYBLOCK_ANIMATED:
                    if (Reflector.MinecraftForge.exists())
                    {
                        ItemStack itemstack = new ItemStack(blockStateIn.getBlock());
                        IClientItemExtensions iclientitemextensions = IClientItemExtensions.of(itemstack);
                        BlockEntityWithoutLevelRenderer blockentitywithoutlevelrenderer = iclientitemextensions.getCustomRenderer();
                        blockentitywithoutlevelrenderer.renderByItem(
                            itemstack, ItemDisplayContext.NONE, matrixStackIn, bufferTypeIn, combinedLightIn, combinedOverlayIn
                        );
                    }
                    else
                    {
                        this.blockEntityRenderer
                        .renderByItem(
                            new ItemStack(blockStateIn.getBlock()),
                            ItemDisplayContext.NONE,
                            matrixStackIn,
                            bufferTypeIn,
                            combinedLightIn,
                            combinedOverlayIn
                        );
                    }
            }
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager p_110909_)
    {
        this.liquidBlockRenderer.setupSprites();
    }
}
