package com.kqp.ezpas.filter;

import com.kqp.ezpas.block.FilteredPipeBlock;
import com.kqp.ezpas.block.entity.FilteredPipeBlockEntity;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Filter {
    private final FilteredPipeBlock.Type type;
    private final List<ItemStack> itemStacks;

    public Filter(FilteredPipeBlockEntity filterPipe) {
        this.type = filterPipe.getFilterType();
        this.itemStacks = filterPipe.inventory;
    }

    public boolean stackPasses(ItemStack queryStack) {
        // Equality checks must equal this condition for the stack to pass
        boolean passingEqualityCondition = type == FilteredPipeBlock.Type.WHITELIST;

        for (int i = 0; i < itemStacks.size(); i++) {
            ItemStack filterStack = itemStacks.get(i);

            if (!filterStack.isEmpty()) {
                if (ItemStack.areItemsEqual(queryStack, filterStack) != passingEqualityCondition) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Filter filter = (Filter) o;
        return type == filter.type &&
                Objects.equals(
                        itemStacks.stream().map(ComparableItemStack::new).collect(Collectors.toSet()),
                        filter.itemStacks.stream().map(ComparableItemStack::new).collect(Collectors.toSet())
                );
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, itemStacks.stream().map(ComparableItemStack::new).collect(Collectors.toSet()));
    }
}
