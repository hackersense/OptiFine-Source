package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.optifine.reflect.Reflector;
import org.slf4j.Logger;

public abstract class DistanceManager
{
    static final Logger LOGGER = LogUtils.getLogger();
    static final int PLAYER_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);
    private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
    final Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk = new Long2ObjectOpenHashMap<>();
    final Long2ObjectOpenHashMap < SortedArraySet < Ticket<? >>> tickets = new Long2ObjectOpenHashMap<>();
    private final DistanceManager.ChunkTicketTracker ticketTracker = new DistanceManager.ChunkTicketTracker();
    private final DistanceManager.FixedPlayerDistanceChunkTracker naturalSpawnChunkCounter = new DistanceManager.FixedPlayerDistanceChunkTracker(8);
    private final TickingTracker tickingTicketsTracker = new TickingTracker();
    private final DistanceManager.PlayerTicketTracker playerTicketManager = new DistanceManager.PlayerTicketTracker(64);
    final Set<ChunkHolder> chunksToUpdateFutures = Sets.newHashSet();
    final ChunkTaskPriorityQueueSorter ticketThrottler;
    final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> ticketThrottlerInput;
    final ProcessorHandle<ChunkTaskPriorityQueueSorter.Release> ticketThrottlerReleaser;
    final LongSet ticketsToRelease = new LongOpenHashSet();
    final Executor mainThreadExecutor;
    private long ticketTickCounter;
    private int simulationDistance = 10;
    private final Long2ObjectOpenHashMap < SortedArraySet < Ticket<? >>> forcedTickets = new Long2ObjectOpenHashMap<>();

    protected DistanceManager(Executor p_140774_, Executor p_140775_)
    {
        ProcessorHandle<Runnable> processorhandle = ProcessorHandle.of("player ticket throttler", p_140775_::execute);
        ChunkTaskPriorityQueueSorter chunktaskpriorityqueuesorter = new ChunkTaskPriorityQueueSorter(ImmutableList.of(processorhandle), p_140774_, 4);
        this.ticketThrottler = chunktaskpriorityqueuesorter;
        this.ticketThrottlerInput = chunktaskpriorityqueuesorter.getProcessor(processorhandle, true);
        this.ticketThrottlerReleaser = chunktaskpriorityqueuesorter.getReleaseProcessor(processorhandle);
        this.mainThreadExecutor = p_140775_;
    }

    protected void purgeStaleTickets()
    {
        this.ticketTickCounter++;
        ObjectIterator < Entry < SortedArraySet < Ticket<? >>> > objectiterator = this.tickets.long2ObjectEntrySet().fastIterator();

        while (objectiterator.hasNext())
        {
            Entry < SortedArraySet < Ticket<? >>> entry = objectiterator.next();
            Iterator < Ticket<? >> iterator = entry.getValue().iterator();
            boolean flag = false;

            while (iterator.hasNext())
            {
                Ticket<?> ticket = iterator.next();

                if (ticket.timedOut(this.ticketTickCounter))
                {
                    iterator.remove();
                    flag = true;
                    this.tickingTicketsTracker.removeTicket(entry.getLongKey(), ticket);
                }
            }

            if (flag)
            {
                this.ticketTracker.update(entry.getLongKey(), getTicketLevelAt(entry.getValue()), false);
            }

            if (entry.getValue().isEmpty())
            {
                objectiterator.remove();
            }
        }
    }

    private static int getTicketLevelAt(SortedArraySet < Ticket<? >> p_140798_)
    {
        return !p_140798_.isEmpty() ? p_140798_.first().getTicketLevel() : ChunkLevel.MAX_LEVEL + 1;
    }

    protected abstract boolean isChunkToRemove(long p_140779_);

    @Nullable
    protected abstract ChunkHolder getChunk(long p_140817_);

    @Nullable
    protected abstract ChunkHolder updateChunkScheduling(long p_140780_, int p_140781_, @Nullable ChunkHolder p_140782_, int p_140783_);

    public boolean runAllUpdates(ChunkMap p_140806_)
    {
        this.naturalSpawnChunkCounter.runAllUpdates();
        this.tickingTicketsTracker.runAllUpdates();
        this.playerTicketManager.runAllUpdates();
        int i = Integer.MAX_VALUE - this.ticketTracker.runDistanceUpdates(Integer.MAX_VALUE);
        boolean flag = i != 0;

        if (flag)
        {
        }

        if (!this.chunksToUpdateFutures.isEmpty())
        {
            this.chunksToUpdateFutures.forEach(holderIn -> holderIn.updateHighestAllowedStatus(p_140806_));
            this.chunksToUpdateFutures.forEach(holderIn -> holderIn.updateFutures(p_140806_, this.mainThreadExecutor));
            this.chunksToUpdateFutures.clear();
            return true;
        }
        else
        {
            if (!this.ticketsToRelease.isEmpty())
            {
                LongIterator longiterator = this.ticketsToRelease.iterator();

                while (longiterator.hasNext())
                {
                    long j = longiterator.nextLong();

                    if (this.getTickets(j).stream().anyMatch(ticketIn -> ticketIn.getType() == TicketType.PLAYER))
                    {
                        ChunkHolder chunkholder = p_140806_.getUpdatingChunkIfPresent(j);

                        if (chunkholder == null)
                        {
                            throw new IllegalStateException();
                        }

                        CompletableFuture<ChunkResult<LevelChunk>> completablefuture = chunkholder.getEntityTickingChunkFuture();
                        completablefuture.thenAccept(
                            voidIn -> this.mainThreadExecutor.execute(() -> this.ticketThrottlerReleaser.tell(ChunkTaskPriorityQueueSorter.release(() ->
                        {
                        }, j, false)))
                        );
                    }
                }

                this.ticketsToRelease.clear();
            }

            return flag;
        }
    }

    void addTicket(long p_140785_, Ticket<?> p_140786_)
    {
        SortedArraySet < Ticket<? >> sortedarrayset = this.getTickets(p_140785_);
        int i = getTicketLevelAt(sortedarrayset);
        Ticket<?> ticket = sortedarrayset.addOrGet(p_140786_);
        ticket.setCreatedTick(this.ticketTickCounter);

        if (p_140786_.getTicketLevel() < i)
        {
            this.ticketTracker.update(p_140785_, p_140786_.getTicketLevel(), true);
        }

        if (Reflector.callBoolean(p_140786_, Reflector.ForgeTicket_isForceTicks))
        {
            SortedArraySet < Ticket<? >> sortedarrayset1 = this.forcedTickets.computeIfAbsent(p_140785_, e -> SortedArraySet.create(4));
            sortedarrayset1.addOrGet(ticket);
        }
    }

    void removeTicket(long p_140819_, Ticket<?> p_140820_)
    {
        SortedArraySet < Ticket<? >> sortedarrayset = this.getTickets(p_140819_);

        if (sortedarrayset.remove(p_140820_))
        {
        }

        if (sortedarrayset.isEmpty())
        {
            this.tickets.remove(p_140819_);
        }

        this.ticketTracker.update(p_140819_, getTicketLevelAt(sortedarrayset), false);

        if (Reflector.callBoolean(p_140820_, Reflector.ForgeTicket_isForceTicks))
        {
            SortedArraySet < Ticket<? >> sortedarrayset1 = this.forcedTickets.get(p_140819_);

            if (sortedarrayset1 != null)
            {
                sortedarrayset1.remove(p_140820_);
            }
        }
    }

    public <T> void addTicket(TicketType<T> p_140793_, ChunkPos p_140794_, int p_140795_, T p_140796_)
    {
        this.addTicket(p_140794_.toLong(), new Ticket<>(p_140793_, p_140795_, p_140796_));
    }

    public <T> void removeTicket(TicketType<T> p_140824_, ChunkPos p_140825_, int p_140826_, T p_140827_)
    {
        Ticket<T> ticket = new Ticket<>(p_140824_, p_140826_, p_140827_);
        this.removeTicket(p_140825_.toLong(), ticket);
    }

    public <T> void addRegionTicket(TicketType<T> p_140841_, ChunkPos p_140842_, int p_140843_, T p_140844_)
    {
        this.addRegionTicket(p_140841_, p_140842_, p_140843_, p_140844_, false);
    }

    public <T> void addRegionTicket(TicketType<T> type, ChunkPos pos, int distance, T value, boolean forceTicks)
    {
        Ticket<T> ticket = new Ticket<>(type, ChunkLevel.byStatus(FullChunkStatus.FULL) - distance, value);
        Reflector.setFieldValue(ticket, Reflector.ForgeTicket_forceTicks, forceTicks);
        long i = pos.toLong();
        this.addTicket(i, ticket);
        this.tickingTicketsTracker.addTicket(i, ticket);
    }

    public <T> void removeRegionTicket(TicketType<T> p_140850_, ChunkPos p_140851_, int p_140852_, T p_140853_)
    {
        this.removeRegionTicket(p_140850_, p_140851_, p_140852_, p_140853_, false);
    }

    public <T> void removeRegionTicket(TicketType<T> type, ChunkPos pos, int distance, T value, boolean forceTicks)
    {
        Ticket<T> ticket = new Ticket<>(type, ChunkLevel.byStatus(FullChunkStatus.FULL) - distance, value);
        Reflector.setFieldValue(ticket, Reflector.ForgeTicket_forceTicks, forceTicks);
        long i = pos.toLong();
        this.removeTicket(i, ticket);
        this.tickingTicketsTracker.removeTicket(i, ticket);
    }

    private SortedArraySet < Ticket<? >> getTickets(long p_140858_)
    {
        return this.tickets.computeIfAbsent(p_140858_, posIn -> SortedArraySet.create(4));
    }

    protected void updateChunkForced(ChunkPos p_140800_, boolean p_140801_)
    {
        Ticket<ChunkPos> ticket = new Ticket<>(TicketType.FORCED, ChunkMap.FORCED_TICKET_LEVEL, p_140800_);
        long i = p_140800_.toLong();

        if (p_140801_)
        {
            this.addTicket(i, ticket);
            this.tickingTicketsTracker.addTicket(i, ticket);
        }
        else
        {
            this.removeTicket(i, ticket);
            this.tickingTicketsTracker.removeTicket(i, ticket);
        }
    }

    public void addPlayer(SectionPos p_140803_, ServerPlayer p_140804_)
    {
        ChunkPos chunkpos = p_140803_.chunk();
        long i = chunkpos.toLong();
        this.playersPerChunk.computeIfAbsent(i, posIn -> new ObjectOpenHashSet<>()).add(p_140804_);
        this.naturalSpawnChunkCounter.update(i, 0, true);
        this.playerTicketManager.update(i, 0, true);
        this.tickingTicketsTracker.addTicket(TicketType.PLAYER, chunkpos, this.getPlayerTicketLevel(), chunkpos);
    }

    public void removePlayer(SectionPos p_140829_, ServerPlayer p_140830_)
    {
        ChunkPos chunkpos = p_140829_.chunk();
        long i = chunkpos.toLong();
        ObjectSet<ServerPlayer> objectset = this.playersPerChunk.get(i);
        objectset.remove(p_140830_);

        if (objectset.isEmpty())
        {
            this.playersPerChunk.remove(i);
            this.naturalSpawnChunkCounter.update(i, Integer.MAX_VALUE, false);
            this.playerTicketManager.update(i, Integer.MAX_VALUE, false);
            this.tickingTicketsTracker.removeTicket(TicketType.PLAYER, chunkpos, this.getPlayerTicketLevel(), chunkpos);
        }
    }

    private int getPlayerTicketLevel()
    {
        return Math.max(0, ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING) - this.simulationDistance);
    }

    public boolean inEntityTickingRange(long p_183914_)
    {
        return ChunkLevel.isEntityTicking(this.tickingTicketsTracker.getLevel(p_183914_));
    }

    public boolean inBlockTickingRange(long p_183917_)
    {
        return ChunkLevel.isBlockTicking(this.tickingTicketsTracker.getLevel(p_183917_));
    }

    protected String getTicketDebugString(long p_140839_)
    {
        SortedArraySet < Ticket<? >> sortedarrayset = this.tickets.get(p_140839_);
        return sortedarrayset != null && !sortedarrayset.isEmpty() ? sortedarrayset.first().toString() : "no_ticket";
    }

    protected void updatePlayerTickets(int p_140778_)
    {
        this.playerTicketManager.updateViewDistance(p_140778_);
    }

    public void updateSimulationDistance(int p_183912_)
    {
        if (p_183912_ != this.simulationDistance)
        {
            this.simulationDistance = p_183912_;
            this.tickingTicketsTracker.replacePlayerTicketsLevel(this.getPlayerTicketLevel());
        }
    }

    public int getNaturalSpawnChunkCount()
    {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.size();
    }

    public boolean hasPlayersNearby(long p_140848_)
    {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.containsKey(p_140848_);
    }

    public String getDebugStatus()
    {
        return this.ticketThrottler.getDebugStatus();
    }

    private void dumpTickets(String p_143208_)
    {
        try (FileOutputStream fileoutputstream = new FileOutputStream(new File(p_143208_)))
        {
            for (Entry < SortedArraySet < Ticket<? >>> entry : this.tickets.long2ObjectEntrySet())
            {
                ChunkPos chunkpos = new ChunkPos(entry.getLongKey());

                for (Ticket<?> ticket : entry.getValue())
                {
                    fileoutputstream.write(
                        (chunkpos.x + "\t" + chunkpos.z + "\t" + ticket.getType() + "\t" + ticket.getTicketLevel() + "\t\n")
                        .getBytes(StandardCharsets.UTF_8)
                    );
                }
            }
        }
        catch (IOException ioexception1)
        {
            LOGGER.error("Failed to dump tickets to {}", p_143208_, ioexception1);
        }
    }

    @VisibleForTesting
    TickingTracker tickingTracker()
    {
        return this.tickingTicketsTracker;
    }

    public void removeTicketsOnClosing()
    {
        ImmutableSet < TicketType<? >> immutableset = ImmutableSet.of(TicketType.UNKNOWN, TicketType.POST_TELEPORT);
        ObjectIterator < Entry < SortedArraySet < Ticket<? >>> > objectiterator = this.tickets.long2ObjectEntrySet().fastIterator();

        while (objectiterator.hasNext())
        {
            Entry < SortedArraySet < Ticket<? >>> entry = objectiterator.next();
            Iterator < Ticket<? >> iterator = entry.getValue().iterator();
            boolean flag = false;

            while (iterator.hasNext())
            {
                Ticket<?> ticket = iterator.next();

                if (!immutableset.contains(ticket.getType()))
                {
                    iterator.remove();
                    flag = true;
                    this.tickingTicketsTracker.removeTicket(entry.getLongKey(), ticket);
                }
            }

            if (flag)
            {
                this.ticketTracker.update(entry.getLongKey(), getTicketLevelAt(entry.getValue()), false);
            }

            if (entry.getValue().isEmpty())
            {
                objectiterator.remove();
            }
        }
    }

    public boolean hasTickets()
    {
        return !this.tickets.isEmpty();
    }

    public boolean shouldForceTicks(long chunkPos)
    {
        SortedArraySet < Ticket<? >> sortedarrayset = this.forcedTickets.get(chunkPos);
        return sortedarrayset != null && !sortedarrayset.isEmpty();
    }

    class ChunkTicketTracker extends ChunkTracker
    {
        private static final int MAX_LEVEL = ChunkLevel.MAX_LEVEL + 1;

        public ChunkTicketTracker()
        {
            super(MAX_LEVEL + 1, 256, 256);
        }

        @Override
        protected int getLevelFromSource(long p_140883_)
        {
            SortedArraySet < Ticket<? >> sortedarrayset = DistanceManager.this.tickets.get(p_140883_);

            if (sortedarrayset == null)
            {
                return Integer.MAX_VALUE;
            }
            else
            {
                return sortedarrayset.isEmpty() ? Integer.MAX_VALUE : sortedarrayset.first().getTicketLevel();
            }
        }

        @Override
        protected int getLevel(long p_140885_)
        {
            if (!DistanceManager.this.isChunkToRemove(p_140885_))
            {
                ChunkHolder chunkholder = DistanceManager.this.getChunk(p_140885_);

                if (chunkholder != null)
                {
                    return chunkholder.getTicketLevel();
                }
            }

            return MAX_LEVEL;
        }

        @Override
        protected void setLevel(long p_140880_, int p_140881_)
        {
            ChunkHolder chunkholder = DistanceManager.this.getChunk(p_140880_);
            int i = chunkholder == null ? MAX_LEVEL : chunkholder.getTicketLevel();

            if (i != p_140881_)
            {
                chunkholder = DistanceManager.this.updateChunkScheduling(p_140880_, p_140881_, chunkholder, i);

                if (chunkholder != null)
                {
                    DistanceManager.this.chunksToUpdateFutures.add(chunkholder);
                }
            }
        }

        public int runDistanceUpdates(int p_140878_)
        {
            return this.runUpdates(p_140878_);
        }
    }

    class FixedPlayerDistanceChunkTracker extends ChunkTracker
    {
        protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
        protected final int maxDistance;

        protected FixedPlayerDistanceChunkTracker(final int p_140891_)
        {
            super(p_140891_ + 2, 2048, 2048);
            this.maxDistance = p_140891_;
            this.chunks.defaultReturnValue((byte)(p_140891_ + 2));
        }

        @Override
        protected int getLevel(long p_140901_)
        {
            return this.chunks.get(p_140901_);
        }

        @Override
        protected void setLevel(long p_140893_, int p_140894_)
        {
            byte b0;

            if (p_140894_ > this.maxDistance)
            {
                b0 = this.chunks.remove(p_140893_);
            }
            else
            {
                b0 = this.chunks.put(p_140893_, (byte)p_140894_);
            }

            this.onLevelChange(p_140893_, b0, p_140894_);
        }

        protected void onLevelChange(long p_140895_, int p_140896_, int p_140897_)
        {
        }

        @Override
        protected int getLevelFromSource(long p_140899_)
        {
            return this.havePlayer(p_140899_) ? 0 : Integer.MAX_VALUE;
        }

        private boolean havePlayer(long p_140903_)
        {
            ObjectSet<ServerPlayer> objectset = DistanceManager.this.playersPerChunk.get(p_140903_);
            return objectset != null && !objectset.isEmpty();
        }

        public void runAllUpdates()
        {
            this.runUpdates(Integer.MAX_VALUE);
        }

        private void dumpChunks(String p_143213_)
        {
            try (FileOutputStream fileoutputstream = new FileOutputStream(new File(p_143213_)))
            {
                for (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry : this.chunks.long2ByteEntrySet())
                {
                    ChunkPos chunkpos = new ChunkPos(entry.getLongKey());
                    String s = Byte.toString(entry.getByteValue());
                    fileoutputstream.write((chunkpos.x + "\t" + chunkpos.z + "\t" + s + "\n").getBytes(StandardCharsets.UTF_8));
                }
            }
            catch (IOException ioexception1)
            {
                DistanceManager.LOGGER.error("Failed to dump chunks to {}", p_143213_, ioexception1);
            }
        }
    }

    class PlayerTicketTracker extends DistanceManager.FixedPlayerDistanceChunkTracker
    {
        private int viewDistance;
        private final Long2IntMap queueLevels = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
        private final LongSet toUpdate = new LongOpenHashSet();

        protected PlayerTicketTracker(final int p_140910_)
        {
            super(p_140910_);
            this.viewDistance = 0;
            this.queueLevels.defaultReturnValue(p_140910_ + 2);
        }

        @Override
        protected void onLevelChange(long p_140915_, int p_140916_, int p_140917_)
        {
            this.toUpdate.add(p_140915_);
        }

        public void updateViewDistance(int p_140913_)
        {
            for (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry : this.chunks.long2ByteEntrySet())
            {
                byte b0 = entry.getByteValue();
                long i = entry.getLongKey();
                this.onLevelChange(i, b0, this.haveTicketFor(b0), b0 <= p_140913_);
            }

            this.viewDistance = p_140913_;
        }

        private void onLevelChange(long p_140919_, int p_140920_, boolean p_140921_, boolean p_140922_)
        {
            if (p_140921_ != p_140922_)
            {
                Ticket<?> ticket = new Ticket<>(TicketType.PLAYER, DistanceManager.PLAYER_TICKET_LEVEL, new ChunkPos(p_140919_));

                if (p_140922_)
                {
                    DistanceManager.this.ticketThrottlerInput.tell(ChunkTaskPriorityQueueSorter.message(() -> DistanceManager.this.mainThreadExecutor.execute(() ->
                    {
                        if (this.haveTicketFor(this.getLevel(p_140919_)))
                        {
                            DistanceManager.this.addTicket(p_140919_, ticket);
                            DistanceManager.this.ticketsToRelease.add(p_140919_);
                        }
                        else {
                            DistanceManager.this.ticketThrottlerReleaser.tell(ChunkTaskPriorityQueueSorter.release(() -> {
                            }, p_140919_, false));
                        }
                    }), p_140919_, () -> p_140920_));
                }
                else
                {
                    DistanceManager.this.ticketThrottlerReleaser
                    .tell(
                        ChunkTaskPriorityQueueSorter.release(
                            () -> DistanceManager.this.mainThreadExecutor.execute(() -> DistanceManager.this.removeTicket(p_140919_, ticket)), p_140919_, true
                        )
                    );
                }
            }
        }

        @Override
        public void runAllUpdates()
        {
            super.runAllUpdates();

            if (!this.toUpdate.isEmpty())
            {
                LongIterator longiterator = this.toUpdate.iterator();

                while (longiterator.hasNext())
                {
                    long i = longiterator.nextLong();
                    int j = this.queueLevels.get(i);
                    int k = this.getLevel(i);

                    if (j != k)
                    {
                        DistanceManager.this.ticketThrottler.onLevelChange(new ChunkPos(i), () -> this.queueLevels.get(i), k, levelIn ->
                        {
                            if (levelIn >= this.queueLevels.defaultReturnValue())
                            {
                                this.queueLevels.remove(i);
                            }
                            else {
                                this.queueLevels.put(i, levelIn);
                            }
                        });
                        this.onLevelChange(i, k, this.haveTicketFor(j), this.haveTicketFor(k));
                    }
                }

                this.toUpdate.clear();
            }
        }

        private boolean haveTicketFor(int p_140933_)
        {
            return p_140933_ <= this.viewDistance;
        }
    }
}
