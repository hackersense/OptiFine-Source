package com.mojang.blaze3d.systems;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeSource;
import net.optifine.Config;
import net.optifine.CustomGuis;
import net.optifine.shaders.Shaders;
import net.optifine.util.TextureUtils;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.slf4j.Logger;

@DontObfuscate
public class RenderSystem
{
    static final Logger LOGGER = LogUtils.getLogger();
    private static final ConcurrentLinkedQueue<RenderCall> recordingQueue = Queues.newConcurrentLinkedQueue();
    private static final Tesselator RENDER_THREAD_TESSELATOR = new Tesselator(1536);
    private static final int MINIMUM_ATLAS_TEXTURE_SIZE = 1024;
    @Nullable
    private static Thread renderThread;
    private static int MAX_SUPPORTED_TEXTURE_SIZE = -1;
    private static boolean isInInit;
    private static double lastDrawTime = Double.MIN_VALUE;
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequential = new RenderSystem.AutoStorageIndexBuffer(1, 1, IntConsumer::accept);
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialQuad = new RenderSystem.AutoStorageIndexBuffer(4, 6, (p_157398_, p_157399_) ->
    {
        p_157398_.accept(p_157399_ + 0);
        p_157398_.accept(p_157399_ + 1);
        p_157398_.accept(p_157399_ + 2);
        p_157398_.accept(p_157399_ + 2);
        p_157398_.accept(p_157399_ + 3);
        p_157398_.accept(p_157399_ + 0);
    });
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialLines = new RenderSystem.AutoStorageIndexBuffer(4, 6, (p_157401_, p_157402_) ->
    {
        p_157401_.accept(p_157402_ + 0);
        p_157401_.accept(p_157402_ + 1);
        p_157401_.accept(p_157402_ + 2);
        p_157401_.accept(p_157402_ + 3);
        p_157401_.accept(p_157402_ + 2);
        p_157401_.accept(p_157402_ + 1);
    });
    private static Matrix4f projectionMatrix = new Matrix4f();
    private static Matrix4f savedProjectionMatrix = new Matrix4f();
    private static VertexSorting vertexSorting = VertexSorting.DISTANCE_TO_ORIGIN;
    private static VertexSorting savedVertexSorting = VertexSorting.DISTANCE_TO_ORIGIN;
    private static final Matrix4fStack modelViewStack = new Matrix4fStack(16);
    private static Matrix4f modelViewMatrix = new Matrix4f();
    private static Matrix4f textureMatrix = new Matrix4f();
    private static final int[] shaderTextures = new int[12];
    private static final float[] shaderColor = new float[] {1.0F, 1.0F, 1.0F, 1.0F};
    private static float shaderGlintAlpha = 1.0F;
    private static float shaderFogStart;
    private static float shaderFogEnd = 1.0F;
    private static final float[] shaderFogColor = new float[] {0.0F, 0.0F, 0.0F, 0.0F};
    private static FogShape shaderFogShape = FogShape.SPHERE;
    private static final Vector3f[] shaderLightDirections = new Vector3f[2];
    private static float shaderGameTime;
    private static float shaderLineWidth = 1.0F;
    private static String apiDescription = "Unknown";
    @Nullable
    private static ShaderInstance shader;
    private static final AtomicLong pollEventsWaitStart = new AtomicLong();
    private static final AtomicBoolean pollingEvents = new AtomicBoolean(false);
    private static boolean fogAllowed = true;
    private static boolean colorToAttribute = false;

    public static void initRenderThread()
    {
        if (renderThread != null)
        {
            throw new IllegalStateException("Could not initialize render thread");
        }
        else
        {
            renderThread = Thread.currentThread();
        }
    }

    public static boolean isOnRenderThread()
    {
        return Thread.currentThread() == renderThread;
    }

    public static boolean isOnRenderThreadOrInit()
    {
        return isInInit || isOnRenderThread();
    }

    public static void assertOnRenderThreadOrInit()
    {
        if (!isInInit && !isOnRenderThread())
        {
            throw constructThreadException();
        }
    }

    public static void assertOnRenderThread()
    {
        if (!isOnRenderThread())
        {
            throw constructThreadException();
        }
    }

    private static IllegalStateException constructThreadException()
    {
        return new IllegalStateException("Rendersystem called from wrong thread");
    }

