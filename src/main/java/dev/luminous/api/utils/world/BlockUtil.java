package dev.luminous.api.utils.world;

import dev.luminous.Alien;
import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.combat.AutoCrystal;
import dev.luminous.mod.modules.impl.combat.AutoWeb;
import dev.luminous.mod.modules.impl.player.AirPlace;
import dev.luminous.mod.modules.settings.enums.Placement;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CartographyTableBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.GrindstoneBlock;
import net.minecraft.block.LoomBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.StonecutterBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

public class BlockUtil implements Wrapper {
   public static final List<BlockPos> placedPos = new ArrayList();
   private static final double MIN_EYE_HEIGHT = 0.4;
   private static final double MAX_EYE_HEIGHT = 1.62;
   private static final double MOVEMENT_THRESHOLD = 2.0E-4;

   public static boolean canPlace(BlockPos pos) {
      return canPlace(pos, 1000.0);
   }

   public static boolean canPlace(BlockPos pos, double distance) {
      if (getPlaceSide(pos, distance) == null) {
         return false;
      } else {
         return !canReplace(pos) ? false : !hasEntity(pos, false);
      }
   }

   public static boolean canPlace(BlockPos pos, double distance, boolean ignoreCrystal) {
      if (getPlaceSide(pos, distance) == null) {
         return false;
      } else {
         return !canReplace(pos) ? false : !hasEntity(pos, ignoreCrystal);
      }
   }

   public static boolean clientCanPlace(BlockPos pos, boolean ignoreCrystal) {
      return !canReplace(pos) ? false : !hasEntity(pos, ignoreCrystal);
   }

   public static List<Entity> getEntities(Box box) {
      List<Entity> list = new ArrayList();

      for (Entity entity : Alien.THREAD.getEntities()) {
         if (entity != null
            && (!(entity instanceof ArmorStandEntity) || !AntiCheat.INSTANCE.ignoreArmorStand.getValue())
            && entity.getBoundingBox().intersects(box)) {
            list.add(entity);
         }
      }

      return list;
   }

   public static List<EndCrystalEntity> getEndCrystals(Box box) {
      List<EndCrystalEntity> list = new ArrayList();

      for (Entity entity : Alien.THREAD.getEntities()) {
         if (entity instanceof EndCrystalEntity crystal && crystal.getBoundingBox().intersects(box)) {
            list.add(crystal);
         }
      }

      return list;
   }

   public static boolean hasEntity(BlockPos pos, boolean ignoreCrystal) {
      return hasEntity(new Box(pos), ignoreCrystal);
   }

   public static boolean hasEntity(Box box, boolean ignoreCrystal) {
      for (Entity entity : getEntities(box)) {
         if (entity.isAlive()
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrbEntity)
            && !(entity instanceof ExperienceBottleEntity)
            && !(entity instanceof ArrowEntity)
            && (
               !ignoreCrystal
                  || !(entity instanceof EndCrystalEntity)
                  || !(mc.player.getEyePos().distanceTo(MathUtil.getClosestPoint(entity)) <= AntiCheat.INSTANCE.ieRange.getValue())
            )) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasCrystal(BlockPos pos) {
      for (Entity entity : getEndCrystals(new Box(pos))) {
         if (entity.isAlive() && entity instanceof EndCrystalEntity) {
            return true;
         }
      }

      return false;
   }

   public static boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal) {
      return noEntityBlockCrystal(pos, ignoreCrystal, false);
   }

   public static boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
      for (Entity entity : getEntities(new Box(pos))) {
         if (entity.isAlive()
            && (!ignoreItem || !(entity instanceof ItemEntity))
            && (
               !ignoreCrystal
                  || !(entity instanceof EndCrystalEntity)
                  || !(mc.player.getEyePos().distanceTo(MathUtil.getClosestPoint(entity)) <= AntiCheat.INSTANCE.ieRange.getValue())
            )) {
            return false;
         }
      }

