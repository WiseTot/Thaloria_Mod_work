package com.thaloria.block.entity;

import com.thaloria.dome.DomeZone;
import com.thaloria.dome.DomeZoneSavedData;
import com.thaloria.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class BreachDetectorBlockEntity extends BlockEntity {

    public boolean isPowered    = false;
    public boolean autoMonitor  = false;
    private UUID zoneId         = null;

    public BreachDetectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BREACH_DETECTOR.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (level.getGameTime() % 20 != 0) return;

        isPowered = checkPower(serverLevel);

        if (isPowered) {
            DomeZoneSavedData data = DomeZoneSavedData.get(serverLevel);
            DomeZone zone = data.getZoneContaining(this.getBlockPos());
            zoneId = zone != null ? zone.id : null;
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

    public DomeZone getZone(ServerLevel level) {
        if (zoneId == null) return null;
        return DomeZoneSavedData.get(level).getZone(zoneId);
    }

    public UUID getZoneId() { return zoneId; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("autoMonitor", autoMonitor);
        if (zoneId != null) tag.putUUID("zoneId", zoneId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        autoMonitor = tag.getBoolean("autoMonitor");
        if (tag.hasUUID("zoneId")) zoneId = tag.getUUID("zoneId");
    }
}