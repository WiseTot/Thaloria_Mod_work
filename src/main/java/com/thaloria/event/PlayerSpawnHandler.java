package com.thaloria.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;

public class PlayerSpawnHandler {

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Проверяем: был ли игрок уже телепортирован
        if (player.getPersistentData().getBoolean("thaloria_spawned")) return;

        ResourceKey<Level> THALORIA_DIM = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("thaloria", "thaloria_planet")
        );

        ServerLevel level = player.server.getLevel(THALORIA_DIM);

        if (level == null) return;

        player.teleportTo(level, 0, 100, 0, player.getYRot(), player.getXRot());

        // Сохраняем что телепорт уже был
        player.getPersistentData().putBoolean("thaloria_spawned", true);
    }
}