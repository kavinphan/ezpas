package net.kqp.ezpas.init.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.kqp.ezpas.init.Ezpas;
import net.minecraft.client.render.RenderLayer;

public class EzpasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.PIPE, RenderLayer.getCutout());

        for (int i = 0; i < Ezpas.COLORED_PIPES.length; i++) {
            BlockRenderLayerMap.INSTANCE.putBlock(Ezpas.COLORED_PIPES[i], RenderLayer.getTranslucent());
        }
    }
}
