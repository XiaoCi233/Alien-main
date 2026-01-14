package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.item.ElytraItem;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class AutoArmor extends Module {
   public static AutoArmor INSTANCE;
   private final EnumSetting<AutoArmor.EnchantPriority> head = this.add(new EnumSetting("Head", AutoArmor.EnchantPriority.Protection));
   private final EnumSetting<AutoArmor.EnchantPriority> body = this.add(new EnumSetting("Body", AutoArmor.EnchantPriority.Protection));
   private final EnumSetting<AutoArmor.EnchantPriority> tights = this.add(new EnumSetting("Tights", AutoArmor.EnchantPriority.Protection));
   private final EnumSetting<AutoArmor.EnchantPriority> feet = this.add(new EnumSetting("Feet", AutoArmor.EnchantPriority.Protection));
   private final BooleanSetting ignoreCurse = this.add(new BooleanSetting("IgnoreCurse", true));
   private final BooleanSetting noMove = this.add(new BooleanSetting("NoMove", false));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 3.0, 0.0, 10.0, 1.0));
   private final BooleanSetting autoElytra = this.add(new BooleanSetting("AutoElytra", true));
   private final EnumSetting<AutoArmor.HotbarSwapMode> hotbarSwap = this.add(new EnumSetting("HotbarSwap", AutoArmor.HotbarSwapMode.Swap));
   private final EnumSetting<AutoArmor.InventorySwapMode> inventorySwap = this.add(new EnumSetting("InventorySwap", AutoArmor.InventorySwapMode.ClickSlot));
   private int tickDelay = 0;

   public AutoArmor() {
      super("AutoArmor", Module.Category.Player);
      this.setChinese("自动穿甲");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (EntityUtil.inInventory()) {
         if (mc.player.playerScreenHandler == mc.player.currentScreenHandler) {
            if (!MovementUtil.isMoving() || !this.noMove.getValue()) {
               if (this.tickDelay > 0) {
                  this.tickDelay--;
               } else {
                  this.tickDelay = this.delay.getValueInt();
                  Map<EquipmentSlot, int[]> armorMap = new HashMap(4);
                  armorMap.put(EquipmentSlot.FEET, new int[]{36, this.getProtection(mc.player.getInventory().getStack(36)), -1, -1});
                  armorMap.put(EquipmentSlot.LEGS, new int[]{37, this.getProtection(mc.player.getInventory().getStack(37)), -1, -1});
                  armorMap.put(EquipmentSlot.CHEST, new int[]{38, this.getProtection(mc.player.getInventory().getStack(38)), -1, -1});
                  armorMap.put(EquipmentSlot.HEAD, new int[]{39, this.getProtection(mc.player.getInventory().getStack(39)), -1, -1});

                  for (int s = 0; s < 36; s++) {
                     if ((
                           mc.player.getInventory().getStack(s).getItem() instanceof ArmorItem
                              || mc.player.getInventory().getStack(s).getItem() == Items.ELYTRA
                        )
                        && (
                           mc.player.getInventory().getStack(s).getItem() != Items.ELYTRA
                              || (!ElytraFly.INSTANCE.isOff() || !this.autoElytra.getValue())
                                 && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.packet.getValue())
                        )) {
                        int protection = this.getProtection(mc.player.getInventory().getStack(s));
                        EquipmentSlot slot = mc.player.getInventory().getStack(s).getItem() instanceof ElytraItem
                           ? EquipmentSlot.CHEST
                           : ((ArmorItem)mc.player.getInventory().getStack(s).getItem()).getSlotType();

                        for (Map.Entry<EquipmentSlot, int[]> e : armorMap.entrySet()) {
                           if (this.autoElytra.getValue() && ElytraFly.INSTANCE.isOn() && e.getKey() == EquipmentSlot.CHEST) {
                              if ((
                                    mc.player.getInventory().getStack(38).isEmpty()
                                       || !(mc.player.getInventory().getStack(38).getItem() instanceof ElytraItem)
                                       || !ElytraItem.isUsable(mc.player.getInventory().getStack(38))
                                 )
                                 && (
                                    e.getValue()[2] == -1
                                       || mc.player.getInventory().getStack(e.getValue()[2]).isEmpty()
                                       || !(mc.player.getInventory().getStack(e.getValue()[2]).getItem() instanceof ElytraItem)
                                       || !ElytraItem.isUsable(mc.player.getInventory().getStack(e.getValue()[2]))
                                 )
                                 && !mc.player.getInventory().getStack(s).isEmpty()
                                 && mc.player.getInventory().getStack(s).getItem() instanceof ElytraItem
                                 && ElytraItem.isUsable(mc.player.getInventory().getStack(s))) {
                                 e.getValue()[2] = s;
                              }
                           } else if (protection > 0 && e.getKey() == slot && protection > e.getValue()[1] && protection > e.getValue()[3]) {
                              e.getValue()[2] = s;
                              e.getValue()[3] = protection;
                           }
                        }
                     }
                  }

                  for (Map.Entry<EquipmentSlot, int[]> equipmentSlotEntry : armorMap.entrySet()) {
                     if (equipmentSlotEntry.getValue()[2] != -1) {
                        if (equipmentSlotEntry.getValue()[2] < 9) {
                           switch ((AutoArmor.HotbarSwapMode)this.hotbarSwap.getValue()) {
                              case Swap:
                                 int armorSlot = 44 - equipmentSlotEntry.getValue()[0];
                                 mc.interactionManager
                                    .clickSlot(mc.player.currentScreenHandler.syncId, armorSlot, equipmentSlotEntry.getValue()[2], SlotActionType.SWAP, mc.player);
                                 EntityUtil.syncInventory();
                                 break;
                              case Switch:
                                 int old = mc.player.getInventory().selectedSlot;
                                 InventoryUtil.switchToSlot(equipmentSlotEntry.getValue()[2]);
                                 sendSequencedPacket(
                                    id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                                 );
                                 InventoryUtil.switchToSlot(old);
                           }
                        } else if (mc.player.playerScreenHandler == mc.player.currentScreenHandler) {
                           int armorSlot = 44 - equipmentSlotEntry.getValue()[0];
                           int newArmorSlot = equipmentSlotEntry.getValue()[2];
                           switch ((AutoArmor.InventorySwapMode)this.inventorySwap.getValue()) {
                              case ClickSlot:
                                 mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, mc.player);
                                 mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, armorSlot, 0, SlotActionType.PICKUP, mc.player);
                                 if (equipmentSlotEntry.getValue()[1] != -1) {
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, mc.player);
                                 }
                                 break;
                              case Pick:
                                 mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(newArmorSlot));
                                 sendSequencedPacket(
                                    id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                                 );
                                 mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(newArmorSlot));
                           }

                           EntityUtil.syncInventory();
                        }

                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private int getProtection(ItemStack is) {
      if (is.getItem() instanceof ArmorItem || is.getItem() == Items.ELYTRA) {
         int prot = 0;
         EquipmentSlot slot = is.getItem() instanceof ArmorItem ai ? ai.getSlotType() : EquipmentSlot.BODY;
         if (is.getItem() instanceof ElytraItem) {
            if (!ElytraItem.isUsable(is)) {
               return 0;
            }

            prot = 1;
         }

         int blastMultiplier = 1;
         int protectionMultiplier = 1;
         switch (slot) {
            case HEAD:
               if (this.head.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case BODY:
               if (this.body.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case LEGS:
               if (this.tights.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case FEET:
               if (this.feet.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
         }

         if (is.hasEnchantments()) {
            ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(is);
            if (enchants.getEnchantments()
               .contains(mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.PROTECTION.getRegistryRef()).getOptional(Enchantments.PROTECTION).get())) {
               prot += enchants.getLevel(
                     (RegistryEntry)mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.PROTECTION.getRegistryRef()).getOptional(Enchantments.PROTECTION).get()
                  )
                  * protectionMultiplier;
            }

            if (enchants.getEnchantments()
               .contains(mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.BLAST_PROTECTION.getRegistryRef()).getOptional(Enchantments.BLAST_PROTECTION).get())) {
               prot += enchants.getLevel(
                     mc.world
                        .getRegistryManager()
                        .getWrapperOrThrow(Enchantments.BLAST_PROTECTION.getRegistryRef())
                        .getOptional(Enchantments.BLAST_PROTECTION)
                        .get()
                  )
                  * blastMultiplier;
            }

            if (enchants.getEnchantments()
                  .contains(mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.BLAST_PROTECTION.getRegistryRef()).getOptional(Enchantments.BINDING_CURSE).get())
               && this.ignoreCurse.getValue()) {
               prot = -999;
            }
         }

         return (is.getItem() instanceof ArmorItem ? ((ArmorItem)is.getItem()).getProtection() : 0) + prot;
      } else {
         return !is.isEmpty() ? 0 : -1;
      }
   }

   private static enum EnchantPriority {
      Blast,
      Protection;
   }

   public static enum HotbarSwapMode {
      Swap,
      Switch;
   }

   public static enum InventorySwapMode {
      ClickSlot,
      Pick;
   }
}
