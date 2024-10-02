package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public class VaultBlock extends BaseEntityBlock
{
    public static final MapCodec<VaultBlock> CODEC = simpleCodec(VaultBlock::new);
    public static final Property<VaultState> STATE = BlockStateProperties.VAULT_STATE;
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OMINOUS = BlockStateProperties.OMINOUS;

    @Override
    public MapCodec<VaultBlock> codec()
    {
        return CODEC;
    }

    public VaultBlock(BlockBehaviour.Properties p_332394_)
    {
        super(p_332394_);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STATE, VaultState.INACTIVE).setValue(OMINOUS, Boolean.valueOf(false))
        );
    }

    @Override
    public ItemInteractionResult useItemOn(
        ItemStack p_330793_, BlockState p_331776_, Level p_335228_, BlockPos p_334682_, Player p_334435_, InteractionHand p_332576_, BlockHitResult p_328969_
    )
    {
        if (p_330793_.isEmpty() || p_331776_.getValue(STATE) != VaultState.ACTIVE)
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        else if (p_335228_ instanceof ServerLevel serverlevel)
        {
            if (serverlevel.getBlockEntity(p_334682_) instanceof VaultBlockEntity vaultblockentity)
            {
                VaultBlockEntity.Server.tryInsertKey(
                    serverlevel,
                    p_334682_,
                    p_331776_,
                    vaultblockentity.getConfig(),
                    vaultblockentity.getServerData(),
                    vaultblockentity.getSharedData(),
                    p_334435_,
                    p_330793_
                );
                return ItemInteractionResult.SUCCESS;
            }
            else
            {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
        }
        else
        {
            return ItemInteractionResult.CONSUME;
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_330778_, BlockState p_329139_)
    {
        return new VaultBlockEntity(p_330778_, p_329139_);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_334106_)
    {
        p_334106_.add(FACING, STATE, OMINOUS);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_328167_, BlockState p_334496_, BlockEntityType<T> p_335892_)
    {
        return p_328167_ instanceof ServerLevel serverlevel
               ? createTickerHelper(
                   p_335892_,
                   BlockEntityType.VAULT,
                   (p_333393_, p_329496_, p_334876_, p_335304_) -> VaultBlockEntity.Server.tick(
                       serverlevel, p_329496_, p_334876_, p_335304_.getConfig(), p_335304_.getServerData(), p_335304_.getSharedData()
                   )
               )
               : createTickerHelper(
                   p_335892_,
                   BlockEntityType.VAULT,
                   (p_329262_, p_332751_, p_331862_, p_336114_) -> VaultBlockEntity.Client.tick(
                       p_329262_, p_332751_, p_331862_, p_336114_.getClientData(), p_336114_.getSharedData()
                   )
               );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_328081_)
    {
        return this.defaultBlockState().setValue(FACING, p_328081_.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState p_333257_, Rotation p_329014_)
    {
        return p_333257_.setValue(FACING, p_329014_.rotate(p_333257_.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState p_330957_, Mirror p_329929_)
    {
        return p_330957_.rotate(p_329929_.getRotation(p_330957_.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState p_329085_)
    {
        return RenderShape.MODEL;
    }
}
