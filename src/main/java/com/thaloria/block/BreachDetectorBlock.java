package com.thaloria.block;

import com.thaloria.block.entity.BreachDetectorBlockEntity;
import com.thaloria.dome.DomeZone;
import com.thaloria.network.ModNetwork;
import com.thaloria.network.OpenBreachDetectorPacket;
import com.thaloria.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class BreachDetectorBlock extends BaseEntityBlock {

    public BreachDetectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        if (level.getBlockEntity(pos) instanceof BreachDetectorBlockEntity detector) {
            DomeZone zone = detector.getZone(serverLevel);
            int breachCount = zone != null ? zone.breaches.size() : 0;
            int shellCount = zone != null ? zone.originalShell.size() : 0;
            float pressure = zone != null ? zone.pressure : 0f;
            boolean hasZone = zone != null;

            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new OpenBreachDetectorPacket(
                            pos, detector.isPowered, detector.autoMonitor,
                            breachCount, shellCount, pressure, hasZone
                    )
            );
        }

        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BreachDetectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                                                                  BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.BREACH_DETECTOR.get(),
                (lvl, pos, st, be) -> be.tick());
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity>
    BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> type,
                                            BlockEntityType<E> targetType, BlockEntityTicker<? super E> ticker) {
        return targetType == type ? (BlockEntityTicker<A>) ticker : null;
    }
}