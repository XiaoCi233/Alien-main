package dev.luminous.mod.gui.windows;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.impl.client.ColorsModule;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class WindowBase {
   private final String name;
   private final Identifier icon;
   private float x;
   private float y;
   private float width;
   private float height;
   private float dragX;
   private float dragY;
   private float scrollOffset;
   private float prevScrollOffset;
   private float maxElementsHeight;
   private boolean dragging;
   private boolean hoveringWindow;
   private boolean scaling;
   private boolean scrolling;
   private boolean visible = true;

   protected WindowBase(float x, float y, float width, float height, String name, Identifier icon) {
      this.setX(x);
      this.setY(y);
      this.setWidth(width);
      this.setHeight(height);
      this.name = name;
      this.icon = icon;
   }

   protected void render(DrawContext context, int mouseX, int mouseY) {
      this.prevScrollOffset = AnimateUtil.fast(this.prevScrollOffset, this.scrollOffset, 12.0F);
      Color color2 = new Color(-983868581, true);
      RenderSystem.enableBlend();
      Render2DUtil.drawRect(context.getMatrices(), this.x, this.y, this.width + 10.0F, this.height, -1072689136);
      Render2DUtil.drawRect(context.getMatrices(), this.x + 0.5F, this.y, this.width + 9.0F, 16.0F, new Color(1593835520, true));
      Render2DUtil.horizontalGradient(
         context.getMatrices(),
         this.x + 2.0F,
         this.y + 16.0F,
         this.x + 2.0F + this.width / 2.0F - 2.0F,
         this.y + 16.5F,
         ColorUtil.injectAlpha(ColorsModule.INSTANCE.clientColor.getValue(), 0),
         ColorsModule.INSTANCE.clientColor.getValue()
      );
      Render2DUtil.horizontalGradient(
         context.getMatrices(),
         this.x + 2.0F + this.width / 2.0F - 2.0F,
         this.y + 16.0F,
         this.x + 2.0F + this.width - 4.0F,
         this.y + 16.5F,
         ColorsModule.INSTANCE.clientColor.getValue(),
         ColorUtil.injectAlpha(ColorsModule.INSTANCE.clientColor.getValue(), 0)
      );
      FontManager.ui.drawString(context.getMatrices(), this.name, this.x + 4.0F, this.y + 5.5F, -1);
      boolean hover1 = Render2DUtil.isHovered(mouseX, mouseY, this.x + this.width - 4.0F, this.y + 3.0F, 10.0, 10.0);
      Render2DUtil.drawRectWithOutline(
         context.getMatrices(),
         this.x + this.width - 4.0F,
         this.y + 3.0F,
         10.0F,
         10.0F,
         hover1 ? new Color(-982026377, true) : new Color(-984131753, true),
         color2
      );
      float ratio = (this.getHeight() - 35.0F) / this.maxElementsHeight;
      boolean hover2 = Render2DUtil.isHovered(mouseX, mouseY, this.x + this.width, this.y + 19.0F, 6.0, this.getHeight() - 34.0F);
      Render2DUtil.drawRectWithOutline(
         context.getMatrices(),
         this.x + this.width,
         this.y + 19.0F,
         6.0F,
         this.getHeight() - 34.0F,
         hover2 ? new Color(1595085587, true) : new Color(1593835520, true),
         color2
      );
      Render2DUtil.drawRect(
         context.getMatrices(),
         this.x + this.width,
         Math.max(this.y + 19.0F - this.scrollOffset * ratio, this.y + 19.0F),
         6.0F,
         Math.min((this.getHeight() - 34.0F) * ratio, this.getHeight() - 34.0F),
         new Color(-1590611663, true)
      );
      Render2DUtil.drawLine(context.getMatrices(), this.x + this.width - 2.0F, this.y + 5.0F, this.x + this.width + 4.0F, this.y + 11.0F, -1);
      Render2DUtil.drawLine(context.getMatrices(), this.x + this.width - 2.0F, this.y + 11.0F, this.x + this.width + 4.0F, this.y + 5.0F, -1);
      RenderSystem.disableBlend();
      if (this.scrolling) {
         float diff = (mouseY - this.y - 19.0F) / (this.getHeight() - 34.0F);
         this.scrollOffset = -(diff * this.maxElementsHeight);
         this.scrollOffset = MathUtil.clamp(this.scrollOffset, -this.maxElementsHeight + (this.getHeight() - 40.0F), 0.0F);
      }

      this.hoveringWindow = Render2DUtil.isHovered(mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight());
      Render2DUtil.drawLine(
         context.getMatrices(),
         this.getX() + this.getWidth(),
         this.getY() + this.getHeight() - 3.0F,
         this.getX() + this.getWidth() + 7.0F,
         this.getY() + this.getHeight() - 10.0F,
         color2.getRGB()
      );
      Render2DUtil.drawLine(
         context.getMatrices(),
         this.getX() + this.getWidth() + 5.0F,
         this.getY() + this.getHeight() - 3.0F,
         this.getX() + this.getWidth() + 7.0F,
         this.getY() + this.getHeight() - 5.0F,
         color2.getRGB()
      );
   }

   protected void mouseClicked(double mouseX, double mouseY, int button) {
      if (Render2DUtil.isHovered(mouseX, mouseY, this.x + this.width - 4.0F, this.y + 3.0F, 10.0, 10.0)) {
         this.setVisible(false);
      } else if (Render2DUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, 10.0)) {
         if (WindowsScreen.draggingWindow == null) {
            this.dragging = true;
         }

         if (WindowsScreen.draggingWindow == null) {
            WindowsScreen.draggingWindow = this;
         }

         WindowsScreen.lastClickedWindow = this;
         this.dragX = (int)(mouseX - this.getX());
         this.dragY = (int)(mouseY - this.getY());
      } else if (Render2DUtil.isHovered(mouseX, mouseY, this.x + this.width, this.y + this.height - 10.0F, 10.0, 10.0)) {
         WindowsScreen.lastClickedWindow = this;
         this.dragX = (int)(mouseX - this.getWidth());
         this.dragY = (int)(mouseY - this.getHeight());
         this.scaling = true;
      } else {
         if (Render2DUtil.isHovered(mouseX, mouseY, this.x + this.width, this.y + 19.0F, 6.0, this.getHeight() - 34.0F)) {
            WindowsScreen.lastClickedWindow = this;
            this.dragX = (int)(mouseX - this.getWidth());
            this.dragY = (int)(mouseY - this.getHeight());
            this.scrolling = true;
         }
      }
   }

   protected void keyPressed(int keyCode, int scanCode, int modifiers) {
   }

   protected void charTyped(char key, int keyCode) {
   }

   protected void mouseScrolled(int i) {
      if (this.hoveringWindow) {
         this.scrollOffset += i * 2;
         this.scrollOffset = MathUtil.clamp(this.scrollOffset, -this.maxElementsHeight + (this.getHeight() - 40.0F), 0.0F);
      }
   }

   public void mouseReleased(double mouseX, double mouseY, int button) {
      this.dragging = false;
      this.scaling = false;
      this.scrolling = false;
      WindowsScreen.draggingWindow = null;
   }

   protected float getX() {
      return this.x;
   }

   protected void setX(float x) {
      this.x = x;
   }

   protected float getY() {
      return this.y;
   }

   protected void setY(float y) {
      this.y = y;
   }

   protected float getWidth() {
      return this.width;
   }

   protected void setWidth(float width) {
      this.width = width;
   }

   protected float getHeight() {
      return this.height;
   }

   protected void setHeight(float height) {
      this.height = height;
   }

   protected float getScrollOffset() {
      return this.prevScrollOffset;
   }

   protected void resetScroll() {
      this.prevScrollOffset = 0.0F;
      this.scrollOffset = 0.0F;
   }

   protected void setMaxElementsHeight(float maxElementsHeight) {
      this.maxElementsHeight = maxElementsHeight;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }

   public Identifier getIcon() {
      return this.icon;
   }
}
