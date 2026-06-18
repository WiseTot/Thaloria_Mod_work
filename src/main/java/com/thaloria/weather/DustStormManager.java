package com.thaloria.weather;

import com.thaloria.dome.DomeZone;
import com.thaloria.dome.DomeZoneSavedData;
import com.thaloria.network.ModNetwork;
import com.thaloria.network.SyncStormPacket;
import com.thaloria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.stream.Collectors;

public class DustStormManager {

    public static boolean isStormActive = false;
    private static int stormTicksRemaining = 0;

    private static final int STORM_MIN_TICKS   = 3600;
    private static final int STORM_MAX_TICKS   = 7200;
    private static final int COOLDOWN_MIN      = 12000;
    private static final int COOLDOWN_MAX      = 24000;
    private static final int DOME_DAMAGE_INTERVAL = 600;

    private static int nextStormIn = 12000;

    // ── Автоматический тик ───────────────────────────────────────

    public static void tick(ServerLevel level) {
        if (!isThaloriaLevel(level)) return;

        if (isStormActive) {
            stormTicksRemaining--;

            if (stormTicksRemaining % DOME_DAMAGE_INTERVAL == 0) {
                damageDomes(level);
            }

            if (stormTicksRemaining <= 0) {
                stopStorm(level);
            }
        } else {
            nextStormIn--;
            if (nextStormIn <= 0) {
                startStorm(level);
            }
        }
    }

    // ── Внутренние методы ─────────────────────────────────────────

    private static void startStorm(ServerLevel level) {
        int duration = STORM_MIN_TICKS +
                level.random.nextInt(STORM_MAX_TICKS - STORM_MIN_TICKS);
        startStormInternal(level, duration);

        // Планируем следующий автоматический шторм
        nextStormIn = COOLDOWN_MIN +
                level.random.nextInt(COOLDOWN_MAX - COOLDOWN_MIN);
    }

    private static void startStormInternal(ServerLevel level, int durationTicks) {
        isStormActive = true;
        stormTicksRemaining = durationTicks;
        syncToAll(level, true);

        level.getServer().sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§6[Thaloria] Dust storm! Duration: " +
                                (durationTicks / 20) + "s"));
    }

    private static void stopStorm(ServerLevel level) {
        isStormActive = false;
        stormTicksRemaining = 0;
        syncToAll(level, false);

        level.getServer().sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§7[Thaloria] Dust storm has passed."));
    }

    private static void damageDomes(ServerLevel level) {
        DomeZoneSavedData data = DomeZoneSavedData.get(level);
        for (DomeZone zone : data.getAllZones()) {
            if (zone.originalShell.isEmpty()) continue;

            List<BlockPos> intact = zone.originalShell.stream()
                    .filter(pos -> !zone.breaches.contains(pos))
                    .filter(pos -> level.getBlockState(pos)
                            .is(ModBlocks.DOME_GLASS.get()))
                    .collect(Collectors.toList());

            if (intact.isEmpty()) continue;

            int count = 1 + level.random.nextInt(2);
            for (int i = 0; i < Math.min(count, intact.size()); i++) {
                BlockPos target = intact.get(level.random.nextInt(intact.size()));
                level.removeBlock(target, false);
                zone.breaches.add(target);
            }
            data.setDirty();
        }
    }

    // ── Публичные методы для команды ──────────────────────────────

    /** Запустить шторм вручную через команду */
    public static void startStormManual(ServerLevel level, int durationTicks) {
        if (isStormActive) {
            // Если шторм уже идёт — просто продлеваем
            stormTicksRemaining = durationTicks;
            syncToAll(level, true);
        } else {
            startStormInternal(level, durationTicks);
        }
        // Сбрасываем авто-таймер чтобы не накладывались
        nextStormIn = COOLDOWN_MIN;
    }

    /** Остановить шторм вручную через команду */
    public static void stopStormManual(ServerLevel level) {
        stopStorm(level);
        // Сбрасываем авто-таймер
        nextStormIn = COOLDOWN_MIN;
    }

    // ── Синхронизация ─────────────────────────────────────────────

    public static void syncToPlayer(ServerPlayer player) {
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncStormPacket(isStormActive));
    }

    private static void syncToAll(ServerLevel level, boolean active) {
        SyncStormPacket packet = new SyncStormPacket(active);
        for (ServerPlayer player : level.players()) {
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    private static boolean isThaloriaLevel(ServerLevel level) {
        return level.dimension().location()
                .equals(new ResourceLocation("thaloria", "thaloria_planet"));
    }
}