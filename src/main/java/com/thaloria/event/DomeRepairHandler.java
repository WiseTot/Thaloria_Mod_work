package com.thaloria.event;

import com.thaloria.block.entity.AtmosphereFilterBlockEntity;
import com.thaloria.client.render.BreachOutlineRenderer;
import com.thaloria.dome.DomeZone;
import com.thaloria.dome.DomeZoneSavedData;
import com.thaloria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.thaloria.ThaloriaMod;

@Mod.EventBusSubscriber(modid = ThaloriaMod.MOD_ID)
public class DomeRepairHandler {

    private static final double MAX_DISTANCE = 6.0;

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;
        ItemStack stack = event.getItemStack();

        if (!stack.is(ModBlocks.DOME_GLASS.get().asItem())) return;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        BlockPos bestBreach = null;
        double bestScore = 0;

        // Ищем брешь на которую смотрит игрок
        for (BlockPos breach : BreachOutlineRenderer.BREACHES) {
            Vec3 center = new Vec3(
                    breach.getX() + 0.5,
                    breach.getY() + 0.5,
                    breach.getZ() + 0.5
            );
            double distance = center.distanceTo(eye);
            if (distance > MAX_DISTANCE) continue;

            Vec3 dir = center.subtract(eye).normalize();
            double dot = dir.dot(look);

            if (dot > bestScore && dot > 0.92) {
                bestScore = dot;
                bestBreach = breach;
            }
        }

        if (bestBreach == null) return;

        // Ставим блок купола
        level.setBlock(bestBreach, ModBlocks.DOME_GLASS.get().defaultBlockState(), 3);
        stack.shrink(1);
        BreachOutlineRenderer.BREACHES.remove(bestBreach);

        // Убираем брешь из всех зон которые её содержат
        DomeZoneSavedData data = DomeZoneSavedData.get(level);
        for (DomeZone zone : data.getAllZones()) {
            if (zone.breaches.remove(bestBreach)) {
                // Брешь была в этой зоне — помечаем как изменённую
                data.setDirty();

                // Если брешей больше нет — запускаем пересканирование
                // чтобы убедиться что купол герметичен
                if (zone.breaches.isEmpty()) {
                    triggerRescan(level, zone, data);
                }
            }
        }

        event.setCanceled(true);
    }

    // Перезапускаем сканирование для зоны через ближайший фильтр
    private static void triggerRescan(ServerLevel level, DomeZone zone,
                                      DomeZoneSavedData data) {
        for (BlockPos filterPos : zone.filters) {
            BlockEntity be = level.getBlockEntity(filterPos);
            if (be instanceof AtmosphereFilterBlockEntity filter) {
                filter.startScan(level);
                return;
            }
        }
    }
}