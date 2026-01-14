package dev.luminous.mod.gui.items.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

public class PickerButton extends Button {
   static MatrixStack matrixStack;
   private final ColorSetting setting;
   boolean pickingColor;
   boolean pickingHue;
   boolean pickingAlpha;
   boolean open;
   float[] hsb = new float[]{1.0F, 1.0F, 1.0F, 1.0F};

   public PickerButton(ColorSetting setting) {
      super(setting.getName());
      this.setting = setting;
   }

   public static boolean mouseOver(int minX, int minY, int maxX, int maxY, int mX, int mY) {
      return mX >= minX && mY >= minY && mX <= maxX && mY <= maxY;
   }

   public static Color getColor(Color color, float alpha) {
      float red = color.getRed() / 255.0F;
      float green = color.getGreen() / 255.0F;
      float blue = color.getBlue() / 255.0F;
      return new Color(red, green, blue, alpha);
   }

   public static void drawPickerBase(int pickerX, int pickerY, int pickerWidth, int pickerHeight, float red, float green, float blue, float alpha) {
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), pickerX, pickerY, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), pickerX, pickerY + pickerHeight, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), pickerX + pickerWidth, pickerY + pickerHeight, 0.0F).color(red, green, blue, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), pickerX + pickerWidth, pickerY, 0.0F).color(red, green, blue, 1.0F);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), pickerX, pickerY, 0.0F).color(0.0F, 0.0F, 0.0F, 0.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), pickerX, pickerY + pickerHeight, 0.0F).color(0.0F, 0.0F, 0.0F, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), pickerX + pickerWidth, pickerY + pickerHeight, 0.0F).color(0.0F, 0.0F, 0.0F, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), pickerX + pickerWidth, pickerY, 0.0F).color(0.0F, 0.0F, 0.0F, 0.0F);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void drawLeftGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), right, top, 0.0F)
         .color((endColor >> 24 & 0xFF) / 255.0F, (endColor >> 16 & 0xFF) / 255.0F, (endColor >> 8 & 0xFF) / 255.0F, (endColor >> 24 & 0xFF) / 255.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), left, top, 0.0F)
         .color((startColor >> 16 & 0xFF) / 255.0F, (startColor >> 8 & 0xFF) / 255.0F, (startColor & 0xFF) / 255.0F, (startColor >> 24 & 0xFF) / 255.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), left, bottom, 0.0F)
         .color((startColor >> 16 & 0xFF) / 255.0F, (startColor >> 8 & 0xFF) / 255.0F, (startColor & 0xFF) / 255.0F, (startColor >> 24 & 0xFF) / 255.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), right, bottom, 0.0F)
         .color((endColor >> 24 & 0xFF) / 255.0F, (endColor >> 16 & 0xFF) / 255.0F, (endColor >> 8 & 0xFF) / 255.0F, (endColor >> 24 & 0xFF) / 255.0F);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void gradient(int minX, int minY, int maxX, int maxY, int startColor, int endColor, boolean left) {
      float startA = (startColor >> 24 & 0xFF) / 255.0F;
      float startR = (startColor >> 16 & 0xFF) / 255.0F;
      float startG = (startColor >> 8 & 0xFF) / 255.0F;
      float startB = (startColor & 0xFF) / 255.0F;
      float endA = (endColor >> 24 & 0xFF) / 255.0F;
      float endR = (endColor >> 16 & 0xFF) / 255.0F;
      float endG = (endColor >> 8 & 0xFF) / 255.0F;
      float endB = (endColor & 0xFF) / 255.0F;
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), minX, minY, 0.0F).color(startR, startG, startB, startA);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), minX, maxY, 0.0F).color(startR, startG, startB, startA);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), maxX, maxY, 0.0F).color(endR, endG, endB, endA);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), maxX, minY, 0.0F).color(endR, endG, endB, endA);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static int gradientColor(int color, int percentage) {
      int r = ((color & 0xFF0000) >> 16) * (100 + percentage) / 100;
      int g = ((color & 0xFF00) >> 8) * (100 + percentage) / 100;
      int b = (color & 0xFF) * (100 + percentage) / 100;
      return new Color(r, g, b).hashCode();
   }

   public static void drawGradientRect(float left, float top, float right, float bottom, int startColor, int endColor, boolean hovered) {
      if (hovered) {
         startColor = gradientColor(startColor, -20);
         endColor = gradientColor(endColor, -20);
      }

      float c = (startColor >> 24 & 0xFF) / 255.0F;
      float c1 = (startColor >> 16 & 0xFF) / 255.0F;
      float c2 = (startColor >> 8 & 0xFF) / 255.0F;
      float c3 = (startColor & 0xFF) / 255.0F;
      float c4 = (endColor >> 24 & 0xFF) / 255.0F;
      float c5 = (endColor >> 16 & 0xFF) / 255.0F;
      float c6 = (endColor >> 8 & 0xFF) / 255.0F;
      float c7 = (endColor & 0xFF) / 255.0F;
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), right, top, 0.0F).color(c1, c2, c3, c);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), left, top, 0.0F).color(c1, c2, c3, c);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), left, bottom, 0.0F).color(c5, c6, c7, c4);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), right, bottom, 0.0F).color(c5, c6, c7, c4);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static String readClipboard() {
      try {
         return (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
      } catch (IOException | UnsupportedFlavorException var1) {
         return null;
      }
   }

   public static void drawOutlineRect(double left, double top, double right, double bottom, Color color, float lineWidth) {
      if (left < right) {
         double i = left;
         left = right;
         right = i;
      }

      if (top < bottom) {
         double j = top;
         top = bottom;
         bottom = j;
      }

      float f3 = (color.getRGB() >> 24 & 0xFF) / 255.0F;
      float f = (color.getRGB() >> 16 & 0xFF) / 255.0F;
      float f1 = (color.getRGB() >> 8 & 0xFF) / 255.0F;
      float f2 = (color.getRGB() & 0xFF) / 255.0F;
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)left, (float)bottom, 0.0F).color(f, f1, f2, f3);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)right, (float)bottom, 0.0F).color(f, f1, f2, f3);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)right, (float)top, 0.0F).color(f, f1, f2, f3);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)left, (float)top, 0.0F).color(f, f1, f2, f3);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      matrixStack = context.getMatrices();
      Color color = ClickGui.getInstance().color.getValue();
      Render2DUtil.rect(
         context.getMatrices(),
         this.x,
         this.y,
         this.x + this.width + 7.0F,
         this.y + this.height - 0.5F,
         this.getState()
            ? (
               !this.isHovering(mouseX, mouseY)
                  ? ColorUtil.injectAlpha(color, ClickGui.getInstance().alpha.getValueInt()).getRGB()
                  : ColorUtil.injectAlpha(color, ClickGui.getInstance().hoverAlpha.getValueInt()).getRGB()
            )
            : (!this.isHovering(mouseX, mouseY) ? defaultColor : hoverColor)
      );
      Render2DUtil.rect(
         matrixStack,
         this.x - 1.5F + this.width + 0.6F - 0.5F,
         this.y + 4.0F,
         this.x + this.width + 7.0F - 2.5F,
         this.y + this.height - 5.0F,
         ColorUtil.injectAlpha(this.setting.getValue(), 255).getRGB()
      );
      this.drawString(this.getName(), this.x + 2.3F, this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(), enableTextColor);
      if (this.open) {
         this.drawPicker(this.setting, (int)this.x, (int)this.y + this.height, (int)this.x, (int)this.y + 103, (int)this.x, (int)this.y + 95, mouseX, mouseY);
         this.drawString(
            "copy",
            this.x + 2.3F,
            this.y + 96.0F + this.height - ClickGuiScreen.getInstance().getTextOffset(),
            this.isInsideCopy(mouseX, mouseY) ? enableTextColor : defaultTextColor
         );
         this.drawString(
            "paste",
            this.x + this.width - 2.3F - this.getWidth("paste") + 11.7F - 4.6F,
            this.y + 96.0F + this.height - ClickGuiScreen.getInstance().getTextOffset(),
            this.isInsidePaste(mouseX, mouseY) ? enableTextColor : defaultTextColor
         );
         this.drawString(
            "sync",
            this.x + 2.3F,
            this.y + 96.0F + this.getFontHeight() + this.height - ClickGuiScreen.getInstance().getTextOffset(),
            this.setting.sync ? ColorUtil.injectAlpha(color, 255).getRGB() : (this.isInsideRainbow(mouseX, mouseY) ? enableTextColor : defaultTextColor)
         );
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0) {
         int pickerWidth = (int)(this.width + 7.4F);
         int pickerHeight = 78;
         int hueSliderWidth = pickerWidth + 3;
         int hueSliderHeight = 7;
         int alphaSliderHeight = 7;
         if (mouseOver((int)this.x, (int)this.y + 15, (int)this.x + pickerWidth, (int)this.y + 15 + pickerHeight, mouseX, mouseY)) {
            this.pickingColor = true;
         }

         if (mouseOver((int)this.x, (int)this.y + 103, (int)this.x + hueSliderWidth, (int)this.y + 103 + hueSliderHeight, mouseX, mouseY)) {
            this.pickingHue = true;
         }

         if (mouseOver((int)this.x, (int)this.y + 95, (int)this.x + pickerWidth, (int)this.y + 95 + alphaSliderHeight, mouseX, mouseY)) {
            this.pickingAlpha = true;
         }
      }

      if (this.isHovering(mouseX, mouseY)) {
         if (mouseButton == 1) {
            sound();
            this.open = !this.open;
         } else if (mouseButton == 0 && this.setting.injectBoolean) {
            sound();
            this.setting.booleanValue = !this.setting.booleanValue;
         }
      }

      if (mouseButton == 0 && this.isInsideRainbow(mouseX, mouseY) && this.open) {
         this.setting.sync = !this.setting.sync;
      }

      if (mouseButton == 0 && this.isInsideCopy(mouseX, mouseY) && this.open) {
         sound();
         String hex = String.format(
            "#%02x%02x%02x%02x",
            this.setting.getValue().getAlpha(),
            this.setting.getValue().getRed(),
            this.setting.getValue().getGreen(),
            this.setting.getValue().getBlue()
         );
         StringSelection selection = new StringSelection(hex);
         Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
         clipboard.setContents(selection, selection);
         CommandManager.sendMessage("Copied the color to your clipboard.");
      }

      if (mouseButton == 0 && this.isInsidePaste(mouseX, mouseY) && this.open) {
         try {
            if (readClipboard() != null) {
               if (((String)Objects.requireNonNull(readClipboard())).startsWith("#")) {
                  String hex = (String)Objects.requireNonNull(readClipboard());
                  int a = Integer.valueOf(hex.substring(1, 3), 16);
                  int r = Integer.valueOf(hex.substring(3, 5), 16);
                  int g = Integer.valueOf(hex.substring(5, 7), 16);
                  int b = Integer.valueOf(hex.substring(7, 9), 16);
                  this.setting.setValue(new Color(r, g, b, a));
               } else {
                  String[] color = readClipboard().split(",");
                  this.setting.setValue(new Color(Integer.parseInt(color[0]), Integer.parseInt(color[1]), Integer.parseInt(color[2])));
               }
            }
         } catch (Exception var9) {
            var9.printStackTrace();
            CommandManager.sendMessage("§4Bad color format! Use Hex (#FFFFFFFF)");
            this.setting.setValue(-1);
         }
      }
   }

   @Override
   public boolean getState() {
      return this.setting.booleanValue;
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
      this.pickingAlpha = false;
      this.pickingHue = false;
      this.pickingColor = false;
   }

   public boolean isInsideCopy(int mouseX, int mouseY) {
      return mouseOver(
         (int)((int)this.x + 2.3F),
         (int)(this.y + 96.0F + this.height - ClickGuiScreen.getInstance().getTextOffset()),
         (int)((int)this.x + 2.3F) + this.getWidth("copy"),
         (int)(this.y + 95.0F + this.height - ClickGuiScreen.getInstance().getTextOffset()) + this.getFontHeight(),
         mouseX,
         mouseY
      );
   }

   public boolean isInsideRainbow(int mouseX, int mouseY) {
      return mouseOver(
         (int)((int)this.x + 2.3F),
         (int)(this.y + 96.0F + this.height + this.getFontHeight() - ClickGuiScreen.getInstance().getTextOffset()),
         (int)((int)this.x + 2.3F) + this.getWidth("sync"),
         (int)(this.y + 95.0F + this.height + this.getFontHeight() - ClickGuiScreen.getInstance().getTextOffset()) + this.getFontHeight(),
         mouseX,
         mouseY
      );
   }

   public boolean isInsidePaste(int mouseX, int mouseY) {
      return mouseOver(
         (int)(this.x + this.width - 2.3F - this.getWidth("paste") + 11.7F - 4.6F),
         (int)(this.y + 96.0F + this.height - ClickGuiScreen.getInstance().getTextOffset()),
         (int)(this.x + this.width - 2.3F - this.getWidth("paste") + 11.7F - 4.6F) + this.getWidth("paste"),
         (int)(this.y + 95.0F + this.height - ClickGuiScreen.getInstance().getTextOffset()) + this.getFontHeight(),
         mouseX,
         mouseY
      );
   }

   @Override
   public int getHeight() {
      return this.open ? super.getHeight() + 119 : super.getHeight();
   }

   public void drawPicker(
      ColorSetting setting, int pickerX, int pickerY, int hueSliderX, int hueSliderY, int alphaSliderX, int alphaSliderY, int mouseX, int mouseY
   ) {
      int pickerWidth = (int)(this.width + 7.4F);
      int pickerHeight = 78;
      int hueSliderHeight = 7;
      int alphaSliderHeight = 7;
      if (this.pickingColor
         && (
            GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1
               || !mouseOver(pickerX, pickerY, pickerX + pickerWidth, pickerY + pickerHeight, mouseX, mouseY)
         )) {
         this.pickingColor = false;
      }

      if (!this.pickingColor) {
         this.hsb = Color.RGBtoHSB(setting.getValue().getRed(), setting.getValue().getGreen(), setting.getValue().getBlue(), null);
      }

      float[] color = new float[]{this.hsb[0], this.hsb[1], this.hsb[2], setting.getValue().getAlpha() / 255.0F};
      if (this.pickingHue
         && (
            GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1
               || !mouseOver(hueSliderX, hueSliderY, hueSliderX + pickerWidth, hueSliderY + hueSliderHeight, mouseX, mouseY)
         )) {
         this.pickingHue = false;
      }

      if (this.pickingAlpha
         && (
            GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1
               || !mouseOver(alphaSliderX, alphaSliderY, alphaSliderX + pickerWidth, alphaSliderY + alphaSliderHeight, mouseX, mouseY)
         )) {
         this.pickingAlpha = false;
      }

      if (this.pickingHue) {
         float restrictedX = Math.min(Math.max(hueSliderX, mouseX), hueSliderX + pickerWidth);
         color[0] = (restrictedX - hueSliderX) / pickerWidth;
      }

      if (this.pickingAlpha) {
         float restrictedX = Math.min(Math.max(alphaSliderX, mouseX), alphaSliderX + pickerWidth);
         color[3] = 1.0F - (restrictedX - alphaSliderX) / pickerWidth;
      }

      if (this.pickingColor) {
         float restrictedX = Math.min(Math.max(pickerX, mouseX), pickerX + pickerWidth);
         float restrictedY = Math.min(Math.max(pickerY, mouseY), pickerY + pickerHeight);
         color[1] = (restrictedX - pickerX) / pickerWidth;
         color[2] = 1.0F - (restrictedY - pickerY) / pickerHeight;
      }

      int selectedColor = Color.HSBtoRGB(color[0], 1.0F, 1.0F);
      float selectedRed = (selectedColor >> 16 & 0xFF) / 255.0F;
      float selectedGreen = (selectedColor >> 8 & 0xFF) / 255.0F;
      float selectedBlue = (selectedColor & 0xFF) / 255.0F;
      drawPickerBase(pickerX, pickerY, pickerWidth, pickerHeight, selectedRed, selectedGreen, selectedBlue, color[3]);
      this.drawHueSlider(hueSliderX, hueSliderY, pickerWidth, hueSliderHeight, color[0]);
      int cursorX = (int)(pickerX + color[1] * pickerWidth);
      int cursorY = (int)(pickerY + pickerHeight - color[2] * pickerHeight);
      setting.setValue(getColor(new Color(Color.HSBtoRGB(color[0], color[1], color[2])), color[3]));
      Render2DUtil.arrow(matrixStack, cursorX, cursorY, setting.getValue());
      this.drawAlphaSlider(alphaSliderX, alphaSliderY, pickerWidth - 1, alphaSliderHeight, selectedRed, selectedGreen, selectedBlue, color[3]);
   }

   public void drawHueSlider(int x, int y, int width, int height, float hue) {
      int step = 0;
      if (height > width) {
         Render2DUtil.rect(matrixStack, x, y, x + width, y + 4, -65536);
         y += 4;

         for (int colorIndex = 0; colorIndex < 6; colorIndex++) {
            int previousStep = Color.HSBtoRGB(step / 6.0F, 1.0F, 1.0F);
            int nextStep = Color.HSBtoRGB((step + 1) / 6.0F, 1.0F, 1.0F);
            drawGradientRect(x, y + step * (height / 6.0F), x + width, y + (step + 1) * (height / 6.0F), previousStep, nextStep, false);
            step++;
         }

         int sliderMinY = (int)(y + height * hue) - 4;
         Render2DUtil.rect(matrixStack, x, sliderMinY - 1, x + width, sliderMinY + 1, -1);
         drawOutlineRect(x, sliderMinY - 1, x + width, sliderMinY + 1, Color.BLACK, 1.0F);
      } else {
         for (int colorIndex = 0; colorIndex < 6; colorIndex++) {
            int previousStep = Color.HSBtoRGB(step / 6.0F, 1.0F, 1.0F);
            int nextStep = Color.HSBtoRGB((step + 1) / 6.0F, 1.0F, 1.0F);
            gradient(x + step * (width / 6), y, x + (step + 1) * (width / 6) + 3, y + height, previousStep, nextStep, true);
            step++;
         }

         int sliderMinX = (int)(x + width * hue);
         Render2DUtil.rect(matrixStack, sliderMinX - 1, y - 1.2F, sliderMinX + 1, y + height + 1.2F, -1);
         drawOutlineRect(sliderMinX - 1.2, y - 1.2, sliderMinX + 1.2, y + height + 1.2, Color.BLACK, 0.1F);
      }
   }

   public void drawAlphaSlider(int x, int y, int width, int height, float red, float green, float blue, float alpha) {
      boolean left = true;
      int checkerBoardSquareSize = height / 2;

      for (int squareIndex = -checkerBoardSquareSize; squareIndex < width; squareIndex += checkerBoardSquareSize) {
         if (!left) {
            Render2DUtil.rect(matrixStack, x + squareIndex, y, x + squareIndex + checkerBoardSquareSize, y + height, -1);
            Render2DUtil.rect(matrixStack, x + squareIndex, y + checkerBoardSquareSize, x + squareIndex + checkerBoardSquareSize, y + height, -7303024);
            if (squareIndex < width - checkerBoardSquareSize) {
               int minX = x + squareIndex + checkerBoardSquareSize;
               int maxX = Math.min(x + width, x + squareIndex + checkerBoardSquareSize * 2);
               Render2DUtil.rect(matrixStack, minX, y, maxX, y + height, -7303024);
               Render2DUtil.rect(matrixStack, minX, y + checkerBoardSquareSize, maxX, y + height, -1);
            }
         }

         left = !left;
      }

      drawLeftGradientRect(x, y, x + width, y + height, new Color(red, green, blue, 1.0F).getRGB(), 0);
      int sliderMinX = (int)(x + width - width * alpha);
      Render2DUtil.rect(matrixStack, sliderMinX - 1, y - 1.2F, sliderMinX + 1, y + height + 1.2F, -1);
      drawOutlineRect(sliderMinX - 1.2, y - 1.2, sliderMinX + 1.2, y + height + 1.2, Color.BLACK, 0.1F);
   }
}
