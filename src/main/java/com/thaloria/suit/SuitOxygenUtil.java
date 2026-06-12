package com.thaloria.suit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import com.thaloria.registry.ModItems;

import java.util.List;

public class SuitOxygenUtil {

    private static final String OXYGEN_TAG = "SuitOxygen";
    private static final int MAX_OXYGEN = 100;

    public static int getOxygen(ItemStack stack) {

        CompoundTag tag = stack.getOrCreateTag();

        if (!tag.contains(OXYGEN_TAG)) {
            tag.putInt(OXYGEN_TAG, MAX_OXYGEN);
        }

        return tag.getInt(OXYGEN_TAG);
    }

    public static void setOxygen(ItemStack stack, int value) {

        CompoundTag tag = stack.getOrCreateTag();

        tag.putInt(OXYGEN_TAG,
                Math.max(0, Math.min(MAX_OXYGEN, value)));
    }

    // 🔹 Получить кислород из комплекта
    public static int getSuitOxygen(List<ItemStack> armorPieces) {

        for (ItemStack piece : armorPieces) {
            if (!piece.isEmpty()) {
                return getOxygen(piece);
            }
        }

        return MAX_OXYGEN;
    }

    // 🔹 Установить кислород во всех частях
    public static void setSuitOxygen(List<ItemStack> armorPieces, int value) {

        for (ItemStack piece : armorPieces) {
            if (!piece.isEmpty()) {
                setOxygen(piece, value);
            }
        }
    }

    // 🔹 Уменьшить во всех частях
    public static void removeSuitOxygen(List<ItemStack> armorPieces, int value) {

        int current = getSuitOxygen(armorPieces);
        setSuitOxygen(armorPieces, current - value);
    }

    public static void restore(List<ItemStack> armorPieces) {
        setSuitOxygen(armorPieces, MAX_OXYGEN);
    }

    public static boolean isFullSuit(Player player) {

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        return helmet.getItem() == ModItems.ATMOSPHERE_HELMET.get()
                && chest.getItem() == ModItems.ATMOSPHERE_CHESTPLATE.get()
                && legs.getItem() == ModItems.ATMOSPHERE_LEGGINGS.get()
                && boots.getItem() == ModItems.ATMOSPHERE_BOOTS.get();
    }
}