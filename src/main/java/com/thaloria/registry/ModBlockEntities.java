package com.thaloria.registry;

import com.thaloria.ThaloriaMod;
import com.thaloria.block.entity.AtmosphereFilterBlockEntity;
import com.thaloria.block.entity.OxygenChargerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.thaloria.block.entity.OxygenCompressorBlockEntity;
import com.thaloria.block.entity.GeneratorBlockEntity;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ThaloriaMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<OxygenChargerBlockEntity>> OXYGEN_CHARGER =
            BLOCK_ENTITIES.register("oxygen_charger",
                    () -> BlockEntityType.Builder.of(
                            OxygenChargerBlockEntity::new,
                            ModBlocks.OXYGEN_CHARGER.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

    public static final RegistryObject<BlockEntityType<OxygenCompressorBlockEntity>> OXYGEN_COMPRESSOR =
            BLOCK_ENTITIES.register("oxygen_compressor",
                    () -> BlockEntityType.Builder.of(
                            OxygenCompressorBlockEntity::new,
                            ModBlocks.OXYGEN_COMPRESSOR.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<GeneratorBlockEntity>> GENERATOR =
            BLOCK_ENTITIES.register("generator",
                    () -> BlockEntityType.Builder.of(
                            GeneratorBlockEntity::new,
                            ModBlocks.GENERATOR.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<AtmosphereFilterBlockEntity>> ATMOSPHERE_FILTER =
            BLOCK_ENTITIES.register("atmosphere_filter",
                    () -> BlockEntityType.Builder.of(
                            AtmosphereFilterBlockEntity::new,
                            ModBlocks.ATMOSPHERE_FILTER.get()
                    ).build(null));
}