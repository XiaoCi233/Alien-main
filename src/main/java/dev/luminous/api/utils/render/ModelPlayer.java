package dev.luminous.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPart.Cuboid;
import net.minecraft.client.model.ModelPart.Quad;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ModelPlayer extends PlayerEntityModel<PlayerEntity> {
   public final PlayerEntity player;
   private static final Vector4f pos1 = new Vector4f();
   private static final Vector4f pos2 = new Vector4f();
   private static final Vector4f pos3 = new Vector4f();
   private static final Vector4f pos4 = new Vector4f();

   public ModelPlayer(PlayerEntity player) {
      super(
         new Context(
               Wrapper.mc.getEntityRenderDispatcher(),
               Wrapper.mc.getItemRenderer(),
               Wrapper.mc.getBlockRenderManager(),
               Wrapper.mc.getEntityRenderDispatcher().getHeldItemRenderer(),
               Wrapper.mc.getResourceManager(),
               Wrapper.mc.getEntityModelLoader(),
               Wrapper.mc.textRenderer
            )
            .getPart(EntityModelLayers.PLAYER),
         false
      );
      this.player = player;
      this.leftPants.visible = false;
      this.rightPants.visible = false;
      this.leftSleeve.visible = false;
      this.rightSleeve.visible = false;
      this.jacket.visible = false;
      this.hat.visible = false;
      this.getHead().scale(new Vector3f(-0.05F, -0.05F, -0.05F));
      this.sneaking = player.isInSneakingPose();
   }

   public void render(MatrixStack matrices, ColorSetting fill, ColorSetting line) {
      this.render(matrices, fill, line, 1.0, 0.0, 1.0, 0.0, false, false);
   }

   public void render(
      MatrixStack matrices, ColorSetting fill, ColorSetting line, double alpha, double yOffset, double scale, double yaw, boolean noLimb, boolean forceSneaking
   ) {
      if (forceSneaking) {
         this.sneaking = true;
      }

      double x = this.player.getX() - Wrapper.mc.getEntityRenderDispatcher().camera.getPos().getX();
      double y = this.player.getY() - Wrapper.mc.getEntityRenderDispatcher().camera.getPos().getY() + yOffset;
      double z = this.player.getZ() - Wrapper.mc.getEntityRenderDispatcher().camera.getPos().getZ();
      matrices.push();
      matrices.translate((float)x, (float)y, (float)z);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtil.rad(180.0F - this.player.bodyYaw + (float)yaw)));
      this.handSwingProgress = this.player.getHandSwingProgress(1.0F);
      float j = ((ILivingEntity)this.player).getLeaningPitch();
      if (this.player.isFallFlying()) {
         float k = this.player.getPitch();
         float l = this.player.getFallFlyingTicks() + this.player.bodyYaw + (float)yaw;
         float m = MathHelper.clamp(l * l / 100.0F, 0.0F, 1.0F);
         if (!this.player.isUsingRiptide()) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(m * (-90.0F - k)));
         }

         Vec3d vec3d = this.player.getRotationVec(1.0F);
         Vec3d vec3d2 = this.player.getVelocity();
         double d = vec3d2.horizontalLengthSquared();
         double e = vec3d.horizontalLengthSquared();
         if (d > 0.0 && e > 0.0) {
            double n = (vec3d2.x * vec3d.x + vec3d2.z * vec3d.z) / Math.sqrt(d * e);
            double o = vec3d2.x * vec3d.z - vec3d2.z * vec3d.x;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float)(Math.signum(o) * Math.acos(n))));
         }
      } else if (j > 0.0F) {
         float kx = this.player.getPitch();
         float lx = this.player.isTouchingWater() ? -90.0F - kx : -90.0F;
         float mx = MathHelper.lerp(j, 0.0F, lx);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mx));
         if (this.player.isInSwimmingPose()) {
            matrices.translate(0.0F, -1.0F, 0.3F);
         }
      }

      matrices.scale(-1.0F, -1.0F, 1.0F);
      matrices.translate(0.0F, -1.401F, 0.0F);
      matrices.scale((float)scale * 0.93F, (float)scale * 0.93F, (float)scale * 0.93F);
      this.animateModel(
         this.player,
         noLimb ? 0.0F : this.player.limbAnimator.getPos(),
         noLimb ? 0.0F : this.player.limbAnimator.getSpeed(),
         Wrapper.mc.getRenderTickCounter().getTickDelta(true)
      );
      this.setAngles(
         this.player,
         noLimb ? 0.0F : this.player.limbAnimator.getPos(),
         noLimb ? 0.0F : this.player.limbAnimator.getSpeed(),
         this.player.age,
         this.player.headYaw - this.player.bodyYaw,
         this.player.getPitch()
      );
      this.riding = this.player.hasVehicle();
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      this.getHeadParts().forEach(modelPart -> render(matrices, modelPart, fill, line, alpha, false));
      this.getBodyParts().forEach(modelPart -> render(matrices, modelPart, fill, line, alpha, false));
      matrices.pop();
      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
   }

   public static void render(MatrixStack matrices, ModelPart part, ColorSetting fill, ColorSetting line, double alpha, boolean texture) {
      if (part.visible && (!part.cuboids.isEmpty() || !part.children.isEmpty())) {
         matrices.push();
         part.rotate(matrices);

         for (Cuboid cuboid : part.cuboids) {
            render(matrices, cuboid, fill, line, alpha, texture);
         }

         for (ModelPart child : part.children.values()) {
            render(matrices, child, fill, line, alpha, texture);
         }

         matrices.pop();
      }
   }

   public static void render(MatrixStack matrices, Cuboid cuboid, ColorSetting fill, ColorSetting line, double alpha, boolean texture) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();

      for (Quad quad : cuboid.sides) {
         pos1.set(quad.vertices[0].pos.x / 16.0F, quad.vertices[0].pos.y / 16.0F, quad.vertices[0].pos.z / 16.0F, 1.0F);
         pos1.mul(matrix);
         pos2.set(quad.vertices[1].pos.x / 16.0F, quad.vertices[1].pos.y / 16.0F, quad.vertices[1].pos.z / 16.0F, 1.0F);
         pos2.mul(matrix);
         pos3.set(quad.vertices[2].pos.x / 16.0F, quad.vertices[2].pos.y / 16.0F, quad.vertices[2].pos.z / 16.0F, 1.0F);
         pos3.mul(matrix);
         pos4.set(quad.vertices[3].pos.x / 16.0F, quad.vertices[3].pos.y / 16.0F, quad.vertices[3].pos.z / 16.0F, 1.0F);
         pos4.mul(matrix);
         if (fill.booleanValue) {
            Color color = fill.getValue();
            float a = (float)(color.getAlpha() / 255.0F * alpha);
            float r = color.getRed() / 255.0F;
            float g = color.getGreen() / 255.0F;
            float b = color.getBlue() / 255.0F;
            BufferBuilder buffer = Tessellator.getInstance()
               .begin(DrawMode.QUADS, texture ? VertexFormats.POSITION_TEXTURE_COLOR : VertexFormats.POSITION_COLOR);
            buffer.vertex(pos1.x, pos1.y, pos1.z).texture(quad.vertices[0].u, quad.vertices[0].v).color(r, g, b, a);
            buffer.vertex(pos2.x, pos2.y, pos2.z).texture(quad.vertices[1].u, quad.vertices[1].v).color(r, g, b, a);
            buffer.vertex(pos2.x, pos2.y, pos2.z).texture(quad.vertices[1].u, quad.vertices[1].v).color(r, g, b, a);
            buffer.vertex(pos3.x, pos3.y, pos3.z).texture(quad.vertices[2].u, quad.vertices[2].v).color(r, g, b, a);
            buffer.vertex(pos3.x, pos3.y, pos3.z).texture(quad.vertices[2].u, quad.vertices[2].v).color(r, g, b, a);
            buffer.vertex(pos4.x, pos4.y, pos4.z).texture(quad.vertices[3].u, quad.vertices[3].v).color(r, g, b, a);
            buffer.vertex(pos1.x, pos1.y, pos1.z).texture(quad.vertices[0].u, quad.vertices[0].v).color(r, g, b, a);
            buffer.vertex(pos1.x, pos1.y, pos1.z).texture(quad.vertices[0].u, quad.vertices[0].v).color(r, g, b, a);
            Render3DUtil.endBuilding(buffer);
         }

         if (line.booleanValue) {
            Color color = line.getValue();
            float a = (float)(color.getAlpha() / 255.0F * alpha);
            float r = color.getRed() / 255.0F;
            float g = color.getGreen() / 255.0F;
            float b = color.getBlue() / 255.0F;
            BufferBuilder buffer = Tessellator.getInstance()
               .begin(DrawMode.DEBUG_LINES, texture ? VertexFormats.POSITION_TEXTURE_COLOR : VertexFormats.POSITION_COLOR);
            buffer.vertex(pos1.x, pos1.y, pos1.z).texture(quad.vertices[0].u, quad.vertices[0].v).color(r, g, b, a);
            buffer.vertex(pos2.x, pos2.y, pos2.z).texture(quad.vertices[1].u, quad.vertices[1].v).color(r, g, b, a);
            buffer.vertex(pos2.x, pos2.y, pos2.z).texture(quad.vertices[1].u, quad.vertices[1].v).color(r, g, b, a);
            buffer.vertex(pos3.x, pos3.y, pos3.z).texture(quad.vertices[2].u, quad.vertices[2].v).color(r, g, b, a);
            buffer.vertex(pos3.x, pos3.y, pos3.z).texture(quad.vertices[2].u, quad.vertices[2].v).color(r, g, b, a);
            buffer.vertex(pos4.x, pos4.y, pos4.z).texture(quad.vertices[3].u, quad.vertices[3].v).color(r, g, b, a);
            buffer.vertex(pos4.x, pos4.y, pos4.z).texture(quad.vertices[3].u, quad.vertices[3].v).color(r, g, b, a);
            buffer.vertex(pos1.x, pos1.y, pos1.z).texture(quad.vertices[0].u, quad.vertices[0].v).color(r, g, b, a);
            Render3DUtil.endBuilding(buffer);
         }
      }
   }
}
