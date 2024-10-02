package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class BackupConfirmScreen extends Screen
{
    private final Runnable onCancel;
    protected final BackupConfirmScreen.Listener onProceed;
    private final Component description;
    private final boolean promptForCacheErase;
    private MultiLineLabel message = MultiLineLabel.EMPTY;
    protected int id;
    private Checkbox eraseCache;

    public BackupConfirmScreen(Runnable p_309995_, BackupConfirmScreen.Listener p_95544_, Component p_95545_, Component p_95546_, boolean p_95547_)
    {
        super(p_95545_);
        this.onCancel = p_309995_;
        this.onProceed = p_95544_;
        this.description = p_95546_;
        this.promptForCacheErase = p_95547_;
    }

    @Override
    protected void init()
    {
        super.init();
        this.message = MultiLineLabel.create(this.font, this.description, this.width - 50);
        int i = (this.message.getLineCount() + 1) * 9;
        this.addRenderableWidget(
            Button.builder(Component.translatable("selectWorld.backupJoinConfirmButton"), p_308190_ -> this.onProceed.proceed(true, this.eraseCache.selected()))
            .bounds(this.width / 2 - 155, 100 + i, 150, 20)
            .build()
        );
        this.addRenderableWidget(
            Button.builder(Component.translatable("selectWorld.backupJoinSkipButton"), p_308188_ -> this.onProceed.proceed(false, this.eraseCache.selected()))
            .bounds(this.width / 2 - 155 + 160, 100 + i, 150, 20)
            .build()
        );
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL, p_308189_ -> this.onCancel.run())
            .bounds(this.width / 2 - 155 + 80, 124 + i, 150, 20)
            .build()
        );
        this.eraseCache = Checkbox.builder(Component.translatable("selectWorld.backupEraseCache"), this.font)
                        .pos(this.width / 2 - 155 + 80, 76 + i)
                        .build();

        if (this.promptForCacheErase)
        {
            this.addRenderableWidget(this.eraseCache);
        }
    }

    @Override
    public void render(GuiGraphics p_282759_, int p_282356_, int p_282725_, float p_281518_)
    {
        super.render(p_282759_, p_282356_, p_282725_, p_281518_);
        p_282759_.drawCenteredString(this.font, this.title, this.width / 2, 50, 16777215);
        this.message.renderCentered(p_282759_, this.width / 2, 70);
    }

    @Override
    public boolean shouldCloseOnEsc()
    {
        return false;
    }

    @Override
    public boolean keyPressed(int p_95549_, int p_95550_, int p_95551_)
    {
        if (p_95549_ == 256)
        {
            this.onCancel.run();
            return true;
        }
        else
        {
            return super.keyPressed(p_95549_, p_95550_, p_95551_);
        }
    }

    public interface Listener
    {
        void proceed(boolean p_95566_, boolean p_95567_);
    }
}
