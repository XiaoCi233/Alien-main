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
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoPush extends Module {
   public static AutoPush INSTANCE;
   private final BooleanSetting torch = this.add(new BooleanSetting("Torch", false));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotation", true));
   private final BooleanSetting yawDeceive = this.add(new BooleanSetting("YawDeceive", true));
   private final BooleanSetting pistonPacket = this.add(new BooleanSetting("PistonPacket", false));
   private final BooleanSetting powerPacket = this.add(new BooleanSetting("PowerPacket", true));
   private final BooleanSetting noEating = this.add(new BooleanSetting("EatingPause", true));
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final BooleanSetting allowWeb = this.add(new BooleanSetting("AllowWeb", true));
   private final SliderSetting updateDelay = this.add(new SliderSetting("Delay", 100, 0, 1000));
   private final BooleanSetting selfGround = this.add(new BooleanSetting("SelfGround", true));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 0.0, 6.0));
   private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 5.0, 0.0, 6.0));
   private final SliderSetting surroundCheck = this.add(new SliderSetting("SurroundCheck", 2, 0, 4));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final Timer timer = new Timer();

   public AutoPush() {
      super("AutoPush", Module.Category.Combat);
      this.setChinese("活塞推人");
      INSTANCE = this;
   }

   public static void pistonFacing(Direction i) {
      if (i == Direction.EAST) {
         Alien.ROTATION.snapAt(-90.0F, 5.0F);
      } else if (i == Direction.WEST) {
         Alien.ROTATION.snapAt(90.0F, 5.0F);
      } else if (i == Direction.NORTH) {
         Alien.ROTATION.snapAt(180.0F, 5.0F);
      } else if (i == Direction.SOUTH) {
         Alien.ROTATION.snapAt(0.0F, 5.0F);
      }
   }

   @Override
   public void onEnable() {
      AutoCrystal.INSTANCE.lastBreakTimer.reset();
   }

   boolean isTargetHere(BlockPos pos, Entity target) {
      return new Box(pos).intersects(target.getBoundingBox());
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.timer.passedMs(this.updateDelay.getValue())) {
         if (!this.selfGround.getValue() || mc.player.isOnGround()) {
            if (this.findBlock(this.getBlockType()) != -1 && this.findClass(PistonBlock.class) != -1) {
               if (!this.noEating.getValue() || !mc.player.isUsingItem()) {
                  if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                     for (PlayerEntity player : CombatUtil.getEnemies(this.range.getValue())) {
                        if (this.canPush(player)) {
                           float[] offset = new float[]{-0.25F, 0.0F, 0.25F};

                           for (float x : offset) {
                              for (float z : offset) {
                                 BlockPosX playerPos = new BlockPosX(player.getX() + x, player.getY() + 0.5, player.getZ() + z);

                                 for (Direction i : Direction.values()) {
                                    if (i != Direction.UP && i != Direction.DOWN) {
                                       BlockPos pos = playerPos.offset(i);
                                       if (this.isTargetHere(pos, player) && mc.world.canCollide(player, new Box(pos))) {
                                          if (this.tryPush(playerPos.offset(i.getOpposite()), i)) {
                                             this.timer.reset();
                                             return;
                                          }

                                          if (this.tryPush(playerPos.offset(i.getOpposite()).up(), i)) {
                                             this.timer.reset();
                                             return;
                                          }
                                       }
                                    }
                                 }
                              }
                           }

                           if (!mc.world
                              .canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 2.5, player.getZ())))) {
                              for (Direction ix : Direction.values()) {
                                 if (ix != Direction.UP && ix != Direction.DOWN) {
                                    BlockPos pos = EntityUtil.getEntityPos(player).offset(ix);
                                    Box box = player.getBoundingBox().offset(new Vec3d(ix.getOffsetX(), ix.getOffsetY(), ix.getOffsetZ()));
                                    if (this.getBlock(pos.up()) != Blocks.PISTON_HEAD
                                       && !mc.world.canCollide(player, box.offset(0.0, 1.0, 0.0))
                                       && !this.isTargetHere(pos, player)) {
                                       if (this.tryPush(EntityUtil.getEntityPos(player).offset(ix.getOpposite()).up(), ix)) {
                                          this.timer.reset();
                                          return;
                                       }

                                       if (this.tryPush(EntityUtil.getEntityPos(player).offset(ix.getOpposite()), ix)) {
                                          this.timer.reset();
                                          return;
                                       }
                                    }
                                 }
                              }
                           }

                           for (float x : offset) {
                              for (float z : offset) {
                                 BlockPosX playerPos = new BlockPosX(player.getX() + x, player.getY() + 0.5, player.getZ() + z);

                                 for (Direction ixx : Direction.values()) {
                                    if (ixx != Direction.UP && ixx != Direction.DOWN) {
                                       BlockPos pos = playerPos.offset(ixx);
                                       if (this.isTargetHere(pos, player)) {
                                          if (this.tryPush(playerPos.offset(ixx.getOpposite()).up(), ixx)) {
                                             this.timer.reset();
                                             return;
                                          }

                                          if (this.tryPush(playerPos.offset(ixx.getOpposite()), ixx)) {
                                             this.timer.reset();
                                             return;
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
            } else {
               if (this.autoDisable.getValue()) {
                  this.disable();
               }
            }
         }
      }
   }

   private boolean tryPush(BlockPos piston, Direction direction) {
      if (!mc.world.isAir(piston.offset(direction))) {
         return false;
      } else {
         if (this.isTrueFacing(piston, direction) && this.facingCheck(piston) && BlockUtil.clientCanPlace(piston, false)) {
            boolean canPower = false;
            if (BlockUtil.getPlaceSide(piston, this.placeRange.getValue()) != null) {
               CombatUtil.modifyPos = piston;
               CombatUtil.modifyBlockState = Blocks.PISTON.getDefaultState();

               for (Direction i : Direction.values()) {
                  if (this.getBlock(piston.offset(i)) == this.getBlockType()) {
                     canPower = true;
                     break;
                  }
               }

               for (Direction ix : Direction.values()) {
                  if (canPower) {
                     break;
                  }

                  if (BlockUtil.canPlace(piston.offset(ix), this.placeRange.getValue())) {
                     canPower = true;
                  }
               }

               CombatUtil.modifyPos = null;
               if (canPower) {
                  int pistonSlot = this.findClass(PistonBlock.class);
                  Direction side = BlockUtil.getPlaceSide(piston);
                  if (side != null) {
                     if (this.rotate.getValue()) {
                        Alien.ROTATION.lookAt(piston.offset(side), side.getOpposite());
                     }

                     if (this.yawDeceive.getValue()) {
                        pistonFacing(direction.getOpposite());
                     }

                     int old = mc.player.getInventory().selectedSlot;
                     this.doSwap(pistonSlot);
                     BlockUtil.placeBlock(piston, false, this.pistonPacket.getValue());
                     if (this.inventory.getValue()) {
                        this.doSwap(pistonSlot);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(old);
                     }

                     if (this.rotate.getValue() && this.yawDeceive.getValue()) {
                        Alien.ROTATION.lookAt(piston.offset(side), side.getOpposite());
                     }

                     if (this.rotate.getValue()) {
                        Alien.ROTATION.snapBack();
                     }

                     for (Direction ix : Direction.values()) {
                        if (this.getBlock(piston.offset(ix)) == this.getBlockType()) {
                           if (this.mine.getValue()) {
                              PacketMine.INSTANCE.mine(piston.offset(ix));
                           }

                           if (this.autoDisable.getValue()) {
                              this.disable();
                           }

                           return true;
                        }
                     }

                     for (Direction ixx : Direction.values()) {
                        if ((ixx != Direction.UP || !this.torch.getValue()) && BlockUtil.canPlace(piston.offset(ixx), this.placeRange.getValue())) {
                           int oldSlot = mc.player.getInventory().selectedSlot;
                           int powerSlot = this.findBlock(this.getBlockType());
                           this.doSwap(powerSlot);
                           BlockUtil.placeBlock(piston.offset(ixx), this.rotate.getValue(), this.powerPacket.getValue());
                           if (this.inventory.getValue()) {
                              this.doSwap(powerSlot);
                              EntityUtil.syncInventory();
                           } else {
                              this.doSwap(oldSlot);
                           }

                           if (this.mine.getValue()) {
                              PacketMine.INSTANCE.mine(piston.offset(ixx));
                           }

                           return true;
                        }
                     }

                     return true;
                  }
               }
            } else {
               Direction powerFacing = null;

               for (Direction ixxx : Direction.values()) {
                  if (ixxx != Direction.UP || !this.torch.getValue()) {
                     if (powerFacing != null) {
                        break;
                     }

                     CombatUtil.modifyPos = piston.offset(ixxx);
                     CombatUtil.modifyBlockState = this.getBlockType().getDefaultState();
                     if (BlockUtil.getPlaceSide(piston) != null) {
                        powerFacing = ixxx;
                     }

                     CombatUtil.modifyPos = null;
                     if (powerFacing != null && !BlockUtil.canPlace(piston.offset(powerFacing))) {
                        powerFacing = null;
                     }
                  }
               }

               if (powerFacing != null) {
                  int oldSlotx = mc.player.getInventory().selectedSlot;
                  int powerSlotx = this.findBlock(this.getBlockType());
                  this.doSwap(powerSlotx);
                  BlockUtil.placeBlock(piston.offset(powerFacing), this.rotate.getValue(), this.powerPacket.getValue());
                  if (this.inventory.getValue()) {
                     this.doSwap(powerSlotx);
                     EntityUtil.syncInventory();
                  } else {
                     this.doSwap(oldSlotx);
                  }

                  CombatUtil.modifyPos = piston.offset(powerFacing);
                  CombatUtil.modifyBlockState = this.getBlockType().getDefaultState();
                  int pistonSlot = this.findClass(PistonBlock.class);
                  Direction side = BlockUtil.getPlaceSide(piston);
                  if (side != null) {
                     if (this.rotate.getValue()) {
                        Alien.ROTATION.lookAt(piston.offset(side), side.getOpposite());
                     }

                     if (this.yawDeceive.getValue()) {
                        pistonFacing(direction.getOpposite());
                     }

                     int oldx = mc.player.getInventory().selectedSlot;
                     this.doSwap(pistonSlot);
                     BlockUtil.placeBlock(piston, false, this.pistonPacket.getValue());
                     if (this.inventory.getValue()) {
                        this.doSwap(pistonSlot);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(oldx);
                     }

                     if (this.rotate.getValue() && this.yawDeceive.getValue()) {
                        Alien.ROTATION.lookAt(piston.offset(side), side.getOpposite());
                     }

                     if (this.rotate.getValue()) {
                        Alien.ROTATION.snapBack();
                     }
                  }

                  CombatUtil.modifyPos = null;
                  return true;
               }
            }
         }

         BlockState state = mc.world.getBlockState(piston);
         if (state.getBlock() instanceof PistonBlock && this.getBlockState(piston).get(FacingBlock.FACING) == direction) {
            for (Direction ixxxx : Direction.values()) {
               if (this.getBlock(piston.offset(ixxxx)) == this.getBlockType()) {
                  if (this.autoDisable.getValue()) {
                     this.disable();
                     return true;
                  }

                  return false;
               }
            }

            for (Direction ixxxxx : Direction.values()) {
               if ((ixxxxx != Direction.UP || !this.torch.getValue()) && BlockUtil.canPlace(piston.offset(ixxxxx), this.placeRange.getValue())) {
                  int oldSlotxx = mc.player.getInventory().selectedSlot;
                  int powerSlotxx = this.findBlock(this.getBlockType());
                  this.doSwap(powerSlotxx);
                  BlockUtil.placeBlock(piston.offset(ixxxxx), this.rotate.getValue(), this.powerPacket.getValue());
                  if (this.inventory.getValue()) {
                     this.doSwap(powerSlotxx);
                     EntityUtil.syncInventory();
                  } else {
                     this.doSwap(oldSlotxx);
                  }

                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean facingCheck(BlockPos pos) {
      if (!ClientSetting.INSTANCE.lowVersion.getValue()) {
         return true;
      } else {
         Direction direction = MathUtil.getDirectionFromEntityLiving(pos, mc.player);
         return direction != Direction.UP && direction != Direction.DOWN;
      }
   }

   private boolean isTrueFacing(BlockPos pos, Direction facing) {
      if (this.yawDeceive.getValue()) {
         return true;
      } else {
         Direction side = BlockUtil.getPlaceSide(pos);
         if (side == null) {
            return false;
         } else {
            Vec3d directionVec = new Vec3d(
               pos.getX() + 0.5 + side.getVector().getX() * 0.5,
               pos.getY() + 0.5 + side.getVector().getY() * 0.5,
               pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
            );
            float[] rotation = RotationManager.getRotation(directionVec);
            return MathUtil.getFacingOrder(rotation[0], rotation[1]).getOpposite() == facing;
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

   public int findBlock(Block blockIn) {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(blockIn) : InventoryUtil.findBlock(blockIn);
   }

   public int findClass(Class<?> clazz) {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(clazz) : InventoryUtil.findClass(clazz);
   }

   private Boolean canPush(PlayerEntity player) {
      if (this.onlyGround.getValue() && !player.isOnGround()) {
         return false;
      } else if (!this.allowWeb.getValue() && Alien.PLAYER.isInWeb(player)) {
         return false;
      } else {
         float[] offset = new float[]{-0.25F, 0.0F, 0.25F};
         int progress = 0;
         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX() + 1.0, player.getY() + 0.5, player.getZ())))) {
            progress++;
         }

         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX() - 1.0, player.getY() + 0.5, player.getZ())))) {
            progress++;
         }

         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ() + 1.0)))) {
            progress++;
         }

         if (mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ() - 1.0)))) {
            progress++;
         }

         for (float x : offset) {
            for (float z : offset) {
               BlockPosX playerPos = new BlockPosX(player.getX() + x, player.getY() + 0.5, player.getZ() + z);

               for (Direction i : Direction.values()) {
                  if (i != Direction.UP && i != Direction.DOWN) {
                     BlockPos pos = playerPos.offset(i);
                     if (this.isTargetHere(pos, player)) {
                        if (mc.world.canCollide(player, new Box(pos))) {
                           return true;
                        }

                        if (progress > this.surroundCheck.getValue() - 1.0) {
                           return true;
                        }
                     }
                  }
               }
            }
         }

         if (!mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 2.5, player.getZ())))) {
            for (Direction ix : Direction.values()) {
               if (ix != Direction.UP && ix != Direction.DOWN) {
                  BlockPos pos = EntityUtil.getEntityPos(player).offset(ix);
                  Box box = player.getBoundingBox().offset(new Vec3d(ix.getOffsetX(), ix.getOffsetY(), ix.getOffsetZ()));
                  if (this.getBlock(pos.up()) != Blocks.PISTON_HEAD
                     && !mc.world.canCollide(player, box.offset(0.0, 1.0, 0.0))
                     && !this.isTargetHere(pos, player)
                     && mc.world.canCollide(player, new Box(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ())))) {
                     return true;
                  }
               }
            }
         }

         return progress > this.surroundCheck.getValue() - 1.0
            || Alien.HOLE.isHard(new BlockPosX(player.getX(), player.getY() + 0.5, player.getZ()));
      }
   }

   private Block getBlock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock();
   }

   private Block getBlockType() {
      return this.torch.getValue() ? Blocks.REDSTONE_TORCH : Blocks.REDSTONE_BLOCK;
   }

   private BlockState getBlockState(BlockPos pos) {
      return mc.world.getBlockState(pos);
   }
}
