package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.DeathEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.StringSetting;

public class AutoKit extends Module {
   public static AutoKit INSTANCE;
   final StringSetting command = this.add(new StringSetting("Command", "kit 1"));
   boolean kit = false;
   final Timer timer = new Timer();

   public AutoKit() {
      super("AutoKit", Module.Category.Misc);
      this.setChinese("自动配装命令");
      INSTANCE = this;
   }

   @Override
   public void onLogin() {
      this.kit = true;
      this.timer.reset();
   }

   @EventListener
   public void onDeath(DeathEvent event) {
      if (event.getPlayer() == mc.player) {
         this.kit = true;
         this.timer.reset();
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.kit && this.timer.passedS(2.0)) {
         this.kit = false;
         mc.player.networkHandler.sendCommand(this.command.getValue());
      }
   }
}
