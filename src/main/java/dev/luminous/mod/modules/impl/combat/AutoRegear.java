package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class AutoRegear extends Module {
   public static AutoRegear INSTANCE;
   public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   public final Timer timeoutTimer = new Timer();
   final int[] stealCountList = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final SliderSetting disableTime = this.add(new SliderSetting("DisableTime", 500, 0, 1000));
   private final BooleanSetting place = this.add(new BooleanSetting("Place", true));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting preferOpen = this.add(new BooleanSetting("PerferOpen", true));
   private final BooleanSetting open = this.add(new BooleanSetting("Open", true));
   private final SliderSetting range = this.add(new SliderSetting("MaxRange", 4.0, 0.0, 6.0, 0.1));
   private final SliderSetting minRange = this.add(new SliderSetting("MinRange", 1.0, 0.0, 3.0, 0.1));
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final BooleanSetting take = this.add(new BooleanSetting("Take", true));
   private final BooleanSetting smart = this.add(new BooleanSetting("Smart", true, this.take::getValue).setParent());
   private final BooleanSetting forceMove = this.add(new BooleanSetting("ForceQuickMove", true, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting crystal = this.add(new SliderSetting("Crystal", 256, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting exp = this.add(new SliderSetting("Exp", 256, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting totem = this.add(new SliderSetting("Totem", 6, 0, 36, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting gapple = this.add(new SliderSetting("Gapple", 128, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting obsidian = this.add(new SliderSetting("Obsidian", 64, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting web = this.add(new SliderSetting("Web", 64, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting glowstone = this.add(new SliderSetting("Glowstone", 128, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting anchor = this.add(new SliderSetting("Anchor", 128, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting pearl = this.add(new SliderSetting("Pearl", 16, 0, 64, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting piston = this.add(new SliderSetting("Piston", 64, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting redstone = this.add(new SliderSetting("RedStone", 64, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting bed = this.add(new SliderSetting("Bed", 256, 0, 512, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting speed = this.add(new SliderSetting("Speed", 1, 0, 8, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting resistance = this.add(new SliderSetting("Resistance", 1, 0, 8, () -> this.take.getValue() && this.smart.isOpen()));
   private final SliderSetting strength = this.add(new SliderSetting("Strength", 1, 0, 8, () -> this.take.getValue() && this.smart.isOpen()));
   private final BindSetting placeKey = this.add(new BindSetting("PlaceKey", -1));
   private final Timer timer = new Timer();
   private final List<BlockPos> openList = new ArrayList();
   public BlockPos placePos = null;
   private BlockPos openPos;
   private boolean opend = false;
   private boolean on = false;

   public AutoRegear() {
      super("AutoRegear", Module.Category.Combat);
      this.setChinese("自动补给");
      INSTANCE = this;
   }

   public int findShulker() {
      if (this.inventory.getValue()) {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
               return i < 9 ? i + 36 : i;
            }
         }

         return -1;
      } else {
         return InventoryUtil.findClass(ShulkerBoxBlock.class);
      }
   }

   @Override
   public void onEnable() {
      this.opend = false;
      this.openPos = null;
      this.timeoutTimer.reset();
      this.placePos = null;
      if (!nullCheck()) {
         if (this.place.getValue()) {
            this.doPlace();
         }
      }
   }

   private void doPlace() {
      int oldSlot = mc.player.getInventory().selectedSlot;
      double distance = 100.0;
      BlockPos bestPos = null;

      for (BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
         if (mc.world.isAir(pos.up())) {
            if (this.preferOpen.getValue() && mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
               return;
            }

            if (!(MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < this.minRange.getValue())
               && BlockUtil.clientCanPlace(pos, false)
               && BlockUtil.isStrictDirection(pos.offset(Direction.DOWN), Direction.UP)
               && BlockUtil.canClick(pos.offset(Direction.DOWN))
               && (bestPos == null || MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < distance)) {
               distance = MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos()));
               bestPos = pos;
            }
         }
      }

      if (bestPos != null) {
         if (this.findShulker() == -1) {
            this.sendMessage("§4No shulkerbox found.");
            return;
         }

         if (this.inventory.getValue()) {
            int slot = this.findShulker();
            InventoryUtil.inventorySwap(slot, oldSlot);
            this.placeBlock(bestPos);
            this.placePos = bestPos;
            InventoryUtil.inventorySwap(slot, oldSlot);
         } else {
            InventoryUtil.switchToSlot(this.findShulker());
            this.placeBlock(bestPos);
            this.placePos = bestPos;
            InventoryUtil.switchToSlot(oldSlot);
         }

         this.timer.reset();
      } else {
         this.sendMessage("§4No place position found.");
      }
   }

   private void update() {
      this.stealCountList[0] = (int)(this.crystal.getValue() - InventoryUtil.getItemCount(Items.END_CRYSTAL));
      this.stealCountList[1] = (int)(this.exp.getValue() - InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE));
      this.stealCountList[2] = (int)(this.totem.getValue() - InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING));
      this.stealCountList[3] = (int)(this.gapple.getValue() - InventoryUtil.getItemCount(Items.ENCHANTED_GOLDEN_APPLE));
      this.stealCountList[4] = (int)(this.obsidian.getValue() - InventoryUtil.getItemCount(Blocks.OBSIDIAN.asItem()));
      this.stealCountList[5] = (int)(this.web.getValue() - InventoryUtil.getItemCount(Blocks.COBWEB.asItem()));
      this.stealCountList[6] = (int)(this.glowstone.getValue() - InventoryUtil.getItemCount(Blocks.GLOWSTONE.asItem()));
      this.stealCountList[7] = (int)(this.anchor.getValue() - InventoryUtil.getItemCount(Blocks.RESPAWN_ANCHOR.asItem()));
      this.stealCountList[8] = (int)(this.pearl.getValue() - InventoryUtil.getItemCount(Items.ENDER_PEARL));
      this.stealCountList[9] = (int)(
         this.piston.getValue() - InventoryUtil.getItemCount(Blocks.PISTON.asItem()) - InventoryUtil.getItemCount(Blocks.STICKY_PISTON.asItem())
      );
      this.stealCountList[10] = (int)(this.redstone.getValue() - InventoryUtil.getItemCount(Blocks.REDSTONE_BLOCK.asItem()));
      this.stealCountList[11] = (int)(this.bed.getValue() - InventoryUtil.getItemCount(BedBlock.class));
      this.stealCountList[12] = (int)(this.speed.getValue() - InventoryUtil.getPotionCount((StatusEffect)StatusEffects.SPEED.value()));
      this.stealCountList[13] = (int)(this.resistance.getValue() - InventoryUtil.getPotionCount((StatusEffect)StatusEffects.RESISTANCE.value()));
      this.stealCountList[14] = (int)(this.strength.getValue() - InventoryUtil.getPotionCount((StatusEffect)StatusEffects.STRENGTH.value()));
   }

   @Override
   public void onDisable() {
      this.opend = false;
      if (this.mine.getValue() && this.placePos != null) {
         PacketMine.INSTANCE.mine(this.placePos);
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.smart.getValue()) {
         this.update();
      }

      if (this.placeKey.isPressed() && mc.currentScreen == null) {
         if (!this.on) {
            this.opend = false;
            this.openPos = null;
            this.timeoutTimer.reset();
            this.placePos = null;
            this.doPlace();
         }

         this.on = true;
      } else {
         this.on = false;
      }

      this.openList.removeIf(pos -> !(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock));
      if (!(mc.currentScreen instanceof ShulkerBoxScreen)) {
         if (this.opend) {
            this.opend = false;
            if (this.autoDisable.getValue()) {
               this.timeoutToDisable();
            }

            if (this.mine.getValue() && this.openPos != null) {
               if (mc.world.getBlockState(this.openPos).getBlock() instanceof ShulkerBoxBlock) {
                  PacketMine.INSTANCE.mine(this.openPos);
               } else {
                  this.openPos = null;
               }
            }
         } else {
            if (this.open.getValue()) {
               if (this.placePos == null
                  || !(MathHelper.sqrt((float)mc.player.squaredDistanceTo(this.placePos.toCenterPos())) <= this.range.getValue())
                  || !mc.world.isAir(this.placePos.up())
                  || this.timer.passed(500L) && !(mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock)) {
                  boolean found = false;

                  for (BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
                     if (!this.openList.contains(pos)
                        && (mc.world.isAir(pos.up()) || BlockUtil.canReplace(pos.up()))
                        && mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
                        this.openPos = pos;
                        BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue());
                        found = true;
                        break;
                     }
                  }

                  if (!found && this.autoDisable.getValue()) {
                     this.timeoutToDisable();
                  }
               } else if (mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock) {
                  this.openPos = this.placePos;
                  BlockUtil.clickBlock(this.placePos, BlockUtil.getClickSide(this.placePos), this.rotate.getValue());
               }
            } else if (!this.take.getValue() && this.autoDisable.getValue()) {
               this.timeoutToDisable();
            }
         }
      } else {
         this.opend = true;
         if (this.openPos != null) {
            this.openList.add(this.openPos);
         }

         if (!this.take.getValue()) {
            if (this.autoDisable.getValue()) {
               this.timeoutToDisable();
            }
         } else {
            boolean take = false;
            if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulker) {
               for (Slot slot : shulker.slots) {
                  if (slot.id < 27 && !slot.getStack().isEmpty()) {
                     AutoRegear.Type type = this.needSteal(slot.getStack());
                     if (this.smart.getValue() && type != AutoRegear.Type.QuickMove && (type != AutoRegear.Type.Stack || !this.forceMove.getValue())) {
                        if (type == AutoRegear.Type.Stack) {
                           for (int slot1 = 0; slot1 < 36; slot1++) {
                              ItemStack stack = mc.player.getInventory().getStack(slot1);
                              if (!stack.isEmpty()
                                 && stack.isStackable()
                                 && stack.getItem() == slot.getStack().getItem()
                                 && stack.getCount() < stack.getMaxCount()) {
                                 int i = (slot1 < 9 ? slot1 + 36 : slot1) + 18;
                                 mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                                 mc.interactionManager.clickSlot(shulker.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                                 mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                                 take = true;
                                 break;
                              }
                           }
                        }
                     } else {
                        mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                        take = true;
                     }
                  }
               }
            }

            if (this.autoDisable.getValue() && !take) {
               this.timeoutToDisable();
            }
         }
      }
   }

   private void timeoutToDisable() {
      if (this.timeoutTimer.passed(this.disableTime.getValueInt())) {
         this.disable();
      }
   }

   private AutoRegear.Type needSteal(ItemStack i) {
      if (i.getItem().equals(Items.END_CRYSTAL) && this.stealCountList[0] > 0) {
         this.stealCountList[0] = this.stealCountList[0] - i.getCount();
         return this.stealCountList[0] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (i.getItem().equals(Items.EXPERIENCE_BOTTLE) && this.stealCountList[1] > 0) {
         this.stealCountList[1] = this.stealCountList[1] - i.getCount();
         return this.stealCountList[1] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (i.getItem().equals(Items.TOTEM_OF_UNDYING) && this.stealCountList[2] > 0) {
         this.stealCountList[2] = this.stealCountList[2] - i.getCount();
         return this.stealCountList[2] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (i.getItem().equals(Items.ENCHANTED_GOLDEN_APPLE) && this.stealCountList[3] > 0) {
         this.stealCountList[3] = this.stealCountList[3] - i.getCount();
         return this.stealCountList[3] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (i.getItem().equals(Blocks.OBSIDIAN.asItem()) && this.stealCountList[4] > 0) {
         this.stealCountList[4] = this.stealCountList[4] - i.getCount();
         return this.stealCountList[4] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (i.getItem().equals(Blocks.COBWEB.asItem()) && this.stealCountList[5] > 0) {
         this.stealCountList[5] = this.stealCountList[5] - i.getCount();
         return this.stealCountList[5] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (i.getItem().equals(Blocks.GLOWSTONE.asItem()) && this.stealCountList[6] > 0) {
         this.stealCountList[6] = this.stealCountList[6] - i.getCount();
         return this.stealCountList[6] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (i.getItem().equals(Blocks.RESPAWN_ANCHOR.asItem()) && this.stealCountList[7] > 0) {
         this.stealCountList[7] = this.stealCountList[7] - i.getCount();
         return this.stealCountList[7] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (i.getItem().equals(Items.ENDER_PEARL) && this.stealCountList[8] > 0) {
         this.stealCountList[8] = this.stealCountList[8] - i.getCount();
         return this.stealCountList[8] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof PistonBlock && this.stealCountList[9] > 0) {
         this.stealCountList[9] = this.stealCountList[9] - i.getCount();
         return this.stealCountList[9] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (i.getItem().equals(Blocks.REDSTONE_BLOCK.asItem()) && this.stealCountList[10] > 0) {
         this.stealCountList[10] = this.stealCountList[10] - i.getCount();
         return this.stealCountList[10] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else if (i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof BedBlock && this.stealCountList[11] > 0) {
         this.stealCountList[11] = this.stealCountList[11] - i.getCount();
         return this.stealCountList[11] < 0 ? AutoRegear.Type.Stack : AutoRegear.Type.QuickMove;
      } else {
         if (Item.getRawId(i.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = (PotionContentsComponent)i.getOrDefault(
               DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT
            );

            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == StatusEffects.SPEED.value()) {
                  if (this.stealCountList[12] > 0) {
                     this.stealCountList[12] = this.stealCountList[12] - i.getCount();
                     if (this.stealCountList[12] < 0) {
                        return AutoRegear.Type.Stack;
                     }

                     return AutoRegear.Type.QuickMove;
                  }
               } else if (effect.getEffectType().value() == StatusEffects.RESISTANCE.value()) {
                  if (this.stealCountList[13] > 0) {
                     this.stealCountList[13] = this.stealCountList[13] - i.getCount();
                     if (this.stealCountList[13] < 0) {
                        return AutoRegear.Type.Stack;
                     }

                     return AutoRegear.Type.QuickMove;
                  }
               } else if (effect.getEffectType().value() == StatusEffects.STRENGTH.value() && this.stealCountList[14] > 0) {
                  this.stealCountList[14] = this.stealCountList[14] - i.getCount();
                  if (this.stealCountList[14] < 0) {
                     return AutoRegear.Type.Stack;
                  }

                  return AutoRegear.Type.QuickMove;
               }
            }
         }

         return AutoRegear.Type.None;
      }
   }

   private void placeBlock(BlockPos pos) {
      AntiRegear.INSTANCE.safe.add(pos);
      BlockUtil.clickBlock(pos.offset(Direction.DOWN), Direction.UP, this.rotate.getValue());
   }

   private static enum Type {
      None,
      Stack,
      QuickMove;
   }
}
