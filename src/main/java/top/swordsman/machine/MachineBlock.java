package top.swordsman.machine;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MachineBlock extends Block implements EntityBlock {

    private final Supplier<BlockEntityType<?>> beSupplier;
    private final Component title;

    public MachineBlock(Supplier<BlockEntityType<?>> beSupplier, Component title, BlockBehaviour.Properties props) {
        super(props);
        this.beSupplier = beSupplier;
        this.title = title;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return beSupplier.get().create(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof AbstractMachineBlockEntity m) m.serverTick();
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openMachine(level, pos, player);
    }

    @Override
    public net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        openMachine(level, pos, player);
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult openMachine(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractMachineBlockEntity m) {
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> new MachineMenu(id, inv, m), title));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractMachineBlockEntity m) Containers.dropContents(level, pos, m);
            super.onRemove(state, level, pos, newState, moved);
        }
    }
}
