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

    private static BlockPos findDomeBlockOrtho(ServerLevel level, BlockPos center) {
        if (isDomeBlock(level.getBlockState(center))) return center;

        BlockPos[] neighbors = {
                center.above(), center.below(),
                center.north(), center.south(),
                center.east(),  center.west()
        };
        for (BlockPos n : neighbors) {
            if (isDomeBlock(level.getBlockState(n))) return n;
        }
        return null;
    }


    // Для вертикальных лучей (вверх) — все 26 соседей в кубе 3×3×3
    // Нужно для ступенчатой верхушки сферы
    private static BlockPos findDomeBlockFull(ServerLevel level, BlockPos center) {
        if (isDomeBlock(level.getBlockState(center))) return center;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos n = center.offset(dx, dy, dz);
                    if (isDomeBlock(level.getBlockState(n))) return n;
                }
            }
        }
        return null;
    }


    // Проверяем блок и 6 ортогональных соседей
    private static BlockPos findDomeBlockNear(ServerLevel level, BlockPos center) {
        if (isDomeBlock(level.getBlockState(center))) return center;

        BlockPos[] neighbors = {
                center.above(), center.below(),
                center.north(), center.south(),
                center.east(),  center.west()
        };

        for (BlockPos neighbor : neighbors) {
            if (isDomeBlock(level.getBlockState(neighbor))) return neighbor;
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

        double step = 0.3;
        int steps = (int)(maxDistance / step);
        BlockPos lastPos = null;

        // Определяем тип луча — вертикальный или горизонтальный
        boolean isVertical = Math.abs(direction.y) > 0.5;

        for (int i = 1; i <= steps; i++) {
            x += direction.x * step;
            y += direction.y * step;
            z += direction.z * step;
            float distance = (float)(i * step);

            BlockPos pos = BlockPos.containing(x, y, z);
            if (pos.equals(lastPos)) continue;
            lastPos = pos;

            // Вертикальные лучи — полная проверка соседей
            // Горизонтальные — только ортогональные (чтобы не прыгать через дыры)
            BlockPos domeBlock = isVertical
                    ? findDomeBlockFull(level, pos)
                    : findDomeBlockOrtho(level, pos);

            if (domeBlock != null) {
                return new RayResult(true, false, domeBlock, distance);
            }

            // Пол только для лучей летящих вниз
            if (direction.y < -0.3) {
                if (isSolidFloor(level, pos)) {
                    return new RayResult(false, true, pos, distance);
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