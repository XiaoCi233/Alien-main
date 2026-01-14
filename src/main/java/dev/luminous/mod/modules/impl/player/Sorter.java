package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.core.Manager;
import dev.luminous.mod.gui.windows.WindowsScreen;
import dev.luminous.mod.gui.windows.impl.ItemSelectWindow;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.apache.commons.io.IOUtils;

public class Sorter extends Module {
   public static Sorter INSTANCE;
   final int[] stealCountList = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
   private final SliderSetting tasksPerTicks = this.add(new SliderSetting("TasksPerTick", 2, 1, 20));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 0.1, 0.0, 5.0, 0.01).setSuffix("s"));
   private final BooleanSetting stack = this.add(new BooleanSetting("Stack", true));
   private final EnumSetting<Sorter.Mode> trashMode = this.add(new EnumSetting("TrashMode", Sorter.Mode.Whitelist));
   private final BooleanSetting edit = this.add(new BooleanSetting("EditTrash", false).injectTask(this::openGui));
   private final BooleanSetting sort = this.add(new BooleanSetting("Sort", true));
   private final BooleanSetting kit = this.add(new BooleanSetting("Kit", false).injectTask(this::onEnable));
   private final StringSetting kitName = this.add(new StringSetting("KitName", "kit1"));
   private final BooleanSetting drop = this.add(new BooleanSetting("Drop", true).setParent());
   private final BooleanSetting trash = this.add(new BooleanSetting("Trash", true, this.drop::isOpen));
   private final BooleanSetting rename = this.add(new BooleanSetting("Rename", true, this.drop::isOpen));
   private final BooleanSetting kitExceed = this.add(new BooleanSetting("KitExceed", true, this.drop::isOpen));
   private final BooleanSetting exceed = this.add(new BooleanSetting("Exceed", true, this.drop::isOpen));
   private final SliderSetting crystal = this.add(new SliderSetting("Crystal", 4, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting exp = this.add(new SliderSetting("Exp", 4, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting totem = this.add(new SliderSetting("Totem", 6, 0, 36, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting eGapple = this.add(new SliderSetting("EGapple", 2, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting gapple = this.add(new SliderSetting("Gapple", 2, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting obsidian = this.add(new SliderSetting("Obsidian", 1, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting web = this.add(new SliderSetting("Web", 1, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting glowstone = this.add(new SliderSetting("Glowstone", 1, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting anchor = this.add(new SliderSetting("Anchor", 1, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting pearl = this.add(new SliderSetting("Pearl", 1, 0, 8, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting piston = this.add(new SliderSetting("Piston", 1, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting redstone = this.add(new SliderSetting("RedStone", 1, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting ladder = this.add(new SliderSetting("Ladder", 2, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting bed = this.add(new SliderSetting("Bed", 4, 0, 12, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting speed = this.add(new SliderSetting("Speed", 1, 0, 8, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting turtle = this.add(new SliderSetting("Resistance", 1, 0, 8, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final SliderSetting strength = this.add(new SliderSetting("Strength", 1, 0, 8, () -> this.drop.isOpen() && this.exceed.isOpen()));
   private final Timer timer = new Timer();
   private final Map<Integer, String> kitMap = new ConcurrentHashMap();

   public Sorter() {
      super("Sorter", Module.Category.Player);
      this.setChinese("背包整理");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         Alien.THREAD.execute(() -> {
            this.kitMap.clear();

            try {
               File file = Manager.getFile(this.kitName.getValue() + ".kit");
               if (!file.exists()) {
                  return;
               }

               for (String s : IOUtils.readLines(new FileInputStream(file), StandardCharsets.UTF_8)) {
                  String[] split = s.split(":");
                  if (split.length != 2) {
                     return;
                  }

                  this.kitMap.put(Integer.valueOf(split[0]), split[1]);
               }
            } catch (Exception var6) {
               var6.printStackTrace();
            }
         });
      }
   }

   public static int getItemCount(Item item) {
      int count = 0;

      for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
         if (entry.getValue().getItem() == item) {
            count++;
         }
      }

      if (mc.player.getOffHandStack().getItem() == item) {
         count++;
      }

      return count;
   }

   public static int getItemCount(Class<?> clazz) {
      int count = 0;

      for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
         if (entry.getValue().getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)entry.getValue().getItem()).getBlock())) {
            count++;
         }
      }

      return count;
   }

   private void openGui() {
      this.edit.setValueWithoutTask(false);
      if (!nullCheck()) {
         mc.setScreen(new WindowsScreen(new ItemSelectWindow(Alien.CLEANER)));
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.timer.passedS(this.delay.getValue())) {
         if (EntityUtil.inInventory()) {
            if (this.exceed.getValue()) {
               this.updateItem();
            }

            for (int i = 0; i < this.tasksPerTicks.getValue(); i++) {
               this.tweak();
            }
         }
      }
   }

   private void tweak() {
      if (this.drop.getValue()) {
         for (int slot1 = 35; slot1 >= 0; slot1--) {
            ItemStack stack = mc.player.getInventory().getStack(slot1);
            if (!stack.isEmpty() && this.shouldDrop(stack)) {
               this.timer.reset();
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot1 < 9 ? slot1 + 36 : slot1, 1, SlotActionType.THROW, mc.player);
               return;
            }
         }
      }

      if (this.stack.getValue()) {
         for (int slot1x = 35; slot1x >= 9; slot1x--) {
            ItemStack stack = mc.player.getInventory().getStack(slot1x);
            if (!stack.isEmpty() && stack.isStackable() && stack.getCount() != stack.getMaxCount()) {
               for (int slot2 = 0; slot2 < 36; slot2++) {
                  if (slot1x != slot2) {
                     ItemStack stack2 = mc.player.getInventory().getStack(slot2);
                     if (stack2.getCount() != stack2.getMaxCount() && canMerge(stack, stack2)) {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1x, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot2 < 9 ? slot2 + 36 : slot2, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1x, 0, SlotActionType.PICKUP, mc.player);
                        this.timer.reset();
                        return;
                     }
                  }
               }
            }
         }
      }

      if (this.drop.getValue()) {
         for (int slot1xx = 35; slot1xx >= 0; slot1xx--) {
            ItemStack stack = mc.player.getInventory().getStack(slot1xx);
            if (!stack.isEmpty() && this.exceed.getValue() && this.exceed(stack, false)) {
               this.timer.reset();
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot1xx < 9 ? slot1xx + 36 : slot1xx, 1, SlotActionType.THROW, mc.player);
               return;
            }
         }
      }

      if (this.sort.getValue()) {
         if (this.kit.getValue()) {
            for (int slot1xxx = 0; slot1xxx < 36; slot1xxx++) {
               if (this.kitMap.containsKey(slot1xxx)) {
                  String target = (String)this.kitMap.get(slot1xxx);
                  String name = mc.player.getInventory().getStack(slot1xxx).getItem().getTranslationKey();
                  if (!name.equals(target)) {
                     for (int slot2x = 0; slot2x < 36; slot2x++) {
                        String slot2Target = (String)this.kitMap.get(slot2x);
                        ItemStack stack = mc.player.getInventory().getStack(slot2x);
                        if (!stack.isEmpty()) {
                           String itemID = stack.getItem().getTranslationKey();
                           if (!itemID.equals(slot2Target) && itemID.equals(target)) {
                              mc.interactionManager
                                 .clickSlot(mc.player.playerScreenHandler.syncId, slot1xxx < 9 ? slot1xxx + 36 : slot1xxx, 0, SlotActionType.PICKUP, mc.player);
                              mc.interactionManager
                                 .clickSlot(mc.player.playerScreenHandler.syncId, slot2x < 9 ? slot2x + 36 : slot2x, 0, SlotActionType.PICKUP, mc.player);
                              mc.interactionManager
                                 .clickSlot(mc.player.playerScreenHandler.syncId, slot1xxx < 9 ? slot1xxx + 36 : slot1xxx, 0, SlotActionType.PICKUP, mc.player);
                              this.timer.reset();
                              return;
                           }
                        }
                     }
                  }
               }
            }
         } else {
            for (int slot1xxxx = 9; slot1xxxx < 36; slot1xxxx++) {
               int id = Item.getRawId(mc.player.getInventory().getStack(slot1xxxx).getItem());
               if (mc.player.getInventory().getStack(slot1xxxx).isEmpty()) {
                  id = 114514;
               }

               int minId = this.getMinId(slot1xxxx, id);
               if (minId < id) {
                  for (int slot2xx = 35; slot2xx > slot1xxxx; slot2xx--) {
                     ItemStack stack = mc.player.getInventory().getStack(slot2xx);
                     if (!stack.isEmpty()) {
                        int itemID = Item.getRawId(stack.getItem());
                        if (itemID == minId) {
                           mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1xxxx, 0, SlotActionType.PICKUP, mc.player);
                           mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot2xx, 0, SlotActionType.PICKUP, mc.player);
                           mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1xxxx, 0, SlotActionType.PICKUP, mc.player);
                           this.timer.reset();
                           return;
                        }
                     }
                  }
               }
            }
         }
      }

      if (this.drop.getValue() && this.kitExceed.getValue()) {
         for (int slot1xxxx = 35; slot1xxxx >= 0; slot1xxxx--) {
            if (this.kitMap.containsKey(slot1xxxx)) {
               ItemStack stack = mc.player.getInventory().getStack(slot1xxxx);
               if ((!this.exceed.getValue() || this.exceed(stack, true))
                  && !stack.isEmpty()
                  && !(stack.getItem() instanceof ArmorItem)
                  && !(stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock)
                  && !stack.getItem().getTranslationKey().equals(this.kitMap.get(slot1xxxx))) {
                  this.timer.reset();
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot1xxxx < 9 ? slot1xxxx + 36 : slot1xxxx, 1, SlotActionType.THROW, mc.player);
                  return;
               }
            }
         }
      }
   }

   private boolean shouldDrop(ItemStack stack) {
      Item item = stack.getItem();
      if (this.trash.getValue() && !(item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock)) {
         boolean inList = Alien.CLEANER.inList(item.getTranslationKey());
         if (!inList && this.trashMode.is(Sorter.Mode.Whitelist) || inList && this.trashMode.is(Sorter.Mode.Blacklist)) {
            return true;
         }
      }

      return !this.rename.getValue() ? false : stack.isStackable() && !stack.getName().getString().equals(item.getName().getString());
   }

   private boolean exceed(ItemStack i, boolean dropOther) {
      if (i.getItem().equals(Items.END_CRYSTAL)) {
         if (this.stealCountList[0] > this.crystal.getValue()) {
            this.stealCountList[0]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Items.EXPERIENCE_BOTTLE)) {
         if (this.stealCountList[1] > this.exp.getValue()) {
            this.stealCountList[1]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Items.TOTEM_OF_UNDYING)) {
         if (this.stealCountList[2] > this.totem.getValue()) {
            this.stealCountList[2]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Items.ENCHANTED_GOLDEN_APPLE)) {
         if (this.stealCountList[3] > this.eGapple.getValue()) {
            this.stealCountList[3]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Blocks.OBSIDIAN.asItem())) {
         if (this.stealCountList[4] > this.obsidian.getValue()) {
            this.stealCountList[4]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Blocks.COBWEB.asItem())) {
         if (this.stealCountList[5] > this.web.getValue()) {
            this.stealCountList[5]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Blocks.GLOWSTONE.asItem())) {
         if (this.stealCountList[6] > this.glowstone.getValue()) {
            this.stealCountList[6]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Blocks.RESPAWN_ANCHOR.asItem())) {
         if (this.stealCountList[7] > this.anchor.getValue()) {
            this.stealCountList[7]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Items.ENDER_PEARL)) {
         if (this.stealCountList[8] > this.pearl.getValue()) {
            this.stealCountList[8]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof PistonBlock) {
         if (this.stealCountList[9] > this.piston.getValue()) {
            this.stealCountList[9]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem().equals(Blocks.REDSTONE_BLOCK.asItem())) {
         if (this.stealCountList[10] > this.redstone.getValue()) {
            this.stealCountList[10]--;
            return true;
         } else {
            return false;
         }
      } else if (i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof BedBlock) {
         if (this.stealCountList[11] > this.bed.getValue()) {
            this.stealCountList[11]--;
            return true;
         } else {
            return false;
         }
      } else {
         if (Item.getRawId(i.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = (PotionContentsComponent)i.getOrDefault(
               DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT
            );

            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == StatusEffects.SPEED.value()) {
                  if (this.stealCountList[12] > this.speed.getValue()) {
                     this.stealCountList[12]--;
                     return true;
                  }

                  return false;
               }

               if (effect.getEffectType().value() == StatusEffects.RESISTANCE.value()) {
                  if (this.stealCountList[13] > this.turtle.getValue()) {
                     this.stealCountList[13]--;
                     return true;
                  }

                  return false;
               }

               if (effect.getEffectType().value() == StatusEffects.STRENGTH.value()) {
                  if (this.stealCountList[16] > this.strength.getValue()) {
                     this.stealCountList[16]--;
                     return true;
                  }

                  return false;
               }
            }
         }

         if (i.getItem().equals(Items.GOLDEN_APPLE) && this.stealCountList[14] > this.gapple.getValue()) {
            this.stealCountList[14]--;
            return true;
         } else if (i.getItem() instanceof BlockItem
            && ((BlockItem)i.getItem()).getBlock() instanceof LadderBlock
            && this.stealCountList[15] > this.ladder.getValue()) {
            this.stealCountList[15]--;
            return true;
         } else {
            return dropOther;
         }
      }
   }

   private void updateItem() {
      this.stealCountList[0] = getItemCount(Items.END_CRYSTAL);
      this.stealCountList[1] = getItemCount(Items.EXPERIENCE_BOTTLE);
      this.stealCountList[2] = getItemCount(Items.TOTEM_OF_UNDYING);
      this.stealCountList[3] = getItemCount(Items.ENCHANTED_GOLDEN_APPLE);
      this.stealCountList[4] = getItemCount(Items.OBSIDIAN);
      this.stealCountList[5] = getItemCount(Items.COBWEB);
      this.stealCountList[6] = getItemCount(Items.GLOWSTONE);
      this.stealCountList[7] = getItemCount(Items.RESPAWN_ANCHOR);
      this.stealCountList[8] = getItemCount(Items.ENDER_PEARL);
      this.stealCountList[9] = getItemCount(Items.PISTON) - getItemCount(Items.STICKY_PISTON);
      this.stealCountList[10] = getItemCount(Items.REDSTONE_BLOCK);
      this.stealCountList[11] = getItemCount(BedBlock.class);
      this.stealCountList[12] = InventoryUtil.getPotionCount((StatusEffect)StatusEffects.SPEED.value());
      this.stealCountList[13] = InventoryUtil.getPotionCount((StatusEffect)StatusEffects.RESISTANCE.value());
      this.stealCountList[14] = getItemCount(Items.GOLDEN_APPLE);
      this.stealCountList[15] = getItemCount(LadderBlock.class);
      this.stealCountList[16] = InventoryUtil.getPotionCount((StatusEffect)StatusEffects.STRENGTH.value());
   }

   private int getMinId(int slot, int currentId) {
      int id = currentId;

      for (int slot1 = slot + 1; slot1 < 36; slot1++) {
         ItemStack stack = mc.player.getInventory().getStack(slot1);
         if (!stack.isEmpty()) {
            int itemID = Item.getRawId(stack.getItem());
            if (itemID < id) {
               id = itemID;
            }
         }
      }

      return id;
   }

   public static boolean canMerge(ItemStack source, ItemStack stack) {
      return ItemStack.areItemsAndComponentsEqual(source, stack);
   }

   private static enum Mode {
      Whitelist,
      Blacklist;
   }
}
