package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public class ModuleButton extends Button {
   private final Module module;
   private List<Item> items = new ArrayList();
   public boolean subOpen;
   public double itemHeight;
   public final Animation animation = new Animation();

   public ModuleButton(Module module) {
      super(module.getName());
      this.module = module;
      this.initSettings();
   }

   public void initSettings() {
      ArrayList<Item> newItems = new ArrayList();

      for (Setting setting : this.module.getSettings()) {
         if (setting instanceof BooleanSetting s) {
            newItems.add(new BooleanButton(s));
         }

         if (setting instanceof BindSetting s) {
            newItems.add(new BindButton(s));
         }

         if (setting instanceof StringSetting s) {
            newItems.add(new StringButton(s));
         }

         if (setting instanceof SliderSetting s) {
            newItems.add(new SliderButton(s));
         }

         if (setting instanceof EnumSetting<?> s) {
            newItems.add(new EnumButton(s));
         }

         if (setting instanceof ColorSetting s) {
            newItems.add(new PickerButton(s));
         }
      }

      this.items = newItems;
   }

   @Override
   public void update() {
      for (Item item : this.items) {
         item.update();
      }
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      boolean pressed = this.getState();
      Color accent = ClickGui.getInstance().activeColor.getValue();
      Color baseFill = pressed
         ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), Math.min(230, ClickGui.getInstance().hoverAlpha.getValueInt()))
         : (hovered ? new Color(246, 248, 250, ClickGui.getInstance().hoverAlpha.getValueInt()) : new Color(255, 255, 255, 232));
      Render2DUtil.drawRoundedRect(context.getMatrices(), this.x, this.y, this.width, this.height - 0.5F, 4.2F, baseFill);
      Render2DUtil.drawRoundedStroke(
         context.getMatrices(),
         this.x,
         this.y,
         this.width,
         this.height - 0.5F,
         4.2F,
         pressed ? new Color(255, 255, 255, 200) : new Color(220, 224, 230, 180),
         48
      );
      if (pressed) {
         float ih = this.height - 2.0F;
         Render2DUtil.drawGlow(context.getMatrices(), this.x - 2.0F, this.y - 2.0F, this.width + 4.0F, this.height + 4.0F, new Color(0, 0, 0, 20).getRGB());
         Render2DUtil.verticalGradient(
            context.getMatrices(), this.x + 2.0F, this.y + 2.0F, this.x + this.width - 2.0F, this.y + ih, new Color(255, 255, 255, 64), new Color(0, 0, 0, 56)
         );
         Render2DUtil.drawRoundedStroke(
            context.getMatrices(),
            this.x + 1.2F,
            this.y + 1.2F,
            this.width - 2.4F,
            ih - 1.2F,
            3.6F,
            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 150),
            96
         );
         Render2DUtil.drawRoundedStroke(
            context.getMatrices(), this.x + 2.0F, this.y + 2.0F, this.width - 4.0F, ih - 2.0F, 3.2F, new Color(255, 255, 255, 120), 96
         );
         Render2DUtil.drawRoundedStroke(context.getMatrices(), this.x + 2.0F, this.y + 2.8F, this.width - 4.0F, ih - 2.8F, 3.2F, new Color(0, 0, 0, 70), 96);
      } else if (hovered) {
         Render2DUtil.drawRoundedStroke(
            context.getMatrices(),
            this.x - 0.5F,
            this.y - 0.5F,
            this.width + 1.0F,
            this.height + 1.0F,
            4.2F,
            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160),
            96
         );
      }

      this.drawString(
         this.module.getDisplayName(),
         this.x + 2.3F,
         this.y - 2.0F - ClickGuiScreen.getInstance().getTextOffset(),
         this.getState() ? enableTextColor : defaultTextColor
      );
      if (ClickGui.getInstance().gear.booleanValue) {
         this.drawString(
            this.subOpen ? "-" : "+",
            this.x + this.width - 8.0F,
            this.y - 1.7F - ClickGuiScreen.getInstance().getTextOffset(),
            ClickGui.getInstance().gear.getValue().getRGB()
         );
      }

      if (this.subOpen || this.itemHeight > 0.0) {
         if (ClickGui.getInstance().line.getValue()) {
            double itemHeight = this.getItemHeight();
            int line = new Color(220, 224, 230, 160).getRGB();
            Render2DUtil.drawLine(
               context.getMatrices(), this.x + 0.6F, (float)(this.y + this.height + itemHeight - 0.5), this.x + 0.6F, this.y + this.height - 0.5F, line
            );
            Render2DUtil.drawLine(
               context.getMatrices(),
               this.x + this.width - 0.6F,
               (float)(this.y + this.height + itemHeight - 0.5),
               this.x + this.width - 0.6F,
               this.y + this.height - 0.5F,
               line
            );
            Render2DUtil.drawLine(
               context.getMatrices(),
               this.x + 0.6F,
               (float)(this.y + this.height + itemHeight - 0.5),
               this.x + this.width - 0.6F,
               (float)(this.y + this.height + itemHeight - 0.7F),
               line
            );
         }

         float height = this.height + 2;

         for (Item item : this.items) {
            if (!item.isHidden()) {
               item.setHeight(this.height);
               item.setLocation(this.x + 1.0F, this.y + height);
               item.setWidth(this.width - 9);
               item.drawScreen(context, mouseX, mouseY, partialTicks);
               height += item.getHeight() + 2;
            }
         }
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (!this.items.isEmpty()) {
         if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.subOpen = !this.subOpen;
            sound();
         }

         if (this.subOpen) {
            for (Item item : this.items) {
               if (!item.isHidden()) {
                  item.mouseClicked(mouseX, mouseY, mouseButton);
               }
            }
         }
      }
   }

   @Override
   public void onKeyTyped(char typedChar, int keyCode) {
      super.onKeyTyped(typedChar, keyCode);
      if (!this.items.isEmpty() && this.subOpen) {
         for (Item item : this.items) {
            if (!item.isHidden()) {
               item.onKeyTyped(typedChar, keyCode);
            }
         }
      }
   }

   @Override
   public void onKeyPressed(int key) {
      super.onKeyPressed(key);
      if (!this.items.isEmpty() && this.subOpen) {
         for (Item item : this.items) {
            if (!item.isHidden()) {
               item.onKeyPressed(key);
            }
         }
      }
   }

   public int getButtonHeight() {
      return super.getHeight();
   }

   public int getItemHeight() {
      int height = 3;

      for (Item item : this.items) {
         if (!item.isHidden()) {
            height += item.getHeight() + 2;
         }
      }

      return height;
   }

   @Override
   public int getHeight() {
      if (this.subOpen) {
         int height = super.getHeight();

         for (Item item : this.items) {
            if (!item.isHidden()) {
               height += item.getHeight() + 1;
            }
         }

         return height + 2;
      } else {
         return super.getHeight();
      }
   }

   public Module getModule() {
      return this.module;
   }

   @Override
   public void toggle() {
      this.module.toggle();
   }

   @Override
   public boolean getState() {
      return this.module.isOn();
   }
}
