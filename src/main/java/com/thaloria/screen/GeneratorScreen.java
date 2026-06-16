package com.thaloria.screen;

import com.thaloria.menu.GeneratorMenu;
import com.thaloria.network.GeneratorActionPacket;
import com.thaloria.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {

    public GeneratorScreen(GeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 220;
        this.imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();
        // Прячем заголовок инвентаря игрока
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick,
                            int mouseX, int mouseY) {
        // Не рисуем стандартный фон
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);

        int cx = width / 2;
        int panelW = 220;
        int panelH = 210;
        int px = cx - panelW / 2;
        int py = height / 2 - panelH / 2;

        int burnTime    = menu.getBurnTime();
        int maxBurnTime = menu.getMaxBurnTime();
        int energy      = menu.getEnergy();
        boolean burning = burnTime > 0;

        // Фон
        graphics.fill(px, py, px + panelW, py + panelH, 0xDD050C14);

        // Цвет рамки — зелёный если горит, красный если нет топлива
        int borderColor = burning ? 0xFF00FF88 : 0xFFFF3300;
        graphics.fill(px,            py,            px + panelW, py + 1,      borderColor);
        graphics.fill(px,            py + panelH-1, px + panelW, py + panelH, borderColor);
        graphics.fill(px,            py,            px + 1,      py + panelH, borderColor);
        graphics.fill(px + panelW-1, py,            px + panelW, py + panelH, borderColor);

        // Заголовок
        graphics.drawCenteredString(font, "POWER GENERATOR MK-I",
                cx, py + 6, 0xFF00FF88);
        graphics.fill(px + 6, py + 16, px + panelW - 6, py + 17, 0x8800FF88);

        // Статус
        String statusStr = burning ? "§aBURNING" : "§cIDLE";
        graphics.drawString(font, "STATUS: " + statusStr,
                px + 8, py + 22, 0xFFCCCCCC, false);

        // Разделитель
        graphics.fill(px + 6, py + 32, px + panelW - 6, py + 33, 0x4400FF88);

        // ENERGY бар
        graphics.drawString(font, "ENERGY OUTPUT",
                px + 8, py + 38, 0xFF00FF88, false);
        graphics.drawString(font, energy + " / 10000",
                px + panelW - 8 - font.width(energy + " / 10000"),
                py + 38, 0xFFFFFFFF, false);

        int barW = panelW - 16;
        int energyY = py + 50;
        graphics.fill(px + 8, energyY, px + 8 + barW, energyY + 12, 0xFF0A1A1A);
        graphics.fill(px + 8, energyY, px + 8 + barW, energyY + 1,  0xFF223333);
        graphics.fill(px + 8, energyY + 11, px + 8 + barW, energyY + 12, 0xFF223333);

        int energyFilled = (int)(barW * energy / 10000f);
        int energyColor = energy > 5000 ? 0xFF00FF88
                : energy > 2000 ? 0xFFAAFF00
                : energy > 500  ? 0xFFFFAA00
                : 0xFFFF2200;
        if (energyFilled > 0) {
            graphics.fill(px + 8, energyY, px + 8 + energyFilled,
                    energyY + 12, energyColor);
        }
        // Метки 25/50/75%
        for (int pct : new int[]{25, 50, 75}) {
            int mx = px + 8 + (int)(barW * pct / 100f);
            graphics.fill(mx, energyY, mx + 1, energyY + 12, 0x44FFFFFF);
        }

        // Разделитель
        graphics.fill(px + 6, py + 66, px + panelW - 6, py + 67, 0x4400FF88);

        // BURN TIME бар
        graphics.drawString(font, "FUEL",
                px + 8, py + 72, 0xFF00FF88, false);
        String burnStr = burning
                ? burnTime + " ticks"
                : "NO FUEL";
        graphics.drawString(font, burnStr,
                px + panelW - 8 - font.width(burnStr),
                py + 72, burning ? 0xFFFFFFFF : 0xFFFF4444, false);

        int burnY = py + 84;
        graphics.fill(px + 8, burnY, px + 8 + barW, burnY + 8, 0xFF0A1A1A);
        if (burning && maxBurnTime > 0) {
            int burnFilled = (int)(barW * ((float)burnTime / maxBurnTime));
            graphics.fill(px + 8, burnY, px + 8 + burnFilled, burnY + 8, 0xFFFF6600);
        }

        // Разделитель
        graphics.fill(px + 6, py + 96, px + panelW - 6, py + 97, 0x4400FF88);

        // Слот топлива — рисуем вручную
        graphics.drawString(font, "FUEL SLOT",
                px + 8, py + 102, 0xFF00FF88, false);
        graphics.drawString(font, "§7(any burnable item)",
                px + 8, py + 114, 0xFF888888, false);

        // Рамка вокруг слота (слот находится в центре)
        int slotX = leftPos + 80;
        int slotY = topPos + 130;
        graphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0xFF00FF88);
        graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF0A1A1A);

        // Надпись под слотом
        graphics.drawCenteredString(font, "§7drop fuel here",
                cx, slotY + 22, 0xFF666666);

        // Рисуем стандартный контент (слоты предметов)
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}