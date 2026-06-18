package com.thaloria.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.thaloria.network.SyncPressureZonesPacket;
import com.thaloria.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class PressureGogglesRenderer {

    private static final List<SyncPressureZonesPacket.ZoneBox> ZONES = new ArrayList<>();

    public static void updateZones(List<SyncPressureZonesPacket.ZoneBox> zones) {
        ZONES.clear();
        ZONES.addAll(zones);
    }

    private static boolean isWearingGoggles(Player player) {
        ItemStack main   = player.getMainHandItem();
        ItemStack off    = player.getOffhandItem();
        ItemStack helmet = player.getInventory().armor.get(3);
        return main.is(ModItems.PRESSURE_GOGGLES.get())
                || off.is(ModItems.PRESSURE_GOGGLES.get())
                || helmet.is(ModItems.PRESSURE_GOGGLES.get());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!isWearingGoggles(mc.player)) return;
        if (ZONES.isEmpty()) return;

        Vec3 cam        = mc.gameRenderer.getMainCamera().getPosition();
        BlockPos playerPos = mc.player.blockPosition();
        PoseStack poseStack = event.getPoseStack();

        // Радиус отображения воздушных блоков вокруг игрока
        int renderRadius = 12;

        long now = System.currentTimeMillis();
        float pulse     = (float)(Math.sin(now * 0.003) * 0.06 + 0.14);
        float fillAlpha = Math.max(0.04f, pulse);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());

        for (SyncPressureZonesPacket.ZoneBox zone : ZONES) {
            // Цвет по давлению зоны
            float r, g, b;
            if (zone.pressure >= 80f) {
                r = 0.0f; g = 1.0f; b = 0.4f;   // зелёный
            } else if (zone.pressure >= 40f) {
                r = 1.0f; g = 0.8f; b = 0.0f;   // жёлтый
            } else {
                r = 1.0f; g = 0.2f; b = 0.0f;   // красный
            }

            // Перебираем воздушные блоки в радиусе вокруг игрока
            // которые попадают внутрь AABB зоны (на 1 блок внутри стен)
            for (int dx = -renderRadius; dx <= renderRadius; dx++) {
                for (int dy = -renderRadius; dy <= renderRadius; dy++) {
                    for (int dz = -renderRadius; dz <= renderRadius; dz++) {
                        BlockPos pos = playerPos.offset(dx, dy, dz);

                        // Только внутри AABB зоны (на 1 блок от стен)
                        if (pos.getX() <= zone.minX || pos.getX() >= zone.maxX) continue;
                        if (pos.getY() <= zone.minY || pos.getY() >= zone.maxY) continue;
                        if (pos.getZ() <= zone.minZ || pos.getZ() >= zone.maxZ) continue;

                        // Только воздушные блоки
                        if (!mc.level.getBlockState(pos).isAir()) continue;

                        float x1 = (float)(pos.getX() - cam.x);
                        float y1 = (float)(pos.getY() - cam.y);
                        float z1 = (float)(pos.getZ() - cam.z);
                        float x2 = x1 + 1f, y2 = y1 + 1f, z2 = z1 + 1f;

                        var pose = poseStack.last().pose();

                        // Заливка
                        builder.vertex(pose,x1,y1,z1).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x2,y1,z1).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x2,y1,z2).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x1,y1,z2).color(r,g,b,fillAlpha).endVertex();

                        builder.vertex(pose,x1,y2,z1).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x1,y2,z2).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x2,y2,z2).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x2,y2,z1).color(r,g,b,fillAlpha).endVertex();

                        builder.vertex(pose,x1,y1,z1).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x1,y2,z1).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x2,y2,z1).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x2,y1,z1).color(r,g,b,fillAlpha).endVertex();

                        builder.vertex(pose,x1,y1,z2).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x2,y1,z2).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x2,y2,z2).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x1,y2,z2).color(r,g,b,fillAlpha).endVertex();

                        builder.vertex(pose,x1,y1,z1).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x1,y1,z2).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x1,y2,z2).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x1,y2,z1).color(r,g,b,fillAlpha).endVertex();

                        builder.vertex(pose,x2,y1,z1).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x2,y2,z1).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x2,y2,z2).color(r,g,b,fillAlpha).endVertex();
                        builder.vertex(pose,x2,y1,z2).color(r,g,b,fillAlpha).endVertex();
                    }
                }
            }
        }

        Tesselator.getInstance().end();
        bufferSource.endBatch(RenderType.lines());

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}