package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.interfaces.IMouseHook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse implements IMouseHook {
   @Shadow
   private boolean cursorLocked;
   @Final
   @Shadow
   private MinecraftClient client;
   @Shadow
   private double x;
   @Shadow
   private double y;

   @Inject(method = "onMouseButton", at = @At("HEAD"))
   private void onMouse(long window, int button, int action, int mods, CallbackInfo ci) {
      int key = -(button + 2);
      if (action == 1) {
         Alien.MODULE.onKeyPressed(key);
      }

      if (action == 0) {
         Alien.MODULE.onKeyReleased(key);
      }
   }

   @Override
   public void alienClient$lock() {
      if (this.client.isWindowFocused() && !this.cursorLocked) {
         if (!MinecraftClient.IS_SYSTEM_MAC) {
            KeyBinding.updatePressedStates();
         }

         this.cursorLocked = true;
         this.x = this.client.getWindow().getWidth() / 2.0;
         this.y = this.client.getWindow().getHeight() / 2.0;
         InputUtil.setCursorParameters(this.client.getWindow().getHandle(), 212995, this.x, this.y);
      }
   }
}
