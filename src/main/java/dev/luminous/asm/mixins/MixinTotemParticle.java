package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.TotemParticleEvent;
import java.awt.Color;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TotemParticle.class)
public abstract class MixinTotemParticle extends MixinParticle {
   @Inject(method = "<init>", at = @At("TAIL"))
   private void hookInit(
      ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider, CallbackInfo ci
   ) {
      TotemParticleEvent event = TotemParticleEvent.get(velocityX, velocityY, velocityZ);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         this.velocityX = event.velocityX;
         this.velocityY = event.velocityY;
         this.velocityZ = event.velocityZ;
         Color color = event.color;
         if (color != null) {
            this.setColor(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
            this.setAlpha(color.getAlpha() / 255.0F);
         }
      }
   }
}
