package net.minecraft.client.gui.screens.worldselection;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.flag.FeatureFlags;

public class ConfirmExperimentalFeaturesScreen extends Screen
{
    private static final Component TITLE = Component.translatable("selectWorld.experimental.title");
    private static final Component MESSAGE = Component.translatable("selectWorld.experimental.message");
    private static final Component DETAILS_BUTTON = Component.translatable("selectWorld.experimental.details");
    private static final int COLUMN_SPACING = 10;
    private static final int DETAILS_BUTTON_WIDTH = 100;
    private final BooleanConsumer callback;
    final Collection<Pack> enabledPacks;
    private final GridLayout layout = new GridLayout().columnSpacing(10).rowSpacing(20);

    public ConfirmExperimentalFeaturesScreen(Collection<Pack> p_252011_, BooleanConsumer p_250152_)
    {
        super(TITLE);
        this.enabledPacks = p_252011_;
        this.callback = p_250152_;
    }

    @Override
    public Component getNarrationMessage()
    {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), MESSAGE);
    }

    @Override
    protected void init()
    {
        super.init();
        GridLayout.RowHelper gridlayout$rowhelper = this.layout.createRowHelper(2);
        LayoutSettings layoutsettings = gridlayout$rowhelper.newCellSettings().alignHorizontallyCenter();
        gridlayout$rowhelper.addChild(new StringWidget(this.title, this.font), 2, layoutsettings);
        MultiLineTextWidget multilinetextwidget = gridlayout$rowhelper.addChild(
                    new MultiLineTextWidget(MESSAGE, this.font).setCentered(true), 2, layoutsettings
                );
        multilinetextwidget.setMaxWidth(310);
        gridlayout$rowhelper.addChild(
            Button.builder(DETAILS_BUTTON, p_280898_ -> this.minecraft.setScreen(new ConfirmExperimentalFeaturesScreen.DetailsScreen())).width(100).build(),
            2,
            layoutsettings
        );
        gridlayout$rowhelper.addChild(Button.builder(CommonComponents.GUI_PROCEED, p_252248_ -> this.callback.accept(true)).build());
        gridlayout$rowhelper.addChild(Button.builder(CommonComponents.GUI_BACK, p_250397_ -> this.callback.accept(false)).build());
        this.layout.visitWidgets(p_325417_ ->
        {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_325417_);
        });
        this.layout.arrangeElements();
        this.repositionElements();
    }

    @Override
    protected void repositionElements()
    {
        FrameLayout.alignInRectangle(this.layout, 0, 0, this.width, this.height, 0.5F, 0.5F);
    }

    @Override
    public void onClose()
    {
        this.callback.accept(false);
    }

    class DetailsScreen extends Screen
    {
        private static final Component TITLE = Component.translatable("selectWorld.experimental.details.title");
        final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        @Nullable
        private ConfirmExperimentalFeaturesScreen.DetailsScreen.PackList list;

        DetailsScreen()
        {
            super(TITLE);
        }

        @Override
        protected void init()
        {
            this.layout.addTitleHeader(TITLE, this.font);
            this.list = this.layout
                             .addToContents(new ConfirmExperimentalFeaturesScreen.DetailsScreen.PackList(this.minecraft, ConfirmExperimentalFeaturesScreen.this.enabledPacks));
            this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, p_251286_ -> this.onClose()).build());
            this.layout.visitWidgets(p_325419_ ->
            {
                AbstractWidget abstractwidget = this.addRenderableWidget(p_325419_);
            });
            this.repositionElements();
        }

        @Override
        protected void repositionElements()
        {
            if (this.list != null)
            {
                this.list.updateSize(this.width, this.layout);
            }

            this.layout.arrangeElements();
        }

        @Override
        public void onClose()
        {
            this.minecraft.setScreen(ConfirmExperimentalFeaturesScreen.this);
        }

        class PackList extends ObjectSelectionList<ConfirmExperimentalFeaturesScreen.DetailsScreen.PackListEntry>
        {
            public PackList(final Minecraft p_249776_, final Collection<Pack> p_251183_)
            {
                super(p_249776_, DetailsScreen.this.width, DetailsScreen.this.layout.getContentHeight(), DetailsScreen.this.layout.getHeaderHeight(), (9 + 2) * 3);

                for (Pack pack : p_251183_)
                {
                    String s = FeatureFlags.printMissingFlags(FeatureFlags.VANILLA_SET, pack.getRequestedFeatures());

                    if (!s.isEmpty())
                    {
                        Component component = ComponentUtils.mergeStyles(pack.getTitle().copy(), Style.EMPTY.withBold(true));
                        Component component1 = Component.translatable("selectWorld.experimental.details.entry", s);
                        this.addEntry(
                            DetailsScreen.this.new PackListEntry(
                                component, component1, MultiLineLabel.create(DetailsScreen.this.font, component1, this.getRowWidth())
                            )
                        );
                    }
                }
            }

            @Override
            public int getRowWidth()
            {
                return this.width * 3 / 4;
            }
        }

        class PackListEntry extends ObjectSelectionList.Entry<ConfirmExperimentalFeaturesScreen.DetailsScreen.PackListEntry>
        {
            private final Component packId;
            private final Component message;
            private final MultiLineLabel splitMessage;

            PackListEntry(final Component p_250724_, final Component p_248883_, final MultiLineLabel p_250949_)
            {
                this.packId = p_250724_;
                this.message = p_248883_;
                this.splitMessage = p_250949_;
            }

            @Override
            public void render(
                GuiGraphics p_282199_,
                int p_282727_,
                int p_283089_,
                int p_283116_,
                int p_281268_,
                int p_283038_,
                int p_283070_,
                int p_282448_,
                boolean p_281417_,
                float p_283226_
            )
            {
                p_282199_.drawString(DetailsScreen.this.minecraft.font, this.packId, p_283116_, p_283089_, -1);
                this.splitMessage.renderLeftAligned(p_282199_, p_283116_, p_283089_ + 12, 9, -1);
            }

            @Override
            public Component getNarration()
            {
                return Component.translatable("narrator.select", CommonComponents.joinForNarration(this.packId, this.message));
            }
        }
    }
}
