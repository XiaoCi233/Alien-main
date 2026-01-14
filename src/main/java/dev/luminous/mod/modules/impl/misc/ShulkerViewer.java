package dev.luminous.mod.modules.impl.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.asm.accessors.IContainerComponent;
import dev.luminous.core.impl.PlayerManager;
import dev.luminous.mod.gui.PeekScreen;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Unique;

public class ShulkerViewer extends Module {
   private static final ItemStack[] ITEMS = new ItemStack[27];
   public static ShulkerViewer INSTANCE;
   private static int offset;
   public final BooleanSetting toolTips = this.add(new BooleanSetting("ToolTips", true));
   public final BooleanSetting icon = this.add(new BooleanSetting("Icon", true));
   private final HashMap<UUID, ShulkerViewer.Shulker> map = new HashMap();
   private final BooleanSetting peek = this.add(new BooleanSetting("Peek", false).setParent());
   private final SliderSetting renderTime = this.add(new SliderSetting("RenderTime", 10.0, 0.0, 100.0, 0.1, this.peek::isOpen).setSuffix("s"));
   private final SliderSetting xOffset = this.add(new SliderSetting("X", 0, 0, 1500, this.peek::isOpen));
   private final SliderSetting yOffset = this.add(new SliderSetting("Y", 120, 0, 1000, this.peek::isOpen));
   private final SliderSetting space = this.add(new SliderSetting("Space", 78.0, 0.0, 200.0, 1.0, this.peek::isOpen));

   public ShulkerViewer() {
      super("ShulkerViewer", Module.Category.Misc);
      this.setChinese("潜影盒查看");
      INSTANCE = this;
   }

   public static void renderShulkerToolTip(DrawContext context, int mouseX, int mouseY, ItemStack stack) {
      getItemsInContainerItem(stack, ITEMS);
      draw(context, mouseX, mouseY);
   }

   @Unique
   private static void draw(DrawContext context, int mouseX, int mouseY) {
      RenderSystem.disableDepthTest();
      GL11.glClear(256);
      mouseX += 8;
      mouseY -= 82;
      Render2DUtil.drawRect(context.getMatrices(), (float)mouseX, (float)mouseY, 176.0F, 67.0F, new Color(0, 0, 0, 120));
      DiffuseLighting.enableGuiDepthLighting();
      int row = 0;
      int i = 0;

      for (ItemStack itemStack : ITEMS) {
         context.drawItem(itemStack, mouseX + 8 + i * 18, mouseY + 7 + row * 18);
         context.drawItemInSlot(mc.textRenderer, itemStack, mouseX + 8 + i * 18, mouseY + 7 + row * 18);
         if (++i >= 9) {
            i = 0;
            row++;
         }
      }

      DiffuseLighting.disableGuiDepthLighting();
      RenderSystem.enableDepthTest();
   }

   public static boolean hasItems(ItemStack itemStack) {
      IContainerComponent container = (IContainerComponent)(Object)itemStack.get(DataComponentTypes.CONTAINER);
      if (container != null && !container.getStacks().isEmpty()) {
         return true;
      } else {
         NbtCompound compoundTag = ((NbtComponent)itemStack.getOrDefault(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.DEFAULT)).getNbt();
         return compoundTag != null && compoundTag.contains("Items", 9);
      }
   }

   public static void getItemsInContainerItem(ItemStack itemStack, ItemStack[] items) {
      if (itemStack.getItem() == Items.ENDER_CHEST) {
         for (int i = 0; i < Alien.PLAYER.ENDERCHEST_ITEM.size(); i++) {
            items[i] = (ItemStack)Alien.PLAYER.ENDERCHEST_ITEM.get(i);
         }
      } else {
         Arrays.fill(items, ItemStack.EMPTY);
         ComponentMap components = itemStack.getComponents();
         if (components.contains(DataComponentTypes.CONTAINER)) {
            IContainerComponent container = (IContainerComponent)(Object)components.get(DataComponentTypes.CONTAINER);
            DefaultedList<ItemStack> stacks = container.getStacks();

            for (int i = 0; i < stacks.size(); i++) {
               if (i >= 0 && i < items.length) {
                  items[i] = (ItemStack)stacks.get(i);
               }
            }
         } else if (components.contains(DataComponentTypes.BLOCK_ENTITY_DATA)) {
            NbtComponent nbt2 = (NbtComponent)components.get(DataComponentTypes.BLOCK_ENTITY_DATA);
            if (nbt2.contains("Items")) {
               NbtList nbt3 = (NbtList)nbt2.getNbt().get("Items");

               for (int ix = 0; ix < nbt3.size(); ix++) {
                  int slot = nbt3.getCompound(ix).getByte("Slot");
                  if (slot >= 0 && slot < items.length) {
                     items[slot] = ItemStack.fromNbtOrEmpty(mc.player.getRegistryManager(), nbt3.getCompound(ix));
                  }
               }
            }
         }
      }
   }

   public static boolean openContainer(ItemStack itemStack, ItemStack[] contents, boolean pause) {
      if (!hasItems(itemStack) && itemStack.getItem() != Items.ENDER_CHEST) {
         return false;
      } else {
         getItemsInContainerItem(itemStack, contents);
         if (pause) {
            PlayerManager.screenToOpen = new PeekScreen(itemStack, contents);
         } else {
            mc.setScreen(new PeekScreen(itemStack, contents));
         }

         return true;
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.peek.getValue()) {
         for (AbstractClientPlayerEntity player : Alien.THREAD.getPlayers()) {
            ItemStack stack = player.getMainHandStack();
            if (!(stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock)) {
               stack = player.getOffHandStack();
            }

            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
               this.map.put(player.getGameProfile().getId(), new ShulkerViewer.Shulker(stack, player.getGameProfile().getName()));
            }
         }
      }
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (this.peek.getValue()) {
         offset = 0;
         this.map.values().removeIf(shulker -> shulker.draw(drawContext));
      }
   }

   class Shulker {
      final ItemStack itemStack;
      final String name;
      private final Timer timer;

      public Shulker(ItemStack itemStack, String name) {
         this.itemStack = itemStack;
         this.timer = new Timer();
         this.name = name;
      }

      public boolean draw(DrawContext context) {
         if (this.timer.passedS(ShulkerViewer.this.renderTime.getValue())) {
            return true;
         } else {
            ShulkerViewer.renderShulkerToolTip(
               context, ShulkerViewer.this.xOffset.getValueInt() - 8, ShulkerViewer.this.yOffset.getValueInt() + ShulkerViewer.offset, this.itemStack
            );
            context.drawText(
               Wrapper.mc.textRenderer,
               this.name,
               ShulkerViewer.this.xOffset.getValueInt(),
               ShulkerViewer.this.yOffset.getValueInt() + ShulkerViewer.offset - 9 - 82,
               -1,
               true
            );
            ShulkerViewer.offset = ShulkerViewer.offset + ShulkerViewer.this.space.getValueInt();
            return false;
         }
      }
   }
}
