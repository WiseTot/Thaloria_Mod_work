package com.thaloria.network;

import com.thaloria.block.entity.AtmosphereFilterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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
            if (!(level.getBlockEntity(packet.filterPos)
                    instanceof AtmosphereFilterBlockEntity filter)) return;

            if (!filter.isPowered) {
                player.sendSystemMessage(Component.literal(
                        "§c[Thaloria] Filter has no power!"));
                return;
            }

            // Лёгкая проверка брешей — эталон не трогаем
            int breachCount = filter.checkBreaches(level);

            if (breachCount < 0) {
                player.sendSystemMessage(Component.literal(
                        "§c[Thaloria] No baseline! Use Update Baseline first."));
            } else if (breachCount == 0) {
                player.sendSystemMessage(Component.literal(
                        "§a[Thaloria] Dome integrity: OK — no breaches detected."));
            } else {
                player.sendSystemMessage(Component.literal(
                        "§e[Thaloria] Dome integrity: " + breachCount + " breach(es) found!"));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}