package com.thaloria.block;

import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class DustLayerBlock extends SnowLayerBlock {

    public DustLayerBlock() {
        super(BlockBehaviour.Properties
                .of()
                .strength(0.1f)
                .sound(SoundType.SAND)
                .noOcclusion()
        );
    }
}