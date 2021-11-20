package com.kqp.ezpas.pipe;

import com.kqp.ezpas.pipe.filter.Filter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Objects;

public class InsertionPoint {
    public final BlockPos blockPos;
    public final Direction side;
    public final List<Filter> filters;
    public final int priority;
    public final int distance;

    public InsertionPoint(BlockPos blockPos, Direction side, List<Filter> filters, int priority, int distance) {
        this.blockPos = blockPos;
        this.side = side;
        this.filters = filters;
        this.priority = priority;
        this.distance = distance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockPos.getX(), blockPos.getY(), blockPos.getZ(), side.ordinal(), filters, priority);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof InsertionPoint
                && blockPos.equals(((InsertionPoint) obj).blockPos)
                && side.equals(((InsertionPoint) obj).side)
                && filters.equals(((InsertionPoint) obj).filters)
                && priority == ((InsertionPoint) obj).priority;
    }

    public int getPriority() {
        return priority;
    }
}