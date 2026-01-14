package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import net.minecraft.client.input.KeyboardInput;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
   @Inject(
      method = "tick",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;sneaking:Z", shift = Shift.AFTER, opcode = Opcodes.PUTFIELD),
      cancellable = true
   )
   private void keyInput(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
      KeyboardInputEvent event = KeyboardInputEvent.get();
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}
