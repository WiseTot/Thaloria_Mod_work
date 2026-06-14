package com.thaloria.network;

import com.thaloria.block.entity.AtmosphereFilterBlockEntity;
import com.thaloria.dome.DomeZone;
import com.thaloria.dome.DomeZoneSavedData;
import com.thaloria.dome.DomeScanTask;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateBaselinePacket {

    private final BlockPos filterPos;

    public UpdateBaselinePacket(BlockPos pos) {
        this.filterPos = pos;
    }

    public static void encode(UpdateBaselinePacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.filterPos);
    }

    public static UpdateBaselinePacket decode(FriendlyByteBuf buf) {
        return new UpdateBaselinePacket(buf.readBlockPos());
    }

    public static void handle(UpdateBaselinePacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            if (!(level.getBlockEntity(packet.filterPos)
                    instanceof AtmosphereFilterBlockEntity filter)) return;

            DomeZone zone = filter.getZone(level);
            if (zone == null) {
                player.sendSystemMessage(Component.literal(
                        "§c[Thaloria] No zone found! Run SCAN first."));
                return;
            }

            if (filter.isScanning) {
                player.sendSystemMessage(Component.literal(
                        "§c[Thaloria] Scan in progress, wait..."));
                return;
            }

            // Запускаем обновление эталона в отдельном потоке
            // чтобы не блокировать сервер
            final int radius = filter.scanRadius;
            final BlockPos origin = filter.getBlockPos();

            filter.isScanning = true;
            filter.scanProgress = 0;

            Thread thread = new Thread(() -> {
                try {
                    for (int i = 0; i <= 90; i += 10) {
                        filter.scanProgress = i;
                        Thread.sleep(200);
                    }

                    DomeScanTask.ScanResult result = DomeScanTask.scan(
                            level, origin, radius);

                    level.getServer().execute(() -> {
                        // Обновляем эталон
                        zone.originalShell.clear();
                        zone.originalShell.addAll(result.shell);
                        zone.volume = result.volume;
                        zone.hasBaseline = true;

                        // Сразу пересчитываем бреши
                        zone.recalculateBreaches(level);

                        DomeZoneSavedData.get(level).setDirty();

                        filter.isScanning = false;
                        filter.scanProgress = 100;
                        filter.setChanged();

                        player.sendSystemMessage(Component.literal(
                                "§a[Thaloria] Baseline updated! Shell=" +
                                        zone.originalShell.size() +
                                        " Breaches=" + zone.breaches.size()));
                    });
                } catch (Exception e) {
                    filter.isScanning = false;
                    e.printStackTrace();
                }
            });

            thread.setDaemon(true);
            thread.setName("BaselineUpdate-" + origin);
            thread.start();
        });
        ctx.get().setPacketHandled(true);
    }
}