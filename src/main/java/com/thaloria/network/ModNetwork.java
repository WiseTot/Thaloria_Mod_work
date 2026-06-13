package com.thaloria.network;

import com.thaloria.ThaloriaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ModNetwork {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ThaloriaMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, FilterRadiusPacket.class,
                FilterRadiusPacket::encode,
                FilterRadiusPacket::decode,
                FilterRadiusPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, FilterScanPacket.class,
                FilterScanPacket::encode,
                FilterScanPacket::decode,
                FilterScanPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, RequestFilterDataPacket.class,
                RequestFilterDataPacket::encode,
                RequestFilterDataPacket::decode,
                RequestFilterDataPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, OpenFilterScreenPacket.class,
                OpenFilterScreenPacket::encode,
                OpenFilterScreenPacket::decode,
                OpenFilterScreenPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, ShowBreachesPacket.class,
                ShowBreachesPacket::encode,
                ShowBreachesPacket::decode,
                ShowBreachesPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, RequestShowBreachesPacket.class,
                RequestShowBreachesPacket::encode,
                RequestShowBreachesPacket::decode,
                RequestShowBreachesPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, TeleportToBreachPacket.class,
                TeleportToBreachPacket::encode,
                TeleportToBreachPacket::decode,
                TeleportToBreachPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}