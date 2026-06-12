package com.thaloria.menu;

import com.thaloria.block.entity.GeneratorBlockEntity;
import com.thaloria.registry.ModMenus;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class GeneratorMenu extends AbstractContainerMenu {

    private final GeneratorBlockEntity blockEntity;
    private final ContainerData data;

    public GeneratorMenu(int id, Inventory playerInv, GeneratorBlockEntity blockEntity) {
        super(ModMenus.GENERATOR_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.data = blockEntity.getData();

        // 🔥 СЛОТ ТОПЛИВА
        this.addSlot(new Slot(blockEntity.getInventory(), 0, 80, 35));

        // 🔄 Синхронизация данных
        addDataSlots(this.data);

        // 🧍 Инвентарь игрока
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInv, j + i * 9 + 9,
                        8 + j * 18,
                        84 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInv, k, 8 + k * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public int getBurnTime() {
        return data.get(0);
    }

    public int getMaxBurnTime() {
        return data.get(1);
    }

    public int getEnergy() {
        return data.get(2);
    }
}