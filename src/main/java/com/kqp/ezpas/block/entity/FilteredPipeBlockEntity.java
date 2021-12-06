package com.kqp.ezpas.block.entity;

import com.kqp.ezpas.Ezpas;
import com.kqp.ezpas.block.FilteredPipeBlock;
import com.kqp.ezpas.block.container.FilteredPipeScreenHandler;
import com.kqp.ezpas.block.entity.pullerpipe.PullerPipeBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashSet;

public class FilteredPipeBlockEntity extends LootableContainerBlockEntity implements ExtendedScreenHandlerFactory {
    public static final int PERSIST_FLAG = 0;
    public static final int OR_AND_FLAG = 1;
    public static final int MATCH_ITEM_TAG_FLAG = 2;
    public static final int MATCH_NBT_FLAG = 3;
    public static final int MATCH_CUSTOM_NAME_FLAG = 4;
    public static final int MATCH_DAMAGE_FLAG = 5;
    public static final int ONE_IN_INV_FLAG = 6;
    public static final int MATCH_MOD_FLAG = 7;
    public static final int MAINTAIN_COUNTS_FLAG = 8;
    public static final int REDSTONE_DISABLE_FLAG = 9;

    public DefaultedList<ItemStack> inventory;

    public boolean[] flags = new boolean[20];

    public FilteredPipeBlockEntity(BlockPos pos, BlockState state) {
        super(Ezpas.FILTERED_PIPE_BLOCK_ENTITY, pos, state);

        this.inventory = DefaultedList.ofSize(27, ItemStack.EMPTY);
    }

    @Override
    public int size() {
        return 27;
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
        return new FilteredPipeScreenHandler(syncId, playerInventory, this, getFilterType());
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);

        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);

        if (!this.deserializeLootTable(tag)) {
            Inventories.readNbt(tag, this.inventory);
        }

        for (int i = 0; i < flags.length; i++) {
            flags[i] = tag.getBoolean("Flag" + i);
        }
    }

    @Override
    public void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);

        if (!this.serializeLootTable(tag)) {
            Inventories.writeNbt(tag, this.inventory);
        }

        for (int i = 0; i < flags.length; i++) {
            tag.putBoolean("Flag" + i, flags[i]);
        }
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
    }
}
