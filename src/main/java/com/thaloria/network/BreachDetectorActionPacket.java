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
import java.util.HashSet;
import java.util.Set;

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
                        // Дыры = воздушные блоки которые граничат с блоками купола
                        // минимум с 2 сторон (угол дыры) — это точно место в стене
                        BlockPos filterPos = zone.filters.isEmpty() ? null
                                : zone.filters.iterator().next();
                        if (filterPos == null) return;

                        Set<BlockPos> allDome = new HashSet<>(
                                com.thaloria.dome.DomeScanTask.collectAllDomeBlocks(
                                        level, filterPos, zone.scanRadius));

                        Set<BlockPos> leaks = new HashSet<>();
                        int[] ddx = {1,-1,0,0,0,0};
                        int[] ddy = {0,0,1,-1,0,0};
                        int[] ddz = {0,0,0,0,1,-1};

                        for (BlockPos domePos : allDome) {
                            // Для каждого блока купола смотрим соседей
                            for (int i = 0; i < 6; i++) {
                                BlockPos neighbor = domePos.offset(ddx[i], ddy[i], ddz[i]);
                                // Сосед — воздух и не в эталоне
                                if (!level.getBlockState(neighbor).isAir()) continue;
                                if (allDome.contains(neighbor)) continue;

                                int domeNeighbors = 0;
                                for (int j = 0; j < 6; j++) {
                                    BlockPos nn = neighbor.offset(ddx[j], ddy[j], ddz[j]);
                                    if (allDome.contains(nn)) domeNeighbors++;
                                }

                                boolean isHole = false;
                                int[][] pairs = {{0,1},{2,3},{4,5}};
                                for (int[] pair : pairs) {
                                    BlockPos a = neighbor.offset(ddx[pair[0]], ddy[pair[0]], ddz[pair[0]]);
                                    BlockPos b = neighbor.offset(ddx[pair[1]], ddy[pair[1]], ddz[pair[1]]);
                                    if (allDome.contains(a) && allDome.contains(b)) {
                                        isHole = true;
                                        break;
                                    }
                                }

                                if (isHole) {
                                    leaks.add(neighbor);
                                }
                            }
                        }

                        if (leaks.isEmpty()) {
                            player.sendSystemMessage(Component.literal(
                                    "§e[Thaloria] No specific leak positions found."));
                        } else {
                            ModNetwork.CHANNEL.send(
                                    PacketDistributor.PLAYER.with(() -> player),
                                    new SyncBreachesPacket(new ArrayList<>(leaks), packet.detectorPos));
                            player.sendSystemMessage(Component.literal(
                                    "§e[Thaloria] Found " + leaks.size() + " leak position(s)!"));
                        }
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