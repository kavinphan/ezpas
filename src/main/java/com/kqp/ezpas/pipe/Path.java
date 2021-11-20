package com.kqp.ezpas.pipe;

import com.kqp.ezpas.pipe.filter.Filter;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Path {
    public final Path parent;

    private final Set<BlockPos> visited;
    private final List<Filter> filters;
    public int priority = 0;

    public Path() {
        this(null);
    }

    public Path(Path parent) {
        this.parent = parent;
        this.visited = new HashSet<BlockPos>();
        this.filters = new ArrayList<Filter>();
    }

    public boolean hasVisited(BlockPos bp) {
        return visited.contains(bp) || (parent != null && parent.hasVisited(bp));
    }

    public int getVisitedCount() {
        return (parent != null ? parent.getVisitedCount() : 0) + visited.size();
    }

    public void addVisited(BlockPos bp) {
        visited.add(bp);
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
