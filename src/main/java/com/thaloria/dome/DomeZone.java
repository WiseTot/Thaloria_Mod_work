package com.thaloria.dome;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DomeZone {

    public final UUID id;
    public final Set<BlockPos> filters = new HashSet<>();
    public final Set<BlockPos> shell = new HashSet<>();
    public final Set<BlockPos> breaches = new HashSet<>();
    public final Set<UUID> playersInside = new HashSet<>();

    public float pressure = 0f;
    public float volume = 1f;
    public int scanRadius = 30;
    public boolean isScanning = false;
    public int scanProgress = 0;

    public DomeZone(UUID id) {
        this.id = id;
    }

    public int getBreachCount() {
        return breaches.size();
    }

    public float calculatePressureDelta() {
        float gain = (filters.size() * 100f) / volume;
        float loss = (breaches.size() * 50f) / volume;
        return gain - loss;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putFloat("pressure", pressure);
        tag.putFloat("volume", volume);
        tag.putInt("scanRadius", scanRadius);

        ListTag filterList = new ListTag();
        for (BlockPos pos : filters) {
            filterList.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("filters", filterList);

        ListTag shellList = new ListTag();
        for (BlockPos pos : shell) {
            shellList.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("shell", shellList);

        ListTag breachList = new ListTag();
        for (BlockPos pos : breaches) {
            breachList.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("breaches", breachList);

        return tag;
    }

    public static DomeZone load(CompoundTag tag) {
        DomeZone zone = new DomeZone(tag.getUUID("id"));
        zone.pressure = tag.getFloat("pressure");
        zone.volume = tag.getFloat("volume");
        zone.scanRadius = tag.getInt("scanRadius");

        ListTag filterList = tag.getList("filters", Tag.TAG_COMPOUND);
        for (int i = 0; i < filterList.size(); i++) {
            zone.filters.add(NbtUtils.readBlockPos(filterList.getCompound(i)));
        }

        ListTag shellList = tag.getList("shell", Tag.TAG_COMPOUND);
        for (int i = 0; i < shellList.size(); i++) {
            zone.shell.add(NbtUtils.readBlockPos(shellList.getCompound(i)));
        }

        ListTag breachList = tag.getList("breaches", Tag.TAG_COMPOUND);
        for (int i = 0; i < breachList.size(); i++) {
            zone.breaches.add(NbtUtils.readBlockPos(breachList.getCompound(i)));
        }

        return zone;
    }
}