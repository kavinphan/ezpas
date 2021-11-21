package com.kqp.ezpas.block;

import com.kqp.ezpas.block.entity.pullerpipe.PullerPipeBlockEntity;
import com.kqp.ezpas.block.pullerpipe.PullerPipeBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.*;

@SuppressWarnings("deprecation")
public class PipeBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.of("north");
    public static final BooleanProperty EAST = BooleanProperty.of("east");
    public static final BooleanProperty SOUTH = BooleanProperty.of("south");
    public static final BooleanProperty WEST = BooleanProperty.of("west");
    public static final BooleanProperty UP = BooleanProperty.of("up");
    public static final BooleanProperty DOWN = BooleanProperty.of("down");

    public static final Map<Direction, BooleanProperty> PROP_MAP = Util.make(new HashMap<>(), map -> {
        map.put(Direction.NORTH, NORTH);
        map.put(Direction.EAST, EAST);
        map.put(Direction.SOUTH, SOUTH);
        map.put(Direction.WEST, WEST);
        map.put(Direction.UP, UP);
        map.put(Direction.DOWN, DOWN);
    });

    private final ShapeUtil shapeUtil;

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

        this.shapeUtil = new ShapeUtil(this);
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
        return shapeUtil.getShape(state);
    }

    protected boolean isConnectable(WorldAccess world, BlockPos pos, Direction dir) {
        Block block = world.getBlockState(pos).getBlock();

        if (block instanceof PullerPipeBlock) {
            Direction facing = world.getBlockState(pos).get(PullerPipeBlock.FACING);

            return facing == dir.getOpposite();
        }

        return block instanceof PipeBlock || PullerPipeBlockEntity.getInventoryAt((World) world, pos) != null;
    }

    /**
     * Thanks Tech Reborn :)
     * Slightly modified for my purposes, but most of it is thanks to the Tech Reborn peeps.
     *
     * <p>
     * Copyright (c) 2020 TechReborn
     * <p>
     * Permission is hereby granted, free of charge, to any person obtaining a copy
     * of this software and associated documentation files (the "Software"), to deal
     * in the Software without restriction, including without limitation the rights
     * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
     * copies of the Software, and to permit persons to whom the Software is
     * furnished to do so, subject to the following conditions:
     * <p>
     * The above copyright notice and this permission notice shall be included in all
     * copies or substantial portions of the Software.
     * <p>
     * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
     * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
     * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
     * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
     * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
     * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
     * SOFTWARE.
     */
    public final class ShapeUtil {

        private final PipeBlock pipeBlock;
        private final HashMap<BlockState, VoxelShape> shapes;

        public ShapeUtil(PipeBlock pipeBlock) {
            this.pipeBlock = pipeBlock;
            this.shapes = createStateShapeMap();
        }

        private HashMap<BlockState, VoxelShape> createStateShapeMap() {
            return Util.make(
                new HashMap<>(),
                map -> pipeBlock.getStateManager().getStates().forEach(state -> map.put(state, getStateShape(state)))
            );
        }

        private VoxelShape getStateShape(BlockState state) {
            final double size = 4;
            final VoxelShape baseShape = Block.createCuboidShape(
                size,
                size,
                size,
                16.0D - size,
                16.0D - size,
                16.0D - size
            );

            final List<VoxelShape> connections = new ArrayList<>();
            for (Direction dir : Direction.values()) {
                if (state.get(PROP_MAP.get(dir))) {
                    double x = dir == Direction.WEST ? 0 : dir == Direction.EAST ? 16D : size;
                    double z = dir == Direction.NORTH ? 0 : dir == Direction.SOUTH ? 16D : size;
                    double y = dir == Direction.DOWN ? 0 : dir == Direction.UP ? 16D : size;

                    VoxelShape shape = VoxelShapes.cuboidUnchecked(x, y, z, 16.0D - size, 16.0D - size, 16.0D - size);
                    connections.add(shape);
                }
            }
            return VoxelShapes.union(baseShape, connections.toArray(new VoxelShape[]{}));
        }

        public VoxelShape getShape(BlockState state) {
            return shapes.get(state);
        }
    }
}
