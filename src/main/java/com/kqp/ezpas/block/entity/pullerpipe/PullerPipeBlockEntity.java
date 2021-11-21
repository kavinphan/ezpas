package com.kqp.ezpas.block.entity.pullerpipe;

import com.kqp.ezpas.Ezpas;
import com.kqp.ezpas.block.ColoredPipeBlock;
import com.kqp.ezpas.block.FilteredPipeBlock;
import com.kqp.ezpas.block.PipeBlock;
import com.kqp.ezpas.block.RigidPipeBlock;
import com.kqp.ezpas.block.entity.FilteredPipeBlockEntity;
import com.kqp.ezpas.block.pullerpipe.PullerPipeBlock;
import com.kqp.ezpas.pipe.InsertionPoint;
import com.kqp.ezpas.pipe.PathNode;
import com.kqp.ezpas.pipe.filter.Filter;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.*;
import java.util.stream.Collectors;
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

    /**
     * Mark puller pipe to recalculate its insertion points.
     */
    public void markToRecalculate() {
        this.shouldRecalculate = true;
    }

    /**
     * Gets all insertion points.
     *
     * @return List of insertion points.
     */
    public List<InsertionPoint> getInsertionPoints() {
        return prioritizedInsertionPoints.stream().flatMap(List::stream).collect(Collectors.toList());
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
        cachedExtractionStorage = ItemStorage.SIDED.find(world, getExtractionBlockPos(), getExtractionFace());
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
        // Clear insertion points list.
        prioritizedInsertionPoints.clear();

        // Create initial path to prevent back-tracking.
        PathNode rootPathNode = new PathNode(this.pos);

        // Create list of insertion points.
        List<InsertionPoint> newIPs = new ArrayList<InsertionPoint>();

        // Create path map.
        Map<BlockPos, List<PathNode>> pathMap = new HashMap<BlockPos, List<PathNode>>();

        // Build inventory graph.
        calculateInsertionPoints(newIPs, getInsertionBlockPos(), getInsertionFace(), pathMap, rootPathNode);

        // Remove gaps in priorities (ie [0, 5, 7] -> [0, 1, 2]).
        deGapPriorities(newIPs);

        // Add insertion points to the prioritized list of lists of insertion points.
        for (InsertionPoint ip : newIPs) {
            getListForPriority(ip.priority).add(ip);
        }

        prioritizedInsertionPoints.forEach(list -> list.sort(Comparator.comparing(InsertionPoint::getDistance)));
    }

    /**
     * Build list of all insertion points accessible from the given block.
     *
     * @param ipList       List of insertion points.
     * @param blockPos     Current block position to inspect.
     * @param inDir        Inbound direction, which is the opposite of the receiving side.
     * @param pathMap      Map of block positions to their paths.
     * @param prevPathNode Current path.
     */
    private void calculateInsertionPoints(List<InsertionPoint> ipList, BlockPos blockPos, Direction inDir,
                                          Map<BlockPos, List<PathNode>> pathMap, PathNode prevPathNode) {
        // Ignore visited blocks.
        if (prevPathNode.hasVisited(blockPos)) {
            return;
        }

        System.out.println(prevPathNode.priority);

        Block currBlock = world.getBlockState(blockPos).getBlock();
        BlockPos prevBlockPos = blockPos.offset(inDir.getOpposite());
        Block prevBlock = world.getBlockState(prevBlockPos).getBlock();

        // First case is if the current block can propagate items.
        // If so, it should inspect surrounding blocks for insertion points.
        if (canPropagate(currBlock, prevBlock)) {
            // Create new path for the current block.
            PathNode currPathNode = prevPathNode.branch(blockPos);

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
                    currPathNode.addFilter(new Filter(filterPipeBE));
                }
            }

            // If the current block is a dense pipe, increment the new path's priority.
            // This ensures that down stream insertion points are prioritized less.
            if (currBlock == Ezpas.DENSE_PIPE) {
                currPathNode.priority++;
            }

            // Check to see if this block position has already been visited by another path.
            // We won't search further if there exists a path for this block with 1) the same
            // filters or 2) a higher priority (0 = highest, infinity = lowest)
            List<PathNode> pathNodeList = pathMap.computeIfAbsent(blockPos, x -> new ArrayList<>());
            for (int i = 0; i < pathNodeList.size(); i++) {
                PathNode existingPathNode = pathNodeList.get(i);

                if (existingPathNode.priority < currPathNode.priority) {
                    return;
                }
            }

            // Add the new path to the path map
            pathNodeList.add(currPathNode);

            // Recursive call in all directions.
            for (Direction searchDirection : Direction.values()) {
                calculateInsertionPoints(ipList,
                    blockPos.offset(searchDirection),
                    searchDirection,
                    pathMap,
                    currPathNode
                );
            }
        }

        // Second case is if the current block is insertable and not a pipe.
        // We also check to see if the previous block was a rigid pipe,
        // which does not propagate items to insertion points.
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, blockPos, inDir.getOpposite());
        if (storage != null && storage.supportsInsertion() && !(currBlock instanceof PipeBlock) &&
            !(prevBlock instanceof RigidPipeBlock)) {
            // Create new path for the current block.
            PathNode currPathNode = prevPathNode.branch(blockPos);

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
                        currPathNode.addFilter(new Filter(filterPipeBE));
                    }
                }
            }

            InsertionPoint newIP = new InsertionPoint(world,
                blockPos,
                inDir.getOpposite(),
                currPathNode.getFilters(),
                currPathNode.priority,
                currPathNode.getVisitedCount(),
                storage
            );

            // We always want the insertion point with the highest priority and lowest distance.
            // This loop ensures that this is maintained.
            for (int i = 0; i < ipList.size(); i++) {
                InsertionPoint ip = ipList.get(i);

                if (ip.blockPos.equals(newIP.blockPos) && ip.side == newIP.side && ip.filters.equals(newIP.filters)) {
                    if (ip.priority < newIP.priority) {
                        return;
                    } else if (ip.priority == newIP.priority) {
                        if (ip.distance <= newIP.distance) {
                            return;
                        } else {
                            ipList.remove(i);
                            i--;
                        }
                    } else {
                        ipList.remove(i);
                        i--;
                    }
                }
            }

            ipList.add(newIP);
        }
    }

    /**
     * Performs all extractions that this puller block should do.
     */
    private void performExtractions() {
        // Set current priority to highest (0).
        currentPriority = 0;
        validateRRCounter();

        // Perform extractions
        for (int i = 0; i < subTickRate; i++) {
            // For each extraction, we'll do x attempts, where x is how many
            // insertion points there are in total.
            final int maxAttempts = prioritizedInsertionPoints.stream().map(List::size).reduce(0, Integer::sum);
            int attempts = 0;
            boolean success = false;
            while (attempts < maxAttempts && !success) {
                success = performExtraction();
                attempts++;
                incrementRRCounter();
            }
        }

        coolDown = speed;
    }

    /**
     * Performs one extraction.
     *
     * @return True if success;
     */
    private boolean performExtraction() {
        InsertionPoint ip = getListForPriority(currentPriority).get(rrCounter);
        boolean success = false;
        Transaction trx = Transaction.openOuter();

        // Attempt to extract and insert every resource in the extraction storage.
        for (StorageView<ItemVariant> iv : cachedExtractionStorage.iterable(trx)) {
            ItemVariant resource = iv.getResource();

            if (resource.isBlank()) {
                continue;
            }

            // Get slots of the insertion point.
            List<SingleSlotStorage<ItemVariant>> slots = StreamSupport.stream(ip.storage.iterable(trx).spliterator(),
                    false
                )
                .filter(sv -> sv instanceof SingleSlotStorage<ItemVariant>)
                .map(sv -> (SingleSlotStorage<ItemVariant>) sv)
                .collect(Collectors.toList());

            // Continue if resource does not pass insertion point's filters.
            if (!stackPasses(resource.toStack(), ip.filters, slots)) {
                continue;
            }

            // Attempt insertion into target insertion point using how many resources there are.
            long inserted = StorageUtil.insertStacking(slots, resource, iv.getAmount(), trx);
            if (inserted > 0) {
                // Attempt extraction from source storage.
                // Continue to next resource if unable to.
                long extracted = iv.extract(resource, inserted, trx);
                if (extracted == 0) {
                    continue;
                }

                success = true;
                break;
            }
        }

        if (success) {
            trx.commit();
        } else {
            trx.abort();
        }

        return success;
    }

    private void validateRRCounter() {
        if (rrCounter >= getListForPriority(currentPriority).size()) {
            rrCounter = 0;
        }
    }

    private void incrementRRCounter() {
        rrCounter++;
        if (rrCounter >= getListForPriority(currentPriority).size()) {
            rrCounter = 0;
            incrementCurrentPriority();
        }
    }

    /**
     * Sets the current priority to the next lowest priority (0 -> 1)
     */
    private void incrementCurrentPriority() {
        currentPriority++;
        if (currentPriority >= prioritizedInsertionPoints.size()) {
            currentPriority = 0;
        }
    }

    /**
     * Gets the list of insertion points for a given priority.
     *
     * @param priority Priority to look for.
     * @return List of insertion points.
     */
    private List<InsertionPoint> getListForPriority(int priority) {
        while (priority >= prioritizedInsertionPoints.size()) {
            prioritizedInsertionPoints.add(new ArrayList<InsertionPoint>());
        }

        return prioritizedInsertionPoints.get(priority);
    }

    /**
     * Gets the block position of the block to be extracted from.
     *
     * @return Block position of the block to be extracted from.
     */
    private BlockPos getExtractionBlockPos() {
        return this.pos.offset(getExtractionFace());
    }

    /**
     * Gets the block position of the block to be inserted into.
     *
     * @return Block position of the block to be inserted into.
     */
    private BlockPos getInsertionBlockPos() {
        return this.pos.offset(getInsertionFace());
    }

    /**
     * Gets the direction of the side that the puller pipe is extracting from.
     *
     * @return The direction of the side that the puller pipe is extracting from.
     */
    private Direction getExtractionFace() {
        return this.world.getBlockState(this.pos).get(FacingBlock.FACING);
    }

    /**
     * Gets the direction of the side that the puller pipe is inserting into.
     *
     * @return The direction of the side that the puller pipe is inserting into.
     */
    private Direction getInsertionFace() {
        return this.world.getBlockState(this.pos).get(FacingBlock.FACING).getOpposite();
    }

    /**
     * Returns true if an itemstack is able to pass through the list of filters.
     *
     * @param itemStack
     * @param filters
     * @return
     */
    private static boolean stackPasses(ItemStack itemStack, List<Filter> filters,
                                       List<SingleSlotStorage<ItemVariant>> destSlots) {
        for (Filter filter : filters) {
            if (!filter.stackPasses(itemStack, destSlots)) {
                return false;
            }
        }

        return true;
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, PullerPipeBlockEntity blockEntity) {
        blockEntity.serverTick();
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

    /**
     * Determines if the query block can propagate items.
     *
     * @param queryBlock Block to query.
     * @param prevBlock  The previous block in the path.
     * @return True if the query block can propagate items.
     */
    private static boolean canPropagate(Block queryBlock, Block prevBlock) {
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
    private static void deGapPriorities(List<InsertionPoint> ips) {
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
