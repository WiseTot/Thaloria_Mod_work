package com.thaloria.registry;

import com.thaloria.ThaloriaMod;
import com.thaloria.block.*;
import com.thaloria.block.custom.FungalHeartBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ThaloriaMod.MOD_ID);

    public static final RegistryObject<Block> OXYGEN_CHARGER =
            BLOCKS.register("oxygen_charger",
                    () -> new OxygenChargerBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(3.0F)
                    )
            );

    public static void register(IEventBus eventBus) {
                BLOCKS.register(eventBus);
    }

    public static final RegistryObject<Block> OXYGEN_COMPRESSOR =
            BLOCKS.register("oxygen_compressor",
                    () -> new OxygenCompressorBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(3.0F)
                                    .requiresCorrectToolForDrops()
                    ));

    public static final RegistryObject<Block> ATMOSPHERE_FILTER =
            BLOCKS.register("atmosphere_filter",
                    () -> new AtmosphereFilterBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(3.0F)
                                    .requiresCorrectToolForDrops()
                    ));

    public static final RegistryObject<Block> DOME_GLASS =
            BLOCKS.register("dome_glass",
                    () -> new DomeGlassBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(0.5F)
                                    .noOcclusion()
                                    .sound(SoundType.GLASS)
                    ));

    public static final RegistryObject<Block> GENERATOR =
            BLOCKS.register("generator",
                    () -> new GeneratorBlock(
                            BlockBehaviour.Properties
                                    .copy(Blocks.IRON_BLOCK)
                                    .strength(3.0f)
                                    .requiresCorrectToolForDrops()
                    ));

    public static final RegistryObject<Block> DUST =
            BLOCKS.register("dust",
            () -> new Block(
                    BlockBehaviour.Properties
                    .copy(Blocks.SAND)
                    .strength(0.5f)
                    .sound(SoundType.SAND)
            ));

    public static final RegistryObject<Block> DUST_LAYER =
            BLOCKS.register("dust_layer",
            () -> new SnowLayerBlock(
                    BlockBehaviour.Properties
                    .copy(Blocks.SNOW)
                    .sound(SoundType.SAND)
            ));

    public static final RegistryObject<Block> PETRIFIED_FUNGAL_SHELL = BLOCKS.register("petrified_fungal_shell",
            () -> new Block(BlockBehaviour.Properties
                    .copy(Blocks.DEEPSLATE)
                    .strength(4.0f, 8.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            ));

    public static final RegistryObject<Block> FUNGAL_FLESH = BLOCKS.register("fungal_flesh",
            () -> new Block(BlockBehaviour.Properties
                    .copy(Blocks.MUSHROOM_STEM)
                    .strength(0.8f)
                    .sound(SoundType.FUNGUS)
            ));

    public static final RegistryObject<Block> FUNGAL_HEART = BLOCKS.register("fungal_heart",
            () -> new FungalHeartBlock(BlockBehaviour.Properties
                    .copy(Blocks.AMETHYST_BLOCK)
                    .strength(3.5f, 6.0f)
                    .lightLevel(state -> 8)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops()
            ));
}