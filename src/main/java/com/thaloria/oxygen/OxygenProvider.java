package com.thaloria.oxygen;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OxygenProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final Capability<IOxygenData> OXYGEN_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final IOxygenData instance = new OxygenData();
    private final LazyOptional<IOxygenData> optional = LazyOptional.of(() -> instance);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == OXYGEN_CAP ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("oxygen", instance.getOxygen());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        instance.setOxygen(nbt.getInt("oxygen"));
    }
}