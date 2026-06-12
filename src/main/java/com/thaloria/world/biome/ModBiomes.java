package com.thaloria.world.biome;

import com.thaloria.ThaloriaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModBiomes {

    public static final DeferredRegister<Biome> BIOMES =
            DeferredRegister.create(Registries.BIOME, ThaloriaMod.MOD_ID);

    public static final RegistryObject<Biome> THALORIA_BIOME =
            BIOMES.register("thaloria_biome", ThaloriaBiome::create);

}