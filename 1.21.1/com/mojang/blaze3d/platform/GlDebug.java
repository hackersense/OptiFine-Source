package com.mojang.blaze3d.platform;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.optifine.Config;
import net.optifine.GlErrors;
import net.optifine.util.ArrayUtils;
import net.optifine.util.StrUtils;
import net.optifine.util.TimedEvent;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageARBCallback;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebug;
import org.slf4j.Logger;

public class GlDebug
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CIRCULAR_LOG_SIZE = 10;
    private static final Queue<GlDebug.LogEntry> MESSAGE_BUFFER = EvictingQueue.create(10);
    @Nullable
    private static volatile GlDebug.LogEntry lastEntry;
    private static final List<Integer> DEBUG_LEVELS = ImmutableList.of(37190, 37191, 37192, 33387);
    private static final List<Integer> DEBUG_LEVELS_ARB = ImmutableList.of(37190, 37191, 37192);
    private static boolean debugEnabled;
    private static int[] ignoredErrors = makeIgnoredErrors();

    private static int[] makeIgnoredErrors()
    {
        String s = System.getProperty("gl.ignore.errors");

        if (s == null)
        {
            return new int[0];
        }
        else
        {
            String[] astring = Config.tokenize(s, ",");
            int[] aint = new int[0];

            for (int i = 0; i < astring.length; i++)
            {
                String s1 = astring[i].trim();
                int j = s1.startsWith("0x") ? Config.parseHexInt(s1, -1) : Config.parseInt(s1, -1);

                if (j < 0)
                {
                    Config.warn("Invalid error id: " + s1);
                }
                else
                {
                    Config.log("Ignore OpenGL error: " + j);
                    aint = ArrayUtils.addIntToArray(aint, j);
                }
            }

            return aint;
        }
    }

    private static String printUnknownToken(int p_84037_)
    {
        return "Unknown (0x" + Integer.toHexString(p_84037_).toUpperCase() + ")";
    }

    public static String sourceToString(int p_84056_)
    {
        switch (p_84056_)
        {
            case 33350:
                return "API";

            case 33351:
                return "WINDOW SYSTEM";

            case 33352:
                return "SHADER COMPILER";

            case 33353:
                return "THIRD PARTY";

            case 33354:
                return "APPLICATION";

            case 33355:
                return "OTHER";

            default:
                return printUnknownToken(p_84056_);
        }
    }

    public static String typeToString(int p_84058_)
    {
        switch (p_84058_)
        {
            case 33356:
                return "ERROR";

            case 33357:
                return "DEPRECATED BEHAVIOR";

            case 33358:
                return "UNDEFINED BEHAVIOR";

            case 33359:
                return "PORTABILITY";

            case 33360:
                return "PERFORMANCE";

            case 33361:
                return "OTHER";

            case 33384:
                return "MARKER";

            default:
                return printUnknownToken(p_84058_);
        }
    }

    public static String severityToString(int p_84060_)
    {
        switch (p_84060_)
        {
            case 33387:
                return "NOTIFICATION";

            case 37190:
                return "HIGH";

            case 37191:
                return "MEDIUM";

            case 37192:
                return "LOW";

            default:
                return printUnknownToken(p_84060_);
        }
    }

    private static void printDebugLog(int p_84039_, int p_84040_, int p_84041_, int p_84042_, int p_84043_, long p_84044_, long p_84045_)
    {
        if (p_84040_ != 33385 && p_84040_ != 33386)
        {
            if (!ArrayUtils.contains(ignoredErrors, p_84041_))
            {
                if (!Config.isShaders() || p_84039_ != 33352)
                {
                    Minecraft minecraft = Minecraft.getInstance();

                    if (minecraft == null || minecraft.getWindow() == null || !minecraft.getWindow().isClosed())
                    {
                        if (GlErrors.isEnabled(p_84041_))
                        {
                            String s = sourceToString(p_84039_);
                            String s1 = typeToString(p_84040_);
                            String s2 = severityToString(p_84042_);
                            String s3 = GLDebugMessageCallback.getMessage(p_84043_, p_84044_);
                            s3 = StrUtils.trim(s3, " \n\r\t");
                            String s4 = String.format("OpenGL %s %s: %s (%s)", s, s1, p_84041_, s3);
                            Exception exception = new Exception("Stack trace");
                            StackTraceElement[] astacktraceelement = exception.getStackTrace();
                            StackTraceElement[] astacktraceelement1 = astacktraceelement.length > 2
                                    ? Arrays.copyOfRange(astacktraceelement, 2, astacktraceelement.length)
                                    : astacktraceelement;
                            exception.setStackTrace(astacktraceelement1);

                            if (p_84040_ == 33356)
                            {
                                LOGGER.error(s4, (Throwable)exception);
                            }
                            else
                            {
                                LOGGER.info(s4, (Throwable)exception);
                            }

                            if (Config.isShowGlErrors() && TimedEvent.isActive("ShowGlErrorDebug", 10000L) && minecraft.level != null)
                            {
                                String s5 = Config.getGlErrorString(p_84041_);

                                if (p_84041_ == 0 || Config.equals(s5, "Unknown"))
                                {
                                    s5 = s3;
                                }

                                String s6 = I18n.get("of.message.openglError", p_84041_, s5);
                                minecraft.gui.getChat().addMessage(Component.literal(s6));
                            }

                            String s7 = GLDebugMessageCallback.getMessage(p_84043_, p_84044_);

                            synchronized (MESSAGE_BUFFER)
                            {
                                GlDebug.LogEntry gldebug$logentry = lastEntry;

                                if (gldebug$logentry != null && gldebug$logentry.isSame(p_84039_, p_84040_, p_84041_, p_84042_, s7))
                                {
                                    gldebug$logentry.count++;
                                }
                                else
                                {
                                    gldebug$logentry = new GlDebug.LogEntry(p_84039_, p_84040_, p_84041_, p_84042_, s7);
                                    MESSAGE_BUFFER.add(gldebug$logentry);
                                    lastEntry = gldebug$logentry;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static List<String> getLastOpenGlDebugMessages()
    {
        synchronized (MESSAGE_BUFFER)
        {
            List<String> list = Lists.newArrayListWithCapacity(MESSAGE_BUFFER.size());

            for (GlDebug.LogEntry gldebug$logentry : MESSAGE_BUFFER)
            {
                list.add(gldebug$logentry + " x " + gldebug$logentry.count);
            }

            return list;
        }
    }

    public static boolean isDebugEnabled()
    {
        return debugEnabled;
    }

    public static void enableDebugCallback(int p_84050_, boolean p_84051_)
    {
        if (p_84050_ > 0)
        {
            GLCapabilities glcapabilities = GL.getCapabilities();

            if (glcapabilities.GL_KHR_debug)
            {
                debugEnabled = true;
                GL11.glEnable(37600);

                if (p_84051_)
                {
                    GL11.glEnable(33346);
                }

                for (int i = 0; i < DEBUG_LEVELS.size(); i++)
                {
                    boolean flag = i < p_84050_;
                    KHRDebug.glDebugMessageControl(4352, 4352, DEBUG_LEVELS.get(i), (int[])null, flag);
                }

                KHRDebug.glDebugMessageCallback(GLX.make(GLDebugMessageCallback.create(GlDebug::printDebugLog), DebugMemoryUntracker::untrack), 0L);
            }
            else if (glcapabilities.GL_ARB_debug_output)
            {
                debugEnabled = true;

                if (p_84051_)
                {
                    GL11.glEnable(33346);
                }

                for (int j = 0; j < DEBUG_LEVELS_ARB.size(); j++)
                {
                    boolean flag1 = j < p_84050_;
                    ARBDebugOutput.glDebugMessageControlARB(4352, 4352, DEBUG_LEVELS_ARB.get(j), (int[])null, flag1);
                }

                ARBDebugOutput.glDebugMessageCallbackARB(GLX.make(GLDebugMessageARBCallback.create(GlDebug::printDebugLog), DebugMemoryUntracker::untrack), 0L);
            }
        }
    }

    static class LogEntry
    {
        private final int id;
        private final int source;
        private final int type;
        private final int severity;
        private final String message;
        int count = 1;

        LogEntry(int p_166234_, int p_166235_, int p_166236_, int p_166237_, String p_166238_)
        {
            this.id = p_166236_;
            this.source = p_166234_;
            this.type = p_166235_;
            this.severity = p_166237_;
            this.message = p_166238_;
        }

        boolean isSame(int p_166240_, int p_166241_, int p_166242_, int p_166243_, String p_166244_)
        {
            return p_166241_ == this.type
                   && p_166240_ == this.source
                   && p_166242_ == this.id
                   && p_166243_ == this.severity
                   && p_166244_.equals(this.message);
        }

        @Override
        public String toString()
        {
            return "id="
                   + this.id
                   + ", source="
                   + GlDebug.sourceToString(this.source)
                   + ", type="
                   + GlDebug.typeToString(this.type)
                   + ", severity="
                   + GlDebug.severityToString(this.severity)
                   + ", message='"
                   + this.message
                   + "'";
        }
    }
}
