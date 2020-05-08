package net.kqp.ezpas.block;

import net.kqp.ezpas.block.entity.EnderPullerPipeBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.BlockView;

public class EnderPullerPipeBlock extends PullerPipeBlock {
    public EnderPullerPipeBlock() {
        super();
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new EnderPullerPipeBlockEntity();
    }
}
