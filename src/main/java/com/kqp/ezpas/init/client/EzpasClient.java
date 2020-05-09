package com.kqp.ezpas.init.client;

import com.kqp.ezpas.block.container.FilteredPipeContainer;
import com.kqp.ezpas.client.screen.FilteredPipeScreen;
import com.kqp.ezpas.init.Ezpas;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.TranslatableText;

public class EzpasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.PIPE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.FILTERED_PIPE_WHITELIST, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.FILTERED_PIPE_BLACKLIST, RenderLayer.getCutout());

        for (int i = 0; i < Ezpas.COLORED_PIPES.length; i++) {
            BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.COLORED_PIPES[i], RenderLayer.getTranslucent());
        }

        ScreenProviderRegistry.INSTANCE.<FilteredPipeContainer>registerFactory(Ezpas.FILTERED_PIPE_ID, (container) ->
                new FilteredPipeScreen(container, MinecraftClient.getInstance().player.inventory)
        );
    }
}