    public static void recordRenderCall(RenderCall p_69880_)
    {
        recordingQueue.add(p_69880_);
    }

    private static void pollEvents()
    {
        pollEventsWaitStart.set(Util.getMillis());
        pollingEvents.set(true);
        GLFW.glfwPollEvents();
        pollingEvents.set(false);
    }

    public static boolean isFrozenAtPollEvents()
    {
        return pollingEvents.get() && Util.getMillis() - pollEventsWaitStart.get() > 200L;
    }

    public static void flipFrame(long p_69496_)
    {
        pollEvents();
        replayQueue();
        Tesselator.getInstance().clear();
        GLFW.glfwSwapBuffers(p_69496_);
        pollEvents();
    }

    public static void replayQueue()
    {
        while (!recordingQueue.isEmpty())
        {
            RenderCall rendercall = recordingQueue.poll();
            rendercall.execute();
        }
    }

    public static void limitDisplayFPS(int p_69831_)
    {
        double d0 = lastDrawTime + 1.0 / (double)p_69831_;
        double d1;

        for (d1 = GLFW.glfwGetTime(); d1 < d0; d1 = GLFW.glfwGetTime())
        {
            GLFW.glfwWaitEventsTimeout(d0 - d1);
        }

        lastDrawTime = d1;
    }

    public static void disableDepthTest()
    {
        assertOnRenderThread();
        GlStateManager._disableDepthTest();
    }

    public static void enableDepthTest()
    {
        GlStateManager._enableDepthTest();
    }

    public static void enableScissor(int p_69489_, int p_69490_, int p_69491_, int p_69492_)
    {
        GlStateManager._enableScissorTest();
        GlStateManager._scissorBox(p_69489_, p_69490_, p_69491_, p_69492_);
    }

    public static void disableScissor()
    {
        GlStateManager._disableScissorTest();
    }

    public static void depthFunc(int p_69457_)
    {
        assertOnRenderThread();
        GlStateManager._depthFunc(p_69457_);
    }

    public static void depthMask(boolean p_69459_)
    {
        assertOnRenderThread();
        GlStateManager._depthMask(p_69459_);
    }

    public static void enableBlend()
    {
        assertOnRenderThread();
        GlStateManager._enableBlend();
    }

    public static void disableBlend()
    {
        assertOnRenderThread();
        GlStateManager._disableBlend();
    }

    public static void blendFunc(GlStateManager.SourceFactor p_69409_, GlStateManager.DestFactor p_69410_)
    {
        assertOnRenderThread();
        GlStateManager._blendFunc(p_69409_.value, p_69410_.value);
    }

    public static void blendFunc(int p_69406_, int p_69407_)
    {
        assertOnRenderThread();
        GlStateManager._blendFunc(p_69406_, p_69407_);
    }

    public static void blendFuncSeparate(
        GlStateManager.SourceFactor p_69417_, GlStateManager.DestFactor p_69418_, GlStateManager.SourceFactor p_69419_, GlStateManager.DestFactor p_69420_
    )
    {
        assertOnRenderThread();
        GlStateManager._blendFuncSeparate(p_69417_.value, p_69418_.value, p_69419_.value, p_69420_.value);
    }

    public static void blendFuncSeparate(int p_69412_, int p_69413_, int p_69414_, int p_69415_)
    {
        assertOnRenderThread();
        GlStateManager._blendFuncSeparate(p_69412_, p_69413_, p_69414_, p_69415_);
    }

    public static void blendEquation(int p_69404_)
    {
        assertOnRenderThread();
        GlStateManager._blendEquation(p_69404_);
    }

    public static void enableCull()
    {
        assertOnRenderThread();
        GlStateManager._enableCull();
    }

    public static void disableCull()
    {
        assertOnRenderThread();
        GlStateManager._disableCull();
    }

    public static void polygonMode(int p_69861_, int p_69862_)
    {
        assertOnRenderThread();
        GlStateManager._polygonMode(p_69861_, p_69862_);
    }

    public static void enablePolygonOffset()
    {
        assertOnRenderThread();
        GlStateManager._enablePolygonOffset();
    }

    public static void disablePolygonOffset()
    {
        assertOnRenderThread();
        GlStateManager._disablePolygonOffset();
    }

