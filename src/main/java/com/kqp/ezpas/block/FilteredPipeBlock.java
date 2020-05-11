package com.kqp.ezpas.block;

import com.kqp.ezpas.block.entity.FilteredPipeBlockEntity;
import com.kqp.ezpas.init.Ezpas;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.container.NameableContainerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class FilteredPipeBlock extends PipeBlock implements BlockEntityProvider {
    public final Type type;

    public FilteredPipeBlock(Type type) {
        super();

        this.type = type;
    }

    @Override
    public boolean onBlockAction(BlockState state, World world, BlockPos pos, int type, int data) {
        super.onBlockAction(state, world, pos, type, data);

        BlockEntity blockEntity = world.getBlockEntity(pos);

        return blockEntity == null ? false : blockEntity.onBlockAction(type, data);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView view) {
        return new FilteredPipeBlockEntity();
    }

    @Override
    public NameableContainerFactory createContainerFactory(BlockState state, World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);

        return blockEntity instanceof NameableContainerFactory ? (NameableContainerFactory) blockEntity : null;
    }

    @Override
    public void onBlockRemoved(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof Inventory) {
                ItemScatterer.spawn(world, pos, (Inventory) blockEntity);
                world.updateHorizontalAdjacent(pos, this);
            }

            super.onBlockRemoved(state, world, pos, newState, moved);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof FilteredPipeBlockEntity) {
                ContainerProviderRegistry.INSTANCE.openContainer(Ezpas.FILTERED_PIPE_ID, player, buf -> buf.writeBlockPos(pos));
            }
        }

        return ActionResult.SUCCESS;
    }

    public enum Type {
        WHITELIST,
        BLACKLIST
    }
}
