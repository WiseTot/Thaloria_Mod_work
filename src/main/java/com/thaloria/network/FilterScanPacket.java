package com.thaloria.network;

import com.thaloria.block.entity.AtmosphereFilterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Пакет от клиента к серверу — игрок нажал кнопку "Сканировать"
public class FilterScanPacket {

    private final BlockPos filterPos;

    public FilterScanPacket(BlockPos pos) {
        this.filterPos = pos;
    }

    public static void encode(FilterScanPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.filterPos);
    }

    public static FilterScanPacket decode(FriendlyByteBuf buf) {
        return new FilterScanPacket(buf.readBlockPos());
    }

    public static void handle(FilterScanPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            if (level.getBlockEntity(packet.filterPos) instanceof AtmosphereFilterBlockEntity filter) {
                if (filter.isPowered && !filter.isScanning) {
                    filter.startScan(level);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}