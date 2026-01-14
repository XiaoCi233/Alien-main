package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.TimerEvent;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.RenderTickCounter.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.Dynamic.class)
public class MixinRenderTickCounter {
   @Shadow
   private float lastFrameDuration;
   @Shadow
   private float tickDelta;
   @Shadow
   private long prevTimeMillis;
   @Final
   @Shadow
   private float tickTime;

   @Inject(method = "beginRenderTick(J)I", at = @At("HEAD"), cancellable = true)
   private void beginRenderTickHook(long timeMillis, CallbackInfoReturnable<Integer> cir) {
      TimerEvent event = TimerEvent.getEvent();
      Alien.EVENT_BUS.post(event);
      if (!event.isCancelled()) {
         float timer;
         if (event.isModified()) {
            timer = event.get();
         } else {
            timer = Alien.TIMER.get();
         }

         if (timer == 1.0F) {
            return;
         }

         this.lastFrameDuration = (float)(timeMillis - this.prevTimeMillis) / this.tickTime * timer;
         this.prevTimeMillis = timeMillis;
         this.tickDelta = this.tickDelta + this.lastFrameDuration;
         int i = (int)this.tickDelta;
         this.tickDelta -= i;
         cir.setReturnValue(i);
      }
   }
}
