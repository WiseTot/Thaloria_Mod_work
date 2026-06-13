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

import java.util.function.Supplier;

// Клиент запрашивает свежие данные фильтра пока экран открыт
public class RequestFilterDataPacket {

    private final BlockPos filterPos;

    public RequestFilterDataPacket(BlockPos pos) {
        this.filterPos = pos;
    }

    public static void encode(RequestFilterDataPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.filterPos);
    }

    public static RequestFilterDataPacket decode(FriendlyByteBuf buf) {
        return new RequestFilterDataPacket(buf.readBlockPos());
    }

    public static void handle(RequestFilterDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            if (!(level.getBlockEntity(packet.filterPos)
                    instanceof AtmosphereFilterBlockEntity filter)) return;

            DomeZone zone = filter.getZone(level);
            int shellCount     = zone != null ? zone.shell.size()     : 0;
            int breachCount    = zone != null ? zone.breaches.size()  : 0;
            float pressure     = zone != null ? zone.pressure         : 0f;
            float pressureDelta= zone != null ? zone.calculatePressureDelta() : 0f;
            int filterCount    = zone != null ? zone.filters.size()   : 0;

            // Отправляем обновление обратно клиенту
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenFilterScreenPacket(
                            packet.filterPos, filter.scanRadius, filter.isPowered,
                            filter.isScanning, filter.scanProgress,
                            shellCount, breachCount, pressure, pressureDelta, filterCount
                    )
            );
        });
        ctx.get().setPacketHandled(true);
    }
}