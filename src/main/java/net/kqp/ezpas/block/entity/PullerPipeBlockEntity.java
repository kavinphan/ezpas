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
import net.minecraft.item.Items;
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
import java.util.stream.IntStream;

public abstract class PullerPipeBlockEntity extends BlockEntity implements Tickable {
    private List<ValidInventory> inventories;

    private int rrCounter;
    public int coolDown;

    public final int speed;
    public final int extractionRate;

    public PullerPipeBlockEntity(BlockEntityType type, int speed, int extractionRate) {
        super(type);

        inventories = new ArrayList();

        this.speed = speed;
        this.extractionRate = extractionRate;
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
                if (attemptExtract()) {
                    coolDown = speed;
                }
            } else {
                coolDown = Math.max(0, coolDown - 1);
            }
        }
    }

    public boolean attemptExtract() {
        Direction facing = getFacing();
        Inventory from = getInventoryAt(world, this.pos.offset(facing));

        if (from != null && !from.isEmpty() && !inventories.isEmpty()) {
            if (rrCounter >= inventories.size()) {
                rrCounter = 0;
            }

            ValidInventory validInventory = inventories.get(rrCounter);
            Inventory to = getInventoryAt(world, validInventory.blockPos);

            if (to != null) {
                extract(from, to, facing.getOpposite(), validInventory.direction);

                rrCounter++;

                return true;
            } else {
                // This should never happen, but if it does we'll just retick

                inventories.remove(rrCounter);

                return attemptExtract();
            }
        }

        return false;
    }

    public void extract(Inventory from, Inventory to, Direction extractionSide, Direction insertSide) {
        // First find a stack to extract
        ItemStack extractionStack = null;
        int extractionSlot = -1;
        int insertionSlot = -1;

        // Iterate through available slots to extract from
        int[] availableExtractionSlots = getAvailableSlots(from, extractionSide);

        for (int i = 0; i < availableExtractionSlots.length; i++) {
            int queryExtractionSlot = availableExtractionSlots[i];
            ItemStack queryStack = from.getStack(queryExtractionSlot);

            if (queryStack != ItemStack.EMPTY && canExtract(from, queryExtractionSlot, queryStack, extractionSide)) {
                // Only continue if stack is not empty
                // Query the receiving inventory to see what slot it can be inserted into
                int queryInsertionSlot = getInsertionSlotForStack(to, queryStack, insertSide);

                if (queryInsertionSlot != -1) {
                    // If the slot is valid (!= -1) then set the appropriate fields, break, and extract and insert

                    extractionStack = queryStack;
                    extractionSlot = queryExtractionSlot;
                    insertionSlot = queryInsertionSlot;

                    break;
                }
            }
        }

        // Continue if all fields are valid
        if (extractionStack != null && extractionSlot != -1 && insertionSlot != -1) {
            ItemStack currentStackInSlot = to.getStack(insertionSlot);

            // The amount to extract is the minimum of the extraction rate and the get of the stack to extract
            int amountToExtract = Math.min(extractionRate, extractionStack.getCount());

            if (currentStackInSlot == ItemStack.EMPTY) {
                // If current stack is empty, just replace it

                to.setStack(insertionSlot, from.removeStack(extractionSlot, amountToExtract));
            } else {
                // Calculate the new amount if the extraction and insertion goes through
                int newInsertionStackCount = currentStackInSlot.getCount() + amountToExtract;
                int maxCount = currentStackInSlot.getMaxCount();

                if (newInsertionStackCount <= currentStackInSlot.getMaxCount()) {
                    // If the new count is below the max, just set current stack to max and decrement from extraction stack

                    currentStackInSlot.setCount(newInsertionStackCount);
                    extractionStack.decrement(amountToExtract);
                } else {
                    // If there is overfill, set current stack to max and then resolve extraction amount

                    currentStackInSlot.setCount(maxCount);

                    int newExtractionStackCount = extractionStack.getCount(); // Get current count
                    newExtractionStackCount -= amountToExtract; // Simulate extraction
                    newExtractionStackCount += newInsertionStackCount - maxCount; // Return overflow

                    extractionStack.setCount(newExtractionStackCount);
                }
            }

            sanitizeSlot(from, extractionSlot);
            sanitizeSlot(to, insertionSlot);
        }
    }

    /**
     * Clears the list of connected inventories and updates it by recursively searching connected pipe blocks.
     */
    public void updateOutputs() {
        inventories.clear();

        Direction facing = getFacing();

        BlockPos immediateBlockPos = this.pos.offset(facing.getOpposite());
        Block immediateBlock = world.getBlockState(immediateBlockPos).getBlock();

        // Init searched set and then add the extracted block position to prevent weird behavior
        Set<BlockPos> searched = new HashSet();
        searched.add(this.pos.offset(facing));

        searchPipeBlock(world, immediateBlockPos, facing.getOpposite(), searched, inventories, immediateBlock instanceof PipeBlock ? immediateBlock : Ezpas.PIPE);
    }

    /**
     * Recursively searches for connected inventories through matching pipe blocks.
     *
     * @param world       World
     * @param blockPos    Current block position to search
     * @param direction   The direction in which the current block pos was searched from
     * @param searched    Set of block positions that have already been searched
     * @param inventories List of inventories that are part of the system
     * @param pipeBlock   Type of pipe block (defaults to normal pipe)
     */
    private static void searchPipeBlock(World world, BlockPos blockPos, Direction direction, Set<BlockPos> searched, List<ValidInventory> inventories, Block pipeBlock) {
        if (!searched.contains(blockPos)) {
            if (world.getBlockState(blockPos).getBlock().is(pipeBlock)) {
                searched.add(blockPos);

                for (int i = 0; i < Direction.values().length; i++) {
                    Direction searchDirection = Direction.values()[i];
                    searchPipeBlock(world, blockPos.offset(searchDirection), searchDirection, searched, inventories, pipeBlock);
                }
            } else if (getInventoryAt(world, blockPos) != null) {
                inventories.add(new ValidInventory(blockPos, direction.getOpposite()));
            }
        }
    }

    private Direction getFacing() {
        BlockState pullerPipe = this.world.getBlockState(this.pos);
        return pullerPipe.get(FacingBlock.FACING);
    }

    public List<ValidInventory> getValidInventories() {
        return inventories;
    }

    private static void sanitizeSlot(Inventory inv, int slot) {
        ItemStack stackInSlot = inv.getStack(slot);

        if (stackInSlot.getItem() == Items.AIR || stackInSlot.getCount() == 0) {
            inv.setStack(slot, ItemStack.EMPTY);
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

    /**
     * Returns whether or not the slot and item stack can be extracted from the inventory from a given side.
     *
     * @param inv The inventory
     * @param slot The slot
     * @param itemStack The item stack
     * @param side The side
     * @return True if it can be extracted
     */
    private static boolean canExtract(Inventory inv, int slot, ItemStack itemStack, Direction side) {
        if (inv instanceof SidedInventory) {
            return ((SidedInventory) inv).canExtract(slot, itemStack, side);
        } else {
            return true;
        }
    }

    /**
     * Attempts to find a valid slot for a given item stack to be inserted into the given inventory and side.
     *
     * @param inv   Inventory to insert into
     * @param stack Item Stack to insert into inventory
     * @param side  Side to insert into
     * @return -1 if no slot could be found
     */
    private static int getInsertionSlotForStack(Inventory inv, ItemStack stack, Direction side) {
        int[] availableSlots = getAvailableSlots(inv, side);

        for (int i = 0; i < availableSlots.length; i++) {
            int slot = availableSlots[i];

            if (inv instanceof SidedInventory) {
                if (((SidedInventory) inv).canInsert(slot, stack, side)) {
                    return slot;
                }
            } else {
                ItemStack queryStack = inv.getStack(slot);

                if (queryStack == ItemStack.EMPTY
                        || (queryStack.getCount() < queryStack.getMaxCount() && ItemStack.areItemsEqual(queryStack, stack) && ItemStack.areTagsEqual(queryStack, stack))) {
                    return slot;
                }
            }
        }

        return -1;
    }

    private static int[] getAvailableSlots(Inventory inventory, Direction side) {
        return inventory instanceof SidedInventory ? ((SidedInventory) inventory).getAvailableSlots(side) : IntStream.range(0, inventory.size()).toArray();
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
                    && direction.equals(((ValidInventory) obj).direction);
        }
    }
}
