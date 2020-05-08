package net.kqp.ezpas.item;

import net.kqp.ezpas.block.entity.PullerPipeBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class PipeProbeItem extends Item {
    public PipeProbeItem() {
        super(new Item.Settings().group(ItemGroup.REDSTONE).maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();

        if (!world.isClient) {
            BlockPos pos = context.getBlockPos();
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof PullerPipeBlockEntity) {
                List<PullerPipeBlockEntity.ValidInventory> invList = ((PullerPipeBlockEntity) be).getValidInventories();

                StringBuilder sb = new StringBuilder();

                if (invList.isEmpty()) {
                    sb.append("No blocks are connected");
                } else {
                    sb.append("Currently connected to the following blocks: \n");

                    for (PullerPipeBlockEntity.ValidInventory inventory : invList) {
                        sb.append(String.format("%s@(%d, %d, %d), %s\n",
                                world.getBlockState(inventory.blockPos).getBlock().getTranslationKey(),
                                inventory.blockPos.getX(),
                                inventory.blockPos.getY(),
                                inventory.blockPos.getZ(),
                                inventory.direction));
                    }
                }

                context.getPlayer().sendMessage(new LiteralText(sb.toString()), false);
            }
        }

        return ActionResult.SUCCESS;
    }
}
