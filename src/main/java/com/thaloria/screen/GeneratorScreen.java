package com.thaloria.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.thaloria.menu.GeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;

public class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("thaloria", "textures/gui/generator.png");

    public GeneratorScreen(GeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int burn = menu.getBurnTime();
        int maxBurn = menu.getMaxBurnTime();
        int energy = menu.getEnergy();

        if (burn > 0 && maxBurn > 0) {
            int height = (int)(13 * ((float)burn / maxBurn));
            graphics.blit(TEXTURE,
                    leftPos + 80,
                    topPos + 50 - height,
                    176,
                    13 - height,
                    14,
                    height);
        }

        int energyWidth = (int)(50 * (energy / 10000f));

        graphics.fill(
                leftPos + 60,
                topPos + 20,
                leftPos + 60 + energyWidth,
                topPos + 28,
                0xFF00FF00
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}