package com.thaloria.oxygen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.thaloria.ThaloriaMod;

@Mod.EventBusSubscriber(modid = ThaloriaMod.MOD_ID)
public class ModCapabilities {

    private static final ResourceLocation OXYGEN_ID =
            new ResourceLocation(ThaloriaMod.MOD_ID, "oxygen");

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Player> event) {
        event.addCapability(OXYGEN_ID, new OxygenProvider());
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(OxygenProvider.OXYGEN_CAP).ifPresent(oldStore -> {
            event.getEntity().getCapability(OxygenProvider.OXYGEN_CAP).ifPresent(newStore -> {
                newStore.setOxygen(oldStore.getOxygen());
            });
        });
    }
}