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

@Mod.EventBusSubscriber(modid = ThaloriaMod.MOD_ID)
public class PlayerAtmosphereHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.getGameTime() % 20 != 0) continue;

            DomeZoneSavedData data = DomeZoneSavedData.get(level);

            for (DomeZone zone : data.getAllZones()) {
                if (zone.isScanning) continue;

                // Считаем активные фильтры
                int activeFilters = 0;
                for (BlockPos filterPos : zone.filters) {
                    if (level.getBlockEntity(filterPos) instanceof
                            com.thaloria.block.entity.AtmosphereFilterBlockEntity f
                            && f.isPowered) {
                        activeFilters++;
                    }
                }

                if (activeFilters == 0) {
                    // Нет питания — давление падает
                    zone.pressure = Math.max(0f, zone.pressure - 1f);
                } else if (!zone.isSealed) {
                    // Купол не герметичен — перепроверяем через строгий raycast раз в 10 секунд
                    if (level.getGameTime() % 200 == 0) {
                        // Берём позицию любого фильтра как origin для проверки
                        BlockPos filterPos = zone.filters.isEmpty() ? null
                                : zone.filters.iterator().next();
                        if (filterPos != null) {
                            com.thaloria.dome.DomeScanTask.SealCheckResult check =
                                    com.thaloria.dome.DomeScanTask.checkSealed(level, filterPos, zone.scanRadius);
                            if (check.isSealed) {
                                zone.isSealed = true;
                                DomeZoneSavedData.get(level).setDirty();
                                for (ServerPlayer p : level.players()) {
                                    p.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                            "§a[Thaloria] Dome sealed! Pressure building up."));
                                }
                            }
                        }
                    }
                    // Давление не растёт пока не герметичен
                    zone.pressure = Math.max(0f, zone.pressure - 0.5f);
                } else {
                    // Питание есть и купол герметичен — давление растёт
                    float delta = zone.calculatePressureDelta(level);
                    zone.pressure = Math.max(0f, Math.min(100f, zone.pressure + delta));
                }

                data.setDirty();
            }

            // Удаляем мёртвые зоны (нет фильтров + давление 0)
            data.removeZoneIf(zone -> zone.filters.isEmpty() && zone.pressure <= 0f);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;
        if (player.tickCount % 20 != 0) return; // раз в секунду

        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition();
        DomeZoneSavedData data = DomeZoneSavedData.get(level);

        // Просто ищем в какой зоне игрок прямо сейчас
        DomeZone currentZone = data.getZoneContaining(pos);

        if (currentZone != null) {
            // Внутри купола — давление зоны
            AtmosphereManager.setPressure(player, currentZone.pressure);
        } else {
            // Снаружи — давление 0
            AtmosphereManager.setPressure(player, 0f);
        }

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
            // Хорошее давление — дыхание сразу полное
            AtmosphereManager.setBreath(player, 40);

        } else if (pressure >= 60f) {
            // Среднее давление — дыхание восстанавливается медленно
            AtmosphereManager.addBreath(player, 2);

        } else if (pressure >= 40f) {
            // Давление низкое но есть — дыхание медленно растёт
            AtmosphereManager.addBreath(player, 1);
            // Если есть костюм — экономим его кислород (не тратим)
            // Если нет костюма — всё равно дыхание растёт, просто медленно
        } else {
            // Критическое давление — дыхание сразу 0
            AtmosphereManager.setBreath(player, 0);

            // Костюм тратит кислород чтобы защитить игрока
            if (hasFullSuit && suitO2 > 0) {
                SuitOxygenUtil.removeSuitOxygen(armorPieces, 4);
            } else {
                // Без костюма — урон
                player.hurt(player.damageSources().generic(), 1.0f);
            }
        }

        // Если дыхание 0 и нет костюма — урон
        if (AtmosphereManager.getBreath(player) <= 0 && !(hasFullSuit && suitO2 > 0)) {
            player.hurt(player.damageSources().generic(), 1.0f);
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
        // Очищаем NBT при выходе
        player.getPersistentData().remove("thaloria_prev_zone");
    }
}