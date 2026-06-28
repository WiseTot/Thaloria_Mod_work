package com.thaloria.screen;

import com.thaloria.network.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class AtmosphereFilterScreen extends Screen {

    private final BlockPos filterPos;
    private int scanRadius;
    private boolean isPowered;
    private boolean isScanning;
    private int scanProgress;
    private int shellCount;
    private int breachCount;
    private float pressure;
    private float pressureDelta;
    private int filterCount;
    private boolean isSealed;

    private RadiusSlider radiusSlider;
    private int updateTick = 0;

    public AtmosphereFilterScreen(BlockPos pos, int radius, boolean isPowered,
                                  boolean isScanning, int scanProgress,
                                  int shellCount, int breachCount,
                                  float pressure, float pressureDelta,
                                  int filterCount, boolean isSealed) {
        super(Component.literal("Atmosphere Filter"));
        this.filterPos     = pos;
        this.scanRadius    = radius;
        this.isPowered     = isPowered;
        this.isScanning    = isScanning;
        this.scanProgress  = scanProgress;
        this.shellCount    = shellCount;
        this.breachCount   = breachCount;
        this.pressure      = pressure;
        this.pressureDelta = pressureDelta;
        this.filterCount   = filterCount;
        this.isSealed      = isSealed;
    }

    public void updateData(boolean isPowered, boolean isScanning, int scanProgress,
                           int shellCount, int breachCount, float pressure,
                           float pressureDelta, int filterCount, boolean isSealed) {
        this.isPowered     = isPowered;
        this.isScanning    = isScanning;
        this.scanProgress  = scanProgress;
        this.shellCount    = shellCount;
        this.breachCount   = breachCount;
        this.pressure      = pressure;
        this.pressureDelta = pressureDelta;
        this.filterCount   = filterCount;
        this.isSealed      = isSealed;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int panelY = height / 2 - 100;

        radiusSlider = new RadiusSlider(cx - 75, panelY + 148, 130, 16, scanRadius);
        addRenderableWidget(radiusSlider);

        addRenderableWidget(Button.builder(
                        Component.literal("Apply"),
                        btn -> {
                            scanRadius = radiusSlider.getRadiusValue();
                            ModNetwork.CHANNEL.sendToServer(
                                    new FilterRadiusPacket(filterPos, scanRadius));
                        })
                .pos(cx + 58, panelY + 148)
                .size(36, 16)
                .build()
        );

        addRenderableWidget(Button.builder(
                        Component.literal(isScanning ? "Scanning..." : "SCAN"),
                        btn -> {
                            if (!isScanning && isPowered) {
                                ModNetwork.CHANNEL.sendToServer(
                                        new FilterScanPacket(filterPos));
                            }
                        })
                .pos(cx - 55, panelY + 170)
                .size(50, 16)
                .build()
        );

        addRenderableWidget(Button.builder(
                        Component.literal("§eUpdate Baseline"),
                        btn -> {
                            if (!isScanning && isPowered) {
                                ModNetwork.CHANNEL.sendToServer(
                                        new UpdateBaselinePacket(filterPos));
                                onClose();
                            }
                        })
                .pos(cx - 1, panelY + 170)
                .size(90, 16)
                .build()
        );
    }

    @Override
    public void tick() {
        super.tick();
        updateTick++;
        if (updateTick >= 10) {
            updateTick = 0;
            ModNetwork.CHANNEL.sendToServer(new RequestFilterDataPacket(filterPos));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g);

        int cx     = width / 2;
        int panelW = 220;
        int panelH = 200;
        int px     = cx - panelW / 2;
        int py     = height / 2 - 100;

        // ── Фон ──────────────────────────────────────────────────
        g.fill(px, py, px + panelW, py + panelH, 0xDD050C14);

        // ── Рамка ────────────────────────────────────────────────
        int borderColor = isPowered ? 0xFF00FFCC : 0xFFFF3300;
        g.fill(px,            py,            px + panelW, py + 1,      borderColor);
        g.fill(px,            py + panelH-1, px + panelW, py + panelH, borderColor);
        g.fill(px,            py,            px + 1,      py + panelH, borderColor);
        g.fill(px + panelW-1, py,            px + panelW, py + panelH, borderColor);

        // ── Заголовок ────────────────────────────────────────────
        g.drawCenteredString(font, "ATMOSPHERE FILTER MK-I",
                cx, py + 5, 0xFF00FFCC);
        g.fill(px + 6, py + 15, px + panelW - 6, py + 16, 0x8800FFCC);

        // ── Статус питания ────────────────────────────────────────
        String powerStr = isPowered ? "§aPOWERED" : "§cNO POWER";
        g.drawString(font, "STATUS: " + powerStr, px + 8, py + 20, 0xFFCCCCCC, false);
        g.fill(px + 6, py + 30, px + panelW - 6, py + 31, 0x4400FFCC);

        if (isScanning) {
            // ── Прогресс сканирования ─────────────────────────────
            g.drawCenteredString(font, "§bSCANNING...", cx, py + 36, 0xFFFFFFFF);
            int barW = panelW - 20;
            g.fill(px + 10, py + 50, px + 10 + barW, py + 62, 0xFF0A1A1A);
            g.fill(px + 10, py + 50,
                    px + 10 + (int)(barW * scanProgress / 100f), py + 62, 0xFF00FFCC);
            g.drawCenteredString(font, scanProgress + "%", cx, py + 52, 0xFFFFFFFF);

        } else if (!isSealed && shellCount > 0) {
            // ── Купол не герметичен — только предупреждение ──────
            long now = System.currentTimeMillis();
            boolean blink = (now / 500) % 2 == 0;
            int warnColor = blink ? 0xFFFF3333 : 0xFFAA1111;

            g.fill(px + 6, py + 35, px + panelW - 6, py + 36, 0x44FF0000);
            g.drawCenteredString(font, "! DOME NOT SEALED !", cx, py + 42, warnColor);
            g.fill(px + 6, py + 55, px + panelW - 6, py + 56, 0x44FF0000);

            g.drawCenteredString(font, "§7Dome is not airtight.",
                    cx, py + 62, 0xFFAAAAAA);
            g.drawCenteredString(font, "§7Pressure will not build up.",
                    cx, py + 74, 0xFFAAAAAA);
            g.drawCenteredString(font, "§7Close all openings and",
                    cx, py + 86, 0xFFAAAAAA);
            g.drawCenteredString(font, "§7press §eUpdate Baseline §7to rescan.",
                    cx, py + 98, 0xFFAAAAAA);

        } else {
            // ── Купол герметичен — полный UI ─────────────────────
            g.drawString(font, "Shell: " + shellCount,
                    px + 8, py + 35, 0xFFAAAAAA, false);

            int breachColor = breachCount > 0 ? 0xFFFF4444 : 0xFF44FF88;
            g.drawString(font, "Breaches: " + breachCount,
                    px + 8, py + 47, breachColor, false);

            g.drawString(font, "Filters: " + filterCount,
                    px + 8, py + 59, 0xFFAAAAAA, false);

            int deltaColor = pressureDelta >= 0 ? 0xFF44FF88 : 0xFFFF4444;
            String deltaStr = (pressureDelta >= 0 ? "+" : "")
                    + String.format("%.2f", pressureDelta) + "%/s";
            g.drawString(font, "Delta: " + deltaStr,
                    px + 8, py + 71, deltaColor, false);

            g.fill(px + 6, py + 83, px + panelW - 6, py + 84, 0x4400FFCC);

            g.drawString(font, "§aINTEGRITY: OK", px + 8, py + 88, 0xFF44FF88, false);

            g.fill(px + 6, py + 100, px + panelW - 6, py + 101, 0x4400FFCC);

            g.drawString(font, "PRESSURE", px + 8, py + 105, 0xFF00FFCC, false);
            g.drawString(font, (int)pressure + "%",
                    px + panelW - 8 - font.width((int)pressure + "%"),
                    py + 105, 0xFFFFFFFF, false);
            renderPressureBar(g, px, py + 117, panelW);

            g.fill(px + 6, py + 133, px + panelW - 6, py + 134, 0x4400FFCC);
        }

        // ── Разделитель и радиус ──────────────────────────────────
        g.fill(px + 6, py + 136, px + panelW - 6, py + 137, 0x4400FFCC);
        int currentRadius = radiusSlider != null ? radiusSlider.getRadiusValue() : scanRadius;
        g.drawString(font, "Scan radius: " + currentRadius,
                px + 8, py + 140, 0xFF00FFCC, false);

        super.render(g, mouseX, mouseY, delta);
    }

    private void renderPressureBar(GuiGraphics g, int px, int barY, int panelW) {
        int barW = panelW - 16;
        g.fill(px + 8, barY, px + 8 + barW, barY + 12, 0xFF0A1A1A);
        g.fill(px + 8, barY, px + 8 + barW, barY + 1,  0xFF334444);
        g.fill(px + 8, barY + 11, px + 8 + barW, barY + 12, 0xFF334444);

        int barColor;
        if      (pressure >= 80) barColor = 0xFF00FF88;
        else if (pressure >= 60) barColor = 0xFFAAFF00;
        else if (pressure >= 40) barColor = 0xFFFFAA00;
        else                     barColor = 0xFFFF2200;

        int filled = (int)(barW * pressure / 100f);
        if (filled > 0) {
            g.fill(px + 8, barY, px + 8 + filled, barY + 12, barColor);
        }
        for (int pct : new int[]{25, 50, 75}) {
            int mx = px + 8 + (int)(barW * pct / 100f);
            g.fill(mx, barY, mx + 1, barY + 12, 0x66FFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    public static class RadiusSlider
            extends net.minecraft.client.gui.components.AbstractSliderButton {

        public RadiusSlider(int x, int y, int width, int height, int initialValue) {
            super(x, y, width, height,
                    Component.literal("" + initialValue),
                    (initialValue - 5) / 445.0);
            updateMessage();
        }

        public int getRadiusValue() {
            return 5 + (int)(value * 445);
        }

        @Override protected void updateMessage() {
            setMessage(Component.literal("" + getRadiusValue()));
        }

        @Override protected void applyValue() {}
    }
}