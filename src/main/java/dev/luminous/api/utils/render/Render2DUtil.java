package dev.luminous.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.Wrapper;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class Render2DUtil implements Wrapper {
   public static void rect(MatrixStack stack, float x1, float y1, float x2, float y2, int color) {
      rectFilled(stack, x1, y1, x2, y2, color);
   }

   public static void arrow(MatrixStack matrixStack, float x, float y, Color color) {
      drawRectWithOutline(matrixStack, x - 1.0F, y - 1.0F, 2.0F, 2.0F, color, Color.BLACK);
   }

   public static void rectFilled(MatrixStack matrix, float x1, float y1, float x2, float y2, int color) {
      float f = (color >> 24 & 0xFF) / 255.0F;
      float g = (color >> 16 & 0xFF) / 255.0F;
      float h = (color >> 8 & 0xFF) / 255.0F;
      float j = (color & 0xFF) / 255.0F;
      if (!(f <= 0.01)) {
         if (x1 < x2) {
            float i = x1;
            x1 = x2;
            x2 = i;
         }

         if (y1 < y2) {
            float i = y1;
            y1 = y2;
            y2 = i;
         }

         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x1, y2, 0.0F).color(g, h, j, f);
         bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x2, y2, 0.0F).color(g, h, j, f);
         bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x2, y1, 0.0F).color(g, h, j, f);
         bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x1, y1, 0.0F).color(g, h, j, f);
         BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         RenderSystem.disableBlend();
      }
   }

   public static void horizontalGradient(MatrixStack matrices, float x1, float y1, float x2, float y2, Color startColor, Color endColor) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(startColor.getRGB());
      bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(startColor.getRGB());
      bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(endColor.getRGB());
      bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(endColor.getRGB());
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void horizontalGradient(MatrixStack matrices, float x1, float y1, float x2, float y2, int startColor, int endColor) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(endColor);
      bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(endColor);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void verticalGradient(MatrixStack matrices, float x1, float y1, float x2, float y2, Color startColor, Color endColor) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(startColor.getRGB());
      bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(startColor.getRGB());
      bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(endColor.getRGB());
      bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(endColor.getRGB());
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void verticalGradient(MatrixStack matrices, float x1, float y1, float x2, float y2, int startColor, int endColor) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(endColor);
      bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(endColor);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void drawLine(MatrixStack matrices, float x, float y, float x1, float y1, int color) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(color);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void drawRectWithOutline(MatrixStack matrices, float x, float y, float width, float height, Color c, Color c2) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, x, y + height, 0.0F).color(c.getRGB());
      buffer.vertex(matrix, x + width, y + height, 0.0F).color(c.getRGB());
      buffer.vertex(matrix, x + width, y, 0.0F).color(c.getRGB());
      buffer.vertex(matrix, x, y, 0.0F).color(c.getRGB());
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, x, y + height, 0.0F).color(c2.getRGB());
      buffer.vertex(matrix, x + width, y + height, 0.0F).color(c2.getRGB());
      buffer.vertex(matrix, x + width, y, 0.0F).color(c2.getRGB());
      buffer.vertex(matrix, x, y, 0.0F).color(c2.getRGB());
      buffer.vertex(matrix, x, y + height, 0.0F).color(c2.getRGB());
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableBlend();
   }

   public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, int c) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x, y + height, 0.0F).color(c);
      bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).color(c);
      bufferBuilder.vertex(matrix, x + width, y, 0.0F).color(c);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(c);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, Color c) {
      drawRect(matrices, x, y, width, height, c.getRGB());
   }

   public static void drawRect(DrawContext drawContext, float x, float y, float width, float height, Color c) {
      drawRect(drawContext.getMatrices(), x, y, width, height, c);
   }

   public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
      return mouseX >= x && mouseX - width <= x && mouseY >= y && mouseY - height <= y;
   }

   public static void drawGlow(MatrixStack matrices, float x, float y, float width, float height, int color) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      int startColor = ColorUtil.injectAlpha(color, 20);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      RenderSystem.disableCull();
      RenderSystem.enableBlend();
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      float halfWidth = width / 2.0F;
      float halfHeight = height / 2.0F;
      float centerX = x + halfWidth;
      float centerY = y + halfHeight;
      float x2 = x + width;
      float y2 = y + height;
      bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x, centerY, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, centerX, y, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
      bufferBuilder.vertex(matrix, centerX, y, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x2, y, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x2, centerY, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x, centerY, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x, y2, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, centerX, y2, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x2, centerY, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, centerX, y2, 0.0F).color(startColor);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
   }

   public static void drawCircle(MatrixStack matrices, float cx, float cy, float r, Color c, int segments) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

      for (int i = 0; i <= segments; i++) {
         double a = i * (Math.PI * 2) / segments;
         float x = (float)(cx + Math.cos(a) * r);
         float y = (float)(cy + Math.sin(a) * r);
         bufferBuilder.vertex(matrix, x, y, 0.0F).color(c.getRGB());
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void drawPill(MatrixStack matrices, float x, float y, float width, float height, Color c) {
      float r = height / 2.0F;
      drawRect(matrices, x + r, y, width - 2.0F * r, height, c);
      drawCircle(matrices, x + r, y + r, r, c, 64);
      drawCircle(matrices, x + width - r, y + r, r, c, 64);
   }

   public static void drawRoundedStroke(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c, int seg) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      float r = Math.min(radius, Math.min(width, height) / 2.0F);
      float x1 = x + r;
      float y1 = y + r;
      float x2 = x + width - r;
      float y2 = y + height - r;

      for (int i = 0; i <= seg; i++) {
         double a = (-Math.PI / 2) + i * (Math.PI / 2) / seg;
         buffer.vertex(matrix, (float)(x2 + Math.cos(a) * r), (float)(y1 + Math.sin(a) * r), 0.0F).color(c.getRGB());
      }

      for (int i = 0; i <= seg; i++) {
         double a = 0.0 + i * (Math.PI / 2) / seg;
         buffer.vertex(matrix, (float)(x2 + Math.cos(a) * r), (float)(y2 + Math.sin(a) * r), 0.0F).color(c.getRGB());
      }

      for (int i = 0; i <= seg; i++) {
         double a = (Math.PI / 2) + i * (Math.PI / 2) / seg;
         buffer.vertex(matrix, (float)(x1 + Math.cos(a) * r), (float)(y2 + Math.sin(a) * r), 0.0F).color(c.getRGB());
      }

      for (int i = 0; i <= seg; i++) {
         double a = Math.PI + i * (Math.PI / 2) / seg;
         buffer.vertex(matrix, (float)(x1 + Math.cos(a) * r), (float)(y1 + Math.sin(a) * r), 0.0F).color(c.getRGB());
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableBlend();
   }

   public static void drawRainbowRoundedStroke(MatrixStack matrices, float x, float y, float width, float height, float radius, int seg, float speed, int alpha) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      float r = Math.min(radius, Math.min(width, height) / 2.0F);
      float x1 = x + r;
      float y1 = y + r;
      float x2 = x + width - r;
      float y2 = y + height - r;
      double t = (double)(System.currentTimeMillis() % (long)(1000.0F / Math.max(0.001F, speed))) / (1000.0F / Math.max(0.001F, speed));

      for (int i = 0; i <= seg; i++) {
         double a = (-Math.PI / 2) + i * (Math.PI / 2) / seg;
         float hue = (float)((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         buffer.vertex(matrix, (float)(x2 + Math.cos(a) * r), (float)(y1 + Math.sin(a) * r), 0.0F).color(argb);
      }

      for (int i = 0; i <= seg; i++) {
         double a = 0.0 + i * (Math.PI / 2) / seg;
         float hue = (float)((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         buffer.vertex(matrix, (float)(x2 + Math.cos(a) * r), (float)(y2 + Math.sin(a) * r), 0.0F).color(argb);
      }

      for (int i = 0; i <= seg; i++) {
         double a = (Math.PI / 2) + i * (Math.PI / 2) / seg;
         float hue = (float)((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         buffer.vertex(matrix, (float)(x1 + Math.cos(a) * r), (float)(y2 + Math.sin(a) * r), 0.0F).color(argb);
      }

      for (int i = 0; i <= seg; i++) {
         double a = Math.PI + i * (Math.PI / 2) / seg;
         float hue = (float)((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         buffer.vertex(matrix, (float)(x1 + Math.cos(a) * r), (float)(y1 + Math.sin(a) * r), 0.0F).color(argb);
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      BufferBuilder inner = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      float rIn = Math.max(0.5F, r - 1.2F);

      for (int i = 0; i <= seg; i++) {
         double a = (-Math.PI / 2) + i * (Math.PI / 2) / seg;
         float hue = (float)((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.vertex(matrix, (float)(x2 + Math.cos(a) * rIn), (float)(y1 + Math.sin(a) * rIn), 0.0F).color(argb);
      }

      for (int i = 0; i <= seg; i++) {
         double a = 0.0 + i * (Math.PI / 2) / seg;
         float hue = (float)((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.vertex(matrix, (float)(x2 + Math.cos(a) * rIn), (float)(y2 + Math.sin(a) * rIn), 0.0F).color(argb);
      }

      for (int i = 0; i <= seg; i++) {
         double a = (Math.PI / 2) + i * (Math.PI / 2) / seg;
         float hue = (float)((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.vertex(matrix, (float)(x1 + Math.cos(a) * rIn), (float)(y2 + Math.sin(a) * rIn), 0.0F).color(argb);
      }

      for (int i = 0; i <= seg; i++) {
         double a = Math.PI + i * (Math.PI / 2) / seg;
         float hue = (float)((a + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.vertex(matrix, (float)(x1 + Math.cos(a) * rIn), (float)(y1 + Math.sin(a) * rIn), 0.0F).color(argb);
      }

      BufferRenderer.drawWithGlobalProgram(inner.end());
      RenderSystem.disableBlend();
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c) {
      if (radius <= 0.0F) {
         drawRect(matrices, x, y, width, height, c);
      } else {
         float r = Math.min(radius, Math.min(width, height) / 2.0F);
         drawRect(matrices, x + r, y, width - 2.0F * r, height, c);
         drawRect(matrices, x, y + r, r, height - 2.0F * r, c);
         drawRect(matrices, x + width - r, y + r, r, height - 2.0F * r, c);
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         int seg = 48;
         float cx = x + r;
         float cy = y + r;
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         buffer.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

         for (int i = 0; i <= seg; i++) {
            double a = Math.PI + i * (Math.PI / 2) / seg;
            buffer.vertex(matrix, (float)(cx + Math.cos(a) * r), (float)(cy + Math.sin(a) * r), 0.0F).color(c.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
         cx = x + width - r;
         cy = y + r;
         buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         buffer.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

         for (int i = 0; i <= seg; i++) {
            double a = (Math.PI * 3.0 / 2.0) + i * (Math.PI / 2) / seg;
            buffer.vertex(matrix, (float)(cx + Math.cos(a) * r), (float)(cy + Math.sin(a) * r), 0.0F).color(c.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
         cx = x + width - r;
         cy = y + height - r;
         buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         buffer.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

         for (int i = 0; i <= seg; i++) {
            double a = 0.0 + i * (Math.PI / 2) / seg;
            buffer.vertex(matrix, (float)(cx + Math.cos(a) * r), (float)(cy + Math.sin(a) * r), 0.0F).color(c.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
         cx = x + r;
         cy = y + height - r;
         buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         buffer.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

         for (int i = 0; i <= seg; i++) {
            double a = (Math.PI / 2) + i * (Math.PI / 2) / seg;
            buffer.vertex(matrix, (float)(cx + Math.cos(a) * r), (float)(cy + Math.sin(a) * r), 0.0F).color(c.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static void drawDropShadow(MatrixStack matrices, float x, float y, float width, float height, float radius) {
   }
}
