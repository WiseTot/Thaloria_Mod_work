package com.thaloria.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.thaloria.weather.DustStormManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public class WeatherCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("weather")
                        .requires(src -> src.hasPermission(2)) // оп-уровень 2
                        .then(Commands.literal("duststorm")
                                .executes(ctx -> startDustStorm(ctx.getSource(), 6000)) // 5 минут по умолчанию
                                .then(Commands.argument("duration", IntegerArgumentType.integer(20, 72000))
                                        .executes(ctx -> startDustStorm(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "duration")
                                        ))
                                )
                        )
                        .then(Commands.literal("clear")
                                .executes(ctx -> clearWeather(ctx.getSource()))
                        )
        );
    }

    private static int startDustStorm(CommandSourceStack source, int durationTicks) {
        ServerLevel level = source.getLevel();

        if (!level.dimension().location()
                .equals(new ResourceLocation("thaloria", "thaloria_planet"))) {
            source.sendFailure(Component.literal(
                    "§c[Thaloria] Dust storms only work in the Thaloria dimension!"));
            return 0;
        }

        DustStormManager.startStormManual(level, durationTicks);

        int seconds = durationTicks / 20;
        source.sendSuccess(() -> Component.literal(
                        "§6[Thaloria] Dust storm started! Duration: " + seconds + "s (" + durationTicks + " ticks)"),
                true);
        return 1;
    }

    private static int clearWeather(CommandSourceStack source) {
        ServerLevel level = source.getLevel();

        if (!DustStormManager.isStormActive) {
            source.sendFailure(Component.literal("§7[Thaloria] No active storm to clear."));
            return 0;
        }

        DustStormManager.stopStormManual(level);
        source.sendSuccess(() -> Component.literal("§a[Thaloria] Weather cleared."), true);
        return 1;
    }
}