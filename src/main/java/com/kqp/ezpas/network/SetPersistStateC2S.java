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

public class SetPersistStateC2S {
    public static final Identifier ID = Ezpas.id("set_persist_state_c2s");

    public static void sendToServer(BlockPos blockPos, boolean persist) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(blockPos);
        buf.writeBoolean(persist);

        ClientSidePacketRegistry.INSTANCE.sendToServer(ID, buf);
    }

    @Environment(EnvType.CLIENT)
    public static void register() {
        ServerSidePacketRegistry.INSTANCE.register(ID, (context, buf) -> {
            BlockPos blockPos = buf.readBlockPos();
            boolean persist = buf.readBoolean();

            context.getTaskQueue().execute(() -> {
                BlockEntity blockEntity = context.getPlayer().world.getBlockEntity(blockPos);

                if (blockEntity instanceof FilteredPipeBlockEntity) {
                    ((FilteredPipeBlockEntity) blockEntity).persist = persist;
                    ((FilteredPipeBlockEntity) blockEntity).updateSystem();
                }
            });
        });
    }
}