    public static void polygonOffset(float p_69864_, float p_69865_)
    {
        assertOnRenderThread();
        GlStateManager._polygonOffset(p_69864_, p_69865_);
    }

    public static void enableColorLogicOp()
    {
        assertOnRenderThread();
        GlStateManager._enableColorLogicOp();
    }

    public static void disableColorLogicOp()
    {
        assertOnRenderThread();
        GlStateManager._disableColorLogicOp();
    }

    public static void logicOp(GlStateManager.LogicOp p_69836_)
    {
        assertOnRenderThread();
        GlStateManager._logicOp(p_69836_.value);
    }

    public static void activeTexture(int p_69389_)
    {
        assertOnRenderThread();
        GlStateManager._activeTexture(p_69389_);
    }

    public static void enableTexture()
    {
        assertOnRenderThread();
        GlStateManager.enableTexture();
    }

    public static void disableTexture()
    {
        assertOnRenderThread();
        GlStateManager.disableTexture();
    }

    public static void texParameter(int p_69938_, int p_69939_, int p_69940_)
    {
        GlStateManager._texParameter(p_69938_, p_69939_, p_69940_);
    }

    public static void deleteTexture(int p_69455_)
    {
        GlStateManager._deleteTexture(p_69455_);
    }

    public static void bindTextureForSetup(int p_157185_)
    {
        bindTexture(p_157185_);
    }

    public static void bindTexture(int p_69397_)
    {
        GlStateManager._bindTexture(p_69397_);
    }

    public static void viewport(int p_69950_, int p_69951_, int p_69952_, int p_69953_)
    {
        GlStateManager._viewport(p_69950_, p_69951_, p_69952_, p_69953_);
    }

    public static void colorMask(boolean p_69445_, boolean p_69446_, boolean p_69447_, boolean p_69448_)
    {
        assertOnRenderThread();
        GlStateManager._colorMask(p_69445_, p_69446_, p_69447_, p_69448_);
    }

    public static void stencilFunc(int p_69926_, int p_69927_, int p_69928_)
    {
        assertOnRenderThread();
        GlStateManager._stencilFunc(p_69926_, p_69927_, p_69928_);
    }

    public static void stencilMask(int p_69930_)
    {
        assertOnRenderThread();
        GlStateManager._stencilMask(p_69930_);
    }

    public static void stencilOp(int p_69932_, int p_69933_, int p_69934_)
    {
        assertOnRenderThread();
        GlStateManager._stencilOp(p_69932_, p_69933_, p_69934_);
    }

    public static void clearDepth(double p_69431_)
    {
        GlStateManager._clearDepth(p_69431_);
    }

    public static void clearColor(float p_69425_, float p_69426_, float p_69427_, float p_69428_)
    {
        GlStateManager._clearColor(p_69425_, p_69426_, p_69427_, p_69428_);
    }

    public static void clearStencil(int p_69433_)
    {
        assertOnRenderThread();
        GlStateManager._clearStencil(p_69433_);
    }

    public static void clear(int p_69422_, boolean p_69423_)
    {
        GlStateManager._clear(p_69422_, p_69423_);
    }

    public static void setShaderFogStart(float p_157446_)
    {
        assertOnRenderThread();
        shaderFogStart = p_157446_;
    }

    public static float getShaderFogStart()
    {
        if (!fogAllowed)
        {
            return Float.MAX_VALUE;
        }
        else
        {
            assertOnRenderThread();
            return shaderFogStart;
        }
    }

    public static void setShaderGlintAlpha(double p_268332_)
    {
        setShaderGlintAlpha((float)p_268332_);
    }

    public static void setShaderGlintAlpha(float p_268329_)
    {
        assertOnRenderThread();
        shaderGlintAlpha = p_268329_;
    }

    public static float getShaderGlintAlpha()
    {
        assertOnRenderThread();
        return shaderGlintAlpha;
    }

    public static void setShaderFogEnd(float p_157444_)
    {
        assertOnRenderThread();
        shaderFogEnd = p_157444_;
    }

    public static float getShaderFogEnd()
    {
        if (!fogAllowed)
        {
            return Float.MAX_VALUE;
        }
        else
        {
            assertOnRenderThread();
            return shaderFogEnd;
        }
    }

