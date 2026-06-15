package com.thaloria.network;

import com.thaloria.screen.BreachDetectorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenBreachDetectorPacket {

    private final BlockPos pos;
    private final boolean isPowered;
    private final boolean autoMonitor;
    private final int breachCount;
    private final int shellCount;
    private final float pressure;
    private final boolean hasZone;

    public OpenBreachDetectorPacket(BlockPos pos, boolean isPowered,
                                    boolean autoMonitor, int breachCount,
                                    int shellCount, float pressure, boolean hasZone) {
        this.pos = pos;
        this.isPowered = isPowered;
        this.autoMonitor = autoMonitor;
        this.breachCount = breachCount;
        this.shellCount = shellCount;
        this.pressure = pressure;
        this.hasZone = hasZone;
    }

    public static void encode(OpenBreachDetectorPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeBoolean(p.isPowered);
        buf.writeBoolean(p.autoMonitor);
        buf.writeInt(p.breachCount);
        buf.writeInt(p.shellCount);
        buf.writeFloat(p.pressure);
        buf.writeBoolean(p.hasZone);
    }

    public static OpenBreachDetectorPacket decode(FriendlyByteBuf buf) {
        return new OpenBreachDetectorPacket(
                buf.readBlockPos(), buf.readBoolean(), buf.readBoolean(),
                buf.readInt(), buf.readInt(), buf.readFloat(), buf.readBoolean()
        );
    }

    public static void handle(OpenBreachDetectorPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new BreachDetectorScreen(
                    packet.pos, packet.isPowered, packet.autoMonitor,
                    packet.breachCount, packet.shellCount,
                    packet.pressure, packet.hasZone
            ));
        });
        ctx.get().setPacketHandled(true);
    }
}