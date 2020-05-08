package com.kqp.ezpas.block;

import com.kqp.ezpas.block.entity.GoldPullerPipeBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.BlockView;

public class GoldPullerPipeBlock extends PullerPipeBlock {
    public GoldPullerPipeBlock() {
        super();
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new GoldPullerPipeBlockEntity();
    }
}
