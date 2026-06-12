package com.thaloria;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = ThaloriaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue DAMAGE_INTERVAL = BUILDER
            .comment("Ticks between atmosphere damage (20 ticks = 1 second)")
            .defineInRange("damageInterval", 60, 20, 600);

    public static final ForgeConfigSpec.DoubleValue DAMAGE_AMOUNT = BUILDER
            .comment("Damage dealt per interval")
            .defineInRange("damageAmount", 1.0, 0.0, 20.0);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int damageInterval;
    public static double damageAmount;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        damageInterval = DAMAGE_INTERVAL.get();
        damageAmount = DAMAGE_AMOUNT.get();
    }
}