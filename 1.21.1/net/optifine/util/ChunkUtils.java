package net.optifine.util;

import net.minecraft.world.level.chunk.LevelChunk;
import net.optifine.ChunkOF;

public class ChunkUtils
{
    public static boolean hasEntities(LevelChunk chunk)
    {
        return chunk instanceof ChunkOF chunkof ? chunkof.hasEntities() : true;
    }

    public static boolean isLoaded(LevelChunk chunk)
    {
        return chunk instanceof ChunkOF chunkof ? chunkof.isLoaded() : false;
    }
}
