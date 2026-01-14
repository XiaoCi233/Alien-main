package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.modules.impl.client.ClickGui;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class Button extends Item {
   private boolean state;
   public static int hoverColor = -2007673515;
   public static int defaultColor = 290805077;
   public static int defaultTextColor = -5592406;
   public static int enableTextColor = -1;

   public Button(String name) {
      super(name);
      this.setHeight(15);
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      Color color = ClickGui.getInstance().activeColor.getValue();
      Render2DUtil.rect(
         context.getMatrices(),
         this.x,
         this.y,
         this.x + this.width,
         this.y + this.height - 0.5F,
         this.getState()
            ? (
               !this.isHovering(mouseX, mouseY)
                  ? ColorUtil.injectAlpha(color, ClickGui.getInstance().alpha.getValueInt()).getRGB()
                  : ColorUtil.injectAlpha(color, ClickGui.getInstance().hoverAlpha.getValueInt()).getRGB()
            )
            : (!this.isHovering(mouseX, mouseY) ? defaultColor : hoverColor)
      );
      this.drawString(
         this.getName(), this.x + 2.3F, this.y - 2.0F - ClickGuiScreen.getInstance().getTextOffset(), this.getState() ? enableTextColor : defaultTextColor
      );
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.onMouseClick();
      }
   }

   public void onMouseClick() {
      this.state = !this.state;
      this.toggle();
      sound();
   }

   public void toggle() {
   }

   public boolean getState() {
      return this.state;
   }

   @Override
   public int getHeight() {
      return this.height - 1;
   }

   public boolean isHovering(int mouseX, int mouseY) {
      for (Component component : ClickGuiScreen.getInstance().getComponents()) {
         if (component.drag) {
            return false;
         }
      }

      return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.height - 1.0F;
   }
}
