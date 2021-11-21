package com.kqp.ezpas.block.pullerpipe;

import com.kqp.ezpas.block.entity.pullerpipe.PullerPipeBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public abstract class PullerPipeBlock extends BlockWithEntity {
    public static final DirectionProperty FACING;

    public PullerPipeBlock() {
        super(FabricBlockSettings.of(Material.METAL));

        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState newState,
                                                WorldAccess world, BlockPos pos, BlockPos posFrom) {
        BlockEntity be = world.getBlockEntity(pos);

        if (be instanceof PullerPipeBlockEntity) {
            ((PullerPipeBlockEntity) be).markToRecalculate();
        }

        return super.getStateForNeighborUpdate(state, direction, newState, world, pos, posFrom);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        BlockEntity be = world.getBlockEntity(pos);

        if (be instanceof PullerPipeBlockEntity) {
            ((PullerPipeBlockEntity) be).markToRecalculate();
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FacingBlock.FACING, ctx.getPlayerLookDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FacingBlock.FACING, rotation.rotate(state.get(FacingBlock.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FacingBlock.FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FacingBlock.FACING);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    static {
        FACING = FacingBlock.FACING;
    }
}
