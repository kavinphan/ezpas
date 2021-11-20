package com.kqp.ezpas.network;

import com.kqp.ezpas.Ezpas;
import com.kqp.ezpas.block.entity.FilteredPipeBlockEntity;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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

        ClientPlayNetworking.send(ID, buf);
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, resSender) -> {
            BlockPos blockPos = buf.readBlockPos();
            int index = buf.readInt();
            boolean state = buf.readBoolean();

            server.execute(() -> {
                World world = player.world;
                BlockEntity blockEntity = world.getBlockEntity(blockPos);

                if (blockEntity instanceof FilteredPipeBlockEntity) {
                    ((FilteredPipeBlockEntity) blockEntity).flags[index] = state;
                    ((FilteredPipeBlockEntity) blockEntity).updateSystem();
                }
            });
        });
    }
}