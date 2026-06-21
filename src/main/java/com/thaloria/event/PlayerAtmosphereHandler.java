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

            // Тикаем давление всех зон
            for (DomeZone zone : data.getAllZones()) {
                if (zone.isScanning) continue;

                int activeFilters = 0;
                for (BlockPos filterPos : zone.filters) {
                    if (level.getBlockEntity(filterPos) instanceof
                            com.thaloria.block.entity.AtmosphereFilterBlockEntity f
                            && f.isPowered) {
                        activeFilters++;
                    }
                }

                if (activeFilters == 0) {
                    // Нет питания — 1% в секунду
                    zone.pressure = Math.max(0f, zone.pressure - 1f);
                } else {
                    float delta = zone.calculatePressureDelta(level);
                    zone.pressure = Math.max(0f, Math.min(100f, zone.pressure + delta));
                }

                // Синхронизируем давление игрокам внутри зоны
                for (UUID playerId : zone.playersInside) {
                    ServerPlayer p = level.getServer().getPlayerList().getPlayer(playerId);
                    if (p != null) {
                        AtmosphereManager.setPressure(p, zone.pressure);
                    }
                }

                data.setDirty();
            }

            // Удаляем мёртвые зоны — только когда давление УЖЕ упало до 0
            data.removeZoneIf(zone -> {
                if (zone.filters.isEmpty() && zone.pressure <= 0f) {
                    zone.playersInside.forEach(id -> {
                        ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
                        if (p != null) {
                            AtmosphereManager.setPressure(p, 0f);
                            p.getPersistentData().remove("thaloria_prev_zone");
                        }
                    });
                    return true;
                }
                return false;
            });
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

        DomeZone currentZone    = data.getZoneContaining(pos);
        UUID     previousZoneId = getPreviousZoneId(player);
        DomeZone previousZone   = previousZoneId != null
                ? data.getZone(previousZoneId) : null;

        // Если предыдущая зона ещё существует (давление падает) —
        // НЕ считаем что игрок вышел, он остаётся в ней
        if (currentZone == null && previousZone != null) {
            // Зона ещё существует (давление > 0, просто нет фильтров)
            // Давление уже обновляется через onServerTick по playersInside
            // Ничего не делаем — ждём пока давление упадёт до 0 и зона удалится
        } else if (currentZone != previousZone) {
            // Реальная смена зоны
            if (previousZone != null) {
                previousZone.playersInside.remove(player.getUUID());
                data.setDirty();
            }

            if (currentZone != null) {
                currentZone.playersInside.add(player.getUUID());
                data.setDirty();
            } else {
                // Вышел из купола и предыдущей зоны нет — давление 0
                AtmosphereManager.setPressure(player, 0f);
                AtmosphereManager.setBreath(player, 0);
            }

            setPreviousZoneId(player, currentZone != null ? currentZone.id : null);
        }

        // Снаружи всех куполов (previousZone тоже null) — давление 0
        if (currentZone == null && previousZone == null) {
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

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        DomeZoneSavedData data = DomeZoneSavedData.get(level);
        BlockPos pos = player.blockPosition();

        // Принудительно пересчитываем зону при входе
        DomeZone zone = data.getZoneContaining(pos);

        if (zone != null) {
            zone.playersInside.add(player.getUUID());
            AtmosphereManager.setPressure(player, zone.pressure);
            setPreviousZoneId(player, zone.id);
            data.setDirty();
        } else {
            // Снаружи — сбрасываем всё
            AtmosphereManager.setPressure(player, 0f);
            player.getPersistentData().remove("thaloria_prev_zone");
        }
    }
}