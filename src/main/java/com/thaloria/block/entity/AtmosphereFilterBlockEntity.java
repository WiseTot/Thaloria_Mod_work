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

import java.util.Set;
import java.util.UUID;

public class AtmosphereFilterBlockEntity extends BlockEntity {

    public int scanRadius = 30;
    private UUID zoneId = null;

    public boolean isPowered = false;
    public int scanProgress = 0;
    public boolean isScanning = false;

    public AtmosphereFilterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATMOSPHERE_FILTER.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (level.getGameTime() % 20 != 0) return;

        boolean wasPowered = isPowered;
        isPowered = checkPower(serverLevel);

        // Питание пропало — давление зоны начинает падать
        if (!isPowered && wasPowered) {
            if (zoneId != null) {
                DomeZoneSavedData data = DomeZoneSavedData.get(serverLevel);
                DomeZone zone = data.getZone(zoneId);
                if (zone != null) {
                    zone.filters.remove(this.getBlockPos());
                    data.setDirty();
                }
            }
            return;
        }

        if (isPowered && !wasPowered && zoneId != null) {
            DomeZoneSavedData data = DomeZoneSavedData.get(serverLevel);
            DomeZone zone = data.getZone(zoneId);
            if (zone != null && !zone.filters.contains(this.getBlockPos())) {
                zone.filters.add(this.getBlockPos());
                data.setDirty();
            }
        }

        if (isPowered) {
            DomeZoneSavedData data = DomeZoneSavedData.get(serverLevel);
            if (zoneId != null && data.getZone(zoneId) == null) {
                zoneId = null;
                setChanged();
            }

            if (zoneId == null && !isScanning) {
                startScan(serverLevel);
            } else {
                consumeGeneratorEnergy(serverLevel);
            }
        }
    }

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

    /**
     * ЛЁГКАЯ проверка брешей — только сравниваем эталон с реальностью.
     * Эталон НЕ меняется. Вызывается кнопкой "Scan" в UI фильтра.
     */
    public int checkBreaches(ServerLevel level) {
        if (zoneId == null) return -1;
        DomeZoneSavedData data = DomeZoneSavedData.get(level);
        DomeZone zone = data.getZone(zoneId);
        if (zone == null || !zone.hasBaseline) return -1;

        zone.recalculateBreaches(level);
        data.setDirty();
        return zone.breaches.size();
    }

    /**
     * ПОЛНОЕ пересканирование — строит новую зону и новый эталон.
     * Вызывается при включении питания, при потере зоны, и кнопкой "Update Baseline".
     */
    public void startScan(ServerLevel level) {
        if (isScanning) return;
        isScanning = true;
        scanProgress = 0;

        // Сохраняем давление если зона уже была
        float savedPressure = 0f;
        boolean alreadyHasBaseline = false;
        DomeZoneSavedData data = DomeZoneSavedData.get(level);
        if (zoneId != null) {
            DomeZone existing = data.getZone(zoneId);
            if (existing != null) {
                savedPressure = existing.pressure;
                alreadyHasBaseline = existing.hasBaseline;
            }
        }
        final float pressureToRestore = savedPressure;
        final boolean keepBaseline = alreadyHasBaseline;

        removeSelfFromZone(level);

        final ServerLevel levelRef = level;
        final BlockPos originPos = this.getBlockPos();
        final int radius = this.scanRadius;

        Thread scanThread = new Thread(() -> {
            try {
                for (int i = 0; i <= 90; i += 10) {
                    scanProgress = i;
                    Thread.sleep(200);
                }

                DomeScanTask.ScanResult result = DomeScanTask.scan(levelRef, originPos, radius);

                levelRef.getServer().execute(() -> {
                    DomeZoneSavedData dataRef = DomeZoneSavedData.get(levelRef);
                    DomeZone existingZone = dataRef.getZoneContaining(originPos);

                    if (existingZone != null && !existingZone.filters.isEmpty()) {
                        existingZone.filters.add(originPos);
                        existingZone.volume = (existingZone.volume + result.volume) / 2f;

                        // Если у существующей зоны уже есть baseline — НЕ перезаписываем
                        if (!existingZone.hasBaseline) {
                            for (BlockPos shellPos : result.shell) {
                                if (DomeScanTask.isDomeBlock(levelRef.getBlockState(shellPos))) {
                                    existingZone.originalShell.add(shellPos);
                                }
                            }
                            existingZone.hasBaseline = true;
                        }

                        zoneId = existingZone.id;
                        dataRef.setDirty();
                        existingZone.recalculateBreaches(levelRef);
                    } else {
                        DomeZone zone = new DomeZone(UUID.randomUUID());
                        zone.filters.add(originPos);
                        zone.volume = result.volume;
                        zone.scanRadius = radius;
                        zone.isScanning = false;
                        zone.pressure = pressureToRestore;

                        // Если baseline уже был (перезагрузка мира) — НЕ строим новый
                        // Baseline строится только через Update Baseline вручную
                        if (!keepBaseline) {
                            Set<BlockPos> allDomeBlocks = DomeScanTask.collectAllDomeBlocks(
                                    levelRef, originPos, radius);
                            zone.originalShell.addAll(allDomeBlocks);
                            zone.hasBaseline = true;
                        }
                        // Если keepBaseline=true но originalShell пустой —
                        // значит зона была удалена, строим baseline заново
                        if (zone.originalShell.isEmpty()) {
                            Set<BlockPos> allDomeBlocks = DomeScanTask.collectAllDomeBlocks(
                                    levelRef, originPos, radius);
                            zone.originalShell.addAll(allDomeBlocks);
                            zone.hasBaseline = true;
                        }

                        dataRef.addZone(zone);
                        zoneId = zone.id;
                        zone.recalculateBreaches(levelRef);
                    }

                    isScanning = false;
                    scanProgress = 100;
                    setChanged();
                });

            } catch (Exception e) {
                isScanning = false;
                e.printStackTrace();
            }
        });

        scanThread.setDaemon(true);
        scanThread.setName("DomeScan-" + this.getBlockPos());
        scanThread.start();
    }

    public void removeSelfFromZone(ServerLevel level) {
        if (zoneId == null) return;

        DomeZoneSavedData data = DomeZoneSavedData.get(level);
        DomeZone zone = data.getZone(zoneId);

        if (zone != null) {
            zone.filters.remove(this.getBlockPos());
            if (zone.filters.isEmpty()) {
                data.removeZone(zoneId);
            } else {
                data.setDirty();
            }
        }

        zoneId = null;
        setChanged();
    }

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
        if (zoneId != null) tag.putUUID("zoneId", zoneId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        scanRadius = tag.getInt("scanRadius");
        if (tag.hasUUID("zoneId")) zoneId = tag.getUUID("zoneId");
    }
}