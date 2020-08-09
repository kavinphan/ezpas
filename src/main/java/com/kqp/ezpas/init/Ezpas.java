package com.kqp.ezpas.init;

import com.kqp.ezpas.block.ColoredPipeBlock;
import com.kqp.ezpas.block.FilteredPipeBlock;
import com.kqp.ezpas.block.PipeBlock;
import com.kqp.ezpas.block.container.FilteredPipeScreenHandler;
import com.kqp.ezpas.block.entity.FilteredPipeBlockEntity;
import com.kqp.ezpas.block.entity.pullerpipe.*;
import com.kqp.ezpas.block.pullerpipe.*;
import com.kqp.ezpas.item.PipeProbeItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class Ezpas implements ModInitializer {
    public static final String ID = "ezpas";

    public static final Item PIPE_PROBE = register("pipe_probe", new PipeProbeItem());

    public static final Block IRON_PP = register("iron_puller_pipe", new IronPullerPipeBlock());
    public static final Block GOLD_PP = register("gold_puller_pipe", new GoldPullerPipeBlock());
    public static final Block DIAMOND_PP = register("diamond_puller_pipe", new DiamondPullerPipeBlock());
    public static final Block NETHERITE_PP = register("netherite_puller_pipe", new NetheritePullerPipeBlock());
    public static final Block ENDER_PP = register("ender_puller_pipe", new EnderPullerPipeBlock());

    public static BlockEntityType<IronPullerPipeBlockEntity> IRON_PP_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, id("iron_puller_pipe"),
            BlockEntityType.Builder.create(IronPullerPipeBlockEntity::new, IRON_PP).build(null));

    public static BlockEntityType<GoldPullerPipeBlockEntity> GOLD_PP_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, id("gold_puller_pipe"),
            BlockEntityType.Builder.create(GoldPullerPipeBlockEntity::new, GOLD_PP).build(null));

    public static BlockEntityType<DiamondPullerPipeBlockEntity> DIAMOND_PP_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, id("diamond_puller_pipe"),
            BlockEntityType.Builder.create(DiamondPullerPipeBlockEntity::new, DIAMOND_PP).build(null));

    public static BlockEntityType<NetheritePullerPipeBlockEntity> NETHERITE_PP_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, id("netherite_puller_pipe"),
            BlockEntityType.Builder.create(NetheritePullerPipeBlockEntity::new, DIAMOND_PP).build(null));

    public static BlockEntityType<EnderPullerPipeBlockEntity> ENDER_PP_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, id("ender_puller_pipe"),
            BlockEntityType.Builder.create(EnderPullerPipeBlockEntity::new, ENDER_PP).build(null));

    public static BlockEntityType<FilteredPipeBlockEntity> FILTERED_PIPE_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, id("filtered_pipe"),
            BlockEntityType.Builder.create(FilteredPipeBlockEntity::new, ENDER_PP).build(null));

    public static final Block PIPE = register("pipe", new PipeBlock());
    public static final Block DENSE_PIPE = register("dense_pipe", new PipeBlock());

    public static final Block FILTERED_PIPE_WHITELIST = register("filtered_pipe_whitelist", new FilteredPipeBlock(FilteredPipeBlock.Type.WHITELIST));
    public static final Block FILTERED_PIPE_BLACKLIST = register("filtered_pipe_blacklist", new FilteredPipeBlock(FilteredPipeBlock.Type.BLACKLIST));

    public static final Block[] COLORED_PIPES = new Block[DyeColor.values().length];

    public static final ScreenHandlerType<FilteredPipeScreenHandler> FILTERED_PIPE_SCREEN_HANDLER_TYPE =
            ScreenHandlerRegistry.registerExtended(id("filtered_pipe"), (syncId, inv, buf) -> {
                final World world = inv.player.world;
                final BlockPos pos = buf.readBlockPos();

                return (FilteredPipeScreenHandler) world.getBlockState(pos).createScreenHandlerFactory(world, pos).createMenu(syncId, inv, inv.player);
            });

    @Override
    public void onInitialize() {
    }

    private static Block register(String name, Block block) {
        Registry.register(Registry.BLOCK, id(name), block);
        Registry.register(Registry.ITEM, id(name), new BlockItem(block, new Item.Settings().group(ItemGroup.REDSTONE)));

        return block;
    }

    private static Item register(String name, Item item) {
        Registry.register(Registry.ITEM, id(name), item);

        return item;
    }

    public static Identifier id(String path) {
        return new Identifier(ID, path);
    }

    static {
        for (int i = 0; i < DyeColor.values().length; i++) {
            DyeColor color = DyeColor.values()[i];

            COLORED_PIPES[i] = register(color.getName() + "_stained_pipe", new ColoredPipeBlock());
        }
    }
}