    public static void setShaderFogColor(float p_157439_, float p_157440_, float p_157441_, float p_157442_)
    {
        assertOnRenderThread();
        shaderFogColor[0] = p_157439_;
        shaderFogColor[1] = p_157440_;
        shaderFogColor[2] = p_157441_;
        shaderFogColor[3] = p_157442_;
    }

    public static void setShaderFogColor(float p_157435_, float p_157436_, float p_157437_)
    {
        setShaderFogColor(p_157435_, p_157436_, p_157437_, 1.0F);
    }

    public static float[] getShaderFogColor()
    {
        assertOnRenderThread();
        return shaderFogColor;
    }

    public static void setShaderFogShape(FogShape p_202161_)
    {
        assertOnRenderThread();
        shaderFogShape = p_202161_;
    }

    public static FogShape getShaderFogShape()
    {
        assertOnRenderThread();
        return shaderFogShape;
    }

    public static void setShaderLights(Vector3f p_254155_, Vector3f p_254006_)
    {
        assertOnRenderThread();
        shaderLightDirections[0] = p_254155_;
        shaderLightDirections[1] = p_254006_;
    }

    public static void setupShaderLights(ShaderInstance p_157462_)
    {
        assertOnRenderThread();

        if (p_157462_.LIGHT0_DIRECTION != null)
        {
            p_157462_.LIGHT0_DIRECTION.set(shaderLightDirections[0]);
        }

        if (p_157462_.LIGHT1_DIRECTION != null)
        {
            p_157462_.LIGHT1_DIRECTION.set(shaderLightDirections[1]);
        }
    }

    public static void setShaderColor(float p_157430_, float p_157431_, float p_157432_, float p_157433_)
    {
        if (!isOnRenderThread())
        {
            recordRenderCall(() -> _setShaderColor(p_157430_, p_157431_, p_157432_, p_157433_));
        }
        else
        {
            _setShaderColor(p_157430_, p_157431_, p_157432_, p_157433_);
        }
    }

    private static void _setShaderColor(float p_157160_, float p_157161_, float p_157162_, float p_157163_)
    {
        shaderColor[0] = p_157160_;
        shaderColor[1] = p_157161_;
        shaderColor[2] = p_157162_;
        shaderColor[3] = p_157163_;

        if (colorToAttribute)
        {
            Shaders.setDefaultAttribColor(p_157160_, p_157161_, p_157162_, p_157163_);
        }
    }

    public static float[] getShaderColor()
    {
        assertOnRenderThread();
        return shaderColor;
    }

    public static void drawElements(int p_157187_, int p_157188_, int p_157189_)
    {
        assertOnRenderThread();
        GlStateManager._drawElements(p_157187_, p_157188_, p_157189_, 0L);
    }

    public static void lineWidth(float p_69833_)
    {
        if (!isOnRenderThread())
        {
            recordRenderCall(() -> shaderLineWidth = p_69833_);
        }
        else
        {
            shaderLineWidth = p_69833_;
        }
    }

    public static float getShaderLineWidth()
    {
        assertOnRenderThread();
        return shaderLineWidth;
    }

    public static void pixelStore(int p_69855_, int p_69856_)
    {
        GlStateManager._pixelStore(p_69855_, p_69856_);
    }

    public static void readPixels(int p_69872_, int p_69873_, int p_69874_, int p_69875_, int p_69876_, int p_69877_, ByteBuffer p_69878_)
    {
        assertOnRenderThread();
        GlStateManager._readPixels(p_69872_, p_69873_, p_69874_, p_69875_, p_69876_, p_69877_, p_69878_);
    }

    public static void getString(int p_69520_, Consumer<String> p_69521_)
    {
        assertOnRenderThread();
        p_69521_.accept(GlStateManager._getString(p_69520_));
    }

    public static String getBackendDescription()
    {
        return String.format(Locale.ROOT, "LWJGL version %s", GLX._getLWJGLVersion());
    }

    public static String getApiDescription()
    {
        return apiDescription;
    }

    public static TimeSource.NanoTimeSource initBackendSystem()
    {
        return GLX._initGlfw()::getAsLong;
    }

    public static void initRenderer(int p_69581_, boolean p_69582_)
    {
        GLX._init(p_69581_, p_69582_);
        apiDescription = GLX.getOpenGLVersionString();
    }

