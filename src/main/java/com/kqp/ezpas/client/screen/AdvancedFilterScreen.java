package com.kqp.ezpas.client.screen;

import com.kqp.ezpas.Ezpas;
import com.kqp.ezpas.network.SetAdvancedFilterFlagC2S;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class AdvancedFilterScreen extends Screen {
    private static final Identifier TEXTURE = Ezpas.id("textures/gui/container/advanced_filtering.png");

    public final BlockPos blockPos;
    public final boolean[] flags;
    public final ButtonWidget[] buttons;

    public int backgroundWidth = 176;
    public int backgroundHeight = 166;

    public AdvancedFilterScreen(BlockPos blockPos, boolean[] flags) {
        super(new TranslatableText("container.filtered_pipe.advanced"));
        this.blockPos = blockPos;
        this.flags = flags;
        this.buttons = new ButtonWidget[20];
    }

    @Override
    protected void init() {
        super.init();

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                int index = i * 4 + j;

                buttons[index] = new ButtonWidget(x + 18 + j * 40,
                    y + 17 + i * 30,
                    20,
                    20,
                    new LiteralText(""),
                    (button) -> {
                        flags[index] = !flags[index];

                        SetAdvancedFilterFlagC2S.sendToServer(blockPos, index, flags[index]);
                    },
                    (button, matrices, mouseX, mouseY) -> {
                        List<Text> text = new ArrayList();
                        text.add(new TranslatableText("container.filtered_pipe.advanced.flag" + index + ".help"));
                        text.add(new TranslatableText("container.filtered_pipe.advanced.flag.current_state").append(new LiteralText(
                            "" + flags[index])));

                        this.renderTooltip(matrices, text, mouseX, mouseY);
                    }
                );

                this.addDrawableChild(buttons[index]);
            }
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        drawBackground(matrices, delta, mouseX, mouseY);

        super.render(matrices, mouseX, mouseY, delta);

        drawForeground(matrices, mouseX, mouseY);
    }

    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        int x = (this.width - this.textRenderer.getWidth(title)) / 2;
        int y = (this.height - this.backgroundHeight) / 2 + 6;

        this.textRenderer.draw(matrices, this.title, x, y, 4210752);

        x = (this.width - this.backgroundWidth) / 2;
        y = (this.height - this.backgroundHeight) / 2;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                int index = i * 4 + j;

                this.itemRenderer.renderGuiItemIcon(FLAG_ICONS[index], x + 20 + j * 40, y + 19 + i * 30);
            }
        }
    }

    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        this.drawTexture(matrices, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.client.player.isAlive() || this.client.player.isRemoved()) {
            this.client.player.closeScreen();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (this.client.options.keyInventory.matchesKey(keyCode, scanCode)) {
            this.onClose();
            return true;
        }

        return true;
    }

    private static final ItemStack[] FLAG_ICONS = { new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.LEVER),
        new ItemStack(Items.WHITE_WOOL), new ItemStack(Items.PAPER), new ItemStack(Items.NAME_TAG),
        new ItemStack(Items.IRON_SWORD), new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.FURNACE),
        new ItemStack(Items.HONEY_BOTTLE), new ItemStack(Items.REDSTONE_TORCH), ItemStack.EMPTY, ItemStack.EMPTY,
        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
        ItemStack.EMPTY, ItemStack.EMPTY, };
}