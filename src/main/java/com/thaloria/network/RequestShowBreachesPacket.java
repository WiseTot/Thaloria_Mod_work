package com.thaloria.network;

import com.thaloria.block.entity.AtmosphereFilterBlockEntity;
import com.thaloria.dome.DomeZone;
import com.thaloria.dome.DomeZoneSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.function.Supplier;

public class RequestShowBreachesPacket {

    private final BlockPos filterPos;

    public RequestShowBreachesPacket(BlockPos pos) {
        this.filterPos = pos;
    }

    public static void encode(RequestShowBreachesPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.filterPos);
    }

    public static RequestShowBreachesPacket decode(FriendlyByteBuf buf) {
        return new RequestShowBreachesPacket(buf.readBlockPos());
    }

    public static void handle(RequestShowBreachesPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            if (!(level.getBlockEntity(packet.filterPos)
                    instanceof AtmosphereFilterBlockEntity filter)) return;

            DomeZone zone = filter.getZone(level);
            if (zone == null || zone.breaches.isEmpty()) return;

            // Отправляем позиции всех брешей клиенту
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new ShowBreachesPacket(new ArrayList<>(zone.breaches))
            );
        });
        ctx.get().setPacketHandled(true);
    }
}