package net.optifine.util;

import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.MemoryUtil.MemoryAllocator;

public class TestBuffers
{
    private static MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(true);

    public static void main(String[] args) throws Exception
    {
        int i = 1000000;

        for (int j = 0; j < i; j++)
        {
            long k = allocate(1000L);
            testBuf(k, 1000);
            testBuf(k, 1000);
            testBuf(k, 1000);
            testBuf(k, 1000);
            testBuf(k, 1000);
            testBuf(k, 1000);
            testBuf(k, 1000);
            testBuf(k, 1000);
            dbg("Mem: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L);
            free(k);
        }
    }

    private static void testBuf(long ptr, int size)
    {
        ByteBuffer bytebuffer = MemoryUtil.memByteBuffer(ptr, size);
    }

    private static long allocate(long capacityIn)
    {
        long i = ALLOCATOR.malloc(capacityIn);
        dbg("Alloc: " + i);
        return i;
    }

    private static long free(long ptr)
    {
        ALLOCATOR.free(ptr);
        dbg("Free: " + ptr);
        return ptr;
    }

    private static void dbg(String str)
    {
        System.out.println(str);
    }
}
