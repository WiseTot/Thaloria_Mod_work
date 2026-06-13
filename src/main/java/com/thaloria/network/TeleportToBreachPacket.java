package com.thaloria.network;

import com.thaloria.block.entity.AtmosphereFilterBlockEntity;
import com.thaloria.dome.DomeZone;
import com.thaloria.dome.DomeZoneSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TeleportToBreachPacket {

    private final BlockPos filterPos;

    public TeleportToBreachPacket(BlockPos pos) {
        this.filterPos = pos;
    }

    public static void encode(TeleportToBreachPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.filterPos);
    }

    public static TeleportToBreachPacket decode(FriendlyByteBuf buf) {
        return new TeleportToBreachPacket(buf.readBlockPos());
    }

    public static void handle(TeleportToBreachPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            if (!(level.getBlockEntity(packet.filterPos)
                    instanceof AtmosphereFilterBlockEntity filter)) return;

            DomeZone zone = filter.getZone(level);
            if (zone == null || zone.breaches.isEmpty()) {
                player.sendSystemMessage(Component.literal(
                        "[Thaloria] No breaches found!"));
                return;
            }

            // Телепортируем к первой бреши
            List<BlockPos> breachList = new ArrayList<>(zone.breaches);
            BlockPos target = breachList.get(0);

            player.teleportTo(
                    target.getX() + 0.5,
                    target.getY() + 1.0,
                    target.getZ() + 0.5
            );

            // Сообщаем координаты всех брешей в чат
            player.sendSystemMessage(Component.literal(
                    "§c[Thaloria] Breaches (" + breachList.size() + "):"));
            for (int i = 0; i < breachList.size(); i++) {
                BlockPos b = breachList.get(i);
                player.sendSystemMessage(Component.literal(
                        "§e  #" + (i+1) + ": X=" + b.getX() +
                                " Y=" + b.getY() + " Z=" + b.getZ()));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}