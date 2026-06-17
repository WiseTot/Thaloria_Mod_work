package com.thaloria.network;

import com.thaloria.block.entity.AtmosphereFilterBlockEntity;
import com.thaloria.dome.DomeZone;
import com.thaloria.dome.DomeZoneSavedData;
import com.thaloria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RepairBreachPacket {

    private final BlockPos breachPos;

    public RepairBreachPacket(BlockPos pos) {
        this.breachPos = pos;
    }

    public static void encode(RepairBreachPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.breachPos);
    }

    public static RepairBreachPacket decode(FriendlyByteBuf buf) {
        return new RepairBreachPacket(buf.readBlockPos());
    }

    public static void handle(RepairBreachPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            BlockPos pos = packet.breachPos;

            // Проверяем что игрок держит блок купола
            ItemStack stack = player.getMainHandItem();
            if (!stack.is(ModBlocks.DOME_GLASS.get().asItem())) return;

            // Проверяем дистанцию (защита от читов)
            if (player.blockPosition().distSqr(pos) > 100) return; // 10 блоков макс

            // Проверяем что позиция — действительно брешь в какой-то зоне
            DomeZoneSavedData savedData = DomeZoneSavedData.get(level);
            DomeZone targetZone = null;
            for (DomeZone zone : savedData.getAllZones()) {
                if (zone.breaches.contains(pos)) {
                    targetZone = zone;
                    break;
                }
            }
            if (targetZone == null) return;

            // Ставим блок
            level.setBlock(pos, ModBlocks.DOME_GLASS.get().defaultBlockState(), 3);
            level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1f, 1f);

            // Тратим предмет
            if (!player.isCreative()) stack.shrink(1);

            // Обновляем клиент
            player.connection.send(new ClientboundBlockUpdatePacket(level, pos));

            // Убираем из зоны
            targetZone.breaches.remove(pos);
            savedData.setDirty();

            // Если брешей не осталось — пересканируем
            if (targetZone.breaches.isEmpty()) {
                for (BlockPos filterPos : targetZone.filters) {
                    BlockEntity be = level.getBlockEntity(filterPos);
                    if (be instanceof AtmosphereFilterBlockEntity filter) {
                        filter.startScan(level);
                        break;
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}