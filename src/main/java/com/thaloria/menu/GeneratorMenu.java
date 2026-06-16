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

    public GeneratorMenu(int id, Inventory playerInv,
                         GeneratorBlockEntity blockEntity) {
        super(ModMenus.GENERATOR_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.data = blockEntity.getData();

        // Слот топлива — позиция подобрана под новый UI
        this.addSlot(new Slot(blockEntity.getInventory(), 0, 80, 130) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Принимаем только горючее
                return net.minecraftforge.common.ForgeHooks
                        .getBurnTime(stack,
                                net.minecraft.world.item.crafting.RecipeType.SMELTING) > 0;
            }
        });

        addDataSlots(this.data);

        // Инвентарь игрока — нужен для drag-and-drop топлива в слот
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInv, j + i * 9 + 9,
                        8 + j * 18, 168 + i * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInv, k, 8 + k * 18, 226));
        }
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // Из слота топлива → в инвентарь игрока
            if (index == 0) {
                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                // Из инвентаря → в слот топлива
            } else {
                if (!this.moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    public int getBurnTime()    { return data.get(0); }
    public int getMaxBurnTime() { return data.get(1); }
    public int getEnergy()      { return data.get(2); }
}