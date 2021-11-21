package com.kqp.ezpas.pipe;

import com.kqp.ezpas.pipe.filter.Filter;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;

public class InsertionPoint {
    public final World world;
    public final BlockPos blockPos;
    public final Direction side;

    public final List<Filter> filters;

    public int priority;

    public final int distance;

    public Storage<ItemVariant> storage;

    public InsertionPoint(World world, BlockPos blockPos, Direction side, List<Filter> filters, int priority, int distance,
                          Storage<ItemVariant> storage) {
        this.world = world;
        this.blockPos = blockPos;
        this.side = side;
        this.filters = filters;
        this.priority = priority;
        this.distance = distance;
        this.storage = storage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockPos.getX(), blockPos.getY(), blockPos.getZ(), side.ordinal(), filters, priority);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof InsertionPoint && blockPos.equals(((InsertionPoint) obj).blockPos) && side.equals(((InsertionPoint) obj).side) && filters.equals(
            ((InsertionPoint) obj).filters) && priority == ((InsertionPoint) obj).priority;
    }

    public int getPriority() {
        return priority;
    }

    public void syncStorage() {
        storage = ItemStorage.SIDED.find(world, blockPos, side);
    }

    public boolean invalid() {
        return storage == null && !storage.supportsInsertion();
    }
}