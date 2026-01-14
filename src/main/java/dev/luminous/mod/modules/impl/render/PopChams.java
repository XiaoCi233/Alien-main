package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.utils.entity.CopyPlayerEntity;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.ModelPlayer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

public class PopChams extends Module {
   public static PopChams INSTANCE;
   public final EnumSetting<Easing> ease = this.add(new EnumSetting("Ease", Easing.CubicInOut));
   private final ColorSetting fill = this.add(new ColorSetting("Fill", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final ColorSetting line = this.add(new ColorSetting("Line", new Color(255, 255, 255, 100)).injectBoolean(true));
   final CopyOnWriteArrayList<PopChams.Person> popList = new CopyOnWriteArrayList();
   private final BooleanSetting alpha = this.add(new BooleanSetting("Alpha", true));
   private final BooleanSetting forceSneak = this.add(new BooleanSetting("ForceSneak", false));
   private final BooleanSetting noSelf = this.add(new BooleanSetting("NoSelf", true));
   private final BooleanSetting noLimb = this.add(new BooleanSetting("NoLimb", true));
   private final SliderSetting fadeTime = this.add(new SliderSetting("FadeTime", 300, 0, 1000));
   private final SliderSetting yOffset = this.add(new SliderSetting("YOffset", 0.0, -10.0, 10.0, 0.01));
   private final SliderSetting scale = this.add(new SliderSetting("Scale", 1.0, 0.0, 2.0, 0.01));
   private final SliderSetting yaw = this.add(new SliderSetting("Yaw", 0.0, 0.0, 720.0, 0.01));

   public PopChams() {
      super("PopChams", Module.Category.Render);
      this.setChinese("爆图腾上色");
      INSTANCE = this;
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      RenderSystem.depthMask(false);
      this.popList.removeIf(person -> person.render(matrixStack));
      RenderSystem.depthMask(true);
   }

   @EventListener
   private void onTotemPop(TotemEvent event) {
      if (!this.noSelf.getValue() || !event.getPlayer().equals(mc.player)) {
         this.popList.add(new PopChams.Person(new CopyPlayerEntity(event.getPlayer())));
      }
   }

   private class Person {
      public final ModelPlayer modelPlayer;
      final Animation animation;

      public Person(PlayerEntity player) {
         this.modelPlayer = new ModelPlayer(player);
         this.animation = new Animation();
      }

      public boolean render(MatrixStack matrixStack) {
         double animation = this.animation.get(1.0, PopChams.this.fadeTime.getValueInt(), (Easing)PopChams.this.ease.getValue());
         if (animation >= 1.0) {
            return true;
         } else {
            this.modelPlayer
               .render(
                  matrixStack,
                  PopChams.this.fill,
                  PopChams.this.line,
                  PopChams.this.alpha.getValue() ? 1.0 - animation : 1.0,
                  PopChams.this.yOffset.getValue() * animation,
                  1.0 + (PopChams.this.scale.getValue() - 1.0) * animation,
                  PopChams.this.yaw.getValue() * animation,
                  PopChams.this.noLimb.getValue(),
                  PopChams.this.forceSneak.getValue()
               );
            return false;
         }
      }
   }
}
