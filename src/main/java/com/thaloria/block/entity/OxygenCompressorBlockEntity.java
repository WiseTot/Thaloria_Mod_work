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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import com.thaloria.menu.OxygenCompressorMenu;

public class OxygenCompressorBlockEntity extends BlockEntity implements MenuProvider {

    private static final int ENERGY_PER_CHARGE = 3;

    private int tickCounter = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // [0] = кислород канистры (-1 если пусто), [1] = isPowered
    private final int[] syncValues = new int[]{-1, 0};

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index)              { return syncValues[index]; }
        @Override public void set(int index, int value)  { syncValues[index] = value; }
        @Override public int getCount()                  { return 2; }
    };

    public OxygenCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_COMPRESSOR.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }
    public ContainerData getData()           { return data; }

    public void tick() {
        if (level == null || level.isClientSide) return;

        // Обновляем кислород в syncValues
        ItemStack stack = itemHandler.getStackInSlot(0);
        syncValues[0] = (stack.isEmpty() || stack.getItem() != ModItems.OXYGEN_CANISTER.get())
                ? -1
                : stack.getOrCreateTag().getInt("Oxygen");

        // Ищем генератор
        GeneratorBlockEntity generator = findGenerator();
        boolean powered = generator != null && generator.hasEnergy();
        syncValues[1] = powered ? 1 : 0;

        if (!powered) {
            tickCounter = 0;
            return;
        }

        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;
            if (!stack.isEmpty() && stack.getItem() == ModItems.OXYGEN_CANISTER.get()) {
                int oxygen = stack.getOrCreateTag().getInt("Oxygen");
                if (oxygen < 100) {
                    stack.getOrCreateTag().putInt("Oxygen", oxygen + 1);
                    generator.consumeEnergy(ENERGY_PER_CHARGE);
                    setChanged();
                }
            }
        }
    }

    private GeneratorBlockEntity findGenerator() {
        if (level == null) return null;
        int radius = GeneratorBlockEntity.WIRELESS_RADIUS;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    if (level.getBlockEntity(p) instanceof GeneratorBlockEntity gen) {
                        if (gen.hasEnergy()) return gen;
                    }
                }
            }
        }
        return null;
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
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new OxygenCompressorMenu(id, playerInventory, this);
    }
}