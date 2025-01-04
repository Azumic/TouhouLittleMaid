package com.github.tartaricacid.touhoulittlemaid.event.maid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

@Mod.EventBusSubscriber
public class PotionItemUse {
    @SubscribeEvent
    public static void onMaidPotionItemUse(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof EntityMaid maid && event.getItem().getItem() instanceof PotionItem) {
            ItemStack potionStack = event.getItem();
            // 原版药水非玩家使用后会原样返回，我们需要正确扣掉内容
            potionStack.shrink(1);
            // 说明喝的是堆叠的药水，需要主动给女仆加瓶子
            if (!potionStack.isEmpty()) {
                CombinedInvWrapper inv = maid.getAvailableInv(false);
                ItemStack leftStack = ItemHandlerHelper.insertItemStacked(inv, new ItemStack(Items.GLASS_BOTTLE), false);
                // 如果背包满了，那就生成掉落物，预防一些改动物品堆叠的模组
                if (!leftStack.isEmpty()) {
                    maid.spawnAtLocation(leftStack);
                }
                event.setResultStack(potionStack);
            } else {
                event.setResultStack(new ItemStack(Items.GLASS_BOTTLE));
            }
        }
    }
}
