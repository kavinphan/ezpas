package com.kqp.ezpas.block.entity.pullerpipe;

import com.kqp.ezpas.Ezpas;
import com.kqp.ezpas.block.ColoredPipeBlock;
import com.kqp.ezpas.block.FilteredPipeBlock;
import com.kqp.ezpas.block.PipeBlock;
import com.kqp.ezpas.block.RigidPipeBlock;
import com.kqp.ezpas.block.entity.FilteredPipeBlockEntity;
import com.kqp.ezpas.block.pullerpipe.PullerPipeBlock;
import com.kqp.ezpas.pipe.InsertionPoint;
import com.kqp.ezpas.pipe.Path;
import com.kqp.ezpas.pipe.filter.Filter;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public abstract class PullerPipeBlockEntity extends BlockEntity {
    private final List<List<InsertionPoint>> prioritizedInsertionPoints = new ArrayList<>();

    private boolean shouldRecalculate = true;

    private int rrCounter;
    private int currentPriority;
    public int coolDown;

    public final int speed;
    public final int extractionSize;
    public final int subTickRate;

    private Storage<ItemVariant> cachedExtractionStorage = null;

    public PullerPipeBlockEntity(BlockEntityType type, BlockPos pos, BlockState state, int speed, int extractionSize,
                                 int subTickRate) {
        super(type, pos, state);

        this.speed = speed;
        this.extractionSize = extractionSize;
        this.subTickRate = subTickRate;
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        this.rrCounter = tag.getInt("RoundRobinCounter");
        this.coolDown = tag.getInt("ExtractCoolDown");
        this.currentPriority = tag.getInt("CurrentPriority");
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        super.writeNbt(tag);

        tag.putInt("RoundRobinCounter", rrCounter);
        tag.putInt("ExtractCoolDown", coolDown);
        tag.putInt("CurrentPriority", currentPriority);

        return tag;
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, PullerPipeBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    private void serverTick() {
        // Sync insertion point storages.
        prioritizedInsertionPoints.stream().flatMap(List::stream).forEach(InsertionPoint::syncStorage);

        // Check for any invalid insertion points.
        // If any appear, mark for recalculation.
        boolean anyIPsInvalid = prioritizedInsertionPoints.stream()
            .flatMap(List::stream)
            .anyMatch(InsertionPoint::invalid);
        if (anyIPsInvalid) {
            shouldRecalculate = true;
        }

        // Re-calculate insertion points if marked
        if (shouldRecalculate) {
            calculateInsertionPoints();
            shouldRecalculate = false;
        }

        // Do nothing if there are no insertion points.
        if (prioritizedInsertionPoints.isEmpty()) {
            return;
        }

        // Mark to recalculate and return if the source storage is missing.
         cachedExtractionStorage = ItemStorage.SIDED.find(world,
            getExtractionBlockPos(),
            getExtractionDirection()
        );
        if (cachedExtractionStorage == null || !cachedExtractionStorage.supportsExtraction()) {
            markToRecalculate();
            return;
        }

        // Do not extract if powered.
        // Also reset cool down.
        if (world.isReceivingRedstonePower(pos)) {
            coolDown = speed;
            return;
        }

        // If on cool down, decrement and return.
        if (coolDown > 0) {
            coolDown = Math.max(0, coolDown - 1);
            return;
        }

        performExtractions();
    }

    /**
     * Rebuild list of insertion points.
     */
    private void calculateInsertionPoints() {
        // Create initial path to prevent back-tracking.
        Path rootPath = new Path();
        rootPath.addVisited(this.pos);

        // Create list of insertion points.
        List<InsertionPoint> newIPs = new ArrayList<InsertionPoint>();

        // Create path map.
        Map<BlockPos, List<Path>> pathMap = new HashMap<BlockPos, List<Path>>();

        // Build inventory graph.
        calculateInsertionPoints(newIPs, getInsertionBlockPos(), getInsertionDirection(), pathMap, rootPath);

        // Group insertion points by priority.
        prioritizedInsertionPoints.clear();
        for (InsertionPoint ip : newIPs) {
            getListForPriority(ip.priority).add(ip);
        }

        // Remove gaps in priorities (ie [0, 5, 7] -> [0, 1, 2])
        deGapPriorities(newIPs);

        // Add insertion points to the prioritized list of lists of insertion points.
        for (InsertionPoint ip : newIPs) {
            getListForPriority(ip.priority).add(ip);
        }
    }

    /**
     * Build list of all insertion points accessible from the given block.
     *
     * @param ipList   List of insertion points.
     * @param blockPos Current block position to inspect.
     * @param inDir    Inbound direction, which is the opposite of the receiving side.
     * @param pathMap  Map of block positions to their paths.
     * @param prevPath Current path.
     */
    private void calculateInsertionPoints(List<InsertionPoint> ipList, BlockPos blockPos, Direction inDir,
                                          Map<BlockPos, List<Path>> pathMap, Path prevPath) {
        // Ignore visited blocks.
        if (prevPath.hasVisited(blockPos)) {
            return;
        }

        Block currBlock = world.getBlockState(blockPos).getBlock();
        BlockPos prevBlockPos = blockPos.offset(inDir.getOpposite());
        Block prevBlock = world.getBlockState(prevBlockPos).getBlock();

        // First case is if the current block can propagate items.
        // If so, it should inspect surrounding blocks for insertion points.
        if (canPropagate(currBlock, prevBlock)) {
            // Create new path for the current block.
            Path currPath = prevPath.branch();
            currPath.addVisited(blockPos);

            // If the current block is a filtered pipe, apply its filters to
            // the current path ONLY if it's enabled and persisted.
            if (currBlock instanceof FilteredPipeBlock) {
                FilteredPipeBlockEntity filterPipeBE = (FilteredPipeBlockEntity) world.getBlockEntity(blockPos);

                // Check to see if redstone should disable the filter pipe.
                boolean disabledWhenPowered = filterPipeBE.flags[FilteredPipeBlockEntity.REDSTONE_DISABLE_FLAG];
                boolean enabled = !disabledWhenPowered || world.isReceivingRedstonePower(blockPos);

                // Check to see if the filters should persist down the path.
                boolean persist = filterPipeBE.flags[FilteredPipeBlockEntity.PERSIST_FLAG];

                // Only add filters to path if enabled and the persist flag is enabled.
                if (enabled && persist) {
                    currPath.addFilter(new Filter(filterPipeBE));
                }
            }

            // If the current block is a dense pipe, increment the new path's priority.
            // This ensures that down stream insertion points are prioritized less.
            if (currBlock == Ezpas.DENSE_PIPE) {
                currPath.priority++;
            }

            // Check to see if this block position has already been visited by another path.
            // We won't search further if there exists a path for this block with 1) the same
            // filters or 2) a higher priority (0 = highest, infinity = lowest)
            List<Path> pathList = pathMap.computeIfAbsent(blockPos, x -> new ArrayList<>());
            for (Path existingPath : pathList) {
                if (existingPath.getFilters()
                    .equals(currPath.getFilters()) || existingPath.priority <= currPath.priority) {
                    return;
                }
            }

            // Add the new path to the path map
            pathList.add(currPath);

            // Recursive call in all directions.
            for (Direction searchDirection : Direction.values()) {
                calculateInsertionPoints(ipList, blockPos.offset(searchDirection), searchDirection, pathMap, currPath);
            }
        }

        // Second case is if the current block is insertable.
        // We also check to see if the previous block was a rigid pipe,
        // which does not propagate items to insertion points.
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, blockPos, inDir.getOpposite());
        if (storage != null && storage.supportsInsertion() && !(prevBlock instanceof RigidPipeBlock)) {
            // Create new path for the current block.
            Path currPath = prevPath.branch();

            // TODO possibly remove this
            currPath.addVisited(blockPos);

            // Add non-persistent filtered pipes
            if (prevBlock instanceof FilteredPipeBlock) {
                BlockEntity be = world.getBlockEntity(prevBlockPos);

                if (be instanceof FilteredPipeBlockEntity) {
                    FilteredPipeBlockEntity filterPipeBE = (FilteredPipeBlockEntity) world.getBlockEntity(blockPos);

                    // Check to see if redstone should disable the filter pipe.
                    boolean disabledWhenPowered = filterPipeBE.flags[FilteredPipeBlockEntity.REDSTONE_DISABLE_FLAG];
                    boolean enabled = !disabledWhenPowered || world.isReceivingRedstonePower(blockPos);

                    // Only add filters to path if enabled.
                    if (enabled) {
                        currPath.addFilter(new Filter(filterPipeBE));
                    }
                }
            }

            InsertionPoint newIP = new InsertionPoint(world,
                blockPos,
                inDir.getOpposite(),
                currPath.getFilters(),
                currPath.priority,
                currPath.getVisitedCount(),
                storage
            );

            // If an insertion points exists with the same parameters except
            // it has a longer path, replace it.
            // TODO this exploits the equals method :(
            int index = ipList.indexOf(newIP);
            if (index != -1) {
                InsertionPoint similarInventory = ipList.get(index);

                // If the new inventory is a shorter path, replace the old one
                if (newIP.distance < similarInventory.distance) {
                    ipList.set(index, newIP);
                }
            } else {
                // If nothing similar exists, add it.
                ipList.add(newIP);
            }
        }
    }

    /**
     * Performs all extractions that this puller block should do.
     */
    public void performExtractions() {
        // Set current priority to highest (0).
        currentPriority = 0;

        // Perform extractions
        for (int i = 0; i < subTickRate; i++) {
            validateRRCounter();
            performExtraction();
        }

        coolDown = speed;
    }

    /**
     * Performs one extraction.
     */
    public void performExtraction() {
        List<InsertionPoint> ips = prioritizedInsertionPoints.stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());

        Transaction trx = Transaction.openOuter();

        main:
        for (StorageView<ItemVariant> iv : cachedExtractionStorage.iterable(trx)) {
            ItemVariant resource = iv.getResource();

            if (resource.isBlank()) {
                continue;
            }

            // Attempt extraction from source storage.
            long extracted = iv.extract(resource, extractionSize, trx);
            if (extracted == 0) {
                continue;
            }

            for (InsertionPoint ip : ips) {
                List<SingleSlotStorage<ItemVariant>> slots = StreamSupport.stream(ip.storage.iterable(trx)
                        .spliterator(), false)
                    .filter(sv -> sv instanceof SingleSlotStorage<ItemVariant>)
                    .map(sv -> (SingleSlotStorage<ItemVariant>) sv)
                    .collect(Collectors.toList());

                // Attempt insertion into target insertion point.
                long inserted = StorageUtil.insertStacking(slots, resource, extracted, trx);
                if (extracted == inserted && extracted > 0) {
                    break main;
                }
            }
        }

        trx.commit();
    }

    /**
     * Validates the round robin counter so that it doesn't extract from an invalid index.
     */
    private void validateRRCounter() {
        if (rrCounter >= getListForPriority(currentPriority).size()) {
            rrCounter = 0;
        }
    }

    private void incrementRrCounter() {
        rrCounter++;
        if (rrCounter >= getListForPriority(currentPriority).size()) {
            rrCounter = 0;
        }
    }

    /**
     * Sets the current priority to the next lowest priority (0 -> 1)
     */
    private void gotoNextPriority() {
        currentPriority++;
        if (currentPriority >= prioritizedInsertionPoints.size()) {
            currentPriority = 0;
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
    public boolean extract(Inventory from, Inventory to, Direction extractionSide, Direction insertSide,
                           List<Filter> filters) {
        // First find a stack to extract
        ItemStack extractionStack = null;
        int extractionSlot = -1;
        int insertionSlot = -1;

        // Iterate through available slots to extract from
        int[] availableExtractionSlots = getAvailableSlots(from, extractionSide);

        for (int i = 0; i < availableExtractionSlots.length; i++) {
            int queryExtractionSlot = availableExtractionSlots[i];
            ItemStack queryStack = from.getStack(queryExtractionSlot);

            if (queryStack != ItemStack.EMPTY && stackPasses(queryStack, filters, to) && canExtract(from,
                queryExtractionSlot,
                queryStack,
                extractionSide
            )) {
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
            int amountToExtract = Math.min(extractionSize, extractionStack.getCount());

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

    public List<InsertionPoint> getListForPriority(int priority) {
        while (priority >= prioritizedInsertionPoints.size()) {
            prioritizedInsertionPoints.add(new ArrayList<InsertionPoint>());
        }

        return prioritizedInsertionPoints.get(priority);
    }

    private BlockPos getExtractionBlockPos() {
        return this.pos.offset(getExtractionDirection());
    }

    private BlockPos getInsertionBlockPos() {
        return this.pos.offset(getInsertionDirection());
    }

    private Direction getExtractionDirection() {
        return this.world.getBlockState(this.pos).get(FacingBlock.FACING);
    }

    private Direction getInsertionDirection() {
        return this.world.getBlockState(this.pos).get(FacingBlock.FACING).getOpposite();
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
        } else if (block instanceof BlockWithEntity) {
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
                        if (queryStack == ItemStack.EMPTY || queryStack.getCount() == 0 || (queryStack.getCount() < queryStack.getMaxCount() && ItemStack.areItemsEqual(queryStack,
                            stack
                        ) && ItemStack.areNbtEqual(queryStack, stack))) {
                            return slot;
                        }
                    }
                } else {
                    ItemStack queryStack = inv.getStack(slot);
                    if (queryStack == ItemStack.EMPTY || queryStack.getCount() == 0 || (queryStack.getCount() < queryStack.getMaxCount() && ItemStack.areItemsEqual(queryStack,
                        stack
                    ) && ItemStack.areNbtEqual(queryStack, stack))) {
                        return slot;
                    }
                }
            }
        }

        return -1;
    }

    private static int[] getAvailableSlots(Inventory inventory, Direction side) {
        return inventory instanceof SidedInventory ? ((SidedInventory) inventory).getAvailableSlots(side) : IntStream.range(0,
            inventory.size()
        ).toArray();
    }

    /**
     * Returns true if an itemstack is able to pass through the list of filters.
     *
     * @param itemStack
     * @param filters
     * @return
     */
    private static boolean stackPasses(ItemStack itemStack, List<Filter> filters, Inventory destination) {
        for (Filter filter : filters) {
            if (!filter.stackPasses(itemStack, destination)) {
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
    public static void updatePullerPipes(WorldAccess world, BlockPos blockPos, Direction direction,
                                         Set<BlockPos> searched) {
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

    public List<List<InsertionPoint>> getValidInventories() {
        return prioritizedInsertionPoints;
    }

    /**
     * Determines if the query block can propagate items.
     *
     * @param queryBlock Block to query.
     * @param prevBlock  The previous block in the path.
     * @return True if the query block can propagate items.
     */
    public static boolean canPropagate(Block queryBlock, Block prevBlock) {
        // First check is if the query block is a PipeBlock
        boolean canPropagate = queryBlock instanceof PipeBlock;

        // Second check is for colored pipes.
        // Colored pipes can only propagate to same-color pipes or the base pipe.
        if (prevBlock instanceof ColoredPipeBlock && queryBlock instanceof ColoredPipeBlock) {
            canPropagate = (prevBlock == queryBlock);
        }

        return canPropagate;
    }

    /**
     * Ensures that priorities increment by one instead of having jumps.
     * IE converts a list of priorities [2, 5, 8] to [0, 1, 2]
     *
     * @param ips List of insertion points.
     */
    public static void deGapPriorities(List<InsertionPoint> ips) {
        ips.sort(Comparator.comparing(InsertionPoint::getPriority));

        int currentPriority = -1;
        int prevIPPriority = -1;

        for (InsertionPoint ip : ips) {
            if (ip.priority > prevIPPriority) {
                prevIPPriority = ip.priority;
                currentPriority++;
            }

            ip.priority = currentPriority;
        }
    }
}
