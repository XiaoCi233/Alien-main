package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.PredictUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class HoleFiller extends Module {
   public static HoleFiller INSTANCE;
   public final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500).setSuffix("ms"));
   public final BooleanSetting inAirPause = this.add(new BooleanSetting("InAirPause", true));
   private final Timer timer = new Timer();
   private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8));
   private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 5.0, 0.0, 8.0).setSuffix("m"));
   private final SliderSetting enemyRange = this.add(new SliderSetting("EnemyRange", 6.0, 0.0, 8.0).setSuffix("m"));
   private final SliderSetting holeRange = this.add(new SliderSetting("HoleRange", 2.0, 0.0, 8.0).setSuffix("m"));
   private final SliderSetting selfRange = this.add(new SliderSetting("SelfRange", 2.0, 0.0, 8.0).setSuffix("m"));
   private final SliderSetting predictTicks = this.add(new SliderSetting("Predict", 1, 1, 8).setSuffix("tick"));
   private final BooleanSetting detectMining = this.add(new BooleanSetting("DetectMining", false));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BooleanSetting packetPlace = this.add(new BooleanSetting("PacketPlace", true));
   private final BooleanSetting breakCrystal = this.add(new BooleanSetting("Break", true).setParent());
   private final BooleanSetting eatPause = this.add(new BooleanSetting("EatingPause", true, this.breakCrystal::isOpen));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting web = this.add(new BooleanSetting("Web", true));
   int progress = 0;

   public HoleFiller() {
      super("HoleFiller", Module.Category.Combat);
      this.setChinese("自动填坑");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.timer.passed((long)this.placeDelay.getValue())) {
         if (!this.inventory.getValue() || EntityUtil.inInventory()) {
            this.progress = 0;
            if (this.getBlock() != -1) {
               if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                  if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
                     if (!this.inAirPause.getValue() || mc.player.isOnGround()) {
                        CombatUtil.getEnemies(this.enemyRange.getValue())
                           .stream()
                           .flatMap(
                              enemy -> BlockUtil.getSphere(this.holeRange.getValueFloat(), PredictUtil.getPos(enemy, this.predictTicks.getValueInt())).stream()
                           )
                           .filter(
                              pos -> pos.toCenterPos().distanceTo(mc.player.getPos()) > this.selfRange.getValue()
                                 && (Alien.HOLE.isHole(pos, true, true, false) || Alien.HOLE.isDoubleHole(pos))
                           )
                           .distinct()
                           .forEach(this::tryPlaceBlock);
                     }
                  }
               }
            }
         }
      }
   }

   private void tryPlaceBlock(BlockPos pos) {
      if (pos != null) {
         if (!this.detectMining.getValue() || !Alien.BREAK.isMining(pos)) {
            if (this.progress < this.blocksPer.getValue()) {
               int block = this.getBlock();
               if (block != -1) {
                  if (BlockUtil.canPlace(pos, this.placeRange.getValue(), true)) {
                     if (this.breakCrystal.getValue()) {
                        CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.eatPause.getValue());
                     } else if (BlockUtil.hasEntity(pos, false)) {
                        return;
                     }

                     int old = mc.player.getInventory().selectedSlot;
                     this.doSwap(block);
                     BlockUtil.placeBlock(pos, this.rotate.getValue(), this.packetPlace.getValue());
                     if (this.inventory.getValue()) {
                        this.doSwap(block);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(old);
                     }

                     this.progress++;
                     this.timer.reset();
                  }
               }
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   private int getBlock() {
      if (this.inventory.getValue()) {
         return this.web.getValue() && InventoryUtil.findBlockInventorySlot(Blocks.COBWEB) != -1
            ? InventoryUtil.findBlockInventorySlot(Blocks.COBWEB)
            : InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
      } else {
         return this.web.getValue() && InventoryUtil.findBlock(Blocks.COBWEB) != -1
            ? InventoryUtil.findBlock(Blocks.COBWEB)
            : InventoryUtil.findBlock(Blocks.OBSIDIAN);
      }
   }
}
