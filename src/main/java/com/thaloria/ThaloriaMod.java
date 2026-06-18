package com.thaloria;

import com.mojang.logging.LogUtils;
import com.thaloria.client.render.DustStormRenderer;
import com.thaloria.client.render.PressureGogglesRenderer;
import com.thaloria.client.render.ThaloriaSkyRenderer;
import com.thaloria.command.WeatherCommand;
import com.thaloria.event.PlayerAtmosphereHandler;
import com.thaloria.event.PressureGogglesHandler;
import com.thaloria.registry.ModBlockEntities;
import com.thaloria.registry.ModBlocks;
import com.thaloria.registry.ModItems;
import com.thaloria.registry.ModMenus;
import com.thaloria.screen.OxygenChargerScreen;
import com.thaloria.screen.OxygenCompressorScreen;
import com.thaloria.weather.DustStormManager;
import com.thaloria.world.feature.ModFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.thaloria.event.DomemkrEvent;
import com.thaloria.event.PlayerSpawnHandler;
import com.thaloria.world.biome.ModBiomes;
import com.thaloria.network.ModNetwork;
import org.slf4j.Logger;

@Mod(ThaloriaMod.MOD_ID)
public class ThaloriaMod {

    public static final String MODID  = "thaloria";
    public static final String MOD_ID = "thaloria";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ThaloriaMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModNetwork.register();

        ModBiomes.BIOMES.register(modEventBus);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(new PlayerAtmosphereHandler());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new DomemkrEvent());
        MinecraftForge.EVENT_BUS.register(new PlayerSpawnHandler());
        MinecraftForge.EVENT_BUS.register(new PressureGogglesHandler());

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModFeatures.FEATURES.register(bus);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MinecraftForge.EVENT_BUS.register(ThaloriaSkyRenderer.class);
            MinecraftForge.EVENT_BUS.register(DustStormRenderer.class);
            MinecraftForge.EVENT_BUS.register(PressureGogglesRenderer.class);

            MenuScreens.register(ModMenus.OXYGEN_CHARGER.get(), OxygenChargerScreen::new);
            MenuScreens.register(ModMenus.OXYGEN_COMPRESSOR.get(), OxygenCompressorScreen::new);

            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.DOME_GLASS.get(), RenderType.translucent());

            MenuScreens.register(ModMenus.GENERATOR_MENU.get(),
                    com.thaloria.screen.GeneratorScreen::new);

            Minecraft.getInstance().options.cloudStatus()
                    .set(net.minecraft.client.CloudStatus.OFF);
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        WeatherCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.dimension().location()
                    .equals(new ResourceLocation("thaloria", "thaloria_planet"))) {
                DustStormManager.tick(level);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DustStormManager.syncToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        DimensionSpecialEffects effects =
                new DimensionSpecialEffects(Float.NaN, false,
                        DimensionSpecialEffects.SkyType.NONE, false, false) {
                    @Override
                    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float sunHeight) {
                        return fogColor;
                    }
                    @Override
                    public boolean isFoggyAt(int x, int y) { return false; }
                };
        event.register(new ResourceLocation("thaloria", "thaloria_sky"), effects);
    }
}