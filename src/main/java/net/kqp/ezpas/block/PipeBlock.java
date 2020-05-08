package net.kqp.ezpas.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.kqp.ezpas.block.entity.PullerPipeBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;

import java.util.HashSet;
import java.util.Set;

public class PipeBlock extends Block {
    public PipeBlock() {
        super(FabricBlockSettings.of(Material.GLASS).strength(0.3F, 0.3F).nonOpaque().sounds(BlockSoundGroup.GLASS).nonOpaque().build());
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState newState, IWorld world, BlockPos pos, BlockPos posFrom) {
        updatePullerPipes(world, pos, new HashSet());

        return super.getStateForNeighborUpdate(state, direction, newState, world, pos, posFrom);
    }

    private void updatePullerPipes(IWorld world, BlockPos blockPos, Set<BlockPos> searched) {
        if (!searched.contains(blockPos)) {
            searched.add(blockPos);

            Block block = world.getBlockState(blockPos).getBlock();

            if (block.is(this)) {
                for (int i = 0; i < Direction.values().length; i++) {
                    updatePullerPipes(world, blockPos.offset(Direction.values()[i]), searched);
                }
            } else if (block instanceof PullerPipeBlock) {
                BlockEntity be = world.getBlockEntity(blockPos);

                if (be instanceof PullerPipeBlockEntity) {
                    ((PullerPipeBlockEntity) be).updateOutputs();
                }
            }
        }
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean isTranslucent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        return stateFrom.isOf(this) || super.isSideInvisible(state, stateFrom, direction);
    }
}
