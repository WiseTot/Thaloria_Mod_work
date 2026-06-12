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

    public int scanRadius = 30;
    private UUID zoneId = null;

    public boolean isPowered = false;
    public int scanProgress = 0;
    public boolean isScanning = false;

    // ИСПРАВЛЕНИЕ: не сохраняем isPowered в NBT —
    // каждый раз при загрузке проверяем заново
    private boolean scanScheduled = false;

    public AtmosphereFilterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATMOSPHERE_FILTER.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (level.getGameTime() % 20 != 0) return;

        boolean wasPowered = isPowered;
        isPowered = checkPower(serverLevel);

        // Питание появилось — запускаем сканирование
        if (isPowered && !wasPowered) {
            startScan(serverLevel);
        }

        // Питание пропало — отключаем зону
        if (!isPowered && wasPowered) {
            removeSelfFromZone(serverLevel);
        }

        // Есть питание, есть зона — потребляем энергию
        if (isPowered && zoneId != null && !isScanning) {
            consumeGeneratorEnergy(serverLevel);
        }

        // ИСПРАВЛЕНИЕ: если зона не создана но питание есть
        // и сканирование не идёт — перезапускаем сканирование
        // Это покрывает случай перезагрузки мира
        if (isPowered && zoneId == null && !isScanning) {
            startScan(serverLevel);
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

    public void startScan(ServerLevel level) {
        if (isScanning) return; // Не запускаем если уже идёт
        isScanning = true;
        scanProgress = 0;

        removeSelfFromZone(level);

        final ServerLevel levelRef = level;
        final BlockPos originPos = this.getBlockPos();
        final int radius = this.scanRadius;

        Thread scanThread = new Thread(() -> {
            try {
                // Прогресс для UI
                for (int i = 0; i <= 90; i += 10) {
                    scanProgress = i;
                    Thread.sleep(200);
                }

                DomeScanTask.ScanResult result = DomeScanTask.scan(levelRef, originPos, radius);

                levelRef.getServer().execute(() -> {
                    DomeZoneSavedData data = DomeZoneSavedData.get(levelRef);

                    DomeZone zone = new DomeZone(UUID.randomUUID());
                    zone.filters.add(originPos);
                    zone.shell.addAll(result.shell);
                    zone.breaches.addAll(result.breaches);
                    zone.volume = result.volume;
                    zone.scanRadius = radius;
                    zone.isScanning = false;

                    data.addZone(zone);
                    zoneId = zone.id;

                    isScanning = false;
                    scanProgress = 100;
                    setChanged();

                    // Отладочное сообщение в лог сервера
                    levelRef.getServer().sendSystemMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "[Thaloria] Scan complete! Shell=" + zone.shell.size() +
                                            " Breaches=" + zone.breaches.size() +
                                            " Volume=" + (int)zone.volume +
                                            " Pressure delta=" + zone.calculatePressureDelta()
                            )
                    );
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
        // НЕ сохраняем isPowered — определяем при каждом тике
        if (zoneId != null) tag.putUUID("zoneId", zoneId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        scanRadius = tag.getInt("scanRadius");
        if (tag.hasUUID("zoneId")) zoneId = tag.getUUID("zoneId");
        // isPowered = false по умолчанию, определится в первом тике
    }
}