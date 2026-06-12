package com.thaloria.block.entity;

import com.thaloria.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class AtmosphereFilterBlockEntity extends BlockEntity {

    public float pressure = 0;
    // Список всех фильтров, которые объединены в одну сеть
    public Set<AtmosphereFilterBlockEntity> network = new HashSet<>();
    public boolean needsRebuild = true;

    // Добавь этот метод в AtmosphereFilterBlockEntity
    public void markDirty() {
        this.needsRebuild = true;
    }

    public AtmosphereFilterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATMOSPHERE_FILTER.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        // Раз в секунду ищем соседей-фильтров
        if (level.getGameTime() % 20 == 0) {
            updateNetwork();
        }

        // Давление растет или падает
        // Теперь мы делим эффективность на размер сети, чтобы не было "читерства"
        if (network.size() > 0) {
            this.pressure = Math.min(100, this.pressure + (network.size() * 0.5f));
        } else {
            this.pressure = Math.max(0, this.pressure - 2);
        }
    }

    private void updateNetwork() {
        network.clear();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(this.getBlockPos());
        visited.add(this.getBlockPos());

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.relative(dir);
                if (!visited.contains(neighborPos)) {
                    BlockEntity be = level.getBlockEntity(neighborPos);
                    if (be instanceof AtmosphereFilterBlockEntity other) {
                        network.add(other);
                        visited.add(neighborPos);
                        queue.add(neighborPos);
                    }
                }
            }
        }
    }
}