package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;

public class AntiWeak extends Module {
   private final EnumSetting<AntiWeak.SwapMode> swapMode = this.add(new EnumSetting("SwapMode", AntiWeak.SwapMode.Inventory));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 500).setSuffix("ms"));
   private final BooleanSetting onlyCrystal = this.add(new BooleanSetting("OnlyCrystal", true));
   private final Timer delayTimer = new Timer();
   boolean ignore = false;
   private PlayerInteractEntityC2SPacket lastPacket = null;

   public AntiWeak() {
      super("AntiWeak", Module.Category.Combat);
      this.setChinese("反虚弱");
   }

   @Override
   public String getInfo() {
      return ((AntiWeak.SwapMode)this.swapMode.getValue()).name();
   }

   @EventListener(priority = 200)
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (!event.isCancelled()) {
            if (!this.ignore) {
               if (mc.player.getStatusEffect(StatusEffects.WEAKNESS) != null) {
                  if (!(mc.player.getMainHandStack().getItem() instanceof SwordItem)) {
                     if (this.delayTimer.passedMs(this.delay.getValue())) {
                        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet && Criticals.getInteractType(packet) == InteractType.ATTACK) {
                           if (this.onlyCrystal.getValue() && !(Criticals.getEntity(packet) instanceof EndCrystalEntity)) {
                              return;
                           }

                           this.lastPacket = packet;
                           this.delayTimer.reset();
                           this.ignore = true;
                           this.doAnti();
                           this.ignore = false;
                           event.cancel();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void doAnti() {
      if (this.lastPacket != null) {
         int strong;
         if (this.swapMode.getValue() != AntiWeak.SwapMode.Inventory) {
            strong = InventoryUtil.findClass(SwordItem.class);
         } else {
            strong = InventoryUtil.findClassInventorySlot(SwordItem.class);
         }

         if (strong != -1) {
            int old = mc.player.getInventory().selectedSlot;
            if (this.swapMode.getValue() != AntiWeak.SwapMode.Inventory) {
               InventoryUtil.switchToSlot(strong);
            } else {
               InventoryUtil.inventorySwap(strong, mc.player.getInventory().selectedSlot);
            }

            mc.getNetworkHandler().sendPacket(this.lastPacket);
            if (this.swapMode.getValue() != AntiWeak.SwapMode.Inventory) {
               if (this.swapMode.getValue() != AntiWeak.SwapMode.Normal) {
                  InventoryUtil.switchToSlot(old);
               }
            } else {
               InventoryUtil.inventorySwap(strong, mc.player.getInventory().selectedSlot);
               EntityUtil.syncInventory();
            }
         }
      }
   }

   public static enum SwapMode {
      Normal,
      Silent,
      Inventory;
   }
}
