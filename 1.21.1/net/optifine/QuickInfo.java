package net.optifine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.optifine.config.Option;
import net.optifine.render.RenderCache;
import net.optifine.util.GpuFrameTimer;
import net.optifine.util.GpuMemory;
import net.optifine.util.MemoryMonitor;
import net.optifine.util.NativeMemory;
import net.optifine.util.NumUtils;

public class QuickInfo
{
    private static RenderCache renderCache = new RenderCache(100L);
    private static Minecraft minecraft = Config.getMinecraft();
    private static Font font = minecraft.font;
    private static double gpuLoadSmooth = 0.0;
    private static McDebugInfo gpuDebugInfo = new McDebugInfo();
    private static int gpuPercCached;

    public static void render(GuiGraphics graphicsIn)
    {
        if (!renderCache.drawCached(graphicsIn))
        {
            renderCache.startRender(graphicsIn);
            renderLeft(graphicsIn);
            renderRight(graphicsIn);
            renderCache.stopRender(graphicsIn);
        }
    }

    private static void renderLeft(GuiGraphics graphicsIn)
    {
        List<String> list = new ArrayList<>();
        StringBuilder stringbuilder = new StringBuilder();
        Options options = Config.getGameSettings();
        boolean flag = !Option.isCompact(options.ofQuickInfoLabels);
        boolean flag1 = Option.isDetailed(options.ofQuickInfoLabels);
        boolean flag2 = !Option.isOff(options.ofQuickInfoFps);
        boolean flag3 = Option.isFull(options.ofQuickInfoFps);
        boolean flag4 = options.ofQuickInfoChunks;
        boolean flag5 = options.ofQuickInfoEntities;
        boolean flag6 = options.ofQuickInfoParticles;
        boolean flag7 = options.ofQuickInfoUpdates;
        boolean flag8 = options.ofQuickInfoGpu;

        if (flag2)
        {
            addFps(next(stringbuilder), flag, flag3);
        }

        if (flag4)
        {
            addChunks(next(stringbuilder), flag);
        }

        if (flag5)
        {
            addEntities(next(stringbuilder), flag);
        }

        if (flag)
        {
            newLine(list, stringbuilder);
        }

        if (flag6)
        {
            addParticles(next(stringbuilder), flag);
        }

        if (flag7)
        {
            addUpdates(next(stringbuilder), flag);
        }

        if (flag8)
        {
            addGpu(next(stringbuilder), flag);
        }

        newLine(list, stringbuilder);
        boolean flag9 = !Option.isOff(options.ofQuickInfoPos);
        boolean flag10 = Option.isFull(options.ofQuickInfoPos);
        boolean flag11 = minecraft.showOnlyReducedInfo();

        if (flag9)
        {
            addPos(next(stringbuilder), flag, flag1, flag10, flag11);
        }

        newLine(list, stringbuilder);

        if (!flag11)
        {
            boolean flag12 = !Option.isOff(options.ofQuickInfoFacing);
            boolean flag13 = Option.isFull(options.ofQuickInfoFacing);

            if (flag12)
            {
                addFacing(next(stringbuilder), flag, flag1, flag12, flag13);
            }

            newLine(list, stringbuilder);
            boolean flag14 = options.ofQuickInfoBiome;
            boolean flag15 = options.ofQuickInfoLight;

            if (flag14)
            {
                addBiome(next(stringbuilder), flag);
            }

            if (flag15)
            {
                addLight(next(stringbuilder), flag);
            }

            newLine(list, stringbuilder);
        }

        render(graphicsIn, list, false);
    }

