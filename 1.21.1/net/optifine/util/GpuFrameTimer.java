package net.optifine.util;

import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL32C;

public class GpuFrameTimer
{
    private static boolean timerQuerySupported = GL.getCapabilities().GL_ARB_timer_query;
    private static long lastTimeCpuNs = 0L;
    private static long frameTimeCpuNs = 0L;
    private static GpuFrameTimer.TimerQuery timerQuery;
    private static long frameTimeGpuNs = 0L;
    private static long lastTimeActiveMs = 0L;

    public static void startRender()
    {
        if (timerQuery != null && timerQuery.hasResult())
        {
            long i = timerQuery.getResult();
            frameTimeGpuNs = (frameTimeGpuNs + i) / 2L;
            timerQuery = null;
        }

        if (System.currentTimeMillis() <= lastTimeActiveMs + 1000L)
        {
            long k = System.nanoTime();

            if (lastTimeCpuNs != 0L)
            {
                long j = k - lastTimeCpuNs;
                frameTimeCpuNs = (frameTimeCpuNs + j) / 2L;
            }

            lastTimeCpuNs = k;

            if (timerQuery == null && timerQuerySupported)
            {
                timerQuery = new GpuFrameTimer.TimerQuery();
                timerQuery.start();
            }
        }
    }

    public static void finishRender()
    {
        if (timerQuery != null)
        {
            timerQuery.finish();
        }
    }

    public static double getGpuLoad()
    {
        lastTimeActiveMs = System.currentTimeMillis();
        return (double)Math.max(frameTimeGpuNs, 0L) / Math.max((double)frameTimeCpuNs, 1.0);
    }

    private static class TimerQuery
    {
        private int[] queries = new int[2];
        private boolean[] executed = new boolean[2];
        private long result = -1L;

        public TimerQuery()
        {
            GL32C.glGenQueries(this.queries);
        }

        public void start()
        {
            if (!this.executed[0])
            {
                ARBTimerQuery.glQueryCounter(this.queries[0], 36392);
                this.executed[0] = true;
            }
        }

        public void finish()
        {
            if (!this.executed[1])
            {
                ARBTimerQuery.glQueryCounter(this.queries[1], 36392);
                this.executed[1] = true;
            }
        }

        public boolean hasResult()
        {
            int i = GL32C.glGetQueryObjecti(this.queries[1], 34919);
            return i == 1;
        }

        public long getResult()
        {
            if (this.queries[1] > 0)
            {
                long i = ARBTimerQuery.glGetQueryObjectui64(this.queries[0], 34918);
                long j = ARBTimerQuery.glGetQueryObjectui64(this.queries[1], 34918);
                this.result = j - i;
                GL32C.glDeleteQueries(this.queries);
                this.queries[0] = 0;
                this.queries[1] = 0;
            }

            return this.result;
        }
    }
}
