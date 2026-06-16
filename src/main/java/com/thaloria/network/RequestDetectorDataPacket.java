package com.thaloria.network;

import com.thaloria.block.entity.BreachDetectorBlockEntity;
import com.thaloria.dome.DomeZone;
import com.thaloria.dome.DomeZoneSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class RequestDetectorDataPacket {

    private final BlockPos detectorPos;

    public RequestDetectorDataPacket(BlockPos pos) {
        this.detectorPos = pos;
    }

    public static void encode(RequestDetectorDataPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.detectorPos);
    }

    public static RequestDetectorDataPacket decode(FriendlyByteBuf buf) {
        return new RequestDetectorDataPacket(buf.readBlockPos());
    }

    public static void handle(RequestDetectorDataPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            if (!(level.getBlockEntity(packet.detectorPos)
                    instanceof BreachDetectorBlockEntity detector)) return;

            DomeZone zone = detector.getZone(level);
            int breachCount  = zone != null ? zone.breaches.size()       : 0;
            int shellCount   = zone != null ? zone.originalShell.size()  : 0;
            float pressure   = zone != null ? zone.pressure              : 0f;
            boolean hasZone  = zone != null;

            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenBreachDetectorPacket(
                            packet.detectorPos, detector.isPowered,
                            detector.autoMonitor, breachCount,
                            shellCount, pressure, hasZone
                    )
            );
        });
        ctx.get().setPacketHandled(true);
    }
}