package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.Ambience;
import java.awt.Color;
import net.minecraft.client.render.DimensionEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionEffects.class)
public class MixinDimensionEffects {
   @Inject(method = "getFogColorOverride", at = @At("HEAD"), cancellable = true)
   private void hookGetFogColorOverride(float skyAngle, float tickDelta, CallbackInfoReturnable<float[]> cir) {
      if (Ambience.INSTANCE.isOn() && Ambience.INSTANCE.dimensionColor.booleanValue) {
         Color color = Ambience.INSTANCE.dimensionColor.getValue();
         cir.setReturnValue(new float[]{color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 1.0F});
      }
   }
}
