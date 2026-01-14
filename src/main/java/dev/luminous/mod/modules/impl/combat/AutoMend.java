package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class AutoMend extends Module {
   public static AutoMend INSTANCE;
   public final BooleanSetting rotation = this.add(new BooleanSetting("Rotation", true).setParent());
   private final BooleanSetting instant = this.add(new BooleanSetting("Instant", false, this.rotation::isOpen));
   public final BooleanSetting onlyBroken = this.add(new BooleanSetting("OnlyBroken", true));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 3, 0, 5));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   public final EnumSetting<SwingSide> interactSwing = this.add(new EnumSetting("InteractSwing", SwingSide.All));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", true));
   public final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final Timer delayTimer = new Timer();
   boolean lookDown = false;
   int exp = 0;
   private boolean throwing = false;

   public AutoMend() {
      super("AutoMend", Module.Category.Combat);
      this.setChinese("自动经验瓶");
      INSTANCE = this;
   }

   @Override
   public void onDisable() {
      this.throwing = false;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.throwing = this.checkThrow();
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (this.lookDown
            && this.isThrow()
            && this.delayTimer.passed(this.delay.getValueInt() * 20L)
            && (!this.onlyGround.getValue() || mc.player.isOnGround())) {
            this.exp = InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE) - 1;
            if (this.rotation.getValue() && this.instant.getValue()) {
               Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 88.0F);
            }

            this.throwExp();
            if (this.rotation.getValue() && this.instant.getValue()) {
               Alien.ROTATION.snapBack();
            }
         }

         if (this.autoDisable.getValue() && !this.isThrow()) {
            this.disable();
         }
      }
   }

   @Override
   public void onEnable() {
      this.lookDown = !this.rotation.getValue() || this.instant.getValue();
      if (nullCheck()) {
         this.disable();
      } else {
         this.exp = InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE);
      }
   }

   @Override
   public String getInfo() {
      return String.valueOf(this.exp);
   }

   public void throwExp() {
      int oldSlot = mc.player.getInventory().selectedSlot;
      int newSlot;
      if (this.inventory.getValue() && (newSlot = InventoryUtil.findItemInventorySlotFromZero(Items.EXPERIENCE_BOTTLE)) != -1) {
         InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)this.interactSwing.getValue());
         InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
         EntityUtil.syncInventory();
         this.delayTimer.reset();
      } else if ((newSlot = InventoryUtil.findItem(Items.EXPERIENCE_BOTTLE)) != -1) {
         InventoryUtil.switchToSlot(newSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)this.interactSwing.getValue());
         InventoryUtil.switchToSlot(oldSlot);
         this.delayTimer.reset();
      }
   }

   @EventListener(priority = -200)
   public void RotateEvent(UpdateRotateEvent event) {
      if (this.rotation.getValue() && !this.instant.getValue()) {
         if (this.isThrow()) {
            event.setPitch(88.0F);
            this.lookDown = true;
         }
      }
   }

   public boolean isThrow() {
      return this.throwing;
   }

   public boolean checkThrow() {
      if (this.isOff()) {
         return false;
      } else if (mc.currentScreen != null) {
         return false;
      } else if (this.usingPause.getValue() && mc.player.isUsingItem()) {
         return false;
      } else if (InventoryUtil.findItem(Items.EXPERIENCE_BOTTLE) != -1
         || this.inventory.getValue() && InventoryUtil.findItemInventorySlotFromZero(Items.EXPERIENCE_BOTTLE) != -1) {
         if (this.onlyBroken.getValue()) {
            for (ItemStack armor : mc.player.getInventory().armor) {
               if (!armor.isEmpty() && EntityUtil.getDamagePercent(armor) < 100) {
                  ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(armor);
                  return enchants.getEnchantments()
                     .contains(mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.MENDING.getRegistryRef()).getOptional(Enchantments.MENDING).get());
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }
}
