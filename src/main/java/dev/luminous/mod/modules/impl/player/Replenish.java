package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class Replenish extends Module {
   private final EnumSetting<Replenish.Mode> mode = this.add(new EnumSetting("Mode", Replenish.Mode.QuickMove));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 2.0, 0.0, 5.0, 0.01).setSuffix("s"));
   private final SliderSetting min = this.add(new SliderSetting("Min", 50, 1, 100)).setSuffix("%");
   private final SliderSetting forceDelay = this.add(new SliderSetting("ForceDelay", 0.2, 0.0, 4.0, 0.01).setSuffix("s"));
   private final SliderSetting forceMin = this.add(new SliderSetting("ForceMin", 16, 1, 100)).setSuffix("%");
   private final Timer timer = new Timer();

   public Replenish() {
      super("Replenish", Module.Category.Player);
      this.setChinese("物品栏补充");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      for (int i = 0; i < 9; i++) {
         if (this.replenish(i)) {
            this.timer.reset();
            return;
         }
      }
   }

   private boolean replenish(int slot) {
      ItemStack stack = mc.player.getInventory().getStack(slot);
      if (stack.isEmpty()) {
         return false;
      } else if (!stack.isStackable()) {
         return false;
      } else {
         int percent = (int)((double)stack.getCount() / stack.getMaxCount() * 100.0);
         if (percent > this.min.getValue()) {
            return false;
         } else {
            for (int i = 9; i < 36; i++) {
               ItemStack item = mc.player.getInventory().getStack(i);
               if (!item.isEmpty() && Sorter.canMerge(stack, item)) {
                  if (percent > this.forceMin.getValueFloat()) {
                     if (!this.timer.passedS(this.delay.getValue())) {
                        return false;
                     }
                  } else if (!this.timer.passedS(this.forceDelay.getValue())) {
                     return false;
                  }

                  switch ((Replenish.Mode)this.mode.getValue()) {
                     case QuickMove:
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                        break;
                     case ClickSlot:
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot + 36, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                  }

                  return true;
               }
            }

            return false;
         }
      }
   }

   public static enum Mode {
      QuickMove,
      ClickSlot;
   }
}
