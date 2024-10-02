package net.minecraft.client.gui.components.toasts;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

public interface Toast
{
    Object NO_TOKEN = new Object();
    int SLOT_HEIGHT = 32;

    Toast.Visibility render(GuiGraphics p_281969_, ToastComponent p_94897_, long p_94898_);

default Object getToken()
    {
        return NO_TOKEN;
    }

default int width()
    {
        return 160;
    }

default int height()
    {
        return 32;
    }

default int slotCount()
    {
        return Mth.positiveCeilDiv(this.height(), 32);
    }

    public static enum Visibility
    {
        SHOW(SoundEvents.UI_TOAST_IN),
        HIDE(SoundEvents.UI_TOAST_OUT);

        private final SoundEvent soundEvent;

        private Visibility(final SoundEvent p_94908_)
        {
            this.soundEvent = p_94908_;
        }

        public void playSound(SoundManager p_94910_)
        {
            p_94910_.play(SimpleSoundInstance.forUI(this.soundEvent, 1.0F, 1.0F));
        }
    }
}
