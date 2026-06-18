package com.thaloria.registry;

import com.thaloria.ThaloriaMod;
import com.thaloria.item.PressureGogglesItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.thaloria.item.OxygenCanisterItem;
import net.minecraft.world.item.BlockItem;
import com.thaloria.item.DomemkrItem;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ThaloriaMod.MOD_ID);

    public static final RegistryObject<Item> ATMOSPHERE_HELMET =
            ITEMS.register("atmosphere_helmet",
                    () -> new com.thaloria.suit.AtmosphereArmorItem(
                            ArmorMaterials.IRON,
                            ArmorItem.Type.HELMET,
                            new Item.Properties().stacksTo(1)
                    ));

    public static final RegistryObject<Item> ATMOSPHERE_CHESTPLATE =
            ITEMS.register("atmosphere_chestplate",
                    () -> new com.thaloria.suit.AtmosphereArmorItem(
                            ArmorMaterials.IRON,
                            ArmorItem.Type.CHESTPLATE,
                            new Item.Properties().stacksTo(1)
                    ));

    public static final RegistryObject<Item> ATMOSPHERE_LEGGINGS =
            ITEMS.register("atmosphere_leggings",
                    () -> new com.thaloria.suit.AtmosphereArmorItem(
                            ArmorMaterials.IRON,
                            ArmorItem.Type.LEGGINGS,
                            new Item.Properties().stacksTo(1)
                    ));

    public static final RegistryObject<Item> ATMOSPHERE_BOOTS =
            ITEMS.register("atmosphere_boots",
                    () -> new com.thaloria.suit.AtmosphereArmorItem(
                            ArmorMaterials.IRON,
                            ArmorItem.Type.BOOTS,
                            new Item.Properties().stacksTo(1)
                    ));

    public static final RegistryObject<Item> OXYGEN_CHARGER_ITEM =
            ITEMS.register("oxygen_charger",
                    () -> new BlockItem(
                            ModBlocks.OXYGEN_CHARGER.get(),
                            new Item.Properties()
                    ));

    public static final RegistryObject<Item> OXYGEN_CANISTER =
            ITEMS.register("oxygen_canister",
                    () -> new OxygenCanisterItem(
                            new Item.Properties().stacksTo(1)
                    ));

    public static final RegistryObject<Item> OXYGEN_COMPRESSOR =
            ITEMS.register("oxygen_compressor",
                    () -> new BlockItem(
                            ModBlocks.OXYGEN_COMPRESSOR.get(),
                            new Item.Properties()
                    ));

    public static final RegistryObject<Item> ATMOSPHERE_FILTER =
            ITEMS.register("atmosphere_filter",
                    () -> new BlockItem(
                            ModBlocks.ATMOSPHERE_FILTER.get(),
                            new Item.Properties()
                    ));

    public static final RegistryObject<Item> DOME_GLASS =
            ITEMS.register("dome_glass",
                    () -> new BlockItem(
                            ModBlocks.DOME_GLASS.get(),
                            new Item.Properties()
                    ));

    public static final RegistryObject<Item> DOMEMKR =
            ITEMS.register("domemkr",
                    () -> new DomemkrItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GENERATOR =
            ITEMS.register("generator",
                    () -> new BlockItem(
                            ModBlocks.GENERATOR.get(),
                            new Item.Properties()
                    ));

    public static final RegistryObject<Item> DUST_ITEM =
            ITEMS.register("dust",
                    () -> new BlockItem(ModBlocks.DUST.get(),
                            new Item.Properties()));

    public static final RegistryObject<Item> DUST_LAYER_ITEM =
            ITEMS.register("dust_layer",
                    () -> new BlockItem(ModBlocks.DUST_LAYER.get(),
                            new Item.Properties()));

    public static final RegistryObject<Item> PETRIFIED_FUNGAL_SHELL =
            ITEMS.register("petrified_fungal_shell",
                    () -> new BlockItem(ModBlocks.PETRIFIED_FUNGAL_SHELL.get(),
                            new Item.Properties()));

    public static final RegistryObject<Item> FUNGAL_FLESH =
            ITEMS.register("fungal_flesh",
                    () -> new BlockItem(ModBlocks.FUNGAL_FLESH.get(),
                            new Item.Properties()));

    public static final RegistryObject<Item> FUNGAL_HEART =
            ITEMS.register("fungal_heart",
                    () -> new BlockItem(ModBlocks.FUNGAL_HEART.get(),
                            new Item.Properties()));

    public static final RegistryObject<Item> BREACH_DETECTOR =
            ITEMS.register("breach_detector",
                    () -> new BlockItem(ModBlocks.BREACH_DETECTOR.get(),
                            new Item.Properties()
                    ));

    // Очки давления — для Creative тестирования
    public static final RegistryObject<Item> PRESSURE_GOGGLES =
            ITEMS.register("pressure_goggles",
                    () -> new PressureGogglesItem(
                            new Item.Properties().stacksTo(1)
                    ));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}