package com.thaloria.event;

import com.thaloria.dome.DomeZone;
import com.thaloria.dome.DomeZoneSavedData;
import com.thaloria.network.ModNetwork;
import com.thaloria.network.SyncPressureZonesPacket;
import com.thaloria.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import com.thaloria.ThaloriaMod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = ThaloriaMod.MOD_ID)
public class PressureGogglesHandler {

    private static final int SYNC_INTERVAL = 40;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (level.getGameTime() % SYNC_INTERVAL != 0) return;
        if (!isWearingGoggles(player)) return;

        DomeZoneSavedData data = DomeZoneSavedData.get(level);
        List<SyncPressureZonesPacket.ZoneBox> boxes = new ArrayList<>();

        for (DomeZone zone : data.getAllZones()) {
            if (zone.originalShell.isEmpty()) continue;

            // Вычисляем AABB из блоков оболочки
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

            for (BlockPos pos : zone.originalShell) {
                if (pos.getX() < minX) minX = pos.getX();
                if (pos.getY() < minY) minY = pos.getY();
                if (pos.getZ() < minZ) minZ = pos.getZ();
                if (pos.getX() > maxX) maxX = pos.getX();
                if (pos.getY() > maxY) maxY = pos.getY();
                if (pos.getZ() > maxZ) maxZ = pos.getZ();
            }

            // Давление зоны
            float pressure = zone.pressure;

            boxes.add(new SyncPressureZonesPacket.ZoneBox(
                    minX, minY, minZ, maxX, maxY, maxZ, pressure));
        }

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPressureZonesPacket(boxes)
        );
    }

    private static boolean isWearingGoggles(ServerPlayer player) {
        ItemStack main   = player.getMainHandItem();
        ItemStack off    = player.getOffhandItem();
        ItemStack helmet = player.getInventory().armor.get(3);
        return main.is(ModItems.PRESSURE_GOGGLES.get())
                || off.is(ModItems.PRESSURE_GOGGLES.get())
                || helmet.is(ModItems.PRESSURE_GOGGLES.get());
    }
}