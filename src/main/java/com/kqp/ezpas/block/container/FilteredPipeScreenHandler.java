package com.kqp.ezpas.block.container;

import com.kqp.ezpas.block.FilteredPipeBlock;
import com.kqp.ezpas.init.Ezpas;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class FilteredPipeScreenHandler extends ScreenHandler {
    public PlayerInventory playerInventory;
    public Inventory inventory;
    public FilteredPipeBlock.Type type;

    public FilteredPipeScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, FilteredPipeBlock.Type type) {
        super(Ezpas.FILTERED_PIPE_SCREEN_HANDLER_TYPE, syncId);

        this.playerInventory = playerInventory;
        this.inventory = inventory;
        this.type = type;

        checkSize(inventory, 27);
        inventory.onOpen(playerInventory.player);

        int i;
        int j;

        // Filtered pipe inventory
        for (i = 0; i < 3; i++) {
            for (j = 0; j < 9; j++) {
                this.addSlot(new Slot(inventory, i * 9 + j, 8 + j * 18, 18 + i * 18));
            }
        }

        // Player inventory
        for (j = 0; j < 3; ++j) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + j * 9 + 9, 8 + l * 18, j * 18 + 86));
            }
        }

        for (j = 0; j < 9; ++j) {
            this.addSlot(new Slot(playerInventory, j, 8 + j * 18, 144));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();

            newStack = originalStack.copy();

            if (invSlot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
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
