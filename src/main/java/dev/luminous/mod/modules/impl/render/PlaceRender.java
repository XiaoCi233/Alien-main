package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.math.FadeUtils;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.HashMap;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class PlaceRender extends Module {
   public static final HashMap<BlockPos, PlaceRender.PlacePos> renderMap = new HashMap();
   public static PlaceRender INSTANCE;
   public final SliderSetting fadeTime = this.add(new SliderSetting("FadeTime", 500, 0, 3000));
   public final SliderSetting timeout = this.add(new SliderSetting("TimeOut", 500, 0, 3000));
   private final ColorSetting box = this.add(new ColorSetting("Box", new Color(255, 255, 255, 255)).injectBoolean(true));
   private final ColorSetting fill = this.add(new ColorSetting("Fill", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final ColorSetting tryPlaceBox = this.add(new ColorSetting("TryPlaceBox", new Color(178, 178, 178, 255)).injectBoolean(true));
   private final ColorSetting tryPlaceFill = this.add(new ColorSetting("TryPlaceFill", new Color(255, 119, 119, 157)).injectBoolean(true));
   private final BooleanSetting noFail = this.add(new BooleanSetting("NoFail", false));
   private final EnumSetting<Easing> ease = this.add(new EnumSetting("Ease", Easing.CubicInOut));
   private final EnumSetting<PlaceRender.Mode> mode = this.add(new EnumSetting("Mode", PlaceRender.Mode.All));

   public PlaceRender() {
      super("PlaceRender", Module.Category.Render);
      this.setChinese("放置显示");
      this.enable();
      INSTANCE = this;
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      renderMap.values().removeIf(v -> v.draw(matrixStack));
   }

   public void create(BlockPos pos) {
      renderMap.put(pos, new PlaceRender.PlacePos(pos));
   }

   private static enum Mode {
      Fade,
      Shrink,
      All;
   }

   public class PlacePos {
      public final FadeUtils fade = new FadeUtils((long)PlaceRender.this.fadeTime.getValue());
      public final BlockPos pos;
      public final Timer timer;
      public boolean isAir;

      public PlacePos(BlockPos placePos) {
         this.pos = placePos;
         this.timer = new Timer();
         this.isAir = true;
      }

      public boolean draw(MatrixStack matrixStack) {
         if (this.isAir) {
            if (!PlaceRender.this.noFail.getValue() && Wrapper.mc.world.isAir(this.pos)) {
               if (!this.timer.passedMs(PlaceRender.this.timeout.getValue())) {
                  this.fade.reset();
                  Box aBox = new Box(this.pos);
                  if (PlaceRender.this.tryPlaceFill.booleanValue) {
                     Render3DUtil.drawFill(matrixStack, aBox, PlaceRender.this.tryPlaceFill.getValue());
                  }

                  if (PlaceRender.this.tryPlaceBox.booleanValue) {
                     Render3DUtil.drawBox(matrixStack, aBox, PlaceRender.this.tryPlaceBox.getValue());
                  }
               }

               return false;
            }

            this.isAir = false;
         }

         double quads = this.fade.ease((Easing)PlaceRender.this.ease.getValue());
         if (quads == 1.0) {
            return true;
         } else {
            double alpha = PlaceRender.this.mode.getValue() != PlaceRender.Mode.Fade && PlaceRender.this.mode.getValue() != PlaceRender.Mode.All
               ? 1.0
               : 1.0 - quads;
            double size = PlaceRender.this.mode.getValue() != PlaceRender.Mode.Shrink && PlaceRender.this.mode.getValue() != PlaceRender.Mode.All ? 0.0 : quads;
            Box aBoxx = new Box(this.pos).expand(-size * 0.5, -size * 0.5, -size * 0.5);
            if (PlaceRender.this.fill.booleanValue) {
               Render3DUtil.drawFill(
                  matrixStack, aBoxx, ColorUtil.injectAlpha(PlaceRender.this.fill.getValue(), (int)(PlaceRender.this.fill.getValue().getAlpha() * alpha))
               );
            }

            if (PlaceRender.this.box.booleanValue) {
               Render3DUtil.drawBox(
                  matrixStack, aBoxx, ColorUtil.injectAlpha(PlaceRender.this.box.getValue(), (int)(PlaceRender.this.box.getValue().getAlpha() * alpha))
               );
            }

            return false;
         }
      }
   }
}
