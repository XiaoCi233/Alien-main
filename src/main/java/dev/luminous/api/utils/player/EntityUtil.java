package dev.luminous.api.utils.player;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.PeekScreen;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class EntityUtil implements Wrapper {
   public static boolean inInventory() {
      return mc.currentScreen == null
         || mc.currentScreen instanceof GameOptionsScreen
         || mc.currentScreen instanceof OptionsScreen
         || mc.currentScreen instanceof PeekScreen
         || mc.currentScreen instanceof ChatScreen
         || mc.currentScreen instanceof InventoryScreen
         || mc.currentScreen instanceof ClickGuiScreen
         || mc.currentScreen instanceof GameMenuScreen;
   }

   public static boolean isHoldingWeapon(PlayerEntity player) {
      return player.getMainHandStack().getItem() instanceof SwordItem
         || player.getMainHandStack().getItem() instanceof AxeItem
         || player.getMainHandStack().getItem() instanceof MaceItem
         || player.getMainHandStack().getItem() instanceof TridentItem;
   }

   public static boolean isInsideBlock(PlayerEntity player) {
      return BlockUtil.canCollide(player, player.getBoundingBox());
   }

   public static boolean isInsideBlock() {
      return isInsideBlock(mc.player);
   }

   public static int getDamagePercent(ItemStack stack) {
      return stack.getDamage() == stack.getMaxDamage() ? 100 : (int)((stack.getMaxDamage() - stack.getDamage()) / Math.max(0.1, stack.getMaxDamage()) * 100.0);
   }

   public static boolean isArmorLow(PlayerEntity player, int durability) {
      for (ItemStack piece : player.getArmorItems()) {
         if (piece == null || piece.isEmpty()) {
            return true;
         }

         if (getDamagePercent(piece) < durability) {
            return true;
         }
      }

      return false;
   }

   public static float getHealth(Entity entity) {
      if (entity.isLiving()) {
         LivingEntity livingBase = (LivingEntity)entity;
         return livingBase.getHealth() + livingBase.getAbsorptionAmount();
      } else {
         return 0.0F;
      }
   }

   public static BlockPos getEntityPos(Entity entity) {
      return new BlockPosX(entity.getPos());
   }

   public static BlockPos getPlayerPos(boolean fix) {
      return new BlockPosX(mc.player.getPos(), fix);
   }

   public static BlockPos getEntityPos(Entity entity, boolean fix) {
      return new BlockPosX(entity.getPos(), fix);
   }

   public static boolean canSee(BlockPos pos, Direction side) {
      Vec3d testVec = pos.toCenterPos().add(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5);
      HitResult result = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), testVec, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
      return result == null || result.getType() == Type.MISS;
   }

   public static void swingHand(Hand hand, SwingSide side) {
      switch (side) {
         case All:
            mc.player.swingHand(hand);
            break;
         case Client:
            mc.player.swingHand(hand, false);
            break;
         case Server:
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
      }
   }

   public static void syncInventory() {
      if (AntiCheat.INSTANCE.closeScreen.getValue()) {
         mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
      }
   }
}
