package com.thaloria.dome;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

public class DomeScanTask {

    private static final int RAY_COUNT = 500;

    // Блоки купола
    private static final TagKey<Block> DOME_BLOCKS_TAG = TagKey.create(
            ForgeRegistries.BLOCKS.getRegistryKey(),
            ResourceLocation.fromNamespaceAndPath("thaloria", "dome_blocks")
    );

    // Блоки пола — пыль Тхалории, купольное стекло и т.д.
    private static final TagKey<Block> FLOOR_BLOCKS_TAG = TagKey.create(
            ForgeRegistries.BLOCKS.getRegistryKey(),
            ResourceLocation.fromNamespaceAndPath("thaloria", "dome_floor_blocks")
    );

    private static boolean isDomeBlock(BlockState state) {
        return state.is(DOME_BLOCKS_TAG);
    }

    private static boolean isFloorBlock(BlockState state) {
        return state.is(FLOOR_BLOCKS_TAG);
    }

    private static boolean isFullSolidBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        if (state.liquid()) return false;
        return state.isCollisionShapeFullBlock(level, pos);
    }

    private static boolean isSolidFloor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // Блок купола как пол — одного слоя достаточно
        if (isDomeBlock(state)) return true;

        // Блоки тега пола (пыль, dust_layer и т.д.) — валидный пол
        if (isFloorBlock(state)) return true;

        // Обычный solid блок — проверяем 3 ниже
        if (state.isAir() || state.liquid()) return false;
        if (!state.isCollisionShapeFullBlock(level, pos)) return false;

        for (int i = 1; i <= 3; i++) {
            BlockPos below = pos.below(i);
            BlockState belowState = level.getBlockState(below);
            if (belowState.isAir() || belowState.liquid()) return false;
            if (!belowState.isCollisionShapeFullBlock(level, below)) return false;
        }
        return true;
    }

    // Единый метод — проверяем блок и всех 26 соседей в кубе 3×3×3
    // Но только те что ДАЛЬШЕ от фильтра чем текущая точка
    // Это не даёт прыгать через реальные дыры
    private static BlockPos findDomeBlockNearby(ServerLevel level, BlockPos center,
                                                BlockPos origin) {
        if (isDomeBlock(level.getBlockState(center))) return center;

        double originDist = Math.sqrt(
                Math.pow(center.getX() - origin.getX(), 2) +
                        Math.pow(center.getY() - origin.getY(), 2) +
                        Math.pow(center.getZ() - origin.getZ(), 2)
        );

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos n = center.offset(dx, dy, dz);

                    // Сосед должен быть НЕ ближе к фильтру чем текущая точка
                    // Это предотвращает "прыжок назад" через дыру
                    double neighborDist = Math.sqrt(
                            Math.pow(n.getX() - origin.getX(), 2) +
                                    Math.pow(n.getY() - origin.getY(), 2) +
                                    Math.pow(n.getZ() - origin.getZ(), 2)
                    );

                    if (neighborDist >= originDist - 1.5
                            && isDomeBlock(level.getBlockState(n))) {
                        return n;
                    }
                }
            }
        }
        return null;
    }

    public static ScanResult scan(ServerLevel level, BlockPos origin, int radius) {

        Set<BlockPos> shellBlocks = new HashSet<>();
        Set<BlockPos> breaches = new HashSet<>();
        int hitRays = 0;
        float totalDistance = 0f;

        double goldenRatio = (1 + Math.sqrt(5)) / 2;

        for (int i = 0; i < RAY_COUNT; i++) {
            double theta = 2 * Math.PI * i / goldenRatio;
            double phi = Math.acos(1 - 2 * (i + 0.5) / RAY_COUNT);

            double dx = Math.sin(phi) * Math.cos(theta);
            double dy = Math.cos(phi);
            double dz = Math.sin(phi) * Math.sin(theta);

            Vec3 direction = new Vec3(dx, dy, dz).normalize();
            RayResult result = castRay(level, origin, direction, radius);

            if (result.hitDome || result.hitFloor) {
                if (result.hitPos != null) shellBlocks.add(result.hitPos);
                hitRays++;
                totalDistance += result.distance;
            } else {
                BlockPos expectedPos = BlockPos.containing(
                        origin.getX() + direction.x * radius,
                        origin.getY() + direction.y * radius,
                        origin.getZ() + direction.z * radius
                );
                breaches.add(expectedPos);
            }
        }

        float avgRadius = hitRays > 0 ? totalDistance / hitRays : 1f;
        float estimatedVolume = Math.max(1f,
                (float)(1.33f * Math.PI * avgRadius * avgRadius * avgRadius));

        return new ScanResult(shellBlocks, breaches, estimatedVolume);
    }

    private static RayResult castRay(ServerLevel level, BlockPos origin,
                                     Vec3 direction, int maxDistance) {
        double x = origin.getX() + 0.5;
        double y = origin.getY() + 0.5;
        double z = origin.getZ() + 0.5;

        // Уменьшаем шаг до 0.2 — точнее для диагональных поверхностей
        double step = 0.2;
        int steps = (int)(maxDistance / step);
        BlockPos lastPos = null;
        boolean goingDown = direction.y < -0.3;

        for (int i = 1; i <= steps; i++) {
            x += direction.x * step;
            y += direction.y * step;
            z += direction.z * step;
            float distance = (float)(i * step);

            BlockPos pos = BlockPos.containing(x, y, z);
            if (pos.equals(lastPos)) continue;
            lastPos = pos;

            // Проверяем блок и соседей с учётом направления от фильтра
            BlockPos domeBlock = findDomeBlockNearby(level, pos, origin);
            if (domeBlock != null) {
                return new RayResult(true, false, domeBlock, distance);
            }

            // Пол только для лучей летящих вниз
            if (goingDown) {
                for (int up = 0; up <= 3; up++) {
                    BlockPos checkPos = pos.above(up);
                    if (isSolidFloor(level, checkPos)) {
                        return new RayResult(false, true, checkPos, distance);
                    }
                }
            }
        }

        return new RayResult(false, false, null, maxDistance);
    }

    public static class RayResult {
        public final boolean hitDome;
        public final boolean hitFloor;
        public final BlockPos hitPos;
        public final float distance;

        public RayResult(boolean hitDome, boolean hitFloor,
                         BlockPos hitPos, float distance) {
            this.hitDome = hitDome;
            this.hitFloor = hitFloor;
            this.hitPos = hitPos;
            this.distance = distance;
        }
    }

    public static class ScanResult {
        public final Set<BlockPos> shell;
        public final Set<BlockPos> breaches;
        public final float volume;

        public ScanResult(Set<BlockPos> shell, Set<BlockPos> breaches, float volume) {
            this.shell = shell;
            this.breaches = breaches;
            this.volume = volume;
        }
    }
}