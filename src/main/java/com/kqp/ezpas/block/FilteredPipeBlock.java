package com.kqp.ezpas.block;

import com.kqp.ezpas.block.entity.FilteredPipeBlockEntity;
import com.kqp.ezpas.init.Ezpas;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class FilteredPipeBlock extends PipeBlock implements BlockEntityProvider {
    public final Type type;

    public FilteredPipeBlock(Type type) {
        super();

        this.type = type;
    }

    @Override
    public BlockEntity createBlockEntity(BlockView view) {
        return new FilteredPipeBlockEntity();
    }

    @Override
    public boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data) {
        super.onSyncedBlockEvent(state, world, pos, type, data);

        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity == null ? false : blockEntity.onSyncedBlockEvent(type, data);
    }

    @Override
    public NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof NamedScreenHandlerFactory ? (NamedScreenHandlerFactory) blockEntity : null;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof FilteredPipeBlockEntity) {
                ItemScatterer.spawn(world, pos, (Inventory) blockEntity);

                world.updateComparators(pos, this);
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof FilteredPipeBlockEntity) {
                ((ServerPlayerEntity) player).openHandledScreen((FilteredPipeBlockEntity) be);
            }
        }

        return ActionResult.SUCCESS;
    }

    public enum Type {
        WHITELIST("pipe_probe.whitelist_header"),
        BLACKLIST("pipe_probe.blacklist_header");

        public final String localizationKey;

        Type(String localizationKey) {
            this.localizationKey = localizationKey;
        }
    }
}
