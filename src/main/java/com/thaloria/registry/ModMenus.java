package com.thaloria.registry;

import com.thaloria.ThaloriaMod;
import com.thaloria.menu.OxygenChargerMenu;
import com.thaloria.menu.OxygenCompressorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.common.extensions.IForgeMenuType;
import com.thaloria.menu.GeneratorMenu;
import com.thaloria.block.entity.GeneratorBlockEntity;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ThaloriaMod.MOD_ID);

    public static final RegistryObject<MenuType<OxygenCompressorMenu>> OXYGEN_COMPRESSOR =
            MENUS.register("oxygen_compressor",
                    () -> IForgeMenuType.create(OxygenCompressorMenu::new));

    public static final RegistryObject<MenuType<OxygenChargerMenu>> OXYGEN_CHARGER =
            MENUS.register("oxygen_charger",
                    () -> IForgeMenuType.create(OxygenChargerMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    public static final RegistryObject<MenuType<GeneratorMenu>> GENERATOR_MENU =
            MENUS.register("generator_menu",
                    () -> IForgeMenuType.create((windowId, inv, data) -> {

                        BlockPos pos = data.readBlockPos();
                        var level = inv.player.level();
                        var blockEntity = level.getBlockEntity(pos);

                        if (blockEntity instanceof GeneratorBlockEntity generator) {
                            return new GeneratorMenu(windowId, inv, generator);
                        }

                        return null;
                    }));
}