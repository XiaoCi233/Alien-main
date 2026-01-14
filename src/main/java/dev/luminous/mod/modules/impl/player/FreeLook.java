package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.LookDirectionEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import net.minecraft.client.util.math.MatrixStack;

public class FreeLook extends Module {
   public static FreeLook INSTANCE;
   private float fakeYaw;
   private float fakePitch;
   private float prevFakeYaw;
   private float prevFakePitch;

   public FreeLook() {
      super("FreeLook", Module.Category.Player);
      this.setChinese("自由视角");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         this.fakePitch = mc.player.getPitch();
         this.fakeYaw = mc.player.getYaw();
         this.prevFakePitch = this.fakePitch;
         this.prevFakeYaw = this.fakeYaw;
      }
   }

   @EventListener
   public void onLookDirection(LookDirectionEvent event) {
      this.fakeYaw = this.fakeYaw + (float)event.getCursorDeltaX() * 0.15F;
      this.fakePitch = this.fakePitch + (float)event.getCursorDeltaY() * 0.15F;
      event.cancel();
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      this.prevFakeYaw = this.fakeYaw;
      this.prevFakePitch = this.fakePitch;
   }

   public float getFakeYaw() {
      return MathUtil.interpolate(this.prevFakeYaw, this.fakeYaw, mc.getRenderTickCounter().getTickDelta(true));
   }

   public float getFakePitch() {
      return MathUtil.interpolate(this.prevFakePitch, this.fakePitch, mc.getRenderTickCounter().getTickDelta(true));
   }
}
