package com.kqp.ezpas.block.pullerpipe;

import com.kqp.ezpas.block.entity.pullerpipe.EnderPullerPipeBlockEntity;
import com.kqp.ezpas.block.entity.pullerpipe.PullerPipeBlockEntity;
import com.kqp.ezpas.Ezpas;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EnderPullerPipeBlock extends PullerPipeBlock {
    public EnderPullerPipeBlock() {
        super();
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EnderPullerPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, Ezpas.ENDER_PP_BLOCK_ENTITY, PullerPipeBlockEntity::tick);
    }
}
