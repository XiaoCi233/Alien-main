package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.asm.accessors.IScreen;
import dev.luminous.core.impl.FontManager;
import dev.luminous.core.impl.ShaderManager;
import dev.luminous.core.impl.ShaderManager.Shader;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {
   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void vitalityBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      float w = context.getScaledWindowWidth();
      float h = context.getScaledWindowHeight();
      MatrixStack m = context.getMatrices();
      long now = System.currentTimeMillis();
      float tNorm = (float)(now % 8000L) / 8000.0F;
      Color blue = new Color(28, 60, 110, 255);
      Color purple = new Color(190, 50, 160, 255);
      float mix = (float)((Math.sin(tNorm * Math.PI * 2.0) + 1.0) * 0.5);
      int topR = (int)(blue.getRed() * (1.0F - mix) + purple.getRed() * mix);
      int topG = (int)(blue.getGreen() * (1.0F - mix) + purple.getGreen() * mix);
      int topB = (int)(blue.getBlue() * (1.0F - mix) + purple.getBlue() * mix);
      int botR = (int)(purple.getRed() * (1.0F - mix) + blue.getRed() * mix);
      int botG = (int)(purple.getGreen() * (1.0F - mix) + blue.getGreen() * mix);
      int botB = (int)(purple.getBlue() * (1.0F - mix) + blue.getBlue() * mix);
      Color g1 = new Color(topR, topG, topB, 255);
      Color g2 = new Color(botR, botG, botB, 255);
      Render2DUtil.verticalGradient(m, 0.0F, 0.0F, w, h, g1, g2);
      context.fillGradient(0, 0, (int)w, (int)h, g1.getRGB(), g2.getRGB());
      float phase = (float)(now % 7000L) / 7000.0F;
      float angle = 0.523599F;
      float dx = (float)Math.tan(angle) * h;
      float base = -h;
      float spacing = 68.0F;
      float shift = phase * spacing * 3.1F;
      Color c1 = new Color(255, 150, 240, 26);
      Color c2 = new Color(120, 220, 255, 20);

      for (float i = base; i < w; i += spacing) {
         float x0 = i + shift;
         Render2DUtil.drawLine(m, x0, 0.0F, x0 + dx, h, c1.getRGB());
      }

      for (float i = base + spacing / 2.0F; i < w; i += spacing) {
         float x0 = i + shift * 0.85F;
         Render2DUtil.drawLine(m, x0, 0.0F, x0 + dx, h, c2.getRGB());
      }

      float pulse = (float)Math.sin((float)(now % 4000L) / 4000.0F * Math.PI * 2.0) * 0.5F + 0.5F;
      float radius = Math.min(w, h) * (0.12F + 0.08F * pulse);
      Color ring = new Color(255, 255, 255, 30);
      Render2DUtil.drawCircle(m, w / 2.0F, h / 2.0F, radius, ring, 80);
      Render2DUtil.drawCircle(m, w / 2.0F, h / 2.0F, radius * 1.2F, new Color(120, 220, 255, 24), 80);
      int dots = 22;

      for (int i = 0; i < dots; i++) {
         float a = (float)((Math.PI * 2) * i / dots + phase * 2.0F * Math.PI);
         float rx = (float)Math.cos(a) * (radius * 1.2F);
         float ry = (float)Math.sin(a) * (radius * 0.7F);
         float px = w / 2.0F + rx;
         float py = h / 2.0F + ry + (float)Math.sin(a * 2.0F + phase * 4.0F) * 6.0F;
         Render2DUtil.drawCircle(m, px, py, 1.5F, new Color(255, 255, 255, 38), 30);
      }

      for (int i = 0; i < 12; i++) {
         float a = (float)((Math.PI * 2) * i / 12.0 + phase * 3.1F);
         float rx = (float)Math.cos(a) * (radius * 1.6F);
         float ry = (float)Math.sin(a) * (radius * 1.0F);
         float px = w / 2.0F + rx;
         float py = h / 2.0F + ry;
         Render2DUtil.drawCircle(m, px, py, 2.4F, new Color(120, 220, 255, 36), 36);
      }

      for (int y = 0; y < h; y += 3) {
         int alpha = 18;
         int c = new Color(0, 0, 0, alpha).getRGB();
         Render2DUtil.drawLine(m, 0.0F, y, w, y, c);
      }

      if (!Alien.SHADER.fullNullCheck()) {
         ManagedShaderEffect gradient = Alien.SHADER.getShader(ShaderManager.Shader.Gradient);
         gradient.setUniformValue("alpha2", 0.36F);
         gradient.setUniformValue("rgb", 0.1F, 0.75F, 1.0F);
         gradient.setUniformValue("rgb1", 0.98F, 0.35F, 0.74F);
         gradient.setUniformValue("rgb2", 0.46F, 0.19F, 0.81F);
         gradient.setUniformValue("rgb3", 0.12F, 0.5F, 0.95F);
         gradient.setUniformValue("step", 99.0F);
         gradient.setUniformValue("radius", 1.6F);
         gradient.setUniformValue("quality", 0.8F);
         gradient.setUniformValue("divider", 220.0F);
         gradient.setUniformValue("maxSample", 6.0F);
         gradient.setUniformValue("resolution", w, h);
         float t = (float)(now % 100000L) / 1000.0F;
         gradient.setUniformValue("time", t * 220.0F);
         gradient.render(MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true));
      }

      m.push();
      float titleScale = 3.6F;
      m.translate(w / 2.0F, h * 0.24F, 0.0F);
      m.scale(titleScale, titleScale, 1.0F);
      FontManager.ui.drawCenteredString(m, "Alien", 0.0, 0.0, new Color(230, 255, 255, 255));
      FontManager.ui.drawCenteredString(m, "Alien", 0.0, 2.2F, new Color(120, 220, 255, 180));
      FontManager.ui.drawCenteredString(m, "Alien", 0.0, -2.2F, new Color(255, 160, 240, 160));
      m.pop();
      if (MinecraftClient.getInstance().currentScreen instanceof TitleScreen ts) {
         int idx = 0;

         for (Drawable d : ((IScreen)IScreen.class.cast(ts)).getDrawables()) {
            if (d instanceof ButtonWidget bw && idx < 3) {
               int bx = bw.getX();
               int by = bw.getY();
               int bwid = bw.getWidth();
               int bhei = bw.getHeight();
               boolean hovered = mouseX >= bx && mouseX <= bx + bwid && mouseY >= by && mouseY <= by + bhei;
               boolean pressed = hovered && GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), 0) == 1;
               Color accent = new Color(0, 120, 212, 255);
               Color neon = new Color(0, 224, 255, 180);
               Color amber = new Color(255, 210, 0, 160);
               Render2DUtil.drawRoundedStroke(
                  context.getMatrices(), bx, by, bwid, bhei, 5.0F, hovered ? new Color(220, 224, 230, 200) : new Color(220, 224, 230, 160), 64
               );
               Render2DUtil.drawLine(context.getMatrices(), bx + 2.0F, by + 2.0F, bx + bwid - 2.0F, by + 2.0F, new Color(255, 255, 255, 80).getRGB());
               Render2DUtil.drawLine(
                  context.getMatrices(), bx + 2.0F, by + bhei - 2.2F, bx + bwid - 2.0F, by + bhei - 2.2F, new Color(120, 130, 140, 60).getRGB()
               );
               Render2DUtil.drawLine(context.getMatrices(), bx + 4.0F, by + 4.0F, bx + 18.0F, by + 10.0F, neon.getRGB());
               Render2DUtil.drawLine(context.getMatrices(), bx + bwid - 18.0F, by + bhei - 6.0F, bx + bwid - 6.0F, by + bhei - 2.0F, amber.getRGB());
               if (pressed) {
                  Render2DUtil.drawGlow(context.getMatrices(), bx - 2.0F, by - 2.0F, bwid + 4.0F, bhei + 4.0F, new Color(0, 0, 0, 18).getRGB());
                  Render2DUtil.verticalGradient(
                     context.getMatrices(), bx + 1.5F, by + 1.5F, bx + bwid - 1.5F, by + bhei - 1.5F, new Color(255, 255, 255, 50), new Color(0, 0, 0, 46)
                  );
                  Render2DUtil.drawRoundedStroke(
                     context.getMatrices(),
                     bx + 1.0F,
                     by + 1.0F,
                     bwid - 2.0F,
                     bhei - 2.0F,
                     4.2F,
                     new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160),
                     96
                  );
                  Render2DUtil.drawRoundedStroke(context.getMatrices(), bx + 1.6F, by + 1.6F, bwid - 3.2F, bhei - 3.2F, 3.8F, new Color(255, 255, 255, 140), 96);
                  Render2DUtil.drawRoundedStroke(context.getMatrices(), bx + 2.0F, by + bhei - 2.8F, bwid - 4.0F, 1.6F, 2.0F, new Color(90, 100, 110, 100), 96);
                  by = (int)(by + 1.2F);
                  bx = (int)(bx + 0.6F);
               } else if (hovered) {
                  Render2DUtil.drawRoundedStroke(
                     context.getMatrices(),
                     bx - 0.5F,
                     by - 0.5F,
                     bwid + 1.0F,
                     bhei + 1.0F,
                     5.4F,
                     new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160),
                     96
                  );
               }

               d.render(context, mouseX, mouseY, delta);
            }

            if (d instanceof ButtonWidget) {
               idx++;
            }

            if (!(d instanceof ButtonWidget)) {
               d.render(context, mouseX, mouseY, delta);
            } else if (idx >= 3) {
               d.render(context, mouseX, mouseY, delta);
            }
         }
      }

      ci.cancel();
   }

   @Inject(method = "init()V", at = @At("TAIL"))
   private void vitalityLayout(CallbackInfo ci) {
      TitleScreen self = (TitleScreen)(Object)this;
      int w = self.width;
      int h = self.height;
      int btnW = Math.min(300, (int)(w * 0.42));
      int btnH = 24;
      int startY = (int)(h * 0.5);
      int spacing = 8 + btnH;
      int xLeft = (int)(w * 0.26) - btnW / 2;
      int xRight = (int)(w * 0.74) - btnW / 2;
      int xCenter = w / 2 - btnW / 2;
      int idx = 0;

      for (Drawable d : ((IScreen)IScreen.class.cast(self)).getDrawables()) {
         if (d instanceof ButtonWidget bw) {
            int col = idx % 2;
            int row = idx / 2;
            if (idx == 0) {
               bw.setWidth(btnW);
               bw.setPosition(xLeft, startY);
            } else if (idx == 1) {
               bw.setWidth(btnW);
               bw.setPosition(xRight, startY);
            } else if (idx == 2) {
               bw.setWidth(btnW);
               bw.setPosition(xCenter, startY + spacing);
            } else {
               int x = col == 0 ? xLeft : xRight;
               bw.setPosition(x, startY + spacing * row);
            }

            idx++;
         }
      }
   }
}
