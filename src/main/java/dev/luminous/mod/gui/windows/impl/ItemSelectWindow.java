package dev.luminous.mod.gui.windows.impl;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.Manager;
import dev.luminous.core.impl.CleanerManager;
import dev.luminous.core.impl.FontManager;
import dev.luminous.core.impl.TradeManager;
import dev.luminous.core.impl.XrayManager;
import dev.luminous.mod.gui.items.buttons.StringButton;
import dev.luminous.mod.gui.windows.WindowBase;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.StringHelper;

public class ItemSelectWindow extends WindowBase {
   private final Manager manager;
   private final ArrayList<ItemSelectWindow.ItemPlate> itemPlates = new ArrayList();
   private final ArrayList<ItemSelectWindow.ItemPlate> allItems = new ArrayList();
   private boolean allTab = true;
   private boolean listening = false;
   private String search = "Search";

   public ItemSelectWindow(Manager manager) {
      this(Wrapper.mc.getWindow().getScaledWidth() / 2.0F - 100.0F, Wrapper.mc.getWindow().getScaledHeight() / 2.0F - 150.0F, 200.0F, 300.0F, manager);
   }

   public ItemSelectWindow(float x, float y, float width, float height, Manager manager) {
      super(x, y, width, height, "Items", null);
      this.manager = manager;
      this.refreshItemPlates();
      int id1 = 0;

      for (Block block : Registries.BLOCK) {
         this.allItems.add(new ItemSelectWindow.ItemPlate((float)id1, (float)(id1 * 20), block.asItem(), block.getTranslationKey()));
         id1++;
      }

      for (Item item : Registries.ITEM) {
         this.allItems.add(new ItemSelectWindow.ItemPlate((float)id1, (float)(id1 * 20), item, item.getTranslationKey()));
         id1++;
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY) {
      super.render(context, mouseX, mouseY);
      boolean hover1 = Render2DUtil.isHovered(mouseX, mouseY, this.getX() + this.getWidth() - 90.0F, this.getY() + 3.0F, 70.0, 10.0);
      Render2DUtil.drawRect(
         context.getMatrices(),
         this.getX() + this.getWidth() - 90.0F,
         this.getY() + 3.0F,
         70.0F,
         10.0F,
         hover1 ? new Color(-981236861, true) : new Color(-984131753, true)
      );
      FontManager.small.drawString(context.getMatrices(), this.search, this.getX() + this.getWidth() - 86.0F, this.getY() + 7.0F, new Color(14013909).getRGB());
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      int tabColor1 = this.allTab ? new Color(14013909).getRGB() : Color.GRAY.getRGB();
      int tabColor2 = this.allTab ? Color.GRAY.getRGB() : new Color(12434877).getRGB();
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(this.getX() + 1.5F, this.getY() + 29.0F, 0.0F).color(Color.DARK_GRAY.getRGB());
      bufferBuilder.vertex(this.getX() + 8.0F, this.getY() + 29.0F, 0.0F).color(tabColor1);
      bufferBuilder.vertex(this.getX() + 8.0F, this.getY() + 19.0F, 0.0F).color(tabColor1);
      bufferBuilder.vertex(this.getX() + 48.0F, this.getY() + 19.0F, 0.0F).color(tabColor1);
      bufferBuilder.vertex(this.getX() + 54.0F, this.getY() + 29.0F, 0.0F).color(tabColor1);
      bufferBuilder.vertex(this.getX() + 52.0F, this.getY() + 25.0F, 0.0F).color(tabColor2);
      bufferBuilder.vertex(this.getX() + 52.0F, this.getY() + 19.0F, 0.0F).color(tabColor2);
      bufferBuilder.vertex(this.getX() + 92.0F, this.getY() + 19.0F, 0.0F).color(tabColor2);
      bufferBuilder.vertex(this.getX() + 100.0F, this.getY() + 29.0F, 0.0F).color(Color.GRAY.getRGB());
      bufferBuilder.vertex(this.getX() + this.getWidth() - 1.0F, this.getY() + 29.0F, 0.0F).color(Color.DARK_GRAY.getRGB());
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      FontManager.small.drawString(context.getMatrices(), "All", this.getX() + 25.0F, this.getY() + 25.0F, tabColor1);
      FontManager.small.drawString(context.getMatrices(), "Selected", this.getX() + 60.0F, this.getY() + 25.0F, tabColor2);
      if (!this.allTab && this.itemPlates.isEmpty()) {
         FontManager.ui
            .drawCenteredString(
               context.getMatrices(),
               "It's empty here yet",
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F,
               new Color(12434877).getRGB()
            );
      }

      context.enableScissor((int)this.getX(), (int)(this.getY() + 30.0F), (int)(this.getX() + this.getWidth()), (int)(this.getY() + this.getHeight() - 1.0F));

      for (ItemSelectWindow.ItemPlate itemPlate : this.allTab ? this.allItems : this.itemPlates) {
         if (!(itemPlate.offset + this.getY() + 25.0F + this.getScrollOffset() > this.getY() + this.getHeight())
            && !(itemPlate.offset + this.getScrollOffset() + this.getY() + 10.0F < this.getY())) {
            context.getMatrices().push();
            context.getMatrices().translate(this.getX() + 6.0F, itemPlate.offset + this.getY() + 32.0F + this.getScrollOffset(), 0.0F);
            context.drawItem(itemPlate.item().getDefaultStack(), 0, 0);
            context.getMatrices().pop();
            FontManager.ui
               .drawString(
                  context.getMatrices(),
                  I18n.translate(itemPlate.key(), new Object[0]),
                  this.getX() + 26.0F,
                  itemPlate.offset + this.getY() + 38.0F + this.getScrollOffset(),
                  new Color(12434877).getRGB()
               );
            boolean hover2 = Render2DUtil.isHovered(
               mouseX, mouseY, this.getX() + this.getWidth() - 20.0F, itemPlate.offset + this.getY() + 35.0F + this.getScrollOffset(), 11.0, 11.0
            );
            Render2DUtil.drawRect(
               context.getMatrices(),
               this.getX() + this.getWidth() - 20.0F,
               itemPlate.offset + this.getY() + 35.0F + this.getScrollOffset(),
               11.0F,
               11.0F,
               hover2 ? new Color(-981828998, true) : new Color(-984131753, true)
            );
            boolean selected = this.itemPlates.stream().anyMatch(sI -> Objects.equals(sI.key, itemPlate.key));
            if (this.allTab && !selected) {
               FontManager.ui
                  .drawString(
                     context.getMatrices(), "+", this.getX() + this.getWidth() - 17.0F, itemPlate.offset + this.getY() + 37.0F + this.getScrollOffset(), -1
                  );
            } else {
               FontManager.ui
                  .drawString(
                     context.getMatrices(), "-", this.getX() + this.getWidth() - 16.5, itemPlate.offset + this.getY() + 37.5 + this.getScrollOffset(), -1
                  );
            }
         }
      }

      this.setMaxElementsHeight((this.allTab ? this.allItems : this.itemPlates).size() * 20);
      context.disableScissor();
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      super.mouseClicked(mouseX, mouseY, button);
      if (Render2DUtil.isHovered(mouseX, mouseY, this.getX() + 8.0F, this.getY() + 19.0F, 52.0, 19.0)) {
         this.allTab = true;
         this.resetScroll();
      }

      if (Render2DUtil.isHovered(mouseX, mouseY, this.getX() + 54.0F, this.getY() + 19.0F, 70.0, 19.0)) {
         this.allTab = false;
         this.resetScroll();
      }

      if (Render2DUtil.isHovered(mouseX, mouseY, this.getX() + this.getWidth() - 90.0F, this.getY() + 3.0F, 70.0, 10.0)) {
         this.listening = true;
         this.search = "";
      }

      for (ItemSelectWindow.ItemPlate itemPlate : Lists.newArrayList(this.allTab ? this.allItems : this.itemPlates)) {
         if (!((int)(itemPlate.offset + this.getY() + 50.0F) + this.getScrollOffset() > this.getY() + this.getHeight())) {
            String name = itemPlate.key().replace("item.minecraft.", "").replace("block.minecraft.", "");
            if (Render2DUtil.isHovered(
               mouseX, mouseY, this.getX() + this.getWidth() - 20.0F, itemPlate.offset + this.getY() + 35.0F + this.getScrollOffset(), 10.0, 10.0
            )) {
               boolean selected = this.itemPlates.stream().anyMatch(sI -> Objects.equals(sI.key(), itemPlate.key));
               if (this.allTab && !selected) {
                  if (this.manager instanceof TradeManager m) {
                     if (!m.inWhitelist(name)) {
                        m.add(name);
                        this.refreshItemPlates();
                     }
                  } else if (this.manager instanceof CleanerManager mx) {
                     if (!mx.inList(name)) {
                        mx.add(name);
                        this.refreshItemPlates();
                     }
                  } else if (this.manager instanceof XrayManager mxx && !mxx.inWhitelist(name)) {
                     mxx.add(name);
                     this.refreshItemPlates();
                  }
               } else {
                  if (this.manager instanceof TradeManager mxx) {
                     mxx.remove(name);
                  } else if (this.manager instanceof CleanerManager mxx) {
                     mxx.remove(name);
                  } else if (this.manager instanceof XrayManager mxx) {
                     mxx.remove(name);
                  }

                  this.refreshItemPlates();
               }
            }
         }
      }
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 70 || !InputUtil.isKeyPressed(Wrapper.mc.getWindow().getHandle(), 341) && !InputUtil.isKeyPressed(Wrapper.mc.getWindow().getHandle(), 345)
         )
       {
         if (this.listening) {
            switch (keyCode) {
               case 32:
                  this.search = this.search + " ";
                  break;
               case 256:
                  this.listening = false;
                  this.search = "Search";
                  this.refreshAllItems();
                  break;
               case 259:
                  this.search = StringButton.removeLastChar(this.search);
                  this.refreshAllItems();
                  if (Objects.equals(this.search, "")) {
                     this.listening = false;
                     this.search = "Search";
                  }
            }
         }
      } else {
         this.listening = !this.listening;
      }
   }

