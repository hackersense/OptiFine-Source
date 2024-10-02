package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.model.data.ModelData;
import net.optifine.BlockPosM;
import net.optifine.Config;
import net.optifine.CustomBlockLayers;
import net.optifine.override.ChunkCacheOF;
import net.optifine.reflect.Reflector;
import net.optifine.render.RenderEnv;
import net.optifine.render.RenderTypes;
import net.optifine.shaders.Shaders;
import net.optifine.util.SingleIterable;

public class SectionCompiler
{
    private final BlockRenderDispatcher blockRenderer;
    private final BlockEntityRenderDispatcher blockEntityRenderer;
    protected SectionRenderDispatcher sectionRenderDispatcher;
    public static final boolean FORGE = Reflector.ForgeHooksClient.exists();

    public SectionCompiler(BlockRenderDispatcher p_344503_, BlockEntityRenderDispatcher p_345164_)
    {
        this.blockRenderer = p_344503_;
        this.blockEntityRenderer = p_345164_;
    }

    public SectionCompiler.Results compile(SectionPos p_344383_, RenderChunkRegion p_342669_, VertexSorting p_342522_, SectionBufferBuilderPack p_343546_)
    {
        ChunkCacheOF chunkcacheof = p_342669_.makeChunkCacheOF();
        return this.compile(p_344383_, chunkcacheof, p_342522_, p_343546_, 0, 0, 0);
    }

    public SectionCompiler.Results compile(
        SectionPos sectionPosIn, ChunkCacheOF regionIn, VertexSorting sortingIn, SectionBufferBuilderPack builderIn, int regionDX, int regionDY, int regionDZ
    )
    {
        Map<BlockPos, ModelData> map = FORGE ? Minecraft.getInstance().level.getModelDataManager().getAt(sectionPosIn) : null;
        SectionCompiler.Results sectioncompiler$results = new SectionCompiler.Results();
        BlockPos blockpos = sectionPosIn.origin();
        BlockPos blockpos1 = blockpos.offset(15, 15, 15);
        VisGraph visgraph = new VisGraph();
        PoseStack posestack = new PoseStack();
        SectionRenderDispatcher.renderChunksUpdated++;
        regionIn.renderStart();
        SingleIterable<RenderType> singleiterable = new SingleIterable<>();
        boolean flag = Config.isMipmaps();
        boolean flag1 = Config.isShaders();
        boolean flag2 = flag1 && Shaders.useMidBlockAttrib;
        ModelBlockRenderer.enableCaching();
        Map<RenderType, BufferBuilder> map1 = new Reference2ObjectArrayMap<>(RenderType.chunkBufferLayers().size());
        RandomSource randomsource = RandomSource.create();
        BlockRenderDispatcher blockrenderdispatcher = Minecraft.getInstance().getBlockRenderer();

        for (BlockPosM blockposm : BlockPosM.getAllInBoxMutableM(blockpos, blockpos1))
        {
            BlockState blockstate = regionIn.getBlockState(blockposm);

            if (!blockstate.isAir())
            {
                if (blockstate.isSolidRender(regionIn, blockposm))
                {
                    visgraph.setOpaque(blockposm);
                }

                if (blockstate.hasBlockEntity())
                {
                    BlockEntity blockentity = regionIn.getBlockEntity(blockposm);

                    if (blockentity != null)
                    {
                        this.handleBlockEntity(sectioncompiler$results, blockentity);
                    }
                }

                FluidState fluidstate = blockstate.getFluidState();

                if (!fluidstate.isEmpty())
                {
                    RenderType rendertype = ItemBlockRenderTypes.getRenderLayer(fluidstate);
                    BufferBuilder bufferbuilder = this.getOrBeginLayer(map1, builderIn, rendertype);
                    RenderEnv renderenv = bufferbuilder.getRenderEnv(blockstate, blockposm);
                    renderenv.setCompileParams(this, map1, builderIn);
                    regionIn.setRenderEnv(renderenv);
                    this.blockRenderer.renderLiquid(blockposm, regionIn, bufferbuilder, blockstate, fluidstate);
                }

                if (blockstate.getRenderShape() == RenderShape.MODEL)
                {
                    BakedModel bakedmodel = blockrenderdispatcher.getBlockModel(blockstate);
                    ModelData modeldata = FORGE ? bakedmodel.getModelData(regionIn, blockposm, blockstate, map.getOrDefault(blockposm, ModelData.EMPTY)) : null;

                    for (RenderType rendertype1 : getBlockRenderLayers(bakedmodel, blockstate, blockposm, randomsource, modeldata, singleiterable))
                    {
                        RenderType rendertype2 = this.fixBlockLayer(regionIn, blockstate, blockposm, rendertype1, flag);
                        BufferBuilder bufferbuilder1 = this.getOrBeginLayer(map1, builderIn, rendertype2);
                        RenderEnv renderenv1 = bufferbuilder1.getRenderEnv(blockstate, blockposm);
                        renderenv1.setCompileParams(this, map1, builderIn);
                        regionIn.setRenderEnv(renderenv1);
                        posestack.pushPose();
                        posestack.translate(
                            (float)regionDX + (float)SectionPos.sectionRelative(blockposm.getX()),
                            (float)regionDY + (float)SectionPos.sectionRelative(blockposm.getY()),
                            (float)regionDZ + (float)SectionPos.sectionRelative(blockposm.getZ())
                        );

                        if (flag2)
                        {
                            bufferbuilder1.setMidBlock(
                                0.5F + (float)regionDX + (float)SectionPos.sectionRelative(blockposm.getX()),
                                0.5F + (float)regionDY + (float)SectionPos.sectionRelative(blockposm.getY()),
                                0.5F + (float)regionDZ + (float)SectionPos.sectionRelative(blockposm.getZ())
                            );
                        }

                        this.blockRenderer.renderBatched(blockstate, blockposm, regionIn, posestack, bufferbuilder1, true, randomsource, modeldata, rendertype1);
                        posestack.popPose();
                    }
                }
            }
        }

        for (RenderType rendertype4 : SectionRenderDispatcher.BLOCK_RENDER_LAYERS)
        {
            sectioncompiler$results.setAnimatedSprites(rendertype4, null);
        }

        for (Entry<RenderType, BufferBuilder> entry : map1.entrySet())
        {
            RenderType rendertype3 = entry.getKey();
            BufferBuilder bufferbuilder2 = entry.getValue();

            if (bufferbuilder2.animatedSprites != null && !bufferbuilder2.animatedSprites.isEmpty())
            {
                sectioncompiler$results.setAnimatedSprites(rendertype3, (BitSet)bufferbuilder2.animatedSprites.clone());
            }

            MeshData meshdata = bufferbuilder2.build();

            if (meshdata != null)
            {
                if (rendertype3 == RenderType.translucent())
                {
                    sectioncompiler$results.transparencyState = meshdata.sortQuads(builderIn.buffer(RenderType.translucent()), sortingIn);
                }

                sectioncompiler$results.renderedLayers.put(rendertype3, meshdata);
            }
        }

        regionIn.renderFinish();
        ModelBlockRenderer.clearCache();
        sectioncompiler$results.visibilitySet = visgraph.resolve();
        return sectioncompiler$results;
    }

