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

    // Эталон — блоки купола при первом скане или после "Update Baseline"
    // Никогда не меняется автоматически
    public final Set<BlockPos> originalShell = new HashSet<>();

    // Текущие бреши = блоки из originalShell которых нет в мире
    public final Set<BlockPos> breaches = new HashSet<>();

    public final Set<UUID> playersInside = new HashSet<>();

    public float pressure = 0f;
    public float volume = 1f;
    public int scanRadius = 30;
    public boolean isScanning = false;
    public int scanProgress = 0;
    public boolean hasBaseline = false; // есть ли эталон

    public DomeZone(UUID id) {
        this.id = id;
    }

    public int getBreachCount() {
        return breaches.size();
    }

    // shell теперь это originalShell минус бреши
    public Set<BlockPos> getActiveShell() {
        Set<BlockPos> active = new HashSet<>(originalShell);
        active.removeAll(breaches);
        return active;
    }

    public float calculatePressureDelta() {
        float gain = (filters.size() * 100f) / volume;
        float loss = (breaches.size() * 50f) / volume;
        return gain - loss;
    }

    // Пересчитываем бреши — сравниваем эталон с реальностью
    // Вызывается после загрузки мира и после установки эталона
    public void recalculateBreaches(net.minecraft.server.level.ServerLevel level) {
        breaches.clear();
        for (BlockPos pos : originalShell) {
            // Если блока купола нет на месте — это брешь
            if (!level.getBlockState(pos).is(
                    net.minecraftforge.registries.ForgeRegistries.BLOCKS
                            .tags().createTagKey(new net.minecraft.resources.ResourceLocation(
                                    "thaloria", "dome_blocks"))
            )) {
                breaches.add(pos);
            }
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putFloat("pressure", pressure);
        tag.putFloat("volume", volume);
        tag.putInt("scanRadius", scanRadius);
        tag.putBoolean("hasBaseline", hasBaseline);

        ListTag filterList = new ListTag();
        for (BlockPos pos : filters) {
            filterList.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("filters", filterList);

        // Сохраняем эталон
        ListTag originalList = new ListTag();
        for (BlockPos pos : originalShell) {
            originalList.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("originalShell", originalList);

        // Бреши пересчитываются при загрузке — не сохраняем
        // (они могут измениться пока сервер был выключен)

        return tag;
    }

    public static DomeZone load(CompoundTag tag) {
        DomeZone zone = new DomeZone(tag.getUUID("id"));
        zone.pressure = tag.getFloat("pressure");
        zone.volume = tag.getFloat("volume");
        zone.scanRadius = tag.getInt("scanRadius");
        zone.hasBaseline = tag.getBoolean("hasBaseline");

        ListTag filterList = tag.getList("filters", Tag.TAG_COMPOUND);
        for (int i = 0; i < filterList.size(); i++) {
            zone.filters.add(NbtUtils.readBlockPos(filterList.getCompound(i)));
        }

        ListTag originalList = tag.getList("originalShell", Tag.TAG_COMPOUND);
        for (int i = 0; i < originalList.size(); i++) {
            zone.originalShell.add(NbtUtils.readBlockPos(originalList.getCompound(i)));
        }

        // Бреши пересчитаются при загрузке уровня в DomeZoneSavedData
        return zone;
    }
}