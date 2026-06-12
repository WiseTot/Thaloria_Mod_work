package com.thaloria.block.entity;

import com.thaloria.registry.ModBlockEntities;
import com.thaloria.suit.SuitOxygenUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import com.thaloria.menu.OxygenChargerMenu;

public class OxygenChargerBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private LazyOptional<ItemStackHandler> lazyHandler = LazyOptional.empty();
    private int chargeTimer = 0;

    public OxygenChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_CHARGER.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyHandler.invalidate();
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        // Проверяем что все 4 слота заполнены
        for (int i = 0; i < 4; i++) {
            if (itemHandler.getStackInSlot(i).isEmpty()) return;
        }

        chargeTimer++;

        // ИСПРАВЛЕНО: только один блок заряда, строго раз в секунду
        if (chargeTimer >= 20) {
            chargeTimer = 0;

            boolean changed = false;
            for (int i = 0; i < 4; i++) {
                var stack = itemHandler.getStackInSlot(i);
                int current = SuitOxygenUtil.getOxygen(stack);
                if (current < 100) {
                    SuitOxygenUtil.setOxygen(stack, current + 1);
                    changed = true;
                }
            }

            if (changed) setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inv", itemHandler.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inv"));
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Oxygen Charger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new OxygenChargerMenu(id, playerInventory, this);
    }
}