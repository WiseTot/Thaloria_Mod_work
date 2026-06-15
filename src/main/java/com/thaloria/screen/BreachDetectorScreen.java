package com.thaloria.screen;

import com.thaloria.network.BreachDetectorActionPacket;
import com.thaloria.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class BreachDetectorScreen extends Screen {

    private final BlockPos detectorPos;
    private final boolean isPowered;
    private boolean autoMonitor;
    private final int breachCount;
    private final int shellCount;
    private final float pressure;
    private final boolean hasZone;

    public BreachDetectorScreen(BlockPos pos, boolean isPowered, boolean autoMonitor,
                                int breachCount, int shellCount,
                                float pressure, boolean hasZone) {
        super(Component.literal("Breach Detector"));
        this.detectorPos = pos;
        this.isPowered = isPowered;
        this.autoMonitor = autoMonitor;
        this.breachCount = breachCount;
        this.shellCount = shellCount;
        this.pressure = pressure;
        this.hasZone = hasZone;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int panelY = height / 2 - 80;

        // Кнопка 1 — сканировать и показать бреши
        addRenderableWidget(Button.builder(
                        Component.literal("§eScan Breaches"),
                        btn -> {
                            ModNetwork.CHANNEL.sendToServer(
                                    new BreachDetectorActionPacket(
                                            detectorPos,
                                            BreachDetectorActionPacket.Action.SCAN_BREACHES
                                    )
                            );
                            onClose();
                        })
                .pos(cx - 60, panelY + 100)
                .size(120, 20)
                .build()
        );

        // Кнопка 2 — авто мониторинг вкл/выкл
        addRenderableWidget(Button.builder(
                        Component.literal(autoMonitor
                                ? "§aAuto-Monitor: ON"
                                : "§7Auto-Monitor: OFF"),
                        btn -> {
                            autoMonitor = !autoMonitor;
                            btn.setMessage(Component.literal(autoMonitor
                                    ? "§aAuto-Monitor: ON"
                                    : "§7Auto-Monitor: OFF"));
                            ModNetwork.CHANNEL.sendToServer(
                                    new BreachDetectorActionPacket(
                                            detectorPos,
                                            BreachDetectorActionPacket.Action.TOGGLE_AUTO_MONITOR
                                    )
                            );
                        })
                .pos(cx - 60, panelY + 125)
                .size(120, 20)
                .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);

        int cx = width / 2;
        int panelW = 210;
        int panelH = 165;
        int px = cx - panelW / 2;
        int py = height / 2 - 80;

        // Фон
        graphics.fill(px, py, px + panelW, py + panelH, 0xDD050C14);
        // Рамка — красная если есть бреши, голубая если всё ок
        int borderColor = breachCount > 0 ? 0xFFFF3300 : 0xFF00FFCC;
        graphics.fill(px,            py,            px + panelW, py + 1,      borderColor);
        graphics.fill(px,            py + panelH-1, px + panelW, py + panelH, borderColor);
        graphics.fill(px,            py,            px + 1,      py + panelH, borderColor);
        graphics.fill(px + panelW-1, py,            px + panelW, py + panelH, borderColor);

        // Заголовок
        graphics.drawCenteredString(font, "BREACH DETECTOR",
                cx, py + 6, borderColor);

        // Разделитель
        graphics.fill(px + 6, py + 16, px + panelW - 6, py + 17, 0x8800FFCC);

        // Статус питания
        String powerStr = isPowered ? "§aPOWERED" : "§cNO POWER";
        graphics.drawString(font, "STATUS: " + powerStr,
                px + 8, py + 22, 0xFFCCCCCC, false);

        // Статус зоны
        String zoneStr = hasZone ? "§aZONE LINKED" : "§cNO ZONE";
        graphics.drawString(font, "ZONE: " + zoneStr,
                px + 8, py + 34, 0xFFCCCCCC, false);

        // Разделитель
        graphics.fill(px + 6, py + 44, px + panelW - 6, py + 45, 0x4400FFCC);

        // Данные купола
        graphics.drawString(font, "Shell blocks: " + shellCount,
                px + 8, py + 50, 0xFFAAAAAA, false);

        int breachColor = breachCount > 0 ? 0xFFFF4444 : 0xFF44FF88;
        String breachStr = breachCount > 0
                ? "§cBREACHES: " + breachCount + " ⚠"
                : "§aINTEGRITY: OK";
        graphics.drawString(font, breachStr, px + 8, py + 62, breachColor, false);

        // Давление купола
        graphics.fill(px + 6, py + 74, px + panelW - 6, py + 75, 0x4400FFCC);
        graphics.drawString(font, "DOME PRESSURE", px + 8, py + 79, 0xFF00FFCC, false);
        graphics.drawString(font, (int)pressure + "%",
                px + panelW - 8 - font.width((int)pressure + "%"),
                py + 79, 0xFFFFFFFF, false);

        int barW = panelW - 16;
        int barY = py + 89;
        graphics.fill(px + 8, barY, px + 8 + barW, barY + 8, 0xFF0A1A1A);
        int barColor = pressure >= 80 ? 0xFF00FF88
                : pressure >= 60 ? 0xFFAAFF00
                : pressure >= 40 ? 0xFFFFAA00
                : 0xFFFF2200;
        int filled = (int)(barW * pressure / 100f);
        if (filled > 0) {
            graphics.fill(px + 8, barY, px + 8 + filled, barY + 8, barColor);
        }

        // Разделитель перед кнопками
        graphics.fill(px + 6, py + 98, px + panelW - 6, py + 99, 0x4400FFCC);

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}