package com.thaloria.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class DustBlock extends Block {

    public DustBlock() {
        super(BlockBehaviour.Properties
                .of()
                .strength(0.3f)
                .sound(SoundType.SAND)
                .noOcclusion()
        );
    }
}