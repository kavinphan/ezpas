package com.kqp.ezpas.block.entity.pullerpipe;

import com.kqp.ezpas.init.Ezpas;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class NetheritePullerPipeBlockEntity extends PullerPipeBlockEntity {
    public NetheritePullerPipeBlockEntity(BlockPos pos, BlockState state) {
        super(Ezpas.NETHERITE_PP_BLOCK_ENTITY, pos, state, 1, 16, 4);
    }
}
