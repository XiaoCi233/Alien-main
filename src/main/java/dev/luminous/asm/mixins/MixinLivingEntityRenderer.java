package dev.luminous.asm.mixins;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.render.NoRender;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> {
   @Unique
   private LivingEntity lastEntity;
   @Unique
   private float originalYaw;
   @Unique
   private float originalHeadYaw;
   @Unique
   private float originalBodyYaw;
   @Unique
   private float originalPitch;
   @Unique
   private float originalPrevYaw;
   @Unique
   private float originalPrevHeadYaw;
   @Unique
   private float originalPrevBodyYaw;

   @Inject(method = "render*", at = @At("HEAD"))
   public void onRenderPre(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
      if (MinecraftClient.getInstance().player != null && livingEntity == MinecraftClient.getInstance().player && ClientSetting.INSTANCE.rotations.getValue()) {
         this.originalYaw = livingEntity.getYaw();
         this.originalHeadYaw = livingEntity.headYaw;
         this.originalBodyYaw = livingEntity.bodyYaw;
         this.originalPitch = livingEntity.getPitch();
         this.originalPrevYaw = livingEntity.prevYaw;
         this.originalPrevHeadYaw = livingEntity.prevHeadYaw;
         this.originalPrevBodyYaw = livingEntity.prevBodyYaw;
         livingEntity.setYaw(RotationManager.getRenderYawOffset());
         livingEntity.headYaw = RotationManager.getRotationYawHead();
         livingEntity.bodyYaw = RotationManager.getRenderYawOffset();
         livingEntity.setPitch(RotationManager.getRenderPitch());
         livingEntity.prevYaw = RotationManager.getPrevRenderYawOffset();
         livingEntity.prevHeadYaw = RotationManager.getPrevRotationYawHead();
         livingEntity.prevBodyYaw = RotationManager.getPrevRenderYawOffset();
         livingEntity.prevPitch = RotationManager.getPrevRenderPitch();
      }

      this.lastEntity = livingEntity;
   }

   @Inject(method = "render*", at = @At("TAIL"))
   public void onRenderPost(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
      if (MinecraftClient.getInstance().player != null && livingEntity == MinecraftClient.getInstance().player && ClientSetting.INSTANCE.rotations.getValue()) {
         livingEntity.setYaw(this.originalYaw);
         livingEntity.headYaw = this.originalHeadYaw;
         livingEntity.bodyYaw = this.originalBodyYaw;
         livingEntity.setPitch(this.originalPitch);
         livingEntity.prevYaw = this.originalPrevYaw;
         livingEntity.prevHeadYaw = this.originalPrevHeadYaw;
         livingEntity.prevBodyYaw = this.originalPrevBodyYaw;
         livingEntity.prevPitch = this.originalPitch;
      }
   }

   @ModifyArgs(
      method = "render*",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
      )
   )
   private void renderHook(Args args) {
      float alpha = -1.0F;
      if (NoRender.INSTANCE.isOn()
         && NoRender.INSTANCE.antiPlayerCollision.getValue()
         && this.lastEntity != Wrapper.mc.player
         && this.lastEntity instanceof PlayerEntity pl
         && !pl.isInvisible()) {
         alpha = MathUtil.clamp((float)(Wrapper.mc.player.squaredDistanceTo(this.lastEntity.getPos()) / 3.0) + 0.2F, 0.0F, 1.0F);
      }

      if (alpha != -1.0F) {
         args.set(4, this.applyOpacity(654311423, alpha));
      }
   }

   @Unique
   int applyOpacity(int color_int, float opacity) {
      opacity = Math.min(1.0F, Math.max(0.0F, opacity));
      Color color = new Color(color_int);
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(color.getAlpha() * opacity)).getRGB();
   }
}
