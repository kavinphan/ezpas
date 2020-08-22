package com.kqp.ezpas.pipe.filter;

import com.google.common.collect.Sets;
import com.kqp.ezpas.block.FilteredPipeBlock;
import com.kqp.ezpas.block.entity.FilteredPipeBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.*;
import java.util.stream.Collectors;

public class Filter {
    public final FilteredPipeBlock.Type type;
    public final List<ItemStack> itemStacks;
    public final boolean[] flags;

    public Filter(FilteredPipeBlockEntity filterPipe) {
        this.type = filterPipe.getFilterType();
        this.itemStacks = filterPipe.inventory;
        this.flags = filterPipe.flags;
    }

    public boolean stackPasses(ItemStack queryStack, Inventory destination) {
        // Equality checks must equal this condition for the stack to pass
        boolean passingEqualityCondition = type == FilteredPipeBlock.Type.WHITELIST;

        // Gather non-empty stacks
        List<ItemStack> filterStacks = getSameItemStacks(queryStack.getItem());

        boolean useOr = flags[FilteredPipeBlockEntity.OR_AND_FLAG];

        List<Boolean> flagPasses = new ArrayList();
        List<Boolean> queryStackParityPasses = new ArrayList();

        if (flags[FilteredPipeBlockEntity.MATCH_MOD_FLAG]) {
            Set<String> validNamespaces = itemStacks.stream()
                    .filter(itemStack -> !itemStack.isEmpty())
                    .map(ItemStack::getItem)
                    .map(Registry.ITEM::getId)
                    .map(Identifier::getNamespace)
                    .collect(Collectors.toSet());

            boolean passes = validNamespaces.contains(Registry.ITEM.getId(queryStack.getItem()).getNamespace());

            queryStackParityPasses.add(passes);
            flagPasses.add(passes);
        }

        if (flags[FilteredPipeBlockEntity.MATCH_ITEM_TAG_FLAG]) {
            // Gather tag IDs of the filter stacks
            Set<Identifier> filterTagIds = itemStacks.stream()
                    .filter(itemStack -> !itemStack.isEmpty())
                    .map(ItemStack::getItem)
                    .map(item -> ItemTags.getTagGroup().getTagsFor(item))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());

            // Gather tag IDs of query stack
            Set<Identifier> queryTagIds = new HashSet(ItemTags.getTagGroup().getTagsFor(queryStack.getItem()));

            // If there is an intersection then there are common tags, which is a pass
            boolean passes = !Sets.intersection(filterTagIds, queryTagIds).isEmpty();

            queryStackParityPasses.add(passes);
            flagPasses.add(passes);
        }

        if (queryStackParityPasses.isEmpty()) {
            boolean passes = !getSameItemStacks(queryStack.getItem()).isEmpty();

            queryStackParityPasses.add(passes);
        }

        boolean queryStackParity = evaluateBooleanList(queryStackParityPasses, useOr);

        if (!queryStackParity) {
            return !passingEqualityCondition;
        }

        if (flags[FilteredPipeBlockEntity.MATCH_NBT_FLAG]) {
            boolean matched = false;

            if (queryStack.getTag() != null) {
                for (ItemStack filterStack : filterStacks) {
                    if (filterStack.getTag() != null && filterStack.getTag().equals(queryStack.getTag())) {
                        matched = true;
                        break;
                    }
                }
            }

            flagPasses.add(matched);
        }

        if (flags[FilteredPipeBlockEntity.MATCH_CUSTOM_NAME_FLAG]) {
            boolean found = false;

            for (ItemStack filterStack : filterStacks) {
                if (filterStack.getName().equals(queryStack.getName())) {
                    found = true;
                    break;
                }
            }

            flagPasses.add(found);
        }

        if (flags[FilteredPipeBlockEntity.MATCH_DAMAGE_FLAG]) {
            boolean matched = false;

            for (ItemStack filterStack : filterStacks) {
                if (filterStack.getDamage() == queryStack.getDamage()) {
                    matched = true;
                    break;
                }
            }

            flagPasses.add(matched);
        }

        if (flags[FilteredPipeBlockEntity.ONE_IN_INV_FLAG]) {
            boolean found = false;

            for (int i = 0; i < destination.size(); i++) {
                ItemStack invStack = destination.getStack(i);

                if (invStack.getItem() == queryStack.getItem()) {
                    found = true;
                    break;
                }
            }

            flagPasses.add(found);
        }

        if (flags[FilteredPipeBlockEntity.MAINTAIN_COUNTS_FLAG]) {
            int needed = 0;
            for (ItemStack filterStack : filterStacks) {
                needed += filterStack.getCount();
            }

            for (int i = 0; i < destination.size(); i++) {
                ItemStack invStack = destination.getStack(i);

                if (invStack.getItem() == queryStack.getItem()) {
                    needed -= invStack.getCount();

                    if (needed <= 0) {
                        break;
                    }
                }
            }

            flagPasses.add(needed > 0);
        }

        return evaluateBooleanList(flagPasses, useOr) == passingEqualityCondition;
    }

    private static boolean evaluateBooleanList(List<Boolean> booleans, boolean useOr) {
        if (booleans.size() == 0) {
            return true;
        } else if (booleans.size() == 1) {
            return booleans.get(0);
        }

        boolean returnValue = booleans.get(0);

        for (int i = 1; i < booleans.size(); i++) {
            if (useOr) {
                returnValue |= booleans.get(i);
            } else {
                returnValue &= booleans.get(i);
            }
        }

        return returnValue;
    }

    private List<ItemStack> getSameItemStacks(Item item) {
        return itemStacks.stream().filter(stack -> !stack.isEmpty() && stack.getItem() == item).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Filter filter = (Filter) o;
        return type == filter.type &&
                flags.equals(filter.flags) &&
                Objects.equals(
                        itemStacks.stream().map(ComparableItemStack::new).collect(Collectors.toSet()),
                        filter.itemStacks.stream().map(ComparableItemStack::new).collect(Collectors.toSet())
                );
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, flags, itemStacks.stream().map(ComparableItemStack::new).collect(Collectors.toSet()));
    }
}
