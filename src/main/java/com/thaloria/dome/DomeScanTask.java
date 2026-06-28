package com.thaloria.dome;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class DomeScanTask {

    private static final int RAY_COUNT = 500;

    private static final TagKey<Block> DOME_BLOCKS_TAG = TagKey.create(
            ForgeRegistries.BLOCKS.getRegistryKey(),
            ResourceLocation.fromNamespaceAndPath("thaloria", "dome_blocks")
    );

    private static final TagKey<Block> FLOOR_BLOCKS_TAG = TagKey.create(
            ForgeRegistries.BLOCKS.getRegistryKey(),
            ResourceLocation.fromNamespaceAndPath("thaloria", "dome_floor_blocks")
    );

    public static boolean isDomeBlock(BlockState state) {
        return state.is(DOME_BLOCKS_TAG);
    }

    private static boolean isFloorBlock(BlockState state) {
        return state.is(FLOOR_BLOCKS_TAG);
    }

    private static boolean isSolidFloor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isDomeBlock(state)) return true;
        if (isFloorBlock(state)) return true;
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

    private static BlockPos findDomeBlockNearby(ServerLevel level, BlockPos center,
                                                BlockPos origin) {
        if (isDomeBlock(level.getBlockState(center))) return center;

        double originDist = distance(center, origin);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos n = center.offset(dx, dy, dz);
                    double neighborDist = distance(n, origin);
                    if (neighborDist >= originDist - 1.5
                            && isDomeBlock(level.getBlockState(n))) {
                        return n;
                    }
                }
            }
        }
        return null;
    }

    private static double distance(BlockPos a, BlockPos b) {
        return Math.sqrt(
                Math.pow(a.getX() - b.getX(), 2) +
                        Math.pow(a.getY() - b.getY(), 2) +
                        Math.pow(a.getZ() - b.getZ(), 2)
        );
    }

    /**
     * Проверка герметичности через flood fill.
     * Запускаем BFS от origin по воздушным блокам.
     * Если дошли до границы радиуса — есть выход наружу → не герметичен.
     * Если flood fill завершился внутри — купол герметичен.
     */
    private static boolean isHermetic(ServerLevel level, BlockPos origin, int radius) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(origin);
        visited.add(origin);

        int[] ddx = {1, -1, 0, 0, 0, 0};
        int[] ddy = {0, 0, 1, -1, 0, 0};
        int[] ddz = {0, 0, 0, 0, 1, -1};

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();

            // Если дошли до границы радиуса — утечка наружу
            if (Math.abs(pos.getX() - origin.getX()) >= radius ||
                    Math.abs(pos.getY() - origin.getY()) >= radius ||
                    Math.abs(pos.getZ() - origin.getZ()) >= radius) {
                return false;
            }

            for (int i = 0; i < 6; i++) {
                BlockPos next = pos.offset(ddx[i], ddy[i], ddz[i]);
                if (visited.contains(next)) continue;
                visited.add(next);

                BlockState state = level.getBlockState(next);

                // Стена — блок купола или твёрдый пол, не проходим
                if (isDomeBlock(state)) continue;
                if (isSolidFloor(level, next)) continue;

                // Воздух или другой нетвёрдый блок — идём дальше
                queue.add(next);
            }
        }

        // Flood fill завершился не достигнув границы — герметично
        return true;
    }

    public static ScanResult scan(ServerLevel level, BlockPos origin, int radius) {

        Set<BlockPos> shellBlocks = new HashSet<>();
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
                hitRays++;
                totalDistance += result.distance;
                if (result.hitPos != null) shellBlocks.add(result.hitPos);
            }
        }

        float avgRadius = hitRays > 0 ? totalDistance / hitRays : 1f;
        float estimatedVolume = Math.max(1f,
                (float)(1.33f * Math.PI * avgRadius * avgRadius * avgRadius));

        // Герметичность — flood fill от origin
        boolean isSealed = isHermetic(level, origin, radius);

        // DEBUG — убери когда всё заработает
        level.getServer().sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§e[SEAL CHECK] isSealed=" + isSealed +
                                " shellBlocks=" + shellBlocks.size()));

        return new ScanResult(shellBlocks, estimatedVolume, isSealed);
    }

    private static RayResult castRay(ServerLevel level, BlockPos origin,
                                     Vec3 direction, int maxDistance) {
        double x = origin.getX() + 0.5;
        double y = origin.getY() + 0.5;
        double z = origin.getZ() + 0.5;

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

            BlockPos domeBlock = findDomeBlockNearby(level, pos, origin);
            if (domeBlock != null) {
                return new RayResult(true, false, domeBlock, distance);
            }

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
        public final float volume;
        public final boolean isSealed;

        public ScanResult(Set<BlockPos> shell, float volume, boolean isSealed) {
            this.shell    = shell;
            this.volume   = volume;
            this.isSealed = isSealed;
        }
    }

    public static Set<BlockPos> collectAllDomeBlocks(ServerLevel level,
                                                     BlockPos origin, int radius) {
        Set<BlockPos> domeBlocks = new HashSet<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x*x + y*y + z*z > radius * radius) continue;
                    BlockPos pos = origin.offset(x, y, z);
                    if (isDomeBlock(level.getBlockState(pos))) {
                        domeBlocks.add(pos);
                    }
                }
            }
        }
        return domeBlocks;
    }

    public static class SealCheckResult {
        public final boolean isSealed;
        public final int sealedHits;

        public SealCheckResult(boolean isSealed, int hits) {
            this.isSealed = isSealed;
            this.sealedHits = hits;
        }
    }

    /**
     * Проверка герметичности для автомониторинга в PlayerAtmosphereHandler.
     * Использует тот же flood fill что и scan.
     */
    public static SealCheckResult checkSealed(ServerLevel level,
                                              BlockPos origin, int radius) {
        boolean isSealed = isHermetic(level, origin, radius);
        return new SealCheckResult(isSealed, 0);
    }
}