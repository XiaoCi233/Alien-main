package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PlaySoundEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.event.KeyEvent;
import net.minecraft.block.AirBlock;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class AutoTool extends Module {
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final BooleanSetting fish = this.add(new BooleanSetting("Fish", true).setParent());
   private final BooleanSetting autoCast = this.add(new BooleanSetting("AutoCast", true, this.fish::isOpen));
   private final SliderSetting ticksAutoCast = this.add(new SliderSetting("TicksAutoCast", 10, 0, 60, this.fish::isOpen));
   private final SliderSetting ticksCatch = this.add(new SliderSetting("TicksCatch", 6, 0, 60, this.fish::isOpen));
   private final SliderSetting ticksThrow = this.add(new SliderSetting("TicksThrow", 14, 0, 60, this.fish::isOpen));
   private final BooleanSetting splashDetection = this.add(new BooleanSetting("SplashDetection", false, this.fish::isOpen));
   private final SliderSetting splashDetectionRange = this.add(new SliderSetting("DetectionRange", 10, 0, 60, this.fish::isOpen));
   private boolean ticksEnabled;
   private int ticksToRightClick;
   private int ticksData;
   private int autoCastTimer;
   private boolean autoCastEnabled;
   private int autoCastCheckTimer;

   public AutoTool() {
      super("AutoTool", Module.Category.Player);
      this.setChinese("自动工具");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.autoFish();
      this.autoTool();
   }

   public void autoTool() {
      if (this.mine.getValue()) {
         if (mc.crosshairTarget instanceof BlockHitResult result) {
            BlockPos var4 = result.getBlockPos();
            if (!mc.world.isAir(var4)) {
               int tool = getTool(var4);
               if (tool != -1 && mc.options.attackKey.isPressed()) {
                  mc.player.getInventory().selectedSlot = tool;
               }
            }
         }
      }
   }

   @Override
   public void onEnable() {
      this.ticksEnabled = false;
      this.autoCastEnabled = false;
      this.autoCastCheckTimer = 0;
   }

   @EventListener
   private void onPlaySound(PlaySoundEvent event) {
      if (!nullCheck()) {
         if (this.fish.getValue()) {
            SoundInstance p = event.sound;
            FishingBobberEntity b = mc.player.fishHook;
            if (b != null) {
               if (p.getId().getPath().equals("entity.fishing_bobber.splash")
                  && (
                     !this.splashDetection.getValue()
                        || MathUtil.distance(b.getX(), b.getY(), b.getZ(), p.getX(), p.getY(), p.getZ())
                           <= this.splashDetectionRange.getValue()
                  )) {
                  this.ticksEnabled = true;
                  this.ticksToRightClick = this.ticksCatch.getValueInt();
                  this.ticksData = 0;
               }
            }
         }
      }
   }

   public void autoFish() {
      if (this.fish.getValue()) {
         if (this.autoCastCheckTimer <= 0) {
            this.autoCastCheckTimer = 30;
            if (this.autoCast.getValue()
               && !this.ticksEnabled
               && !this.autoCastEnabled
               && mc.player.fishHook == null
               && mc.player.getMainHandStack().getItem() == Items.FISHING_ROD) {
               this.autoCastTimer = 0;
               this.autoCastEnabled = true;
            }
         } else {
            this.autoCastCheckTimer--;
         }

         if (this.autoCastEnabled) {
            this.autoCastTimer++;
            if (this.autoCastTimer > this.ticksAutoCast.getValue()) {
               this.autoCastEnabled = false;
               mc.doItemUse();
            }
         }

         if (this.ticksEnabled && this.ticksToRightClick <= 0) {
            if (this.ticksData == 0) {
               mc.doItemUse();
               this.ticksToRightClick = this.ticksThrow.getValueInt();
               this.ticksData = 1;
            } else if (this.ticksData == 1) {
               mc.doItemUse();
               this.ticksEnabled = false;
            }
         }

         this.ticksToRightClick--;
      }
   }

   @EventListener
   private void onKey(KeyEvent event) {
      if (mc.options.useKey.isPressed()) {
         this.ticksEnabled = false;
      }
   }

   public static int getTool(BlockPos pos) {
      int index = -1;
      float CurrentFastest = 1.0F;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack != ItemStack.EMPTY) {
            float digSpeed = EnchantmentHelper.getLevel(
               (RegistryEntry)mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.EFFICIENCY.getRegistryRef()).getOptional(Enchantments.EFFICIENCY).get(), stack
            );
            float destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(pos));
            if (mc.world.getBlockState(pos).getBlock() instanceof AirBlock) {
               return -1;
            }

            if (digSpeed + destroySpeed > CurrentFastest) {
               CurrentFastest = digSpeed + destroySpeed;
               index = i;
            }
         }
      }

      return index;
   }
}