    private static void renderRight(GuiGraphics graphicsIn)
    {
        List<String> list = new ArrayList<>();
        StringBuilder stringbuilder = new StringBuilder();
        Options options = Config.getGameSettings();
        boolean flag = !Option.isCompact(options.ofQuickInfoLabels);
        boolean flag1 = Option.isDetailed(options.ofQuickInfoLabels);
        boolean flag2 = !Option.isOff(options.ofQuickInfoMemory);
        boolean flag3 = Option.isFull(options.ofQuickInfoMemory);
        boolean flag4 = !Option.isOff(options.ofQuickInfoNativeMemory);
        boolean flag5 = Option.isFull(options.ofQuickInfoNativeMemory);

        if (flag2)
        {
            addMem(next(stringbuilder), flag);
        }

        if (flag3)
        {
            addMemAlloc(next(stringbuilder), flag);
        }

        newLine(list, stringbuilder);

        if (flag4)
        {
            addMemNative(next(stringbuilder), flag);
        }

        if (flag5)
        {
            addMemGpu(next(stringbuilder), flag);
        }

        newLine(list, stringbuilder);

        if (!minecraft.showOnlyReducedInfo())
        {
            boolean flag6 = !Option.isOff(options.ofQuickInfoTargetBlock);
            boolean flag7 = Option.isFull(options.ofQuickInfoTargetBlock);

            if (flag6)
            {
                addTargetBlock(next(stringbuilder), flag, flag7);
            }

            newLine(list, stringbuilder);
            boolean flag8 = !Option.isOff(options.ofQuickInfoTargetFluid);
            boolean flag9 = Option.isFull(options.ofQuickInfoTargetFluid);

            if (flag8)
            {
                addTargetFluid(next(stringbuilder), flag, flag9);
            }

            newLine(list, stringbuilder);
            boolean flag10 = !Option.isOff(options.ofQuickInfoTargetEntity);
            boolean flag11 = Option.isFull(options.ofQuickInfoTargetEntity);

            if (flag10)
            {
                addTargetEntity(next(stringbuilder), flag, flag1, flag11);
            }

            newLine(list, stringbuilder);
        }

        render(graphicsIn, list, true);
    }

    private static StringBuilder next(StringBuilder sb)
    {
        if (!sb.isEmpty())
        {
            sb.append(", ");
        }

        return sb;
    }

    private static void newLine(List<String> lines, StringBuilder sb)
    {
        if (!sb.isEmpty())
        {
            lines.add(sb.toString());
            sb.setLength(0);
        }
    }

    private static void render(GuiGraphics graphicsIn, List<String> linesIn, boolean rightIn)
    {
        if (!linesIn.isEmpty())
        {
            Options options = Config.getGameSettings();
            boolean flag = options.ofQuickInfoBackground;
            boolean flag1 = false;

            if (!rightIn)
            {
                graphicsIn.pose().pushPose();
                graphicsIn.pose().translate(0.0F, 0.0F, 10.0F);
            }

            int i = 9;

            if (flag)
            {
                int j = 2;

                for (int k = 0; k < linesIn.size(); k++)
                {
                    String s = linesIn.get(k);

                    if (!StringUtil.isNullOrEmpty(s))
                    {
                        int l = font.width(s);
                        int i1 = rightIn ? graphicsIn.guiWidth() - 2 - l : 2;
                        graphicsIn.fill(i1 - 1, j - 1, i1 + l + 1, j + i - 1, -1873784752);
                        j += i;
                    }
                }
            }

            int j1 = 2;

            for (int k1 = 0; k1 < linesIn.size(); k1++)
            {
                String s1 = linesIn.get(k1);

                if (!StringUtil.isNullOrEmpty(s1))
                {
                    int l1 = font.width(s1);
                    int i2 = rightIn ? graphicsIn.guiWidth() - 2 - l1 : 2;
                    graphicsIn.drawString(font, s1, i2, j1, -2039584, flag1);
                    j1 += i;
                }
            }

            if (!rightIn)
            {
                graphicsIn.pose().popPose();
            }
        }
    }

    private static String getName(ResourceLocation loc)
    {
        if (loc == null)
        {
            return "";
        }
        else
        {
            return loc.isDefaultNamespace() ? loc.getPath() : loc.toString();
        }
    }

    private static void addFps(StringBuilder sb, boolean fullLabel, boolean addFpsMin)
    {
        if (Config.isShowFrameTime())
        {
            int k = Config.getFpsAverage();
            appendDouble1(sb, 1000.0 / (double)Config.limit(k, 1, Integer.MAX_VALUE));

            if (addFpsMin)
            {
                int l = Config.getFpsMin();
                sb.append('/');
                appendDouble1(sb, 1000.0 / (double)Config.limit(l, 1, Integer.MAX_VALUE));
            }

            sb.append(" ms");
        }
        else
        {
            int i = Config.getFpsAverage();
            sb.append(Integer.toString(i));

            if (addFpsMin)
            {
                int j = Config.getFpsMin();
                sb.append('/');
                sb.append(Integer.toString(j));
            }

            sb.append(" fps");
        }
    }

