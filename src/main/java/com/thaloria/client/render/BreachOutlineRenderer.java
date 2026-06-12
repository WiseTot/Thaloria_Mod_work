package com.thaloria.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.thaloria.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BreachOutlineRenderer {

    public static final Set<BlockPos> BREACHES = new HashSet<>();

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (BREACHES == null || BREACHES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        double time = System.currentTimeMillis() * 0.003;
        float pulse = (float)((Math.sin(time) + 1) / 2);

        float r = 1.0f;
        float g = pulse;
        float b = 0.0f;

        for (Iterator<BlockPos> it = BREACHES.iterator(); it.hasNext();) {

            BlockPos pos = it.next();

            if (mc.level.getBlockState(pos).is(ModBlocks.DOME_GLASS.get())) {
                it.remove();
                continue;
            }

            double x1 = pos.getX() - camX;
            double y1 = pos.getY() - camY;
            double z1 = pos.getZ() - camZ;

            double x2 = x1 + 1;
            double y2 = y1 + 1;
            double z2 = z1 + 1;

            LevelRenderer.renderLineBox(
                    poseStack,
                    buffer.getBuffer(RenderType.lines()),
                    x1, y1, z1,
                    x2, y2, z2,
                    r, g, b, 1f
            );

            LevelRenderer.renderLineBox(
                    poseStack,
                    buffer.getBuffer(RenderType.lines()),
                    x1-0.02, y1-0.02, z1-0.02,
                    x2+0.02, y2+0.02, z2+0.02,
                    r, g, b, 1f
            );
        }

        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        buffer.endBatch();
    }
}