package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Offhand extends Module {
   private final EnumSetting<Offhand.OffhandItem> item = this.add(new EnumSetting("Item", Offhand.OffhandItem.Totem));
   private final BooleanSetting safe = this.add(new BooleanSetting("Safe", true).setParent());
   private final SliderSetting safeHealth = this.add(new SliderSetting("Health", 16.0, 0.0, 36.0, 0.1, this.safe::isOpen));
   private final BooleanSetting lethalCrystal = this.add(new BooleanSetting("LethalCrystal", true, this.safe::isOpen));
   private final BooleanSetting gapSwitch = this.add(new BooleanSetting("GapSwitch", true).setParent());
   private final BooleanSetting always = this.add(new BooleanSetting("Always", false, this.gapSwitch::isOpen));
   private final BooleanSetting gapOnTotem = this.add(new BooleanSetting("Gap-Totem", false, this.gapSwitch::isOpen));
   private final BooleanSetting gapOnSword = this.add(new BooleanSetting("Gap-Sword", true, this.gapSwitch::isOpen));
   private final BooleanSetting gapOnPick = this.add(new BooleanSetting("Gap-Pickaxe", false, this.gapSwitch::isOpen));
   private final BooleanSetting mainHandTotem = this.add(new BooleanSetting("MainHandTotem", false).setParent());
   private final SliderSetting slot = this.add(new SliderSetting("Slot", 1.0, 1.0, 9.0, 1.0, this.mainHandTotem::isOpen));
   private final BooleanSetting forceUpdate = this.add(new BooleanSetting("ForceUpdate", false, this.mainHandTotem::isOpen));
   private final BooleanSetting withOffhand = this.add(new BooleanSetting("WithOffhand", false, this.mainHandTotem::isOpen));
   private final EnumSetting<Offhand.SwapMode> swapMode = this.add(new EnumSetting("SwapMode", Offhand.SwapMode.OffhandSwap));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 50.0, 0.0, 500.0, 1.0));
   private final EnumSetting<Timing> timing = this.add(new EnumSetting("Timing", Timing.All));
   private final Timer timer = new Timer();

   public Offhand() {
      super("Offhand", Module.Category.Combat);
      this.setChinese("副手物品");
   }

   @EventListener
   public void totem(TotemEvent event) {
      if (event.getPlayer() == mc.player) {
         if (mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            mc.player.getInventory().removeStack(0);
         } else if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            mc.player.getInventory().offHand.set(0, ItemStack.EMPTY);
         }
      }
   }

   private boolean lethalCrystal() {
      if (!this.lethalCrystal.getValue()) {
         return false;
      } else {
         for (Entity entity : Alien.THREAD.getEntities()) {
            if (entity instanceof EndCrystalEntity
               && mc.player.distanceTo(entity) <= 12.0F
               && AutoCrystal.INSTANCE.calculateDamage(new Vec3d(entity.getX(), entity.getY(), entity.getZ()), mc.player, mc.player)
                  >= EntityUtil.getHealth(mc.player)) {
               return true;
            }
         }

         return false;
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
            if (this.timer.passed(this.delay.getValueInt())) {
               if (EntityUtil.inInventory()) {
                  boolean switchMainHandTotem = mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING || this.withOffhand.getValue();
                  boolean unsafe = EntityUtil.getHealth(mc.player) < this.safeHealth.getValue() || this.lethalCrystal();
                  if (this.mainHandTotem.getValue()) {
                     int totemSlot = InventoryUtil.findItemInventorySlot(Items.TOTEM_OF_UNDYING);
                     if (totemSlot != -1 && mc.player.getInventory().getStack(this.slot.getValueInt() - 1).getItem() != Items.TOTEM_OF_UNDYING) {
                        switch ((Offhand.SwapMode)this.swapMode.getValue()) {
                           case ClickSlot:
                              mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totemSlot, 0, SlotActionType.PICKUP, mc.player);
                              mc.interactionManager
                                 .clickSlot(mc.player.currentScreenHandler.syncId, this.slot.getValueInt() - 1 + 36, 0, SlotActionType.PICKUP, mc.player);
                              mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totemSlot, 0, SlotActionType.PICKUP, mc.player);
                              EntityUtil.syncInventory();
                              break;
                           case OffhandSwap:
                              mc.interactionManager
                                 .clickSlot(mc.player.currentScreenHandler.syncId, totemSlot, this.slot.getValueInt() - 1, SlotActionType.SWAP, mc.player);
                              EntityUtil.syncInventory();
                              break;
                           case Pick:
                              int old = mc.player.getInventory().selectedSlot;
                              InventoryUtil.switchToSlot(this.slot.getValueInt() - 1);
                              mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(totemSlot));
                              InventoryUtil.switchToSlot(old);
                        }

                        if (switchMainHandTotem
                           && (!this.safe.getValue() || unsafe)
                           && (this.slot.getValueInt() - 1 != mc.player.getInventory().selectedSlot || this.forceUpdate.getValue())) {
                           InventoryUtil.switchToSlot(this.slot.getValueInt() - 1);
                        }

                        this.timer.reset();
                        return;
                     }
                  }

                  if (this.safe.getValue()) {
                     if (unsafe) {
                        if (!this.mainHandTotem.getValue() || !switchMainHandTotem) {
                           this.swap(Items.TOTEM_OF_UNDYING);
                           this.timer.reset();
                           return;
                        }

                        int hotBarSlot = InventoryUtil.findItem(Items.TOTEM_OF_UNDYING);
                        if (hotBarSlot != -1 && (hotBarSlot != mc.player.getInventory().selectedSlot || this.forceUpdate.getValue())) {
                           InventoryUtil.switchToSlot(hotBarSlot);
                        }
                     }
                  } else if (this.mainHandTotem.getValue() && switchMainHandTotem) {
                     int hotBarSlot = InventoryUtil.findItem(Items.TOTEM_OF_UNDYING);
                     if (hotBarSlot != -1 && (hotBarSlot != mc.player.getInventory().selectedSlot || this.forceUpdate.getValue())) {
                        InventoryUtil.switchToSlot(hotBarSlot);
                     }
                  }

                  if ((
                        this.gapOnSword.getValue() && mc.player.getMainHandStack().getItem() instanceof SwordItem
                           || this.always.getValue()
                              && mc.player.getMainHandStack().getItem() != Items.GOLDEN_APPLE
                              && mc.player.getMainHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE
                           || this.gapOnPick.getValue() && mc.player.getMainHandStack().getItem() instanceof PickaxeItem
                           || this.gapOnTotem.getValue() && mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING
                     )
                     && mc.options.useKey.isPressed()
                     && this.gapSwitch.getValue()) {
                     this.swap(Items.GOLDEN_APPLE);
                     this.timer.reset();
                  } else {
                     EnumSetting<Offhand.OffhandItem> item = this.item;
                     Offhand.OffhandItem i = (Offhand.OffhandItem)item.getValue();
                     if (i == Offhand.OffhandItem.Shield) {
                        this.swap(Items.SHIELD);
                        this.timer.reset();
                     } else if (i == Offhand.OffhandItem.Chorus) {
                        this.swap(Items.CHORUS_FRUIT);
                        this.timer.reset();
                     } else if (i == Offhand.OffhandItem.Crystal) {
                        this.swap(Items.END_CRYSTAL);
                        this.timer.reset();
                     } else if (i == Offhand.OffhandItem.Totem) {
                        this.swap(Items.TOTEM_OF_UNDYING);
                        this.timer.reset();
                     } else {
                        if (i == Offhand.OffhandItem.Gapple) {
                           this.swap(Items.GOLDEN_APPLE);
                           this.timer.reset();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void swap(Item item) {
      int itemSlot = item == Items.GOLDEN_APPLE ? this.getGAppleSlot() : this.findItemInventorySlot(item);
      if (itemSlot != -1) {
         switch ((Offhand.SwapMode)this.swapMode.getValue()) {
            case ClickSlot:
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
               EntityUtil.syncInventory();
               break;
            case OffhandSwap:
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 40, SlotActionType.SWAP, mc.player);
               EntityUtil.syncInventory();
               break;
            case Pick:
               mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(itemSlot));
               mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN, 0));
               mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(itemSlot));
         }
      }
   }

   private int getGAppleSlot() {
      return this.findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE) != -1
         ? this.findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE)
         : this.findItemInventorySlot(Items.GOLDEN_APPLE);
   }

   @Override
   public String getInfo() {
      return ((Offhand.OffhandItem)this.item.getValue()).name();
   }

   public int findItemInventorySlot(Item item) {
      if (mc.player.getOffHandStack().getItem() == Items.GOLDEN_APPLE && item == Items.GOLDEN_APPLE) {
         return -1;
      } else if (mc.player.getOffHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE || item != Items.GOLDEN_APPLE && item != Items.ENCHANTED_GOLDEN_APPLE) {
         if (item == mc.player.getOffHandStack().getItem()) {
            return -1;
         } else {
            switch ((Offhand.SwapMode)this.swapMode.getValue()) {
               case ClickSlot:
               case OffhandSwap:
                  for (int i = 44; i >= 0; i--) {
                     ItemStack stack = mc.player.getInventory().getStack(i);
                     if (stack.getItem() == item) {
                        return i < 9 ? i + 36 : i;
                     }
                  }
                  break;
               case Pick:
                  for (int ix = 9; ix < mc.player.getInventory().size() + 1; ix++) {
                     ItemStack s = mc.player.getInventory().getStack(ix);
                     if (s.getItem() == item) {
                        return ix;
                     }
                  }

                  for (int ixx = 0; ixx < 9; ixx++) {
                     ItemStack s = mc.player.getInventory().getStack(ixx);
                     if (s.getItem() == item) {
                        return ixx;
                     }
                  }
            }

            return -1;
         }
      } else {
         return -1;
      }
   }

   public static enum OffhandItem {
      None,
      Totem,
      Crystal,
      Gapple,
      Shield,
      Chorus;
   }

   public static enum SwapMode {
      ClickSlot,
      OffhandSwap,
      Pick;
   }
}
