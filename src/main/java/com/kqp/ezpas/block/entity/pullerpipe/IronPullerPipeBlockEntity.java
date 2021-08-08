package com.kqp.ezpas.block.entity.pullerpipe;

import com.kqp.ezpas.init.Ezpas;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class IronPullerPipeBlockEntity extends PullerPipeBlockEntity {
    public IronPullerPipeBlockEntity(BlockPos pos, BlockState state) {
        super(Ezpas.IRON_PP_BLOCK_ENTITY, pos, state, 12, 1, 1);
    }
}
