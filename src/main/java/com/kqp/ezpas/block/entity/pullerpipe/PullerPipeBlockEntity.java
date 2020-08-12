package com.kqp.ezpas.block.entity.pullerpipe;

import com.kqp.ezpas.block.ColoredPipeBlock;
import com.kqp.ezpas.block.FilteredPipeBlock;
import com.kqp.ezpas.block.PipeBlock;
import com.kqp.ezpas.block.RigidPipeBlock;
import com.kqp.ezpas.block.entity.FilteredPipeBlockEntity;
import com.kqp.ezpas.block.pullerpipe.PullerPipeBlock;
import com.kqp.ezpas.pipe.InsertionPoint;
import com.kqp.ezpas.pipe.filter.Filter;
import com.kqp.ezpas.init.Ezpas;
import net.minecraft.block.*;
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
import net.minecraft.world.WorldAccess;

import java.util.*;
import java.util.stream.IntStream;

public abstract class PullerPipeBlockEntity extends BlockEntity implements Tickable {
    private List<PrioritizedList<InsertionPoint>> prioritizedInsertionPoints;

    private boolean shouldRecalculate = true;

    private int rrCounter;
    private int currentPriority;
    public int coolDown;

    public final int speed;
    public final int extractionRate;
    public final int subTickRate;

    public PullerPipeBlockEntity(BlockEntityType type, int speed, int extractionRate, int subTickRate) {
        super(type);

        prioritizedInsertionPoints = new ArrayList();

        this.speed = speed;
        this.extractionRate = extractionRate;
        this.subTickRate = subTickRate;
    }

    @Override
    public void fromTag(BlockState bs, CompoundTag tag) {
        super.fromTag(bs, tag);
        this.rrCounter = tag.getInt("RoundRobinCounter");
        this.coolDown = tag.getInt("ExtractCoolDown");
        this.currentPriority = tag.getInt("CurrentPriority");
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);

        tag.putInt("RoundRobinCounter", rrCounter);
        tag.putInt("ExtractCoolDown", coolDown);
        tag.putInt("CurrentPriority", currentPriority);

