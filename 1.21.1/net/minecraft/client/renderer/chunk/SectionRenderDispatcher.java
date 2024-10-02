package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.optifine.Config;
import net.optifine.override.ChunkCacheOF;
import net.optifine.render.AabbFrame;
import net.optifine.render.ChunkLayerMap;
import net.optifine.render.ChunkLayerSet;
import net.optifine.render.ICamera;
import net.optifine.render.RenderTypes;
import net.optifine.util.ChunkUtils;

public class SectionRenderDispatcher
{
    private static final int MAX_HIGH_PRIORITY_QUOTA = 2;
    private final PriorityBlockingQueue<SectionRenderDispatcher.RenderSection.CompileTask> toBatchHighPriority = Queues.newPriorityBlockingQueue();
    private final Queue<SectionRenderDispatcher.RenderSection.CompileTask> toBatchLowPriority = Queues.newLinkedBlockingDeque();
    private int highPriorityQuota = 2;
    private final Queue<Runnable> toUpload = Queues.newConcurrentLinkedQueue();
    final SectionBufferBuilderPack fixedBuffers;
    private final SectionBufferBuilderPool bufferPool;
    private volatile int toBatchCount;
    private volatile boolean closed;
    private final ProcessorMailbox<Runnable> mailbox;
    private final Executor executor;
    ClientLevel level;
    final LevelRenderer renderer;
    private Vec3 camera = Vec3.ZERO;
    final SectionCompiler sectionCompiler;
    private int countRenderBuilders;
    private List<SectionBufferBuilderPack> listPausedBuilders = new ArrayList<>();
    public static final RenderType[] BLOCK_RENDER_LAYERS = RenderType.chunkBufferLayers().toArray(new RenderType[0]);
    public static int renderChunksUpdated;

    public SectionRenderDispatcher(
        ClientLevel p_299878_,
        LevelRenderer p_299032_,
        Executor p_298480_,
        RenderBuffers p_310401_,
        BlockRenderDispatcher p_343142_,
        BlockEntityRenderDispatcher p_344654_
    )
    {
        this.level = p_299878_;
        this.renderer = p_299032_;
        this.fixedBuffers = p_310401_.fixedBufferPack();
        this.bufferPool = p_310401_.sectionBufferPool();
        this.countRenderBuilders = this.bufferPool.getFreeBufferCount();
        this.executor = p_298480_;
        this.mailbox = ProcessorMailbox.create(p_298480_, "Section Renderer");
        this.mailbox.tell(this::runTask);
        this.sectionCompiler = new SectionCompiler(p_343142_, p_344654_);
        this.sectionCompiler.sectionRenderDispatcher = this;
    }

    public void setLevel(ClientLevel p_298968_)
    {
        this.level = p_298968_;
    }

