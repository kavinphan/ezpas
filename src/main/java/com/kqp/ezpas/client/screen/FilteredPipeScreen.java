package com.kqp.ezpas.client.screen;

import com.kqp.ezpas.block.container.FilteredPipeScreenHandler;
import com.kqp.ezpas.init.Ezpas;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public class FilteredPipeScreen extends HandledScreen<FilteredPipeScreenHandler> {
    private static final Identifier TEXTURE = Ezpas.id("textures/gui/container/filtered_pipe.png");

    public FilteredPipeScreen(FilteredPipeScreenHandler container, PlayerInventory playerInventory) {
        super(container, playerInventory, new TranslatableText("container.filtered_pipe_" + container.type.name().toLowerCase()));

        this.backgroundWidth = 176;
        this.backgroundHeight = 133;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        super.render(matrices, mouseX, mouseY, delta);

        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        this.textRenderer.draw(matrices, this.title, 8.0F, 6.0F, 4210752);
        this.textRenderer.draw(matrices, this.playerInventory.getDisplayName(), 8.0F, (float) (this.backgroundHeight - 96 + 2), 4210752);
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        MinecraftClient.getInstance().getTextureManager().bindTexture(TEXTURE);

        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }
}
