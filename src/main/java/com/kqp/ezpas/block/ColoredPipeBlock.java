package com.kqp.ezpas.block;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.IWorld;

public class ColoredPipeBlock extends PipeBlock {
    @Override
    protected boolean isConnectable(IWorld world, BlockPos pos, Direction dir) {
        Block block = world.getBlockState(pos).getBlock();

        if (block instanceof ColoredPipeBlock) {
            return block == this;
        }

        return super.isConnectable(world, pos, dir);
    }
}