      return true;
   }

   public static boolean canPlaceCrystal(BlockPos pos) {
      BlockPos obsPos = pos.down();
      BlockPos boost = obsPos.up();
      return (getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN)
         && getClickSideStrict(obsPos) != null
         && mc.world.isAir(boost)
         && noEntityBlockCrystal(boost, false)
         && noEntityBlockCrystal(boost.up(), false)
         && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(boost.up()));
   }

   public static void placeCrystal(BlockPos pos, boolean rotate) {
      boolean offhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
      BlockPos obsPos = pos.down();
      Direction facing = getClickSide(obsPos);
      Vec3d vec = obsPos.toCenterPos().add(facing.getVector().getX() * 0.5, facing.getVector().getY() * 0.5, facing.getVector().getZ() * 0.5);
      if (rotate) {
         Alien.ROTATION.lookAt(vec);
      }

      clickBlock(obsPos, facing, false, offhand ? Hand.OFF_HAND : Hand.MAIN_HAND);
   }

   public static void placeBlock(BlockPos pos, boolean rotate) {
      placeBlock(pos, rotate, AntiCheat.INSTANCE.packetPlace.getValue());
   }

   public static void placeBlock(BlockPos pos, boolean rotate, boolean packet) {
      if (allowAirPlace()) {
         placedPos.add(pos);
         airPlace(pos, rotate, Hand.MAIN_HAND, packet);
      } else {
         Direction side = getPlaceSide(pos);
         if (side != null) {
            placedPos.add(pos);
            clickBlock(pos.offset(side), side.getOpposite(), rotate, Hand.MAIN_HAND, packet);
         }
      }
   }

   public static void clickBlock(BlockPos pos, Direction side, boolean rotate) {
      clickBlock(pos, side, rotate, Hand.MAIN_HAND);
   }

   public static void clickBlock(BlockPos pos, Direction side, boolean rotate, Hand hand) {
      clickBlock(pos, side, rotate, hand, AntiCheat.INSTANCE.packetPlace.getValue());
   }

   public static void clickBlock(BlockPos pos, Direction side, boolean rotate, boolean packet) {
      clickBlock(pos, side, rotate, Hand.MAIN_HAND, packet);
   }

   public static void clickBlock(BlockPos pos, Direction side, boolean rotate, Hand hand, boolean packet) {
      Vec3d directionVec = new Vec3d(
         pos.getX() + 0.5 + side.getVector().getX() * 0.5,
         pos.getY() + 0.5 + side.getVector().getY() * 0.5,
         pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
      );
      if (rotate) {
         Alien.ROTATION.lookAt(directionVec);
      }

      EntityUtil.swingHand(hand, (SwingSide)AntiCheat.INSTANCE.interactSwing.getValue());
      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      if (packet) {
         Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(hand, result, id));
      } else {
         mc.interactionManager.interactBlock(mc.player, hand, result);
      }

      mc.itemUseCooldown = 4;
      if (rotate) {
         Alien.ROTATION.snapBack();
      }
   }

   public static void clickBlock(BlockPos pos, Direction side, boolean rotate, Hand hand, SwingSide swingSide) {
      Vec3d directionVec = new Vec3d(
         pos.getX() + 0.5 + side.getVector().getX() * 0.5,
         pos.getY() + 0.5 + side.getVector().getY() * 0.5,
         pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
      );
      if (rotate) {
         Alien.ROTATION.lookAt(directionVec);
      }

      EntityUtil.swingHand(hand, swingSide);
      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(hand, result, id));
      mc.itemUseCooldown = 4;
      if (rotate) {
         Alien.ROTATION.snapBack();
      }
   }

   public static void airPlace(BlockPos pos, boolean rotate) {
      airPlace(pos, rotate, Hand.MAIN_HAND, AntiCheat.INSTANCE.packetPlace.getValue());
   }

   public static void airPlace(BlockPos pos, boolean rotate, Hand hand, boolean packet) {
      boolean bypass = hand == Hand.MAIN_HAND && AirPlace.INSTANCE.grimBypass.getValue();
      if (bypass) {
         mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
         hand = Hand.OFF_HAND;
      }

      Direction side = getClickSide(pos);
      Vec3d directionVec = new Vec3d(
         pos.getX() + 0.5 + side.getVector().getX() * 0.5,
         pos.getY() + 0.5 + side.getVector().getY() * 0.5,
         pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
      );
      if (rotate) {
         Alien.ROTATION.lookAt(directionVec);
      }

      EntityUtil.swingHand(hand, (SwingSide)AntiCheat.INSTANCE.interactSwing.getValue());
      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      if (packet) {
         Hand finalHand = hand;
         Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(finalHand, result, id));
      } else {
         mc.interactionManager.interactBlock(mc.player, hand, result);
      }

      mc.itemUseCooldown = 4;
      if (rotate) {
         Alien.ROTATION.snapBack();
      }

      if (bypass) {
         mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
      }
   }

   public static double distanceToXZ(double x, double z, double x2, double z2) {
      double dx = x2 - x;
      double dz = z2 - z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   public static double distanceToXZ(double x, double z) {
      return distanceToXZ(x, z, mc.player.getX(), mc.player.getZ());
   }

   public static Direction getPlaceSide(BlockPos pos) {
      if (allowAirPlace()) {
         return getClickSide(pos);
      } else {
         double minDistance = Double.MAX_VALUE;
         Direction side = null;

         for (Direction i : Direction.values()) {
            if (canClick(pos.offset(i)) && !canReplace(pos.offset(i)) && isStrictDirection(pos.offset(i), i.getOpposite())) {
               double vecDis = mc.player
                  .getEyePos()
                  .squaredDistanceTo(pos.toCenterPos().add(i.getVector().getX() * 0.5, i.getVector().getY() * 0.5, i.getVector().getZ() * 0.5));
               if (!(vecDis > minDistance)) {
                  side = i;
                  minDistance = vecDis;
               }
            }
         }

         return side;
      }
   }

   public static Direction getBestNeighboring(BlockPos pos, Direction facing) {
      Direction bestFacing = null;
      double distance = 0.0;

      for (Direction i : Direction.values()) {
         if ((facing == null || !pos.offset(i).equals(pos.offset(facing, -1)))
            && i != Direction.DOWN
            && getPlaceSide(pos) != null
            && (bestFacing == null || mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos()) < distance)) {
            bestFacing = i;
            distance = mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos());
         }
      }

      return bestFacing;
   }

   public static Direction getPlaceSide(BlockPos pos, double reachDistance) {
      if (allowAirPlace()) {
         Direction i = getClickSide(pos);
         double vecDis = mc.player
            .getEyePos()
            .squaredDistanceTo(pos.toCenterPos().add(i.getVector().getX() * 0.5, i.getVector().getY() * 0.5, i.getVector().getZ() * 0.5));
         return Math.sqrt(vecDis) > reachDistance ? null : Direction.DOWN;
      } else {
         double minDistance = Double.MAX_VALUE;
         Direction side = null;

         for (Direction i : Direction.values()) {
            if (canClick(pos.offset(i)) && !canReplace(pos.offset(i)) && isStrictDirection(pos.offset(i), i.getOpposite())) {
               double vecDis = mc.player
                  .getEyePos()
                  .squaredDistanceTo(pos.toCenterPos().add(i.getVector().getX() * 0.5, i.getVector().getY() * 0.5, i.getVector().getZ() * 0.5));
               if (!(Math.sqrt(vecDis) > reachDistance) && !(vecDis > minDistance)) {
                  side = i;
                  minDistance = vecDis;
               }
            }
         }

         return side;
      }
   }

   public static Direction getClickSide(BlockPos pos) {
      Direction side = Direction.UP;
      double minDistance = Double.MAX_VALUE;

      for (Direction i : Direction.values()) {
         if (isStrictDirection(pos, i)) {
            double disSq = mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos());
            if (!(disSq > minDistance)) {
               side = i;
               minDistance = disSq;
            }
         }
      }

      return side;
   }

   public static Direction getClickSideStrict(BlockPos pos) {
      Direction side = null;
      double minDistance = Double.MAX_VALUE;

      for (Direction i : Direction.values()) {
         if (isStrictDirection(pos, i)) {
            double disSq = mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos());
            if (!(disSq > minDistance)) {
               side = i;
               minDistance = disSq;
            }
         }
      }

      return side;
   }

   public static boolean isStrictDirection(BlockPos pos, Direction side, double reachDistance) {
      double vecDis = mc.player
         .getEyePos()
         .squaredDistanceTo(pos.toCenterPos().add(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5));
      return Math.sqrt(vecDis) > reachDistance ? false : isStrictDirection(pos, side);
   }

   public static boolean isStrictDirection(BlockPos pos, Direction side) {
      switch ((Placement)AntiCheat.INSTANCE.placement.getValue()) {
         case Vanilla:
            return true;
         case Legit:
            return EntityUtil.canSee(pos, side);
         case Grim:
            return grimStrictDirectionCheck(pos, side, mc.world, mc.player);
         case NCP:
            if (mc.world.getBlockState(pos.offset(side)).isFullCube(mc.world, pos.offset(side))) {
               return false;
            }

            Vec3d eyePos = mc.player.getEyePos();
            Vec3d blockCenter = pos.toCenterPos();
            ArrayList<Direction> validAxis = new ArrayList();
            validAxis.addAll(checkAxis(eyePos.x - blockCenter.x, Direction.WEST, Direction.EAST, false));
            validAxis.addAll(checkAxis(eyePos.y - blockCenter.y, Direction.DOWN, Direction.UP, true));
            validAxis.addAll(checkAxis(eyePos.z - blockCenter.z, Direction.NORTH, Direction.SOUTH, false));
            return validAxis.contains(side);
         default:
            return true;
      }
   }

   public static boolean grimStrictDirectionCheck(BlockPos pos, Direction direction, ClientWorld level, ClientPlayerEntity player) {
      Box combined = getCombinedBox(pos, level);
      Box eyePositions = new Box(
            player.getX(),
            player.getY() + 0.4,
            player.getZ(),
            player.getX(),
            player.getY() + 1.62,
            player.getZ()
         )
         .expand(2.0E-4);
      if (isIntersected(eyePositions, combined)) {
         return true;
      } else {
         switch (direction) {
            case NORTH:
               if (!(eyePositions.minZ > combined.minZ)) {
                  return true;
               }
               break;
            case SOUTH:
               if (!(eyePositions.maxZ < combined.maxZ)) {
                  return true;
               }
               break;
            case EAST:
               if (!(eyePositions.maxX < combined.maxX)) {
                  return true;
               }
               break;
            case WEST:
               if (!(eyePositions.minX > combined.minX)) {
                  return true;
               }
               break;
            case UP:
               if (!(eyePositions.maxY < combined.maxY)) {
                  return true;
               }
               break;
            case DOWN:
               if (!(eyePositions.minY > combined.minY)) {
                  return true;
               }
               break;
            default:
               throw new MatchException(null, null);
         }

         return false;
      }
   }

   private static Box getCombinedBox(BlockPos pos, World level) {
      VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos).offset(pos.getX(), pos.getY(), pos.getZ());
      Box combined = new Box(pos);

      for (Box box : shape.getBoundingBoxes()) {
         double minX = Math.max(box.minX, combined.minX);
         double minY = Math.max(box.minY, combined.minY);
         double minZ = Math.max(box.minZ, combined.minZ);
         double maxX = Math.min(box.maxX, combined.maxX);
         double maxY = Math.min(box.maxY, combined.maxY);
         double maxZ = Math.min(box.maxZ, combined.maxZ);
         combined = new Box(minX, minY, minZ, maxX, maxY, maxZ);
      }

      return combined;
   }

   private static boolean isIntersected(Box bb, Box other) {
      return other.maxX - 1.0E-7 > bb.minX
         && other.minX + 1.0E-7 < bb.maxX
         && other.maxY - 1.0E-7 > bb.minY
         && other.minY + 1.0E-7 < bb.maxY
         && other.maxZ - 1.0E-7 > bb.minZ
         && other.minZ + 1.0E-7 < bb.maxZ;
   }

   public static ArrayList<Direction> checkAxis(double diff, Direction negativeSide, Direction positiveSide, boolean vertical) {
      ArrayList<Direction> valid = new ArrayList();
      if (vertical) {
         if (diff < -0.5) {
            valid.add(negativeSide);
         }

         if (AntiCheat.INSTANCE.upDirectionLimit.getValue()) {
            if (diff > 0.5) {
               valid.add(positiveSide);
            }
         } else if (diff > -0.5) {
            valid.add(positiveSide);
         }
      } else {
         if (diff < -0.5) {
            valid.add(negativeSide);
         }

         if (diff > 0.5) {
            valid.add(positiveSide);
         }
      }

      return valid;
   }

   public static ArrayList<BlockEntity> getTileEntities() {
      return (ArrayList<BlockEntity>)getLoadedChunks()
         .flatMap(chunk -> chunk.getBlockEntities().values().stream())
         .collect(Collectors.toCollection(ArrayList::new));
   }

   public static Stream<WorldChunk> getLoadedChunks() {
      int radius = Math.max(2, mc.options.getClampedViewDistance()) + 3;
      int diameter = radius * 2 + 1;
      ChunkPos center = mc.player.getChunkPos();
      ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
      ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);
      return Stream.iterate(min, pos -> {
         int x = pos.x;
         int z = pos.z;
         if (++x > max.x) {
            x = min.x;
            z++;
         }

         return new ChunkPos(x, z);
      }).limit((long)diameter * diameter).filter(c -> mc.world.isChunkLoaded(c.x, c.z)).map(c -> mc.world.getChunk(c.x, c.z)).filter(Objects::nonNull);
   }

   public static ArrayList<BlockPos> getSphere(float range) {
      return getSphere(range, mc.player.getEyePos());
   }

   public static BlockPos getBlock(Block block, float range) {
      for (BlockPos pos : getSphere(range)) {
         if (mc.world.getBlockState(pos).getBlock() == block) {
            return pos;
         }
      }

      return null;
   }

   public static BlockPos getBlock(Class<?> block, float range) {
      for (BlockPos pos : getSphere(range)) {
         if (block.isInstance(mc.world.getBlockState(pos).getBlock())) {
            return pos;
         }
      }

      return null;
   }

   public static ArrayList<BlockPos> getSphere(float range, Vec3d pos) {
      ArrayList<BlockPos> list = new ArrayList();

      for (double y = pos.getY() + range; y > pos.getY() - range; y--) {
         if (!(y < -64.0)) {
            for (double x = pos.getX() - range; x < pos.getX() + range; x++) {
               for (double z = pos.getZ() - range; z < pos.getZ() + range; z++) {
                  BlockPos curPos = new BlockPosX(x, y, z);
                  if (!(curPos.toCenterPos().distanceTo(pos) > range)) {
                     list.add(curPos);
                  }
               }
            }
         }
      }

      return list;
   }

   public static Block getBlock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock();
   }

   public static boolean canReplace(BlockPos pos) {
      if (pos.getY() >= 320) {
         return false;
      } else if (AntiCheat.INSTANCE.multiPlace.getValue() && placedPos.contains(pos)) {
         return false;
      } else {
         BlockState state = mc.world.getBlockState(pos);
         return state.getBlock() == Blocks.COBWEB && AutoWeb.ignore && AutoCrystal.INSTANCE.replace.getValue() || state.isReplaceable();
      }
   }

   public static boolean canClick(BlockPos pos) {
      if (AntiCheat.INSTANCE.multiPlace.getValue() && placedPos.contains(pos)) {
         return true;
      } else {
         BlockState state = mc.world.getBlockState(pos);
         Block block = state.getBlock();
         return block == Blocks.COBWEB && AutoWeb.ignore ? AutoCrystal.INSTANCE.airPlace.getValue() : mc.player.isSneaking() || !isClickable(block);
      }
   }

   public static boolean isClickable(Block block) {
      return block instanceof CraftingTableBlock
         || block instanceof AnvilBlock
         || block instanceof LoomBlock
         || block instanceof CartographyTableBlock
         || block instanceof GrindstoneBlock
         || block instanceof StonecutterBlock
         || block instanceof ButtonBlock
         || block instanceof AbstractPressurePlateBlock
         || block instanceof BlockWithEntity
         || block instanceof BedBlock
         || block instanceof FenceGateBlock
         || block instanceof DoorBlock
         || block instanceof NoteBlock
         || block instanceof TrapdoorBlock;
   }

   public static boolean canCollide(Box box) {
      return canCollide(mc.player, box);
   }

   public static boolean canCollide(@Nullable Entity entity, Box box) {
      BlockCollisionSpliterator<VoxelShape> blockCollisionSpliterator = new BlockCollisionSpliterator(
         mc.world, entity, box, false, (pos, voxelShape) -> voxelShape
      );

      while (blockCollisionSpliterator.hasNext()) {
         if (!((VoxelShape)blockCollisionSpliterator.next()).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   public static boolean allowAirPlace() {
      return AirPlace.INSTANCE.isOn() && AirPlace.INSTANCE.module.getValue();
   }
}
