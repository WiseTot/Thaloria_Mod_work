package com.thaloria.item;

import com.thaloria.suit.SuitOxygenUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class OxygenCanisterItem extends Item {

    public OxygenCanisterItem(Properties properties) {
        super(properties);
    }

    private int getOxygen(ItemStack stack) {
        return stack.getOrCreateTag().getInt("Oxygen");
    }

    private void setOxygen(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt("Oxygen", Math.max(value, 0));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level,
                                                  Player player,
                                                  InteractionHand hand) {

        ItemStack canister = player.getItemInHand(hand);

        if (level.isClientSide) return InteractionResultHolder.success(canister);

        int canisterOxygen = getOxygen(canister);
        if (canisterOxygen <= 0) {
            return InteractionResultHolder.fail(canister);
        }

        // Проверяем полный комплект
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        if (!SuitOxygenUtil.isFullSuit(player)) {
            return InteractionResultHolder.fail(canister);
        }

        int suitOxygen = SuitOxygenUtil.getOxygen(helmet);

        int space = 100 - suitOxygen;
        if (space <= 0) {
            return InteractionResultHolder.fail(canister);
        }

        int transfer = Math.min(space, canisterOxygen);

        // Добавляем кислород в костюм
        SuitOxygenUtil.setOxygen(helmet, suitOxygen + transfer);
        SuitOxygenUtil.setOxygen(chest, suitOxygen + transfer);
        SuitOxygenUtil.setOxygen(legs, suitOxygen + transfer);
        SuitOxygenUtil.setOxygen(boots, suitOxygen + transfer);

        // Уменьшаем в канистре
        setOxygen(canister, canisterOxygen - transfer);

        return InteractionResultHolder.success(canister);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                Level level,
                                List<Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {

        tooltip.add(Component.literal("Oxygen: " + getOxygen(stack) + " / 100")
                .withStyle(ChatFormatting.AQUA));
    }
}