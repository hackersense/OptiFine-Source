package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class DropperBlock extends DispenserBlock
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<DropperBlock> CODEC = simpleCodec(DropperBlock::new);
    private static final DispenseItemBehavior DISPENSE_BEHAVIOUR = new DefaultDispenseItemBehavior();

    @Override
    public MapCodec<DropperBlock> codec()
    {
        return CODEC;
    }

    public DropperBlock(BlockBehaviour.Properties p_52942_)
    {
        super(p_52942_);
    }

    @Override
    protected DispenseItemBehavior getDispenseMethod(Level p_331221_, ItemStack p_52947_)
    {
        return DISPENSE_BEHAVIOUR;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_153179_, BlockState p_153180_)
    {
        return new DropperBlockEntity(p_153179_, p_153180_);
    }

    @Override
    protected void dispenseFrom(ServerLevel p_52944_, BlockState p_301813_, BlockPos p_52945_)
    {
        DispenserBlockEntity dispenserblockentity = p_52944_.getBlockEntity(p_52945_, BlockEntityType.DROPPER).orElse(null);

        if (dispenserblockentity == null)
        {
            LOGGER.warn("Ignoring dispensing attempt for Dropper without matching block entity at {}", p_52945_);
        }
        else
        {
            BlockSource blocksource = new BlockSource(p_52944_, p_52945_, p_301813_, dispenserblockentity);
            int i = dispenserblockentity.getRandomSlot(p_52944_.random);

            if (i < 0)
            {
                p_52944_.levelEvent(1001, p_52945_, 0);
            }
            else
            {
                ItemStack itemstack = dispenserblockentity.getItem(i);

                if (!itemstack.isEmpty())
                {
                    Direction direction = p_52944_.getBlockState(p_52945_).getValue(FACING);
                    Container container = HopperBlockEntity.getContainerAt(p_52944_, p_52945_.relative(direction));
                    ItemStack itemstack1;

                    if (container == null)
                    {
                        itemstack1 = DISPENSE_BEHAVIOUR.dispense(blocksource, itemstack);
                    }
                    else
                    {
                        itemstack1 = HopperBlockEntity.addItem(dispenserblockentity, container, itemstack.copyWithCount(1), direction.getOpposite());

                        if (itemstack1.isEmpty())
                        {
                            itemstack1 = itemstack.copy();
                            itemstack1.shrink(1);
                        }
                        else
                        {
                            itemstack1 = itemstack.copy();
                        }
                    }

                    dispenserblockentity.setItem(i, itemstack1);
                }
            }
        }
    }
}