    private static void addChunks(StringBuilder sb, boolean fullLabel)
    {
        int i = minecraft.levelRenderer.countRenderedSections();
        sb.append(fullLabel ? "Chunks: " : "C: ");
        sb.append(Integer.toString(i));
    }

    private static void addEntities(StringBuilder sb, boolean fullLabel)
    {
        int i = minecraft.levelRenderer.getCountEntitiesRendered();
        sb.append(fullLabel ? "Entities: " : "E: ");
        sb.append(Integer.toString(i));
        int j = minecraft.levelRenderer.getCountTileEntitiesRendered();
        sb.append('+');
        sb.append(Integer.toString(j));
    }

    private static void addParticles(StringBuilder sb, boolean fullLabel)
    {
        int i = minecraft.particleEngine.getCountParticles();
        sb.append(fullLabel ? "Particles: " : "P: ");
        sb.append(Integer.toString(i));
    }

    private static void addUpdates(StringBuilder sb, boolean fullLabel)
    {
        int i = Config.getChunkUpdates();
        sb.append(fullLabel ? "Updates: " : "U: ");
        sb.append(Integer.toString(i));
    }

    private static void addGpu(StringBuilder sb, boolean fullLabel)
    {
        double d0 = GpuFrameTimer.getGpuLoad();
        gpuLoadSmooth = (gpuLoadSmooth * 4.0 + d0) / 5.0;
        int i = (int)Math.round(gpuLoadSmooth * 100.0);
        i = NumUtils.limit(i, 0, 100);

        if (gpuPercCached <= 0 || gpuDebugInfo.isChanged())
        {
            gpuPercCached = i;
        }

        sb.append(fullLabel ? "GPU: " : "G: ");
        sb.append(Integer.toString(gpuPercCached));
        sb.append("%");
    }

    private static void addPos(StringBuilder sb, boolean fullLabel, boolean detailedCoords, boolean posRel, boolean reducedDebug)
    {
        Entity entity = minecraft.getCameraEntity();
        BlockPos blockpos = entity.blockPosition();

        if (!reducedDebug || posRel)
        {
            sb.append(fullLabel ? "Position: " : "Pos: ");
        }

        if (!reducedDebug)
        {
            if (detailedCoords)
            {
                sb.append(" (");
                appendDouble3(sb, entity.getX());
                sb.append(", ");
                appendDouble3(sb, entity.getY());
                sb.append(", ");
                appendDouble3(sb, entity.getZ());
                sb.append(")");
            }
            else
            {
                sb.append(Integer.toString(blockpos.getX()));
                sb.append(", ");
                sb.append(Integer.toString(blockpos.getY()));
                sb.append(", ");
                sb.append(Integer.toString(blockpos.getZ()));
            }
        }

        if (posRel)
        {
            sb.append(" [");
            sb.append(Integer.toString(blockpos.getX() & 15));
            sb.append(", ");
            sb.append(Integer.toString(blockpos.getY() & 15));
            sb.append(", ");
            sb.append(Integer.toString(blockpos.getZ() & 15));
            sb.append("]");
        }
    }

    private static void addFacing(StringBuilder sb, boolean fullLabel, boolean detailedCoords, boolean facingXyz, boolean yawPitch)
    {
        Entity entity = minecraft.getCameraEntity();
        Direction direction = entity.getDirection();
        sb.append(fullLabel ? "Facing: " : "F: ");
        sb.append(direction.toString());

        if (facingXyz)
        {
            String s = getXyz(direction);
            sb.append(" [");
            sb.append(s);
            sb.append("]");
        }

        if (yawPitch)
        {
            float f1 = Mth.wrapDegrees(entity.getYRot());
            float f = Mth.wrapDegrees(entity.getXRot());

            if (detailedCoords)
            {
                sb.append(" (");
                appendDouble1(sb, (double)f1);
                sb.append('/');
                appendDouble1(sb, (double)f);
                sb.append(')');
            }
            else
            {
                sb.append(" (");
                sb.append(Integer.toString(Math.round(f1)));
                sb.append('/');
                sb.append(Integer.toString(Math.round(f)));
                sb.append(')');
            }
        }
    }

