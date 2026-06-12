package com.thaloria.menu;

import com.thaloria.block.entity.OxygenCompressorBlockEntity;
import com.thaloria.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraft.world.item.ItemStack;
import com.thaloria.registry.ModItems;

public class OxygenCompressorMenu extends AbstractContainerMenu {

    private final OxygenCompressorBlockEntity blockEntity;

    public OxygenCompressorMenu(int id,
                                Inventory playerInventory,
                                OxygenCompressorBlockEntity blockEntity) {

        super(ModMenus.OXYGEN_COMPRESSOR.get(), id);
        this.blockEntity = blockEntity;

        // 1 слот канистры
        this.addSlot(new SlotItemHandler(
                blockEntity.getItemHandler(),
                0,
                80,
                35
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == ModItems.OXYGEN_CANISTER.get();
            }
        });

        // Инвентарь игрока
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory,
                        col + row * 9 + 9,
                        8 + col * 18,
                        84 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory,
                    col,
                    8 + col * 18,
                    142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public OxygenCompressorMenu(int id,
                                Inventory playerInventory,
                                FriendlyByteBuf buffer) {

        this(id,
                playerInventory,
                (OxygenCompressorBlockEntity)
                        playerInventory.player.level()
                                .getBlockEntity(buffer.readBlockPos()));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {

        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {

            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            // Если кликнули из слота компрессора (индекс 0)
            if (index == 0) {

                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }

            }
            // Если кликнули из инвентаря игрока
            else {

                // Пытаемся засунуть в слот компрессора (0)
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

        return itemstack;
    }
}