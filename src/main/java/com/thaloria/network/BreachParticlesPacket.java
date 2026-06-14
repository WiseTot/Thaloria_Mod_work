package com.thaloria.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class BreachParticlesPacket {

    private final List<BlockPos> breaches;

    public BreachParticlesPacket(List<BlockPos> breaches) {
        this.breaches = breaches;
    }

    public static void encode(BreachParticlesPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.breaches.size());
        for (BlockPos pos : p.breaches) buf.writeBlockPos(pos);
    }

    public static BreachParticlesPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<BlockPos> list = new ArrayList<>();
        for (int i = 0; i < size; i++) list.add(buf.readBlockPos());
        return new BreachParticlesPacket(list);
    }

    public static void handle(BreachParticlesPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Level level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null) return;

            for (BlockPos pos : packet.breaches) {
                // Красные частицы — видны сквозь блоки благодаря большому количеству
                for (int i = 0; i < 30; i++) {
                    double ox = (Math.random() - 0.5) * 0.6;
                    double oy = (Math.random() - 0.5) * 0.6;
                    double oz = (Math.random() - 0.5) * 0.6;
                    level.addParticle(ParticleTypes.FLAME,
                            pos.getX() + 0.5 + ox,
                            pos.getY() + 0.5 + oy,
                            pos.getZ() + 0.5 + oz,
                            0, 0.02, 0);
                }
                // Дополнительно — белый дым для видимости
                for (int i = 0; i < 10; i++) {
                    level.addParticle(ParticleTypes.CLOUD,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            (Math.random() - 0.5) * 0.1,
                            0.05,
                            (Math.random() - 0.5) * 0.1);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}