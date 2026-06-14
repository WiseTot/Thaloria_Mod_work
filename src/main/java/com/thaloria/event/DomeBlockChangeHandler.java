package com.thaloria.event;

import com.thaloria.block.entity.AtmosphereFilterBlockEntity;
import com.thaloria.dome.DomeScanTask;
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

@Mod.EventBusSubscriber(modid = ThaloriaMod.MOD_ID)
public class DomeBlockChangeHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        if (state.is(ModBlocks.DOME_GLASS.get())) {
            handleDomeBlockRemoved(level, pos);
            return;
        }

        if (level.getBlockEntity(pos) instanceof AtmosphereFilterBlockEntity filter) {
            filter.onRemoved();
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getPlacedBlock();

        if (state.is(ModBlocks.DOME_GLASS.get())) {
            handleDomeBlockPlaced(level, pos);
        }
    }

    private static void handleDomeBlockRemoved(ServerLevel level, BlockPos pos) {
        DomeZoneSavedData data = DomeZoneSavedData.get(level);

        for (DomeZone zone : data.getAllZones()) {
            if (zone.isScanning) continue;
            if (!zone.hasBaseline) continue;

            // НОВАЯ ЛОГИКА: проверяем эталон, не shell
            if (zone.originalShell.contains(pos)) {
                zone.breaches.add(pos);
                data.setDirty();
            }
        }
    }

    private static void handleDomeBlockPlaced(ServerLevel level, BlockPos pos) {
        DomeZoneSavedData data = DomeZoneSavedData.get(level);

        for (DomeZone zone : data.getAllZones()) {
            if (zone.isScanning) continue;
            if (!zone.hasBaseline) continue;

            // Блок поставлен на место бреши — убираем брешь
            if (zone.breaches.contains(pos)) {
                zone.breaches.remove(pos);
                data.setDirty();
            }
        }
    }
}