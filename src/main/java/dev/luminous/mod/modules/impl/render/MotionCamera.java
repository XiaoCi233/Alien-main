package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class MotionCamera extends Module {
   public static MotionCamera INSTANCE;
   public final BooleanSetting noFirstPerson = this.add(new BooleanSetting("NoFirstPerson", true));
   public final SliderSetting firstPersonSpeed = this.add(new SliderSetting("FirstPersonSpeed", 0.6, 0.0, 1.0, 0.01));
   public final SliderSetting speed = this.add(new SliderSetting("Speed", 0.3, 0.0, 1.0, 0.01));
   private double fakeX;
   private double fakeY;
   private double fakeZ;
   private double prevFakeX;
   private double prevFakeY;
   private double prevFakeZ;

   public MotionCamera() {
      super("MotionCamera", Module.Category.Render);
      INSTANCE = this;
      this.setChinese("运动相机");
   }

   public boolean on() {
      return this.isOn() && (!this.noFirstPerson.getValue() || !mc.options.getPerspective().isFirstPerson());
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         this.fakeX = mc.player.getX();
         this.fakeY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
         this.fakeZ = mc.player.getZ();
         this.prevFakeX = this.fakeX;
         this.prevFakeY = this.fakeY;
         this.prevFakeZ = this.fakeZ;
      }
   }

   @EventListener
   public void onUpdate(ClientTickEvent event) {
      if (!event.isPre() && !nullCheck()) {
         this.prevFakeX = this.fakeX;
         this.prevFakeY = this.fakeY;
         this.prevFakeZ = this.fakeZ;
         double speed = mc.options.getPerspective().isFirstPerson() ? this.firstPersonSpeed.getValue() : this.speed.getValue();
         this.fakeX = AnimateUtil.animate(this.fakeX, mc.player.getX(), speed);
         this.fakeY = AnimateUtil.animate(this.fakeY, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), speed);
         this.fakeZ = AnimateUtil.animate(this.fakeZ, mc.player.getZ(), speed);
      }
   }

   public double getFakeX() {
      return MathUtil.interpolate(this.prevFakeX, this.fakeX, (double)mc.getRenderTickCounter().getTickDelta(true));
   }

   public double getFakeY() {
      return MathUtil.interpolate(this.prevFakeY, this.fakeY, (double)mc.getRenderTickCounter().getTickDelta(true));
   }

   public double getFakeZ() {
      return MathUtil.interpolate(this.prevFakeZ, this.fakeZ, (double)mc.getRenderTickCounter().getTickDelta(true));
   }
}
