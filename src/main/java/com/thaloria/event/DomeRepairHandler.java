package com.thaloria.event;

import com.thaloria.block.entity.AtmosphereFilterBlockEntity;
import com.thaloria.dome.DomeZone;
import com.thaloria.dome.DomeZoneSavedData;
import com.thaloria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.thaloria.ThaloriaMod;

@Mod.EventBusSubscriber(modid = ThaloriaMod.MOD_ID)
public class DomeRepairHandler {

    private static final double MAX_REPAIR_DISTANCE = 8.0;
    private static final double MIN_DOT = 0.90;

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        tryRepair(event.getEntity(), event.getItemStack());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!stack.is(ModBlocks.DOME_GLASS.get().asItem())) return;
        if (!(player.level() instanceof ServerLevel)) return;

        // Не мешаем если кликают на сам блок купола
        if (event.getLevel().getBlockState(event.getPos())
                .is(ModBlocks.DOME_GLASS.get())) return;

        if (tryRepair(player, stack)) {
            event.setCanceled(true);
        }
    }

    private static boolean tryRepair(Player player, ItemStack stack) {
        if (!stack.is(ModBlocks.DOME_GLASS.get().asItem())) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;

        Vec3 eye  = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        // Читаем бреши прямо из серверных данных DomeZone
        DomeZoneSavedData savedData = DomeZoneSavedData.get(level);

        BlockPos bestBreach = null;
        double   bestDot    = MIN_DOT;

        for (DomeZone zone : savedData.getAllZones()) {
            for (BlockPos breach : zone.breaches) {
                // Дополнительно проверяем что блок действительно воздух
                if (!level.getBlockState(breach).isAir()) continue;

                Vec3 center   = Vec3.atCenterOf(breach);
                double distance = center.distanceTo(eye);
                if (distance > MAX_REPAIR_DISTANCE) continue;

                Vec3 dir = center.subtract(eye).normalize();
                double dot = dir.dot(look);

                if (dot > bestDot) {
                    bestDot    = dot;
                    bestBreach = breach;
                }
            }
        }

        if (bestBreach == null) return false;

        // Ставим блок купола
        level.setBlock(bestBreach, ModBlocks.DOME_GLASS.get().defaultBlockState(), 3);

        // Звук
        level.playSound(null, bestBreach,
                SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);

        // Тратим предмет (не в творческом режиме)
        if (!player.isCreative()) {
            stack.shrink(1);
        }

        // Обновляем блок на клиенте
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(
                    new ClientboundBlockUpdatePacket(level, bestBreach));
        }

        // Удаляем брешь из зон, пересканируем если нужно
        final BlockPos repairedPos = bestBreach;
        for (DomeZone zone : savedData.getAllZones()) {
            if (zone.breaches.remove(repairedPos)) {
                savedData.setDirty();
                if (zone.breaches.isEmpty()) {
                    triggerRescan(level, zone, savedData);
                }
            }
        }

        return true;
    }

    private static void triggerRescan(ServerLevel level, DomeZone zone,
                                      DomeZoneSavedData data) {
        for (BlockPos filterPos : zone.filters) {
            BlockEntity be = level.getBlockEntity(filterPos);
            if (be instanceof AtmosphereFilterBlockEntity filter) {
                filter.startScan(level);
                return;
            }
        }
    }
}