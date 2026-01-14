package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.PredictUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConcretePowderBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoTrap extends Module {
   public static AutoTrap INSTANCE;
   public final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 500).setSuffix("ms"));
   private final EnumSetting<AutoTrap.TargetMode> targetMod = this.add(new EnumSetting("TargetMode", AutoTrap.TargetMode.Single));
   private final EnumSetting<AutoTrap.Mode> headMode = this.add(new EnumSetting("BlockForHead", AutoTrap.Mode.Anchor));
   final ArrayList<BlockPos> trapList = new ArrayList();
   final ArrayList<BlockPos> placeList = new ArrayList();
   private final Timer timer = new Timer();
   private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 4.0, 1.0, 6.0).setSuffix("m"));
   private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8));
   public final SliderSetting predictTicks = this.add(new SliderSetting("PredictTicks", 2.0, 0.0, 50.0, 1.0));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 1.0, 8.0).setSuffix("m"));
   private final BooleanSetting checkMine = this.add(new BooleanSetting("DetectMining", false));
   private final BooleanSetting helper = this.add(new BooleanSetting("Helper", true));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting onlyCrawling = this.add(new BooleanSetting("OnlyCrawling", false));
   private final BooleanSetting checkElytra = this.add(new BooleanSetting("CheckElytra", false));
   private final BooleanSetting extend = this.add(new BooleanSetting("Extend", true));
   private final BooleanSetting antiStep = this.add(new BooleanSetting("AntiStep", false));
   private final BooleanSetting onlyBreak = this.add(new BooleanSetting("OnlyBreak", false, this.antiStep::getValue));
   private final BooleanSetting head = this.add(new BooleanSetting("Head", true));
   private final BooleanSetting headExtend = this.add(new BooleanSetting("HeadExtend", true));
   private final BooleanSetting chestUp = this.add(new BooleanSetting("ChestUp", true));
   private final BooleanSetting onlyBreaking = this.add(new BooleanSetting("OnlyBreaking", false, this.chestUp::getValue));
   private final BooleanSetting chest = this.add(new BooleanSetting("Chest", true));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false, this.chest::getValue));
   private final BooleanSetting ignoreCrawling = this.add(new BooleanSetting("IgnoreCrawling", false, this.chest::getValue));
   private final BooleanSetting legs = this.add(new BooleanSetting("Legs", false));
   private final BooleanSetting legAnchor = this.add(new BooleanSetting("LegAnchor", true));
   private final BooleanSetting down = this.add(new BooleanSetting("Down", false));
   private final BooleanSetting onlyHole = this.add(new BooleanSetting("OnlyHole", false));
   private final BooleanSetting breakCrystal = this.add(new BooleanSetting("Break", true));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting selfGround = this.add(new BooleanSetting("SelfGround", true));
   public PlayerEntity target;
   int progress = 0;

   public AutoTrap() {
      super("AutoTrap", Module.Category.Combat);
      this.setChinese("自动困住");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.trapList.clear();
      this.placeList.clear();
      this.progress = 0;
      this.target = null;
      if (!this.selfGround.getValue() || mc.player.isOnGround()) {
         if (!this.inventory.getValue() || EntityUtil.inInventory()) {
            if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
               if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
                  if (this.timer.passed((long)this.delay.getValue())) {
                     if (this.targetMod.getValue() == AutoTrap.TargetMode.Single) {
                        this.target = CombatUtil.getClosestEnemy(this.range.getValue());
                        if (this.target == null) {
                           if (this.autoDisable.getValue()) {
                              this.disable();
                           }

                           return;
                        }

                        this.trapTarget(this.target);
                     } else if (this.targetMod.getValue() == AutoTrap.TargetMode.Multi) {
                        boolean found = false;

                        for (PlayerEntity player : CombatUtil.getEnemies(this.range.getValue())) {
                           found = true;
                           this.target = player;
                           this.trapTarget(this.target);
                        }

                        if (!found) {
                           if (this.autoDisable.getValue()) {
                              this.disable();
                           }

                           this.target = null;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void trapTarget(PlayerEntity target) {
      if (!this.onlyHole.getValue() || Alien.HOLE.isHole(EntityUtil.getEntityPos(target))) {
         if (!this.onlyCrawling.getValue()
            || target.isCrawling()
            || this.checkElytra.getValue()
               && ((ItemStack)target.getInventory().armor.get(2)).getItem() instanceof ElytraItem
               && (!(mc.player.getY() < target.getY() + 1.0) || target.isFallFlying())) {
            Vec3d playerPos = this.predictTicks.getValue() > 0.0 ? PredictUtil.getPos(target, this.predictTicks.getValueInt()) : target.getPos();
            this.doTrap(target, new BlockPosX(playerPos.getX(), playerPos.getY(), playerPos.getZ()));
         }
      }
   }

   private void doTrap(PlayerEntity player, BlockPos pos) {
      if (pos != null) {
         if (!this.trapList.contains(pos)) {
            this.trapList.add(pos);
            int headOffset = player.isCrawling() ? 1 : 2;
            int chestOffset = player.isCrawling() ? 0 : 1;
            if (this.legs.getValue()) {
               for (Direction i : Direction.values()) {
                  if (i != Direction.DOWN && i != Direction.UP) {
                     BlockPos offsetPos = pos.offset(i);
                     this.tryPlaceBlock(offsetPos, this.legAnchor.getValue(), false, false);
                     if (BlockUtil.getPlaceSide(offsetPos) == null
                        && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                        && this.getHelper(offsetPos) != null) {
                        this.tryPlaceObsidian(this.getHelper(offsetPos));
                     }
                  }
               }
            }

            if (this.headExtend.getValue()) {
               for (int x : new int[]{1, 0, -1}) {
                  for (int z : new int[]{1, 0, -1}) {
                     BlockPos offsetPos = pos.add(z, 0, x);
                     if (this.checkEntity(new BlockPos(offsetPos))) {
                        this.tryPlaceBlock(
                           offsetPos.up(headOffset),
                           this.headMode.getValue() == AutoTrap.Mode.Anchor,
                           this.headMode.getValue() == AutoTrap.Mode.Concrete,
                           this.headMode.getValue() == AutoTrap.Mode.Web
                        );
                     }
                  }
               }
            }

            if (this.head.getValue() && BlockUtil.clientCanPlace(pos.up(headOffset), this.breakCrystal.getValue())) {
               if (BlockUtil.getPlaceSide(pos.up(headOffset)) == null) {
                  boolean trapChest = this.helper.getValue();
                  if (this.getHelper(pos.up(headOffset)) != null) {
                     this.tryPlaceObsidian(this.getHelper(pos.up(headOffset)));
                     trapChest = false;
                  }

                  if (trapChest) {
                     for (Direction ix : Direction.values()) {
                        if (ix != Direction.DOWN && ix != Direction.UP) {
                           BlockPos offsetPos = pos.offset(ix).up(chestOffset);
                           if (BlockUtil.isStrictDirection(pos.offset(ix).up(), ix.getOpposite())
                              && BlockUtil.clientCanPlace(offsetPos.up(chestOffset), this.breakCrystal.getValue())
                              && BlockUtil.canPlace(offsetPos, this.placeRange.getValue(), this.breakCrystal.getValue())) {
                              this.tryPlaceObsidian(offsetPos);
                              trapChest = false;
                              break;
                           }
                        }
                     }

                     if (trapChest) {
                        for (Direction ixx : Direction.values()) {
                           if (ixx != Direction.DOWN && ixx != Direction.UP) {
                              BlockPos offsetPos = pos.offset(ixx).up(chestOffset);
                              if (BlockUtil.isStrictDirection(pos.offset(ixx).up(), ixx.getOpposite())
                                 && BlockUtil.clientCanPlace(offsetPos.up(chestOffset), this.breakCrystal.getValue())
                                 && BlockUtil.getPlaceSide(offsetPos) == null
                                 && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                                 && this.getHelper(offsetPos) != null) {
                                 this.tryPlaceObsidian(this.getHelper(offsetPos));
                                 trapChest = false;
                                 break;
                              }
                           }
                        }

                        if (trapChest) {
                           for (Direction ixxx : Direction.values()) {
                              if (ixxx != Direction.DOWN && ixxx != Direction.UP) {
                                 BlockPos offsetPos = pos.offset(ixxx).up(chestOffset);
                                 if (BlockUtil.isStrictDirection(pos.offset(ixxx).up(), ixxx.getOpposite())
                                    && BlockUtil.clientCanPlace(offsetPos.up(chestOffset), this.breakCrystal.getValue())
                                    && BlockUtil.getPlaceSide(offsetPos) == null
                                    && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                                    && this.getHelper(offsetPos) != null
                                    && BlockUtil.getPlaceSide(offsetPos.down()) == null
                                    && BlockUtil.clientCanPlace(offsetPos.down(), this.breakCrystal.getValue())
                                    && this.getHelper(offsetPos.down()) != null) {
                                    this.tryPlaceObsidian(this.getHelper(offsetPos.down()));
                                    break;
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               this.tryPlaceBlock(
                  pos.up(headOffset),
                  this.headMode.getValue() == AutoTrap.Mode.Anchor,
                  this.headMode.getValue() == AutoTrap.Mode.Concrete,
                  this.headMode.getValue() == AutoTrap.Mode.Web
               );
            }

            if (this.antiStep.getValue() && (Alien.BREAK.isMining(pos.up(headOffset)) || !this.onlyBreak.getValue())) {
               if (BlockUtil.getPlaceSide(pos.up(3)) == null
                  && BlockUtil.clientCanPlace(pos.up(3), this.breakCrystal.getValue())
                  && this.getHelper(pos.up(3), Direction.DOWN) != null) {
                  this.tryPlaceObsidian(this.getHelper(pos.up(3)));
               }

               this.tryPlaceObsidian(pos.up(3));
            }

            if (this.down.getValue()) {
               BlockPos offsetPos = pos.down();
               this.tryPlaceObsidian(offsetPos);
               if (BlockUtil.getPlaceSide(offsetPos) == null
                  && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                  && this.getHelper(offsetPos) != null) {
                  this.tryPlaceObsidian(this.getHelper(offsetPos));
               }
            }

            if (this.chestUp.getValue()) {
               for (Direction ixxxx : Direction.values()) {
                  if (ixxxx != Direction.DOWN && ixxxx != Direction.UP) {
                     BlockPos offsetPos = pos.offset(ixxxx).up(headOffset);
                     if (!this.onlyBreaking.getValue() || Alien.BREAK.isMining(pos.up(headOffset))) {
                        this.tryPlaceObsidian(offsetPos);
                        if (BlockUtil.getPlaceSide(offsetPos) == null && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())) {
                           if (this.getHelper(offsetPos) != null) {
                              this.tryPlaceObsidian(this.getHelper(offsetPos));
                           } else if (BlockUtil.getPlaceSide(offsetPos.down()) == null
                              && BlockUtil.clientCanPlace(offsetPos.down(), this.breakCrystal.getValue())
                              && this.getHelper(offsetPos.down()) != null) {
                              this.tryPlaceObsidian(this.getHelper(offsetPos.down()));
                           }
                        }
                     }
                  }
               }
            }

            if (this.chest.getValue()
               && (!this.onlyGround.getValue() || this.target.isOnGround())
               && (!this.ignoreCrawling.getValue() || !this.target.isCrawling())) {
               for (Direction ixxxxx : Direction.values()) {
                  if (ixxxxx != Direction.DOWN && ixxxxx != Direction.UP) {
                     BlockPos offsetPos = pos.offset(ixxxxx).up(chestOffset);
                     this.tryPlaceObsidian(offsetPos);
                     if (BlockUtil.getPlaceSide(offsetPos) == null && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())) {
                        if (this.getHelper(offsetPos) != null) {
                           this.tryPlaceObsidian(this.getHelper(offsetPos));
                        } else if (BlockUtil.getPlaceSide(offsetPos.down()) == null
                           && BlockUtil.clientCanPlace(offsetPos.down(), this.breakCrystal.getValue())
                           && this.getHelper(offsetPos.down()) != null) {
                           this.tryPlaceObsidian(this.getHelper(offsetPos.down()));
                        }
                     }
                  }
               }
            }

            if (this.extend.getValue()) {
               for (int x : new int[]{1, 0, -1}) {
                  for (int zx : new int[]{1, 0, -1}) {
                     BlockPos offsetPos = pos.add(x, 0, zx);
                     if (this.checkEntity(new BlockPos(offsetPos))) {
                        this.doTrap(player, offsetPos);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public String getInfo() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   public BlockPos getHelper(BlockPos pos) {
      if (!this.helper.getValue()) {
         return null;
      } else {
         for (Direction i : Direction.values()) {
            if ((!this.checkMine.getValue() || !Alien.BREAK.isMining(pos.offset(i)))
               && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite())
               && BlockUtil.canPlace(pos.offset(i), this.placeRange.getValue(), this.breakCrystal.getValue())) {
               return pos.offset(i);
            }
         }

         return null;
      }
   }

   public BlockPos getHelper(BlockPos pos, Direction ignore) {
      if (!this.helper.getValue()) {
         return null;
      } else {
         for (Direction i : Direction.values()) {
            if (i != ignore
               && (!this.checkMine.getValue() || !Alien.BREAK.isMining(pos.offset(i)))
               && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite())
               && BlockUtil.canPlace(pos.offset(i), this.placeRange.getValue(), this.breakCrystal.getValue())) {
               return pos.offset(i);
            }
         }

         return null;
      }
   }

   private boolean checkEntity(BlockPos pos) {
      if (mc.player.getBoundingBox().intersects(new Box(pos))) {
         return false;
      } else {
         for (Entity entity : Alien.THREAD.getPlayers()) {
            if (entity.getBoundingBox().intersects(new Box(pos)) && entity.isAlive()) {
               return true;
            }
         }

         return false;
      }
   }

   private void tryPlaceBlock(BlockPos pos, boolean anchor, boolean sand, boolean web) {
      if (!this.placeList.contains(pos)) {
         if (!Alien.BREAK.isMining(pos)) {
            if (BlockUtil.canPlace(pos, 6.0, this.breakCrystal.getValue())) {
               if (this.progress < this.blocksPer.getValue()) {
                  if (!(MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > this.placeRange.getValue())) {
                     int old = mc.player.getInventory().selectedSlot;
                     int block = sand
                        ? this.getConcrete()
                        : (
                           web
                              ? (this.getWeb() != -1 ? this.getWeb() : this.getBlock())
                              : (anchor && this.getAnchor() != -1 ? this.getAnchor() : this.getBlock())
                        );
                     if (block != -1) {
                        this.placeList.add(pos);
                        CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.usingPause.getValue());
                        this.doSwap(block);
                        BlockUtil.placeBlock(pos, this.rotate.getValue());
                        if (this.inventory.getValue()) {
                           this.doSwap(block);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(old);
                        }

                        this.timer.reset();
                        this.progress++;
                     }
                  }
               }
            }
         }
      }
   }

   private void tryPlaceObsidian(BlockPos pos) {
      if (pos != null) {
         if (!this.placeList.contains(pos)) {
            if (!Alien.BREAK.isMining(pos)) {
               if (BlockUtil.canPlace(pos, 6.0, this.breakCrystal.getValue())) {
                  if (this.progress < this.blocksPer.getValue()) {
                     if (!(MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > this.placeRange.getValue())) {
                        int old = mc.player.getInventory().selectedSlot;
                        int block = this.getBlock();
                        if (block != -1) {
                           BlockUtil.placedPos.add(pos);
                           this.placeList.add(pos);
                           CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.usingPause.getValue());
                           this.doSwap(block);
                           BlockUtil.placeBlock(pos, this.rotate.getValue());
                           if (this.inventory.getValue()) {
                              this.doSwap(block);
                              EntityUtil.syncInventory();
                           } else {
                              this.doSwap(old);
                           }

                           this.timer.reset();
                           this.progress++;
                        }
                     }
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
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
   }

   private int getConcrete() {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(ConcretePowderBlock.class) : InventoryUtil.findClass(ConcretePowderBlock.class);
   }

   private int getWeb() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.COBWEB) : InventoryUtil.findBlock(Blocks.COBWEB);
   }

   private int getAnchor() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
   }

   private static enum Mode {
      Obsidian,
      Anchor,
      Web,
      Concrete;
   }

   public static enum TargetMode {
      Single,
      Multi;
   }
}
