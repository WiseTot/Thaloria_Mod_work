package com.thaloria.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.thaloria.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BreachOutlineRenderer {

    // Позиция бреши → время когда она была добавлена (в миллисекундах)
    public static final Map<BlockPos, Long> BREACHES = new HashMap<>();

    // Сколько миллисекунд показывать брешь (30 секунд)
    public static final long BREACH_DURATION_MS = 30_000L;

    // Максимальное расстояние от детектора (50 блоков)
    // Хранится отдельно — устанавливается BreachDetector
    public static BlockPos detectorPos = null;
    public static final double MAX_DISTANCE = 50.0;

    // Добавить брешь с текущим временем
    public static void addBreach(BlockPos pos) {
        BREACHES.put(pos, System.currentTimeMillis());
    }

    // Очистить все бреши
    public static void clearBreaches() {
        BREACHES.clear();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (BREACHES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        long now = System.currentTimeMillis();

        // Проверяем расстояние от детектора
        if (detectorPos != null) {
            double distToDetector = mc.player.blockPosition().distSqr(detectorPos);
            if (distToDetector > MAX_DISTANCE * MAX_DISTANCE) {
                BREACHES.clear();
                detectorPos = null;
                return;
            }
        }

        PoseStack poseStack = event.getPoseStack();
        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;

        // Цикл цветов: красный → оранжевый → жёлтый → оранжевый → красный
        double time = now * 0.002;
        float cycle = (float)((Math.sin(time) + 1.0) / 2.0); // 0.0 - 1.0

        // Красный всегда 1.0, зелёный меняется 0.0-0.65 (даёт красный-оранжевый-жёлтый)
        float r = 1.0f;
        float g = cycle * 0.65f;
        float b = 0.0f;

        Iterator<Map.Entry<BlockPos, Long>> it = BREACHES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> entry = it.next();
            BlockPos pos = entry.getKey();
            long addedTime = entry.getValue();

            // Удаляем если время вышло
            if (now - addedTime > BREACH_DURATION_MS) {
                it.remove();
                continue;
            }

            // Удаляем если блок купола восстановлен
            if (mc.level.getBlockState(pos).is(ModBlocks.DOME_GLASS.get())) {
                it.remove();
                continue;
            }

            // Альфа мигает в последние 5 секунд — предупреждение об исчезновении
            long remaining = BREACH_DURATION_MS - (now - addedTime);
            float alpha;
            if (remaining < 5000) {
                alpha = (float)(Math.sin(now * 0.01) * 0.3 + 0.4);
            } else {
                alpha = 0.35f;
            }

            double x1 = pos.getX() - camX;
            double y1 = pos.getY() - camY;
            double z1 = pos.getZ() - camZ;
            double x2 = x1 + 1.0;
            double y2 = y1 + 1.0;
            double z2 = z1 + 1.0;

            // Слой 1: заполненный полупрозрачный куб — виден СКВОЗЬ блоки
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            // 6 граней куба
            float fr = r, fg = g, fb = b, fa = alpha;

            // Нижняя грань
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y1,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y1,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y1,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y1,(float)z2).color(fr,fg,fb,fa).endVertex();
            // Верхняя грань
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y2,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y2,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y2,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y2,(float)z1).color(fr,fg,fb,fa).endVertex();
            // Северная грань
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y1,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y2,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y2,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y1,(float)z1).color(fr,fg,fb,fa).endVertex();
            // Южная грань
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y1,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y1,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y2,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y2,(float)z2).color(fr,fg,fb,fa).endVertex();
            // Западная грань
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y1,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y1,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y2,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y2,(float)z1).color(fr,fg,fb,fa).endVertex();
            // Восточная грань
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y1,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y2,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y2,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y1,(float)z2).color(fr,fg,fb,fa).endVertex();

            Tesselator.getInstance().end();

            // Слой 2: рамка поверх куба — тоже без depth test
            MultiBufferSource.BufferSource bufferSource =
                    mc.renderBuffers().bufferSource();

            // Чуть больше чем куб чтобы рамка была видна
            double e = 0.002;
            LevelRenderer.renderLineBox(
                    poseStack,
                    bufferSource.getBuffer(RenderType.lines()),
                    x1 - e, y1 - e, z1 - e,
                    x2 + e, y2 + e, z2 + e,
                    r, g + 0.2f, b, 1.0f
            );

            bufferSource.endBatch(RenderType.lines());
        }

        // Восстанавливаем состояние рендера
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}