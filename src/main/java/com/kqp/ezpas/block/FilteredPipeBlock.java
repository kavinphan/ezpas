package com.kqp.ezpas.block;

import com.kqp.ezpas.block.entity.FilteredPipeBlockEntity;
import com.kqp.ezpas.block.entity.pullerpipe.PullerPipeBlockEntity;
import com.kqp.ezpas.block.pullerpipe.PullerPipeBlock;
import com.kqp.ezpas.init.Ezpas;
import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

public class FilteredPipeBlock extends BlockWithEntity {
    public final Type type;

    public FilteredPipeBlock(Type type) {
        super(FabricBlockSettings.of(Material.GLASS).strength(0.3F, 0.3F).nonOpaque().sounds(BlockSoundGroup.GLASS).nonOpaque().build());

        this.type = type;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockView view) {
        return new FilteredPipeBlockEntity();
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

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState newState, IWorld world, BlockPos pos, BlockPos posFrom) {
        updateSystem(world, pos, new HashSet());

        return super.getStateForNeighborUpdate(state, direction, newState, world, pos, posFrom);
    }

    public void updateSystem(IWorld world, BlockPos blockPos, Set<BlockPos> searched) {
        if (!searched.contains(blockPos)) {
            searched.add(blockPos);

            Block block = world.getBlockState(blockPos).getBlock();

            if (block == this || block instanceof PipeBlock) {
                for (int i = 0; i < Direction.values().length; i++) {
                    updateSystem(world, blockPos.offset(Direction.values()[i]), searched);
                }
            } else if (block instanceof PullerPipeBlock) {
                BlockEntity be = world.getBlockEntity(blockPos);

                if (be instanceof PullerPipeBlockEntity) {
                    ((PullerPipeBlockEntity) be).updateSystem();
                }
            }
        }
    }

    public enum Type {
        WHITELIST,
        BLACKLIST;
    }
}
