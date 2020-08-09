package com.kqp.ezpas.pipe.filter;

import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Registry;

import java.util.Objects;

public class ComparableItemStack {
    public final ItemStack itemStack;

    public ComparableItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComparableItemStack that = (ComparableItemStack) o;
        return ItemStack.areEqual(itemStack, that.itemStack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                Registry.ITEM.getId(itemStack.getItem()),
                itemStack.getCount(),
                itemStack.getOrCreateTag()
        );
    }
}
