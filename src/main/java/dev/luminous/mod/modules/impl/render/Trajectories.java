package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.player.AutoPearl;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class Trajectories extends Module {
   static MatrixStack matrixStack;
   private final ColorSetting hand = this.add(new ColorSetting("Hand", new Color(255, 255, 255, 255)).injectBoolean(true));
   private final ColorSetting pearl = this.add(new ColorSetting("Pearl", new Color(255, 255, 255, 255)).injectBoolean(true));
   private final ColorSetting arrow = this.add(new ColorSetting("Arrow", new Color(255, 255, 255, 255)).injectBoolean(true));
   private final ColorSetting xp = this.add(new ColorSetting("XP", new Color(255, 255, 255, 255)).injectBoolean(true));

   public Trajectories() {
      super("Trajectories", Module.Category.Render);
      this.setChinese("抛物线预测");
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (!nullCheck()) {
         Trajectories.matrixStack = matrixStack;
         if (this.pearl.booleanValue || this.arrow.booleanValue || this.xp.booleanValue) {
            RenderSystem.disableDepthTest();

            for (Entity en : Alien.THREAD.getEntities()) {
               if (en instanceof EnderPearlEntity && this.pearl.booleanValue) {
                  this.calcTrajectory(en, this.pearl.getValue());
               }

               if (en instanceof ArrowEntity && this.arrow.booleanValue) {
                  this.calcTrajectory(en, this.arrow.getValue());
               }

               if (en instanceof ExperienceBottleEntity && this.xp.booleanValue) {
                  this.calcTrajectory(en, this.xp.getValue());
               }
            }

            RenderSystem.enableDepthTest();
         }

         if (this.hand.booleanValue) {
            if (!mc.options.getPerspective().isFirstPerson()) {
               return;
            }

            ItemStack mainHand = mc.player.getMainHandStack();
            ItemStack offHand = mc.player.getOffHandStack();
            Hand hand;
            if (!(mainHand.getItem() instanceof BowItem)
               && !(mainHand.getItem() instanceof CrossbowItem)
               && !this.isThrowable(mainHand.getItem())
               && !AutoPearl.INSTANCE.isOn()) {
               if (!(offHand.getItem() instanceof BowItem) && !(offHand.getItem() instanceof CrossbowItem) && !this.isThrowable(offHand.getItem())) {
                  return;
               }

               hand = Hand.OFF_HAND;
            } else {
               hand = Hand.MAIN_HAND;
            }

            RenderSystem.disableDepthTest();
            boolean prev_bob = (Boolean)mc.options.getBobView().getValue();
            mc.options.getBobView().setValue(false);
            double x = MathUtil.interpolate(mc.player.prevX, mc.player.getX(), (double)mc.getRenderTickCounter().getTickDelta(true));
            double y = MathUtil.interpolate(mc.player.prevY, mc.player.getY(), (double)mc.getRenderTickCounter().getTickDelta(true));
            double z = MathUtil.interpolate(mc.player.prevZ, mc.player.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true));
            if (offHand.getItem() instanceof CrossbowItem
                  && EnchantmentHelper.getLevel(
                        (RegistryEntry)mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.MULTISHOT.getRegistryRef()).getOptional(Enchantments.MULTISHOT).get(),
                        offHand
                     )
                     != 0
               || mainHand.getItem() instanceof CrossbowItem
                  && EnchantmentHelper.getLevel(
                        (RegistryEntry)mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.MULTISHOT.getRegistryRef()).getOptional(Enchantments.MULTISHOT).get(),
                        mainHand
                     )
                     != 0) {
               this.calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYaw() - 10.0F, x, y, z);
               this.calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYaw(), x, y, z);
               this.calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYaw() + 10.0F, x, y, z);
            } else {
               this.calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYaw(), x, y, z);
            }

            mc.options.getBobView().setValue(prev_bob);
            RenderSystem.enableDepthTest();
         }
      }
   }

   private void calcTrajectory(Entity e, Color color) {
      double motionX = e.getVelocity().x;
      double motionY = e.getVelocity().y;
      double motionZ = e.getVelocity().z;
      if (motionX != 0.0 || motionY != 0.0 || motionZ != 0.0) {
         double x = e.getX();
         double y = e.getY();
         double z = e.getZ();

         for (int i = 0; i < 300; i++) {
            Vec3d lastPos = new Vec3d(x, y, z);
            x += motionX;
            y += motionY;
            z += motionZ;
            if (mc.world.getBlockState(new BlockPos((int)x, (int)y, (int)z)).getBlock() == Blocks.WATER) {
               motionX *= 0.8;
               motionY *= 0.8;
               motionZ *= 0.8;
            } else {
               motionX *= 0.99;
               motionY *= 0.99;
               motionZ *= 0.99;
            }

            if (e instanceof ArrowEntity) {
               motionY -= 0.05F;
            } else {
               motionY -= 0.03F;
            }

            Vec3d pos = new Vec3d(x, y, z);
            if (mc.world.raycast(new RaycastContext(lastPos, pos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player)) != null
                  && (
                     mc.world.raycast(new RaycastContext(lastPos, pos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player)).getType() == Type.ENTITY
                        || mc.world.raycast(new RaycastContext(lastPos, pos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player)).getType()
                           == Type.BLOCK
                  )
               || y <= -65.0) {
               break;
            }

            int alpha = (int)MathUtil.clamp(255.0F * ((i + 1) / 10.0F), 0.0F, 255.0F);
            Render3DUtil.drawLine(lastPos, pos, ColorUtil.injectAlpha(color, alpha));
         }
      }
   }

   private void calcTrajectory(Item item, float yaw, double x, double y, double z) {
      y = y + mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;
      if (item == mc.player.getMainHandStack().getItem()) {
         x -= MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
         z -= MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
      } else {
         x += MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
         z += MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
      }

      float maxDist = this.getDistance(item);
      double motionX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(mc.player.getPitch() / 180.0F * (float) Math.PI) * maxDist;
      double motionY = -MathHelper.sin((mc.player.getPitch() - this.getThrowPitch(item)) / 180.0F * 3.141593F) * maxDist;
      double motionZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(mc.player.getPitch() / 180.0F * (float) Math.PI) * maxDist;
      float power = mc.player.getItemUseTime() / 20.0F;
      power = (power * power + power * 2.0F) / 3.0F;
      if (power > 1.0F) {
         power = 1.0F;
      }

      float distance = MathHelper.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
      motionX /= distance;
      motionY /= distance;
      motionZ /= distance;
      float pow = (item instanceof BowItem ? power * 2.0F : (item instanceof CrossbowItem ? 2.2F : 1.0F)) * this.getThrowVelocity(item);
      motionX *= pow;
      motionY *= pow;
      motionZ *= pow;
      motionX += mc.player.getVelocity().getX();
      motionY += mc.player.getVelocity().getY();
      motionZ += mc.player.getVelocity().getZ();

      for (int i = 0; i < 300; i++) {
         Vec3d lastPos = new Vec3d(x, y, z);
         x += motionX;
         y += motionY;
         z += motionZ;
         if (mc.world.getBlockState(new BlockPos((int)x, (int)y, (int)z)).getBlock() == Blocks.WATER) {
            motionX *= 0.8;
            motionY *= 0.8;
            motionZ *= 0.8;
         } else {
            motionX *= 0.99;
            motionY *= 0.99;
            motionZ *= 0.99;
         }

         if (item instanceof BowItem) {
            motionY -= 0.05F;
         } else if (mc.player.getMainHandStack().getItem() instanceof CrossbowItem) {
            motionY -= 0.05F;
         } else {
            motionY -= 0.03F;
         }

         Vec3d pos = new Vec3d(x, y, z);

         for (Entity ent : Alien.THREAD.getEntities()) {
            if (!(ent instanceof ArrowEntity)
               && !ent.equals(mc.player)
               && ent.getBoundingBox().intersects(new Box(x - 0.3, y - 0.3, z - 0.3, x + 0.3, y + 0.3, z + 0.3))) {
               Render3DUtil.drawBox(matrixStack, ent.getBoundingBox(), this.hand.getValue());
               break;
            }
         }

         BlockHitResult bhr = mc.world.raycast(new RaycastContext(lastPos, pos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
         if (bhr != null && bhr.getType() == Type.BLOCK) {
            Render3DUtil.drawBox(matrixStack, new Box(bhr.getBlockPos()), this.hand.getValue());
            break;
         }

         if (y <= -65.0) {
            break;
         }

         if (motionX != 0.0 || motionY != 0.0 || motionZ != 0.0) {
            Render3DUtil.drawLine(lastPos, pos, this.hand.getValue());
         }
      }
   }

   private boolean isThrowable(Item item) {
      return item instanceof EnderPearlItem
         || item instanceof TridentItem
         || item instanceof ExperienceBottleItem
         || item instanceof SnowballItem
         || item instanceof EggItem
         || item instanceof SplashPotionItem
         || item instanceof LingeringPotionItem;
   }

   private float getDistance(Item item) {
      return item instanceof BowItem ? 1.0F : 0.4F;
   }

   private float getThrowVelocity(Item item) {
      if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
         return 0.5F;
      } else if (item instanceof ExperienceBottleItem) {
         return 0.59F;
      } else {
         return item instanceof TridentItem ? 2.0F : 1.5F;
      }
   }

   private int getThrowPitch(Item item) {
      return !(item instanceof SplashPotionItem) && !(item instanceof LingeringPotionItem) && !(item instanceof ExperienceBottleItem) ? 0 : 20;
   }
}
