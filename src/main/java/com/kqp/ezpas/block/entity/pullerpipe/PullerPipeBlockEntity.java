package com.kqp.ezpas.block.entity.pullerpipe;

import com.kqp.ezpas.block.ColoredPipeBlock;
import com.kqp.ezpas.block.FilteredPipeBlock;
import com.kqp.ezpas.block.PipeBlock;
import com.kqp.ezpas.block.entity.FilteredPipeBlockEntity;
import com.kqp.ezpas.block.pullerpipe.PullerPipeBlock;
import com.kqp.ezpas.filter.Filter;
import com.kqp.ezpas.init.Ezpas;
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
import net.minecraft.world.WorldAccess;
import net.minecraft.world.World;

import java.util.*;
import java.util.stream.IntStream;

public abstract class PullerPipeBlockEntity extends BlockEntity implements Tickable {
    private List<ValidInventory> inventories;

    private int rrCounter;
    public int coolDown;

    public final int speed;
    public final int extractionRate;
    public final int subTickRate;

    public boolean loaded = false;

    private boolean loopedinCurrentPriority = false;

    public PullerPipeBlockEntity(BlockEntityType type, int speed, int extractionRate, int subTickRate) {
        super(type);

        inventories = new ArrayList();

        this.speed = speed;
        this.extractionRate = extractionRate;
        this.subTickRate = subTickRate;
    }

    @Override
    public void fromTag(BlockState bs, CompoundTag tag) {
        super.fromTag(bs, tag);
        this.rrCounter = tag.getInt("RoundRobinCounter");
        this.coolDown = tag.getInt("ExtractCoolDown");
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);

        tag.putInt("RoundRobinCounter", rrCounter);
        tag.putInt("ExtractCoolDown", coolDown);

