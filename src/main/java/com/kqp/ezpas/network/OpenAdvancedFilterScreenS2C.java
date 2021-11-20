package com.kqp.ezpas.network;

import com.kqp.ezpas.Ezpas;
import com.kqp.ezpas.client.EzpasClient;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class OpenAdvancedFilterScreenS2C {
    public static final Identifier ID = Ezpas.id("open_advanced_filter_screen_s2c");

    public static void sendToPlayer(ServerPlayerEntity player, BlockPos blockPos, boolean[] flags) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(blockPos);

        buf.writeInt(flags.length);
        for (boolean flag : flags) {
            buf.writeBoolean(flag);
        }

        ServerPlayNetworking.getSender(player).sendPacket(ID, buf);
    }

    @Environment(EnvType.CLIENT)
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, handler, buf, resSender) -> {
            BlockPos blockPos = buf.readBlockPos();
            int flagArrayLength = buf.readInt();
            boolean[] flags = new boolean[flagArrayLength];

            for (int i = 0; i < flagArrayLength; i++) {
                flags[i] = buf.readBoolean();
            }

            client.execute(() -> {
                EzpasClient.openAdvancedFilterScreen(blockPos, flags);
            });
        });
    }
}