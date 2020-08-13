package com.kqp.ezpas.init.client;

import com.kqp.ezpas.block.container.FilteredPipeScreenHandler;
import com.kqp.ezpas.client.screen.FilteredPipeScreen;
import com.kqp.ezpas.init.Ezpas;
import com.kqp.ezpas.network.OpenAdvancedFilterScreenS2C;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.client.render.RenderLayer;

public class EzpasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.PIPE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.RIGID_PIPE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.FILTERED_PIPE_WHITELIST, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.FILTERED_PIPE_BLACKLIST, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.DENSE_PIPE, RenderLayer.getTranslucent());

        for (int i = 0; i < Ezpas.COLORED_PIPES.length; i++) {
            BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.COLORED_PIPES[i], RenderLayer.getTranslucent());
        }

        ScreenRegistry.<FilteredPipeScreenHandler, FilteredPipeScreen>register(Ezpas.FILTERED_PIPE_SCREEN_HANDLER_TYPE, (screenHandler, inv, text) -> new FilteredPipeScreen(screenHandler, inv));

        OpenAdvancedFilterScreenS2C.register();
    }
}
