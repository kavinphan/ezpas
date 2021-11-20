package com.kqp.ezpas.block.pullerpipe;

import com.kqp.ezpas.block.entity.pullerpipe.DiamondPullerPipeBlockEntity;
import com.kqp.ezpas.block.entity.pullerpipe.PullerPipeBlockEntity;
import com.kqp.ezpas.Ezpas;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;

public class DiamondPullerPipeBlock extends PullerPipeBlock {
    public DiamondPullerPipeBlock() {
        super();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DiamondPullerPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, Ezpas.DIAMOND_PP_BLOCK_ENTITY, PullerPipeBlockEntity::tick);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> GameEventListener getGameEventListener(World world, T blockEntity) {
        return super.getGameEventListener(world, blockEntity);
    }
}
