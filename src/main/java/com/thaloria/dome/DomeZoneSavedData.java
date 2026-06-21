package com.thaloria.dome;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class DomeZoneSavedData extends SavedData {

    private static final String DATA_NAME = "thaloria_dome_zones";
    private final Map<UUID, DomeZone> zones = new HashMap<>();

    public static DomeZoneSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                DomeZoneSavedData::load,
                DomeZoneSavedData::new,
                DATA_NAME
        );
    }

    public void addZone(DomeZone zone) {
        zones.put(zone.id, zone);
        setDirty();
    }

    public void removeZone(UUID id) {
        zones.remove(id);
        setDirty();
    }

    public DomeZone getZone(UUID id) {
        return zones.get(id);
    }

    public Collection<DomeZone> getAllZones() {
        return zones.values();
    }

    public DomeZone getZoneByFilter(BlockPos filterPos) {
        for (DomeZone zone : zones.values()) {
            if (zone.filters.contains(filterPos)) return zone;
        }
        return null;
    }

    public DomeZone getZoneContaining(BlockPos pos) {
        for (DomeZone zone : zones.values()) {
            if (zone.isScanning) continue;

            if (!zone.filters.isEmpty()) {
                // Обычная проверка через raycast к фильтру
                if (isInsideZone(zone, pos)) return zone;
            } else if (!zone.originalShell.isEmpty() && zone.pressure > 0f) {
                // Фильтр снесён но зона ещё "живая" (давление падает)
                // Проверяем через AABB оболочки — быстро и без фильтра
                if (isInsideShellAABB(zone, pos)) return zone;
            }
        }
        return null;
    }

    // ИСПРАВЛЕНИЕ: используем центр фильтра и радиус сканирования
    // вместо AABB по shell блокам — это надёжнее
    private boolean isInsideZone(DomeZone zone, BlockPos playerPos) {
        if (zone.filters.isEmpty()) return false;
        if (zone.originalShell.isEmpty()) return false;

        for (BlockPos filterPos : zone.filters) {
            int inside = 0;
            int total = 6;

            // Центр
            if (!rayHitsShell(zone,
                    playerPos.getX() + 0.5, playerPos.getY() + 0.5,
                    playerPos.getZ() + 0.5, filterPos)) inside++;
            // Верх-лево-перед
            if (!rayHitsShell(zone,
                    playerPos.getX() + 0.2, playerPos.getY() + 0.9,
                    playerPos.getZ() + 0.2, filterPos)) inside++;
            // Верх-право-зад
            if (!rayHitsShell(zone,
                    playerPos.getX() + 0.8, playerPos.getY() + 0.9,
                    playerPos.getZ() + 0.8, filterPos)) inside++;
            // Низ
            if (!rayHitsShell(zone,
                    playerPos.getX() + 0.5, playerPos.getY() + 0.1,
                    playerPos.getZ() + 0.5, filterPos)) inside++;
            // Лево-зад
            if (!rayHitsShell(zone,
                    playerPos.getX() + 0.1, playerPos.getY() + 0.5,
                    playerPos.getZ() + 0.8, filterPos)) inside++;
            // Право-перед
            if (!rayHitsShell(zone,
                    playerPos.getX() + 0.9, playerPos.getY() + 0.5,
                    playerPos.getZ() + 0.2, filterPos)) inside++;

            // 5 из 6 чтобы быть внутри
            if (inside >= 5) return true;
        }
        return false;
    }

    /**
     * Быстрая проверка через AABB оболочки — используется когда фильтров нет.
     * Игрок считается внутри если его позиция строго внутри min/max границ shell.
     */
    private boolean isInsideShellAABB(DomeZone zone, BlockPos playerPos) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos p : zone.originalShell) {
            if (p.getX() < minX) minX = p.getX();
            if (p.getY() < minY) minY = p.getY();
            if (p.getZ() < minZ) minZ = p.getZ();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getY() > maxY) maxY = p.getY();
            if (p.getZ() > maxZ) maxZ = p.getZ();
        }

        return playerPos.getX() > minX && playerPos.getX() < maxX
                && playerPos.getY() > minY && playerPos.getY() < maxY
                && playerPos.getZ() > minZ && playerPos.getZ() < maxZ;
    }

    private boolean rayHitsShell(DomeZone zone,
                                 double fromX, double fromY, double fromZ,
                                 BlockPos to) {
        double dx = to.getX() + 0.5 - fromX;
        double dy = to.getY() + 0.5 - fromY;
        double dz = to.getZ() + 0.5 - fromZ;
        double length = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (length == 0) return false;

        dx /= length; dy /= length; dz /= length;

        double x = fromX;
        double y = fromY;
        double z = fromZ;

        double step = 0.4;
        int steps = (int)(length / step) + 1;
        int skipSteps = (int)(0.8 / step); // Уменьшили skip до 0.8
        BlockPos last = null;

        for (int i = 0; i < steps; i++) {
            x += dx * step;
            y += dy * step;
            z += dz * step;

            BlockPos pos = BlockPos.containing(x, y, z);
            if (pos.equals(last)) continue;
            last = pos;

            // Если достигли фильтра — оболочку не пересекли
            if (pos.getX() == to.getX() && pos.getY() == to.getY()
                    && pos.getZ() == to.getZ()) return false;

            if (i < skipSteps) continue;

            if (zone.originalShell.contains(pos)) return true;
            if (zone.originalShell.contains(pos.above())) return true;
            if (zone.originalShell.contains(pos.below())) return true;
            if (zone.originalShell.contains(pos.north())) return true;
            if (zone.originalShell.contains(pos.south())) return true;
            if (zone.originalShell.contains(pos.east())) return true;
            if (zone.originalShell.contains(pos.west())) return true;
        }
        return false;
    }

    public void resetZoneShell(UUID zoneId, Set<BlockPos> newShell,
                               Set<BlockPos> newBreaches, float newVolume) {
        DomeZone zone = zones.get(zoneId);
        if (zone == null) return;
        zone.originalShell.clear();
        zone.breaches.clear();
        zone.originalShell.addAll(newShell);
        zone.breaches.addAll(newBreaches);
        zone.volume = newVolume;
        setDirty();
    }

    public void removeZoneIf(Predicate<DomeZone> condition) {
        zones.values().removeIf(condition);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (DomeZone zone : zones.values()) {
            list.add(zone.save());
        }
        tag.put("zones", list);
        return tag;
    }

    public static DomeZoneSavedData load(CompoundTag tag) {
        DomeZoneSavedData data = new DomeZoneSavedData();
        ListTag list = tag.getList("zones", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            DomeZone zone = DomeZone.load(list.getCompound(i));
            data.zones.put(zone.id, zone);
        }
        // Бреши пересчитаются когда уровень загрузится
        // через DomeZoneSavedData.recalculateAllBreaches(level)
        return data;
    }

    // Вызывается при загрузке уровня
    public void recalculateAllBreaches(net.minecraft.server.level.ServerLevel level) {
        for (DomeZone zone : zones.values()) {
            if (zone.hasBaseline) {
                zone.recalculateBreaches(level);
            }
        }
        setDirty();
    }
}