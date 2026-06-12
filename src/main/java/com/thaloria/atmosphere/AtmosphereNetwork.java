package com.thaloria.atmosphere;

import com.thaloria.block.entity.AtmosphereFilterBlockEntity;
import net.minecraft.core.BlockPos;
import java.util.HashSet;
import java.util.Set;

public class AtmosphereNetwork {
    public Set<AtmosphereFilterBlockEntity> filters = new HashSet<>();
    public float pressure = 0;
    public int airVolume = 0;

    public void update() {
        // Логика давления для всей сети сразу
        if (filters.isEmpty()) return;

        // Чем больше фильтров, тем выше эффективность (напр. +2 за каждый фильтр)
        float production = filters.size() * 2.0f;
        pressure = Math.min(100, pressure + (production / 20.0f));
    }
}