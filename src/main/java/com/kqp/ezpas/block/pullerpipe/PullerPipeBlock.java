package com.kqp.ezpas.block.pullerpipe;

import com.kqp.ezpas.block.entity.pullerpipe.PullerPipeBlockEntity;
import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.Material;
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
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public abstract class PullerPipeBlock extends BlockWithEntity {
    public static final DirectionProperty FACING;

    public PullerPipeBlock() {
        super(FabricBlockSettings.of(Material.METAL).build());

        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
        );
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState newState, IWorld world, BlockPos pos, BlockPos posFrom) {
        BlockEntity be = world.getBlockEntity(pos);

        if (be instanceof PullerPipeBlockEntity) {
            ((PullerPipeBlockEntity) be).searchForSystem();
        }

        return super.getStateForNeighborUpdate(state, direction, newState, world, pos, posFrom);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        BlockEntity be = world.getBlockEntity(pos);

        if (be instanceof PullerPipeBlockEntity) {
            ((PullerPipeBlockEntity) be).searchForSystem();
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
