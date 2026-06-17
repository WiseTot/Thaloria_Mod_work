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

import java.util.ArrayList;
import java.util.List;

public class DustStormManager {

    // Состояние шторма
    public static boolean isStormActive = false;
    private static int stormTicksRemaining = 0;

    // Шторм длится 3–6 минут (3600–7200 тиков)
    private static final int STORM_MIN_TICKS = 3600;
    private static final int STORM_MAX_TICKS = 7200;

    // Шторм начинается раз в 10–20 минут (12000–24000 тиков)
    private static final int STORM_COOLDOWN_MIN = 12000;
    private static final int STORM_COOLDOWN_MAX = 24000;

    private static int nextStormIn = 12000; // первый шторм через 10 минут

    // Урон куполу: раз в 30 секунд ломаем 1–2 блока
    private static final int DOME_DAMAGE_INTERVAL = 600;

    public static void tick(ServerLevel level) {
        // Только в измерении Талории
        if (!level.dimension().location()
                .equals(new ResourceLocation("thaloria", "thaloria_planet"))) return;

        if (isStormActive) {
            tickStorm(level);
        } else {
            tickCooldown(level);
        }
    }

    private static void tickStorm(ServerLevel level) {
        stormTicksRemaining--;

        // Урон куполу раз в 30 секунд
        if (stormTicksRemaining % DOME_DAMAGE_INTERVAL == 0) {
            damageDomes(level);
        }

        if (stormTicksRemaining <= 0) {
            stopStorm(level);
        }
    }

    private static void tickCooldown(ServerLevel level) {
        nextStormIn--;
        if (nextStormIn <= 0) {
            startStorm(level);
        }
    }

    private static void startStorm(ServerLevel level) {
        isStormActive = true;
        stormTicksRemaining = STORM_MIN_TICKS +
                level.random.nextInt(STORM_MAX_TICKS - STORM_MIN_TICKS);

        // Планируем следующий шторм
        nextStormIn = STORM_COOLDOWN_MIN +
                level.random.nextInt(STORM_COOLDOWN_MAX - STORM_COOLDOWN_MIN);

        // Синхронизируем всем игрокам в измерении
        syncToAll(level, true);

        level.getServer().sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§6[Thaloria] Dust storm incoming! Duration: " +
                                (stormTicksRemaining / 20) + "s"));
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

            // Берём случайные блоки из эталона которые ещё целы
            List<BlockPos> intact = zone.originalShell.stream()
                    .filter(pos -> !zone.breaches.contains(pos))
                    .filter(pos -> level.getBlockState(pos)
                            .is(ModBlocks.DOME_GLASS.get()))
                    .collect(java.util.stream.Collectors.toList());

            if (intact.isEmpty()) continue;

            // Ломаем 1–2 блока
            int count = 1 + level.random.nextInt(2);
            for (int i = 0; i < Math.min(count, intact.size()); i++) {
                BlockPos target = intact.get(level.random.nextInt(intact.size()));
                level.removeBlock(target, false);
                zone.breaches.add(target);
            }
            data.setDirty();
        }
    }

    private static void syncToAll(ServerLevel level, boolean active) {
        SyncStormPacket packet = new SyncStormPacket(active);
        for (ServerPlayer player : level.players()) {
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    /** Вызывается при входе игрока — синхронизируем текущее состояние */
    public static void syncToPlayer(ServerPlayer player) {
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncStormPacket(isStormActive));
    }
}