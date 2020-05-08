package net.kqp.ezpas.block;

import net.kqp.ezpas.block.entity.IronPullerPipeBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.BlockView;

public class IronPullerPipeBlock extends PullerPipeBlock {
    public IronPullerPipeBlock() {
        super();
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new IronPullerPipeBlockEntity();
    }
}
