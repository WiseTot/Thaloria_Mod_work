package com.thaloria.event;

import com.thaloria.block.entity.AtmosphereFilterBlockEntity;
import com.thaloria.dome.DomeZone;
import com.thaloria.dome.DomeZoneSavedData;
import com.thaloria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.thaloria.ThaloriaMod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ThaloriaMod.MOD_ID)
public class DomeBlockChangeHandler {

    // Блок сломан — проверяем был ли он частью оболочки купола
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        // Сломали блок купола — добавляем брешь в зону
        if (state.is(ModBlocks.DOME_GLASS.get())) {
            handleDomeBlockRemoved(level, pos);
            return;
        }

        // Сломали фильтр — убираем зону
        if (level.getBlockEntity(pos) instanceof AtmosphereFilterBlockEntity filter) {
            filter.onRemoved();
        }
    }

    // Блок поставлен — проверяем закрывает ли он брешь
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getPlacedBlock();

        // Поставили блок купола — проверяем закрыл ли он брешь
        if (state.is(ModBlocks.DOME_GLASS.get())) {
            handleDomeBlockPlaced(level, pos);
        }
    }

    // Блок купола сломан — добавляем брешь и пересчитываем давление
    private static void handleDomeBlockRemoved(ServerLevel level, BlockPos pos) {
        DomeZoneSavedData data = DomeZoneSavedData.get(level);

        for (DomeZone zone : data.getAllZones()) {
            if (zone.isScanning) continue;

            // Этот блок был частью оболочки?
            if (zone.shell.contains(pos)) {
                zone.shell.remove(pos);
                zone.breaches.add(pos);
                data.setDirty();

                // Логируем для отладки
                level.getServer().sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                                "[Thaloria] Breach detected at " + pos +
                                        " | Total breaches: " + zone.breaches.size() +
                                        " | New delta: " + zone.calculatePressureDelta()
                        )
                );
            }
        }
    }

    // Блок купола поставлен — проверяем закрыл ли брешь
    private static void handleDomeBlockPlaced(ServerLevel level, BlockPos pos) {
        DomeZoneSavedData data = DomeZoneSavedData.get(level);

        for (DomeZone zone : data.getAllZones()) {
            if (zone.isScanning) continue;

            // Эта позиция была брешью?
            if (zone.breaches.contains(pos)) {
                zone.breaches.remove(pos);
                zone.shell.add(pos);
                data.setDirty();

                level.getServer().sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                                "[Thaloria] Breach repaired at " + pos +
                                        " | Remaining breaches: " + zone.breaches.size() +
                                        " | New delta: " + zone.calculatePressureDelta()
                        )
                );

                // Если брешей не осталось — пересканируем для точности
                if (zone.breaches.isEmpty()) {
                    triggerRescan(level, zone);
                }
            }
        }
    }

    // Перезапускаем сканирование через ближайший фильтр зоны
    private static void triggerRescan(ServerLevel level, DomeZone zone) {
        for (BlockPos filterPos : zone.filters) {
            if (level.getBlockEntity(filterPos) instanceof AtmosphereFilterBlockEntity filter) {
                filter.startScan(level);
                return;
            }
        }
    }
}