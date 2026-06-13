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

    private static final TagKey<Block> DOME_BLOCKS_TAG = TagKey.create(
            ForgeRegistries.BLOCKS.getRegistryKey(),
            ResourceLocation.fromNamespaceAndPath("thaloria", "dome_blocks")
    );

    private static boolean isDomeBlock(BlockState state) {
        return state.is(DOME_BLOCKS_TAG);
    }

    private static boolean isFullSolidBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        if (state.liquid()) return false;
        return state.isCollisionShapeFullBlock(level, pos);
    }

    private static final TagKey<Block> FLOOR_BLOCKS_TAG = TagKey.create(
            ForgeRegistries.BLOCKS.getRegistryKey(),
            ResourceLocation.fromNamespaceAndPath("thaloria", "dome_floor_blocks")
    );

    private static boolean isSolidFloor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // Блок из тега пола — всегда валиден (пыль, купольное стекло и т.д.)
        if (state.is(FLOOR_BLOCKS_TAG)) return true;

        // Обычный solid блок с 3 слоями ниже — натуральный грунт
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

    // Проверяем блок и только ортогональных соседей (6 направлений)
    // НЕ диагональных — иначе луч "перепрыгивает" через дыру
    private static BlockPos findDomeBlockNear(ServerLevel level, BlockPos center) {
        if (isDomeBlock(level.getBlockState(center))) return center;

        // Только 6 ортогональных соседей (вверх/вниз/влево/вправо/вперёд/назад)
        // Не диагональные — они слишком далеко и дают ложные срабатывания
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
                // Брешь — записываем примерную позицию
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

        for (int i = 1; i <= steps; i++) {
            x += direction.x * step;
            y += direction.y * step;
            z += direction.z * step;
            float distance = (float)(i * step);

            BlockPos pos = BlockPos.containing(x, y, z);

            // Пропускаем если та же позиция что и предыдущий шаг
            if (pos.equals(lastPos)) continue;
            lastPos = pos;

            // Проверяем блок и ортогональных соседей
            BlockPos domeBlock = findDomeBlockNear(level, pos);
            if (domeBlock != null) {
                return new RayResult(true, false, domeBlock, distance);
            }

            // Пол только для лучей идущих вниз
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