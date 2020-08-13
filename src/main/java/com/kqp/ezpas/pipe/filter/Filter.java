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

        boolean checkFlags = false;

        for (int i = 0; i < flags.length; i++) {
            if (flags[i]) {
                checkFlags = true;
                break;
            }
        }

        if (!checkFlags) {
            boolean passes = !getSameItemStacks(queryStack.getItem()).isEmpty();

            return passes == passingEqualityCondition;
        }

        if (flags[FilteredPipeBlockEntity.MATCH_MOD_FLAG]) {
            Set<String> validNamespaces = itemStacks.stream()
                    .filter(itemStack -> !itemStack.isEmpty())
                    .map(ItemStack::getItem)
                    .map(Registry.ITEM::getId)
                    .map(Identifier::getNamespace)
                    .collect(Collectors.toSet());

            boolean passes = validNamespaces.contains(Registry.ITEM.getId(queryStack.getItem()).getNamespace());

            return passes == passingEqualityCondition;
        }

        // Gather non-empty stacks
        List<ItemStack> filterStacks = getSameItemStacks(queryStack.getItem());

        boolean useOr = flags[FilteredPipeBlockEntity.OR_AND_FLAG];
        List<Boolean> passes = new ArrayList();

        if (flags[FilteredPipeBlockEntity.MATCH_ITEM_TAG_FLAG]) {
            // Gather tag IDs of the filter stacks
            Set<Identifier> filterTagIds = itemStacks.stream()
                    .filter(itemStack -> !itemStack.isEmpty())
                    .map(ItemStack::getItem)
                    .map(Filter::getTagIdsFor)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());

            // Gather tag IDs of query stack
            Set<Identifier> queryTagIds = new HashSet(getTagIdsFor(queryStack.getItem()));

            // If there is an intersection then there are common tags, which is a pass
            passes.add(!Sets.intersection(filterTagIds, queryTagIds).isEmpty());
        }

        if (flags[FilteredPipeBlockEntity.MATCH_NBT_FLAG]) {
            boolean matched = false;

            for (ItemStack filterStack : filterStacks) {
                if (filterStack.getTag().equals(queryStack.getTag())) {
                    matched = true;
                    break;
                }
            }

            passes.add(matched);
        }

        if (flags[FilteredPipeBlockEntity.MATCH_CUSTOM_NAME_FLAG]) {
            boolean found = false;

            for (ItemStack filterStack : filterStacks) {
                if (filterStack.getName().equals(queryStack.getName())) {
                    found = true;
                    break;
                }
            }

            passes.add(found);
        }

        if (flags[FilteredPipeBlockEntity.MATCH_DAMAGE_FLAG]) {
            boolean matched = false;

            for (ItemStack filterStack : filterStacks) {
                if (filterStack.getDamage() == queryStack.getDamage()) {
                    matched = true;
                    break;
                }
            }

            passes.add(matched);
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

            passes.add(found);
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

            passes.add(needed > 0);
        }

        boolean finalPass = true;

        if (useOr) {
            finalPass = false;
        }

        for (Boolean pass : passes) {
            if (pass) {
                if (useOr) {
                    finalPass = true;
                    break;
                }
            } else {
                if (!useOr) {
                    finalPass = false;
                    break;
                }
            }
        }

        return finalPass == passingEqualityCondition;
    }

    private static List<Identifier> getTagIdsFor(Item item) {
        List<Identifier> tagIds = new ArrayList();

        ItemTags.getContainer().getEntries().forEach((id, tag) -> {
            if (tag.contains(item)) {
                tagIds.add(id);
            }
        });

        return tagIds;
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
