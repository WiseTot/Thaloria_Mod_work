package com.thaloria.event;

import com.thaloria.dome.DomeZoneSavedData;
import com.thaloria.ThaloriaMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ThaloriaMod.MOD_ID)
public class WorldLoadHandler {

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        // Пересчитываем бреши при загрузке уровня
        // Блоки могли измениться пока сервер был выключен
        DomeZoneSavedData data = DomeZoneSavedData.get(level);
        data.recalculateAllBreaches(level);
    }
}