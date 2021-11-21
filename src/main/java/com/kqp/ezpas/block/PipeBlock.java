package com.kqp.ezpas.block;

import com.kqp.ezpas.block.entity.pullerpipe.PullerPipeBlockEntity;
import com.kqp.ezpas.block.pullerpipe.PullerPipeBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@SuppressWarnings("deprecation")
public class PipeBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.of("north");
    public static final BooleanProperty EAST = BooleanProperty.of("east");
    public static final BooleanProperty SOUTH = BooleanProperty.of("south");
    public static final BooleanProperty WEST = BooleanProperty.of("west");
    public static final BooleanProperty UP = BooleanProperty.of("up");
    public static final BooleanProperty DOWN = BooleanProperty.of("down");

    public static final Map<Direction, BooleanProperty> PROP_MAP = Util.make(new HashMap<Direction, BooleanProperty>(),
        map -> {
            map.put(Direction.NORTH, NORTH);
            map.put(Direction.EAST, EAST);
            map.put(Direction.SOUTH, SOUTH);
            map.put(Direction.WEST, WEST);
            map.put(Direction.UP, UP);
            map.put(Direction.DOWN, DOWN);
        }
    );

    public PipeBlock() {
        super(FabricBlockSettings.of(Material.GLASS).strength(0.3F, 0.3F).sounds(BlockSoundGroup.GLASS).nonOpaque());

        this.setDefaultState(this.getStateManager()
            .getDefaultState()
            .with(NORTH, false)
            .with(EAST, false)
            .with(SOUTH, false)
            .with(WEST, false)
            .with(UP, false)
            .with(DOWN, false));
    }

    public Property<Boolean> getProperty(Direction facing) {
        return PROP_MAP.get(facing);
    }

    private BlockState makeConnections(World world, BlockPos pos) {
        Boolean north = isConnectable(world, pos.north(), Direction.SOUTH);
        Boolean east = isConnectable(world, pos.east(), Direction.WEST);
        Boolean south = isConnectable(world, pos.south(), Direction.NORTH);
        Boolean west = isConnectable(world, pos.west(), Direction.EAST);
        Boolean up = isConnectable(world, pos.up(), Direction.DOWN);
        Boolean down = isConnectable(world, pos.down(), Direction.UP);

        return this.getDefaultState()
            .with(NORTH, north)
            .with(EAST, east)
            .with(SOUTH, south)
            .with(WEST, west)
            .with(UP, up)
            .with(DOWN, down);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState newState,
                                                WorldAccess world, BlockPos pos, BlockPos posFrom) {
        PullerPipeBlockEntity.updatePullerPipes(world, pos, direction, new HashSet<>());

        Boolean value = isConnectable(world, posFrom, direction.getOpposite());
        return state.with(getProperty(direction), value);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return makeConnections(context.getWorld(), context.getBlockPos());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return PipeShapeUtil.getShape(state);
    }

    protected boolean isConnectable(WorldAccess world, BlockPos pos, Direction dir) {
        Block block = world.getBlockState(pos).getBlock();

        if (block instanceof PullerPipeBlock) {
            Direction facing = world.getBlockState(pos).get(PullerPipeBlock.FACING);

            return facing == dir.getOpposite();
        }

        BlockEntity be = world.getBlockEntity(pos);

        // TODO find a better way to detect connectable surfaces

        boolean isChest = block instanceof AbstractChestBlock;
        boolean hasInventory = be instanceof Inventory;

        return block instanceof PipeBlock || isChest || hasInventory;
    }
}
