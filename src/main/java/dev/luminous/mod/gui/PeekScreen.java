package dev.luminous.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.impl.misc.ShulkerViewer;
import java.awt.Color;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.screen.ingame.BookScreen.Contents;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

public class PeekScreen extends ShulkerBoxScreen {
   private final Identifier TEXTURE = Identifier.of("textures/gui/container/shulker_box.png");
   private final ItemStack[] contents;
   private final ItemStack storageBlock;

   public PeekScreen(ItemStack storageBlock, ItemStack[] contents) {
      super(
         new ShulkerBoxScreenHandler(0, Wrapper.mc.player.getInventory(), new SimpleInventory(contents)),
         Wrapper.mc.player.getInventory(),
         storageBlock.getName()
      );
      this.contents = contents;
      this.storageBlock = storageBlock;
   }

   public static Color getShulkerColor(ItemStack shulkerItem) {
      if (shulkerItem.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock shulkerBlock) {
         DyeColor dye = shulkerBlock.getColor();
         if (dye == null) {
            return Color.WHITE;
         } else {
            int color = dye.getEntityColor();
            return new Color(color);
         }
      } else {
         return Color.WHITE;
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 2 && this.focusedSlot != null && !this.focusedSlot.getStack().isEmpty() && Wrapper.mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
         ItemStack itemStack = this.focusedSlot.getStack();
         if (ShulkerViewer.hasItems(itemStack) || itemStack.getItem() == Items.ENDER_CHEST) {
            return ShulkerViewer.openContainer(this.focusedSlot.getStack(), this.contents, false);
         }

         if (itemStack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT) != null
            || itemStack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT) != null) {
            this.close();
            Wrapper.mc.setScreen(new BookScreen(Contents.create(itemStack)));
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return false;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 256 && !Wrapper.mc.options.inventoryKey.matchesKey(keyCode, scanCode)) {
         return false;
      } else {
         this.close();
         return true;
      }
   }

   @Override
   public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.close();
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
      Color color = getShulkerColor(this.storageBlock);
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      RenderSystem.setShaderColor(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
      int i = (this.width - this.backgroundWidth) / 2;
      int j = (this.height - this.backgroundHeight) / 2;
      context.drawTexture(this.TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
