package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.LookDirectionEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class Yaw extends Module {
   public static Yaw INSTANCE;
   private final BooleanSetting yawLock = this.add(new BooleanSetting("YawLock", true));
   private final BooleanSetting smart = this.add(new BooleanSetting("Smart", true));
   private final SliderSetting yaw = this.add(new SliderSetting("Yaw", 0.0, -180.0, 180.0, 0.1, () -> !this.smart.getValue()));
   private final BooleanSetting pitchLock = this.add(new BooleanSetting("PitchLock", true));
   private final SliderSetting pitch = this.add(new SliderSetting("Pitch", 0.0, -90.0, 90.0, 0.1));
   private final BooleanSetting lock = this.add(new BooleanSetting("Lock", true));

   public Yaw() {
      super("Yaw", Module.Category.Player);
      this.setChinese("视角锁定");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.yawLock.getValue()) {
         mc.player.setYaw(this.smart.getValue() ? Math.round((mc.player.getYaw() + 1.0F) / 45.0F) * 45.0F : this.yaw.getValueFloat());
      }

      if (this.pitchLock.getValue()) {
         mc.player.setPitch(this.pitch.getValueFloat());
      }
   }

   @EventListener
   public void onLookDirection(LookDirectionEvent event) {
      if (this.lock.getValue()) {
         event.cancel();
      }
   }
}
