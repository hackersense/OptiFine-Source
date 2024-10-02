package net.minecraft.client;

import com.mojang.logging.LogUtils;
import com.mojang.text2speech.Narrator;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

public class GameNarrator
{
    public static final Component NO_TITLE = CommonComponents.EMPTY;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;
    private final Narrator narrator = Narrator.getNarrator();

    public GameNarrator(Minecraft p_240577_)
    {
        this.minecraft = p_240577_;
    }

    public void sayChat(Component p_263413_)
    {
        if (this.getStatus().shouldNarrateChat())
        {
            String s = p_263413_.getString();
            this.logNarratedMessage(s);
            this.narrator.say(s, false);
        }
    }

    public void say(Component p_263389_)
    {
        String s = p_263389_.getString();

        if (this.getStatus().shouldNarrateSystem() && !s.isEmpty())
        {
            this.logNarratedMessage(s);
            this.narrator.say(s, false);
        }
    }

    public void sayNow(Component p_168786_)
    {
        this.sayNow(p_168786_.getString());
    }

    public void sayNow(String p_93320_)
    {
        if (this.getStatus().shouldNarrateSystem() && !p_93320_.isEmpty())
        {
            this.logNarratedMessage(p_93320_);

            if (this.narrator.active())
            {
                this.narrator.clear();
                this.narrator.say(p_93320_, true);
            }
        }
    }

    private NarratorStatus getStatus()
    {
        return this.minecraft.options.narrator().get();
    }

    private void logNarratedMessage(String p_168788_)
    {
        if (SharedConstants.IS_RUNNING_IN_IDE)
        {
            LOGGER.debug("Narrating: {}", p_168788_.replaceAll("\n", "\\\\n"));
        }
    }

    public void updateNarratorStatus(NarratorStatus p_93318_)
    {
        this.clear();
        this.narrator.say(Component.translatable("options.narrator").append(" : ").append(p_93318_.getName()).getString(), true);
        ToastComponent toastcomponent = Minecraft.getInstance().getToasts();

        if (this.narrator.active())
        {
            if (p_93318_ == NarratorStatus.OFF)
            {
                SystemToast.addOrUpdate(toastcomponent, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("narrator.toast.disabled"), null);
            }
            else
            {
                SystemToast.addOrUpdate(toastcomponent, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("narrator.toast.enabled"), p_93318_.getName());
            }
        }
        else
        {
            SystemToast.addOrUpdate(
                toastcomponent,
                SystemToast.SystemToastId.NARRATOR_TOGGLE,
                Component.translatable("narrator.toast.disabled"),
                Component.translatable("options.narrator.notavailable")
            );
        }
    }

    public boolean isActive()
    {
        return this.narrator.active();
    }

    public void clear()
    {
        if (this.getStatus() != NarratorStatus.OFF && this.narrator.active())
        {
            this.narrator.clear();
        }
    }

    public void destroy()
    {
        this.narrator.destroy();
    }

    public void checkStatus(boolean p_289016_)
    {
        if (p_289016_
                && !this.isActive()
                && !TinyFileDialogs.tinyfd_messageBox(
                    "Minecraft",
                    "Failed to initialize text-to-speech library. Do you want to continue?\nIf this problem persists, please report it at bugs.mojang.com",
                    "yesno",
                    "error",
                    true
                ))
        {
            throw new GameNarrator.NarratorInitException("Narrator library is not active");
        }
    }

    public static class NarratorInitException extends SilentInitException
    {
        public NarratorInitException(String p_288985_)
        {
            super(p_288985_);
        }
    }
}