        return tag;
    }

    @Override
    public void tick() {
        if (!this.world.isClient) {
            if (!loaded) {
                this.updatePullerPipes();

                loaded = true;
            }

            if (!this.world.isReceivingRedstonePower(pos)) {
                if (coolDown <= 0) {
                    for (int i = 0; i < subTickRate; i++) {
                        doExtraction();
                    }

                    coolDown = speed;
                } else {
                    coolDown = Math.max(0, coolDown - 1);
                }
            } else {
                coolDown = speed;
            }
        }
    }

    public void doExtraction() {
        Direction facing = getFacing();
        Inventory from = getInventoryAt(world, this.pos.offset(facing));

        // Check that the extracting inventory still exists
        if (from != null && !from.isEmpty() && !inventories.isEmpty()) {
            int currentPriority = inventories.get(rrCounter).priority;

            // Before trying the current counter, check if previous inventories can be inserted to
            for (int i = 0; i < rrCounter; i++) {
                ValidInventory validInventory = inventories.get(i);

                // Break if reached our current priority
                if (validInventory.priority == currentPriority) {
                    break;
                }

                // If we can extract to it, set the round robin counter
                boolean extracted = extract(from, getInventoryAt(world, validInventory.blockPos), facing.getOpposite(), validInventory.direction, validInventory.filters);
                if (extracted) {
                    rrCounter = i;
                    incrementRrCounter();

                    return;
                }
            }

            ValidInventory validInventory = inventories.get(rrCounter);
            boolean extracted = extract(from, getInventoryAt(world, validInventory.blockPos), facing.getOpposite(), validInventory.direction, validInventory.filters);

            if (extracted) {
                incrementRrCounter();
            }

            int nextPriority = inventories.get(rrCounter).priority;

            if (currentPriority != nextPriority) {

            }
        }
    }

    private void incrementRrCounter() {
        rrCounter++;
        if (rrCounter >= inventories.size()) {
            rrCounter = 0;
        }
    }

    /**
     * Returns true if successful.
     *
     * @param from
     * @param to
     * @param extractionSide
     * @param insertSide
     * @param filters
     * @return
     */
    public boolean extract(Inventory from, Inventory to, Direction extractionSide, Direction insertSide, List<Filter> filters) {
        // First find a stack to extract
        ItemStack extractionStack = null;
        int extractionSlot = -1;
        int insertionSlot = -1;

        // Iterate through available slots to extract from
        int[] availableExtractionSlots = getAvailableSlots(from, extractionSide);

        for (int i = 0; i < availableExtractionSlots.length; i++) {
            int queryExtractionSlot = availableExtractionSlots[i];
            ItemStack queryStack = from.getStack(queryExtractionSlot);

            if (queryStack != ItemStack.EMPTY
                    && stackPasses(queryStack, filters)
                    && canExtract(from, queryExtractionSlot, queryStack, extractionSide)) {
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

            if (currentStackInSlot == ItemStack.EMPTY || currentStackInSlot.getCount() == 0) {
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

            return true;
        }

        return false;
    }

    /**
     * Clears the list of connected inventories and updates it by recursively searching connected pipe blocks.
     */
    public void updatePullerPipes() {
        inventories.clear();

        Direction facing = getFacing();

        BlockPos immediateBlockPos = this.pos.offset(facing.getOpposite());

        // Create initial path to prevent back-tracking
        Path initPath = new Path();
        initPath.visited.add(this.pos);

        buildInventoryGraph(immediateBlockPos, facing.getOpposite(), initPath);

        inventories.sort(Comparator.comparing(ValidInventory::getPriority));
    }

    /**
     * Recursively build list of inventories.
     */
    private void buildInventoryGraph(BlockPos blockPos, Direction direction, Path path) {
        if (!path.visited.contains(blockPos)) {
            Block queryBlock = world.getBlockState(blockPos).getBlock();
            Block prevBlock = world.getBlockState(blockPos.offset(direction.getOpposite())).getBlock();

            boolean validPipeBranch = queryBlock instanceof PipeBlock;
            if (prevBlock instanceof ColoredPipeBlock && queryBlock instanceof ColoredPipeBlock) {
                validPipeBranch = (prevBlock == queryBlock);
            }

            if (validPipeBranch) {
                Path newPath = Path.from(path);
                newPath.visited.add(blockPos);

                if (queryBlock instanceof FilteredPipeBlock) {
                    BlockEntity be = world.getBlockEntity(blockPos);

                    if (be instanceof FilteredPipeBlockEntity) {
                        newPath.filters.add(new Filter((FilteredPipeBlockEntity) be));
                    }
                } else if (queryBlock == Ezpas.DENSE_PIPE) {
                    newPath.priority++;
                }

                for (int i = 0; i < Direction.values().length; i++) {
                    Direction searchDirection = Direction.values()[i];

                    buildInventoryGraph(blockPos.offset(searchDirection), searchDirection, newPath);
                }
            } else if (getInventoryAt(world, blockPos) != null) {
                ValidInventory newInventory = new ValidInventory(blockPos, direction.getOpposite(), path.filters, path.priority, path.visited.size());

                // Check if there's a similar path already
                int index = inventories.indexOf(newInventory);

                if (index != -1) {
                    ValidInventory similarInventory = inventories.get(index);

                    // If the new inventory is a shorter path, replace the old one
                    if (newInventory.distance < similarInventory.distance) {
                        inventories.set(index, newInventory);
                    }
                } else {
                    inventories.add(newInventory);
                }
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

    public static Inventory getInventoryAt(World world, BlockPos blockPos) {
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
     * @param inv       The inventory
     * @param slot      The slot
     * @param itemStack The item stack
     * @param side      The side
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

        for (int slot : availableSlots) {
            if (inv.isValid(slot, stack)) {
                if (inv instanceof SidedInventory) {
                    if (((SidedInventory) inv).canInsert(slot, stack, side)) {
                        ItemStack queryStack = inv.getStack(slot);

                        // canInsert doesn't check for item parity
                        if (queryStack == ItemStack.EMPTY || queryStack.getCount() == 0
                                || (queryStack.getCount() < queryStack.getMaxCount() && ItemStack.areItemsEqual(queryStack, stack) && ItemStack.areTagsEqual(queryStack, stack))) {
                            return slot;
                        }
                    }
                } else {
                    ItemStack queryStack = inv.getStack(slot);
                    if (queryStack == ItemStack.EMPTY || queryStack.getCount() == 0
                            || (queryStack.getCount() < queryStack.getMaxCount() && ItemStack.areItemsEqual(queryStack, stack) && ItemStack.areTagsEqual(queryStack, stack))) {
                        return slot;
                    }
                }
            }
        }

        return -1;
    }

    private static int[] getAvailableSlots(Inventory inventory, Direction side) {
        return inventory instanceof SidedInventory ? ((SidedInventory) inventory).getAvailableSlots(side) : IntStream.range(0, inventory.size()).toArray();
    }

    /**
     * Returns true if an itemstack is able to pass through the list of filters.
     *
     * @param itemStack
     * @param filters
     * @return
     */
    private static boolean stackPasses(ItemStack itemStack, List<Filter> filters) {
        for (Filter filter : filters) {
            if (!filter.stackPasses(itemStack)) {
                return false;
            }
        }

        return true;
    }

    /**
     * This method is called when a pipe is placed/updated.
     * Traverses the pipe graph and updates any puller pipes found.
     *
     * @param world     World
     * @param blockPos  BlockPos to search
     * @param searched  Set of block positions already searched
     */
    public static void updatePullerPipes(WorldAccess world, BlockPos blockPos, Direction direction, Set<BlockPos> searched) {
        if (!searched.contains(blockPos)) {
            searched.add(blockPos);

            Block queryBlock = world.getBlockState(blockPos).getBlock();
            Block prevBlock = world.getBlockState(blockPos.offset(direction.getOpposite())).getBlock();

            boolean validPipeBranch = queryBlock instanceof PipeBlock;

            if (prevBlock instanceof ColoredPipeBlock && queryBlock instanceof ColoredPipeBlock) {
                validPipeBranch = (prevBlock == queryBlock);
            }

            if (validPipeBranch) {
                for (int i = 0; i < Direction.values().length; i++) {
                    Direction searchDirection = Direction.values()[i];

                    updatePullerPipes(world, blockPos.offset(searchDirection), searchDirection, searched);
                }
            } else if (queryBlock instanceof PullerPipeBlock) {
                // If we run into a puller block, RESET THE SYSTEM

                BlockEntity be = world.getBlockEntity(blockPos);

                if (be instanceof PullerPipeBlockEntity) {
                    ((PullerPipeBlockEntity) be).updatePullerPipes();
                }
            }
        }
    }

    public static class ValidInventory {
        public final BlockPos blockPos;
        public final Direction direction;
        public final List<Filter> filters;
        public final int priority;
        public final int distance;

        public ValidInventory(BlockPos blockPos, Direction direction, List<Filter> filters, int priority, int distance) {
            this.blockPos = blockPos;
            this.direction = direction;
            this.filters = filters;
            this.priority = priority;
            this.distance = distance;
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockPos.getX(), blockPos.getY(), blockPos.getZ(), direction.ordinal(), filters, priority);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ValidInventory
                    && blockPos.equals(((ValidInventory) obj).blockPos)
                    && direction.equals(((ValidInventory) obj).direction)
                    && filters.equals(((ValidInventory) obj).filters)
                    && priority == ((ValidInventory) obj).priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    public static class Path {
        public final Set<BlockPos> visited;
        public final List<Filter> filters;
        public int priority = 0;

        public Path() {
            this.visited = new HashSet();
            this.filters = new ArrayList();
        }

        public static Path from(Path oldPath) {
            Path newPath = new Path();
            newPath.visited.addAll(oldPath.visited);
            newPath.filters.addAll(oldPath.filters);
            newPath.priority = oldPath.priority;

            return newPath;
        }
    }
}
