package dev.luminous.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.Wrapper;
import java.awt.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

public class JelloUtil implements Wrapper {
   private static float prevCircleStep;
   private static float circleStep;

   public static void drawJello(MatrixStack matrix, Entity target, Color color) {
      double cs = prevCircleStep + (circleStep - prevCircleStep) * mc.getRenderTickCounter().getTickDelta(true);
      double prevSinAnim = absSinAnimation(cs - 0.45F);
      double sinAnim = absSinAnimation(cs);
      double x = target.prevX
         + (target.getX() - target.prevX) * mc.getRenderTickCounter().getTickDelta(true)
         - mc.getEntityRenderDispatcher().camera.getPos().getX();
      double y = target.prevY
         + (target.getY() - target.prevY) * mc.getRenderTickCounter().getTickDelta(true)
         - mc.getEntityRenderDispatcher().camera.getPos().getY()
         + prevSinAnim * target.getHeight();
      double z = target.prevZ
         + (target.getZ() - target.prevZ) * mc.getRenderTickCounter().getTickDelta(true)
         - mc.getEntityRenderDispatcher().camera.getPos().getZ();
      double nextY = target.prevY
         + (target.getY() - target.prevY) * mc.getRenderTickCounter().getTickDelta(true)
         - mc.getEntityRenderDispatcher().camera.getPos().getY()
         + sinAnim * target.getHeight();
      matrix.push();
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferBuilder = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);

      for (int i = 0; i <= 30; i++) {
         float cos = (float)(
            x
               + Math.cos(i * 6.28 / 30.0)
                  * (target.getBoundingBox().maxX - target.getBoundingBox().minX + (target.getBoundingBox().maxZ - target.getBoundingBox().minZ))
                  * 0.5
         );
         float sin = (float)(
            z
               + Math.sin(i * 6.28 / 30.0)
                  * (target.getBoundingBox().maxX - target.getBoundingBox().minX + (target.getBoundingBox().maxZ - target.getBoundingBox().minZ))
                  * 0.5
         );
         bufferBuilder.vertex(matrix.peek().getPositionMatrix(), cos, (float)nextY, sin).color(color.getRGB());
         bufferBuilder.vertex(matrix.peek().getPositionMatrix(), cos, (float)y, sin).color(ColorUtil.injectAlpha(color, 0).getRGB());
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
      matrix.pop();
   }

   public static void updateJello() {
      prevCircleStep = circleStep;
      circleStep += 0.15F;
   }

   private static double absSinAnimation(double input) {
      return Math.abs(1.0 + Math.sin(input)) / 2.0;
   }
}