        return tag;
    }

    @Override
    public void tick() {
        if (!this.world.isClient) {
            if (shouldRecalculate) {
                this.calculateInsertionPoints();

                shouldRecalculate = false;
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

    /**
     * This is the main extraction method.
     * It starts by checking all the previous prioritized lists
     * to see if any of them have opened up. If none are available,
     * it goes to the current priority. If it can't successfully
     * make an extraction, it exits the method and bumps the priority.
     */
    public void doExtraction() {
        if (prioritizedInsertionPoints.isEmpty()) {
            return;
        }

        // If current priority is broken, increment to revalidate
        if (getPriorityList(currentPriority) == null) {
            incrementCurrentPriority();
        }

        // If round robin counter is out of bounds for current priority, revalidate
        if (rrCounter >= getPriorityList(currentPriority).size()) {
            rrCounter = 0;
        }

        Direction facing = getFacing();
        Inventory from = getInventoryAt(world, this.pos.offset(facing));

        // Check that the extracting inventory still exists
        if (from != null && !from.isEmpty() && !prioritizedInsertionPoints.isEmpty()) {
            // Before going to current stuff, check previous inventories
            for (PrioritizedList<InsertionPoint> prioritizedList : prioritizedInsertionPoints) {
                if (prioritizedList.priority == currentPriority) {
                    break;
                }

                for (int i = 0; i < prioritizedList.size(); i++) {
                    InsertionPoint inventory = prioritizedList.get(i);
                    boolean extracted = extract(from, getInventoryAt(world, inventory.blockPos), facing.getOpposite(), inventory.direction, inventory.filters);

                    // If we're able to extract from a non-current priority inventory,
                    // Reset our current priority and counter
                    if (extracted) {
                        currentPriority = prioritizedList.priority;
                        rrCounter = i;
                        incrementRrCounter();

                        return;
                    }
                }
            }

            PrioritizedList<InsertionPoint> insertionPoints = getPriorityList(currentPriority);
            int attempts = 0;
            boolean extracted = false;

            while (!extracted && attempts < insertionPoints.size()) {
                InsertionPoint insertionPoint = insertionPoints.get(rrCounter);
                extracted = extract(from, getInventoryAt(world, insertionPoint.blockPos), facing.getOpposite(), insertionPoint.direction, insertionPoint.filters);
                incrementRrCounter();
                attempts++;
            }

            if (!extracted) {
                incrementCurrentPriority();
            }
        }
    }

    private void incrementRrCounter() {
        rrCounter++;
        if (rrCounter >= getPriorityList(currentPriority).size()) {
            rrCounter = 0;
        }
    }

    private void incrementCurrentPriority() {
        int minPriority = currentPriority;

        // Iterate through list to find a list with a higher priority
        // If we find one, set that as the current priority and exit
        // Also find the minimum priority during this
        for (int i = 0; i < prioritizedInsertionPoints.size(); i++) {
            PrioritizedList<InsertionPoint> prioritizedList = prioritizedInsertionPoints.get(i);

            if (prioritizedList.priority > currentPriority) {
                currentPriority = prioritizedList.priority;
                return;
            }

            if (prioritizedList.priority < minPriority) {
                minPriority = prioritizedList.priority;
            }
        }

        // If we aren't able to find a higher priority, set it to the minimum
        currentPriority = minPriority;
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
    private void calculateInsertionPoints() {
        Direction facing = getFacing();

        BlockPos immediateBlockPos = this.pos.offset(facing.getOpposite());

        // Create initial path to prevent back-tracking
        Path initPath = new Path();
        initPath.visited.add(this.pos);

        // Create list of inventories
        List<InsertionPoint> newInventories = new ArrayList();

        // Create path map
        Map<BlockPos, List<Path>> pathMap = new HashMap();

        // Build inventory graph
        calculateInsertionPoints(newInventories, immediateBlockPos, facing.getOpposite(), pathMap, initPath);

        // Sort by priority
        newInventories.sort(Comparator.comparing(InsertionPoint::getPriority));

        // Group inventories by priority
        prioritizedInsertionPoints.clear();
        for (InsertionPoint inventory : newInventories) {
            PrioritizedList<InsertionPoint> listForInventory = getPriorityList(inventory.priority);

            if (listForInventory == null) {
                listForInventory = new PrioritizedList(inventory.priority);
                prioritizedInsertionPoints.add(listForInventory);
            }

            listForInventory.add(inventory);
        }
    }

    public PrioritizedList<InsertionPoint> getPriorityList(int priority) {
        for (PrioritizedList<InsertionPoint> prioritizedList : prioritizedInsertionPoints) {
            if (prioritizedList.priority == priority) {
                return prioritizedList;
            }
        }

        return null;
    }

    /**
     * Recursively build list of inventories.
     */
    private void calculateInsertionPoints(List<InsertionPoint> inventoryList, BlockPos blockPos, Direction direction, Map<BlockPos, List<Path>> pathMap, Path path) {
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
                        FilteredPipeBlockEntity filteredPipeBlockEntity = (FilteredPipeBlockEntity) be;

                        if (filteredPipeBlockEntity.persist) {
                            newPath.filters.add(new Filter(filteredPipeBlockEntity));
                        }
                    }
                } else if (queryBlock == Ezpas.DENSE_PIPE) {
                    newPath.priority++;
                }

                List<Path> pathList = pathMap.computeIfAbsent(blockPos, x -> new ArrayList());

                for (Path queryPath : pathList) {
                    if (queryPath.filters.equals(newPath.filters) || queryPath.priority <= newPath.priority) {
                        return;
                    }
                }

                pathList.add(newPath);

                for (int i = 0; i < Direction.values().length; i++) {
                    Direction searchDirection = Direction.values()[i];

                    calculateInsertionPoints(inventoryList, blockPos.offset(searchDirection), searchDirection, pathMap, newPath);
                }
            } else if (!(prevBlock instanceof RigidPipeBlock) && getInventoryAt(world, blockPos) != null) {
                // Add non-persistent filtered pipes
                if (prevBlock instanceof FilteredPipeBlock) {
                    BlockEntity be = world.getBlockEntity(blockPos);

                    if (be instanceof FilteredPipeBlockEntity) {
                        FilteredPipeBlockEntity filteredPipeBlockEntity = (FilteredPipeBlockEntity) be;

                        if (!filteredPipeBlockEntity.persist) {
                            path.filters.add(new Filter(filteredPipeBlockEntity));
                        }
                    }
                }

                InsertionPoint newInventory = new InsertionPoint(blockPos, direction.getOpposite(), path.filters, path.priority, path.visited.size());

                // Check if there's a similar path already
                int index = prioritizedInsertionPoints.indexOf(newInventory);

                if (index != -1) {
                    InsertionPoint similarInventory = inventoryList.get(index);

                    // If the new inventory is a shorter path, replace the old one
                    if (newInventory.distance < similarInventory.distance) {
                        inventoryList.set(index, newInventory);
                    }
                } else {
                    inventoryList.add(newInventory);
                }
            }
        }
    }

    private Direction getFacing() {
        BlockState pullerPipe = this.world.getBlockState(this.pos);
        return pullerPipe.get(FacingBlock.FACING);
    }

    public List<PrioritizedList<InsertionPoint>> getValidInventories() {
        return prioritizedInsertionPoints;
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
     * @param world    World
     * @param blockPos BlockPos to search
     * @param searched Set of block positions already searched
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
                    ((PullerPipeBlockEntity) be).markToRecalculate();
                }
            }
        }
    }

    public void markToRecalculate() {
        this.shouldRecalculate = true;
    }

    public static class PrioritizedList<E> extends ArrayList<E> {
        public final int priority;

        public PrioritizedList(int priority) {
            this.priority = priority;
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