    private static String getXyz(Direction dir)
    {
        switch (dir)
        {
            case NORTH:
                return "Z-";

            case SOUTH:
                return "Z+";

            case WEST:
                return "X-";

            case EAST:
                return "X+";

            case UP:
                return "Y+";

            case DOWN:
                return "Y-";

            default:
                return "?";
        }
    }

    private static void addBiome(StringBuilder sb, boolean fullLabel)
    {
        Entity entity = minecraft.getCameraEntity();
        BlockPos blockpos = entity.blockPosition();
        Holder<Biome> holder = minecraft.level.getBiome(blockpos);
        Optional<ResourceKey<Biome>> optional = holder.unwrapKey();
        String s = getBiomeName(optional);
        sb.append(fullLabel ? "Biome: " : "B: ");
        sb.append(s);
    }

    private static String getBiomeName(Optional<ResourceKey<Biome>> key)
    {
        if (!key.isPresent())
        {
            return "[unregistered]";
        }
        else
        {
            ResourceLocation resourcelocation = key.get().location();
            return resourcelocation.isDefaultNamespace() ? resourcelocation.getPath() : resourcelocation.toString();
        }
    }

    private static void addLight(StringBuilder sb, boolean fullLabel)
    {
        Entity entity = minecraft.getCameraEntity();
        BlockPos blockpos = entity.blockPosition();
        ClientLevel clientlevel = minecraft.level;
        int i = clientlevel.getBrightness(LightLayer.SKY, blockpos);
        int j = clientlevel.getBrightness(LightLayer.BLOCK, blockpos);

        if (fullLabel)
        {
            sb.append("Light: ");
            sb.append(Integer.toString(i));
            sb.append(" sky, ");
            sb.append(Integer.toString(j));
            sb.append(" block");
        }
        else
        {
            sb.append("L: ");
            sb.append(Integer.toString(i));
            sb.append("/");
            sb.append(Integer.toString(j));
        }
    }

    private static void addMem(StringBuilder sb, boolean fullLabel)
    {
        long i = Runtime.getRuntime().maxMemory();
        long j = Runtime.getRuntime().totalMemory();
        long k = Runtime.getRuntime().freeMemory();
        long l = j - k;
        sb.append(fullLabel ? "Memory: " : "M: ");
        sb.append(Integer.toString(bytesToMb(l)));
        sb.append("/");
        sb.append(Integer.toString(bytesToMb(i)));

        if (fullLabel)
        {
            sb.append(" MB");
        }
    }

    private static void addMemAlloc(StringBuilder sb, boolean fullLabel)
    {
        int i = (int)MemoryMonitor.getAllocationRateAvgMb();
        sb.append(fullLabel ? "Allocation: " : "A: ");
        sb.append(Integer.toString(i));

        if (fullLabel)
        {
            sb.append(" MB/s");
        }
    }

    private static void addMemNative(StringBuilder sb, boolean fullLabel)
    {
        long i = NativeMemory.getBufferAllocated();
        long j = NativeMemory.getBufferMaximum();
        long k = NativeMemory.getImageAllocated();
        sb.append(fullLabel ? "Native: " : "N: ");
        sb.append(Integer.toString(bytesToMb(i)));
        sb.append("/");
        sb.append(Integer.toString(bytesToMb(j)));
        sb.append("+");
        sb.append(Integer.toString(bytesToMb(k)));

        if (fullLabel)
        {
            sb.append(" MB");
        }
    }

    private static void addMemGpu(StringBuilder sb, boolean fullLabel)
    {
        long i = GpuMemory.getBufferAllocated();
        long j = GpuMemory.getTextureAllocated();
        sb.append(fullLabel ? "GPU: " : "G: ");
        sb.append(Integer.toString(bytesToMb(i)));
        sb.append("+");
        sb.append(Integer.toString(bytesToMb(j)));

        if (fullLabel)
        {
            sb.append(" MB");
        }
    }

