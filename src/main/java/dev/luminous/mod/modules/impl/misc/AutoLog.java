package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;

public class AutoLog extends Module {
   public static boolean loggedOut = false;
   private final BooleanSetting logOnEnable = this.add(new BooleanSetting("LogOnEnable", false));
   private final BooleanSetting onPop = this.add(new BooleanSetting("OnPop", true));
   private final BooleanSetting lowArmor = this.add(new BooleanSetting("LowArmor", true));
   private final BooleanSetting totemLess = this.add(new BooleanSetting("TotemLess", true).setParent());
   private final SliderSetting totems = this.add(new SliderSetting("Totems", 2.0, 0.0, 20.0, 1.0, this.totemLess::isOpen));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final BooleanSetting showReason = this.add(new BooleanSetting("ShowReason", false));

   public AutoLog() {
      super("AutoLog", Module.Category.Misc);
      this.setChinese("自动下线");
   }

   @Override
   public void onEnable() {
      if (this.logOnEnable.getValue()) {
         this.disconnect("Enabled");
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.totemLess.getValue()) {
         int totem = InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING);
         if (totem <= this.totems.getValue()) {
            this.disconnect("You have too few totems (" + totem + ").");
            return;
         }
      }

      if (this.lowArmor.getValue()) {
         for (ItemStack armor : mc.player.getInventory().armor) {
            if (!armor.isEmpty()) {
               int damage = EntityUtil.getDamagePercent(armor);
               if (damage < 5) {
                  this.disconnect("Your armor has a durability of less than 5%.");
                  return;
               }
            }
         }
      }
   }

   @EventListener
   public void onPop(TotemEvent event) {
      if (this.onPop.getValue() && event.getPlayer() == mc.player) {
         this.disconnect("You poped 1 totem!");
      }
   }

   @Override
   public void onLogout() {
      if (this.autoDisable.getValue()) {
         this.disable();
      }
   }

   private void disconnect(String reason) {
      loggedOut = true;
      CommandManager.sendMessage("§4[AutoLog] " + reason);
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(114514));
      if (this.showReason.getValue()) {
         mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[AutoLog]" + reason)));
      }
   }
}
