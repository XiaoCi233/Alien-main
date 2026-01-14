package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public class BooleanButton extends Button {
   private final BooleanSetting setting;

   public BooleanButton(BooleanSetting setting) {
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
         this.getName(), this.x + 2.3F, this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(), this.getState() ? enableTextColor : defaultTextColor
      );
      if (this.setting.hasParent()) {
         this.drawString(
            this.setting.isOpen() ? "-" : "+",
            this.x + this.width - 1.0F,
            this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(),
            ClickGui.getInstance().gear.getValue()
         );
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
         sound();
         this.setting.setOpen(!this.setting.isOpen());
      }
   }

   @Override
   public void toggle() {
      this.setting.setValue(!this.setting.getValue());
   }

   @Override
   public boolean getState() {
      return this.setting.getValue();
   }
}
