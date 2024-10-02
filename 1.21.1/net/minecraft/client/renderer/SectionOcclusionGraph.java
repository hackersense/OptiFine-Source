package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.phys.Vec3;
import net.optifine.BlockPosM;
import net.optifine.Vec3M;
import org.slf4j.Logger;

public class SectionOcclusionGraph
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
    private static final double CEILED_SECTION_DIAGONAL = Math.ceil(Math.sqrt(3.0) * 16.0);
    private boolean needsFullUpdate = true;
    @Nullable
    private Future<?> fullUpdateTask;
    @Nullable
    private ViewArea viewArea;
    private final AtomicReference<SectionOcclusionGraph.GraphState> currentGraph = new AtomicReference<>();
    private final AtomicReference<SectionOcclusionGraph.GraphEvents> nextGraphEvents = new AtomicReference<>();
    private final AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);
    private LevelRenderer levelRenderer;

    public void waitAndReset(@Nullable ViewArea p_298923_)
    {
        if (this.fullUpdateTask != null)
        {
            try
            {
                this.fullUpdateTask.get();
                this.fullUpdateTask = null;
            }
            catch (Exception exception)
            {
                LOGGER.warn("Full update failed", (Throwable)exception);
            }
        }

        this.viewArea = p_298923_;
        this.levelRenderer = Minecraft.getInstance().levelRenderer;

        if (p_298923_ != null)
        {
            this.currentGraph.set(new SectionOcclusionGraph.GraphState(p_298923_.sections.length));
            this.invalidate();
        }
        else
        {
            this.currentGraph.set(null);
        }
    }

    public void invalidate()
    {
        this.needsFullUpdate = true;
    }

    public void addSectionsInFrustum(Frustum p_299761_, List<SectionRenderDispatcher.RenderSection> p_301346_)
    {
        this.addSectionsInFrustum(p_299761_, p_301346_, true, -1);
    }

    public void addSectionsInFrustum(Frustum frustumIn, List<SectionRenderDispatcher.RenderSection> sectionsIn, boolean updateSections, int maxChunkDistance)
    {
        List<SectionRenderDispatcher.RenderSection> list = this.levelRenderer.getRenderInfosTerrain();
        List<SectionRenderDispatcher.RenderSection> list1 = this.levelRenderer.getRenderInfosTileEntities();
        int i = (int)frustumIn.getCameraX() >> 4 << 4;
        int j = (int)frustumIn.getCameraY() >> 4 << 4;
        int k = (int)frustumIn.getCameraZ() >> 4 << 4;
        int l = maxChunkDistance * maxChunkDistance;

        for (SectionOcclusionGraph.Node sectionocclusiongraph$node : this.currentGraph.get().storage().renderSections)
        {
            if (frustumIn.isVisible(sectionocclusiongraph$node.section.getBoundingBox()))
            {
                if (maxChunkDistance > 0)
                {
                    BlockPos blockpos = sectionocclusiongraph$node.section.getOrigin();
                    int i1 = i - blockpos.getX();
                    int j1 = j - blockpos.getY();
                    int k1 = k - blockpos.getZ();
                    int l1 = i1 * i1 + j1 * j1 + k1 * k1;

                    if (l1 > l)
                    {
                        continue;
                    }
                }

                if (updateSections)
                {
                    sectionsIn.add(sectionocclusiongraph$node.section);
                }

                SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = sectionocclusiongraph$node.section.getCompiled();

                if (!sectionrenderdispatcher$compiledsection.hasNoRenderableLayers())
                {
                    list.add(sectionocclusiongraph$node.section);
                }

                if (!sectionrenderdispatcher$compiledsection.getRenderableBlockEntities().isEmpty())
                {
                    list1.add(sectionocclusiongraph$node.section);
                }
            }
        }
    }

    public boolean consumeFrustumUpdate()
    {
        return this.needsFrustumUpdate.compareAndSet(true, false);
    }

    public void onChunkLoaded(ChunkPos p_299612_)
    {
        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents = this.nextGraphEvents.get();

        if (sectionocclusiongraph$graphevents != null)
        {
            this.addNeighbors(sectionocclusiongraph$graphevents, p_299612_);
        }

        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents1 = this.currentGraph.get().events;

        if (sectionocclusiongraph$graphevents1 != sectionocclusiongraph$graphevents)
        {
            this.addNeighbors(sectionocclusiongraph$graphevents1, p_299612_);
        }
    }

    public void onSectionCompiled(SectionRenderDispatcher.RenderSection p_301377_)
    {
        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents = this.nextGraphEvents.get();

        if (sectionocclusiongraph$graphevents != null)
        {
            sectionocclusiongraph$graphevents.sectionsToPropagateFrom.add(p_301377_);
        }

        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents1 = this.currentGraph.get().events;

        if (sectionocclusiongraph$graphevents1 != sectionocclusiongraph$graphevents)
        {
            sectionocclusiongraph$graphevents1.sectionsToPropagateFrom.add(p_301377_);
        }

        if (p_301377_.getCompiled().hasTerrainBlockEntities())
        {
            this.needsFrustumUpdate.set(true);
        }
    }

    public void update(boolean p_301275_, Camera p_298972_, Frustum p_298939_, List<SectionRenderDispatcher.RenderSection> p_300432_)
    {
        Vec3 vec3 = p_298972_.getPosition();

        if (this.needsFullUpdate && (this.fullUpdateTask == null || this.fullUpdateTask.isDone()))
        {
            this.scheduleFullUpdate(p_301275_, p_298972_, vec3);
        }

        this.runPartialUpdate(p_301275_, p_298939_, p_300432_, vec3);
    }

    private void scheduleFullUpdate(boolean p_298569_, Camera p_299582_, Vec3 p_297830_)
    {
        this.needsFullUpdate = false;
        this.fullUpdateTask = Util.backgroundExecutor().submit(() ->
        {
            SectionOcclusionGraph.GraphState sectionocclusiongraph$graphstate = new SectionOcclusionGraph.GraphState(this.viewArea.sections.length);
            this.nextGraphEvents.set(sectionocclusiongraph$graphstate.events);
            Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();
            this.initializeQueueForFullUpdate(p_299582_, queue);
            queue.forEach(nodeIn -> sectionocclusiongraph$graphstate.storage.sectionToNodeMap.put(nodeIn.section, nodeIn));
            this.runUpdates(sectionocclusiongraph$graphstate.storage, p_297830_, queue, p_298569_, sectionIn -> {
            });
            this.currentGraph.set(sectionocclusiongraph$graphstate);
            this.nextGraphEvents.set(null);
            this.needsFrustumUpdate.set(true);
        });
    }

    private void runPartialUpdate(boolean p_298388_, Frustum p_299940_, List<SectionRenderDispatcher.RenderSection> p_297967_, Vec3 p_299094_)
    {
        SectionOcclusionGraph.GraphState sectionocclusiongraph$graphstate = this.currentGraph.get();
        this.queueSectionsWithNewNeighbors(sectionocclusiongraph$graphstate);

        if (!sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.isEmpty())
        {
            Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();

            while (!sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.isEmpty())
            {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.poll();
                SectionOcclusionGraph.Node sectionocclusiongraph$node = sectionocclusiongraph$graphstate.storage
                        .sectionToNodeMap
                        .get(sectionrenderdispatcher$rendersection);

                if (sectionocclusiongraph$node != null && sectionocclusiongraph$node.section == sectionrenderdispatcher$rendersection)
                {
                    queue.add(sectionocclusiongraph$node);
                }
            }

            List<SectionRenderDispatcher.RenderSection> list1 = this.levelRenderer.getRenderInfos();
            List<SectionRenderDispatcher.RenderSection> list2 = this.levelRenderer.getRenderInfosTerrain();
            List<SectionRenderDispatcher.RenderSection> list = this.levelRenderer.getRenderInfosTileEntities();
            Frustum frustum = LevelRenderer.offsetFrustum(p_299940_);
            Consumer<SectionRenderDispatcher.RenderSection> consumer = sectionIn ->
            {
                if (frustum.isVisible(sectionIn.getBoundingBox()))
                {
                    p_297967_.add(sectionIn);

                    if (sectionIn == list1)
                    {
                        SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = sectionIn.compiled.get();

                        if (!sectionrenderdispatcher$compiledsection.hasNoRenderableLayers())
                        {
                            list2.add(sectionIn);
                        }

                        if (!sectionrenderdispatcher$compiledsection.getRenderableBlockEntities().isEmpty())
                        {
                            list.add(sectionIn);
                        }
                    }
                }
            };
            this.runUpdates(sectionocclusiongraph$graphstate.storage, p_299094_, queue, p_298388_, consumer);
        }
    }

    private void queueSectionsWithNewNeighbors(SectionOcclusionGraph.GraphState p_298801_)
    {
        LongIterator longiterator = p_298801_.events.chunksWhichReceivedNeighbors.iterator();

        while (longiterator.hasNext())
        {
            long i = longiterator.nextLong();
            List<SectionRenderDispatcher.RenderSection> list = p_298801_.storage.chunksWaitingForNeighbors.get(i);

            if (list != null && list.get(0).hasAllNeighbors())
            {
                p_298801_.events.sectionsToPropagateFrom.addAll(list);
                p_298801_.storage.chunksWaitingForNeighbors.remove(i);
            }
        }

        p_298801_.events.chunksWhichReceivedNeighbors.clear();
    }

    private void addNeighbors(SectionOcclusionGraph.GraphEvents p_300825_, ChunkPos p_297758_)
    {
        p_300825_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_297758_.x - 1, p_297758_.z));
        p_300825_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_297758_.x, p_297758_.z - 1));
        p_300825_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_297758_.x + 1, p_297758_.z));
        p_300825_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_297758_.x, p_297758_.z + 1));
    }

    private void initializeQueueForFullUpdate(Camera p_298889_, Queue<SectionOcclusionGraph.Node> p_297605_)
    {
        int i = 16;
        Vec3 vec3 = p_298889_.getPosition();
        BlockPos blockpos = p_298889_.getBlockPosition();
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSectionAt(blockpos);

        if (sectionrenderdispatcher$rendersection == null)
        {
            LevelHeightAccessor levelheightaccessor = this.viewArea.getLevelHeightAccessor();
            boolean flag = blockpos.getY() > levelheightaccessor.getMinBuildHeight();
            int j = flag ? levelheightaccessor.getMaxBuildHeight() - 8 : levelheightaccessor.getMinBuildHeight() + 8;
            int k = Mth.floor(vec3.x / 16.0) * 16;
            int l = Mth.floor(vec3.z / 16.0) * 16;
            int i1 = this.viewArea.getViewDistance();
            List<SectionOcclusionGraph.Node> list = Lists.newArrayList();

            for (int j1 = -i1; j1 <= i1; j1++)
            {
                for (int k1 = -i1; k1 <= i1; k1++)
                {
                    SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.viewArea
                            .getRenderSectionAt(new BlockPos(k + SectionPos.sectionToBlockCoord(j1, 8), j, l + SectionPos.sectionToBlockCoord(k1, 8)));

                    if (sectionrenderdispatcher$rendersection1 != null && this.isInViewDistance(blockpos, sectionrenderdispatcher$rendersection1.getOrigin()))
                    {
                        Direction direction = flag ? Direction.DOWN : Direction.UP;
                        SectionOcclusionGraph.Node sectionocclusiongraph$node = sectionrenderdispatcher$rendersection1.getRenderInfo(direction, 0);
                        sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, direction);

                        if (j1 > 0)
                        {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.EAST);
                        }
                        else if (j1 < 0)
                        {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.WEST);
                        }

                        if (k1 > 0)
                        {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.SOUTH);
                        }
                        else if (k1 < 0)
                        {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.NORTH);
                        }

                        list.add(sectionocclusiongraph$node);
                    }
                }
            }

            list.sort(Comparator.comparingDouble(nodeIn -> blockpos.distSqr(nodeIn.section.getOrigin().offset(8, 8, 8))));
            p_297605_.addAll(list);
        }
        else
        {
            p_297605_.add(sectionrenderdispatcher$rendersection.getRenderInfo(null, 0));
        }
    }

    private void runUpdates(
        SectionOcclusionGraph.GraphStorage p_299200_,
        Vec3 p_300018_,
        Queue<SectionOcclusionGraph.Node> p_300570_,
        boolean p_300892_,
        Consumer<SectionRenderDispatcher.RenderSection> p_298647_
    )
    {
        int i = 16;
        BlockPos blockpos = new BlockPos(
            Mth.floor(p_300018_.x / 16.0) * 16, Mth.floor(p_300018_.y / 16.0) * 16, Mth.floor(p_300018_.z / 16.0) * 16
        );
        BlockPos blockpos1 = blockpos.offset(8, 8, 8);

        while (!p_300570_.isEmpty())
        {
            SectionOcclusionGraph.Node sectionocclusiongraph$node = p_300570_.poll();
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = sectionocclusiongraph$node.section;

            if (p_299200_.renderSections.add(sectionocclusiongraph$node))
            {
                p_298647_.accept(sectionocclusiongraph$node.section);
            }

            boolean flag = Math.abs(sectionrenderdispatcher$rendersection.getOrigin().getX() - blockpos.getX()) > 60
                           || Math.abs(sectionrenderdispatcher$rendersection.getOrigin().getY() - blockpos.getY()) > 60
                           || Math.abs(sectionrenderdispatcher$rendersection.getOrigin().getZ() - blockpos.getZ()) > 60;

            for (Direction direction : DIRECTIONS)
            {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.getRelativeFrom(
                            blockpos, sectionrenderdispatcher$rendersection, direction
                        );

                if (sectionrenderdispatcher$rendersection1 != null && (!p_300892_ || !sectionocclusiongraph$node.hasDirection(direction.getOpposite())))
                {
                    if (p_300892_ && sectionocclusiongraph$node.hasSourceDirections())
                    {
                        SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = sectionrenderdispatcher$rendersection.getCompiled();
                        boolean flag1 = false;

                        for (int j = 0; j < DIRECTIONS.length; j++)
                        {
                            if (sectionocclusiongraph$node.hasSourceDirection(j)
                                    && sectionrenderdispatcher$compiledsection.facesCanSeeEachother(DIRECTIONS[j].getOpposite(), direction))
                            {
                                flag1 = true;
                                break;
                            }
                        }

                        if (!flag1)
                        {
                            continue;
                        }
                    }

                    if (p_300892_ && flag)
                    {
                        BlockPos blockpos2 = sectionrenderdispatcher$rendersection1.getOrigin();
                        int l = (
                                    direction.getAxis() == Direction.Axis.X
                                    ? blockpos1.getX() > blockpos2.getX()
                                    : blockpos1.getX() < blockpos2.getX()
                                )
                                ? 16
                                : 0;
                        int i1 = (
                                     direction.getAxis() == Direction.Axis.Y
                                     ? blockpos1.getY() > blockpos2.getY()
                                     : blockpos1.getY() < blockpos2.getY()
                                 )
                                 ? 16
                                 : 0;
                        int k = (
                                    direction.getAxis() == Direction.Axis.Z
                                    ? blockpos1.getZ() > blockpos2.getZ()
                                    : blockpos1.getZ() < blockpos2.getZ()
                                )
                                ? 16
                                : 0;
                        Vec3M vec3m = p_299200_.vec3M1
                                      .set((double)(blockpos2.getX() + l), (double)(blockpos2.getY() + i1), (double)(blockpos2.getZ() + k));
                        Vec3M vec3m1 = p_299200_.vec3M2.set(p_300018_).subtract(vec3m).normalize().scale(CEILED_SECTION_DIAGONAL);
                        boolean flag2 = true;

                        while (p_299200_.vec3M3.set(p_300018_).subtract(vec3m).lengthSqr() > 3600.0)
                        {
                            vec3m = vec3m.add(vec3m1);
                            LevelHeightAccessor levelheightaccessor = this.viewArea.getLevelHeightAccessor();

                            if (vec3m.y > (double)levelheightaccessor.getMaxBuildHeight() || vec3m.y < (double)levelheightaccessor.getMinBuildHeight())
                            {
                                break;
                            }

                            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection2 = this.viewArea
                                    .getRenderSectionAt(p_299200_.blockPosM1.setXyz(vec3m.x, vec3m.y, vec3m.z));

                            if (sectionrenderdispatcher$rendersection2 == null || p_299200_.sectionToNodeMap.get(sectionrenderdispatcher$rendersection2) == null
                               )
                            {
                                flag2 = false;
                                break;
                            }
                        }

                        if (!flag2)
                        {
                            continue;
                        }
                    }

                    SectionOcclusionGraph.Node sectionocclusiongraph$node1 = p_299200_.sectionToNodeMap.get(sectionrenderdispatcher$rendersection1);

                    if (sectionocclusiongraph$node1 != null)
                    {
                        sectionocclusiongraph$node1.addSourceDirection(direction);
                    }
                    else
                    {
                        SectionOcclusionGraph.Node sectionocclusiongraph$node2 = sectionrenderdispatcher$rendersection1.getRenderInfo(
                                    direction, sectionocclusiongraph$node.step + 1
                                );
                        sectionocclusiongraph$node2.setDirections(sectionocclusiongraph$node.directions, direction);

                        if (sectionrenderdispatcher$rendersection1.hasAllNeighbors())
                        {
                            p_300570_.add(sectionocclusiongraph$node2);
                            p_299200_.sectionToNodeMap.put(sectionrenderdispatcher$rendersection1, sectionocclusiongraph$node2);
                        }
                        else if (this.isInViewDistance(blockpos, sectionrenderdispatcher$rendersection1.getOrigin()))
                        {
                            p_299200_.sectionToNodeMap.put(sectionrenderdispatcher$rendersection1, sectionocclusiongraph$node2);
                            p_299200_.chunksWaitingForNeighbors
                            .computeIfAbsent(ChunkPos.asLong(sectionrenderdispatcher$rendersection1.getOrigin()), posLongIn -> new ArrayList<>())
                            .add(sectionrenderdispatcher$rendersection1);
                        }
                    }
                }
            }
        }
    }

    private boolean isInViewDistance(BlockPos p_298566_, BlockPos p_298230_)
    {
        int i = SectionPos.blockToSectionCoord(p_298566_.getX());
        int j = SectionPos.blockToSectionCoord(p_298566_.getZ());
        int k = SectionPos.blockToSectionCoord(p_298230_.getX());
        int l = SectionPos.blockToSectionCoord(p_298230_.getZ());
        return ChunkTrackingView.isInViewDistance(i, j, this.viewArea.getViewDistance(), k, l);
    }

    @Nullable
    private SectionRenderDispatcher.RenderSection getRelativeFrom(BlockPos p_298318_, SectionRenderDispatcher.RenderSection p_299737_, Direction p_301139_)
    {
        BlockPos blockpos = p_299737_.getRelativeOrigin(p_301139_);
        ClientLevel clientlevel = this.levelRenderer.level;

        if (blockpos.getY() < clientlevel.getMinBuildHeight() || blockpos.getY() >= clientlevel.getMaxBuildHeight())
        {
            return null;
        }
        else if (Mth.abs(p_298318_.getY() - blockpos.getY()) > this.levelRenderer.renderDistance)
        {
            return null;
        }
        else
        {
            int i = p_298318_.getX() - blockpos.getX();
            int j = p_298318_.getZ() - blockpos.getZ();
            int k = i * i + j * j;
            return k > this.levelRenderer.renderDistanceXZSq ? null : this.viewArea.getRenderSectionAt(blockpos);
        }
    }

    @Nullable
    @VisibleForDebug
    protected SectionOcclusionGraph.Node getNode(SectionRenderDispatcher.RenderSection p_299335_)
    {
        return this.currentGraph.get().storage.sectionToNodeMap.get(p_299335_);
    }

    public boolean needsFrustumUpdate()
    {
        return this.needsFrustumUpdate.get();
    }

    public void setNeedsFrustumUpdate(boolean val)
    {
        this.needsFrustumUpdate.set(val);
    }

    static record GraphEvents(LongSet chunksWhichReceivedNeighbors, BlockingQueue<SectionRenderDispatcher.RenderSection> sectionsToPropagateFrom)
    {
        public GraphEvents()
        {
            this(new LongOpenHashSet(), new LinkedBlockingQueue<>());
        }
    }

    static record GraphState(SectionOcclusionGraph.GraphStorage storage, SectionOcclusionGraph.GraphEvents events)
    {
        public GraphState(int p_298520_)
        {
            this(new SectionOcclusionGraph.GraphStorage(p_298520_), new SectionOcclusionGraph.GraphEvents());
        }
    }

    static class GraphStorage
    {
        public final SectionOcclusionGraph.SectionToNodeMap sectionToNodeMap;
        public final Set<SectionOcclusionGraph.Node> renderSections;
        public final Long2ObjectMap<List<SectionRenderDispatcher.RenderSection>> chunksWaitingForNeighbors;
        public final Vec3M vec3M1 = new Vec3M(0.0, 0.0, 0.0);
        public final Vec3M vec3M2 = new Vec3M(0.0, 0.0, 0.0);
        public final Vec3M vec3M3 = new Vec3M(0.0, 0.0, 0.0);
        public final BlockPosM blockPosM1 = new BlockPosM();

        public GraphStorage(int p_299453_)
        {
            this.sectionToNodeMap = new SectionOcclusionGraph.SectionToNodeMap(p_299453_);
            this.renderSections = new ObjectLinkedOpenHashSet<>(p_299453_);
            this.chunksWaitingForNeighbors = new Long2ObjectOpenHashMap<>();
        }

        @Override
        public String toString()
        {
            return "sectionToNode: " + this.sectionToNodeMap + ", renderSections: " + this.renderSections + ", sectionsWaiting: " + this.chunksWaitingForNeighbors;
        }
    }

    @VisibleForDebug
    public static class Node
    {
        @VisibleForDebug
        public final SectionRenderDispatcher.RenderSection section;
        private int sourceDirections;
        int directions;
        @VisibleForDebug
        protected int step;

        public Node(SectionRenderDispatcher.RenderSection p_299649_, @Nullable Direction p_299325_, int p_298364_)
        {
            this.section = p_299649_;

            if (p_299325_ != null)
            {
                this.addSourceDirection(p_299325_);
            }

            this.step = p_298364_;
        }

        void setDirections(int directionsIn, Direction directionIn)
        {
            this.directions = this.directions | directionsIn | 1 << directionIn.ordinal();
        }

        public void initialize(Direction facingIn, int counter)
        {
            this.sourceDirections = facingIn != null ? 1 << facingIn.ordinal() : 0;
            this.directions = 0;
            this.step = counter;
        }

        @Override
        public String toString()
        {
            return this.section.getOrigin() + "";
        }

        boolean hasDirection(Direction p_299145_)
        {
            return (this.directions & 1 << p_299145_.ordinal()) > 0;
        }

        void addSourceDirection(Direction p_299877_)
        {
            this.sourceDirections = (byte)(this.sourceDirections | this.sourceDirections | 1 << p_299877_.ordinal());
        }

        @VisibleForDebug
        protected boolean hasSourceDirection(int p_301075_)
        {
            return (this.sourceDirections & 1 << p_301075_) > 0;
        }

        boolean hasSourceDirections()
        {
            return this.sourceDirections != 0;
        }

        @Override
        public int hashCode()
        {
            return this.section.getOrigin().hashCode();
        }

        @Override
        public boolean equals(Object p_300561_)
        {
            return p_300561_ instanceof SectionOcclusionGraph.Node sectionocclusiongraph$node
                   ? this.section.getOrigin().equals(sectionocclusiongraph$node.section.getOrigin())
                   : false;
        }
    }

    static class SectionToNodeMap
    {
        private final SectionOcclusionGraph.Node[] nodes;

        SectionToNodeMap(int p_298573_)
        {
            this.nodes = new SectionOcclusionGraph.Node[p_298573_];
        }

        public void put(SectionRenderDispatcher.RenderSection p_297513_, SectionOcclusionGraph.Node p_298532_)
        {
            this.nodes[p_297513_.index] = p_298532_;
        }

        @Nullable
        public SectionOcclusionGraph.Node get(SectionRenderDispatcher.RenderSection p_297749_)
        {
            int i = p_297749_.index;
            return i >= 0 && i < this.nodes.length ? this.nodes[i] : null;
        }
    }
}
