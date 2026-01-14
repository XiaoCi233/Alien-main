package dev.luminous.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.render.ModelPlayer;
import dev.luminous.mod.modules.impl.render.Chams;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class MixinEndCrystalEntityRenderer extends EntityRenderer<EndCrystalEntity> {
   @Mutable
   @Final
   @Shadow
   private static RenderLayer END_CRYSTAL;
   @Shadow
   @Final
   private static Identifier TEXTURE;
   @Unique
   private static final Identifier BLANK = Identifier.of("textures/blank.png");
   @Unique
   private static final RenderLayer END_CRYSTAL_BLANK = RenderLayer.getEntityTranslucent(BLANK);
   @Unique
   private static final RenderLayer END_CRYSTAL_CUSTOM = RenderLayer.getEntityTranslucent(TEXTURE);
   @Final
   @Shadow
   private static float SINE_45_DEGREES;
   @Final
   @Shadow
   private ModelPart core;
   @Final
   @Shadow
   private ModelPart frame;
   @Final
   @Shadow
   private ModelPart bottom;

   protected MixinEndCrystalEntityRenderer(Context ctx) {
      super(ctx);
   }

   @Unique
   private float yOffset(int age, float tickDelta, Chams module) {
      float f = (age + tickDelta) * module.floatValue.getValueFloat();
      float g = MathHelper.sin(f * 0.2F) / 2.0F + 0.5F;
      g = (g * g + g) * 0.4F * module.bounceHeight.getValueFloat();
      return g - 1.4F + module.floatOffset.getValueFloat();
   }

   @Inject(
      method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
      at = @At("HEAD"),
      cancellable = true
   )
   public void render(
      EndCrystalEntity endCrystalEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci
   ) {
      Chams module = Chams.INSTANCE;
      if (module.customCrystal()) {
         ci.cancel();
         int age = module.spinSync.getValue() ? module.age : endCrystalEntity.endCrystalAge;
         float h = this.yOffset(age, g, module);
         float j = (age + g) * 3.0F * module.spinValue.getValueFloat();
         matrixStack.push();
         if (module.custom.getValue()) {
            ShaderProgram s = RenderSystem.getShader();
            if (module.depth.getValue()) {
               RenderSystem.enableDepthTest();
            }

            RenderSystem.enableBlend();
            if (module.chamsTexture.getValue()) {
               RenderSystem.setShaderTexture(0, TEXTURE);
               RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            } else {
               RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            }

            matrixStack.push();
            matrixStack.scale(2.0F * module.scale.getValueFloat(), 2.0F * module.scale.getValueFloat(), 2.0F * module.scale.getValueFloat());
            matrixStack.translate(0.0F, -0.5F, 0.0F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
            matrixStack.translate(0.0F, 1.5F + h / 2.0F, 0.0F);
            matrixStack.multiply(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
            if (module.outerFrame.booleanValue) {
               ModelPlayer.render(matrixStack, this.frame, module.fill, module.line, 1.0, module.chamsTexture.getValue());
            }

            matrixStack.scale(0.875F, 0.875F, 0.875F);
            matrixStack.multiply(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
            if (module.innerFrame.booleanValue) {
               ModelPlayer.render(matrixStack, this.frame, module.fill, module.line, 1.0, module.chamsTexture.getValue());
            }

            matrixStack.scale(0.875F, 0.875F, 0.875F);
            matrixStack.multiply(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
            if (module.core.booleanValue) {
               ModelPlayer.render(matrixStack, this.core, module.fill, module.line, 1.0, module.chamsTexture.getValue());
            }

            matrixStack.pop();
            RenderSystem.setShader(() -> s);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
         }

         VertexConsumer vertexConsumer = ItemRenderer.getDirectItemGlintConsumer(
            vertexConsumerProvider, module.texture.getValue() ? END_CRYSTAL_CUSTOM : END_CRYSTAL_BLANK, false, module.glint.getValue()
         );
         matrixStack.push();
         matrixStack.scale(2.0F * module.scale.getValueFloat(), 2.0F * module.scale.getValueFloat(), 2.0F * module.scale.getValueFloat());
         matrixStack.translate(0.0F, -0.5F, 0.0F);
         int k = OverlayTexture.DEFAULT_UV;
         if (endCrystalEntity.shouldShowBottom()) {
            this.bottom.render(matrixStack, vertexConsumer, i, k);
         }

         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
         matrixStack.translate(0.0F, 1.5F + h / 2.0F, 0.0F);
         matrixStack.multiply(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
         if (module.outerFrame.booleanValue) {
            this.frame.render(matrixStack, vertexConsumer, i, k, module.outerFrame.getValue().getRGB());
         }

         matrixStack.scale(0.875F, 0.875F, 0.875F);
         matrixStack.multiply(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
         if (module.innerFrame.booleanValue) {
            this.frame.render(matrixStack, vertexConsumer, i, k, module.innerFrame.getValue().getRGB());
         }

         matrixStack.scale(0.875F, 0.875F, 0.875F);
         matrixStack.multiply(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
         if (module.core.booleanValue) {
            this.core.render(matrixStack, vertexConsumer, i, k, module.core.getValue().getRGB());
         }

         matrixStack.pop();
         matrixStack.pop();
         BlockPos blockPos = endCrystalEntity.getBeamTarget();
         if (blockPos != null) {
            float m = blockPos.getX() + 0.5F;
            float n = blockPos.getY() + 0.5F;
            float o = blockPos.getZ() + 0.5F;
            float p = (float)(m - endCrystalEntity.getX());
            float q = (float)(n - endCrystalEntity.getY());
            float r = (float)(o - endCrystalEntity.getZ());
            matrixStack.translate(p, q, r);
            EnderDragonEntityRenderer.renderCrystalBeam(-p, -q + h, -r, g, endCrystalEntity.endCrystalAge, matrixStack, vertexConsumerProvider, i);
         }

         super.render(endCrystalEntity, f, g, matrixStack, vertexConsumerProvider, i);
      }
   }
}
