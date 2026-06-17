package com.thaloria.menu;

import com.thaloria.block.entity.OxygenCompressorBlockEntity;
import com.thaloria.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraft.world.item.ItemStack;
import com.thaloria.registry.ModItems;

public class OxygenCompressorMenu extends AbstractContainerMenu {

    private final OxygenCompressorBlockEntity blockEntity;

    // Клиентская копия данных: 0 = кислород канистры, 1 = isPowered
    private final ContainerData syncData;

    public OxygenCompressorMenu(int id, Inventory playerInventory,
                                OxygenCompressorBlockEntity blockEntity) {
        super(ModMenus.OXYGEN_COMPRESSOR.get(), id);
        this.blockEntity = blockEntity;
        this.syncData = blockEntity.getData();

        // 1 слот для канистры — справа по центру
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 170, 60) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == ModItems.OXYGEN_CANISTER.get();
            }
        });

        // Синхронизация на клиент
        addDataSlots(syncData);

        // Инвентарь игрока
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory,
                        col + row * 9 + 9, 8 + col * 18, 120 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 178));
        }
    }

    public OxygenCompressorMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory,
                (OxygenCompressorBlockEntity)
                        playerInventory.player.level()
                                .getBlockEntity(buffer.readBlockPos()));
    }

    /** Кислород в канистре (0–100, -1 если пусто) */
    public int getCanisterOxygen() {
        return syncData.get(0);
    }

    /** Подключён ли к генератору */
    public boolean isPowered() {
        return syncData.get(1) == 1;
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true))
                    return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, 1, false))
                    return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }
}