package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.combat.SelfTrap;
import dev.luminous.mod.modules.impl.combat.Surround;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;

public class Step extends Module {
   private final EnumSetting<Step.Mode> mode = this.add(new EnumSetting("Mode", Step.Mode.Vanilla));
   private final SliderSetting height = this.add(new SliderSetting("Height", 1.0, 0.0, 5.0, 0.5));
   private final BooleanSetting useTimer = this.add(
      new BooleanSetting("Timer", true, () -> this.mode.getValue() == Step.Mode.OldNCP || this.mode.getValue() == Step.Mode.NCP)
   );
   private final BooleanSetting fast = this.add(new BooleanSetting("Fast", true, () -> this.mode.getValue() == Step.Mode.NCP && this.useTimer.getValue()));
   private final BooleanSetting onlyMoving = this.add(new BooleanSetting("OnlyMoving", true));
   private final BooleanSetting surroundPause = this.add(new BooleanSetting("SurroundPause", true));
   private final BooleanSetting inWebPause = this.add(new BooleanSetting("InWebPause", true));
   private final BooleanSetting inBlockPause = this.add(new BooleanSetting("InBlockPause", true));
   private final BooleanSetting sneakingPause = this.add(new BooleanSetting("SneakingPause", true));
   private final BooleanSetting pathingPause = this.add(new BooleanSetting("PathingPause", true));
   boolean timer;
   int packets = 0;

   public Step() {
      super("Step", "Steps up blocks.", Module.Category.Movement);
      this.setChinese("步行辅助");
   }

   public static void setStepHeight(float v) {
      mc.player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT).setBaseValue(v);
   }

   @Override
   public void onDisable() {
      if (!nullCheck()) {
         setStepHeight(0.6F);
         Alien.TIMER.reset();
      }
   }

   @Override
   public String getInfo() {
      return ((Step.Mode)this.mode.getValue()).name();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if ((!this.pathingPause.getValue() || !BaritoneUtil.isActive())
         && (!this.sneakingPause.getValue() || !mc.player.isSneaking())
         && (!this.inBlockPause.getValue() || !EntityUtil.isInsideBlock())
         && !mc.player.isInLava()
         && !mc.player.isTouchingWater()
         && (!this.inWebPause.getValue() || !Alien.PLAYER.isInWeb(mc.player))
         && mc.player.isOnGround()
         && (!this.onlyMoving.getValue() || MovementUtil.isMoving())
         && (!this.surroundPause.getValue() || !Surround.INSTANCE.isOn() && !SelfTrap.INSTANCE.isOn())) {
         setStepHeight(this.height.getValueFloat());
      } else {
         setStepHeight(0.6F);
      }
   }

   @EventListener
   public void onStep(TickEvent event) {
      if (event.isPost()) {
         this.packets--;
      } else {
         if (this.timer && this.packets <= 0) {
            Alien.TIMER.reset();
            this.timer = false;
         }

         boolean strict = this.mode.getValue() == Step.Mode.NCP;
         if (((Step.Mode)this.mode.getValue()).equals(Step.Mode.OldNCP) || strict) {
            double stepHeight = mc.player.getY() - mc.player.prevY;
            if (stepHeight <= 0.75 || stepHeight > this.height.getValue()) {
               return;
            }

            double[] offsets = this.getOffset(stepHeight);
            if (offsets != null && offsets.length > 1) {
               if (this.useTimer.getValue()) {
                  Alien.TIMER.set((float)this.getTimer(stepHeight));
                  this.timer = true;
                  this.packets = 2;
               }

               for (double offset : offsets) {
                  mc.getNetworkHandler()
                     .sendPacket(new PositionAndOnGround(mc.player.prevX, mc.player.prevY + offset, mc.player.prevZ, false));
               }
            }
         }
      }
   }

   public double getTimer(double height) {
      if (height > 0.6 && height <= 1.0) {
         return !this.fast.getValue() && this.mode.getValue() == Step.Mode.NCP ? 0.3333333333333333 : 0.5;
      } else {
         double[] offsets = this.getOffset(height);
         return offsets == null ? 1.0 : 1.0 / offsets.length;
      }
   }

   public double[] getOffset(double height) {
      boolean strict = this.mode.getValue() == Step.Mode.NCP;
      if (height == 0.75) {
         return strict ? new double[]{0.42, 0.753, 0.75} : new double[]{0.42, 0.753};
      } else if (height == 0.8125) {
         return strict ? new double[]{0.39, 0.7, 0.8125} : new double[]{0.39, 0.7};
      } else if (height == 0.875) {
         return strict ? new double[]{0.39, 0.7, 0.875} : new double[]{0.39, 0.7};
      } else if (height == 1.0) {
         return strict ? new double[]{0.42, 0.753, 1.0} : new double[]{0.42, 0.753};
      } else if (height == 1.5) {
         return new double[]{0.42, 0.75, 1.0, 1.16, 1.23, 1.2};
      } else if (height == 2.0) {
         return new double[]{0.42, 0.78, 0.63, 0.51, 0.9, 1.21, 1.45, 1.43};
      } else {
         return height == 2.5 ? new double[]{0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.907} : null;
      }
   }

   public static enum Mode {
      Vanilla,
      OldNCP,
      NCP;
   }
}
