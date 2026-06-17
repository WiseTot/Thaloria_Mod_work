package com.thaloria.screen;

import com.thaloria.menu.OxygenCompressorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class OxygenCompressorScreen extends AbstractContainerScreen<OxygenCompressorMenu> {

    // Координаты слота в меню (из OxygenCompressorMenu)
    private static final int SLOT_X = 170;
    private static final int SLOT_Y = 60;

    public OxygenCompressorScreen(OxygenCompressorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth  = 220;
        this.imageHeight = 210;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY     = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g);

        int cx     = width / 2;
        int panelW = 220;
        int panelH = 210;
        int px     = cx - panelW / 2;
        int py     = height / 2 - panelH / 2;

        boolean powered = menu.isPowered();
        int oxygen      = menu.getCanisterOxygen();
        boolean has     = oxygen >= 0;

        // ── Фон ──────────────────────────────────────────────────
        g.fill(px, py, px + panelW, py + panelH, 0xDD050C14);

        // ── Рамка панели ─────────────────────────────────────────
        int border = powered ? 0xFF00CCFF : 0xFFFF3300;
        g.fill(px,            py,            px + panelW, py + 1,      border);
        g.fill(px,            py + panelH-1, px + panelW, py + panelH, border);
        g.fill(px,            py,            px + 1,      py + panelH, border);
        g.fill(px + panelW-1, py,            px + panelW, py + panelH, border);

        // ── Заголовок ────────────────────────────────────────────
        g.drawCenteredString(font, "OXYGEN COMPRESSOR MK-I", cx, py + 6, 0xFF00CCFF);
        g.fill(px + 6, py + 16, px + panelW - 6, py + 17, 0x8800CCFF);

        // ── Статус ───────────────────────────────────────────────
        String status = powered ? "§aPOWERED" : "§cNO POWER";
        g.drawString(font, "STATUS: " + status, px + 8, py + 21, 0xFFCCCCCC, false);
        g.fill(px + 6, py + 31, px + panelW - 6, py + 32, 0x4400CCFF);

        // ── Canister oxygen label + значение ─────────────────────
        g.drawString(font, "CANISTER OXYGEN", px + 8, py + 37, 0xFF00CCFF, false);
        String oxyStr = has ? oxygen + "%" : "NO CANISTER";
        int oxyCol    = has
                ? (oxygen > 60 ? 0xFF00CCFF : oxygen > 30 ? 0xFFFFAA00 : 0xFFFF2200)
                : 0xFF555555;
        g.drawString(font, oxyStr,
                px + panelW - 8 - font.width(oxyStr), py + 37, oxyCol, false);

        // ── Бар кислорода (левая часть, справа место под слот) ───
        // Реальная экранная позиция слота
        int slotScreenX = leftPos + SLOT_X;
        int slotScreenY = topPos  + SLOT_Y;

        int barX = px + 8;
        int barW = slotScreenX - barX - 8; // до слота с отступом 8px
        int barY = py + 50;

        g.fill(barX, barY, barX + barW, barY + 14, 0xFF0A1A1A);
        g.fill(barX, barY, barX + barW, barY + 1,  0xFF1A3344);
        g.fill(barX, barY + 13, barX + barW, barY + 14, 0xFF1A3344);

        if (has) {
            int filled   = (int)(barW * oxygen / 100f);
            int barColor = oxygen > 60 ? 0xFF00CCFF
                    : oxygen > 30 ? 0xFFFFAA00 : 0xFFFF2200;
            if (filled > 0)
                g.fill(barX, barY, barX + filled, barY + 14, barColor);
            for (int pct : new int[]{25, 50, 75}) {
                int mx2 = barX + (int)(barW * pct / 100f);
                g.fill(mx2, barY, mx2 + 1, barY + 14, 0x44FFFFFF);
            }
            g.drawString(font, oxygen + "%", barX + 4, barY + 3, 0xFFFFFFFF, false);
        } else {
            g.drawCenteredString(font, "§7---", barX + barW / 2, barY + 3, 0xFF333333);
        }

        // ── Рамка вокруг слота — точно по 16x16 пикселям слота ──
        int sx = slotScreenX - 1;
        int sy = slotScreenY - 1;
        int slotBorder = has
                ? (oxygen > 60 ? 0xFF00CCFF : oxygen > 30 ? 0xFFFFAA00 : 0xFFFF2200)
                : 0xFF334455;
        g.fill(sx,      sy,      sx + 18, sy + 1,  slotBorder); // верх
        g.fill(sx,      sy + 17, sx + 18, sy + 18, slotBorder); // низ
        g.fill(sx,      sy,      sx + 1,  sy + 18, slotBorder); // лево
        g.fill(sx + 17, sy,      sx + 18, sy + 18, slotBorder); // право

        // Подпись под слотом
        g.drawCenteredString(font, "§7SLOT", slotScreenX + 8, slotScreenY + 20, 0xFF334455);

        // ── Статус зарядки ───────────────────────────────────────
        g.fill(px + 6, barY + 18, px + panelW - 6, barY + 19, 0x4400CCFF);

        String chargeStatus;
        int    chargeColor;
        if (!powered)        { chargeStatus = "OFFLINE — connect to Generator"; chargeColor = 0xFFFF4444; }
        else if (!has)       { chargeStatus = "IDLE — insert canister";          chargeColor = 0xFF888888; }
        else if (oxygen>=100){ chargeStatus = "FULLY CHARGED";                   chargeColor = 0xFF00FF88; }
        else                 { chargeStatus = "CHARGING...";                     chargeColor = 0xFF00CCFF; }

        g.drawCenteredString(font, chargeStatus, cx, barY + 23, chargeColor);

        super.render(g, mouseX, mouseY, delta);
        renderTooltip(g, mouseX, mouseY);
    }
}