    private void runTask()
    {
        if (!this.closed && !this.bufferPool.isEmpty())
        {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.pollTask();

            if (sectionrenderdispatcher$rendersection$compiletask != null)
            {
                SectionBufferBuilderPack sectionbufferbuilderpack = Objects.requireNonNull(this.bufferPool.acquire());

                if (sectionbufferbuilderpack == null)
                {
                    this.toBatchHighPriority.add(sectionrenderdispatcher$rendersection$compiletask);
                    return;
                }

                this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
                CompletableFuture.supplyAsync(
                    Util.wrapThreadWithTaskName(
                        sectionrenderdispatcher$rendersection$compiletask.name(),
                        () -> sectionrenderdispatcher$rendersection$compiletask.doTask(sectionbufferbuilderpack)
                    ),
                    this.executor
                )
                .thenCompose(resultIn -> (CompletionStage<SectionRenderDispatcher.SectionTaskResult>)resultIn)
                .whenComplete((taskResultIn, throwableIn) ->
                {
                    if (throwableIn != null)
                    {
                        Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwableIn, "Batching sections"));
                    }
                    else {
                        this.mailbox.tell(() -> {
                            if (taskResultIn == SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL)
                            {
                                sectionbufferbuilderpack.clearAll();
                            }
                            else {
                                sectionbufferbuilderpack.discardAll();
                            }

                            this.bufferPool.release(sectionbufferbuilderpack);
                            this.runTask();
                        });
                    }
                });
            }
        }
    }

    @Nullable
    private SectionRenderDispatcher.RenderSection.CompileTask pollTask()
    {
        if (this.highPriorityQuota <= 0)
        {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.toBatchLowPriority.poll();

            if (sectionrenderdispatcher$rendersection$compiletask != null)
            {
                this.highPriorityQuota = 2;
                return sectionrenderdispatcher$rendersection$compiletask;
            }
        }

        SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask1 = this.toBatchHighPriority.poll();

        if (sectionrenderdispatcher$rendersection$compiletask1 != null)
        {
            this.highPriorityQuota--;
            return sectionrenderdispatcher$rendersection$compiletask1;
        }
        else
        {
            this.highPriorityQuota = 2;
            return this.toBatchLowPriority.poll();
        }
    }

    public String getStats()
    {
        return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.toBatchCount, this.toUpload.size(), this.bufferPool.getFreeBufferCount());
    }

    public int getToBatchCount()
    {
        return this.toBatchCount;
    }

    public int getToUpload()
    {
        return this.toUpload.size();
    }

    public int getFreeBufferCount()
    {
        return this.bufferPool.getFreeBufferCount();
    }

    public void setCamera(Vec3 p_297762_)
    {
        this.camera = p_297762_;
    }

    public Vec3 getCameraPosition()
    {
        return this.camera;
    }

    public void uploadAllPendingUploads()
    {
        Runnable runnable;

        while ((runnable = this.toUpload.poll()) != null)
        {
            runnable.run();
        }
    }

    public void rebuildSectionSync(SectionRenderDispatcher.RenderSection p_299640_, RenderRegionCache p_297835_)
    {
        p_299640_.compileSync(p_297835_);
    }

    public void blockUntilClear()
    {
        this.clearBatchQueue();
    }

    public void schedule(SectionRenderDispatcher.RenderSection.CompileTask p_297747_)
    {
        if (!this.closed)
        {
            this.mailbox.tell(() ->
            {
                if (!this.closed)
                {
                    if (p_297747_.isHighPriority)
                    {
                        this.toBatchHighPriority.offer(p_297747_);
                    }
                    else
                    {
                        this.toBatchLowPriority.offer(p_297747_);
                    }

                    this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
                    this.runTask();
                }
            });
        }
    }

    public CompletableFuture<Void> uploadSectionLayer(MeshData p_344050_, VertexBuffer p_298938_)
    {
        return this.closed ? CompletableFuture.completedFuture(null) : CompletableFuture.runAsync(() ->
        {
            if (p_298938_.isInvalid())
            {
                p_344050_.close();
            }
            else {
                p_298938_.bind();
                p_298938_.upload(p_344050_);
                VertexBuffer.unbind();
            }
        }, this.toUpload::add);
    }

    public CompletableFuture<Void> uploadSectionIndexBuffer(ByteBufferBuilder.Result p_343213_, VertexBuffer p_344049_)
    {
        return this.closed ? CompletableFuture.completedFuture(null) : CompletableFuture.runAsync(() ->
        {
            if (p_344049_.isInvalid())
            {
                p_343213_.close();
            }
            else {
                p_344049_.bind();
                p_344049_.uploadIndexBuffer(p_343213_);
                VertexBuffer.unbind();
            }
        }, this.toUpload::add);
    }

    private void clearBatchQueue()
    {
        while (!this.toBatchHighPriority.isEmpty())
        {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.toBatchHighPriority.poll();

            if (sectionrenderdispatcher$rendersection$compiletask != null)
            {
                sectionrenderdispatcher$rendersection$compiletask.cancel();
            }
        }

        while (!this.toBatchLowPriority.isEmpty())
        {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask1 = this.toBatchLowPriority.poll();

            if (sectionrenderdispatcher$rendersection$compiletask1 != null)
            {
                sectionrenderdispatcher$rendersection$compiletask1.cancel();
            }
        }

        this.toBatchCount = 0;
    }

    public boolean isQueueEmpty()
    {
        return this.toBatchCount == 0 && this.toUpload.isEmpty();
    }

    public void dispose()
    {
        this.closed = true;
        this.clearBatchQueue();
        this.uploadAllPendingUploads();
    }

    public void pauseChunkUpdates()
    {
        long i = System.currentTimeMillis();

        if (this.listPausedBuilders.size() <= 0)
        {
            while (this.listPausedBuilders.size() != this.countRenderBuilders)
            {
                this.uploadAllPendingUploads();
                SectionBufferBuilderPack sectionbufferbuilderpack = this.bufferPool.acquire();

                if (sectionbufferbuilderpack != null)
                {
                    this.listPausedBuilders.add(sectionbufferbuilderpack);
                }

                if (System.currentTimeMillis() > i + 1000L)
                {
                    break;
                }
            }
        }
    }

    public void resumeChunkUpdates()
    {
        for (SectionBufferBuilderPack sectionbufferbuilderpack : this.listPausedBuilders)
        {
            this.bufferPool.release(sectionbufferbuilderpack);
        }

        this.listPausedBuilders.clear();
    }

    public boolean updateChunkNow(SectionRenderDispatcher.RenderSection renderChunk, RenderRegionCache regionCacheIn)
    {
        this.rebuildSectionSync(renderChunk, regionCacheIn);
        return true;
    }

    public boolean updateChunkLater(SectionRenderDispatcher.RenderSection renderChunk, RenderRegionCache regionCacheIn)
    {
        if (this.bufferPool.isEmpty())
        {
            return false;
        }
        else
        {
            renderChunk.rebuildSectionAsync(this, regionCacheIn);
            return true;
        }
    }

    public boolean updateTransparencyLater(SectionRenderDispatcher.RenderSection renderChunk)
    {
        return this.bufferPool.isEmpty() ? false : renderChunk.resortTransparency(RenderTypes.TRANSLUCENT, this);
    }

    public void addUploadTask(Runnable r)
    {
        if (r != null)
        {
            this.toUpload.add(r);
        }
    }

    public static class CompiledSection
    {
        public static final SectionRenderDispatcher.CompiledSection UNCOMPILED = new SectionRenderDispatcher.CompiledSection()
        {
            @Override
            public boolean facesCanSeeEachother(Direction p_301280_, Direction p_299155_)
            {
                return false;
            }
            public void setAnimatedSprites(RenderType layer, BitSet animatedSprites)
            {
                throw new UnsupportedOperationException();
            }
        };
        public static final SectionRenderDispatcher.CompiledSection EMPTY = new SectionRenderDispatcher.CompiledSection()
        {
            @Override
            public boolean facesCanSeeEachother(Direction p_343413_, Direction p_342431_)
            {
                return true;
            }
        };
        final Set<RenderType> hasBlocks = new ChunkLayerSet();
        final List<BlockEntity> renderableBlockEntities = Lists.newArrayList();
        VisibilitySet visibilitySet = new VisibilitySet();
        @Nullable
        MeshData.SortState transparencyState;
        private BitSet[] animatedSprites = new BitSet[RenderType.CHUNK_RENDER_TYPES.length];

        public boolean hasNoRenderableLayers()
        {
            return this.hasBlocks.isEmpty();
        }

        public boolean isEmpty(RenderType p_300861_)
        {
            return !this.hasBlocks.contains(p_300861_);
        }

        public List<BlockEntity> getRenderableBlockEntities()
        {
            return this.renderableBlockEntities;
        }

        public boolean facesCanSeeEachother(Direction p_301006_, Direction p_300193_)
        {
            return this.visibilitySet.visibilityBetween(p_301006_, p_300193_);
        }

        public BitSet getAnimatedSprites(RenderType layer)
        {
            return this.animatedSprites[layer.ordinal()];
        }

        public void setAnimatedSprites(BitSet[] animatedSprites)
        {
            this.animatedSprites = animatedSprites;
        }

        public boolean isLayerUsed(RenderType renderTypeIn)
        {
            return this.hasBlocks.contains(renderTypeIn);
        }

        public void setLayerUsed(RenderType renderTypeIn)
        {
            this.hasBlocks.add(renderTypeIn);
        }

        public boolean hasTerrainBlockEntities()
        {
            return !this.hasNoRenderableLayers() || !this.getRenderableBlockEntities().isEmpty();
        }

        public Set<RenderType> getLayersUsed()
        {
            return this.hasBlocks;
        }
    }

    public class RenderSection
    {
        public static final int SIZE = 16;
        public final int index;
        public final AtomicReference<SectionRenderDispatcher.CompiledSection> compiled = new AtomicReference<>(
            SectionRenderDispatcher.CompiledSection.UNCOMPILED
        );
        private final AtomicInteger initialCompilationCancelCount = new AtomicInteger(0);
        @Nullable
        private SectionRenderDispatcher.RenderSection.RebuildTask lastRebuildTask;
        @Nullable
        private SectionRenderDispatcher.RenderSection.ResortTransparencyTask lastResortTransparencyTask;
        private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
        private final ChunkLayerMap<VertexBuffer> buffers = new ChunkLayerMap<>(renderType -> new VertexBuffer(VertexBuffer.Usage.STATIC));
        private AABB bb;
        private boolean dirty = true;
        final BlockPos.MutableBlockPos origin = new BlockPos.MutableBlockPos(-1, -1, -1);
        private final BlockPos.MutableBlockPos[] relativeOrigins = Util.make(new BlockPos.MutableBlockPos[6], posArrIn ->
        {
            for (int i = 0; i < posArrIn.length; i++)
            {
                posArrIn[i] = new BlockPos.MutableBlockPos();
            }
        });
        private boolean playerChanged;
        private boolean playerUpdate = false;
        private boolean needsBackgroundPriorityUpdate;
        private boolean renderRegions = Config.isRenderRegions();
        public int regionX;
        public int regionZ;
        public int regionDX;
        public int regionDY;
        public int regionDZ;
        private final SectionRenderDispatcher.RenderSection[] renderChunksOfset16 = new SectionRenderDispatcher.RenderSection[6];
        private boolean renderChunksOffset16Updated = false;
        private LevelChunk chunk;
        private SectionRenderDispatcher.RenderSection[] renderChunkNeighbours = new SectionRenderDispatcher.RenderSection[Direction.VALUES.length];
        private SectionRenderDispatcher.RenderSection[] renderChunkNeighboursValid = new SectionRenderDispatcher.RenderSection[Direction.VALUES.length];
        private boolean renderChunkNeighboursUpated = false;
        private SectionOcclusionGraph.Node renderInfo = new SectionOcclusionGraph.Node(this, null, 0);
        public AabbFrame boundingBoxParent;
        private SectionPos sectionPosition;

        public RenderSection(final int p_299358_, final int p_299044_, final int p_300810_, final int p_299840_)
        {
            this.index = p_299358_;
            this.setOrigin(p_299044_, p_300810_, p_299840_);
        }

        private boolean doesChunkExistAt(BlockPos p_297611_)
        {
            return SectionRenderDispatcher.this.level
                   .getChunk(SectionPos.blockToSectionCoord(p_297611_.getX()), SectionPos.blockToSectionCoord(p_297611_.getZ()), ChunkStatus.FULL, false)
                   != null;
        }

        public boolean hasAllNeighbors()
        {
            int i = 24;
            return !(this.getDistToPlayerSqr() > 576.0) ? true : this.doesChunkExistAt(this.origin);
        }

        public AABB getBoundingBox()
        {
            return this.bb;
        }

        public VertexBuffer getBuffer(RenderType p_298748_)
        {
            return this.buffers.get(p_298748_);
        }

        public void setOrigin(int p_298099_, int p_299019_, int p_299020_)
        {
            this.reset();
            this.origin.set(p_298099_, p_299019_, p_299020_);
            this.sectionPosition = SectionPos.of(this.origin);

            if (this.renderRegions)
            {
                int i = 8;
                this.regionX = p_298099_ >> i << i;
                this.regionZ = p_299020_ >> i << i;
                this.regionDX = p_298099_ - this.regionX;
                this.regionDY = p_299019_;
                this.regionDZ = p_299020_ - this.regionZ;
            }

            this.bb = new AABB(
                (double)p_298099_, (double)p_299019_, (double)p_299020_, (double)(p_298099_ + 16), (double)(p_299019_ + 16), (double)(p_299020_ + 16)
            );

            for (Direction direction : Direction.VALUES)
            {
                this.relativeOrigins[direction.ordinal()].set(this.origin).move(direction, 16);
            }

            this.renderChunksOffset16Updated = false;
            this.renderChunkNeighboursUpated = false;

            for (int j = 0; j < this.renderChunkNeighbours.length; j++)
            {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.renderChunkNeighbours[j];

                if (sectionrenderdispatcher$rendersection != null)
                {
                    sectionrenderdispatcher$rendersection.renderChunkNeighboursUpated = false;
                }
            }

            this.chunk = null;
            this.boundingBoxParent = null;
        }

        protected double getDistToPlayerSqr()
        {
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            double d0 = this.bb.minX + 8.0 - camera.getPosition().x;
            double d1 = this.bb.minY + 8.0 - camera.getPosition().y;
            double d2 = this.bb.minZ + 8.0 - camera.getPosition().z;
            return d0 * d0 + d1 * d1 + d2 * d2;
        }

        public SectionRenderDispatcher.CompiledSection getCompiled()
        {
            return this.compiled.get();
        }

        private void reset()
        {
            this.cancelTasks();
            this.compiled.set(SectionRenderDispatcher.CompiledSection.UNCOMPILED);
            this.dirty = true;
        }

        public void releaseBuffers()
        {
            this.reset();
            this.buffers.values().forEach(VertexBuffer::close);
        }

        public BlockPos getOrigin()
        {
            return this.origin;
        }

        public void setDirty(boolean p_298731_)
        {
            boolean flag = this.dirty;
            this.dirty = true;
            this.playerChanged = p_298731_ | (flag && this.playerChanged);

            if (this.isWorldPlayerUpdate())
            {
                this.playerUpdate = true;
            }

            if (!flag)
            {
                SectionRenderDispatcher.this.renderer.onChunkRenderNeedsUpdate(this);
            }
        }

        public void setNotDirty()
        {
            this.dirty = false;
            this.playerChanged = false;
            this.playerUpdate = false;
            this.needsBackgroundPriorityUpdate = false;
        }

        public boolean isDirty()
        {
            return this.dirty;
        }

        public boolean isDirtyFromPlayer()
        {
            return this.dirty && this.playerChanged;
        }

        public BlockPos getRelativeOrigin(Direction p_299060_)
        {
            return this.relativeOrigins[p_299060_.ordinal()];
        }

        public boolean resortTransparency(RenderType p_301074_, SectionRenderDispatcher p_298196_)
        {
            SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = this.getCompiled();

            if (this.lastResortTransparencyTask != null)
            {
                this.lastResortTransparencyTask.cancel();
            }

            if (!sectionrenderdispatcher$compiledsection.hasBlocks.contains(p_301074_))
            {
                return false;
            }
            else
            {
                this.lastResortTransparencyTask = new SectionRenderDispatcher.RenderSection.ResortTransparencyTask(this.getDistToPlayerSqr(), sectionrenderdispatcher$compiledsection);
                p_298196_.schedule(this.lastResortTransparencyTask);
                return true;
            }
        }

        protected boolean cancelTasks()
        {
            boolean flag = false;

            if (this.lastRebuildTask != null)
            {
                this.lastRebuildTask.cancel();
                this.lastRebuildTask = null;
                flag = true;
            }

            if (this.lastResortTransparencyTask != null)
            {
                this.lastResortTransparencyTask.cancel();
                this.lastResortTransparencyTask = null;
            }

            return flag;
        }

        public SectionRenderDispatcher.RenderSection.CompileTask createCompileTask(RenderRegionCache p_300037_)
        {
            boolean flag = this.cancelTasks();
            RenderChunkRegion renderchunkregion = p_300037_.createRegion(SectionRenderDispatcher.this.level, SectionPos.of(this.origin));
            boolean flag1 = this.compiled.get() == SectionRenderDispatcher.CompiledSection.UNCOMPILED;

            if (flag1 && flag)
            {
                this.initialCompilationCancelCount.incrementAndGet();
            }

            this.lastRebuildTask = new SectionRenderDispatcher.RenderSection.RebuildTask(
                this, this.getDistToPlayerSqr(), renderchunkregion, !flag1 || this.initialCompilationCancelCount.get() > 2, p_300037_
            );
            return this.lastRebuildTask;
        }

        public void rebuildSectionAsync(SectionRenderDispatcher p_299090_, RenderRegionCache p_297331_)
        {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(p_297331_);
            p_299090_.schedule(sectionrenderdispatcher$rendersection$compiletask);
        }

        void updateGlobalBlockEntities(Collection<BlockEntity> p_300373_)
        {
            Set<BlockEntity> set = Sets.newHashSet(p_300373_);
            Set<BlockEntity> set1;

            synchronized (this.globalBlockEntities)
            {
                set1 = Sets.newHashSet(this.globalBlockEntities);
                set.removeAll(this.globalBlockEntities);
                set1.removeAll(p_300373_);
                this.globalBlockEntities.clear();
                this.globalBlockEntities.addAll(p_300373_);
            }

            SectionRenderDispatcher.this.renderer.updateGlobalBlockEntities(set1, set);
        }

        public void compileSync(RenderRegionCache p_298605_)
        {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(p_298605_);
            sectionrenderdispatcher$rendersection$compiletask.doTask(SectionRenderDispatcher.this.fixedBuffers);
        }

        public boolean isAxisAlignedWith(int p_297900_, int p_299871_, int p_299328_)
        {
            BlockPos blockpos = this.getOrigin();
            return p_297900_ == SectionPos.blockToSectionCoord(blockpos.getX())
                   || p_299328_ == SectionPos.blockToSectionCoord(blockpos.getZ())
                   || p_299871_ == SectionPos.blockToSectionCoord(blockpos.getY());
        }

        void setCompiled(SectionRenderDispatcher.CompiledSection p_343239_)
        {
            this.compiled.set(p_343239_);
            this.initialCompilationCancelCount.set(0);
            SectionRenderDispatcher.this.renderer.addRecentlyCompiledSection(this);
        }

        VertexSorting createVertexSorting()
        {
            Vec3 vec3 = SectionRenderDispatcher.this.getCameraPosition();
            return VertexSorting.byDistance(
                       (float)this.regionDX + (float)(vec3.x - (double)this.origin.getX()),
                       (float)this.regionDY + (float)(vec3.y - (double)this.origin.getY()),
                       (float)this.regionDZ + (float)(vec3.z - (double)this.origin.getZ())
                   );
        }

        private boolean isWorldPlayerUpdate()
        {
            if (SectionRenderDispatcher.this.level instanceof ClientLevel)
            {
                ClientLevel clientlevel = SectionRenderDispatcher.this.level;
                return clientlevel.isPlayerUpdate();
            }
            else
            {
                return false;
            }
        }

        public boolean isPlayerUpdate()
        {
            return this.playerUpdate;
        }

        public void setNeedsBackgroundPriorityUpdate(boolean needsBackgroundPriorityUpdate)
        {
            this.needsBackgroundPriorityUpdate = needsBackgroundPriorityUpdate;
        }

        public boolean needsBackgroundPriorityUpdate()
        {
            return this.needsBackgroundPriorityUpdate;
        }

        public SectionRenderDispatcher.RenderSection getRenderChunkOffset16(ViewArea viewFrustum, Direction facing)
        {
            if (!this.renderChunksOffset16Updated)
            {
                for (int i = 0; i < Direction.VALUES.length; i++)
                {
                    Direction direction = Direction.VALUES[i];
                    BlockPos blockpos = this.getRelativeOrigin(direction);
                    this.renderChunksOfset16[i] = viewFrustum.getRenderSectionAt(blockpos);
                }

                this.renderChunksOffset16Updated = true;
            }

            return this.renderChunksOfset16[facing.ordinal()];
        }

        public LevelChunk getChunk()
        {
            return this.getChunk(this.origin);
        }

        private LevelChunk getChunk(BlockPos posIn)
        {
            LevelChunk levelchunk = this.chunk;

            if (levelchunk != null && ChunkUtils.isLoaded(levelchunk))
            {
                return levelchunk;
            }
            else
            {
                levelchunk = SectionRenderDispatcher.this.level.getChunkAt(posIn);
                this.chunk = levelchunk;
                return levelchunk;
            }
        }

        public boolean isChunkRegionEmpty()
        {
            return this.isChunkRegionEmpty(this.origin);
        }

        private boolean isChunkRegionEmpty(BlockPos posIn)
        {
            int i = posIn.getY();
            int j = i + 15;
            return this.getChunk(posIn).isYSpaceEmpty(i, j);
        }

        public void setRenderChunkNeighbour(Direction facing, SectionRenderDispatcher.RenderSection neighbour)
        {
            this.renderChunkNeighbours[facing.ordinal()] = neighbour;
            this.renderChunkNeighboursValid[facing.ordinal()] = neighbour;
        }

        public SectionRenderDispatcher.RenderSection getRenderChunkNeighbour(Direction facing)
        {
            if (!this.renderChunkNeighboursUpated)
            {
                this.updateRenderChunkNeighboursValid();
            }

            return this.renderChunkNeighboursValid[facing.ordinal()];
        }

        public SectionOcclusionGraph.Node getRenderInfo()
        {
            return this.renderInfo;
        }

        public SectionOcclusionGraph.Node getRenderInfo(Direction dirIn, int counterIn)
        {
            this.renderInfo.initialize(dirIn, counterIn);
            return this.renderInfo;
        }

        private void updateRenderChunkNeighboursValid()
        {
            int i = this.getOrigin().getX();
            int j = this.getOrigin().getZ();
            int k = Direction.NORTH.ordinal();
            int l = Direction.SOUTH.ordinal();
            int i1 = Direction.WEST.ordinal();
            int j1 = Direction.EAST.ordinal();
            this.renderChunkNeighboursValid[k] = this.renderChunkNeighbours[k].getOrigin().getZ() == j - 16 ? this.renderChunkNeighbours[k] : null;
            this.renderChunkNeighboursValid[l] = this.renderChunkNeighbours[l].getOrigin().getZ() == j + 16 ? this.renderChunkNeighbours[l] : null;
            this.renderChunkNeighboursValid[i1] = this.renderChunkNeighbours[i1].getOrigin().getX() == i - 16 ? this.renderChunkNeighbours[i1] : null;
            this.renderChunkNeighboursValid[j1] = this.renderChunkNeighbours[j1].getOrigin().getX() == i + 16 ? this.renderChunkNeighbours[j1] : null;
            this.renderChunkNeighboursUpated = true;
        }

        public boolean isBoundingBoxInFrustum(ICamera camera, int frameCount)
        {
            return this.getBoundingBoxParent().isBoundingBoxInFrustumFully(camera, frameCount) ? true : camera.isBoundingBoxInFrustum(this.bb);
        }

        public AabbFrame getBoundingBoxParent()
        {
            if (this.boundingBoxParent == null)
            {
                BlockPos blockpos = this.getOrigin();
                int i = blockpos.getX();
                int j = blockpos.getY();
                int k = blockpos.getZ();
                int l = 5;
                int i1 = i >> l << l;
                int j1 = j >> l << l;
                int k1 = k >> l << l;

                if (i1 != i || j1 != j || k1 != k)
                {
                    AabbFrame aabbframe = SectionRenderDispatcher.this.renderer.getRenderChunk(new BlockPos(i1, j1, k1)).getBoundingBoxParent();

                    if (aabbframe != null && aabbframe.minX == (double)i1 && aabbframe.minY == (double)j1 && aabbframe.minZ == (double)k1)
                    {
                        this.boundingBoxParent = aabbframe;
                    }
                }

                if (this.boundingBoxParent == null)
                {
                    int l1 = 1 << l;
                    this.boundingBoxParent = new AabbFrame((double)i1, (double)j1, (double)k1, (double)(i1 + l1), (double)(j1 + l1), (double)(k1 + l1));
                }
            }

            return this.boundingBoxParent;
        }

        public ClientLevel getWorld()
        {
            return SectionRenderDispatcher.this.level;
        }

        public SectionPos getSectionPosition()
        {
            return this.sectionPosition;
        }

        @Override
        public String toString()
        {
            return "pos: " + this.getOrigin();
        }

        abstract class CompileTask implements Comparable<SectionRenderDispatcher.RenderSection.CompileTask>
        {
            protected final double distAtCreation;
            protected final AtomicBoolean isCancelled = new AtomicBoolean(false);
            protected final boolean isHighPriority;

            public CompileTask(final double p_300617_, final boolean p_299251_)
            {
                this.distAtCreation = p_300617_;
                this.isHighPriority = p_299251_;
            }

            public abstract CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack p_300298_);

            public abstract void cancel();

            protected abstract String name();

            public int compareTo(SectionRenderDispatcher.RenderSection.CompileTask p_298947_)
            {
                return Doubles.compare(this.distAtCreation, p_298947_.distAtCreation);
            }
        }

        class RebuildTask extends SectionRenderDispatcher.RenderSection.CompileTask
        {
            @Nullable
            protected RenderChunkRegion region;
            private final SectionRenderDispatcher.RenderSection renderSection;
            private final RenderRegionCache renderRegionCache;

            public RebuildTask(
                @Nullable final SectionRenderDispatcher.RenderSection p_298793_,
                final double p_301300_,
                final RenderChunkRegion p_300496_,
                final boolean p_299891_
            )
            {
                this(p_298793_, p_301300_, p_300496_, p_299891_, null);
            }

            public RebuildTask(
                @Nullable final SectionRenderDispatcher.RenderSection renderSection,
                final double distanceSqIn,
                final RenderChunkRegion renderCacheIn,
                final boolean highPriorityIn,
                RenderRegionCache renderRegionCacheIn
            )
            {
                super(distanceSqIn, highPriorityIn);
                this.renderSection = renderSection;
                this.region = renderCacheIn;
                this.renderRegionCache = renderRegionCacheIn;

                if (this.renderRegionCache != null)
                {
                    this.renderRegionCache.compileStarted();
                }
            }

            @Override
            protected String name()
            {
                return "rend_chk_rebuild";
            }

            @Override
            public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack p_299595_)
            {
                try
                {
                    if (this.isCancelled.get())
                    {
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    }

                    if (!this.renderSection.hasAllNeighbors())
                    {
                        this.cancel();
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    }

                    if (this.isCancelled.get())
                    {
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    }

                    RenderChunkRegion renderchunkregion = this.region;
                    this.region = null;

                    if (renderchunkregion == null)
                    {
                        this.renderSection.setCompiled(SectionRenderDispatcher.CompiledSection.EMPTY);
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL);
                    }

                    SectionPos sectionpos = SectionPos.of(this.renderSection.origin);
                    ChunkCacheOF chunkcacheof = renderchunkregion.makeChunkCacheOF();
                    SectionCompiler.Results sectioncompiler$results = SectionRenderDispatcher.this.sectionCompiler
                            .compile(sectionpos, chunkcacheof, this.renderSection.createVertexSorting(), p_299595_, this.renderSection.regionDX, this.renderSection.regionDY, this.renderSection.regionDZ);
                    this.renderSection.updateGlobalBlockEntities(sectioncompiler$results.globalBlockEntities);

                    if (!this.isCancelled.get())
                    {
                        SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsectionx = new SectionRenderDispatcher.CompiledSection();
                        sectionrenderdispatcher$compiledsectionx.visibilitySet = sectioncompiler$results.visibilitySet;
                        sectionrenderdispatcher$compiledsectionx.renderableBlockEntities.addAll(sectioncompiler$results.blockEntities);
                        sectionrenderdispatcher$compiledsectionx.transparencyState = sectioncompiler$results.transparencyState;
                        sectionrenderdispatcher$compiledsectionx.setAnimatedSprites(sectioncompiler$results.animatedSprites);
                        List<CompletableFuture<Void>> list = new ArrayList<>(sectioncompiler$results.renderedLayers.size());
                        sectioncompiler$results.renderedLayers.forEach((renderTypeIn, bufferIn) ->
                        {
                            list.add(SectionRenderDispatcher.this.uploadSectionLayer(bufferIn, this.renderSection.getBuffer(renderTypeIn)));
                            sectionrenderdispatcher$compiledsectionx.hasBlocks.add(renderTypeIn);
                        });
                        return Util.sequenceFailFast(list).handle((voidIn, throwableIn) ->
                        {
                            if (throwableIn != null && !(throwableIn instanceof CancellationException) && !(throwableIn instanceof InterruptedException))
                            {
                                Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwableIn, "Rendering section"));
                            }

                            if (this.isCancelled.get())
                            {
                                return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                            }
                            else {
                                this.renderSection.setCompiled(sectionrenderdispatcher$compiledsectionx);
                                return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                            }
                        });
                    }

                    sectioncompiler$results.release();
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                }
                finally
                {
                    if (this.renderRegionCache != null)
                    {
                        this.renderRegionCache.compileFinished();
                    }
                }
            }

            @Override
            public void cancel()
            {
                this.region = null;

                if (this.isCancelled.compareAndSet(false, true))
                {
                    this.renderSection.setDirty(false);
                }
            }
        }

        class ResortTransparencyTask extends SectionRenderDispatcher.RenderSection.CompileTask
        {
            private final SectionRenderDispatcher.CompiledSection compiledSection;

            public ResortTransparencyTask(final double p_300619_, final SectionRenderDispatcher.CompiledSection p_297742_)
            {
                super(p_300619_, true);
                this.compiledSection = p_297742_;
            }

            @Override
            protected String name()
            {
                return "rend_chk_sort";
            }

            @Override
            public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack p_297366_)
            {
                if (this.isCancelled.get())
                {
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                }
                else if (!RenderSection.this.hasAllNeighbors())
                {
                    this.isCancelled.set(true);
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                }
                else if (this.isCancelled.get())
                {
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                }
                else
                {
                    MeshData.SortState meshdata$sortstate = this.compiledSection.transparencyState;

                    if (meshdata$sortstate != null && !this.compiledSection.isEmpty(RenderType.translucent()))
                    {
                        VertexSorting vertexsorting = RenderSection.this.createVertexSorting();
                        ByteBufferBuilder.Result bytebufferbuilder$result = meshdata$sortstate.buildSortedIndexBuffer(
                                    p_297366_.buffer(RenderType.translucent()), vertexsorting
                                );

                        if (bytebufferbuilder$result == null)
                        {
                            return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                        }
                        else if (this.isCancelled.get())
                        {
                            bytebufferbuilder$result.close();
                            return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                        }
                        else
                        {
                            CompletableFuture<SectionRenderDispatcher.SectionTaskResult> completablefuture = SectionRenderDispatcher.this.uploadSectionIndexBuffer(
                                        bytebufferbuilder$result, RenderSection.this.getBuffer(RenderType.translucent())
                                    )
                                    .thenApply(voidIn -> SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                            return completablefuture.handle(
                                       (taskResultIn, throwableIn) ->
                            {
                                if (throwableIn != null
                                && !(throwableIn instanceof CancellationException)
                                && !(throwableIn instanceof InterruptedException))
                                {
                                    Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwableIn, "Rendering section"));
                                }

                                return this.isCancelled.get()
                                ? SectionRenderDispatcher.SectionTaskResult.CANCELLED
                                : SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                            }
                                   );
                        }
                    }
                    else
                    {
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    }
                }
            }

            @Override
            public void cancel()
            {
                this.isCancelled.set(true);
            }
        }
    }

    static enum SectionTaskResult
    {
        SUCCESSFUL,
        CANCELLED;
    }
}
