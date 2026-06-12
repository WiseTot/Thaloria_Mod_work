package com.thaloria.world.feature;

import com.thaloria.ThaloriaMod;
import com.thaloria.worldgen.features.SphereDungeonFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, ThaloriaMod.MODID);

    public static final RegistryObject<Feature<?>> SPHERE_DUNGEON =
            FEATURES.register("sphere_dungeon", SphereDungeonFeature::new);

}