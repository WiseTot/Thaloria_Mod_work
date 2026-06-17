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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import com.thaloria.menu.OxygenChargerMenu;

public class OxygenChargerBlockEntity extends BlockEntity implements MenuProvider {

    private static final int ENERGY_PER_CHARGE = 5;

    private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // int[] — реальное хранилище, синхронизируется на клиент через ContainerData
    // [0–3] = кислород каждого слота брони, [4] = isPowered
    private final int[] syncValues = new int[5];

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index)               { return syncValues[index]; }
        @Override public void set(int index, int value)   { syncValues[index] = value; }
        @Override public int getCount()                   { return 5; }
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

    public ItemStackHandler getItemHandler() { return itemHandler; }
    public ContainerData getData()           { return data; }

    public void tick() {
        if (level == null || level.isClientSide) return;

        // Обновляем кислород в syncValues (для UI)
        for (int i = 0; i < 4; i++) {
            var stack = itemHandler.getStackInSlot(i);
            syncValues[i] = stack.isEmpty() ? -1 : SuitOxygenUtil.getOxygen(stack);
        }

        // Ищем генератор
        GeneratorBlockEntity generator = findGenerator();
        boolean powered = generator != null && generator.hasEnergy();
        syncValues[4] = powered ? 1 : 0;

        if (!powered) {
            chargeTimer = 0;
            return;
        }

        // Проверяем что все 4 слота заняты
        boolean allFull = true;
        for (int i = 0; i < 4; i++) {
            if (itemHandler.getStackInSlot(i).isEmpty()) { allFull = false; break; }
        }
        if (!allFull) { chargeTimer = 0; return; }

        chargeTimer++;
        if (chargeTimer >= 20) {
            chargeTimer = 0;
            boolean changed = false;
            for (int i = 0; i < 4; i++) {
                var stack = itemHandler.getStackInSlot(i);
                int current = SuitOxygenUtil.getOxygen(stack);
                if (current < 100) {
                    SuitOxygenUtil.setOxygen(stack, current + 1);
                    generator.consumeEnergy(ENERGY_PER_CHARGE);
                    changed = true;
                }
            }
            if (changed) setChanged();
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