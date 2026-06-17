package com.thaloria.screen;

import com.thaloria.menu.OxygenChargerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class OxygenChargerScreen extends AbstractContainerScreen<OxygenChargerMenu> {

    private static final String[] SLOT_NAMES = { "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS" };

    // Координаты слотов в меню (из OxygenChargerMenu)
    private static final int SLOT_X       = 170;
    private static final int SLOT_START_Y = 38;
    private static final int SLOT_STEP    = 22;

    public OxygenChargerScreen(OxygenChargerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth  = 220;
        this.imageHeight = 230;
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
        int panelH = 230;
        int px     = cx - panelW / 2;
        int py     = height / 2 - panelH / 2;

        boolean powered = menu.isPowered();

        // ── Фон ──────────────────────────────────────────────────
        g.fill(px, py, px + panelW, py + panelH, 0xDD050C14);

        // ── Рамка панели ─────────────────────────────────────────
        int border = powered ? 0xFF00CCFF : 0xFFFF3300;
        g.fill(px,            py,            px + panelW, py + 1,      border);
        g.fill(px,            py + panelH-1, px + panelW, py + panelH, border);
        g.fill(px,            py,            px + 1,      py + panelH, border);
        g.fill(px + panelW-1, py,            px + panelW, py + panelH, border);

        // ── Заголовок ────────────────────────────────────────────
        g.drawCenteredString(font, "OXYGEN CHARGER MK-I", cx, py + 6, 0xFF00CCFF);
        g.fill(px + 6, py + 16, px + panelW - 6, py + 17, 0x8800CCFF);

        // ── Статус ───────────────────────────────────────────────
        String status = powered ? "§aPOWERED" : "§cNO POWER";
        g.drawString(font, "STATUS: " + status, px + 8, py + 21, 0xFFCCCCCC, false);
        g.fill(px + 6, py + 31, px + panelW - 6, py + 32, 0x4400CCFF);

        // ── 4 ряда: название + бар слева, слот справа ────────────
        int barAreaX = px + 8;
        int barAreaW = 150;

        for (int i = 0; i < 4; i++) {
            // Реальная экранная Y-координата слота
            int slotScreenX = leftPos  + SLOT_X;
            int slotScreenY = topPos   + SLOT_START_Y + i * SLOT_STEP;

            // Центрируем текст и бар по вертикали слота (16px высота слота)
            int labelY = slotScreenY + 1;
            int barY   = slotScreenY + 9;

            int oxygen = menu.getSlotOxygen(i);
            boolean has = oxygen >= 0;

            // Название
            g.drawString(font, SLOT_NAMES[i], barAreaX, labelY, 0xFF00CCFF, false);

            // Бар
            int barW = barAreaW;
            g.fill(barAreaX, barY, barAreaX + barW, barY + 6, 0xFF0A1A1A);
            g.fill(barAreaX, barY, barAreaX + barW, barY + 1, 0xFF1A3344);
            g.fill(barAreaX, barY + 5, barAreaX + barW, barY + 6, 0xFF1A3344);

            if (has) {
                int filled   = (int)(barW * oxygen / 100f);
                int barColor = oxygen > 60 ? 0xFF00CCFF
                        : oxygen > 30 ? 0xFFFFAA00 : 0xFFFF2200;
                if (filled > 0)
                    g.fill(barAreaX, barY, barAreaX + filled, barY + 6, barColor);

                String pct = oxygen + "%";
                g.drawString(font, pct,
                        barAreaX + barW - font.width(pct), labelY,
                        0xFFFFFFFF, false);
            } else {
                g.drawString(font, "EMPTY",
                        barAreaX + barW - font.width("EMPTY"), labelY,
                        0xFF555555, false);
            }

            // Рамка вокруг слота — точно по границе слота (16x16)
            // Слот рисуется с leftPos+slotX, topPos+slotY, размер 16x16
            int sx = slotScreenX - 1;
            int sy = slotScreenY - 1;
            int slotBorder = has
                    ? (oxygen > 60 ? 0xFF00CCFF : oxygen > 30 ? 0xFFFFAA00 : 0xFFFF2200)
                    : 0xFF334455;
            g.fill(sx,      sy,      sx + 18, sy + 1,  slotBorder); // верх
            g.fill(sx,      sy + 17, sx + 18, sy + 18, slotBorder); // низ
            g.fill(sx,      sy,      sx + 1,  sy + 18, slotBorder); // лево
            g.fill(sx + 17, sy,      sx + 18, sy + 18, slotBorder); // право

            // Разделитель
            if (i < 3)
                g.fill(px + 6, slotScreenY + 18, px + panelW - 6, slotScreenY + 19, 0x2200CCFF);
        }

        // ── Разделитель над инвентарём ───────────────────────────
        int divY = topPos + SLOT_START_Y + 4 * SLOT_STEP + 4;
        g.fill(px + 6, divY, px + panelW - 6, divY + 1, 0x4400CCFF);
        g.drawString(font, "§7Place suit pieces in slots →",
                px + 8, divY + 4, 0xFF555555, false);

        super.render(g, mouseX, mouseY, delta);
        renderTooltip(g, mouseX, mouseY);
    }
}