    private static int bytesToMb(long bytes)
    {
        return (int)(bytes / 1024L / 1024L);
    }

    private static void addTargetBlock(StringBuilder sb, boolean fullLabel, boolean showPos)
    {
        Entity entity = minecraft.getCameraEntity();
        double d0 = minecraft.player.blockInteractionRange();
        HitResult hitresult = entity.pick(d0, 0.0F, false);

        if (hitresult.getType() == HitResult.Type.BLOCK)
        {
            BlockPos blockpos = ((BlockHitResult)hitresult).getBlockPos();
            BlockState blockstate = minecraft.level.getBlockState(blockpos);
            sb.append(fullLabel ? "Target Block: " : "TB: ");
            sb.append(getName(blockstate.getBlockLocation()));

            if (showPos)
            {
                sb.append(" (");
                sb.append(Integer.toString(blockpos.getX()));
                sb.append(", ");
                sb.append(Integer.toString(blockpos.getY()));
                sb.append(", ");
                sb.append(Integer.toString(blockpos.getZ()));
                sb.append(")");
            }
        }
    }

    private static void addTargetFluid(StringBuilder sb, boolean fullLabel, boolean showPos)
    {
        Entity entity = minecraft.getCameraEntity();
        double d0 = minecraft.player.blockInteractionRange();
        HitResult hitresult = entity.pick(d0, 0.0F, true);

        if (hitresult.getType() == HitResult.Type.BLOCK)
        {
            BlockPos blockpos = ((BlockHitResult)hitresult).getBlockPos();
            BlockState blockstate = minecraft.level.getBlockState(blockpos);
            Fluid fluid = blockstate.getFluidState().getType();

            if (fluid == Fluids.EMPTY)
            {
                return;
            }

            ResourceLocation resourcelocation = BuiltInRegistries.FLUID.getKey(fluid);
            String s = getName(resourcelocation);
            sb.append(fullLabel ? "Target Fluid: " : "TF: ");
            sb.append(s);

            if (showPos)
            {
                sb.append(" (");
                sb.append(Integer.toString(blockpos.getX()));
                sb.append(", ");
                sb.append(Integer.toString(blockpos.getY()));
                sb.append(", ");
                sb.append(Integer.toString(blockpos.getZ()));
                sb.append(")");
            }
        }
    }

    private static void addTargetEntity(StringBuilder sb, boolean fullLabel, boolean detailedCoords, boolean showPos)
    {
        Entity entity = minecraft.crosshairPickEntity;

        if (entity != null)
        {
            BlockPos blockpos = entity.blockPosition();
            ResourceLocation resourcelocation = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            String s = getName(resourcelocation);

            if (resourcelocation != null)
            {
                sb.append(fullLabel ? "Target Entity: " : "TE: ");
                sb.append(s);

                if (showPos)
                {
                    if (detailedCoords)
                    {
                        sb.append(" (");
                        appendDouble3(sb, entity.getX());
                        sb.append(", ");
                        appendDouble3(sb, entity.getY());
                        sb.append(", ");
                        appendDouble3(sb, entity.getZ());
                        sb.append(")");
                    }
                    else
                    {
                        sb.append(" (");
                        sb.append(Integer.toString(blockpos.getX()));
                        sb.append(", ");
                        sb.append(Integer.toString(blockpos.getY()));
                        sb.append(", ");
                        sb.append(Integer.toString(blockpos.getZ()));
                        sb.append(")");
                    }
                }
            }
        }
    }

    private static void appendDouble1(StringBuilder sb, double num)
    {
        num = (double)Math.round(num * 10.0) / 10.0;
        sb.append(num);
    }

    private static void appendDouble3(StringBuilder sb, double num)
    {
        num = (double)Math.round(num * 1000.0) / 1000.0;
        sb.append(num);

        if (sb.charAt(sb.length() - 2) == '.')
        {
            sb.append('0');
        }

        if (sb.charAt(sb.length() - 3) == '.')
        {
            sb.append('0');
        }
    }
}
