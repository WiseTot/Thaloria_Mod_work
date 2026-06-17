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

    public static final Map<BlockPos, Long> BREACHES = new HashMap<>();
    public static final long BREACH_DURATION_MS = 30_000L;

    public static BlockPos detectorPos = null;
    public static final double MAX_DISTANCE = 50.0;

    // true только пока аутлайны активны (после Scan Breaches)
    public static boolean scanActive = false;

    public static void addBreach(BlockPos pos) {
        BREACHES.put(pos, System.currentTimeMillis());
        scanActive = true;
    }

    public static void clearBreaches() {
        BREACHES.clear();
        scanActive = false;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (BREACHES.isEmpty()) {
            scanActive = false; // все аутлайны исчезли — режим выключаем
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        long now = System.currentTimeMillis();

        if (detectorPos != null) {
            double distToDetector = mc.player.blockPosition().distSqr(detectorPos);
            if (distToDetector > MAX_DISTANCE * MAX_DISTANCE) {
                BREACHES.clear();
                scanActive = false;
                detectorPos = null;
                return;
            }
        }

        PoseStack poseStack = event.getPoseStack();
        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;

        double time = now * 0.002;
        float cycle = (float)((Math.sin(time) + 1.0) / 2.0);
        float r = 1.0f;
        float g = cycle * 0.65f;
        float b = 0.0f;

        Iterator<Map.Entry<BlockPos, Long>> it = BREACHES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> entry = it.next();
            BlockPos pos = entry.getKey();
            long addedTime = entry.getValue();

            if (now - addedTime > BREACH_DURATION_MS) {
                it.remove();
                continue;
            }

            if (mc.level.getBlockState(pos).is(ModBlocks.DOME_GLASS.get())) {
                it.remove();
                continue;
            }

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

            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            float fr = r, fg = g, fb = b, fa = alpha;

            builder.vertex(poseStack.last().pose(), (float)x1,(float)y1,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y1,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y1,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y1,(float)z2).color(fr,fg,fb,fa).endVertex();

            builder.vertex(poseStack.last().pose(), (float)x1,(float)y2,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y2,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y2,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y2,(float)z1).color(fr,fg,fb,fa).endVertex();

            builder.vertex(poseStack.last().pose(), (float)x1,(float)y1,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y2,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y2,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y1,(float)z1).color(fr,fg,fb,fa).endVertex();

            builder.vertex(poseStack.last().pose(), (float)x1,(float)y1,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y1,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y2,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y2,(float)z2).color(fr,fg,fb,fa).endVertex();

            builder.vertex(poseStack.last().pose(), (float)x1,(float)y1,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y1,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y2,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x1,(float)y2,(float)z1).color(fr,fg,fb,fa).endVertex();

            builder.vertex(poseStack.last().pose(), (float)x2,(float)y1,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y2,(float)z1).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y2,(float)z2).color(fr,fg,fb,fa).endVertex();
            builder.vertex(poseStack.last().pose(), (float)x2,(float)y1,(float)z2).color(fr,fg,fb,fa).endVertex();

            Tesselator.getInstance().end();

            MultiBufferSource.BufferSource bufferSource =
                    mc.renderBuffers().bufferSource();
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

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}