package net.minecraft.client.gui.screens.inventory;

import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.world.entity.vehicle.MinecartCommandBlock;
import net.minecraft.world.level.BaseCommandBlock;

public class MinecartCommandBlockEditScreen extends AbstractCommandBlockEditScreen
{
    private final BaseCommandBlock commandBlock;

    public MinecartCommandBlockEditScreen(BaseCommandBlock p_99216_)
    {
        this.commandBlock = p_99216_;
    }

    @Override
    public BaseCommandBlock getCommandBlock()
    {
        return this.commandBlock;
    }

    @Override
    int getPreviousY()
    {
        return 150;
    }

    @Override
    protected void init()
    {
        super.init();
        this.commandEdit.setValue(this.getCommandBlock().getCommand());
    }

    @Override
    protected void populateAndSendPacket(BaseCommandBlock p_99218_)
    {
        if (p_99218_ instanceof MinecartCommandBlock.MinecartCommandBase minecartcommandblock$minecartcommandbase)
        {
            this.minecraft
            .getConnection()
            .send(
                new ServerboundSetCommandMinecartPacket(
                    minecartcommandblock$minecartcommandbase.getMinecart().getId(), this.commandEdit.getValue(), p_99218_.isTrackOutput()
                )
            );
        }
    }
}
