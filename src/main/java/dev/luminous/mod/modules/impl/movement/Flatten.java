package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class Flatten extends Module {
   public static Flatten INSTANCE;
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BooleanSetting checkMine = this.add(new BooleanSetting("DetectMining", true));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting cover = this.add(new BooleanSetting("Cover", false));
   private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 2, 1, 8));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 1000));
   private final Timer timer = new Timer();
   int progress = 0;

   public Flatten() {
      super("Flatten", Module.Category.Movement);
      this.setChinese("填平脚下");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.progress = 0;
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
            if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
               if (mc.player.isOnGround()) {
                  if (this.timer.passed(this.delay.getValueInt())) {
                     int oldSlot = mc.player.getInventory().selectedSlot;
                     int block;
                     if ((block = this.getBlock()) != -1) {
                        if (EntityUtil.isInsideBlock()) {
                           BlockPos pos1 = new BlockPosX(mc.player.getX() + 0.5, mc.player.getY() + 0.5, mc.player.getZ() + 0.5)
                              .down();
                           BlockPos pos2 = new BlockPosX(mc.player.getX() - 0.5, mc.player.getY() + 0.5, mc.player.getZ() + 0.5)
                              .down();
                           BlockPos pos3 = new BlockPosX(mc.player.getX() + 0.5, mc.player.getY() + 0.5, mc.player.getZ() - 0.5)
                              .down();
                           BlockPos pos4 = new BlockPosX(mc.player.getX() - 0.5, mc.player.getY() + 0.5, mc.player.getZ() - 0.5)
                              .down();
                           if (this.canPlace(pos1) || this.canPlace(pos2) || this.canPlace(pos3) || this.canPlace(pos4)) {
                              CombatUtil.attackCrystal(pos1, this.rotate.getValue(), this.usingPause.getValue());
                              CombatUtil.attackCrystal(pos2, this.rotate.getValue(), this.usingPause.getValue());
                              CombatUtil.attackCrystal(pos3, this.rotate.getValue(), this.usingPause.getValue());
                              CombatUtil.attackCrystal(pos4, this.rotate.getValue(), this.usingPause.getValue());
                              this.doSwap(block);
                              this.tryPlaceObsidian(pos1, this.rotate.getValue());
                              this.tryPlaceObsidian(pos2, this.rotate.getValue());
                              this.tryPlaceObsidian(pos3, this.rotate.getValue());
                              this.tryPlaceObsidian(pos4, this.rotate.getValue());
                              if (this.inventory.getValue()) {
                                 this.doSwap(block);
                                 EntityUtil.syncInventory();
                              } else {
                                 this.doSwap(oldSlot);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void tryPlaceObsidian(BlockPos pos, boolean rotate) {
      if (this.canPlace(pos)) {
         if (!(this.progress < this.blocksPer.getValue())) {
            return;
         }

         if (BlockUtil.allowAirPlace()) {
            BlockUtil.placedPos.add(pos);
            BlockUtil.airPlace(pos, rotate);
            this.timer.reset();
            this.progress++;
            return;
         }

         Direction side = BlockUtil.getPlaceSide(pos);
         if (side == null) {
            return;
         }

         this.progress++;
         BlockUtil.placedPos.add(pos);
         BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), rotate);
         this.timer.reset();
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   private boolean canPlace(BlockPos pos) {
      if (this.checkMine.getValue() && Alien.BREAK.isMining(pos)) {
         return false;
      } else if (this.cover.getValue() && mc.world.isAir(pos.up())) {
         return false;
      } else if (BlockUtil.getPlaceSide(pos) == null) {
         return false;
      } else {
         return !BlockUtil.canReplace(pos) ? false : !this.hasEntity(pos);
      }
   }

   private boolean hasEntity(BlockPos pos) {
      for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
         if (entity != mc.player
            && entity.isAlive()
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrbEntity)
            && !(entity instanceof ExperienceBottleEntity)
            && !(entity instanceof ArrowEntity)
            && !(entity instanceof EndCrystalEntity)) {
            return true;
         }
      }

      return false;
   }

   private int getBlock() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
   }
}