    public BufferBuilder getOrBeginLayer(Map<RenderType, BufferBuilder> p_344204_, SectionBufferBuilderPack p_344936_, RenderType p_343427_)
    {
        BufferBuilder bufferbuilder = p_344204_.get(p_343427_);

        if (bufferbuilder == null)
        {
            ByteBufferBuilder bytebufferbuilder = p_344936_.buffer(p_343427_);
            bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK, p_343427_);
            p_344204_.put(p_343427_, bufferbuilder);
        }

        return bufferbuilder;
    }

    private <E extends BlockEntity> void handleBlockEntity(SectionCompiler.Results p_343713_, E p_343478_)
    {
        BlockEntityRenderer<E> blockentityrenderer = this.blockEntityRenderer.getRenderer(p_343478_);

        if (blockentityrenderer != null)
        {
            if (blockentityrenderer.shouldRenderOffScreen(p_343478_))
            {
                p_343713_.globalBlockEntities.add(p_343478_);
            }
            else
            {
                p_343713_.blockEntities.add(p_343478_);
            }
        }
    }

    public static Iterable<RenderType> getBlockRenderLayers(
        BakedModel model, BlockState blockState, BlockPos blockPos, RandomSource randomsource, ModelData modelData, SingleIterable<RenderType> singleLayer
    )
    {
        if (FORGE)
        {
            randomsource.setSeed(blockState.getSeed(blockPos));
            return model.getRenderTypes(blockState, randomsource, modelData);
        }
        else
        {
            singleLayer.setValue(ItemBlockRenderTypes.getChunkRenderType(blockState));
            return singleLayer;
        }
    }

    private RenderType fixBlockLayer(BlockGetter worldReader, BlockState blockState, BlockPos blockPos, RenderType layer, boolean isMipmaps)
    {
        if (CustomBlockLayers.isActive())
        {
            RenderType rendertype = CustomBlockLayers.getRenderLayer(worldReader, blockState, blockPos);

            if (rendertype != null)
            {
                return rendertype;
            }
        }

        if (isMipmaps)
        {
            if (layer == RenderTypes.CUTOUT)
            {
                Block block = blockState.getBlock();

                if (block instanceof RedStoneWireBlock)
                {
                    return layer;
                }

                if (block instanceof CactusBlock)
                {
                    return layer;
                }

                return RenderTypes.CUTOUT_MIPPED;
            }
        }
        else if (layer == RenderTypes.CUTOUT_MIPPED)
        {
            return RenderTypes.CUTOUT;
        }

        return layer;
    }

    public static final class Results
    {
        public final List<BlockEntity> globalBlockEntities = new ArrayList<>();
        public final List<BlockEntity> blockEntities = new ArrayList<>();
        public final Map<RenderType, MeshData> renderedLayers = new Reference2ObjectArrayMap<>();
        public VisibilitySet visibilitySet = new VisibilitySet();
        @Nullable
        public MeshData.SortState transparencyState;
        public BitSet[] animatedSprites = new BitSet[RenderType.CHUNK_RENDER_TYPES.length];

        public void setAnimatedSprites(RenderType layer, BitSet animatedSprites)
        {
            this.animatedSprites[layer.ordinal()] = animatedSprites;
        }

        public void release()
        {
            this.renderedLayers.values().forEach(MeshData::close);
        }
    }
}
