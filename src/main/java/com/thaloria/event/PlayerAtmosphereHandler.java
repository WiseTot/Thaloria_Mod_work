package com.thaloria.event;

import com.thaloria.dome.DomeZone;
import com.thaloria.dome.DomeZoneSavedData;
import com.thaloria.oxygen.AtmosphereManager;
import com.thaloria.suit.SuitOxygenUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.thaloria.ThaloriaMod;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ThaloriaMod.MOD_ID)
public class PlayerAtmosphereHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.getGameTime() % 20 != 0) continue;

            DomeZoneSavedData data = DomeZoneSavedData.get(level);

            // Удаляем мёртвые зоны (без фильтров и с нулевым давлением)
            data.removeZoneIf(zone -> {
                if (zone.filters.isEmpty() && zone.pressure <= 0f) {
                    zone.playersInside.forEach(id -> {
                        ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
                        if (p != null) AtmosphereManager.setPressure(p, 0f);
                    });
                    return true;
                }
                return false;
            });

            // Обычный тик для живых зон
            for (DomeZone zone : data.getAllZones()) {
                if (zone.isScanning) continue;

                // Фильтров нет — давление быстро падает
                if (zone.filters.isEmpty()) {
                    zone.pressure = Math.max(0f, zone.pressure - 10f);
                    data.setDirty();
                    continue;
                }

                // Обычный пересчёт
                float delta = zone.calculatePressureDelta(level);
                zone.pressure = Math.max(0f, Math.min(100f, zone.pressure + delta));

                for (UUID playerId : zone.playersInside) {
                    ServerPlayer player = level.getServer()
                            .getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        AtmosphereManager.setPressure(player, zone.pressure);
                        applyBreathEffect(player, zone.pressure);
                    }
                }

                data.setDirty();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;
        if (player.tickCount % 10 != 0) return;

        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition();
        DomeZoneSavedData data = DomeZoneSavedData.get(level);

        DomeZone currentZone = data.getZoneContaining(pos);
        UUID previousZoneId = getPreviousZoneId(player);
        DomeZone previousZone = previousZoneId != null
                ? data.getZone(previousZoneId) : null;

        if (currentZone != previousZone) {
            if (previousZone != null) {
                previousZone.playersInside.remove(player.getUUID());
                data.setDirty();
            }

            if (currentZone != null) {
                currentZone.playersInside.add(player.getUUID());
                AtmosphereManager.setPressure(player, currentZone.pressure);
                data.setDirty();
            } else {
                // Вышел из купола — давление мгновенно 0
                AtmosphereManager.setPressure(player, 0f);
                AtmosphereManager.setBreath(player, 0);
            }

            setPreviousZoneId(player, currentZone != null ? currentZone.id : null);
        }

        // Каждый тик синхронизируем давление игрока с зоной
        // (чтобы падение давления зоны отражалось на игроке)
        if (currentZone != null) {
            AtmosphereManager.setPressure(player, currentZone.pressure);
        } else {
            // Снаружи — всегда 0
            float current = AtmosphereManager.getPressure(player);
            if (current > 0f) AtmosphereManager.setPressure(player, 0f);
        }

        if (player.tickCount % 20 != 0) return;

        float pressure = AtmosphereManager.getPressure(player);
        applyBreathEffect(player, pressure);

        player.displayClientMessage(Component.literal(
                "Pressure: " + (int) pressure + "%" +
                        " | Breath: " + AtmosphereManager.getBreath(player) +
                        " | O2: " + getSuitOxygen(player)
        ), true);
    }

    private static void applyBreathEffect(ServerPlayer player, float pressure) {
        List<net.minecraft.world.item.ItemStack> armorPieces = List.of(
                player.getInventory().armor.get(3),
                player.getInventory().armor.get(2),
                player.getInventory().armor.get(1),
                player.getInventory().armor.get(0)
        );

        boolean hasFullSuit = SuitOxygenUtil.isFullSuit(player);
        int suitO2 = hasFullSuit ? SuitOxygenUtil.getSuitOxygen(armorPieces) : 0;

        if (pressure >= 80f) {
            AtmosphereManager.addBreath(player, 2);

        } else if (pressure >= 60f) {
            if (hasFullSuit && suitO2 > 0) {
                SuitOxygenUtil.removeSuitOxygen(armorPieces, 1);
                AtmosphereManager.addBreath(player, 1);
            } else {
                AtmosphereManager.removeBreath(player, 1);
            }

        } else if (pressure >= 40f) {
            if (hasFullSuit && suitO2 > 0) {
                SuitOxygenUtil.removeSuitOxygen(armorPieces, 2);
            } else {
                AtmosphereManager.removeBreath(player, 2);
            }

        } else {
            if (hasFullSuit && suitO2 > 0) {
                SuitOxygenUtil.removeSuitOxygen(armorPieces, 4);
            } else {
                AtmosphereManager.removeBreath(player, 4);
            }
        }

        if (AtmosphereManager.getBreath(player) <= 0) {
            player.hurt(player.damageSources().generic(), 1.0f);
        }
    }

    private static final String PREV_ZONE_TAG = "thaloria_prev_zone";

    private static UUID getPreviousZoneId(ServerPlayer player) {
        var tag = player.getPersistentData();
        if (!tag.hasUUID(PREV_ZONE_TAG)) return null;
        return tag.getUUID(PREV_ZONE_TAG);
    }

    private static void setPreviousZoneId(ServerPlayer player, UUID id) {
        if (id == null) {
            player.getPersistentData().remove(PREV_ZONE_TAG);
        } else {
            player.getPersistentData().putUUID(PREV_ZONE_TAG, id);
        }
    }

    private static int getSuitOxygen(ServerPlayer player) {
        List<net.minecraft.world.item.ItemStack> armorPieces = List.of(
                player.getInventory().armor.get(3),
                player.getInventory().armor.get(2),
                player.getInventory().armor.get(1),
                player.getInventory().armor.get(0)
        );
        return SuitOxygenUtil.isFullSuit(player)
                ? SuitOxygenUtil.getSuitOxygen(armorPieces) : 0;
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        DomeZoneSavedData data = DomeZoneSavedData.get(level);
        UUID prevId = getPreviousZoneId(player);
        if (prevId != null) {
            DomeZone zone = data.getZone(prevId);
            if (zone != null) {
                zone.playersInside.remove(player.getUUID());
                data.setDirty();
            }
        }
    }
}