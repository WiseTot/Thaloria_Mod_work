package com.thaloria.network;

import com.thaloria.client.render.BreachOutlineRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// Сервер → Клиент: синхронизировать бреши для аутлайна
public class SyncBreachesPacket {

    private final List<BlockPos> breaches;
    private final BlockPos detectorPos;

    public SyncBreachesPacket(List<BlockPos> breaches, BlockPos detectorPos) {
        this.breaches = breaches;
        this.detectorPos = detectorPos;
    }

    public static void encode(SyncBreachesPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.detectorPos);
        buf.writeInt(p.breaches.size());
        for (BlockPos pos : p.breaches) buf.writeBlockPos(pos);
    }

    public static SyncBreachesPacket decode(FriendlyByteBuf buf) {
        BlockPos detectorPos = buf.readBlockPos();
        int size = buf.readInt();
        List<BlockPos> list = new ArrayList<>();
        for (int i = 0; i < size; i++) list.add(buf.readBlockPos());
        return new SyncBreachesPacket(list, detectorPos);
    }

    public static void handle(SyncBreachesPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Очищаем старые бреши и добавляем новые
            BreachOutlineRenderer.clearBreaches();
            BreachOutlineRenderer.detectorPos = packet.detectorPos;

            for (BlockPos pos : packet.breaches) {
                BreachOutlineRenderer.addBreach(pos);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}