package com.mojang.blaze3d.vertex;

import java.util.function.Consumer;
import net.optifine.render.VertexBuilderWrapper;

public class VertexMultiConsumer
{
    public static VertexConsumer create()
    {
        throw new IllegalArgumentException();
    }

    public static VertexConsumer create(VertexConsumer p_167062_)
    {
        return p_167062_;
    }

    public static VertexConsumer create(VertexConsumer p_86169_, VertexConsumer p_86170_)
    {
        return new VertexMultiConsumer.Double(p_86169_, p_86170_);
    }

    public static VertexConsumer create(VertexConsumer... p_167064_)
    {
        return new VertexMultiConsumer.Multiple(p_167064_);
    }

    static class Double extends VertexBuilderWrapper implements VertexConsumer
    {
        private final VertexConsumer first;
        private final VertexConsumer second;
        private boolean fixMultitextureUV;

        public Double(VertexConsumer p_86174_, VertexConsumer p_86175_)
        {
            super(p_86175_);

            if (p_86174_ == p_86175_)
            {
                throw new IllegalArgumentException("Duplicate delegates");
            }
            else
            {
                this.first = p_86174_;
                this.second = p_86175_;
                this.updateFixMultitextureUv();
            }
        }

        @Override
        public VertexConsumer addVertex(float p_344257_, float p_342162_, float p_344087_)
        {
            this.first.addVertex(p_344257_, p_342162_, p_344087_);
            this.second.addVertex(p_344257_, p_342162_, p_344087_);
            return this;
        }

        @Override
        public VertexConsumer setColor(int p_344640_, int p_343095_, int p_343643_, int p_342454_)
        {
            this.first.setColor(p_344640_, p_343095_, p_343643_, p_342454_);
            this.second.setColor(p_344640_, p_343095_, p_343643_, p_342454_);
            return this;
        }

        @Override
        public VertexConsumer setUv(float p_342147_, float p_344679_)
        {
            this.first.setUv(p_342147_, p_344679_);
            this.second.setUv(p_342147_, p_344679_);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int p_342434_, int p_344283_)
        {
            this.first.setUv1(p_342434_, p_344283_);
            this.second.setUv1(p_342434_, p_344283_);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int p_343541_, int p_342529_)
        {
            this.first.setUv2(p_343541_, p_342529_);
            this.second.setUv2(p_343541_, p_342529_);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float p_342340_, float p_343788_, float p_345466_)
        {
            this.first.setNormal(p_342340_, p_343788_, p_345466_);
            this.second.setNormal(p_342340_, p_343788_, p_345466_);
            return this;
        }

        @Override
        public void addVertex(
            float p_345388_,
            float p_343258_,
            float p_344041_,
            int p_343827_,
            float p_342641_,
            float p_344103_,
            int p_345208_,
            int p_344566_,
            float p_344092_,
            float p_344193_,
            float p_343729_
        )
        {
            if (this.fixMultitextureUV)
            {
                this.first
                .addVertex(
                    p_345388_, p_343258_, p_344041_, p_343827_, p_342641_ / 32.0F, p_344103_ / 32.0F, p_345208_, p_344566_, p_344092_, p_344193_, p_343729_
                );
            }
            else
            {
                this.first
                .addVertex(p_345388_, p_343258_, p_344041_, p_343827_, p_342641_, p_344103_, p_345208_, p_344566_, p_344092_, p_344193_, p_343729_);
            }

            this.second.addVertex(p_345388_, p_343258_, p_344041_, p_343827_, p_342641_, p_344103_, p_345208_, p_344566_, p_344092_, p_344193_, p_343729_);
        }

        private void updateFixMultitextureUv()
        {
            this.fixMultitextureUV = !this.first.isMultiTexture() && this.second.isMultiTexture();
        }

        @Override
        public VertexConsumer getSecondaryBuilder()
        {
            return this.first;
        }
    }

    static class Multiple extends VertexBuilderWrapper implements VertexConsumer
    {
        private VertexConsumer[] delegates;

        Multiple(VertexConsumer[] delegates)
        {
            super(delegates.length > 0 ? delegates[0] : null);
            this.delegates = delegates;

            for (int i = 0; i < delegates.length; i++)
            {
                for (int j = i + 1; j < delegates.length; j++)
                {
                    if (delegates[i] == delegates[j])
                    {
                        throw new IllegalArgumentException("Duplicate delegates");
                    }
                }
            }

            this.delegates = delegates;
        }

        private void forEach(Consumer<VertexConsumer> p_167145_)
        {
            for (VertexConsumer vertexconsumer : this.delegates)
            {
                p_167145_.accept(vertexconsumer);
            }
        }

        @Override
        public VertexConsumer addVertex(float p_167147_, float p_167148_, float p_167149_)
        {
            this.forEach(consumerIn -> consumerIn.addVertex(p_167147_, p_167148_, p_167149_));
            return this;
        }

        @Override
        public VertexConsumer setColor(int p_167130_, int p_167131_, int p_167132_, int p_167133_)
        {
            this.forEach(consumerIn -> consumerIn.setColor(p_167130_, p_167131_, p_167132_, p_167133_));
            return this;
        }

        @Override
        public VertexConsumer setUv(float p_167084_, float p_167085_)
        {
            this.forEach(consumerIn -> consumerIn.setUv(p_167084_, p_167085_));
            return this;
        }

        @Override
        public VertexConsumer setUv1(int p_343411_, int p_342288_)
        {
            this.forEach(consumerIn -> consumerIn.setUv1(p_343411_, p_342288_));
            return this;
        }

        @Override
        public VertexConsumer setUv2(int p_343645_, int p_344197_)
        {
            this.forEach(consumerIn -> consumerIn.setUv2(p_343645_, p_344197_));
            return this;
        }

        @Override
        public VertexConsumer setNormal(float p_343750_, float p_344366_, float p_342844_)
        {
            this.forEach(consumerIn -> consumerIn.setNormal(p_343750_, p_344366_, p_342844_));
            return this;
        }

        @Override
        public void addVertex(
            float p_342518_,
            float p_344848_,
            float p_345186_,
            int p_343970_,
            float p_345395_,
            float p_342765_,
            int p_345332_,
            int p_342050_,
            float p_343977_,
            float p_342883_,
            float p_344334_
        )
        {
            this.forEach(
                consumerIn -> consumerIn.addVertex(
                    p_342518_, p_344848_, p_345186_, p_343970_, p_345395_, p_342765_, p_345332_, p_342050_, p_343977_, p_342883_, p_344334_
                )
            );
        }
    }
}
