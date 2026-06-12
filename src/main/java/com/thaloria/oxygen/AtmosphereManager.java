package com.thaloria.oxygen;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.thaloria.ThaloriaMod;

import java.util.HashMap;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ThaloriaMod.MOD_ID)
public class AtmosphereManager {

    private static final HashMap<UUID, Integer> breath = new HashMap<>();
    private static final int MAX_BREATH = 40;

    public static int getBreath(ServerPlayer player) {
        return breath.getOrDefault(player.getUUID(), MAX_BREATH);
    }

    public static void setBreath(ServerPlayer player, int value) {
        breath.put(player.getUUID(), Math.max(0, Math.min(MAX_BREATH, value)));
    }

    public static void removeBreath(ServerPlayer player, int amount) {
        setBreath(player, getBreath(player) - amount);
    }

    public static void restoreBreath(ServerPlayer player) {
        setBreath(player, MAX_BREATH);
    }

    public static void addBreath(ServerPlayer player, int amount) {
        setBreath(player, getBreath(player) + amount);
    }

    // Очищаем запись когда игрок выходит — убираем утечку памяти
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        breath.remove(event.getEntity().getUUID());
    }

    // Давление хранится в PersistentData — сохраняется при рестарте
    private static final String PRESSURE_TAG = "thaloria_pressure";

    public static float getPressure(ServerPlayer player) {
        return player.getPersistentData().getFloat(PRESSURE_TAG);
    }

    public static void setPressure(ServerPlayer player, float value) {
        player.getPersistentData().putFloat(PRESSURE_TAG,
                Math.max(0f, Math.min(100f, value)));
    }
}