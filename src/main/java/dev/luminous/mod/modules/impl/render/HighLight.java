package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

public class HighLight extends Module {
   public static HighLight INSTANCE;
   private final BooleanSetting depth = this.add(new BooleanSetting("Depth", true));
   private final ColorSetting fill = this.add(new ColorSetting("Fill", new Color(255, 0, 0, 50)).injectBoolean(true));
   private final ColorSetting boxColor = this.add(new ColorSetting("Box", new Color(255, 0, 0, 100)).injectBoolean(true));

   public HighLight() {
      super("HighLight", Module.Category.Render);
      INSTANCE = this;
      this.setChinese("方块高亮");
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (mc.crosshairTarget.getType() == Type.BLOCK
         && mc.crosshairTarget instanceof BlockHitResult hitResult
         && (this.fill.booleanValue || this.boxColor.booleanValue)) {
         VoxelShape shape = mc.world.getBlockState(hitResult.getBlockPos()).getOutlineShape(mc.world, hitResult.getBlockPos());
         if (shape == null) {
            return;
         }

         if (shape.isEmpty()) {
            return;
         }

         Box box = shape.getBoundingBox().offset(hitResult.getBlockPos()).expand(0.001);
         box = box.offset(mc.gameRenderer.getCamera().getPos().negate());
         RenderSystem.enableBlend();
         if (!this.depth.getValue()) {
            RenderSystem.disableDepthTest();
         } else {
            RenderSystem.enableDepthTest();
         }

         Matrix4f matrix = matrixStack.peek().getPositionMatrix();
         if (this.fill.booleanValue) {
            Color color = this.fill.getValue();
            float a = color.getAlpha() / 255.0F;
            float r = color.getRed() / 255.0F;
            float g = color.getGreen() / 255.0F;
            float b = color.getBlue() / 255.0F;
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         }

         if (this.depth.getValue()) {
            RenderSystem.disableDepthTest();
         }

         if (this.boxColor.booleanValue) {
            Color color = this.boxColor.getValue();
            float a = color.getAlpha() / 255.0F;
            float r = color.getRed() / 255.0F;
            float g = color.getGreen() / 255.0F;
            float b = color.getBlue() / 255.0F;
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
            bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         }

         RenderSystem.enableDepthTest();
         RenderSystem.disableBlend();
      }
   }
}
