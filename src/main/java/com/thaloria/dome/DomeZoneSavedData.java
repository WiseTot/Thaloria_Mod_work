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
            if (isInsideShell(zone, pos)) return zone;
        }
        return null;
    }

    // ИСПРАВЛЕНИЕ: при пересканировании полностью сбрасываем
    // оболочку и бреши зоны и заполняем заново
    public void resetZoneShell(UUID zoneId, java.util.Set<BlockPos> newShell,
                               java.util.Set<BlockPos> newBreaches, float newVolume) {
        DomeZone zone = zones.get(zoneId);
        if (zone == null) return;
        zone.shell.clear();
        zone.breaches.clear();
        zone.shell.addAll(newShell);
        zone.breaches.addAll(newBreaches);
        zone.volume = newVolume;
        setDirty();
    }

    private boolean isInsideShell(DomeZone zone, BlockPos pos) {
        if (zone.shell.isEmpty()) return false;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos shell : zone.shell) {
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
        return data;
    }
}