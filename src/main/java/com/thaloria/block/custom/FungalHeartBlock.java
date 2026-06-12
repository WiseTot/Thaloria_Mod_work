package com.thaloria.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class FungalHeartBlock extends Block {

    public FungalHeartBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {

        boolean exposed =
                level.getBlockState(pos.above()).isAir() ||
                        level.getBlockState(pos.below()).isAir() ||
                        level.getBlockState(pos.north()).isAir() ||
                        level.getBlockState(pos.south()).isAir() ||
                        level.getBlockState(pos.east()).isAir() ||
                        level.getBlockState(pos.west()).isAir();

        if (!exposed) return;

        if (random.nextInt(20) == 0) {

            level.addParticle(
                    new DustParticleOptions(new Vector3f(1f, 0.9f, 0.3f), 3.2f),
                    pos.getX()+0.5,
                    pos.getY()+0.8,
                    pos.getZ()+0.5,
                    0, 0.05, 0
            );
        }

        for (int i = 0; i < 2; i++) {

            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 0.6 + random.nextDouble() * 0.4;
            double z = pos.getZ() + random.nextDouble();

            level.addParticle(
                    new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.2f), 1.6f),
                    x, y, z,
                    0, 0.03, 0
            );
        }
    }
}