package net.kqp.ezpas.block;

import net.kqp.ezpas.block.entity.DiamondPullerPipeBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.BlockView;

public class DiamondPullerPipeBlock extends PullerPipeBlock {
    public DiamondPullerPipeBlock() {
        super();
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new DiamondPullerPipeBlockEntity();
    }
}
