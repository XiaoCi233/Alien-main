package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class AutoPot extends Module {
   public static AutoPot INSTANCE;
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 5.0, 0.0, 10.0, 0.1).setSuffix("s"));
   private final BooleanSetting speed = this.add(new BooleanSetting("Speed", false));
   private final BooleanSetting resistance = this.add(new BooleanSetting("Resistance", false));
   private final BooleanSetting strength = this.add(new BooleanSetting("Strength", false));
   private final BooleanSetting slowFalling = this.add(new BooleanSetting("SlowFalling", false));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BindSetting speedKey = this.add(new BindSetting("SpeedKey", -1));
   private final BindSetting strengthKey = this.add(new BindSetting("StrengthKey", -1));
   private final BindSetting resistanceKey = this.add(new BindSetting("ResistanceKey", -1));
   private final Timer delayTimer = new Timer();
   private boolean throwing = false;
   private boolean turtlePress;
   private boolean speedPress;
   private boolean strengthPress;

   public AutoPot() {
      super("AutoPot", Module.Category.Player);
      this.setChinese("自动药水");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new AutoPot.AutoPotTick());
   }

   public static int findPotionInventorySlot(StatusEffect targetEffect) {
      for (int i = 35; i >= 0; i--) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (Item.getRawId(itemStack.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = (PotionContentsComponent)itemStack.getOrDefault(
               DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT
            );

            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == targetEffect) {
                  return i < 9 ? i + 36 : i;
               }
            }
         }
      }

      return -1;
   }

   public static int findPotion(StatusEffect targetEffect) {
      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (Item.getRawId(itemStack.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = (PotionContentsComponent)itemStack.getOrDefault(
               DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT
            );

            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == targetEffect) {
                  return i;
               }
            }
         }
      }

      return -1;
   }

   @Override
   public void onDisable() {
      this.throwing = false;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (this.delayTimer.passedMs(this.delay.getValue() * 1000.0)) {
            if (!this.onlyGround.getValue()
               || (mc.player.isOnGround() || Alien.PLAYER.isInWeb(mc.player))
                  && !mc.world.isAir(new BlockPosX(mc.player.getPos().add(0.0, -1.0, 0.0)))) {
               if (this.resistance.getValue()
                  && (!mc.player.hasStatusEffect(StatusEffects.RESISTANCE) || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() < 2)) {
                  this.throwing = this.checkThrow((StatusEffect)StatusEffects.RESISTANCE.value());
                  if (this.isThrow()) {
                     this.throwPotion((StatusEffect)StatusEffects.RESISTANCE.value());
                     return;
                  }
               }

               if (this.speed.getValue() && !mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                  this.throwing = this.checkThrow((StatusEffect)StatusEffects.SPEED.value());
                  if (this.isThrow()) {
                     this.throwPotion((StatusEffect)StatusEffects.SPEED.value());
                     return;
                  }
               }

               if (this.strength.getValue() && !mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
                  this.throwing = this.checkThrow((StatusEffect)StatusEffects.STRENGTH.value());
                  if (this.isThrow()) {
                     this.throwPotion((StatusEffect)StatusEffects.STRENGTH.value());
                     return;
                  }
               }

               if (this.slowFalling.getValue() && !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                  this.throwing = this.checkThrow((StatusEffect)StatusEffects.SLOW_FALLING.value());
                  if (this.isThrow()) {
                     this.throwPotion((StatusEffect)StatusEffects.SLOW_FALLING.value());
                  }
               }
            }
         }
      }
   }

   public void throwPotion(StatusEffect targetEffect) {
      int oldSlot = mc.player.getInventory().selectedSlot;
      int newSlot;
      if (this.inventory.getValue() && (newSlot = findPotionInventorySlot(targetEffect)) != -1) {
         Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 90.0F);
         InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
         EntityUtil.syncInventory();
         Alien.ROTATION.snapBack();
         this.delayTimer.reset();
      } else if ((newSlot = findPotion(targetEffect)) != -1) {
         Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 90.0F);
         InventoryUtil.switchToSlot(newSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         InventoryUtil.switchToSlot(oldSlot);
         Alien.ROTATION.snapBack();
         this.delayTimer.reset();
      }
   }

   public boolean isThrow() {
      return this.throwing;
   }

   public boolean checkThrow(StatusEffect targetEffect) {
      if (!EntityUtil.inInventory()) {
         return false;
      } else {
         return this.usingPause.getValue() && mc.player.isUsingItem()
            ? false
            : findPotion(targetEffect) != -1 || this.inventory.getValue() && findPotionInventorySlot(targetEffect) != -1;
      }
   }

   public class AutoPotTick {
      @EventListener
      public void onTick(ClientTickEvent event) {
         if (!Module.nullCheck() && !event.isPost()) {
            if (!AutoPot.this.inventory.getValue() || EntityUtil.inInventory()) {
               if (Wrapper.mc.currentScreen == null) {
                  if (AutoPot.this.resistanceKey.isPressed()) {
                     if (!AutoPot.this.turtlePress && AutoPot.this.checkThrow((StatusEffect)StatusEffects.RESISTANCE.value())) {
                        AutoPot.this.throwPotion((StatusEffect)StatusEffects.RESISTANCE.value());
                        AutoPot.this.turtlePress = true;
                        return;
                     }
                  } else {
                     AutoPot.this.turtlePress = false;
                  }

                  if (AutoPot.this.strengthKey.isPressed()) {
                     if (!AutoPot.this.strengthPress && AutoPot.this.checkThrow((StatusEffect)StatusEffects.STRENGTH.value())) {
                        AutoPot.this.throwPotion((StatusEffect)StatusEffects.STRENGTH.value());
                        AutoPot.this.strengthPress = true;
                        return;
                     }
                  } else {
                     AutoPot.this.strengthPress = false;
                  }

                  if (AutoPot.this.speedKey.isPressed()) {
                     if (!AutoPot.this.speedPress && AutoPot.this.checkThrow((StatusEffect)StatusEffects.SPEED.value())) {
                        AutoPot.this.throwPotion((StatusEffect)StatusEffects.SPEED.value());
                        AutoPot.this.speedPress = true;
                     }
                  } else {
                     AutoPot.this.speedPress = false;
                  }
               } else {
                  AutoPot.this.speedPress = false;
                  AutoPot.this.turtlePress = false;
                  AutoPot.this.strengthPress = false;
               }
            }
         }
      }
   }
}