    public static void setErrorCallback(GLFWErrorCallbackI p_69901_)
    {
        GLX._setGlfwErrorCallback(p_69901_);
    }

    public static void renderCrosshair(int p_69882_)
    {
        assertOnRenderThread();
        GLX._renderCrosshair(p_69882_, true, true, true);
    }

    public static String getCapsString()
    {
        assertOnRenderThread();
        return "Using framebuffer using OpenGL 3.2";
    }

    public static void setupDefaultState(int p_69903_, int p_69904_, int p_69905_, int p_69906_)
    {
        GlStateManager._clearDepth(1.0);
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(515);
        projectionMatrix.identity();
        savedProjectionMatrix.identity();
        modelViewMatrix.identity();
        textureMatrix.identity();
        GlStateManager._viewport(p_69903_, p_69904_, p_69905_, p_69906_);
    }

    public static int maxSupportedTextureSize()
    {
        if (MAX_SUPPORTED_TEXTURE_SIZE == -1)
        {
            assertOnRenderThreadOrInit();
            int i = TextureUtils.getGLMaximumTextureSize();

            if (i > 0)
            {
                MAX_SUPPORTED_TEXTURE_SIZE = i;
                return MAX_SUPPORTED_TEXTURE_SIZE;
            }

            int j = GlStateManager._getInteger(3379);

            for (int k = Math.max(32768, j); k >= 1024; k >>= 1)
            {
                GlStateManager._texImage2D(32868, 0, 6408, k, k, 0, 6408, 5121, null);
                int l = GlStateManager._getTexLevelParameter(32868, 0, 4096);

                if (l != 0)
                {
                    MAX_SUPPORTED_TEXTURE_SIZE = k;
                    return k;
                }
            }

            MAX_SUPPORTED_TEXTURE_SIZE = Math.max(j, 1024);
            LOGGER.info("Failed to determine maximum texture size by probing, trying GL_MAX_TEXTURE_SIZE = {}", MAX_SUPPORTED_TEXTURE_SIZE);
        }

        return MAX_SUPPORTED_TEXTURE_SIZE;
    }

    public static void glBindBuffer(int p_157209_, int p_344603_)
    {
        GlStateManager._glBindBuffer(p_157209_, p_344603_);
    }

    public static void glBindVertexArray(int p_344671_)
    {
        GlStateManager._glBindVertexArray(p_344671_);
    }

    public static void glBufferData(int p_69526_, ByteBuffer p_69527_, int p_69528_)
    {
        assertOnRenderThreadOrInit();
        GlStateManager._glBufferData(p_69526_, p_69527_, p_69528_);
    }

    public static void glDeleteBuffers(int p_69530_)
    {
        assertOnRenderThread();
        GlStateManager._glDeleteBuffers(p_69530_);
    }

    public static void glDeleteVertexArrays(int p_157214_)
    {
        assertOnRenderThread();
        GlStateManager._glDeleteVertexArrays(p_157214_);
    }

    public static void glUniform1i(int p_69544_, int p_69545_)
    {
        assertOnRenderThread();
        GlStateManager._glUniform1i(p_69544_, p_69545_);
    }

    public static void glUniform1(int p_69541_, IntBuffer p_69542_)
    {
        assertOnRenderThread();
        GlStateManager._glUniform1(p_69541_, p_69542_);
    }

    public static void glUniform2(int p_69550_, IntBuffer p_69551_)
    {
        assertOnRenderThread();
        GlStateManager._glUniform2(p_69550_, p_69551_);
    }

    public static void glUniform3(int p_69556_, IntBuffer p_69557_)
    {
        assertOnRenderThread();
        GlStateManager._glUniform3(p_69556_, p_69557_);
    }

    public static void glUniform4(int p_69562_, IntBuffer p_69563_)
    {
        assertOnRenderThread();
        GlStateManager._glUniform4(p_69562_, p_69563_);
    }

    public static void glUniform1(int p_69538_, FloatBuffer p_69539_)
    {
        assertOnRenderThread();
        GlStateManager._glUniform1(p_69538_, p_69539_);
    }

    public static void glUniform2(int p_69547_, FloatBuffer p_69548_)
    {
        assertOnRenderThread();
        GlStateManager._glUniform2(p_69547_, p_69548_);
    }

