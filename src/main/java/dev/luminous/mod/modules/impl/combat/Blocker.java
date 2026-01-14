package dev.luminous.mod.modules.impl.combat;

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
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class Blocker extends Module {
   public static Blocker INSTANCE;
   private final EnumSetting<Blocker.Page> page = this.add(new EnumSetting("Page", Blocker.Page.General));
   final List<BlockPos> placePos = new ArrayList();
   final List<BlockPos> blockerPos = new ArrayList();
   final List<BlockPos> list = new ArrayList();
   private final Timer timer = new Timer();
   private final SliderSetting delay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500, () -> this.page.getValue() == Blocker.Page.General));
   private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8, () -> this.page.getValue() == Blocker.Page.General));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == Blocker.Page.General));
   private final BooleanSetting breakCrystal = this.add(new BooleanSetting("Break", true, () -> this.page.getValue() == Blocker.Page.General));
   private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.getValue() == Blocker.Page.General));
   private final BooleanSetting bevelCev = this.add(new BooleanSetting("BevelCev", true, () -> this.page.getValue() == Blocker.Page.Target));
   private final BooleanSetting burrow = this.add(new BooleanSetting("Burrow", true, () -> this.page.getValue() == Blocker.Page.Target));
   private final BooleanSetting face = this.add(new BooleanSetting("Face", true, () -> this.page.getValue() == Blocker.Page.Target).setParent());
   private final BooleanSetting faceUp = this.add(new BooleanSetting("FaceUp", false, () -> this.page.getValue() == Blocker.Page.Target && this.face.isOpen()));
   private final BooleanSetting feet = this.add(new BooleanSetting("Feet", true, () -> this.page.getValue() == Blocker.Page.Target).setParent());
   private final BooleanSetting extend = this.add(new BooleanSetting("Extend", false, () -> this.page.getValue() == Blocker.Page.Target && this.feet.isOpen()));
   private final BooleanSetting onlySurround = this.add(
      new BooleanSetting("OnlySurround", true, () -> this.page.getValue() == Blocker.Page.Target && this.feet.isOpen())
   );
   private final BooleanSetting inAirPause = this.add(new BooleanSetting("InAirPause", false, () -> this.page.getValue() == Blocker.Page.Check));
   private final BooleanSetting detectMining = this.add(new BooleanSetting("DetectMining", true, () -> this.page.getValue() == Blocker.Page.Check));
   private final BooleanSetting eatingPause = this.add(new BooleanSetting("EatingPause", true, () -> this.page.getValue() == Blocker.Page.Check));
   private int placeProgress = 0;
   private BlockPos playerBP;

   public Blocker() {
      super("Blocker", Module.Category.Combat);
      this.setChinese("水晶阻挡");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.list.clear();
      if (!this.inventorySwap.getValue() || EntityUtil.inInventory()) {
         if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
            if (this.timer.passedMs(this.delay.getValue())) {
               if (!this.eatingPause.getValue() || !mc.player.isUsingItem()) {
                  this.placeProgress = 0;
                  if (this.playerBP != null && !this.playerBP.equals(EntityUtil.getPlayerPos(true))) {
                     this.placePos.clear();
                     this.blockerPos.clear();
                  }

                  this.playerBP = EntityUtil.getPlayerPos(true);
                  double[] offset = new double[]{AntiCheat.getOffset(), -AntiCheat.getOffset(), 0.0};
                  if (this.bevelCev.getValue()) {
                     for (Direction i : Direction.values()) {
                        if (i != Direction.DOWN && !this.isBedrock(this.playerBP.offset(i).up())) {
                           BlockPos blockerPos = this.playerBP.offset(i).up(2);
                           if (this.crystalHere(blockerPos) && !this.placePos.contains(blockerPos)) {
                              this.placePos.add(blockerPos);
                           }
                        }
                     }
                  }

                  if (this.face.getValue() && (!this.onlySurround.getValue() || Surround.INSTANCE.isOn() || SelfTrap.INSTANCE.isOn())) {
                     for (double x : offset) {
                        for (double z : offset) {
                           for (Direction ix : Direction.values()) {
                              for (int d = 0; d < 3; d++) {
                                 BlockPos aroundPos = new BlockPosX(mc.player.getX() + x, mc.player.getY() + 0.5, mc.player.getZ() + z)
                                    .offset(ix, 1)
                                    .up();
                                 BlockPos blockerPos = new BlockPosX(mc.player.getX() + x, mc.player.getY() + 0.5, mc.player.getZ() + z)
                                    .offset(ix, d)
                                    .up();
                                 if (this.crystalHere(blockerPos) && !this.placePos.contains(blockerPos) && !Alien.HOLE.isHard(aroundPos)) {
                                    this.placePos.add(blockerPos);
                                 }
                              }
                           }
                        }
                     }

                     if (this.faceUp.getValue()) {
                        for (Direction ix : Direction.values()) {
                           if (ix != Direction.DOWN && !this.isBedrock(this.playerBP.offset(ix).up())) {
                              BlockPos blockerPos = this.playerBP.offset(ix).up(2);
                              if (this.crystalHere(blockerPos) && !this.placePos.contains(blockerPos)) {
                                 this.placePos.add(blockerPos);
                              }
                           }
                        }
                     }
                  }

                  if (this.getObsidian() != -1) {
                     this.placePos.removeIf(pos -> !BlockUtil.clientCanPlace(pos, true));
                     if (this.burrow.getValue()) {
                        for (double x : offset) {
                           for (double z : offset) {
                              BlockPos surroundPos = new BlockPosX(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z);
                              if (!this.isBedrock(surroundPos) && Alien.BREAK.isMining(surroundPos)) {
                                 for (Direction direction : Direction.values()) {
                                    if (direction != Direction.DOWN && direction != Direction.UP) {
                                       BlockPos defensePos = surroundPos.offset(direction);
                                       if (!this.detectMining.getValue() || !Alien.BREAK.isMining(defensePos)) {
                                          if (this.breakCrystal.getValue()) {
                                             CombatUtil.attackCrystal(defensePos, this.rotate.getValue(), false);
                                          }

                                          if (BlockUtil.canPlace(defensePos, 6.0, this.breakCrystal.getValue())) {
                                             this.blockerPos.add(defensePos);
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }

                     if (this.feet.getValue() && (!this.onlySurround.getValue() || Surround.INSTANCE.isOn() || SelfTrap.INSTANCE.isOn())) {
                        for (double x : offset) {
                           for (double zx : offset) {
                              for (Direction ixx : Direction.values()) {
                                 BlockPos surroundPos = new BlockPosX(
                                       mc.player.getX() + x, mc.player.getY() + 0.5, mc.player.getZ() + zx
                                    )
                                    .offset(ixx);
                                 if (!this.isBedrock(surroundPos) && Alien.BREAK.isMining(surroundPos)) {
                                    for (Direction directionx : Direction.values()) {
                                       BlockPos defensePos = surroundPos.offset(directionx);
                                       if (!this.detectMining.getValue() || !Alien.BREAK.isMining(defensePos)) {
                                          if (this.breakCrystal.getValue()) {
                                             CombatUtil.attackCrystal(defensePos, this.rotate.getValue(), false);
                                          }

                                          if (BlockUtil.canPlace(defensePos, 6.0, this.breakCrystal.getValue())) {
                                             this.blockerPos.add(defensePos);
                                          } else if (BlockUtil.canReplace(defensePos)
                                             && !BlockUtil.hasEntity(defensePos, true)
                                             && this.getHelper(defensePos) != null) {
                                             this.blockerPos.add(this.getHelper(defensePos));
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }

                     if (this.feet.getValue()
                        && this.extend.getValue()
                        && (!this.onlySurround.getValue() || Surround.INSTANCE.isOn() || SelfTrap.INSTANCE.isOn())) {
                        for (double x : offset) {
                           for (double zx : offset) {
                              for (Direction ixxx : Direction.values()) {
                                 if (ixxx != Direction.UP && ixxx != Direction.DOWN) {
                                    BlockPos surroundPos = new BlockPosX(
                                          mc.player.getX() + x, mc.player.getY() + 0.5, mc.player.getZ() + zx
                                       )
                                       .offset(ixxx);
                                    if (!this.isBedrock(surroundPos)) {
                                       for (Direction directionxx : Direction.values()) {
                                          if (directionxx != Direction.UP && directionxx != Direction.DOWN) {
                                             BlockPos blockPos = surroundPos.offset(directionxx);
                                             if (AutoCrystal.INSTANCE.canPlaceCrystal(blockPos, true, true)
                                                && (!this.detectMining.getValue() || !Alien.BREAK.isMining(blockPos))) {
                                                if (this.breakCrystal.getValue()) {
                                                   CombatUtil.attackCrystal(blockPos, this.rotate.getValue(), false);
                                                }

                                                if (BlockUtil.canPlace(blockPos, 6.0, this.breakCrystal.getValue())) {
                                                   this.blockerPos.add(blockPos);
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

                     this.blockerPos.removeIf(pos -> !BlockUtil.clientCanPlace(pos, true));
                     if (!this.inAirPause.getValue() || mc.player.isOnGround()) {
                        if (!this.blockerPos.isEmpty()) {
                           int oldSlot = mc.player.getInventory().selectedSlot;
                           int block = this.getObsidian();
                           if (block != -1) {
                              this.doSwap(block);

                              for (BlockPos defensePos : this.blockerPos) {
                                 if (BlockUtil.canPlace(defensePos, 6.0, this.breakCrystal.getValue())) {
                                    this.doPlace(defensePos);
                                 }
                              }

                              if (this.inventorySwap.getValue()) {
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

   public BlockPos getHelper(BlockPos pos) {
      for (Direction i : Direction.values()) {
         if (i != Direction.UP
            && (!this.detectMining.getValue() || !Alien.BREAK.isMining(pos.offset(i)))
            && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite())
            && BlockUtil.canPlace(pos.offset(i), 6.0)) {
            return pos.offset(i);
         }
      }

      return null;
   }

   private boolean crystalHere(BlockPos pos) {
      return BlockUtil.getEndCrystals(new Box(pos)).stream().anyMatch(entity -> entity.getBlockPos().equals(pos));
   }

   private boolean isBedrock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
   }

   private void doPlace(BlockPos pos) {
      if (!this.list.contains(pos)) {
         this.list.add(pos);
         if (this.placeProgress < this.blocksPer.getValue()) {
            BlockUtil.placeBlock(pos, this.rotate.getValue());
            this.timer.reset();
            this.placeProgress++;
         }
      }
   }

   private void tryPlaceObsidian(BlockPos pos) {
      if (!this.list.contains(pos)) {
         this.list.add(pos);
         if (this.placeProgress < this.blocksPer.getValue()) {
            if (!this.detectMining.getValue() || !Alien.BREAK.isMining(pos)) {
               int oldSlot = mc.player.getInventory().selectedSlot;
               int block;
               if ((block = this.getObsidian()) != -1) {
                  this.doSwap(block);
                  BlockUtil.placeBlock(pos, this.rotate.getValue());
                  this.timer.reset();
                  if (this.inventorySwap.getValue()) {
                     this.doSwap(block);
                     EntityUtil.syncInventory();
                  } else {
                     this.doSwap(oldSlot);
                  }

                  this.placeProgress++;
               }
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventorySwap.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   private int getObsidian() {
      return this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
   }

   public static enum Page {
      General,
      Target,
      Check;
   }
}
