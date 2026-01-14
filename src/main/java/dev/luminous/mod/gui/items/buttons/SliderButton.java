package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class SliderButton extends Button {
   private final double min;
   private final double max;
   private final double difference;
   public final SliderSetting setting;
   public boolean isListening;
   private String currentString = "";
   private boolean drag = false;

   public SliderButton(SliderSetting setting) {
      super(setting.getName());
      this.setting = setting;
      this.min = setting.getMin();
      this.max = setting.getMax();
      this.difference = this.max - this.min;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      this.dragSetting(mouseX, mouseY);
      Render2DUtil.rect(
         context.getMatrices(),
         this.x,
         this.y,
         this.x + this.width + 7.0F,
         this.y + this.height - 0.5F,
         !this.isHovering(mouseX, mouseY) ? defaultColor : hoverColor
      );
      Color color = ClickGui.getInstance().color.getValue();
      Render2DUtil.rect(
         context.getMatrices(),
         this.x,
         this.y,
         this.setting.getValue() <= this.min ? this.x : (float)(this.x + (this.width + 7.0F) * this.partialMultiplier()),
         this.y + this.height - 0.5F,
         !this.isHovering(mouseX, mouseY)
            ? ColorUtil.injectAlpha(color, ClickGui.getInstance().alpha.getValueInt()).getRGB()
            : ColorUtil.injectAlpha(color, ClickGui.getInstance().hoverAlpha.getValueInt()).getRGB()
      );
      if (this.isListening) {
         this.drawString(
            this.currentString + StringButton.getIdleSign(),
            this.x + 2.3F,
            this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(),
            this.getState() ? enableTextColor : defaultTextColor
         );
      } else {
         this.drawString(
            this.getName() + " " + Formatting.GRAY + this.setting.getValueFloat() + this.setting.getSuffix(),
            this.x + 2.3F,
            this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(),
            enableTextColor
         );
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (this.isHovering(mouseX, mouseY)) {
         sound();
         if (mouseButton == 0) {
            if (this.isListening) {
               this.toggle();
            } else {
               this.setSettingFromX(mouseX);
               this.drag = true;
            }
         } else if (mouseButton == 1) {
            this.toggle();
         }
      }
   }

   @Override
   public boolean isHovering(int mouseX, int mouseY) {
      for (Component component : ClickGuiScreen.getInstance().getComponents()) {
         if (component.drag) {
            return false;
         }
      }

      return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() + 8.0F && mouseY >= this.getY() && mouseY <= this.getY() + this.height - 1.0F;
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.isListening) {
         this.setString(this.currentString + typedChar);
      }
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isListening) {
         switch (key) {
            case 86:
               if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 341)) {
                  this.setString(this.currentString + SelectionManager.getClipboard(mc));
               }
               break;
            case 256:
               this.isListening = false;
               break;
            case 257:
            case 335:
               this.enterString();
               break;
            case 259:
               this.setString(StringButton.removeLastChar(this.currentString));
         }
      }
   }

   private void enterString() {
      if (!this.currentString.isEmpty() && this.isNumeric(this.currentString)) {
         this.setting.setValue(Double.parseDouble(this.currentString));
      } else {
         this.setting.setValue(this.setting.getDefaultValue());
      }

      this.onMouseClick();
   }

   public void setString(String newString) {
      this.currentString = newString;
   }

   private void dragSetting(int mouseX, int mouseY) {
      if (this.drag && this.isHovering(mouseX, mouseY) && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
         this.setSettingFromX(mouseX);
      } else {
         this.drag = false;
      }
   }

   @Override
   public void toggle() {
      this.setString(this.setting.getValueFloat() + "");
      this.isListening = !this.isListening;
   }

   @Override
   public boolean getState() {
      return !this.isListening;
   }

   private void setSettingFromX(int mouseX) {
      double percent = (mouseX - this.x) / (this.width + 7.4);
      double result = Math.min(this.setting.getMin() + this.difference * percent, this.max);
      this.setting.setValue(result);
   }

   private double part() {
      return this.setting.getValue() - this.min;
   }

   private double partialMultiplier() {
      return Math.min(this.part() / this.difference, 1.0);
   }

   private boolean isNumeric(String str) {
      return str.matches("-?\\d+(\\.\\d+)?");
   }
}
