package com.kqp.ezpas.pipe;

import com.kqp.ezpas.pipe.filter.Filter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Objects;

public class InsertionPoint {
    public final BlockPos blockPos;
    public final Direction direction;
    public final List<Filter> filters;
    public final int priority;
    public final int distance;

    public InsertionPoint(BlockPos blockPos, Direction direction, List<Filter> filters, int priority, int distance) {
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
        return obj instanceof InsertionPoint
                && blockPos.equals(((InsertionPoint) obj).blockPos)
                && direction.equals(((InsertionPoint) obj).direction)
                && filters.equals(((InsertionPoint) obj).filters)
                && priority == ((InsertionPoint) obj).priority;
    }

    public int getPriority() {
        return priority;
    }
}