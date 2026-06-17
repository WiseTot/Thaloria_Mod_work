package com.thaloria.menu;

import com.thaloria.block.entity.OxygenChargerBlockEntity;
import com.thaloria.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import com.thaloria.registry.ModItems;

public class OxygenChargerMenu extends AbstractContainerMenu {

    private final OxygenChargerBlockEntity blockEntity;

    // Клиентская копия данных: 0–3 = кислород брони, 4 = isPowered
    private final ContainerData syncData;

    public OxygenChargerMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory,
                (OxygenChargerBlockEntity)
                        playerInventory.player.level()
                                .getBlockEntity(buffer.readBlockPos()));
    }

    public OxygenChargerMenu(int id, Inventory playerInventory,
                             OxygenChargerBlockEntity blockEntity) {
        super(ModMenus.OXYGEN_CHARGER.get(), id);
        this.blockEntity = blockEntity;
        this.syncData = blockEntity.getData();

        // 4 слота для частей скафандра — справа, Y начиная с 38 (ниже заголовка и статуса)
        int slotX = 170;
        int slotStartY = 38;
        for (int i = 0; i < 4; i++) {
            final int slotIndex = i;
            this.addSlot(new SlotItemHandler(
                    blockEntity.getItemHandler(), i,
                    slotX, slotStartY + i * 22
            ) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return switch (slotIndex) {
                        case 0 -> stack.getItem() == ModItems.ATMOSPHERE_HELMET.get();
                        case 1 -> stack.getItem() == ModItems.ATMOSPHERE_CHESTPLATE.get();
                        case 2 -> stack.getItem() == ModItems.ATMOSPHERE_LEGGINGS.get();
                        case 3 -> stack.getItem() == ModItems.ATMOSPHERE_BOOTS.get();
                        default -> false;
                    };
                }
            });
        }

        // Синхронизация на клиент
        addDataSlots(syncData);

        // Инвентарь игрока
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory,
                        col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    /** Кислород в слоте брони (0–100, -1 если пусто) */
    public int getSlotOxygen(int slot) {
        return syncData.get(slot);
    }

    /** Подключён ли к генератору */
    public boolean isPowered() {
        return syncData.get(4) == 1;
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
            if (index < 4) {
                if (!this.moveItemStackTo(stack, 4, this.slots.size(), true))
                    return ItemStack.EMPTY;
            } else {
                for (int i = 0; i < 4; i++) {
                    if (this.slots.get(i).mayPlace(stack) && !this.slots.get(i).hasItem()) {
                        if (!this.moveItemStackTo(stack, i, i + 1, false))
                            return ItemStack.EMPTY;
                        break;
                    }
                }
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }
}