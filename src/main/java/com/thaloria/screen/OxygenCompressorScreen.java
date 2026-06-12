package com.thaloria.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.thaloria.menu.OxygenCompressorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class OxygenCompressorScreen
        extends AbstractContainerScreen<OxygenCompressorMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft",
                    "textures/gui/container/generic_54.png");

    public OxygenCompressorScreen(OxygenCompressorMenu menu,
                                  Inventory inventory,
                                  Component title) {

        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics,
                            float partialTicks,
                            int mouseX,
                            int mouseY) {

        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.blit(TEXTURE, x, y, 0, 0,
                imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics,
                       int mouseX,
                       int mouseY,
                       float partialTicks) {

        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}