package com.kqp.ezpas.block;

import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.world.BlockView;

public class FilteredPipeBlock extends BlockWithEntity {
    public FilteredPipeBlock() {
        super(FabricBlockSettings.of(Material.GLASS).strength(0.3F, 0.3F).nonOpaque().sounds(BlockSoundGroup.GLASS).nonOpaque().build());
    }

    @Override
    public BlockEntity createBlockEntity(BlockView view) {
        return null;
    }
}
