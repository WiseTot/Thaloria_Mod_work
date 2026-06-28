package com.thaloria.network;

import com.thaloria.block.entity.BreachDetectorBlockEntity;
import com.thaloria.client.render.BreachOutlineRenderer;
import com.thaloria.dome.DomeZone;
import com.thaloria.dome.DomeZoneSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.function.Supplier;

public class BreachDetectorActionPacket {

    public enum Action {
        SCAN_BREACHES,
        TOGGLE_AUTO_MONITOR
    }

    private final BlockPos detectorPos;
    private final Action action;

    public BreachDetectorActionPacket(BlockPos pos, Action action) {
        this.detectorPos = pos;
        this.action = action;
    }

    public static void encode(BreachDetectorActionPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.detectorPos);
        buf.writeEnum(p.action);
    }

    public static BreachDetectorActionPacket decode(FriendlyByteBuf buf) {
        return new BreachDetectorActionPacket(buf.readBlockPos(),
                buf.readEnum(Action.class));
    }

    public static void handle(BreachDetectorActionPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            if (!(level.getBlockEntity(packet.detectorPos)
                    instanceof BreachDetectorBlockEntity detector)) return;

            if (!detector.isPowered) {
                player.sendSystemMessage(Component.literal(
                        "§c[Thaloria] Breach Detector has no power!"));
                return;
            }

            DomeZone zone = detector.getZone(level);

            switch (packet.action) {
                case SCAN_BREACHES -> {
                    if (zone == null) {
                        player.sendSystemMessage(Component.literal(
                                "§c[Thaloria] No dome zone found nearby!"));
                        return;
                    }
                    if (!zone.hasBaseline) {
                        player.sendSystemMessage(Component.literal(
                                "§c[Thaloria] No baseline! Run SCAN on Atmosphere Filter first."));
                        return;
                    }

                    if (!zone.isSealed) {
                        // Купол не герметичен — показываем дыры через flood fill
                        // Берём позицию фильтра как точку старта
                        BlockPos filterPos = zone.filters.isEmpty() ? null
                                : zone.filters.iterator().next();

                        if (filterPos == null) {
                            player.sendSystemMessage(Component.literal(
                                    "§c[Thaloria] No filter found in zone!"));
                            return;
                        }

                        java.util.Set<BlockPos> leaks = com.thaloria.dome.DomeScanTask
                                .findLeakPositions(level, filterPos, zone.scanRadius);

                        if (leaks.isEmpty()) {
                            // Flood fill не нашёл дыр — может купол уже герметичен
                            player.sendSystemMessage(Component.literal(
                                    "§e[Thaloria] No leak positions found. " +
                                            "Try pressing Update Baseline on the filter."));
                        } else {
                            ModNetwork.CHANNEL.send(
                                    PacketDistributor.PLAYER.with(() -> player),
                                    new SyncBreachesPacket(
                                            new ArrayList<>(leaks),
                                            packet.detectorPos
                                    )
                            );
                            player.sendSystemMessage(Component.literal(
                                    "§e[Thaloria] Found " + leaks.size() +
                                            " leak position(s)! Dome is NOT sealed."));
                        }

                    } else {
                        // Купол герметичен — обычная проверка брешей через эталон
                        zone.recalculateBreaches(level);
                        DomeZoneSavedData.get(level).setDirty();

                        ModNetwork.CHANNEL.send(
                                PacketDistributor.PLAYER.with(() -> player),
                                new SyncBreachesPacket(
                                        new ArrayList<>(zone.breaches),
                                        packet.detectorPos
                                )
                        );

                        player.sendSystemMessage(Component.literal(
                                zone.breaches.isEmpty()
                                        ? "§a[Thaloria] No breaches detected!"
                                        : "§e[Thaloria] Found " + zone.breaches.size() + " breach(es)!"
                        ));
                    }
                }

                case TOGGLE_AUTO_MONITOR -> {
                    detector.autoMonitor = !detector.autoMonitor;
                    detector.setChanged();
                    player.sendSystemMessage(Component.literal(
                            detector.autoMonitor
                                    ? "§a[Thaloria] Auto-Monitor ENABLED"
                                    : "§7[Thaloria] Auto-Monitor DISABLED"
                    ));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}