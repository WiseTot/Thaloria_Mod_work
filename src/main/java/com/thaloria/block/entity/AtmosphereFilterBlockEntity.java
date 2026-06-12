package com.thaloria.block.entity;

import com.thaloria.dome.DomeZone;
import com.thaloria.dome.DomeZoneSavedData;
import com.thaloria.dome.DomeScanTask;
import com.thaloria.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class AtmosphereFilterBlockEntity extends BlockEntity {

    // Радиус сканирования — игрок выставляет в UI (1-450)
    public int scanRadius = 30;

    // UUID зоны к которой привязан этот фильтр
    private UUID zoneId = null;

    // Статус для UI
    public boolean isPowered = false;
    public int scanProgress = 0;
    public boolean isScanning = false;

    public AtmosphereFilterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATMOSPHERE_FILTER.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        // Раз в секунду проверяем питание от генератора
        if (level.getGameTime() % 20 != 0) return;

        boolean wasPoewered = isPowered;
        isPowered = checkPower(serverLevel);

        // Питание появилось — запускаем сканирование
        if (isPowered && !wasPoewered) {
            startScan(serverLevel);
        }

        // Питание пропало — отключаем зону
        if (!isPowered && wasPoewered) {
            removeSelfFromZone(serverLevel);
        }

        // Если питание есть и зона есть — потребляем энергию
        if (isPowered && zoneId != null) {
            consumeGeneratorEnergy(serverLevel);
        }
    }

    // Проверяем есть ли рядом генератор с энергией
    // Ищем в радиусе 5 блоков
    private boolean checkPower(ServerLevel level) {
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos checkPos = this.getBlockPos().offset(x, y, z);
                    if (level.getBlockEntity(checkPos) instanceof GeneratorBlockEntity gen) {
                        if (gen.hasEnergy()) return true;
                    }
                }
            }
        }
        return false;
    }

    // Потребляем энергию генератора раз в секунду
    private void consumeGeneratorEnergy(ServerLevel level) {
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos checkPos = this.getBlockPos().offset(x, y, z);
                    if (level.getBlockEntity(checkPos) instanceof GeneratorBlockEntity gen) {
                        if (gen.hasEnergy()) {
                            gen.consumeEnergy(10);
                            return;
                        }
                    }
                }
            }
        }
    }

    // Запускаем сканирование асинхронно
    public void startScan(ServerLevel level) {
        isScanning = true;
        scanProgress = 0;

        // Убираем себя из старой зоны если была
        removeSelfFromZone(level);

        // Запускаем сканирование в отдельном потоке
        Thread scanThread = new Thread(() -> {
            try {
                // Имитируем прогресс для UI
                for (int i = 0; i <= 100; i += 10) {
                    scanProgress = i;
                    Thread.sleep(200); // 2 секунды общего сканирования
                }

                DomeScanTask.ScanResult result = DomeScanTask.scan(level, this.getBlockPos(), scanRadius);

                // Возвращаемся в основной поток для работы с миром
                level.getServer().execute(() -> {
                    DomeZoneSavedData data = DomeZoneSavedData.get(level);

                    // Создаём новую зону
                    DomeZone zone = new DomeZone(UUID.randomUUID());
                    zone.filters.add(this.getBlockPos());
                    zone.shell.addAll(result.shell);
                    zone.breaches.addAll(result.breaches);
                    zone.volume = result.volume;
                    zone.scanRadius = scanRadius;
                    zone.isScanning = false;

                    data.addZone(zone);
                    this.zoneId = zone.id;

                    isScanning = false;
                    scanProgress = 100;
                    setChanged();
                });

            } catch (InterruptedException e) {
                isScanning = false;
            }
        });

        scanThread.setDaemon(true);
        scanThread.setName("DomeScan-" + this.getBlockPos());
        scanThread.start();
    }

    // Убираем этот фильтр из зоны
    public void removeSelfFromZone(ServerLevel level) {
        if (zoneId == null) return;

        DomeZoneSavedData data = DomeZoneSavedData.get(level);
        DomeZone zone = data.getZone(zoneId);

        if (zone != null) {
            zone.filters.remove(this.getBlockPos());
            // Если фильтров не осталось — удаляем зону
            if (zone.filters.isEmpty()) {
                data.removeZone(zoneId);
            } else {
                data.setDirty();
            }
        }

        zoneId = null;
    }

    // Вызывается когда блок сломан
    public void onRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            removeSelfFromZone(serverLevel);
        }
    }

    public UUID getZoneId() { return zoneId; }

    public DomeZone getZone(ServerLevel level) {
        if (zoneId == null) return null;
        return DomeZoneSavedData.get(level).getZone(zoneId);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("scanRadius", scanRadius);
        tag.putBoolean("isPowered", isPowered);
        if (zoneId != null) tag.putUUID("zoneId", zoneId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        scanRadius = tag.getInt("scanRadius");
        isPowered = tag.getBoolean("isPowered");
        if (tag.hasUUID("zoneId")) zoneId = tag.getUUID("zoneId");
    }
}