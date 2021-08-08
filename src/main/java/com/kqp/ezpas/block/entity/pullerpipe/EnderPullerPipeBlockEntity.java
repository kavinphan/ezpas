package com.kqp.ezpas.block.entity.pullerpipe;

import com.kqp.ezpas.init.Ezpas;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class EnderPullerPipeBlockEntity extends PullerPipeBlockEntity {
    public EnderPullerPipeBlockEntity(BlockPos pos, BlockState state) {
        super(Ezpas.ENDER_PP_BLOCK_ENTITY, pos, state, 10, 64, 1);
    }
}
