package net.minecraft.client.gui.screens.options;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.optifine.Lang;
import net.optifine.gui.GuiScreenCapeOF;

public class SkinCustomizationScreen extends OptionsSubScreen
{
    private static final Component TITLE = Component.translatable("options.skinCustomisation.title");
    private Button btnCape;
    private int countButtons;

    public SkinCustomizationScreen(Screen p_343566_, Options p_344917_)
    {
        super(p_343566_, p_344917_, TITLE);
    }

    @Override
    protected void addOptions()
    {
        List<AbstractWidget> list = new ArrayList<>();

        for (PlayerModelPart playermodelpart : PlayerModelPart.values())
        {
            list.add(
                CycleButton.onOffBuilder(this.options.isModelPartEnabled(playermodelpart))
                .create(playermodelpart.getName(), (p_338916_2_, p_338916_3_) -> this.options.toggleModelPart(playermodelpart, p_338916_3_))
            );
        }

        list.add(this.options.mainHand().createButton(this.options));
        this.list.addSmall(list);
        this.countButtons = list.size();
        this.btnCape = Button.builder(Lang.getComponent("of.options.skinCustomisation.ofCape"), button -> this.minecraft.setScreen(new GuiScreenCapeOF(this)))
                       .size(200, 20)
                       .build();
        this.btnCape.setPosition(this.width / 2 - 100, this.layout.getHeaderHeight() + 8 + 24 * (this.countButtons / 2));
        this.addRenderableWidget(this.btnCape);
    }

    @Override
    protected void repositionElements()
    {
        super.repositionElements();

        if (this.btnCape != null)
        {
            this.removeWidget(this.btnCape);
            this.addRenderableWidget(this.btnCape);
            this.btnCape.setPosition(this.width / 2 - 100, this.layout.getHeaderHeight() + 8 + 24 * (this.countButtons / 2));
        }
    }
}
