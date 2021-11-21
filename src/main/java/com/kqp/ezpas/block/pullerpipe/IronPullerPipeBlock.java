package com.kqp.ezpas.block.pullerpipe;

import com.kqp.ezpas.Ezpas;
import com.kqp.ezpas.block.entity.pullerpipe.IronPullerPipeBlockEntity;
import com.kqp.ezpas.block.entity.pullerpipe.PullerPipeBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class IronPullerPipeBlock extends PullerPipeBlock {
    public IronPullerPipeBlock() {
        super();
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new IronPullerPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                  BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, Ezpas.IRON_PP_BLOCK_ENTITY, PullerPipeBlockEntity::tick);
    }
}
