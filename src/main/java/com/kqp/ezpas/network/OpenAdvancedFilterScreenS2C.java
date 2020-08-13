package com.kqp.ezpas.network;

import com.kqp.ezpas.client.screen.AdvancedFilterScreen;
import com.kqp.ezpas.init.Ezpas;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
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
        for (int i = 0; i < flags.length; i++) {
            buf.writeBoolean(flags[i]);
        }

        ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, ID, buf);
    }

    @Environment(EnvType.CLIENT)
    public static void register() {
        ClientSidePacketRegistry.INSTANCE.register(ID, (context, buf) -> {
            BlockPos blockPos = buf.readBlockPos();
            int flagArrayLength = buf.readInt();
            boolean flags[] = new boolean[flagArrayLength];

            for (int i = 0; i < flagArrayLength; i++) {
                flags[i] = buf.readBoolean();
            }

            context.getTaskQueue().execute(() -> {
                MinecraftClient.getInstance().openScreen(new AdvancedFilterScreen(blockPos, flags));
            });
        });
    }
}