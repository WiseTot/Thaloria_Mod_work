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

    // Данные которые обновляются в реальном времени
    private boolean isPowered;
    private boolean isScanning;
    private int scanProgress;
    private int shellCount;
    private int breachCount;
    private float pressure;
    private float pressureDelta;
    private int filterCount;

    private RadiusSlider radiusSlider;

    // Тик для запроса обновления данных
    private int updateTick = 0;

    public AtmosphereFilterScreen(BlockPos pos, int radius, boolean isPowered,
                                  boolean isScanning, int scanProgress,
                                  int shellCount, int breachCount,
                                  float pressure, float pressureDelta, int filterCount) {
        super(Component.literal("Atmosphere Filter"));
        this.filterPos = pos;
        this.scanRadius = radius;
        this.isPowered = isPowered;
        this.isScanning = isScanning;
        this.scanProgress = scanProgress;
        this.shellCount = shellCount;
        this.breachCount = breachCount;
        this.pressure = pressure;
        this.pressureDelta = pressureDelta;
        this.filterCount = filterCount;
    }

    // Вызывается сервером для обновления данных пока экран открыт
    public void updateData(boolean isPowered, boolean isScanning, int scanProgress,
                           int shellCount, int breachCount, float pressure,
                           float pressureDelta, int filterCount) {
        this.isPowered = isPowered;
        this.isScanning = isScanning;
        this.scanProgress = scanProgress;
        this.shellCount = shellCount;
        this.breachCount = breachCount;
        this.pressure = pressure;
        this.pressureDelta = pressureDelta;
        this.filterCount = filterCount;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int panelY = height / 2 - 95;

        // Ползунок радиуса
        radiusSlider = new RadiusSlider(cx - 75, panelY + 130, 130, 16, scanRadius);
        addRenderableWidget(radiusSlider);

        // Кнопка Apply
        addRenderableWidget(Button.builder(
                        Component.literal("Apply"),
                        btn -> {
                            scanRadius = radiusSlider.getRadiusValue();
                            ModNetwork.CHANNEL.sendToServer(
                                    new FilterRadiusPacket(filterPos, scanRadius));
                        })
                .pos(cx + 58, panelY + 130)
                .size(36, 16)
                .build()
        );

        // Кнопка SCAN
        addRenderableWidget(Button.builder(
                        Component.literal(isScanning ? "Scanning..." : "SCAN"),
                        btn -> {
                            if (!isScanning && isPowered) {
                                ModNetwork.CHANNEL.sendToServer(
                                        new FilterScanPacket(filterPos));
                            }
                        })
                .pos(cx - 70, panelY + 152)
                .size(60, 16)
                .build()
        );

        // Кнопка Show Breaches ON/OFF — только если есть бреши
        if (breachCount > 0) {
            boolean showBreaches;
            addRenderableWidget(Button.builder(
                            Component.literal(showBreaches ? "§aBreaches: ON" : "§cBreaches: OFF"),
                            btn -> {
                                showBreaches = !showBreaches;
                                btn.setMessage(Component.literal(
                                        showBreaches ? "§aBreaches: ON" : "§cBreaches: OFF"));
                                if (showBreaches) {
                                    ModNetwork.CHANNEL.sendToServer(
                                            new RequestShowBreachesPacket(filterPos));
                                }
                            })
                    .pos(cx - 5, panelY + 152)
                    .size(75, 16)
                    .build()
            );

            // Кнопка Teleport to Breach
            addRenderableWidget(Button.builder(
                            Component.literal("§eTeleport"),
                            btn -> {
                                ModNetwork.CHANNEL.sendToServer(
                                        new TeleportToBreachPacket(filterPos));
                                onClose();
                            })
                    .pos(cx + 74, panelY + 152)
                    .size(56, 16)
                    .build()
            );
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateTick++;
        if (updateTick >= 10) {
            updateTick = 0;
            // Запрашиваем свежие данные у сервера
            ModNetwork.CHANNEL.sendToServer(new RequestFilterDataPacket(filterPos));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);

        int cx = width / 2;
        int panelW = 220;
        int panelH = 185;
        int px = cx - panelW / 2;
        int py = height / 2 - 95;

        // Фон
        graphics.fill(px, py, px + panelW, py + panelH, 0xDD050C14);
        // Рамка
        graphics.fill(px,           py,            px + panelW, py + 1,      0xFF00FFCC);
        graphics.fill(px,           py + panelH-1, px + panelW, py + panelH, 0xFF00FFCC);
        graphics.fill(px,           py,            px + 1,      py + panelH, 0xFF00FFCC);
        graphics.fill(px + panelW-1,py,            px + panelW, py + panelH, 0xFF00FFCC);

        // Заголовок
        graphics.drawCenteredString(font, "ATMOSPHERE FILTER MK-I",
                cx, py + 5, 0xFF00FFCC);

        // Разделитель под заголовком
        graphics.fill(px + 6, py + 15, px + panelW - 6, py + 16, 0x8800FFCC);

        // Статус
        String powerStr = isPowered ? "§aPOWERED" : "§cNO POWER";
        graphics.drawString(font, "STATUS: " + powerStr, px + 8, py + 20, 0xFFCCCCCC, false);

        // Разделитель
        graphics.fill(px + 6, py + 30, px + panelW - 6, py + 31, 0x4400FFCC);

        if (isScanning) {
            // Прогресс сканирования
            graphics.drawCenteredString(font, "§bSCANNING...", cx, py + 36, 0xFFFFFFFF);
            int barW = panelW - 20;
            graphics.fill(px + 10, py + 48, px + 10 + barW, py + 58, 0xFF0A1A1A);
            graphics.fill(px + 10, py + 48, px + 10 + (int)(barW * scanProgress / 100f),
                    py + 58, 0xFF00FFCC);
            graphics.drawCenteredString(font, scanProgress + "%", cx, py + 49, 0xFFFFFFFF);
        } else {
            // Строка 1: Shell + Breaches
            graphics.drawString(font, "Shell: " + shellCount,
                    px + 8, py + 35, 0xFFAAAAAA, false);
            int breachColor = breachCount > 0 ? 0xFFFF4444 : 0xFF44FF88;
            graphics.drawString(font, "Breaches: " + breachCount,
                    px + 8, py + 47, breachColor, false);

            // Строка 2: Filters + Delta
            graphics.drawString(font, "Filters: " + filterCount,
                    px + 8, py + 59, 0xFFAAAAAA, false);
            int deltaColor = pressureDelta >= 0 ? 0xFF44FF88 : 0xFFFF4444;
            String deltaStr = (pressureDelta >= 0 ? "+" : "")
                    + String.format("%.2f", pressureDelta) + "%/s";
            graphics.drawString(font, "Delta: " + deltaStr,
                    px + 8, py + 71, deltaColor, false);

            // Разделитель перед баром давления
            graphics.fill(px + 6, py + 83, px + panelW - 6, py + 84, 0x4400FFCC);

            // PRESSURE бар
            graphics.drawString(font, "PRESSURE", px + 8, py + 88, 0xFF00FFCC, false);
            graphics.drawString(font, (int)pressure + "%",
                    px + panelW - 8 - font.width((int)pressure + "%"),
                    py + 88, 0xFFFFFFFF, false);

            int barW = panelW - 16;
            int barY = py + 100;
            // Фон бара
            graphics.fill(px + 8, barY, px + 8 + barW, barY + 12, 0xFF0A1A1A);
            // Граница бара
            graphics.fill(px + 8, barY, px + 8 + barW, barY + 1, 0xFF334444);
            graphics.fill(px + 8, barY + 11, px + 8 + barW, barY + 12, 0xFF334444);

            // Цвет бара по уровню давления
            int barColor;
            if (pressure >= 80)      barColor = 0xFF00FF88;
            else if (pressure >= 60) barColor = 0xFFAAFF00;
            else if (pressure >= 40) barColor = 0xFFFFAA00;
            else                     barColor = 0xFFFF2200;

            int filled = (int)(barW * pressure / 100f);
            if (filled > 0) {
                graphics.fill(px + 8, barY, px + 8 + filled, barY + 12, barColor);
            }

            // Метки 25/50/75%
            for (int pct : new int[]{25, 50, 75}) {
                int mx = px + 8 + (int)(barW * pct / 100f);
                graphics.fill(mx, barY, mx + 1, barY + 12, 0x66FFFFFF);
            }
        }

        // Разделитель перед ползунком
        graphics.fill(px + 6, py + 118, px + panelW - 6, py + 119, 0x4400FFCC);

        // Метка ползунка
        int currentRadius = radiusSlider != null ? radiusSlider.getRadiusValue() : scanRadius;
        graphics.drawString(font, "Scan radius: " + currentRadius,
                px + 8, py + 122, 0xFF00FFCC, false);

        super.render(graphics, mouseX, mouseY, delta);
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

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("" + getRadiusValue()));
        }

        @Override
        protected void applyValue() {}
    }
}