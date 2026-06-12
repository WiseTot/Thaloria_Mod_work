package com.thaloria.world.biome;

import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;

public class ThaloriaBiome {

    public static Biome create() {

        BiomeGenerationSettings generation = BiomeGenerationSettings.EMPTY;

        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .fogColor(0x000000)
                .skyColor(0x000000)
                .waterColor(0x1a1a1a)
                .waterFogColor(0x000000)
                .build();

        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.3f)
                .downfall(0f)
                .specialEffects(effects)
                .generationSettings(generation)
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .build();
    }
}