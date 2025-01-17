package com.github.tartaricacid.touhoulittlemaid.client.event;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = TouhouLittleMaid.MOD_ID, value = Dist.CLIENT)
public final class AddInformationEvent {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderTooltips(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        if (BaubleManager.getBauble(stack) != null) {
            event.getToolTip().add(Component.literal(" "));
            event.getToolTip().add(Component.translatable("tooltips.touhou_little_maid.bauble.desc"));
            event.getToolTip().add(Component.translatable("tooltips.touhou_little_maid.bauble.usage").withStyle(ChatFormatting.GRAY));
        }
        if (BackpackManager.findBackpack(stack).isPresent()) {
            event.getToolTip().add(Component.literal(" "));
            event.getToolTip().add(Component.translatable("tooltips.touhou_little_maid.backpack.desc"));
            event.getToolTip().add(Component.translatable("tooltips.touhou_little_maid.backpack.usage").withStyle(ChatFormatting.GRAY));
        }
    }
}
