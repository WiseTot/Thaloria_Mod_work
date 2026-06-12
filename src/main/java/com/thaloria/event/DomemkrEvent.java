package com.thaloria.event;

import com.thaloria.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class DomemkrEvent {

    // ЛКМ → P1
    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {

        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getMainHandItem().getItem() != ModItems.DOMEMKR.get()) return;

        var tag = player.getPersistentData();

        long currentTick = player.level().getGameTime();
        long lastClick = tag.getLong("domemkr_last_click");

        if (currentTick - lastClick < 5) return;

        tag.putLong("domemkr_last_click", currentTick);

        BlockPos pos = event.getPos();

        tag.putInt("dome_pos1_x", pos.getX());
        tag.putInt("dome_pos1_y", pos.getY());
        tag.putInt("dome_pos1_z", pos.getZ());

        player.sendSystemMessage(
                Component.literal("P1 set to: " + pos.getX() + " " + pos.getY() + " " + pos.getZ())
        );

        event.setCanceled(true);
    }

    // ПКМ → P2
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {

        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getMainHandItem().getItem() != ModItems.DOMEMKR.get()) return;

        BlockPos pos = event.getPos();

        var tag = player.getPersistentData();
        tag.putInt("dome_pos2_x", pos.getX());
        tag.putInt("dome_pos2_y", pos.getY());
        tag.putInt("dome_pos2_z", pos.getZ());

        player.sendSystemMessage(
                Component.literal("P2 set to: " + pos.getX() + " " + pos.getY() + " " + pos.getZ())
        );

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}