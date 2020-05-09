package com.kqp.ezpas.item;

import com.kqp.ezpas.block.entity.pullerpipe.PullerPipeBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
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
                PullerPipeBlockEntity ppBe = ((PullerPipeBlockEntity) be);
                List<PullerPipeBlockEntity.ValidInventory> invList = ppBe.getValidInventories();

                StringBuilder sb = new StringBuilder();

                sb.append("Insertion points: \n");
                if (invList.isEmpty()) {
                    sb.append("None\n");
                } else {
                    for (PullerPipeBlockEntity.ValidInventory inventory : invList) {
                        sb.append(String.format("%s@(%d, %d, %d), %s\n",
                                world.getBlockState(inventory.blockPos).getBlock().getTranslationKey(),
                                inventory.blockPos.getX(),
                                inventory.blockPos.getY(),
                                inventory.blockPos.getZ(),
                                inventory.direction));
                    }
                }

                sb.append("Whitelisted: \n");
                if (ppBe.whitelist.isEmpty()) {
                    sb.append("None\n");
                } else {
                    for (ItemStack stack : ppBe.whitelist) {
                        sb.append(stack.getItem().getTranslationKey() + "\n");
                    }
                }

                sb.append("Blacklisted: \n");
                if (ppBe.blacklist.isEmpty()) {
                    sb.append("None\n");
                } else {
                    for (ItemStack stack : ppBe.blacklist) {
                        sb.append(stack.getItem().getTranslationKey() + "\n");
                    }
                }

                context.getPlayer().sendMessage(new LiteralText(sb.toString()));
            }
        }

        return ActionResult.SUCCESS;
    }
}
