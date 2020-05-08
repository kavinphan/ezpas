package net.kqp.ezpas.block.entity;

import net.kqp.ezpas.block.PipeBlock;
import net.kqp.ezpas.init.Ezpas;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class PullerPipeBlockEntity extends BlockEntity implements Tickable {
    private List<ValidInventory> inventories;

    private int rrCounter;
    public int coolDown;

    public final int speed;
    public final int sizeExtract;

    public PullerPipeBlockEntity(BlockEntityType type, int speed, int sizeExtract) {
        super(type);

        inventories = new ArrayList();

        this.speed = speed;
        this.sizeExtract = sizeExtract;
    }

    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);
        this.rrCounter = tag.getInt("RoundRobinCounter");
        this.coolDown = tag.getInt("ExtractCoolDown");

        int invCount = tag.getInt("InventoryCount");
        int[] invTag = tag.getIntArray("InventoryArray");

        inventories.clear();

        for (int i = 0; i < invCount; i++) {
            int x = invTag[i * 4];
            int y = invTag[i * 4 + 1];
            int z = invTag[i * 4 + 2];
            int direction = invTag[i * 4 + 3];

            inventories.add(new ValidInventory(new BlockPos(x, y, z), Direction.values()[direction]));
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);

        tag.putInt("RoundRobinCounter", rrCounter);
        tag.putInt("ExtractCoolDown", coolDown);

        tag.putInt("InventoryCount", inventories.size());

        int[] invArray = new int[inventories.size() * 4];

        for (int i = 0; i < inventories.size(); i++) {
            ValidInventory inv = inventories.get(i);

            invArray[i * 4] = inv.blockPos.getX();
            invArray[i * 4 + 1] = inv.blockPos.getY();
            invArray[i * 4 + 2] = inv.blockPos.getZ();
            invArray[i * 4 + 3] = inv.direction.ordinal();
        }

        tag.putIntArray("InventoryArray", invArray);

        return tag;
    }

    @Override
    public void tick() {
        if (!this.world.isClient) {
            if (coolDown <= 0) {
                Direction facing = getFacing();
                Inventory from = getInventoryAt(world, this.pos.offset(facing));

                if (from != null && !inventories.isEmpty()) {
                    if (rrCounter >= inventories.size()) {
                        rrCounter = 0;
                    }

                    ValidInventory validInventory = inventories.get(rrCounter);
                    Inventory to = getInventoryAt(world, validInventory.blockPos);

                    if (to != null) {
                        attemptExtract(from, to, facing.getOpposite(), validInventory.direction);

                        rrCounter++;
                        coolDown = speed;
                    } else {
                        // This should never happen, but if it does we'll just retick

                        inventories.remove(rrCounter);
                        tick();
                    }
                }
            } else {
                coolDown = Math.max(0, coolDown - 1);
            }
        }
    }

    public boolean attemptExtract(Inventory from, Inventory to, Direction extractionSide, Direction insertSide) {
        ItemStack pullStack = ItemStack.EMPTY;
        int pullSlot = -1;

        if (from instanceof SidedInventory) {
            // If sided, check the extracting side slots

            SidedInventory inventory = (SidedInventory) from;
            int[] availableSlots = inventory.getAvailableSlots(extractionSide);

            for (int i = 0; i < availableSlots.length; i++) {
                int slot = availableSlots[i];
                ItemStack stack = inventory.getStack(slot);

                if (inventory.canExtract(slot, stack, extractionSide)) {
                    if (stack != ItemStack.EMPTY) {
                        pullStack = stack;
                        pullSlot = slot;

                        break;
                    }
                }
            }
        } else {
            // If just a normal inventory, iterate through slots and pull

            for (int i = 0; i < from.size(); i++) {
                ItemStack stack = from.getStack(i);

                if (stack != ItemStack.EMPTY) {
                    if (to instanceof SidedInventory && !canInsert((SidedInventory) to, stack, insertSide)) {
                        continue;
                    }

                    pullStack = stack;
                    pullSlot = i;

                    break;
                }
            }
        }

        if (pullStack != ItemStack.EMPTY) {
            ItemStack pushStack = ItemStack.EMPTY;
            int pushSlot = -1;

            if (to instanceof SidedInventory) {
                // If sided, check insert side slots

                SidedInventory inventory = (SidedInventory) to;
                int[] availableSlots = inventory.getAvailableSlots(insertSide);

                for (int i = 0; i < availableSlots.length; i++) {
                    int slot = availableSlots[i];
                    ItemStack stack = inventory.getStack(slot);

                    if (inventory.canInsert(slot, pullStack, insertSide)) {
                        pushStack = stack;
                        pushSlot = slot;

                        break;
                    }
                }
            } else {
                for (int i = 0; i < to.size(); i++) {
                    ItemStack stack = to.getStack(i);

                    if (stack == ItemStack.EMPTY
                            || (stack.getItem() == pullStack.getItem() && ItemStack.areTagsEqual(pullStack, stack) && stack.getCount() < stack.getMaxCount())) {
                        pushStack = stack;
                        pushSlot = i;

                        break;
                    }
                }
            }

            if (pushSlot != -1) {
                int extractCount = Math.min(sizeExtract, pullStack.getCount());

                if (pushStack == ItemStack.EMPTY) {
                    to.setStack(pushSlot, from.removeStack(pullSlot, extractCount));
                } else {
                    int newPushCount = extractCount + pushStack.getCount();
                    int max = pullStack.getMaxCount();

                    if (newPushCount <= max) {
                        pullStack.decrement(extractCount);

                        pushStack.setCount(newPushCount);
                    } else {
                        int actualExtract = max - pushStack.getCount();

                        pullStack.decrement(actualExtract);

                        pushStack.setCount(max);
                    }

                    if (pullStack.getCount() == 0) {
                        from.setStack(pullSlot, ItemStack.EMPTY);
                    }
                }

                return true;
            }
        }

        return false;
    }

    public void updateOutputs() {
        inventories.clear();

        Direction facing = getFacing();

        BlockPos immediateBlockPos = this.pos.offset(facing.getOpposite());
        Block immediateBlock = world.getBlockState(immediateBlockPos).getBlock();

        // Init searched set and then add the extracted block position to prevent weird behavior
        Set<BlockPos> searched = new HashSet();
        searched.add(this.pos.offset(facing));

        searchPipeBlock(world, immediateBlockPos, facing.getOpposite(), searched, inventories, immediateBlock instanceof PipeBlock ?  immediateBlock : Ezpas.PIPE);
    }

    private static void searchPipeBlock(World world, BlockPos blockPos, Direction direction, Set<BlockPos> searched, List<ValidInventory> inventories, Block pipeBlock) {
        if (!searched.contains(blockPos)) {
            if (world.getBlockState(blockPos).getBlock().is(pipeBlock)) {
                searched.add(blockPos);

                for (int i = 0; i < Direction.values().length; i++) {
                    Direction searchDirection = Direction.values()[i];
                    searchPipeBlock(world, blockPos.offset(searchDirection), searchDirection.getOpposite(), searched, inventories, pipeBlock);
                }
            } else if (getInventoryAt(world, blockPos) != null) {
                inventories.add(new ValidInventory(blockPos, direction));
            }
        }
    }

    private static Inventory getInventoryAt(World world, BlockPos blockPos) {
        Inventory inventory = null;
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();

        if (block instanceof InventoryProvider) {
            inventory = ((InventoryProvider) block).getInventory(blockState, world, blockPos);
        } else if (block.hasBlockEntity()) {
            BlockEntity blockEntity = world.getBlockEntity(blockPos);

            if (blockEntity instanceof Inventory) {
                inventory = (Inventory) blockEntity;

                if (inventory instanceof ChestBlockEntity && block instanceof ChestBlock) {
                    inventory = ChestBlock.getInventory((ChestBlock) block, blockState, world, blockPos, true);
                }
            }
        }

        return inventory;
    }

    private Direction getFacing() {
        BlockState pullerPipe = this.world.getBlockState(this.pos);
        return pullerPipe.get(FacingBlock.FACING);
    }

    public List<ValidInventory> getValidInventories() {
        return inventories;
    }

    private static boolean canInsert(SidedInventory inv, ItemStack stack, Direction side) {
        int[] availableSlots = inv.getAvailableSlots(side);

        for (int i = 0; i < availableSlots.length; i++) {
            if (inv.canInsert(availableSlots[i], stack, side)) {
                return true;
            }
        }

        return false;
    }

    public static class ValidInventory {
        public final BlockPos blockPos;
        public final Direction direction;

        public ValidInventory(BlockPos blockPos, Direction direction) {
            this.blockPos = blockPos;
            this.direction = direction;
        }

        @Override
        public String toString() {
            return blockPos.toString() + ", into " + direction;
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockPos.getX(), blockPos.getY(), blockPos.getZ(), direction.ordinal());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ValidInventory
                    && blockPos.equals(((ValidInventory) obj).blockPos)
                    &&  direction.equals(((ValidInventory) obj).direction);
        }
    }
}
