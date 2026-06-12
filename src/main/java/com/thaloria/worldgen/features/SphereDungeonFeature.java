package com.thaloria.worldgen.features; // Проверь, совпадает ли путь с твоей папкой

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class SphereDungeonFeature extends Feature<NoneFeatureConfiguration> {

    public SphereDungeonFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        System.out.println("Шар заспавнился в: " + origin.toShortString());

        int radius = 8 + random.nextInt(5); // Размер шара от 8 до 12

        // Генерируем сферу
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    int distSq = x * x + y * y + z * z;

                    // Если точка внутри сферы
                    if (distSq <= radius * radius) {
                        BlockPos pos = origin.offset(x, y, z);

                        // Если это внешний слой (оболочка)
                        if (distSq > (radius - 1) * (radius - 1)) {
                            level.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
                        }
                        // Если внутри
                        else {
                            // 1% шанс поставить фонарь вместо пустоты
                            if (random.nextInt(100) == 0) {
                                level.setBlock(pos, Blocks.SEA_LANTERN.defaultBlockState(), 3);
                            } else {
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}