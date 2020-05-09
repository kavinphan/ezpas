package com.kqp.ezpas.client.screen;

import com.kqp.ezpas.block.container.FilteredPipeContainer;
import com.kqp.ezpas.init.Ezpas;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public class FilteredPipeScreen extends ContainerScreen<FilteredPipeContainer> {
    private static final Identifier TEXTURE = new Identifier(Ezpas.ID, "textures/gui/container/filtered_pipe.png");

    public FilteredPipeScreen(FilteredPipeContainer container, PlayerInventory playerInventory) {
        super(container, playerInventory, new TranslatableText("container.filtered_pipe_" + container.type.name().toLowerCase()));

        this.containerWidth = 176;
        this.containerHeight = 133;
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        this.renderBackground();

        super.render(mouseX, mouseY, delta);

        this.drawMouseoverTooltip(mouseX, mouseY);
    }

    @Override
    protected void drawForeground(int mouseX, int mouseY) {
        this.font.draw(this.title.asFormattedString(), 8.0F, 6.0F, 4210752);
        this.font.draw(this.playerInventory.getDisplayName().asFormattedString(), 8.0F, (float) (this.containerHeight - 96 + 2), 4210752);
    }

    @Override
    protected void drawBackground(float delta, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        this.minecraft.getTextureManager().bindTexture(TEXTURE);

        int i = (this.width - this.containerWidth) / 2;
        int j = (this.height - this.containerHeight) / 2;

        this.blit(i, j, 0, 0, this.containerWidth, this.containerHeight);
    }
}
