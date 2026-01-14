package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.NoRender;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public class MixinArmorFeatureRenderer<T extends LivingEntity, A extends BipedEntityModel<T>> {
   @Inject(method = "renderArmorParts", at = @At("HEAD"), cancellable = true)
   private void onRenderArmorParts(
      MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, A model, int i, Identifier identifier, CallbackInfo ci
   ) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.armorParts.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderTrim", at = @At("HEAD"), cancellable = true)
   private void onRenderArmorTrim(
      RegistryEntry<ArmorMaterial> armorMaterial,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      ArmorTrim trim,
      A model,
      boolean leggings,
      CallbackInfo ci
   ) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.armorTrim.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderGlint", at = @At("HEAD"), cancellable = true)
   private void onRenderArmorGlint(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, A model, CallbackInfo ci) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.armorGlint.getValue()) {
         ci.cancel();
      }
   }
}