    public static void glUniform3(int p_69553_, FloatBuffer p_69554_)
    {
        assertOnRenderThread();
        GlStateManager._glUniform3(p_69553_, p_69554_);
    }

    public static void glUniform4(int p_69559_, FloatBuffer p_69560_)
    {
        assertOnRenderThread();
        GlStateManager._glUniform4(p_69559_, p_69560_);
    }

    public static void glUniformMatrix2(int p_69565_, boolean p_69566_, FloatBuffer p_69567_)
    {
        assertOnRenderThread();
        GlStateManager._glUniformMatrix2(p_69565_, p_69566_, p_69567_);
    }

    public static void glUniformMatrix3(int p_69569_, boolean p_69570_, FloatBuffer p_69571_)
    {
        assertOnRenderThread();
        GlStateManager._glUniformMatrix3(p_69569_, p_69570_, p_69571_);
    }

    public static void glUniformMatrix4(int p_69573_, boolean p_69574_, FloatBuffer p_69575_)
    {
        assertOnRenderThread();
        GlStateManager._glUniformMatrix4(p_69573_, p_69574_, p_69575_);
    }

    public static void setupOverlayColor(int p_69922_, int p_342657_)
    {
        assertOnRenderThread();
        setShaderTexture(1, p_69922_);
    }

    public static void teardownOverlayColor()
    {
        assertOnRenderThread();
        setShaderTexture(1, 0);
    }

    public static void setupLevelDiffuseLighting(Vector3f p_254489_, Vector3f p_254541_)
    {
        assertOnRenderThread();
        setShaderLights(p_254489_, p_254541_);
    }

    public static void setupGuiFlatDiffuseLighting(Vector3f p_254419_, Vector3f p_254483_)
    {
        assertOnRenderThread();
        GlStateManager.setupGuiFlatDiffuseLighting(p_254419_, p_254483_);
    }

    public static void setupGui3DDiffuseLighting(Vector3f p_253859_, Vector3f p_253890_)
    {
        assertOnRenderThread();
        GlStateManager.setupGui3DDiffuseLighting(p_253859_, p_253890_);
    }

    public static void beginInitialization()
    {
        isInInit = true;
    }

    public static void finishInitialization()
    {
        isInInit = false;

        if (!recordingQueue.isEmpty())
        {
            replayQueue();
        }

        if (!recordingQueue.isEmpty())
        {
            throw new IllegalStateException("Recorded to render queue during initialization");
        }
    }

    public static void glGenBuffers(Consumer<Integer> p_69532_)
    {
        if (!isOnRenderThread())
        {
            recordRenderCall(() -> p_69532_.accept(GlStateManager._glGenBuffers()));
        }
        else
        {
            p_69532_.accept(GlStateManager._glGenBuffers());
        }
    }

    public static void glGenVertexArrays(Consumer<Integer> p_157216_)
    {
        if (!isOnRenderThread())
        {
            recordRenderCall(() -> p_157216_.accept(GlStateManager._glGenVertexArrays()));
        }
        else
        {
            p_157216_.accept(GlStateManager._glGenVertexArrays());
        }
    }

    public static Tesselator renderThreadTesselator()
    {
        assertOnRenderThread();
        return RENDER_THREAD_TESSELATOR;
    }

