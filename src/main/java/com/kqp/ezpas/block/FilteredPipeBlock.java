package com.kqp.ezpas.block;

import com.kqp.ezpas.block.entity.pullerpipe.FilteredPipeBlockEntity;
import com.kqp.ezpas.init.Ezpas;
import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class FilteredPipeBlock extends BlockWithEntity {
    public final Type type;

    public FilteredPipeBlock(Type type) {
        super(FabricBlockSettings.of(Material.GLASS).strength(0.3F, 0.3F).nonOpaque().sounds(BlockSoundGroup.GLASS).nonOpaque().build());

        this.type = type;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockView view) {
        return new FilteredPipeBlockEntity();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof FilteredPipeBlockEntity) {
                ContainerProviderRegistry.INSTANCE.openContainer(Ezpas.FILTERED_PIPE_ID, player, buf -> buf.writeBlockPos(pos));
            }
        }

        return ActionResult.SUCCESS;
    }

    public enum Type {
        WHITELIST,
        BLACKLIST;
    }
}
