package com.thaloria.block.entity;

import com.thaloria.menu.GeneratorMenu;
import com.thaloria.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
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

public class GeneratorBlockEntity extends BlockEntity implements MenuProvider {

    private final SimpleContainer inventory = new SimpleContainer(1);

    private final ContainerData data = new SimpleContainerData(3);

    public int burnTime = 0;
    public int maxBurnTime = 0;
    public int energy = 0;
    public static final int MAX_ENERGY = 10000;

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
                int fuel = net.minecraftforge.common.ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);

                if (fuel > 0) {
                    burnTime = fuel;
                    maxBurnTime = fuel;
                    stack.shrink(1);
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
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putInt("energy", energy);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        burnTime = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
        energy = tag.getInt("energy");
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Mini Generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id,
                                            Inventory inv,
                                            Player player) {
        return new GeneratorMenu(id, inv, this);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public ContainerData getData() {
        return data;
    }
}