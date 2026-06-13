package com.thaloria.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// Сервер → Клиент: показать частицы на позициях брешей
public class ShowBreachesPacket {

    private final List<BlockPos> breaches;

    public ShowBreachesPacket(List<BlockPos> breaches) {
        this.breaches = breaches;
    }

    public static void encode(ShowBreachesPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.breaches.size());
        for (BlockPos pos : p.breaches) {
            buf.writeBlockPos(pos);
        }
    }

    public static ShowBreachesPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<BlockPos> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(buf.readBlockPos());
        }
        return new ShowBreachesPacket(list);
    }

    public static void handle(ShowBreachesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Level level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null) return;

            for (BlockPos pos : packet.breaches) {
                // Спавним яркие красные частицы на каждой бреши
                // Много частиц чтобы было хорошо видно
                for (int i = 0; i < 20; i++) {
                    double ox = (Math.random() - 0.5) * 0.8;
                    double oy = (Math.random() - 0.5) * 0.8;
                    double oz = (Math.random() - 0.5) * 0.8;
                    level.addParticle(
                            ParticleTypes.FLAME,
                            pos.getX() + 0.5 + ox,
                            pos.getY() + 0.5 + oy,
                            pos.getZ() + 0.5 + oz,
                            0, 0.05, 0
                    );
                }
                // Дополнительно — большая вспышка в центре
                level.addParticle(
                        ParticleTypes.EXPLOSION,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        0, 0, 0
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}