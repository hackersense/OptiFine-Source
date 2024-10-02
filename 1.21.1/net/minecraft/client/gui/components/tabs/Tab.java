package net.minecraft.client.gui.components.tabs;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

public interface Tab
{
    Component getTabTitle();

    void visitChildren(Consumer<AbstractWidget> p_268213_);

    void doLayout(ScreenRectangle p_268081_);
}
