package net.minecraft.server.network;

import com.google.common.collect.Comparators;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;

public class PlayerChunkSender
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final float MIN_CHUNKS_PER_TICK = 0.01F;
    public static final float MAX_CHUNKS_PER_TICK = 64.0F;
    private static final float START_CHUNKS_PER_TICK = 9.0F;
    private static final int MAX_UNACKNOWLEDGED_BATCHES = 10;
    private final LongSet pendingChunks = new LongOpenHashSet();
    private final boolean memoryConnection;
    private float desiredChunksPerTick = 9.0F;
    private float batchQuota;
    private int unacknowledgedBatches;
    private int maxUnacknowledgedBatches = 1;

    public PlayerChunkSender(boolean p_300389_)
    {
        this.memoryConnection = p_300389_;
    }

    public void markChunkPendingToSend(LevelChunk p_298187_)
    {
        this.pendingChunks.add(p_298187_.getPos().toLong());
    }

    public void dropChunk(ServerPlayer p_298166_, ChunkPos p_300687_)
    {
        if (!this.pendingChunks.remove(p_300687_.toLong()) && p_298166_.isAlive())
        {
            p_298166_.connection.send(new ClientboundForgetLevelChunkPacket(p_300687_));
        }
    }

    public void sendNextChunks(ServerPlayer p_297274_)
    {
        if (this.unacknowledgedBatches < this.maxUnacknowledgedBatches)
        {
            float f = Math.max(1.0F, this.desiredChunksPerTick);
            this.batchQuota = Math.min(this.batchQuota + this.desiredChunksPerTick, f);

            if (!(this.batchQuota < 1.0F))
            {
                if (!this.pendingChunks.isEmpty())
                {
                    ServerLevel serverlevel = p_297274_.serverLevel();
                    ChunkMap chunkmap = serverlevel.getChunkSource().chunkMap;
                    List<LevelChunk> list = this.collectChunksToSend(chunkmap, p_297274_.chunkPosition());

                    if (!list.isEmpty())
                    {
                        ServerGamePacketListenerImpl servergamepacketlistenerimpl = p_297274_.connection;
                        this.unacknowledgedBatches++;
                        servergamepacketlistenerimpl.send(ClientboundChunkBatchStartPacket.INSTANCE);

                        for (LevelChunk levelchunk : list)
                        {
                            sendChunk(servergamepacketlistenerimpl, serverlevel, levelchunk);
                        }

                        servergamepacketlistenerimpl.send(new ClientboundChunkBatchFinishedPacket(list.size()));
                        this.batchQuota = this.batchQuota - (float)list.size();
                    }
                }
            }
        }
    }

    private static void sendChunk(ServerGamePacketListenerImpl p_299748_, ServerLevel p_298120_, LevelChunk p_297712_)
    {
        p_299748_.send(new ClientboundLevelChunkWithLightPacket(p_297712_, p_298120_.getLightEngine(), null, null));
        ChunkPos chunkpos = p_297712_.getPos();
        DebugPackets.sendPoiPacketsForChunk(p_298120_, chunkpos);
    }

    private List<LevelChunk> collectChunksToSend(ChunkMap p_298180_, ChunkPos p_298514_)
    {
        int i = Mth.floor(this.batchQuota);
        List<LevelChunk> list;

        if (!this.memoryConnection && this.pendingChunks.size() > i)
        {
            list = this.pendingChunks
                   .stream()
                   .collect(Comparators.least(i, Comparator.comparingInt(p_298514_::distanceSquared)))
                   .stream()
                   .mapToLong(Long::longValue)
                   .mapToObj(p_298180_::getChunkToSend)
                   .filter(Objects::nonNull)
                   .toList();
        }
        else
        {
            list = this.pendingChunks
                   .longStream()
                   .mapToObj(p_298180_::getChunkToSend)
                   .filter(Objects::nonNull)
                   .sorted(Comparator.comparingInt(p_299102_ -> p_298514_.distanceSquared(p_299102_.getPos())))
                   .toList();
        }

        for (LevelChunk levelchunk : list)
        {
            this.pendingChunks.remove(levelchunk.getPos().toLong());
        }

        return list;
    }

    public void onChunkBatchReceivedByClient(float p_298238_)
    {
        this.unacknowledgedBatches--;
        this.desiredChunksPerTick = Double.isNaN((double)p_298238_) ? 0.01F : Mth.clamp(p_298238_, 0.01F, 64.0F);

        if (this.unacknowledgedBatches == 0)
        {
            this.batchQuota = 1.0F;
        }

        this.maxUnacknowledgedBatches = 10;
    }

    public boolean isPending(long p_299869_)
    {
        return this.pendingChunks.contains(p_299869_);
    }
}
