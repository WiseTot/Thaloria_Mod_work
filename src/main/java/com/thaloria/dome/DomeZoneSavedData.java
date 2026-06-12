package com.thaloria.dome;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DomeZoneSavedData extends SavedData {

    private static final String DATA_NAME = "thaloria_dome_zones";

    // Все зоны на уровне, по UUID
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

    // Найти зону по позиции фильтра
    public DomeZone getZoneByFilter(net.minecraft.core.BlockPos filterPos) {
        for (DomeZone zone : zones.values()) {
            if (zone.filters.contains(filterPos)) {
                return zone;
            }
        }
        return null;
    }

    // Найти зону в которой находится позиция игрока
    // Проверяем просто — если позиция внутри AABB оболочки зоны
    public DomeZone getZoneContaining(net.minecraft.core.BlockPos pos) {
        for (DomeZone zone : zones.values()) {
            if (zone.isScanning) continue;
            if (isInsideShell(zone, pos)) {
                return zone;
            }
        }
        return null;
    }

    // Грубая проверка — позиция внутри выпуклой оболочки зоны
    // Используем AABB (bounding box) всех блоков оболочки
    private boolean isInsideShell(DomeZone zone, net.minecraft.core.BlockPos pos) {
        if (zone.shell.isEmpty()) return false;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (net.minecraft.core.BlockPos shell : zone.shell) {
            minX = Math.min(minX, shell.getX());
            minY = Math.min(minY, shell.getY());
            minZ = Math.min(minZ, shell.getZ());
            maxX = Math.max(maxX, shell.getX());
            maxY = Math.max(maxY, shell.getY());
            maxZ = Math.max(maxZ, shell.getZ());
        }

        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
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
        return data;
    }
}