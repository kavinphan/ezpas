package com.kqp.ezpas.block.container;

import com.kqp.ezpas.block.FilteredPipeBlock;
import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public class FilteredPipeContainer extends Container {
    public PlayerInventory playerInventory;
    public Inventory inventory;
    public FilteredPipeBlock.Type type;

    public FilteredPipeContainer(int syncId, PlayerInventory playerInventory, Inventory inventory, FilteredPipeBlock.Type type) {
        super(null, syncId);

        this.playerInventory = playerInventory;
        this.inventory = inventory;
        this.type = type;

        checkContainerSize(inventory, 9);
        inventory.onInvOpen(playerInventory.player);

        int i;
        int j;

        // Filtered pipe inventory
        for (i = 0; i < 9; i++) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 20));
        }


        // Player inventory
        for (j = 0; j < 3; ++j) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + j * 9 + 9, 8 + l * 18, j * 18 + 51));
            }
        }

        for (j = 0; j < 9; ++j) {
            this.addSlot(new Slot(playerInventory, j, 8 + j * 18, 109));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUseInv(player);
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();

            newStack = originalStack.copy();

            if (invSlot < this.inventory.getInvSize()) {
                if (!this.insertItem(originalStack, this.inventory.getInvSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.getInvSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }
}
