package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class BedCrafter extends Module {
   public static BedCrafter INSTANCE;
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", false));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5, 0, 8));
   private final BooleanSetting disable = this.add(new BooleanSetting("Disable", true));

   public BedCrafter() {
      super("BedCrafter", Module.Category.Misc);
      this.setChinese("自动制作床");
      INSTANCE = this;
   }

   public static int getEmptySlots() {
      int emptySlots = 0;

      for (int i = 0; i < 36; i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (itemStack.isEmpty()) {
            emptySlots++;
         }
      }

      return emptySlots;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (getEmptySlots() == 0) {
         if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
            mc.player.closeHandledScreen();
         }

         if (this.disable.getValue()) {
            this.disable();
         }
      } else {
         if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
            boolean craft = false;

            for (RecipeResultCollection recipeResult : mc.player.getRecipeBook().getOrderedResults()) {
               for (RecipeEntry<?> recipe : recipeResult.getRecipes(true)) {
                  if (recipe.value().getResult(mc.world.getRegistryManager()).getItem() instanceof BedItem) {
                     for (int i = 0; i < getEmptySlots(); i++) {
                        craft = true;
                        mc.interactionManager.clickRecipe(mc.player.currentScreenHandler.syncId, recipe, false);
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 1, SlotActionType.QUICK_MOVE, mc.player);
                     }
                     break;
                  }
               }
            }

            if (!craft) {
               if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
                  mc.player.closeHandledScreen();
               }

               if (this.disable.getValue()) {
                  this.disable();
               }
            }
         } else {
            this.doPlace();
         }
      }
   }

   private void doPlace() {
      BlockPos bestPos = null;
      double distance = 100.0;
      boolean place = true;

      for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat())) {
         if (mc.world.getBlockState(pos).getBlock() == Blocks.CRAFTING_TABLE && BlockUtil.getClickSideStrict(pos) != null) {
            place = false;
            bestPos = pos;
            break;
         }

         if (BlockUtil.canPlace(pos) && (bestPos == null || MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < distance)) {
            bestPos = pos;
            distance = MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos()));
         }
      }

      if (bestPos != null) {
         if (!place) {
            BlockUtil.clickBlock(bestPos, BlockUtil.getClickSide(bestPos), this.rotate.getValue());
         } else {
            if (InventoryUtil.findItem(Items.CRAFTING_TABLE) == -1) {
               return;
            }

            int old = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(InventoryUtil.findItem(Items.CRAFTING_TABLE));
            BlockUtil.placeBlock(bestPos, this.rotate.getValue());
            InventoryUtil.switchToSlot(old);
         }
      }
   }
}
