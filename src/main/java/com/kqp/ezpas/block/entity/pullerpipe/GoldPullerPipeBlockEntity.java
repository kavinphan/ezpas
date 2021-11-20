package com.kqp.ezpas.block.entity.pullerpipe;

import com.kqp.ezpas.Ezpas;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class GoldPullerPipeBlockEntity extends PullerPipeBlockEntity {
    public GoldPullerPipeBlockEntity(BlockPos pos, BlockState state) {
        super(Ezpas.GOLD_PP_BLOCK_ENTITY, pos, state, 6, 1, 1);
    }
}
