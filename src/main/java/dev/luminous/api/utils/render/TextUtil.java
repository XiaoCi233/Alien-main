package dev.luminous.api.utils.render;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.core.impl.FontManager;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

public class TextUtil implements Wrapper {
   public static final Matrix4f lastProjMat = new Matrix4f();
   public static final Matrix4f lastModMat = new Matrix4f();
   public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();

   public static float getWidth(String s) {
      return mc.textRenderer.getWidth(s);
   }

   public static float getHeight() {
      return 9.0F;
   }

   public static void drawStringWithScale(DrawContext drawContext, String text, float x, float y, int color, float scale) {
      MatrixStack matrixStack = drawContext.getMatrices();
      if (scale != 1.0F) {
         matrixStack.push();
         matrixStack.scale(scale, scale, 1.0F);
         if (scale > 1.0F) {
            matrixStack.translate(-x / scale, -y / scale, 0.0F);
         } else {
            matrixStack.translate(x / scale / 2.0F, y / scale / 2.0F, 0.0F);
         }
      }

      drawString(drawContext, text, x, y, color);
      matrixStack.pop();
   }

   public static void drawStringScale(DrawContext drawContext, String text, float x, float y, int color, float scale, boolean shadow) {
      MatrixStack matrixStack = drawContext.getMatrices();
      if (scale != 1.0F) {
         matrixStack.push();
         matrixStack.scale(scale, scale, 1.0F);
         if (scale > 1.0F) {
            matrixStack.translate(-x / scale, -y / scale, 0.0F);
         } else {
            matrixStack.translate(x / scale / 2.0F, y / scale / 2.0F, 0.0F);
         }
      }

      drawContext.drawText(mc.textRenderer, text, (int)x, (int)y, color, shadow);
      matrixStack.pop();
   }

   public static void drawString(DrawContext drawContext, String text, double x, double y, int color) {
      drawString(drawContext, text, x, y, color, false);
   }

   public static void drawString(DrawContext drawContext, String text, double x, double y, int color, boolean customFont) {
      drawString(drawContext, text, x, y, color, customFont, true);
   }

   public static void drawString(DrawContext drawContext, String text, double x, double y, int color, boolean customFont, boolean shadow) {
      if (customFont) {
         FontManager.ui.drawString(drawContext.getMatrices(), text, (int)x, (int)y, color, shadow);
      } else {
         drawContext.drawText(mc.textRenderer, text, (int)x, (int)y, color, shadow);
      }
   }

   public static void drawStringPulse(
      DrawContext drawContext, String text, double x, double y, Color startColor, Color endColor, double speed, int counter, boolean customFont
   ) {
      char[] stringToCharArray = text.toCharArray();
      int index = 0;
      boolean color = false;
      String s = null;

      for (char c : stringToCharArray) {
         if (c == 167) {
            color = true;
         } else if (color) {
            if (c == 'r') {
               s = null;
            } else {
               s = "§" + c;
            }

            color = false;
         } else {
            index++;
            if (s != null) {
               drawString(drawContext, s + c, x, y, startColor.getRGB(), customFont);
            } else {
               drawString(drawContext, String.valueOf(c), x, y, ColorUtil.pulseColor(startColor, endColor, index, counter, speed).getRGB(), customFont);
            }

            x += customFont ? FontManager.ui.getWidth(String.valueOf(c)) : mc.textRenderer.getWidth(String.valueOf(c));
         }
      }
   }

   public static void drawStringPulse(
      DrawContext drawContext, String text, double x, double y, Color startColor, Color endColor, double speed, int counter, boolean customFont, boolean shadow
   ) {
      char[] stringToCharArray = text.toCharArray();
      int index = 0;
      boolean color = false;
      String s = null;

      for (char c : stringToCharArray) {
         if (c == 167) {
            color = true;
         } else if (color) {
            if (c == 'r') {
               s = null;
            } else {
               s = "§" + c;
            }

            color = false;
         } else {
            index++;
            if (s != null) {
               drawString(drawContext, s + c, x, y, startColor.getRGB(), customFont, shadow);
            } else {
               drawString(drawContext, String.valueOf(c), x, y, ColorUtil.pulseColor(startColor, endColor, index, counter, speed).getRGB(), customFont, shadow);
            }

            x += customFont ? FontManager.ui.getWidth(String.valueOf(c)) : mc.textRenderer.getWidth(String.valueOf(c));
         }
      }
   }

   public static Vec3d worldSpaceToScreenSpace(Vec3d pos) {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      int displayHeight = mc.getWindow().getHeight();
      int[] viewport = new int[4];
      GL11.glGetIntegerv(2978, viewport);
      Vector3f target = new Vector3f();
      double deltaX = pos.x - camera.getPos().x;
      double deltaY = pos.y - camera.getPos().y;
      double deltaZ = pos.z - camera.getPos().z;
      Vector4f transformedCoordinates = new Vector4f((float)deltaX, (float)deltaY, (float)deltaZ, 1.0F).mul(lastWorldSpaceMatrix);
      Matrix4f matrixProj = new Matrix4f(lastProjMat);
      Matrix4f matrixModel = new Matrix4f(lastModMat);
      matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);
      return new Vec3d(target.x / mc.getWindow().getScaleFactor(), (displayHeight - target.y) / mc.getWindow().getScaleFactor(), target.z);
   }
}
