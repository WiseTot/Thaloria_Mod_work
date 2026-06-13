package com.thaloria.network;

import com.thaloria.block.entity.AtmosphereFilterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Пакет от клиента к серверу — игрок изменил радиус сканирования
public class FilterRadiusPacket {

    private final BlockPos filterPos;
    private final int newRadius;

    public FilterRadiusPacket(BlockPos pos, int radius) {
        this.filterPos = pos;
        this.newRadius = radius;
    }

    public static void encode(FilterRadiusPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.filterPos);
        buf.writeInt(packet.newRadius);
    }

    public static FilterRadiusPacket decode(FriendlyByteBuf buf) {
        return new FilterRadiusPacket(buf.readBlockPos(), buf.readInt());
    }

    public static void handle(FilterRadiusPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            if (level.getBlockEntity(packet.filterPos) instanceof AtmosphereFilterBlockEntity filter) {
                // Ограничиваем радиус 5-450
                int clamped = Math.max(5, Math.min(450, packet.newRadius));
                filter.scanRadius = clamped;
                filter.setChanged();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}