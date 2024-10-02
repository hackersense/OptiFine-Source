package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ChunkPos;

public class SystemToast implements Toast
{
    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/system");
    private static final int MAX_LINE_SIZE = 200;
    private static final int LINE_SPACING = 12;
    private static final int MARGIN = 10;
    private final SystemToast.SystemToastId id;
    private Component title;
    private List<FormattedCharSequence> messageLines;
    private long lastChanged;
    private boolean changed;
    private final int width;
    private boolean forceHide;

    public SystemToast(SystemToast.SystemToastId p_94832_, Component p_94833_, @Nullable Component p_94834_)
    {
        this(
            p_94832_,
            p_94833_,
            nullToEmpty(p_94834_),
            Math.max(
                160, 30 + Math.max(Minecraft.getInstance().font.width(p_94833_), p_94834_ == null ? 0 : Minecraft.getInstance().font.width(p_94834_))
            )
        );
    }

    public static SystemToast multiline(Minecraft p_94848_, SystemToast.SystemToastId p_94849_, Component p_94850_, Component p_94851_)
    {
        Font font = p_94848_.font;
        List<FormattedCharSequence> list = font.split(p_94851_, 200);
        int i = Math.max(200, list.stream().mapToInt(font::width).max().orElse(200));
        return new SystemToast(p_94849_, p_94850_, list, i + 30);
    }

    private SystemToast(SystemToast.SystemToastId p_94827_, Component p_94828_, List<FormattedCharSequence> p_94829_, int p_94830_)
    {
        this.id = p_94827_;
        this.title = p_94828_;
        this.messageLines = p_94829_;
        this.width = p_94830_;
    }

    private static ImmutableList<FormattedCharSequence> nullToEmpty(@Nullable Component p_94861_)
    {
        return p_94861_ == null ? ImmutableList.of() : ImmutableList.of(p_94861_.getVisualOrderText());
    }

    @Override
    public int width()
    {
        return this.width;
    }

    @Override
    public int height()
    {
        return 20 + Math.max(this.messageLines.size(), 1) * 12;
    }

    public void forceHide()
    {
        this.forceHide = true;
    }

    @Override
    public Toast.Visibility render(GuiGraphics p_281624_, ToastComponent p_282333_, long p_282762_)
    {
        if (this.changed)
        {
            this.lastChanged = p_282762_;
            this.changed = false;
        }

        int i = this.width();

        if (i == 160 && this.messageLines.size() <= 1)
        {
            p_281624_.blitSprite(BACKGROUND_SPRITE, 0, 0, i, this.height());
        }
        else
        {
            int j = this.height();
            int k = 28;
            int l = Math.min(4, j - 28);
            this.renderBackgroundRow(p_281624_, i, 0, 0, 28);

            for (int i1 = 28; i1 < j - l; i1 += 10)
            {
                this.renderBackgroundRow(p_281624_, i, 16, i1, Math.min(16, j - i1 - l));
            }

            this.renderBackgroundRow(p_281624_, i, 32 - l, j - l, l);
        }

        if (this.messageLines.isEmpty())
        {
            p_281624_.drawString(p_282333_.getMinecraft().font, this.title, 18, 12, -256, false);
        }
        else
        {
            p_281624_.drawString(p_282333_.getMinecraft().font, this.title, 18, 7, -256, false);

            for (int j1 = 0; j1 < this.messageLines.size(); j1++)
            {
                p_281624_.drawString(p_282333_.getMinecraft().font, this.messageLines.get(j1), 18, 18 + j1 * 12, -1, false);
            }
        }

        double d0 = (double)this.id.displayTime * p_282333_.getNotificationDisplayTimeMultiplier();
        long k1 = p_282762_ - this.lastChanged;
        return !this.forceHide && (double)k1 < d0 ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }

    private void renderBackgroundRow(GuiGraphics p_281840_, int p_281750_, int p_282371_, int p_283613_, int p_282880_)
    {
        int i = p_282371_ == 0 ? 20 : 5;
        int j = Math.min(60, p_281750_ - i);
        ResourceLocation resourcelocation = BACKGROUND_SPRITE;
        p_281840_.blitSprite(resourcelocation, 160, 32, 0, p_282371_, 0, p_283613_, i, p_282880_);

        for (int k = i; k < p_281750_ - j; k += 64)
        {
            p_281840_.blitSprite(resourcelocation, 160, 32, 32, p_282371_, k, p_283613_, Math.min(64, p_281750_ - k - j), p_282880_);
        }

        p_281840_.blitSprite(resourcelocation, 160, 32, 160 - j, p_282371_, p_281750_ - j, p_283613_, j, p_282880_);
    }

