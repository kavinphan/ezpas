package com.kqp.ezpas.block.entity.pullerpipe;

import com.kqp.ezpas.init.Ezpas;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class DiamondPullerPipeBlockEntity extends PullerPipeBlockEntity {
    public DiamondPullerPipeBlockEntity(BlockPos pos, BlockState state) {
        super(Ezpas.DIAMOND_PP_BLOCK_ENTITY, pos, state, 1, 1, 4);
    }
}
