package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.MathUtil;
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
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AntiPhase extends Module {
   private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 4, 0, 8));
   private final BooleanSetting ladder = this.add(new BooleanSetting("Ladder", true).setParent());
   private final BooleanSetting onlyHard = this.add(new BooleanSetting("OnlyHard", true, this.ladder::isOpen));
   private final BooleanSetting itemFrame = this.add(new BooleanSetting("ItemFrame", true).setParent());
   private final BooleanSetting fill = this.add(new BooleanSetting("Fill", false, this.itemFrame::isOpen));
   private final BooleanSetting scaffolding = this.add(new BooleanSetting("Scaffolding", true));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BooleanSetting eatingPause = this.add(new BooleanSetting("EatingPause", true));
   private final BooleanSetting collideSkip = this.add(new BooleanSetting("CollideSkip", true));
   private final BooleanSetting crawlingSkip = this.add(new BooleanSetting("CrawlingSkip", true));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("InAirSkip", false));
   private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 5.0, 0.0, 7.0, 0.1));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 100.0, 0.0, 2000.0, 1.0));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final Timer timer = new Timer();

   public AntiPhase() {
      super("AntiPhase", Module.Category.Combat);
      this.setChinese("反穿墙");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!this.eatingPause.getValue() || !mc.player.isUsingItem()) {
         if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
            for (Entity target : CombatUtil.getEnemies(this.targetRange.getValue())) {
               if ((!this.crawlingSkip.getValue() || !target.isCrawling())
                  && (!this.onlyGround.getValue() || target.isOnGround())
                  && (!this.collideSkip.getValue() || !BlockUtil.canCollide(target, target.getBoundingBox()))
                  && this.timer.passed(this.delay.getValueInt())) {
                  if (this.scaffolding.getValue() && BlockUtil.canReplace(target.getBlockPos())) {
                     int block = this.getScaffolding();
                     if (block != -1) {
                        BlockPos bp = target.getBlockPos();
                        Direction downSide = null;
                        Direction placeSide = BlockUtil.getPlaceSide(bp, 6.0);
                        if (placeSide != null
                           || (downSide = this.getSideIgnore(bp.down())) != null
                              && BlockUtil.getBlock(bp.down()) instanceof ScaffoldingBlock
                              && !mc.player.isSneaking()
                           || mc.player.isSneaking()
                              && (downSide = this.getSideOnly(bp.down())) != null
                              && BlockUtil.getBlock(bp.down()) instanceof ScaffoldingBlock) {
                           Vec3d targetPos = placeSide != null
                              ? bp.offset(placeSide)
                                 .toCenterPos()
                                 .add(
                                    placeSide.getOpposite().getVector().getX() * 0.5,
                                    placeSide.getOpposite().getVector().getY() * 0.5,
                                    placeSide.getOpposite().getVector().getZ() * 0.5
                                 )
                              : bp.down()
                                 .toCenterPos()
                                 .add(downSide.getVector().getX() * 0.5, downSide.getVector().getY() * 0.5, downSide.getVector().getZ() * 0.5);
                           double distance = mc.player.getEyePos().distanceTo(targetPos);
                           if (distance <= this.placeRange.getValue()) {
                              int old = mc.player.getInventory().selectedSlot;
                              this.doSwap(block);
                              if (BlockUtil.getBlock(bp.down()) instanceof ScaffoldingBlock && downSide != null) {
                                 BlockUtil.clickBlock(bp.down(), downSide, this.rotate.getValue());
                              } else {
                                 BlockUtil.placeBlock(bp, this.rotate.getValue());
                              }

                              this.timer.reset();
                              if (this.inventory.getValue()) {
                                 this.doSwap(block);
                                 EntityUtil.syncInventory();
                              } else {
                                 this.doSwap(old);
                              }
                           }
                        }
                     }
                  }

                  if (this.itemFrame.getValue() && mc.world.isAir(target.getBlockPos())) {
                     ItemFrameEntity itemFrameEntity = this.hasItemFrame(new Box(target.getBlockPos()));
                     if (itemFrameEntity == null) {
                        int block = this.getItemFrame();
                        if (block != -1) {
                           BlockPos bp = target.getBlockPos().down();
                           double distance = mc.player.getEyePos().distanceTo(bp.toBottomCenterPos().add(0.0, 1.0, 0.0));
                           if (distance <= this.placeRange.getValue()
                              && BlockUtil.isStrictDirection(bp, Direction.UP)
                              && !BlockUtil.canReplace(bp)
                              && BlockUtil.canClick(bp)) {
                              int oldx = mc.player.getInventory().selectedSlot;
                              this.doSwap(block);
                              BlockUtil.clickBlock(bp, Direction.UP, this.rotate.getValue());
                              this.timer.reset();
                              if (this.inventory.getValue()) {
                                 this.doSwap(block);
                                 EntityUtil.syncInventory();
                              } else {
                                 this.doSwap(oldx);
                              }
                           }
                        }
                     }

                     if (this.fill.getValue() && itemFrameEntity != null && itemFrameEntity.getHeldItemStack().isEmpty()) {
                        Vec3d hitVec = MathUtil.getClosestPointToBox(mc.player.getEyePos(), itemFrameEntity.getBoundingBox());
                        if (mc.player.getEyePos().distanceTo(hitVec) <= AntiCheat.INSTANCE.ieRange.getValue()) {
                           int block = this.getObsidian();
                           if (block != -1) {
                              int oldx = mc.player.getInventory().selectedSlot;
                              this.doSwap(block);
                              if (this.rotate.getValue()) {
                                 Alien.ROTATION.snapAt(hitVec);
                              }

                              mc.player
                                 .networkHandler
                                 .sendPacket(PlayerInteractEntityC2SPacket.interact(itemFrameEntity, mc.player.isSneaking(), Hand.MAIN_HAND));
                              this.timer.reset();
                              if (this.inventory.getValue()) {
                                 this.doSwap(block);
                                 EntityUtil.syncInventory();
                              } else {
                                 this.doSwap(oldx);
                              }

                              if (this.rotate.getValue()) {
                                 Alien.ROTATION.snapBack();
                              }
                           }
                        }
                     }
                  }

                  if (this.ladder.getValue()) {
                     int block = this.getLadder();
                     if (block != -1 && BlockUtil.canReplace(target.getBlockPos())) {
                        Direction facing = this.targetFacing(target.getPos());
                        if (facing != null) {
                           BlockPos bp = target.getBlockPos().offset(facing);
                           double distance = mc.player
                              .getEyePos()
                              .distanceTo(
                                 bp.toCenterPos()
                                    .add(
                                       facing.getOpposite().getVector().getX() * 0.5,
                                       facing.getOpposite().getVector().getY() * 0.5,
                                       facing.getOpposite().getVector().getZ() * 0.5
                                    )
                              );
                           if (distance <= this.placeRange.getValue()) {
                              BlockUtil.placedPos.add(target.getBlockPos());
                              int oldxx = mc.player.getInventory().selectedSlot;
                              this.doSwap(block);
                              BlockUtil.clickBlock(bp, facing.getOpposite(), this.rotate.getValue());
                              this.timer.reset();
                              if (this.inventory.getValue()) {
                                 this.doSwap(block);
                                 EntityUtil.syncInventory();
                              } else {
                                 this.doSwap(oldxx);
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

   private Direction getSideOnly(BlockPos pos) {
      return BlockUtil.isStrictDirection(pos, Direction.UP) ? Direction.UP : null;
   }

   private Direction getSideIgnore(BlockPos pos) {
      for (Direction i : Direction.values()) {
         if (i != Direction.UP && BlockUtil.isStrictDirection(pos, i)) {
            return i;
         }
      }

      return null;
   }

   private ItemFrameEntity hasItemFrame(Box box) {
      for (Entity entity : BlockUtil.getEntities(box)) {
         if (entity instanceof ItemFrameEntity itemFrameEntity && entity.getFacing() == Direction.UP) {
            return itemFrameEntity;
         }
      }

      return null;
   }

   private static Box getBox(Direction facing, BlockPos bp) {
      Box box = null;
      double wide = 0.1875;
      double x = facing.getOffsetX() * 0.5 + bp.getX() + 0.5;
      double y = bp.getY();
      double z = facing.getOffsetZ() * 0.5 + bp.getZ() + 0.5;
      switch (facing) {
         case WEST:
            box = new Box(x, y, z, x + wide, y + 1.0, z + 1.0);
            break;
         case EAST:
            box = new Box(x, y, z, x - wide, y + 1.0, z + 1.0);
            break;
         case NORTH:
            box = new Box(x, y, z, x + 1.0, y + 1.0, z + wide);
            break;
         case SOUTH:
            box = new Box(x, y, z, x + 1.0, y + 1.0, z - wide);
      }

      return box;
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   private int getFlintAndSteel() {
      return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(Items.FLINT_AND_STEEL) : InventoryUtil.findItem(Items.FLINT_AND_STEEL);
   }

   private int getObsidian() {
      return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(Items.OBSIDIAN) : InventoryUtil.findItem(Items.OBSIDIAN);
   }

   private int getItemFrame() {
      return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(Items.ITEM_FRAME) : InventoryUtil.findItem(Items.ITEM_FRAME);
   }

   private int getLadder() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.LADDER) : InventoryUtil.findBlock(Blocks.LADDER);
   }

   private int getScaffolding() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.SCAFFOLDING) : InventoryUtil.findBlock(Blocks.SCAFFOLDING);
   }

   private Direction targetFacing(Vec3d vec3d) {
      BlockPos blockPos = new BlockPosX(vec3d);
      Vec3d centerPos = blockPos.toBottomCenterPos();
      float factorValue = 0.4F;
      double minDistance = Double.MAX_VALUE;
      Direction facing = null;

      for (Direction direction : Direction.values()) {
         if (direction != Direction.UP && direction != Direction.DOWN) {
            BlockPos bp = blockPos.offset(direction);
            if (BlockUtil.isStrictDirection(bp, direction.getOpposite())
               && (this.onlyHard.getValue() ? Alien.HOLE.isHard(bp) : !BlockUtil.canReplace(bp) && BlockUtil.canClick(bp))) {
               Box box = getBox(direction, blockPos);
               if (box != null && !hasEntity(box)) {
                  Vec3d tempPos = centerPos.add(direction.getOffsetX() * factorValue, 0.0, direction.getOffsetZ() * factorValue);
                  double distance = tempPos.distanceTo(vec3d);
                  if (distance < minDistance) {
                     minDistance = distance;
                     facing = direction;
                  }
               }
            }
         }
      }

      return facing;
   }

   public static boolean hasEntity(Box box) {
      for (Entity entity : BlockUtil.getEntities(box)) {
         if (entity.isAlive()
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrbEntity)
            && !(entity instanceof ExperienceBottleEntity)
            && !(entity instanceof ArrowEntity)
            && !(entity instanceof ItemFrameEntity)) {
            return true;
         }
      }

      return false;
   }
}
