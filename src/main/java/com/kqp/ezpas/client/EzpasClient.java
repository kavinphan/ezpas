package com.kqp.ezpas.client;

import com.kqp.ezpas.Ezpas;
import com.kqp.ezpas.block.container.FilteredPipeScreenHandler;
import com.kqp.ezpas.client.screen.AdvancedFilterScreen;
import com.kqp.ezpas.client.screen.FilteredPipeScreen;
import com.kqp.ezpas.network.OpenAdvancedFilterScreenS2C;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.BlockPos;

public class EzpasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        setRenderLayers();
        registerScreens();
        initNetworking();
    }

    private static void setRenderLayers() {
        BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.PIPE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.RIGID_PIPE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.FILTERED_PIPE_WHITELIST, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.FILTERED_PIPE_BLACKLIST, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.DENSE_PIPE, RenderLayer.getTranslucent());

        Ezpas.COLORED_PIPES.forEach((coloredPipe) -> {
            BlockRenderLayerMap.INSTANCE.putBlock(coloredPipe, RenderLayer.getTranslucent());
        });
    }

    private static void registerScreens() {
        ScreenRegistry.<FilteredPipeScreenHandler, FilteredPipeScreen>register(
            Ezpas.FILTERED_PIPE_SCREEN_HANDLER_TYPE,
            (screenHandler, inv, text) -> new FilteredPipeScreen(screenHandler, inv)
        );
    }

    private static void initNetworking() {
        OpenAdvancedFilterScreenS2C.register();
    }

    public static void openAdvancedFilterScreen(BlockPos blockPos, boolean[] flags) {
        MinecraftClient.getInstance().setScreen(new AdvancedFilterScreen(blockPos, flags));
    }
}
