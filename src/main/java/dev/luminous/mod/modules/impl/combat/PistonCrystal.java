package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class PistonCrystal extends Module {
   public static PistonCrystal INSTANCE;
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", false));
   private final BooleanSetting pistonPacket = this.add(new BooleanSetting("PistonPacket", false));
   private final BooleanSetting noEating = this.add(new BooleanSetting("NoEating", true));
   private final BooleanSetting eatingBreak = this.add(new BooleanSetting("EatingBreak", false));
   private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 5.0, 1.0, 8.0));
   private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 1.0, 8.0));
   private final BooleanSetting fire = this.add(new BooleanSetting("Fire", true));
   private final BooleanSetting switchPos = this.add(new BooleanSetting("Switch", false));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("SelfGround", true));
   private final BooleanSetting onlyStatic = this.add(new BooleanSetting("MovingPause", true));
   private final SliderSetting updateDelay = this.add(new SliderSetting("PlaceDelay", 100, 0, 500));
   private final SliderSetting posUpdateDelay = this.add(new SliderSetting("PosUpdateDelay", 500, 0, 1000));
   private final SliderSetting stageSetting = this.add(new SliderSetting("Stage", 4, 1, 10));
   private final SliderSetting pistonStage = this.add(new SliderSetting("PistonStage", 1, 1, 10));
   private final SliderSetting pistonMaxStage = this.add(new SliderSetting("PistonMaxStage", 1, 1, 10));
   private final SliderSetting powerStage = this.add(new SliderSetting("PowerStage", 3, 1, 10));
   private final SliderSetting powerMaxStage = this.add(new SliderSetting("PowerMaxStage", 3, 1, 10));
   private final SliderSetting crystalStage = this.add(new SliderSetting("CrystalStage", 4, 1, 10));
   private final SliderSetting crystalMaxStage = this.add(new SliderSetting("CrystalMaxStage", 4, 1, 10));
   private final SliderSetting fireStage = this.add(new SliderSetting("FireStage", 2, 1, 10));
   private final SliderSetting fireMaxStage = this.add(new SliderSetting("FireMaxStage", 2, 1, 10));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting debug = this.add(new BooleanSetting("Debug", false));
   private final Timer timer = new Timer();
   private final Timer crystalTimer = new Timer();
   public BlockPos bestPos = null;
   public BlockPos bestOPos = null;
   public Direction bestFacing = null;
   public double distance = 100.0;
   public boolean getPos = false;
   public int stage = 1;
   private PlayerEntity target = null;
   private boolean isPiston = false;

   public PistonCrystal() {
      super("PistonCrystal", Module.Category.Combat);
      this.setChinese("活塞水晶");
      INSTANCE = this;
   }

   private static boolean canFire(BlockPos pos) {
      if (BlockUtil.canReplace(pos.down())) {
         return false;
      } else if (!mc.world.isAir(pos)) {
         return false;
      } else {
         return !BlockUtil.canClick(pos.offset(Direction.DOWN)) ? false : BlockUtil.isStrictDirection(pos.down(), Direction.UP);
      }
   }

   public void onTick() {
      if (this.pistonStage.getValue() > this.stageSetting.getValue()) {
         this.pistonStage.setValue(this.stageSetting.getValue());
      }

      if (this.fireStage.getValue() > this.stageSetting.getValue()) {
         this.fireStage.setValue(this.stageSetting.getValue());
      }

      if (this.powerStage.getValue() > this.stageSetting.getValue()) {
         this.powerStage.setValue(this.stageSetting.getValue());
      }

      if (this.crystalStage.getValue() > this.stageSetting.getValue()) {
         this.crystalStage.setValue(this.stageSetting.getValue());
      }

      if (this.pistonMaxStage.getValue() > this.stageSetting.getValue()) {
         this.pistonMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.fireMaxStage.getValue() > this.stageSetting.getValue()) {
         this.fireMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.powerMaxStage.getValue() > this.stageSetting.getValue()) {
         this.powerMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.crystalMaxStage.getValue() > this.stageSetting.getValue()) {
         this.crystalMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.crystalMaxStage.getValue() < this.crystalStage.getValue()) {
         this.crystalStage.setValue(this.crystalMaxStage.getValue());
      }

      if (this.powerMaxStage.getValue() < this.powerStage.getValue()) {
         this.powerStage.setValue(this.powerMaxStage.getValue());
      }

      if (this.pistonMaxStage.getValue() < this.pistonStage.getValue()) {
         this.pistonStage.setValue(this.pistonMaxStage.getValue());
      }

      if (this.fireMaxStage.getValue() < this.fireStage.getValue()) {
         this.fireStage.setValue(this.fireMaxStage.getValue());
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.onTick();
      this.target = CombatUtil.getClosestEnemy(this.range.getValue());
      if (this.target != null) {
         if (!this.noEating.getValue() || !mc.player.isUsingItem()) {
            if (!this.check(this.onlyStatic.getValue(), !mc.player.isOnGround(), this.onlyGround.getValue())) {
               BlockPos pos = EntityUtil.getEntityPos(this.target, true);
               if (!mc.player.isUsingItem() || this.eatingBreak.getValue()) {
                  if (this.checkCrystal(pos.up(0))) {
                     CombatUtil.attackCrystal(pos.up(0), this.rotate.getValue(), true);
                  }

                  if (this.checkCrystal(pos.up(1))) {
                     CombatUtil.attackCrystal(pos.up(1), this.rotate.getValue(), true);
                  }

                  if (this.checkCrystal(pos.up(2))) {
                     CombatUtil.attackCrystal(pos.up(2), this.rotate.getValue(), true);
                  }
               }

               if (this.bestPos != null && mc.world.getBlockState(this.bestPos).getBlock() instanceof PistonBlock) {
                  this.isPiston = true;
               } else if (this.isPiston) {
                  this.isPiston = false;
                  this.crystalTimer.reset();
                  this.bestPos = null;
               }

               if (this.crystalTimer.passedMs(this.posUpdateDelay.getValueInt())) {
                  this.stage = 0;
                  this.distance = 100.0;
                  this.getPos = false;
                  this.getBestPos(pos.up(2));
                  this.getBestPos(pos.up());
               }

               if (this.timer.passedMs(this.updateDelay.getValueInt())) {
                  if (this.getPos && this.bestPos != null) {
                     this.timer.reset();
                     if (this.debug.getValue()) {
                        this.sendMessage(
                           "[Debug] PistonPos:" + this.bestPos + " Facing:" + this.bestFacing + " CrystalPos:" + this.bestOPos.offset(this.bestFacing)
                        );
                     }

                     this.doPistonAura(this.bestPos, this.bestFacing, this.bestOPos);
                  }
               }
            }
         }
      }
   }

   public boolean check(boolean onlyStatic, boolean onGround, boolean onlyGround) {
      if (MovementUtil.isMoving() && onlyStatic) {
         return true;
      } else if (onGround && onlyGround) {
         return true;
      } else if (this.findBlock(Blocks.REDSTONE_BLOCK) == -1) {
         return true;
      } else {
         return this.findClass(PistonBlock.class) == -1 ? true : this.findItem(Items.END_CRYSTAL) == -1;
      }
   }

   private boolean checkCrystal(BlockPos pos) {
      for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
         if (entity instanceof EndCrystalEntity) {
            float damage = AutoCrystal.INSTANCE.calculateDamage(entity.getPos(), this.target, this.target);
            if (damage > 7.0F) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean checkCrystal2(BlockPos pos) {
      for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
         if (entity instanceof EndCrystalEntity && EntityUtil.getEntityPos(entity).equals(pos)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public String getInfo() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   private void getBestPos(BlockPos pos) {
      for (Direction i : Direction.values()) {
         if (i != Direction.DOWN && i != Direction.UP) {
            this.getPos(pos, i);
         }
      }
   }

   private void getPos(BlockPos pos, Direction i) {
      if (BlockUtil.canPlaceCrystal(pos.offset(i)) || this.checkCrystal2(pos.offset(i))) {
         this.getPos(pos.offset(i, 3), i, pos);
         this.getPos(pos.offset(i, 3).up(), i, pos);
         int offsetX = pos.offset(i).getX() - pos.getX();
         int offsetZ = pos.offset(i).getZ() - pos.getZ();
         this.getPos(pos.offset(i, 3).add(offsetZ, 0, offsetX), i, pos);
         this.getPos(pos.offset(i, 3).add(-offsetZ, 0, -offsetX), i, pos);
         this.getPos(pos.offset(i, 3).add(offsetZ, 1, offsetX), i, pos);
         this.getPos(pos.offset(i, 3).add(-offsetZ, 1, -offsetX), i, pos);
         this.getPos(pos.offset(i, 2), i, pos);
         this.getPos(pos.offset(i, 2).up(), i, pos);
         this.getPos(pos.offset(i, 2).add(offsetZ, 0, offsetX), i, pos);
         this.getPos(pos.offset(i, 2).add(-offsetZ, 0, -offsetX), i, pos);
         this.getPos(pos.offset(i, 2).add(offsetZ, 1, offsetX), i, pos);
         this.getPos(pos.offset(i, 2).add(-offsetZ, 1, -offsetX), i, pos);
      }
   }

   private void getPos(BlockPos pos, Direction facing, BlockPos oPos) {
      if (!this.switchPos.getValue() || this.bestPos == null || !this.bestPos.equals(pos) || !mc.world.isAir(this.bestPos)) {
         if (BlockUtil.canPlace(pos, this.placeRange.getValue()) || this.getBlock(pos) instanceof PistonBlock) {
            if (this.findClass(PistonBlock.class) != -1) {
               if (!ClientSetting.INSTANCE.lowVersion.getValue()
                  || this.getBlock(pos) instanceof PistonBlock
                  || !(mc.player.getY() - pos.getY() <= -2.0) && !(mc.player.getY() - pos.getY() >= 3.0)
                  || !(BlockUtil.distanceToXZ(pos.getX() + 0.5, pos.getZ() + 0.5) < 2.6)) {
                  if (mc.world.isAir(pos.offset(facing, -1))
                     && mc.world.getBlockState(pos.offset(facing, -1)).getBlock() != Blocks.FIRE
                     && (this.getBlock(pos.offset(facing.getOpposite())) != Blocks.MOVING_PISTON || this.checkCrystal2(pos.offset(facing.getOpposite())))) {
                     if (BlockUtil.canPlace(pos, this.placeRange.getValue()) || this.isPiston(pos, facing)) {
                        if (MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) < this.distance || this.bestPos == null) {
                           this.bestPos = pos;
                           this.bestOPos = oPos;
                           this.bestFacing = facing;
                           this.distance = MathHelper.sqrt((float)mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()));
                           this.getPos = true;
                           this.crystalTimer.reset();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void doPistonAura(BlockPos pos, Direction facing, BlockPos oPos) {
      if (this.stage >= this.stageSetting.getValue()) {
         this.stage = 0;
      }

      this.stage++;
      if (mc.world.isAir(pos)) {
         if (!BlockUtil.canPlace(pos)) {
            return;
         }

         if (this.stage >= this.pistonStage.getValue() && this.stage <= this.pistonMaxStage.getValue()) {
            Direction side = BlockUtil.getPlaceSide(pos);
            if (side == null) {
               return;
            }

            int old = mc.player.getInventory().selectedSlot;
            AutoPush.pistonFacing(facing);
            int piston = this.findClass(PistonBlock.class);
            this.doSwap(piston);
            BlockUtil.placeBlock(pos, false, this.pistonPacket.getValue());
            if (this.inventory.getValue()) {
               this.doSwap(piston);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(old);
            }

            BlockPos neighbour = pos.offset(side);
            Direction opposite = side.getOpposite();
            if (this.rotate.getValue()) {
               Alien.ROTATION.lookAt(neighbour, opposite);
            }
         }
      }

      if (this.stage >= this.powerStage.getValue() && this.stage <= this.powerMaxStage.getValue()) {
         this.doRedStone(pos, facing, oPos.offset(facing));
      }

      if (this.stage >= this.crystalStage.getValue() && this.stage <= this.crystalMaxStage.getValue()) {
         this.placeCrystal(oPos, facing);
      }

      if (this.stage >= this.fireStage.getValue() && this.stage <= this.fireMaxStage.getValue()) {
         this.doFire(oPos, facing);
      }
   }

   private void placeCrystal(BlockPos pos, Direction facing) {
      if (BlockUtil.canPlaceCrystal(pos.offset(facing))) {
         int crystal = this.findItem(Items.END_CRYSTAL);
         if (crystal != -1) {
            int old = mc.player.getInventory().selectedSlot;
            this.doSwap(crystal);
            BlockUtil.placeCrystal(pos.offset(facing), true);
            if (this.inventory.getValue()) {
               this.doSwap(crystal);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(old);
            }
         }
      }
   }

   private boolean isPiston(BlockPos pos, Direction facing) {
      if (!(mc.world.getBlockState(pos).getBlock() instanceof PistonBlock)) {
         return false;
      } else {
         return ((Direction)mc.world.getBlockState(pos).get(FacingBlock.FACING)).getOpposite() != facing
            ? false
            : mc.world.isAir(pos.offset(facing, -1))
               || this.getBlock(pos.offset(facing, -1)) == Blocks.FIRE
               || this.getBlock(pos.offset(facing.getOpposite())) == Blocks.MOVING_PISTON;
      }
   }

   private void doFire(BlockPos pos, Direction facing) {
      if (this.fire.getValue()) {
         int fire = this.findItem(Items.FLINT_AND_STEEL);
         if (fire != -1) {
            int old = mc.player.getInventory().selectedSlot;
            int[] xOffset = new int[]{0, facing.getOffsetZ(), -facing.getOffsetZ()};
            int[] yOffset = new int[]{0, 1};
            int[] zOffset = new int[]{0, facing.getOffsetX(), -facing.getOffsetX()};

            for (int x : xOffset) {
               for (int y : yOffset) {
                  for (int z : zOffset) {
                     if (this.getBlock(pos.add(x, y, z)) == Blocks.FIRE) {
                        return;
                     }
                  }
               }
            }

            for (int x : xOffset) {
               for (int y : yOffset) {
                  for (int zx : zOffset) {
                     if (canFire(pos.add(x, y, zx))) {
                        this.doSwap(fire);
                        this.placeFire(pos.add(x, y, zx));
                        if (this.inventory.getValue()) {
                           this.doSwap(fire);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(old);
                        }

                        return;
                     }
                  }
               }
            }
         }
      }
   }

   public void placeFire(BlockPos pos) {
      BlockPos neighbour = pos.offset(Direction.DOWN);
      BlockUtil.clickBlock(neighbour, Direction.UP, this.rotate.getValue());
   }

   private void doRedStone(BlockPos pos, Direction facing, BlockPos crystalPos) {
      if (mc.world.isAir(pos.offset(facing, -1))
         || this.getBlock(pos.offset(facing, -1)) == Blocks.FIRE
         || this.getBlock(pos.offset(facing.getOpposite())) == Blocks.MOVING_PISTON) {
         for (Direction i : Direction.values()) {
            if (this.getBlock(pos.offset(i)) == Blocks.REDSTONE_BLOCK) {
               return;
            }
         }

         int power = this.findBlock(Blocks.REDSTONE_BLOCK);
         if (power != -1) {
            int old = mc.player.getInventory().selectedSlot;
            Direction bestNeighboring = BlockUtil.getBestNeighboring(pos, facing);
            if (bestNeighboring != null
               && bestNeighboring != facing.getOpposite()
               && BlockUtil.canPlace(pos.offset(bestNeighboring), this.placeRange.getValue())
               && !pos.offset(bestNeighboring).equals(crystalPos)) {
               this.doSwap(power);
               BlockUtil.placeBlock(pos.offset(bestNeighboring), this.rotate.getValue());
               if (this.inventory.getValue()) {
                  this.doSwap(power);
                  EntityUtil.syncInventory();
               } else {
                  this.doSwap(old);
               }
            } else {
               for (Direction ix : Direction.values()) {
                  if (BlockUtil.canPlace(pos.offset(ix), this.placeRange.getValue()) && !pos.offset(ix).equals(crystalPos) && ix != facing.getOpposite()) {
                     this.doSwap(power);
                     BlockUtil.placeBlock(pos.offset(ix), this.rotate.getValue());
                     if (this.inventory.getValue()) {
                        this.doSwap(power);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(old);
                     }

                     return;
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

   public int findItem(Item itemIn) {
      return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(itemIn) : InventoryUtil.findItem(itemIn);
   }

   public int findBlock(Block blockIn) {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(blockIn) : InventoryUtil.findBlock(blockIn);
   }

   public int findClass(Class<?> clazz) {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(clazz) : InventoryUtil.findClass(clazz);
   }

   private Block getBlock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock();
   }
}
