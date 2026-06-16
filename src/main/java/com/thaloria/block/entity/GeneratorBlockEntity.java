package com.thaloria.block.entity;

import com.thaloria.menu.GeneratorMenu;
import com.thaloria.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeHooks;

public class GeneratorBlockEntity extends BlockEntity implements MenuProvider {

    private final SimpleContainer inventory = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            GeneratorBlockEntity.this.setChanged();
        }
    };

    private final ContainerData data = new SimpleContainerData(3);

    public int burnTime    = 0;
    public int maxBurnTime = 0;
    public int energy      = 0;
    public static final int MAX_ENERGY = 10000;

    // Для будущей системы кабелей — список подключённых устройств
    // Пока используем радиус 5 блоков как заглушку
    public static final int WIRELESS_RADIUS = 5;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENERATOR.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        if (burnTime > 0) {
            burnTime--;
            energy = Math.min(MAX_ENERGY, energy + 20);
        }

        if (burnTime == 0) {
            ItemStack stack = inventory.getItem(0);
            if (!stack.isEmpty()) {
                int fuel = ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
                if (fuel > 0) {
                    burnTime    = fuel;
                    maxBurnTime = fuel;
                    stack.shrink(1);
                    if (stack.isEmpty()) inventory.setItem(0, ItemStack.EMPTY);
                }
            }
        }

        data.set(0, burnTime);
        data.set(1, maxBurnTime);
        data.set(2, energy);

        setChanged();
    }

    public boolean hasEnergy() {
        return energy > 0;
    }

    public void consumeEnergy(int amount) {
        energy = Math.max(0, energy - amount);
        setChanged();
    }

    public boolean isActive() {
        return burnTime > 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("burnTime",    burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putInt("energy",      energy);
        // ИСПРАВЛЕНИЕ: сохраняем инвентарь
        CompoundTag invTag = new CompoundTag();
        ItemStack fuel = inventory.getItem(0);
        if (!fuel.isEmpty()) {
            invTag.put("fuel", fuel.save(new CompoundTag()));
        }
        tag.put("inventory", invTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        burnTime    = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
        energy      = tag.getInt("energy");
        // Загружаем инвентарь
        if (tag.contains("inventory")) {
            CompoundTag invTag = tag.getCompound("inventory");
            if (invTag.contains("fuel")) {
                inventory.setItem(0, ItemStack.of(invTag.getCompound("fuel")));
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Power Generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new GeneratorMenu(id, inv, this);
    }

    public SimpleContainer getInventory() { return inventory; }
    public ContainerData getData()        { return data; }
}