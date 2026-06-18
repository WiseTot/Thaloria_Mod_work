package com.thaloria.network;

import com.thaloria.client.render.PressureGogglesRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncPressureZonesPacket {

    // Список AABB зон: каждая зона = {minX, minY, minZ, maxX, maxY, maxZ, pressure}
    private final List<ZoneBox> zones;

    public SyncPressureZonesPacket(List<ZoneBox> zones) {
        this.zones = zones;
    }

    public static class ZoneBox {
        public final int minX, minY, minZ, maxX, maxY, maxZ;
        public final float pressure;

        public ZoneBox(int minX, int minY, int minZ,
                       int maxX, int maxY, int maxZ, float pressure) {
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
            this.pressure = pressure;
        }
    }

    public static void encode(SyncPressureZonesPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.zones.size());
        for (ZoneBox z : p.zones) {
            buf.writeInt(z.minX); buf.writeInt(z.minY); buf.writeInt(z.minZ);
            buf.writeInt(z.maxX); buf.writeInt(z.maxY); buf.writeInt(z.maxZ);
            buf.writeFloat(z.pressure);
        }
    }

    public static SyncPressureZonesPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<ZoneBox> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new ZoneBox(
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readFloat()
            ));
        }
        return new SyncPressureZonesPacket(list);
    }

    public static void handle(SyncPressureZonesPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        PressureGogglesRenderer.updateZones(packet.zones))
        );
        ctx.get().setPacketHandled(true);
    }
}