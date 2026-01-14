package dev.luminous.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.Mod;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.gui.items.buttons.ModuleButton;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.client.ClickGui;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class ClickGuiScreen extends Screen {
   private static ClickGuiScreen INSTANCE = new ClickGuiScreen();
   private final ArrayList<Component> components = new ArrayList();

   public ClickGuiScreen() {
      super(Text.literal("Alien"));
      this.setInstance();
      this.load();
   }
   
   public static ClickGuiScreen getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ClickGuiScreen();
      }

      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   
   private void load() {
      int x = -84;

      for (final Module.Category category : Module.Category.values()) {
          String var10004 = category.toString();
         x += 94;
         this.components.add(new Component(var10004, category, x, 4, true) {
            @Override
            public void setupItems() {
               for (Module module : Alien.MODULE.getModules()) {
                  if (module.getCategory().equals(category)) {
                     this.addButton(new ModuleButton(module));
                  }
               }
            }
         });
      }

      this.components.forEach(components -> components.getItems().sort(Comparator.comparing(Mod::getName)));
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      float a = (float)ClickGui.getInstance().alphaValue;
      float scale = 0.92F + 0.08F * a;
      float slideY = (1.0F - a) * 20.0F;
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);
      Item.context = context;
      this.renderBackground(context, mouseX, mouseY, delta);
      int minX = Integer.MAX_VALUE;
      int minY = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int maxY = Integer.MIN_VALUE;

      for (Component c : this.components) {
         minX = Math.min(minX, c.getX());
         minY = Math.min(minY, c.getY());
         maxX = Math.max(maxX, c.getX() + c.getWidth());
         maxY = Math.max(maxY, c.getY() + c.getHeight());
      }

      int margin = 16;
      int panelX = Math.max(8, minX - margin);
      int panelY = Math.max(6, minY - margin);
      int panelW = Math.min(context.getScaledWindowWidth() - panelX - 8, maxX - minX + margin * 2);
      int panelH = Math.min(context.getScaledWindowHeight() - panelY - 6, maxY - minY + margin * 2 + 24);
      boolean focused = mouseX >= panelX && mouseX <= panelX + panelW && mouseY >= panelY && mouseY <= panelY + panelH;
      int alpha = focused ? (int)Math.round(242.25) : (int)Math.round(226.95000000000002);
       //Alien.BLUR.applyBlur(30.0F, panelX, panelY, panelW, panelH);
      float r = 4.0F;
      context.getMatrices().push();
      context.getMatrices().translate(panelX + panelW / 2.0F, panelY + panelH / 2.0F + slideY, 0.0F);
      context.getMatrices().scale(scale, scale, 1.0F);
      context.getMatrices().translate(-(panelX + panelW / 2.0F), -(panelY + panelH / 2.0F), 0.0F);
      //Render2DUtil.drawRoundedRect(context.getMatrices(), panelX, panelY, panelW, panelH, r, new Color(255, 255, 255, alpha));
      int strokeA = Math.max(0, Math.min(255, (int)Math.round(alpha * 0.22)));
      //Render2DUtil.drawRoundedStroke(context.getMatrices(), panelX, panelY, panelW, panelH, r, new Color(220, 224, 230, strokeA), 48);
      context.getMatrices().pop();
      context.getMatrices().push();
      context.getMatrices().translate(0.0F, slideY, 0.0F);
      context.getMatrices().scale(scale, scale, 1.0F);
      this.components.forEach(components -> components.drawScreen(context, mouseX, mouseY, delta));
      context.getMatrices().pop();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int clickedButton) {
      this.components.forEach(components -> components.mouseClicked((int)mouseX, (int)mouseY, clickedButton));
      return super.mouseClicked(mouseX, mouseY, clickedButton);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int releaseButton) {
      this.components.forEach(components -> components.mouseReleased((int)mouseX, (int)mouseY, releaseButton));
      return super.mouseReleased(mouseX, mouseY, releaseButton);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (InputUtil.isKeyPressed(Wrapper.mc.getWindow().getHandle(), 340)) {
         if (verticalAmount < 0.0) {
            this.components.forEach(component -> component.setX(component.getX() - 15));
         } else if (verticalAmount > 0.0) {
            this.components.forEach(component -> component.setX(component.getX() + 15));
         }
      } else if (verticalAmount < 0.0) {
         this.components.forEach(component -> component.setY(component.getY() - 15));
      } else if (verticalAmount > 0.0) {
         this.components.forEach(component -> component.setY(component.getY() + 15));
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.components.forEach(component -> component.onKeyPressed(keyCode));
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      this.components.forEach(component -> component.onKeyTyped(chr, modifiers));
      return super.charTyped(chr, modifiers);
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   public final ArrayList<Component> getComponents() {
      return this.components;
   }

   public int getTextOffset() {
      return -ClickGui.getInstance().textOffset.getValueInt() - 6;
   }
}
