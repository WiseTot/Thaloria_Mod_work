package com.thaloria.network;

import com.thaloria.screen.AtmosphereFilterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenFilterScreenPacket {

    private final BlockPos pos;
    private final int radius;
    private final boolean isPowered;
    private final boolean isScanning;
    private final int scanProgress;
    private final int shellCount;
    private final int breachCount;
    private final float pressure;
    private final float pressureDelta;
    private final int filterCount;
    private final boolean isSealed;

    public OpenFilterScreenPacket(BlockPos pos, int radius, boolean isPowered,
                                  boolean isScanning, int scanProgress,
                                  int shellCount, int breachCount,
                                  float pressure, float pressureDelta,
                                  int filterCount, boolean isSealed) {
        this.pos = pos;
        this.radius = radius;
        this.isPowered = isPowered;
        this.isScanning = isScanning;
        this.scanProgress = scanProgress;
        this.shellCount = shellCount;
        this.breachCount = breachCount;
        this.pressure = pressure;
        this.pressureDelta = pressureDelta;
        this.filterCount = filterCount;
        this.isSealed = isSealed;
    }

    public static void encode(OpenFilterScreenPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeInt(p.radius);
        buf.writeBoolean(p.isPowered);
        buf.writeBoolean(p.isScanning);
        buf.writeInt(p.scanProgress);
        buf.writeInt(p.shellCount);
        buf.writeInt(p.breachCount);
        buf.writeFloat(p.pressure);
        buf.writeFloat(p.pressureDelta);
        buf.writeInt(p.filterCount);
        buf.writeBoolean(p.isSealed);
    }

    public static OpenFilterScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenFilterScreenPacket(
                buf.readBlockPos(), buf.readInt(), buf.readBoolean(),
                buf.readBoolean(), buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readFloat(), buf.readFloat(),
                buf.readInt(), buf.readBoolean()
        );
    }

    public static void handle(OpenFilterScreenPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof AtmosphereFilterScreen filterScreen) {
                filterScreen.updateData(
                        packet.isPowered, packet.isScanning, packet.scanProgress,
                        packet.shellCount, packet.breachCount, packet.pressure,
                        packet.pressureDelta, packet.filterCount, packet.isSealed
                );
            } else {
                mc.setScreen(new AtmosphereFilterScreen(
                        packet.pos, packet.radius, packet.isPowered,
                        packet.isScanning, packet.scanProgress,
                        packet.shellCount, packet.breachCount,
                        packet.pressure, packet.pressureDelta,
                        packet.filterCount, packet.isSealed
                ));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}