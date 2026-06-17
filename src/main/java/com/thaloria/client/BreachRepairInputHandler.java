package com.thaloria.client;

import com.thaloria.client.render.BreachOutlineRenderer;
import com.thaloria.network.ModNetwork;
import com.thaloria.network.RepairBreachPacket;
import com.thaloria.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class BreachRepairInputHandler {

    private static final double MAX_DISTANCE = 8.0;
    private static final double MIN_DOT      = 0.90;

    @SubscribeEvent
    public static void onMouseClick(InputEvent.MouseButton event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;

        // Только если режим сканирования активен и аутлайны видны
        if (!BreachOutlineRenderer.scanActive) return;
        if (BreachOutlineRenderer.BREACHES.isEmpty()) return;

        // Только если в руке блок купола
        if (!mc.player.getMainHandItem().is(
                ModBlocks.DOME_GLASS.get().asItem())) return;

        Vec3 eye  = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();

        BlockPos bestBreach = null;
        double   bestDot    = MIN_DOT;

        for (BlockPos breach : BreachOutlineRenderer.BREACHES.keySet()) {
            Vec3 center   = Vec3.atCenterOf(breach);
            double distance = center.distanceTo(eye);
            if (distance > MAX_DISTANCE) continue;

            Vec3 dir = center.subtract(eye).normalize();
            double dot = dir.dot(look);

            if (dot > bestDot) {
                bestDot    = dot;
                bestBreach = breach;
            }
        }

        if (bestBreach == null) return;

        ModNetwork.CHANNEL.sendToServer(new RepairBreachPacket(bestBreach));
        BreachOutlineRenderer.BREACHES.remove(bestBreach);

        // Если починили последнюю брешь — выключаем режим
        if (BreachOutlineRenderer.BREACHES.isEmpty()) {
            BreachOutlineRenderer.scanActive = false;
        }

        event.setCanceled(true);
    }
}