   @Override
   public void charTyped(char key, int keyCode) {
      if (StringHelper.isValidChar(key) && this.listening) {
         this.search = this.search + key;
         this.refreshAllItems();
      }
   }

   private void refreshItemPlates() {
      this.itemPlates.clear();
      int id = 0;

      for (Item item : Registries.ITEM) {
         if (this.manager instanceof TradeManager m) {
            if (m.inWhitelist(item.getTranslationKey())) {
               this.itemPlates.add(new ItemSelectWindow.ItemPlate((float)id, (float)(id * 20), item.asItem(), item.getTranslationKey()));
               id++;
            }
         } else if (this.manager instanceof CleanerManager mx) {
            if (mx.inList(item.getTranslationKey())) {
               this.itemPlates.add(new ItemSelectWindow.ItemPlate((float)id, (float)(id * 20), item.asItem(), item.getTranslationKey()));
               id++;
            }
         } else if (this.manager instanceof XrayManager mxx && mxx.inWhitelist(item.getTranslationKey())) {
            this.itemPlates.add(new ItemSelectWindow.ItemPlate((float)id, (float)(id * 20), item.asItem(), item.getTranslationKey()));
            id++;
         }
      }
   }

   private void refreshAllItems() {
      this.allItems.clear();
      this.resetScroll();
      int id1 = 0;

      for (Item item : Registries.ITEM) {
         if (this.search.equals("Search")
            || this.search.isEmpty()
            || item.getTranslationKey().contains(this.search)
            || item.getName().getString().toLowerCase().contains(this.search.toLowerCase())) {
            this.allItems.add(new ItemSelectWindow.ItemPlate((float)id1, (float)(id1 * 20), item, item.getTranslationKey()));
            id1++;
         }
      }
   }

   private record ItemPlate(float id, float offset, Item item, String key) {
   }
}
