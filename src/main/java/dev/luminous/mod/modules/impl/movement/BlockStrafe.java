package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.combat.AutoAnchor;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class BlockStrafe extends Module {
   public static BlockStrafe INSTANCE;
   private final SliderSetting speed = this.add(new SliderSetting("Speed", 10.0, 0.0, 20.0, 1.0).setSuffix("%"));
   private final SliderSetting aSpeed = this.add(new SliderSetting("AnchorSpeed", 3.0, 0.0, 20.0, 1.0).setSuffix("%"));

   public BlockStrafe() {
      super("BlockStrafe", Module.Category.Movement);
      this.setChinese("方块灵活移动");
      INSTANCE = this;
   }

   @EventListener
   public void onMove(MoveEvent event) {
      if (EntityUtil.isInsideBlock()) {
         double speed = AutoAnchor.INSTANCE.currentPos == null ? this.speed.getValue() : this.aSpeed.getValue();
         double moveSpeed = 0.002873 * speed;
         double n = mc.player.input.movementForward;
         double n2 = mc.player.input.movementSideways;
         double n3 = mc.player.getYaw();
         if (n == 0.0 && n2 == 0.0) {
            event.setX(0.0);
            event.setZ(0.0);
         } else {
            if (n != 0.0 && n2 != 0.0) {
               n *= Math.sin(Math.PI / 4);
               n2 *= Math.cos(Math.PI / 4);
            }

            event.setX(n * moveSpeed * -Math.sin(Math.toRadians(n3)) + n2 * moveSpeed * Math.cos(Math.toRadians(n3)));
            event.setZ(n * moveSpeed * Math.cos(Math.toRadians(n3)) - n2 * moveSpeed * -Math.sin(Math.toRadians(n3)));
         }
      }
   }
}
