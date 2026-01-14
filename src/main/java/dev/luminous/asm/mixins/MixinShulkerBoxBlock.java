package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.misc.ShulkerViewer;
import java.util.List;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBoxBlock.class)
public class MixinShulkerBoxBlock {
   @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true)
   private void onAppendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType options, CallbackInfo ci) {
      if (ShulkerViewer.INSTANCE.isOn() && ShulkerViewer.INSTANCE.toolTips.getValue()) {
         ci.cancel();
      }
   }
}
