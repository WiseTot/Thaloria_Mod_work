package com.thaloria.event;

import com.thaloria.block.entity.AtmosphereFilterBlockEntity;
import com.thaloria.client.render.BreachOutlineRenderer;
import com.thaloria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class DomeRepairHandler {

    private static final double MAX_DISTANCE = 6.0;

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;
        ItemStack stack = event.getItemStack();

        if (!stack.is(ModBlocks.DOME_GLASS.get().asItem())) return;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        BlockPos bestBreach = null;
        double bestScore = 0;

        // Ищем брешь, на которую смотрит игрок
        for (BlockPos breach : BreachOutlineRenderer.BREACHES) {
            Vec3 center = new Vec3(breach.getX() + 0.5, breach.getY() + 0.5, breach.getZ() + 0.5);
            double distance = center.distanceTo(eye);
            if (distance > MAX_DISTANCE) continue;

            Vec3 dir = center.subtract(eye).normalize();
            double dot = dir.dot(look);

            if (dot > bestScore && dot > 0.92) {
                bestScore = dot;
                bestBreach = breach;
            }
        }

        if (bestBreach == null) return;

        // Ставим блок стекла
        level.setBlock(bestBreach, ModBlocks.DOME_GLASS.get().defaultBlockState(), 3);
        stack.shrink(1);
        BreachOutlineRenderer.BREACHES.remove(bestBreach);

        // Теперь уведомляем ближайшие фильтры, что дыра закрыта
        int searchRadius = 10;
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = bestBreach.offset(x, y, z);
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof AtmosphereFilterBlockEntity filter) {
                        filter.markDirty(); // Этот метод мы добавим в сущность фильтра
                    }
                }
            }
        }
        event.setCanceled(true);
    }
}