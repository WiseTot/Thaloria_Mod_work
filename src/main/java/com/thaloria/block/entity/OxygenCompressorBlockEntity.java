package com.thaloria.block.entity;

import com.thaloria.registry.ModBlockEntities;
import com.thaloria.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import com.thaloria.menu.OxygenCompressorMenu;

public class OxygenCompressorBlockEntity
        extends BlockEntity
        implements MenuProvider {

    private int tickCounter = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public OxygenCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_COMPRESSOR.get(), pos, state);
    }

    public void tick() {

        if (level == null || level.isClientSide) return;

        tickCounter++;

        if (tickCounter >= 20) { // раз в секунду
            tickCounter = 0;

            ItemStack stack = itemHandler.getStackInSlot(0);

            if (!stack.isEmpty() &&
                    stack.getItem() == ModItems.OXYGEN_CANISTER.get()) {

                int oxygen = stack.getOrCreateTag().getInt("Oxygen");

                if (oxygen < 100) {
                    stack.getOrCreateTag().putInt("Oxygen", oxygen + 1);
                    setChanged();
                }
            }
        }
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Oxygen Compressor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id,
                                            Inventory playerInventory,
                                            Player player) {
        return new OxygenCompressorMenu(id, playerInventory, this);
    }
}