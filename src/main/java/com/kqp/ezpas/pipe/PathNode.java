package com.kqp.ezpas.pipe;

import com.kqp.ezpas.pipe.filter.Filter;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class PathNode {
    public final BlockPos blockPos;
    public final PathNode parent;

    private final List<Filter> filters;
    public int priority = 0;

    public PathNode(BlockPos blockPos) {
        this(blockPos, null);
    }

    private PathNode(BlockPos blockPos, PathNode parent) {
        this.blockPos = blockPos;
        this.parent = parent;
        this.filters = new ArrayList<Filter>();

        if (parent != null) {
            this.priority = parent.priority;
        }
    }

    public PathNode branch(BlockPos bp) {
        return new PathNode(bp, this);
    }

    public boolean hasVisited(BlockPos bp) {
        PathNode curr = this;
        while (curr != null) {
            if (curr.blockPos.equals(bp)) {
                return true;
            }

            curr = curr.parent;
        }

        return false;
    }

    public int getVisitedCount() {
        int visited = 0;

        PathNode curr = this;
        while (curr != null) {
            visited++;
            curr = curr.parent;
        }

        return visited;
    }

    public List<Filter> getFilters() {
        List<Filter> ret = new ArrayList<Filter>();
        if (parent != null) {
            ret.addAll(parent.getFilters());
        }
        ret.addAll(filters);

        return ret;
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
    }
}
