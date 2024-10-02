package net.minecraft.realms;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class DisconnectedRealmsScreen extends RealmsScreen
{
    private final Component reason;
    private MultiLineLabel message = MultiLineLabel.EMPTY;
    private final Screen parent;
    private int textHeight;

    public DisconnectedRealmsScreen(Screen p_120653_, Component p_120654_, Component p_120655_)
    {
        super(p_120654_);
        this.parent = p_120653_;
        this.reason = p_120655_;
    }

    @Override
    public void init()
    {
        this.minecraft.getDownloadedPackSource().cleanupAfterDisconnect();
        this.message = MultiLineLabel.create(this.font, this.reason, this.width - 50);
        this.textHeight = this.message.getLineCount() * 9;
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_BACK, p_296421_ -> this.minecraft.setScreen(this.parent))
            .bounds(this.width / 2 - 100, this.height / 2 + this.textHeight / 2 + 9, 200, 20)
            .build()
        );
    }

    @Override
    public Component getNarrationMessage()
    {
        return Component.empty().append(this.title).append(": ").append(this.reason);
    }

    @Override
    public void onClose()
    {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics p_282959_, int p_120658_, int p_120659_, float p_120660_)
    {
        super.render(p_282959_, p_120658_, p_120659_, p_120660_);
        p_282959_.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - this.textHeight / 2 - 9 * 2, 11184810);
        this.message.renderCentered(p_282959_, this.width / 2, this.height / 2 - this.textHeight / 2);
    }
}
