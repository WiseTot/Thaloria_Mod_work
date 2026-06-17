package com.thaloria.network;

import com.thaloria.client.render.DustStormRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncStormPacket {

    private final boolean stormActive;

    public SyncStormPacket(boolean stormActive) {
        this.stormActive = stormActive;
    }

    public static void encode(SyncStormPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.stormActive);
    }

    public static SyncStormPacket decode(FriendlyByteBuf buf) {
        return new SyncStormPacket(buf.readBoolean());
    }

    public static void handle(SyncStormPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    DustStormRenderer.setStormActive(packet.stormActive));
        });
        ctx.get().setPacketHandled(true);
    }
}