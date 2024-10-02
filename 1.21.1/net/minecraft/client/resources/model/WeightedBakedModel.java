package net.minecraft.client.resources.model;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

public class WeightedBakedModel implements BakedModel, IDynamicBakedModel
{
    private final int totalWeight;
    private final List<WeightedEntry.Wrapper<BakedModel>> list;
    private final BakedModel wrapped;

    public WeightedBakedModel(List<WeightedEntry.Wrapper<BakedModel>> p_119544_)
    {
        this.list = p_119544_;
        this.totalWeight = WeightedRandom.getTotalWeight(p_119544_);
        this.wrapped = p_119544_.get(0).data();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState p_235058_, @Nullable Direction p_235059_, RandomSource p_235060_)
    {
        WeightedEntry.Wrapper<BakedModel> wrapper = getWeightedItem(this.list, Math.abs((int)p_235060_.nextLong()) % this.totalWeight);
        return wrapper == null ? Collections.emptyList() : wrapper.data().getQuads(p_235058_, p_235059_, p_235060_);
    }

    public static <T extends WeightedEntry> T getWeightedItem(List<T> items, int targetWeight)
    {
        for (int i = 0; i < items.size(); i++)
        {
            T t = (T)items.get(i);
            targetWeight -= t.getWeight().asInt();

            if (targetWeight < 0)
            {
                return t;
            }
        }

        return null;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData modelData, RenderType renderType)
    {
        WeightedEntry.Wrapper<BakedModel> wrapper = getWeightedItem(this.list, Math.abs((int)rand.nextLong()) % this.totalWeight);
        return wrapper == null ? Collections.emptyList() : wrapper.data().getQuads(state, side, rand, modelData, renderType);
    }

    @Override
    public boolean useAmbientOcclusion(BlockState state)
    {
        return this.wrapped.useAmbientOcclusion(state);
    }

    @Override
    public boolean useAmbientOcclusion(BlockState state, RenderType renderType)
    {
        return this.wrapped.useAmbientOcclusion(state, renderType);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData modelData)
    {
        return this.wrapped.getParticleIcon(modelData);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform)
    {
        return this.wrapped.applyTransform(transformType, poseStack, applyLeftHandTransform);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data)
    {
        WeightedEntry.Wrapper<BakedModel> wrapper = getWeightedItem(this.list, Math.abs((int)rand.nextLong()) % this.totalWeight);
        return wrapper == null ? ChunkRenderTypeSet.none() : wrapper.data().getRenderTypes(state, rand, data);
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return this.wrapped.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d()
    {
        return this.wrapped.isGui3d();
    }

    @Override
    public boolean usesBlockLight()
    {
        return this.wrapped.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer()
    {
        return this.wrapped.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon()
    {
        return this.wrapped.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms()
    {
        return this.wrapped.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides()
    {
        return this.wrapped.getOverrides();
    }

    public static class Builder
    {
        private final List<WeightedEntry.Wrapper<BakedModel>> list = Lists.newArrayList();

        public WeightedBakedModel.Builder add(@Nullable BakedModel p_119560_, int p_119561_)
        {
            if (p_119560_ != null)
            {
                this.list.add(WeightedEntry.wrap(p_119560_, p_119561_));
            }

            return this;
        }

        @Nullable
        public BakedModel build()
        {
            if (this.list.isEmpty())
            {
                return null;
            }
            else
            {
                return (BakedModel)(this.list.size() == 1 ? this.list.get(0).data() : new WeightedBakedModel(this.list));
            }
        }
    }
}