    public void reset(Component p_94863_, @Nullable Component p_94864_)
    {
        this.title = p_94863_;
        this.messageLines = nullToEmpty(p_94864_);
        this.changed = true;
    }

    public SystemToast.SystemToastId getToken()
    {
        return this.id;
    }

    public static void add(ToastComponent p_94856_, SystemToast.SystemToastId p_94857_, Component p_94858_, @Nullable Component p_94859_)
    {
        p_94856_.addToast(new SystemToast(p_94857_, p_94858_, p_94859_));
    }

    public static void addOrUpdate(ToastComponent p_94870_, SystemToast.SystemToastId p_94871_, Component p_94872_, @Nullable Component p_94873_)
    {
        SystemToast systemtoast = p_94870_.getToast(SystemToast.class, p_94871_);

        if (systemtoast == null)
        {
            add(p_94870_, p_94871_, p_94872_, p_94873_);
        }
        else
        {
            systemtoast.reset(p_94872_, p_94873_);
        }
    }

    public static void forceHide(ToastComponent p_311181_, SystemToast.SystemToastId p_311637_)
    {
        SystemToast systemtoast = p_311181_.getToast(SystemToast.class, p_311637_);

        if (systemtoast != null)
        {
            systemtoast.forceHide();
        }
    }

    public static void onWorldAccessFailure(Minecraft p_94853_, String p_94854_)
    {
        add(p_94853_.getToasts(), SystemToast.SystemToastId.WORLD_ACCESS_FAILURE, Component.translatable("selectWorld.access_failure"), Component.literal(p_94854_));
    }

    public static void onWorldDeleteFailure(Minecraft p_94867_, String p_94868_)
    {
        add(p_94867_.getToasts(), SystemToast.SystemToastId.WORLD_ACCESS_FAILURE, Component.translatable("selectWorld.delete_failure"), Component.literal(p_94868_));
    }

    public static void onPackCopyFailure(Minecraft p_94876_, String p_94877_)
    {
        add(p_94876_.getToasts(), SystemToast.SystemToastId.PACK_COPY_FAILURE, Component.translatable("pack.copyFailure"), Component.literal(p_94877_));
    }

    public static void onFileDropFailure(Minecraft p_343671_, int p_343465_)
    {
        add(
            p_343671_.getToasts(),
            SystemToast.SystemToastId.FILE_DROP_FAILURE,
            Component.translatable("gui.fileDropFailure.title"),
            Component.translatable("gui.fileDropFailure.detail", p_343465_)
        );
    }

    public static void onLowDiskSpace(Minecraft p_335579_)
    {
        addOrUpdate(
            p_335579_.getToasts(),
            SystemToast.SystemToastId.LOW_DISK_SPACE,
            Component.translatable("chunk.toast.lowDiskSpace"),
            Component.translatable("chunk.toast.lowDiskSpace.description")
        );
    }

    public static void onChunkLoadFailure(Minecraft p_335709_, ChunkPos p_330201_)
    {
        addOrUpdate(
            p_335709_.getToasts(),
            SystemToast.SystemToastId.CHUNK_LOAD_FAILURE,
            Component.translatable("chunk.toast.loadFailure", Component.translationArg(p_330201_)).withStyle(ChatFormatting.RED),
            Component.translatable("chunk.toast.checkLog")
        );
    }

    public static void onChunkSaveFailure(Minecraft p_328693_, ChunkPos p_333444_)
    {
        addOrUpdate(
            p_328693_.getToasts(),
            SystemToast.SystemToastId.CHUNK_SAVE_FAILURE,
            Component.translatable("chunk.toast.saveFailure", Component.translationArg(p_333444_)).withStyle(ChatFormatting.RED),
            Component.translatable("chunk.toast.checkLog")
        );
    }

    public static class SystemToastId
    {
        public static final SystemToast.SystemToastId NARRATOR_TOGGLE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId WORLD_BACKUP = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId PACK_LOAD_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId WORLD_ACCESS_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId PACK_COPY_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId FILE_DROP_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId PERIODIC_NOTIFICATION = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId LOW_DISK_SPACE = new SystemToast.SystemToastId(10000L);
        public static final SystemToast.SystemToastId CHUNK_LOAD_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId CHUNK_SAVE_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId UNSECURE_SERVER_WARNING = new SystemToast.SystemToastId(10000L);
        final long displayTime;

        public SystemToastId(long p_311745_)
        {
            this.displayTime = p_311745_;
        }

        public SystemToastId()
        {
            this(5000L);
        }
    }
}
