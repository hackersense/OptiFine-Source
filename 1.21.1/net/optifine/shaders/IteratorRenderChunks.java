package net.optifine.shaders;

import java.util.Iterator;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.optifine.BlockPosM;

public class IteratorRenderChunks implements Iterator<SectionRenderDispatcher.RenderSection>
{
    private ViewArea viewFrustum;
    private Iterator3d Iterator3d;
    private BlockPosM posBlock = new BlockPosM(0, 0, 0);

    public IteratorRenderChunks(ViewArea viewFrustum, BlockPos posStart, BlockPos posEnd, int width, int height)
    {
        this.viewFrustum = viewFrustum;
        this.Iterator3d = new Iterator3d(posStart, posEnd, width, height);
    }

    @Override
    public boolean hasNext()
    {
        return this.Iterator3d.hasNext();
    }

    public SectionRenderDispatcher.RenderSection next()
    {
        BlockPos blockpos = this.Iterator3d.next();
        this.posBlock.setXyz(blockpos.getX() << 4, blockpos.getY() << 4, blockpos.getZ() << 4);
        return this.viewFrustum.getRenderSectionAt(this.posBlock);
    }

    @Override
    public void remove()
    {
        throw new RuntimeException("Not implemented");
    }
}