    public static void defaultBlendFunc()
    {
        blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );
    }

    @Deprecated
    public static void runAsFancy(Runnable p_69891_)
    {
        boolean flag = Minecraft.useShaderTransparency();

        if (!flag)
        {
            p_69891_.run();
        }
        else
        {
            OptionInstance<GraphicsStatus> optioninstance = Minecraft.getInstance().options.graphicsMode();
            GraphicsStatus graphicsstatus = optioninstance.get();
            optioninstance.set(GraphicsStatus.FANCY);
            p_69891_.run();
            optioninstance.set(graphicsstatus);
        }
    }

    public static void setShader(Supplier<ShaderInstance> p_157428_)
    {
        if (!isOnRenderThread())
        {
            recordRenderCall(() -> shader = p_157428_.get());
        }
        else
        {
            shader = p_157428_.get();
        }
    }

    @Nullable
    public static ShaderInstance getShader()
    {
        assertOnRenderThread();
        return shader;
    }

    public static void setShaderTexture(int p_157457_, ResourceLocation p_157458_)
    {
        if (!isOnRenderThread())
        {
            recordRenderCall(() -> _setShaderTexture(p_157457_, p_157458_));
        }
        else
        {
            _setShaderTexture(p_157457_, p_157458_);
        }
    }

    public static void _setShaderTexture(int p_157180_, ResourceLocation p_157181_)
    {
        if (Config.isCustomGuis())
        {
            p_157181_ = CustomGuis.getTextureLocation(p_157181_);
        }

        if (p_157180_ >= 0 && p_157180_ < shaderTextures.length)
        {
            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            AbstractTexture abstracttexture = texturemanager.getTexture(p_157181_);
            shaderTextures[p_157180_] = abstracttexture.getId();
        }
    }

    public static void setShaderTexture(int p_157454_, int p_157455_)
    {
        if (!isOnRenderThread())
        {
            recordRenderCall(() -> _setShaderTexture(p_157454_, p_157455_));
        }
        else
        {
            _setShaderTexture(p_157454_, p_157455_);
        }
    }

    public static void _setShaderTexture(int p_157177_, int p_157178_)
    {
        if (p_157177_ >= 0 && p_157177_ < shaderTextures.length)
        {
            shaderTextures[p_157177_] = p_157178_;
        }
    }

    public static int getShaderTexture(int p_157204_)
    {
        assertOnRenderThread();
        return p_157204_ >= 0 && p_157204_ < shaderTextures.length ? shaderTextures[p_157204_] : 0;
    }

    public static void setProjectionMatrix(Matrix4f p_277884_, VertexSorting p_277702_)
    {
        Matrix4f matrix4f = new Matrix4f(p_277884_);

        if (!isOnRenderThread())
        {
            recordRenderCall(() ->
            {
                projectionMatrix = matrix4f;
                vertexSorting = p_277702_;
            });
        }
        else
        {
            projectionMatrix = matrix4f;
            vertexSorting = p_277702_;
        }
    }

    public static void setTextureMatrix(Matrix4f p_254081_)
    {
        Matrix4f matrix4f = new Matrix4f(p_254081_);

        if (!isOnRenderThread())
        {
            recordRenderCall(() -> textureMatrix = matrix4f);
        }
        else
        {
            textureMatrix = matrix4f;
        }
    }

    public static void resetTextureMatrix()
    {
        if (!isOnRenderThread())
        {
            recordRenderCall(() -> textureMatrix.identity());
        }
        else
        {
            textureMatrix.identity();
        }
    }

    public static void applyModelViewMatrix()
    {
        Matrix4f matrix4f = modelViewStack;

        if (!isOnRenderThread())
        {
            recordRenderCall(() -> modelViewMatrix.set(matrix4f));
        }
        else
        {
            modelViewMatrix.set(matrix4f);
        }
    }

    public static void backupProjectionMatrix()
    {
        if (!isOnRenderThread())
        {
            recordRenderCall(() -> _backupProjectionMatrix());
        }
        else
        {
            _backupProjectionMatrix();
        }
    }

    private static void _backupProjectionMatrix()
    {
        savedProjectionMatrix = projectionMatrix;
        savedVertexSorting = vertexSorting;
    }

    public static void restoreProjectionMatrix()
    {
        if (!isOnRenderThread())
        {
            recordRenderCall(() -> _restoreProjectionMatrix());
        }
        else
        {
            _restoreProjectionMatrix();
        }
    }

    private static void _restoreProjectionMatrix()
    {
        projectionMatrix = savedProjectionMatrix;
        vertexSorting = savedVertexSorting;
    }

    public static Matrix4f getProjectionMatrix()
    {
        assertOnRenderThread();
        return projectionMatrix;
    }

    public static Matrix4f getModelViewMatrix()
    {
        assertOnRenderThread();
        return modelViewMatrix;
    }

    public static Matrix4fStack getModelViewStack()
    {
        return modelViewStack;
    }

    public static Matrix4f getTextureMatrix()
    {
        assertOnRenderThread();
        return textureMatrix;
    }

    public static RenderSystem.AutoStorageIndexBuffer getSequentialBuffer(VertexFormat.Mode p_221942_)
    {
        assertOnRenderThread();

        return switch (p_221942_)
        {
            case QUADS -> sharedSequentialQuad;

            case LINES -> sharedSequentialLines;

            default -> sharedSequential;
        };
    }

    public static void setShaderGameTime(long p_157448_, float p_157449_)
    {
        float f = ((float)(p_157448_ % 24000L) + p_157449_) / 24000.0F;

        if (!isOnRenderThread())
        {
            recordRenderCall(() -> shaderGameTime = f);
        }
        else
        {
            shaderGameTime = f;
        }
    }

    public static float getShaderGameTime()
    {
        assertOnRenderThread();
        return shaderGameTime;
    }

    public static VertexSorting getVertexSorting()
    {
        assertOnRenderThread();
        return vertexSorting;
    }

    public static void setFogAllowed(boolean fogAllowed)
    {
        RenderSystem.fogAllowed = fogAllowed;

        if (Config.isShaders())
        {
            Shaders.setFogAllowed(fogAllowed);
        }
    }

    public static boolean isFogAllowed()
    {
        return fogAllowed;
    }

    public static void setColorToAttribute(boolean colorToAttribute)
    {
        if (Config.isShaders())
        {
            if (RenderSystem.colorToAttribute != colorToAttribute)
            {
                RenderSystem.colorToAttribute = colorToAttribute;

                if (colorToAttribute)
                {
                    Shaders.setDefaultAttribColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
                }
                else
                {
                    Shaders.setDefaultAttribColor();
                }
            }
        }
    }

    public static final class AutoStorageIndexBuffer
    {
        private final int vertexStride;
        private final int indexStride;
        private final RenderSystem.AutoStorageIndexBuffer.IndexGenerator generator;
        private int name;
        private VertexFormat.IndexType type = VertexFormat.IndexType.SHORT;
        private int indexCount;

        AutoStorageIndexBuffer(int p_157472_, int p_157473_, RenderSystem.AutoStorageIndexBuffer.IndexGenerator p_157474_)
        {
            this.vertexStride = p_157472_;
            this.indexStride = p_157473_;
            this.generator = p_157474_;
        }

        public boolean hasStorage(int p_221945_)
        {
            return p_221945_ <= this.indexCount;
        }

        public void bind(int p_221947_)
        {
            if (this.name == 0)
            {
                this.name = GlStateManager._glGenBuffers();
            }

            GlStateManager._glBindBuffer(34963, this.name);
            this.ensureStorage(p_221947_);
        }

        public void ensureStorage(int p_157477_)
        {
            if (!this.hasStorage(p_157477_))
            {
                p_157477_ = Mth.roundToward(p_157477_ * 2, this.indexStride);
                RenderSystem.LOGGER.debug("Growing IndexBuffer: Old limit {}, new limit {}.", this.indexCount, p_157477_);
                int i = p_157477_ / this.indexStride;
                int j = i * this.vertexStride;
                VertexFormat.IndexType vertexformat$indextype = VertexFormat.IndexType.least(j);
                int k = Mth.roundToward(p_157477_ * vertexformat$indextype.bytes, 4);
                GlStateManager._glBufferData(34963, (long)k, 35048);
                ByteBuffer bytebuffer = GlStateManager._glMapBuffer(34963, 35001);

                if (bytebuffer == null)
                {
                    throw new RuntimeException("Failed to map GL buffer");
                }

                this.type = vertexformat$indextype;
                it.unimi.dsi.fastutil.ints.IntConsumer intconsumer = this.intConsumer(bytebuffer);

                for (int l = 0; l < p_157477_; l += this.indexStride)
                {
                    this.generator.accept(intconsumer, l * this.vertexStride / this.indexStride);
                }

                GlStateManager._glUnmapBuffer(34963);
                this.indexCount = p_157477_;
            }
        }

        private it.unimi.dsi.fastutil.ints.IntConsumer intConsumer(ByteBuffer p_157479_)
        {
            switch (this.type)
            {
                case SHORT:
                    return valueIn -> p_157479_.putShort((short)valueIn);

                case INT:
                default:
                    return p_157479_::putInt;
            }
        }

        public VertexFormat.IndexType type()
        {
            return this.type;
        }

        interface IndexGenerator
        {
            void accept(it.unimi.dsi.fastutil.ints.IntConsumer p_157488_, int p_157489_);
        }
    }
}
