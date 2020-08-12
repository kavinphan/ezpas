package com.kqp.ezpas.block.entity;

import com.kqp.ezpas.block.FilteredPipeBlock;
import com.kqp.ezpas.block.container.FilteredPipeScreenHandler;
import com.kqp.ezpas.block.entity.pullerpipe.PullerPipeBlockEntity;
import com.kqp.ezpas.init.Ezpas;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;

import java.util.HashSet;

public class FilteredPipeBlockEntity extends LootableContainerBlockEntity implements ExtendedScreenHandlerFactory {
    public DefaultedList<ItemStack> inventory;
    public boolean persist = false;

    public FilteredPipeBlockEntity() {
        super(Ezpas.FILTERED_PIPE_BLOCK_ENTITY);

        this.inventory = DefaultedList.ofSize(9, ItemStack.EMPTY);
    }

    @Override
    public int size() {
        return 9;
    }

    @Override
    protected void setInvStackList(DefaultedList<ItemStack> list) {
        this.inventory = list;
    }

    @Override
    protected Text getContainerName() {
        return new TranslatableText("container.filtered_pipe");
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new FilteredPipeScreenHandler(syncId, playerInventory, this,
                getFilterType(), persist);
    }

    @Override
    public void fromTag(BlockState bs, CompoundTag tag) {
        super.fromTag(bs, tag);

        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);

        if (!this.deserializeLootTable(tag)) {
            Inventories.fromTag(tag, this.inventory);
        }

        this.persist = tag.getBoolean("Persist");
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);

        if (!this.serializeLootTable(tag)) {
            Inventories.toTag(tag, this.inventory);
        }

        tag.putBoolean("Persist", persist);

        return tag;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack stack = super.removeStack(slot, amount);
        updateSystem();

        return stack;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack stack = super.removeStack(slot);

        updateSystem();

        return stack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        super.setStack(slot, stack);

        updateSystem();
    }

    @Override
    protected DefaultedList<ItemStack> getInvStackList() {
        return this.inventory;
    }

    public void updateSystem() {
        Block block = world.getBlockState(pos).getBlock();

        if (block instanceof FilteredPipeBlock) {
            PullerPipeBlockEntity.updatePullerPipes(world, pos, Direction.NORTH, new HashSet());
        }
    }

    public FilteredPipeBlock.Type getFilterType() {
        return ((FilteredPipeBlock) this.world.getBlockState(this.pos).getBlock()).type;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeBoolean(persist);
    }
}
