package com.kqp.ezpas.block.pullerpipe;

import com.kqp.ezpas.block.entity.pullerpipe.GoldPullerPipeBlockEntity;
import com.kqp.ezpas.block.entity.pullerpipe.PullerPipeBlockEntity;
import com.kqp.ezpas.Ezpas;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GoldPullerPipeBlock extends PullerPipeBlock {
    public GoldPullerPipeBlock() {
        super();
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GoldPullerPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, Ezpas.GOLD_PP_BLOCK_ENTITY, PullerPipeBlockEntity::tick);
    }
}
