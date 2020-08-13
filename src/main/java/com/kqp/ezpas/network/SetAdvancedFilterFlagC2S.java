package com.kqp.ezpas.network;

import com.kqp.ezpas.block.entity.FilteredPipeBlockEntity;
import com.kqp.ezpas.init.Ezpas;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SetAdvancedFilterFlagC2S {
    public static final Identifier ID = Ezpas.id("set_advanced_filter_flag_c2s");

    public static void sendToServer(BlockPos blockPos, int index, boolean state) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(blockPos);
        buf.writeInt(index);
        buf.writeBoolean(state);

        ClientSidePacketRegistry.INSTANCE.sendToServer(ID, buf);
    }

    public static void register() {
        ServerSidePacketRegistry.INSTANCE.register(ID, (context, buf) -> {
            BlockPos blockPos = buf.readBlockPos();
            int index = buf.readInt();
            boolean state = buf.readBoolean();

            context.getTaskQueue().execute(() -> {
                World world = context.getPlayer().world;
                BlockEntity blockEntity = world.getBlockEntity(blockPos);

                if (blockEntity instanceof FilteredPipeBlockEntity) {
                    ((FilteredPipeBlockEntity) blockEntity).flags[index] = state;
                    ((FilteredPipeBlockEntity) blockEntity).updateSystem();
                }
            });
        });
    }
}