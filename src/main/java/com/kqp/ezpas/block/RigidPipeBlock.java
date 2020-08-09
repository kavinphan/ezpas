package com.kqp.ezpas.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

public class RigidPipeBlock extends PipeBlock {
    @Override
    protected boolean isConnectable(WorldAccess world, BlockPos pos, Direction dir) {
        return world.getBlockState(pos).getBlock() instanceof PipeBlock;
    }
}
