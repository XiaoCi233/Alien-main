package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;

public class EnumButton extends Button {
   public final EnumSetting<?> setting;
   private boolean open;

   public EnumButton(EnumSetting<?> setting) {
      super(setting.getName());
      this.setting = setting;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
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
      this.drawString(
         this.setting.getName() + " " + Formatting.GRAY + (this.setting.getValue().name().equalsIgnoreCase("ABC") ? "ABC" : this.setting.getValue().name()),
         this.x + 2.3F,
         this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(),
         this.getState() ? enableTextColor : defaultTextColor
      );
      if (this.open) {
         int y = (int)this.y;

         for (Enum<?> e : (Enum[])this.setting.getValue().getDeclaringClass().getEnumConstants()) {
            String s = e.name();
            this.drawString(
               s,
               this.width / 2.0F - this.getWidth(s) / 2.0F + 2.0F + this.x,
               (float)y + this.height - 3.0F - ClickGuiScreen.getInstance().getTextOffset(),
               this.setting.getValue().name().equals(s) ? enableTextColor : defaultTextColor
            );
            y += 11;
         }
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
         this.open = !this.open;
         sound();
      } else if (mouseButton == 0 && this.open) {
         int y = (int)this.y;

         for (Object o : (Enum[])this.setting.getValue().getDeclaringClass().getEnumConstants()) {
            if (mouseX > this.x && mouseX < this.x + this.width && mouseY >= y + this.height + 1 && mouseY < y + this.height + 11 + 1) {
               this.setting.setEnumValue(String.valueOf(o));
               sound();
            }

            y += 11;
         }
      }
   }

   @Override
   public int getHeight() {
      return super.getHeight() + (this.open ? 11 * ((Enum[])this.setting.getValue().getDeclaringClass().getEnumConstants()).length : 0);
   }

   @Override
   public void toggle() {
      this.setting.increaseEnum();
   }

   @Override
   public boolean getState() {
      return true;
   }
}
