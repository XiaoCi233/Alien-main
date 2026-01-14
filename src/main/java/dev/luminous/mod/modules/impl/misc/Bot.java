package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.gui.windows.WindowsScreen;
import dev.luminous.mod.gui.windows.impl.ItemSelectWindow;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.combat.Aura;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.impl.render.PlaceRender;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class Bot extends Module {
   public static Bot INSTANCE;
   public final EnumSetting<Bot.Mode> mode = this.add(new EnumSetting("Mode", Bot.Mode.AutoTrade));
   private final BooleanSetting autoEat = this.add(new BooleanSetting("AutoEat", true).setParent());
   private final SliderSetting hunger = this.add(new SliderSetting("Hunger", 10.0, 0.0, 20.0, 1.0, this.autoEat::isOpen));
   private final SliderSetting health = this.add(new SliderSetting("Health", 20.0, 0.0, 36.0, 0.1, this.autoEat::isOpen));
   private final BooleanSetting anyFood = this.add(new BooleanSetting("AnyFood", false, this.autoEat::isOpen));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 5, 0, 100, () -> this.mode.is(Bot.Mode.TridentDupe)));
   private final BooleanSetting dropTridents = this.add(new BooleanSetting("DropTridents", true, () -> this.mode.is(Bot.Mode.TridentDupe)));
   private final BooleanSetting durabilityManagement = this.add(new BooleanSetting("DurabilityManagement", true, () -> this.mode.is(Bot.Mode.TridentDupe)));
   public final BooleanSetting edit = this.add(new BooleanSetting("Edit", false, () -> this.mode.is(Bot.Mode.AutoTrade)).injectTask(this::openAutoTradeEdit));
   public final SliderSetting repeatSetting = this.add(new SliderSetting("Repeat", 2.0, 1.0, 15.0, 1.0, () -> this.mode.is(Bot.Mode.AutoTrade)));
   public final BooleanSetting autoCloseSetting = this.add(new BooleanSetting("AutoClose", true, () -> this.mode.is(Bot.Mode.AutoTrade)));
   public final BooleanSetting timeoutCloseSetting = this.add(new BooleanSetting("TimeoutClose", true, () -> this.mode.is(Bot.Mode.AutoTrade)));
   public final SliderSetting timeOutSetting = this.add(new SliderSetting("Timeout", 1.0, 0.0, 15.0, 0.1, () -> this.mode.is(Bot.Mode.AutoTrade)));
   public final BooleanSetting autoOpenSetting = this.add(new BooleanSetting("AutoOpen", true, () -> this.mode.is(Bot.Mode.AutoTrade)));
   private final SliderSetting range = this.add(
      new SliderSetting("Range", 4.0, 0.0, 8.0, 0.1, () -> this.mode.is(Bot.Mode.SlabPlacer) || this.mode.is(Bot.Mode.AutoTrade))
   );
   private final BooleanSetting inventory = this.add(new BooleanSetting("Inventory", true, () -> this.mode.is(Bot.Mode.NPlusOneDupe)));
   private final BooleanSetting ai = this.add(new BooleanSetting("AI", true, () -> this.mode.is(Bot.Mode.SandMiner)));
   private final BooleanSetting nuker = this.add(new BooleanSetting("Nuker", true, () -> this.mode.is(Bot.Mode.SandMiner)));
   private final BooleanSetting redSand = this.add(new BooleanSetting("RedSand", false, () -> this.mode.is(Bot.Mode.SandMiner)));
   private final SliderSetting breaks = this.add(new SliderSetting("Breaks", 10, 0, 20, () -> this.mode.is(Bot.Mode.SandMiner)));
   private final SliderSetting maxTime = this.add(new SliderSetting("MaxTime", 60, 0, 100, () -> this.mode.is(Bot.Mode.Ominous)));
   public final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500, () -> this.mode.is(Bot.Mode.SlabPlacer)));
   private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8, () -> this.mode.is(Bot.Mode.SlabPlacer)));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.mode.is(Bot.Mode.SlabPlacer)));
   private final StringSetting name = this.add(new StringSetting("Name", "KizuatoResu", () -> this.mode.is(Bot.Mode.ScoreFarmer)));
   private final BooleanSetting getScore = this.add(new BooleanSetting("GetScore", false, () -> this.mode.is(Bot.Mode.ScoreFarmer)));
   private final Timer commandTimer = new Timer();
   private final Timer duelItemTimer = new Timer();
   final List<BlockPos> emptyBox = new ArrayList();
   final List<TurtleEntity> inLove = new ArrayList();
   final Timer timeOut = new Timer();
   final Timer closeScreen = new Timer();
   final Timer openTimeOut = new Timer();
   final Timer putTimer = new Timer();
   final Timer ominousTimer = new Timer();
   Bot.Stage stage = Bot.Stage.Open;
   BlockPos boxPos;
   boolean closeToBox;
   boolean putIn;
   LlamaEntity llama;
   boolean storageSand = false;
   boolean endEat = false;
   final Timer craftTimer = new Timer();
   final Timer screenTimeout = new Timer();
   int lastSlot = -1;
   int tick = 0;
   private final List<VillagerEntity> tradedVillager = new ArrayList();
   private final Timer timeoutTimer = new Timer();
   int placeProgress = 0;
   private final Timer slabPlacerDelay = new Timer();
   private boolean cancel = true;
   private final List<Pair<Long, Runnable>> scheduledTasks = new ArrayList();
   private final List<Pair<Long, Runnable>> scheduledTasks2 = new ArrayList();

   public Bot() {
      super("Bot", Module.Category.Misc);
      this.setChinese("机器人");
      INSTANCE = this;
   }

   private void openAutoTradeEdit() {
      this.edit.setValueWithoutTask(false);
      if (!nullCheck()) {
         mc.setScreen(new WindowsScreen(new ItemSelectWindow(Alien.TRADE)));
      }
   }

   private void placeBlock(BlockPos pos) {
      int block;
      if ((block = this.getBlock()) != -1) {
         int oldSlot = mc.player.getInventory().selectedSlot;
         if (BlockUtil.canPlace(pos)) {
            if (BlockUtil.allowAirPlace()) {
               this.doSwap(block);
               BlockUtil.placedPos.add(pos);
               BlockUtil.airPlace(pos, this.rotate.getValue());
               if (this.inventory.getValue()) {
                  this.doSwap(block);
                  EntityUtil.syncInventory();
               } else {
                  this.doSwap(oldSlot);
               }

               return;
            }

            Direction side = BlockUtil.getPlaceSide(pos);
            if (side == null) {
               return;
            }

            this.doSwap(block);
            BlockUtil.placedPos.add(pos);
            BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), this.rotate.getValue());
            if (this.inventory.getValue()) {
               this.doSwap(block);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(oldSlot);
            }
         }
      }
   }

   private void tryPlaceBlock(BlockPos pos) {
      if (pos != null) {
         if (this.placeProgress < this.blocksPer.getValue()) {
            int block;
            if (this.inventory.getValue()) {
               block = InventoryUtil.findClassInventorySlot(SlabBlock.class);
            } else {
               block = InventoryUtil.findClass(SlabBlock.class);
            }

            if (block != -1) {
               Direction side = BlockUtil.getPlaceSide(pos);
               if (side != null) {
                  Vec3d directionVec = new Vec3d(
                     pos.getX() + 0.5 + side.getVector().getX() * 0.5,
                     pos.getY() + 0.5 + side.getVector().getY() * 0.5,
                     pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
                  );
                  if (BlockUtil.canPlace(pos, 6.0, true)) {
                     if (this.rotate.getValue()) {
                        Alien.ROTATION.lookAt(directionVec);
                     }

                     if (!BlockUtil.hasEntity(pos, false)) {
                        int old = mc.player.getInventory().selectedSlot;
                        this.doSwap(block);
                        BlockUtil.placedPos.add(pos);
                        if (BlockUtil.allowAirPlace()) {
                           BlockUtil.airPlace(pos, false, Hand.MAIN_HAND, true);
                        } else {
                           BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), false, Hand.MAIN_HAND);
                        }

                        if (this.inventory.getValue()) {
                           this.doSwap(block);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(old);
                        }

                        if (this.rotate.getValue()) {
                           Alien.ROTATION.snapBack();
                        }

                        this.placeProgress++;
                        this.slabPlacerDelay.reset();
                     }
                  }
               }
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   private int getBlock() {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(ShulkerBoxBlock.class) : InventoryUtil.findClass(ShulkerBoxBlock.class);
   }

   @Override
   public String getInfo() {
      return this.mode.is(Bot.Mode.XinDupe) ? "Stage:" + this.stage.name() + ", Riding:" + mc.player.hasVehicle() : ((Bot.Mode)this.mode.getValue()).name();
   }

   @Override
   public void onEnable() {
      this.emptyBox.clear();
      this.storageSand = false;
      this.stage = Bot.Stage.Summon;
      this.boxPos = null;
      this.closeToBox = false;
      this.llama = null;
      this.putIn = false;
      this.tick = 0;
      this.scheduledTasks.clear();
      this.scheduledTasks2.clear();
      if (this.mode.is(Bot.Mode.TridentDupe)) {
         this.tridentDupe();
      }
   }

   @EventListener(priority = 201)
   private void onSendPacket(PacketEvent.Send event) {
      if (this.mode.is(Bot.Mode.TridentDupe)) {
         if (!this.cancel) {
            return;
         }

         if (event.getPacket() instanceof PlayerMoveC2SPacket || event.getPacket() instanceof CloseHandledScreenC2SPacket) {
            return;
         }

         if (!(event.getPacket() instanceof ClickSlotC2SPacket) && !(event.getPacket() instanceof PlayerActionC2SPacket)) {
            return;
         }

         event.cancel();
      }
   }

   private void tridentDupe() {
      int delayInt = this.delay.getValueInt() * 100;
      int lowestHotbarSlot = 0;
      int lowestHotbarDamage = 1000;

      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.TRIDENT) {
            int currentHotbarDamage = mc.player.getInventory().getStack(i).getDamage();
            if (lowestHotbarDamage > currentHotbarDamage) {
               lowestHotbarSlot = i;
               lowestHotbarDamage = currentHotbarDamage;
            }
         }
      }

      mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      this.cancel = true;
      int finalLowestHotbarSlot = lowestHotbarSlot;
      this.scheduleTask(() -> {
         this.cancel = false;
         if (this.durabilityManagement.getValue() && finalLowestHotbarSlot != 0) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 44, 0, SlotActionType.SWAP, mc.player);
            if (this.dropTridents.getValue()) {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 44, 0, SlotActionType.THROW, mc.player);
            }

            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36 + finalLowestHotbarSlot, 0, SlotActionType.SWAP, mc.player);
         }

         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 3, 0, SlotActionType.SWAP, mc.player);
         PlayerActionC2SPacket packet2 = new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN, 0);
         mc.getNetworkHandler().sendPacket(packet2);
         if (this.dropTridents.getValue()) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 44, 0, SlotActionType.THROW, mc.player);
         }

         this.cancel = true;
         this.scheduleTask2(this::tridentDupe, delayInt);
      }, delayInt);
   }

   public void scheduleTask(Runnable task, long delayMillis) {
      long executeTime = System.currentTimeMillis() + delayMillis;
      this.scheduledTasks.add(new Pair(executeTime, task));
   }

   public void scheduleTask2(Runnable task, long delayMillis) {
      long executeTime = System.currentTimeMillis() + delayMillis;
      this.scheduledTasks2.add(new Pair(executeTime, task));
   }

   @Override
   public void onDisable() {
      BaritoneUtil.cancelEverything();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (nullCheck()) {
         this.emptyBox.clear();
         this.stage = Bot.Stage.Summon;
         this.boxPos = null;
         this.closeToBox = false;
         this.llama = null;
         this.putIn = false;
      } else if (!this.autoEat.getValue()
         || !(mc.player.getHealth() + mc.player.getAbsorptionAmount() < this.health.getValueFloat())
            && mc.player.getHungerManager().getFoodLevel() >= this.hunger.getValueInt()) {
         if (this.endEat) {
            this.endEat = false;
            mc.options.useKey.setPressed(false);
         }

         switch ((Bot.Mode)this.mode.getValue()) {
            case AutoTrade:
               if (mc.player.currentScreenHandler instanceof MerchantScreenHandler handler) {
                  if (this.timeoutCloseSetting.getValue() && this.timeoutTimer.passedS(this.timeOutSetting.getValue())) {
                     mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                     mc.currentScreen.close();
                     return;
                  }

                  int i = 0;
                  TradeOfferList list = handler.getRecipes();

                  for (int size = 0; size < list.size(); size++) {
                     if (i >= this.repeatSetting.getValue()) {
                        return;
                     }

                     TradeOffer tradeOffer = (TradeOffer)list.get(size);
                     if (!tradeOffer.isDisabled() && Alien.TRADE.inWhitelist(tradeOffer.getSellItem().getItem().getTranslationKey())) {
                        while (i < this.repeatSetting.getValue()) {
                           if (!tradeOffer.getDisplayedFirstBuyItem().isEmpty()) {
                              int count = InventoryUtil.getItemCount(tradeOffer.getDisplayedFirstBuyItem().getItem());
                              if (handler.getSlot(0).getStack().getItem() == tradeOffer.getDisplayedFirstBuyItem().getItem()) {
                                 count += handler.getSlot(0).getStack().getCount();
                              }

                              if (count < tradeOffer.getDisplayedFirstBuyItem().getCount()) {
                                 break;
                              }
                           }

                           if (!tradeOffer.getDisplayedSecondBuyItem().isEmpty()) {
                              int countx = InventoryUtil.getItemCount(tradeOffer.getDisplayedSecondBuyItem().getItem());
                              if (handler.getSlot(1).getStack().getItem() == tradeOffer.getDisplayedSecondBuyItem().getItem()) {
                                 countx += handler.getSlot(1).getStack().getCount();
                              }

                              if (countx < tradeOffer.getDisplayedSecondBuyItem().getCount()) {
                                 break;
                              }
                           }

                           mc.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(size));
                           mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 2, 1, SlotActionType.QUICK_MOVE, mc.player);
                           i++;
                        }
                     }
                  }

                  if (this.autoCloseSetting.getValue() && i < this.repeatSetting.getValue()) {
                     mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                     mc.currentScreen.close();
                  }
               } else {
                  this.timeoutTimer.reset();
                  if (this.autoOpenSetting.getValue()) {
                     for (Entity entityxxxx : Alien.THREAD.getEntities()) {
                        if (entityxxxx instanceof VillagerEntity villager) {
                           if (mc.player.distanceTo(villager) <= this.range.getValue()) {
                              if (!this.tradedVillager.contains(villager)) {
                                 this.tradedVillager.add(villager);
                                 if (this.rotate.getValue()) {
                                    Alien.ROTATION.snapAt(villager.getEyePos());
                                 }

                                 mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(villager, mc.player.isSneaking(), Hand.MAIN_HAND));
                                 if (this.rotate.getValue()) {
                                    Alien.ROTATION.snapBack();
                                 }

                                 return;
                              }
                           } else {
                              this.tradedVillager.remove(villager);
                           }
                        }
                     }
                  }
               }
               break;
            case ItemFrameDupe: {
                this.tick++;
                int shulkerx = InventoryUtil.findClass(ShulkerBoxBlock.class);
                if (shulkerx != -1) {
                    for (Entity entityxxx : Alien.THREAD.getEntities()) {
                        if (entityxxx instanceof ItemFrameEntity itemFrameEntity && !(entityxxx.distanceTo(mc.player) > 3.0F)) {
                            InventoryUtil.switchToSlot(shulkerx);
                            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(itemFrameEntity, false, Hand.MAIN_HAND));
                            if (this.tick >= 2) {
                                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(itemFrameEntity, false));
                                mc.player.swingHand(Hand.MAIN_HAND);
                                this.tick = 0;
                            }

                            return;
                        }
                    }
                }

                this.tick = 0;
                break;
            }
            case XinDupe:
               if (this.llama != null && (this.llama.isDead() || this.llama.distanceTo(mc.player) > 20.0F)) {
                  this.llama = null;
               }

               int chestSlot = InventoryUtil.findBlockInventorySlot(Blocks.CHEST);
               int swordSlot = InventoryUtil.findClass(SwordItem.class);
               if (chestSlot == -1) {
                  this.emptyBox.clear();
                  this.stage = Bot.Stage.Open;
                  this.boxPos = null;
                  this.closeToBox = false;
                  this.llama = null;
                  this.putIn = false;
                  return;
               }

               int shulkers = 0;

               for (Map.Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
                  if (entry.getValue().getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                     shulkers++;
                  }
               }

               if (shulkers > 18) {
                  if (mc.currentScreen != null) {
                     mc.currentScreen.close();
                  }

                  for (int slot1x = 9; slot1x < 36; slot1x++) {
                     ItemStack stack = mc.player.getInventory().getStack(slot1x);
                     if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                        shulkers--;
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1x, 1, SlotActionType.THROW, mc.player);
                        if (shulkers <= 18) {
                           return;
                        }
                     }
                  }

                  return;
               }

               if (this.closeToBox && this.boxPos != null) {
                  this.closeTo(this.boxPos);
               }

               if (!this.openTimeOut.passed(100L)) {
                  return;
               }

               switch (this.stage) {
                  case Open:
                     if (!this.closeScreen.passed(250L)) {
                        if (mc.currentScreen != null) {
                           mc.currentScreen.close();
                        }

                        return;
                     }

                     if (mc.player.hasVehicle()) {
                        mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(0.0F, 0.0F, false, true));
                        return;
                     }

                     if (this.boxPos == null || this.emptyBox.contains(this.boxPos)) {
                        for (BlockPos posxxxx : BlockUtil.getSphere(3.0F)) {
                           if (!this.emptyBox.contains(posxxxx)
                              && mc.world.getBlockEntity(posxxxx) instanceof ShulkerBoxBlockEntity
                              && BlockUtil.getClickSideStrict(posxxxx) != null) {
                              this.closeToBox = false;
                              this.boxPos = posxxxx;
                              break;
                           }
                        }
                     }

                     if (this.boxPos != null && !this.emptyBox.contains(this.boxPos)) {
                        if (mc.player.getEyePos().distanceTo(this.boxPos.toCenterPos()) < 4.0) {
                           if (this.openTimeOut.passedS(1.0)) {
                              this.closeToBox = false;
                              this.openTimeOut.reset();
                              BlockUtil.clickBlock(this.boxPos, BlockUtil.getClickSide(this.boxPos), true);
                              this.stage = Bot.Stage.Take;
                              return;
                           }
                        } else {
                           this.closeToBox = true;
                        }

                        return;
                     }

                     return;
                  case Take: {
                      if (mc.player.hasVehicle()) {
                          mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(0.0F, 0.0F, false, true));
                          return;
                      }

                      if (this.boxPos == null || this.emptyBox.contains(this.boxPos)) {
                          this.closeScreen.reset();
                          this.stage = Bot.Stage.Open;
                          return;
                      }

                      if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulkerx) {
                          boolean egg = false;
                          boolean hay = false;

                          for (Slot slotxxx : shulkerx.slots) {
                              if (slotxxx.id < 27 && !slotxxx.getStack().isEmpty()) {
                                  if (slotxxx.getStack().getItem() == Items.EGG) {
                                      egg = true;
                                  }

                                  if (slotxxx.getStack().getItem() == Blocks.HAY_BLOCK.asItem()) {
                                      hay = true;
                                  }
                              }
                          }

                          if (egg && hay) {
                              int eggs = 0;
                              int hays = 0;

                              for (Map.Entry<Integer, ItemStack> entryx : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
                                  if (entryx.getValue().getItem() == Items.EGG) {
                                      eggs++;
                                  }

                                  if (entryx.getValue().getItem() == Blocks.HAY_BLOCK.asItem()) {
                                      hays++;
                                  }
                              }

                              for (Slot slotxxxx : shulkerx.slots) {
                                  if (!slotxxxx.getStack().isEmpty()) {
                                      if (slotxxxx.id < 27) {
                                          if (slotxxxx.getStack().getItem() == Items.EGG && eggs < 2) {
                                              eggs++;
                                              mc.interactionManager.clickSlot(shulkerx.syncId, slotxxxx.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                          }

                                          if (slotxxxx.getStack().getItem() == Blocks.HAY_BLOCK.asItem() && hays < 2) {
                                              hays++;
                                              mc.interactionManager.clickSlot(shulkerx.syncId, slotxxxx.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                          }
                                      } else if (slotxxxx.getStack().getItem() == Items.LEATHER) {
                                          mc.interactionManager.clickSlot(shulkerx.syncId, slotxxxx.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                      }
                                  }
                              }

                              if (hays < 1 || eggs < 1) {
                                  this.emptyBox.add(this.boxPos);
                              }

                              if (mc.currentScreen != null) {
                                  mc.currentScreen.close();
                              }

                              this.stage = Bot.Stage.Summon;
                          } else {
                              this.closeScreen.reset();
                              this.emptyBox.add(this.boxPos);
                              this.stage = Bot.Stage.Open;
                          }

                          return;
                      } else {
                          if (this.openTimeOut.passedS(1.0)) {
                              this.closeScreen.reset();
                              this.stage = Bot.Stage.Open;
                              return;
                          }

                          return;
                      }
                  }
                  case Summon: {
                      int eggs = 0;
                      int hays = 0;

                      for (Map.Entry<Integer, ItemStack> entryx : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
                          if (entryx.getValue().getItem() == Items.EGG) {
                              eggs++;
                          }

                          if (entryx.getValue().getItem() == Blocks.HAY_BLOCK.asItem()) {
                              hays++;
                          }
                      }

                      if (eggs <= 1 || hays <= 1) {
                          this.closeScreen.reset();
                          this.stage = Bot.Stage.Open;
                          return;
                      }

                      for (Entity entityxxxxx : Alien.THREAD.getEntities()) {
                          if (entityxxxxx instanceof LlamaEntity llamaEntity
                                  && mc.player.getEyePos().distanceTo(entityxxxxx.getPos()) < 10.0
                                  && entityxxxxx.isAlive()) {
                              if (mc.player.getEyePos().distanceTo(entityxxxxx.getPos()) < 5.0) {
                                  this.llama = llamaEntity;
                                  this.stage = Bot.Stage.Tame;
                              } else {
                                  this.closeTo(entityxxxxx.getBlockPos());
                              }

                              return;
                          }
                      }

                      if (mc.currentScreen != null) {
                          mc.currentScreen.close();
                      }

                      int slotxxx = InventoryUtil.findItemInventorySlot(Items.EGG);
                      InventoryUtil.inventorySwap(slotxxx, mc.player.getInventory().selectedSlot);
                      Alien.ROTATION.snapAt(Alien.ROTATION.getLastYaw(), 89.0F);
                      sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
                      InventoryUtil.inventorySwap(slotxxx, mc.player.getInventory().selectedSlot);
                      return;
                  }
                  case Tame:
                     if (this.llama == null || this.llama.isDead()) {
                        this.stage = Bot.Stage.Summon;
                        return;
                     }

                     int eggs = 0;
                     int hays = 0;

                     for (Map.Entry<Integer, ItemStack> entryx : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
                        if (entryx.getValue().getItem() == Items.EGG) {
                           eggs++;
                        }

                        if (entryx.getValue().getItem() == Blocks.HAY_BLOCK.asItem()) {
                           hays++;
                        }
                     }

                     if (eggs <= 1 || hays <= 1) {
                        this.closeScreen.reset();
                        this.stage = Bot.Stage.Open;
                        return;
                     }

                     if (mc.player.hasVehicle()) {
                        if (this.llama.isTame()) {
                           if (this.llama.hasChest()) {
                              int moves = 0;
                              if (mc.player.currentScreenHandler instanceof HorseScreenHandler shulkerx) {
                                 if (this.putTimer.passed(250L)) {
                                    if (!this.putIn) {
                                       for (Slot slotxxxxx : shulkerx.slots) {
                                          if (slotxxxxx.getStack().getItem() instanceof BlockItem blockItemx
                                             && blockItemx.getBlock() instanceof ShulkerBoxBlock) {
                                             mc.interactionManager.clickSlot(shulkerx.syncId, slotxxxxx.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                             moves++;
                                             this.putTimer.reset();
                                             if (moves >= 15) {
                                                break;
                                             }
                                          }
                                       }

                                       this.putIn = true;
                                    } else {
                                       this.stage = Bot.Stage.Kill;
                                       mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(0.0F, 0.0F, false, true));
                                    }

                                    return;
                                 }
                              } else {
                                 this.putIn = false;
                                 this.putTimer.reset();
                                 mc.player.openRidingInventory();
                              }

                              return;
                           } else {
                              mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(0.0F, 0.0F, false, true));
                              return;
                           }
                        }

                        return;
                     } else {
                        if (mc.player.getEyePos().distanceTo(this.llama.getPos()) > 5.0) {
                           this.closeTo(this.llama.getBlockPos());
                           return;
                        }

                        if (this.llama.isBaby()) {
                           if (mc.currentScreen != null) {
                              mc.currentScreen.close();
                           }

                           int slotxxxxxx = InventoryUtil.findBlockInventorySlot(Blocks.HAY_BLOCK);
                           InventoryUtil.inventorySwap(slotxxxxxx, mc.player.getInventory().selectedSlot);
                           Alien.ROTATION.lookAt(this.llama.getPos());
                           mc.interactionManager.interactEntity(mc.player, this.llama, Hand.MAIN_HAND);
                           InventoryUtil.inventorySwap(slotxxxxxx, mc.player.getInventory().selectedSlot);
                        } else if (this.llama.isTame()) {
                           if (this.llama.hasChest()) {
                              for (int i = 0; i < 9; i++) {
                                 if (mc.player.getInventory().getStack(i).isEmpty()) {
                                    InventoryUtil.switchToSlot(i);
                                    mc.interactionManager.interactEntity(mc.player, this.llama, Hand.MAIN_HAND);
                                    return;
                                 }
                              }

                              for (int ix = 0; ix < 9; ix++) {
                                 if (mc.player.getInventory().getStack(ix).getItem() instanceof BlockItem blockItemxx
                                       && blockItemxx.getBlock() instanceof ShulkerBoxBlock
                                    || mc.player.getInventory().getStack(ix).getItem() == Items.LEATHER) {
                                    InventoryUtil.switchToSlot(ix);
                                    mc.player.dropSelectedItem(true);
                                    mc.interactionManager.interactEntity(mc.player, this.llama, Hand.MAIN_HAND);
                                    return;
                                 }
                              }

                              return;
                           } else {
                              this.putTimer.reset();
                              this.putIn = false;
                              if (mc.currentScreen != null) {
                                 mc.currentScreen.close();
                              }

                              InventoryUtil.inventorySwap(chestSlot, mc.player.getInventory().selectedSlot);
                              Alien.ROTATION.lookAt(this.llama.getPos());
                              mc.interactionManager.interactEntity(mc.player, this.llama, Hand.MAIN_HAND);
                              InventoryUtil.inventorySwap(chestSlot, mc.player.getInventory().selectedSlot);
                           }
                        } else {
                           if (mc.currentScreen != null) {
                              mc.currentScreen.close();
                           }

                           for (int ixx = 0; ixx < 9; ixx++) {
                              if (mc.player.getInventory().getStack(ixx).isEmpty()) {
                                 InventoryUtil.switchToSlot(ixx);
                                 mc.interactionManager.interactEntity(mc.player, this.llama, Hand.MAIN_HAND);
                                 return;
                              }
                           }

                           for (int ixxx = 0; ixxx < 9; ixxx++) {
                              if (mc.player.getInventory().getStack(ixxx).getItem() instanceof BlockItem blockItemxx
                                 && blockItemxx.getBlock() instanceof ShulkerBoxBlock) {
                                 InventoryUtil.switchToSlot(ixxx);
                                 mc.player.dropSelectedItem(true);
                                 mc.interactionManager.interactEntity(mc.player, this.llama, Hand.MAIN_HAND);
                                 return;
                              }
                           }

                           return;
                        }
                     }

                     return;
                  case Kill:
                     if (this.llama == null || this.llama.isDead()) {
                        this.llama = null;
                        this.stage = Bot.Stage.Summon;
                        return;
                     }

                     if (mc.currentScreen != null) {
                        mc.currentScreen.close();
                     }

                     if (mc.player.hasVehicle()) {
                        mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(0.0F, 0.0F, false, true));
                        return;
                     }

                     if (mc.player.getPos().distanceTo(this.llama.getPos()) > 1.0) {
                        this.closeTo(this.llama.getBlockPos());
                     }

                     if (mc.player.getPos().distanceTo(this.llama.getPos()) > 2.0) {
                        return;
                     }

                     InventoryUtil.switchToSlot(swordSlot);
                     if (this.check()) {
                        Alien.ROTATION.lookAt(this.llama.getEyePos());
                        mc.interactionManager.attackEntity(mc.player, this.llama);
                        EntityUtil.swingHand(Hand.MAIN_HAND, SwingSide.All);
                     }

                     return;
                  default:
                     return;
               }
            case NPlusOneDupe:
               BlockPos placePos = PacketMine.getBreakPos();
               if (placePos != null && BlockUtil.canPlace(placePos)) {
                  this.placeBlock(placePos);
               }
               break;
            case TridentDupe:
               long currentTime = System.currentTimeMillis();
               Iterator<Pair<Long, Runnable>> iterator = this.scheduledTasks.iterator();

               while (iterator.hasNext()) {
                  Pair<Long, Runnable> entry = (Pair<Long, Runnable>)iterator.next();
                  if ((Long)entry.getLeft() <= currentTime) {
                     ((Runnable)entry.getRight()).run();
                     iterator.remove();
                  }
               }

               iterator = this.scheduledTasks2.iterator();

               while (iterator.hasNext()) {
                  Pair<Long, Runnable> entry = (Pair<Long, Runnable>)iterator.next();
                  if ((Long)entry.getLeft() <= currentTime) {
                     ((Runnable)entry.getRight()).run();
                     iterator.remove();
                  }
               }
               break;
            case TurtlePath: {
                int seagrass = InventoryUtil.findItem(Items.SEAGRASS);
                if (seagrass == -1) {
                    if (BaritoneUtil.isActive()) {
                        BaritoneUtil.cancelEverything();
                    }

                    return;
                }

                if (this.timeOut.passedS(300.0)) {
                    this.inLove.clear();
                    this.timeOut.reset();
                }

                double distance = 0.0;
                TurtleEntity target = null;

                for (Entity entity : Alien.THREAD.getEntities()) {
                    if (entity instanceof TurtleEntity turtle
                            && !turtle.isBaby()
                            && !this.inLove.contains(turtle)
                            && !(Math.abs(mc.player.getY() - turtle.getY()) > 3.0)) {
                        double dis = mc.player.distanceTo(turtle);
                        if (target == null || dis < distance) {
                            distance = dis;
                            target = turtle;
                        }
                    }
                }

                if (target == null) {
                    if (this.timeOut.passedS(20.0)) {
                        this.inLove.clear();
                        this.timeOut.reset();
                    }

                    if (BaritoneUtil.isActive()) {
                        BaritoneUtil.cancelEverything();
                    }

                    return;
                }

                if (mc.player.distanceTo(target) < 3.0F) {
                    BaritoneUtil.cancelEverything();
                    InventoryUtil.switchToSlot(seagrass);
                    Alien.ROTATION.snapAt(target.getPos());
                    mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(target, mc.player.isSneaking(), Hand.MAIN_HAND));
                    EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)AntiCheat.INSTANCE.interactSwing.getValue());
                    Alien.ROTATION.snapBack();
                    this.inLove.add(target);
                } else {
                    BaritoneUtil.gotoPos(target.getBlockPos());
                }
                break;
            }
            case Turtle: {
                int seagrassx = InventoryUtil.findItem(Items.SEAGRASS);
                if (seagrassx == -1) {
                    return;
                }

                if (this.timeOut.passedS(300.0)) {
                    this.inLove.clear();
                    this.timeOut.reset();
                }

                double distance = 0.0;
                TurtleEntity target = null;

                for (Entity entityx : Alien.THREAD.getEntities()) {
                    if (entityx instanceof TurtleEntity turtlex
                            && !turtlex.isBaby()
                            && !this.inLove.contains(turtlex)
                            && !(mc.player.distanceTo(turtlex) > 3.0F)) {
                        double dis = mc.player.distanceTo(turtlex);
                        if (target == null || dis < distance) {
                            distance = dis;
                            target = turtlex;
                        }
                    }
                }

                if (target == null) {
                    if (this.timeOut.passedS(20.0)) {
                        this.inLove.clear();
                        this.timeOut.reset();
                    }

                    return;
                }

                InventoryUtil.switchToSlot(seagrassx);
                Alien.ROTATION.snapAt(target.getPos());
                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.interact(target, mc.player.isSneaking(), Hand.MAIN_HAND));
                EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)AntiCheat.INSTANCE.interactSwing.getValue());
                Alien.ROTATION.snapBack();
                this.inLove.add(target);
                break;
            }
            case SandMiner:
               Block sandBlock = Blocks.SAND;
               if (this.redSand.getValue()) {
                  sandBlock = Blocks.RED_SAND;
               }

               if (Aura.INSTANCE.isOn() && Aura.INSTANCE.getTarget(Aura.INSTANCE.range.getValue()) != null) {
                  if (mc.currentScreen != null) {
                     mc.currentScreen.close();
                  }

                  int slot = InventoryUtil.findClass(SwordItem.class);
                  if (mc.player.getInventory().selectedSlot != slot) {
                     InventoryUtil.switchToSlot(slot);
                  }

                  BaritoneUtil.cancelEverything();
                  return;
               }

               if (mc.currentScreen == null) {
                  this.screenTimeout.reset();
               } else if (this.screenTimeout.passedS(5.0)) {
                  mc.currentScreen.close();
               }

               if (InventoryUtil.findClassInventorySlot(ShovelItem.class) == -1) {
                  BaritoneUtil.cancelEverything();
                  if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
                     if (this.craftTimer.passedS(1.0)) {
                        for (RecipeResultCollection recipeResult : mc.player.getRecipeBook().getOrderedResults()) {
                           for (RecipeEntry<?> recipe : recipeResult.getRecipes(true)) {
                              if (recipe.value().getResult(mc.world.getRegistryManager()).getItem() instanceof ShovelItem) {
                                 this.craftTimer.reset();
                                 mc.interactionManager.clickRecipe(mc.player.currentScreenHandler.syncId, recipe, false);
                                 mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 1, SlotActionType.QUICK_MOVE, mc.player);
                                 return;
                              }
                           }
                        }
                     }
                  } else {
                     BlockPos bestPos = null;
                     double distance = 100.0;

                     for (BlockPos pos : BlockUtil.getSphere(3.0F)) {
                        if (mc.world.getBlockState(pos).getBlock() == Blocks.CRAFTING_TABLE && BlockUtil.getClickSideStrict(pos) != null) {
                           BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue());
                           return;
                        }

                        if (BlockUtil.canPlace(pos) && (bestPos == null || MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < distance)) {
                           bestPos = pos;
                           distance = MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos()));
                        }
                     }

                     if (bestPos != null) {
                        int craftTable = InventoryUtil.findItemInventorySlot(Items.CRAFTING_TABLE);
                        if (craftTable == -1) {
                           return;
                        }

                        InventoryUtil.inventorySwap(craftTable, mc.player.getInventory().selectedSlot);
                        BlockUtil.placeBlock(bestPos, this.rotate.getValue());
                        InventoryUtil.inventorySwap(craftTable, mc.player.getInventory().selectedSlot);
                     }
                  }

                  return;
               }

               Entity hasShulkerItemEntity = null;

               for (Entity entityxx : Alien.THREAD.getEntities()) {
                  if (entityxx instanceof ItemEntity itemEntity
                     && itemEntity.getStack().getItem() instanceof BlockItem item
                     && (item.getBlock() instanceof ShulkerBoxBlock || item.getBlock() == Blocks.ENDER_CHEST)) {
                     hasShulkerItemEntity = itemEntity;
                     break;
                  }
               }

               if (mc.player.currentScreenHandler instanceof CraftingScreenHandler && mc.currentScreen != null) {
                  mc.currentScreen.close();
               }

               int sands = InventoryUtil.getItemCount(Items.SAND);
               if (sands >= 1728) {
                  for (int slot1 = 9; slot1 < 36; slot1++) {
                     ItemStack stack = mc.player.getInventory().getStack(slot1);
                     if (!stack.isEmpty()
                        && stack.getItem() instanceof BlockItem blockItem
                        && blockItem.getBlock() == sandBlock
                        && stack.getCount() < stack.getMaxCount()) {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot1, 1, SlotActionType.THROW, mc.player);
                     }
                  }

                  if (mc.currentScreen instanceof InventoryScreen) {
                     mc.currentScreen.close();
                     return;
                  }

                  int shulkerSlot = InventoryUtil.findClassInventorySlot(ShulkerBoxBlock.class);
                  BlockPos shulker = BlockUtil.getBlock(ShulkerBoxBlock.class, 3.0F);
                  if (mc.currentScreen instanceof HandledScreen) {
                     ScreenHandler var104 = mc.player.currentScreenHandler;
                     if (var104 instanceof ScreenHandler) {
                        if (!(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) {
                           BaritoneUtil.cancelEverything();
                           if (shulkerSlot != -1) {
                              mc.currentScreen.close();
                           } else {
                              Iterator var106 = var104.slots.iterator();

                              Slot slot;
                              do {
                                 if (!var106.hasNext()) {
                                    return;
                                 }

                                 slot = (Slot)var106.next();
                              } while (
                                 slot.id >= 27
                                    || !(
                                       slot.getStack().getItem() instanceof BlockItem blockItem
                                          && blockItem.getBlock() instanceof ShulkerBoxBlock
                                          && !ShulkerViewer.hasItems(slot.getStack())
                                    )
                              );

                              mc.interactionManager.clickSlot(var104.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                           }
                        } else {
                           BaritoneUtil.cancelEverything();
                           this.storageSand = true;
                           if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulkerBoxScreenHandler) {
                              for (Slot slot : shulkerBoxScreenHandler.slots) {
                                 if (slot.id < 27 && slot.getStack().isEmpty()) {
                                    for (Slot slot2 : shulkerBoxScreenHandler.slots) {
                                       if (slot2.id >= 27
                                          && slot2.getStack().getItem() == Items.SAND
                                          && slot2.getStack().getCount() == slot2.getStack().getMaxCount()) {
                                          mc.interactionManager
                                             .clickSlot(shulkerBoxScreenHandler.syncId, slot2.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                          return;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                        break;
                     }
                  }

                  if (shulker == null) {
                     double distance = 100.0;
                     BlockPos bestPos = null;

                     for (BlockPos pos : BlockUtil.getSphere(3.0F, mc.player.getEyePos())) {
                        if (mc.world.isAir(pos.up())
                           && BlockUtil.clientCanPlace(pos, false)
                           && BlockUtil.isStrictDirection(pos.offset(Direction.DOWN), Direction.UP)
                           && BlockUtil.canClick(pos.offset(Direction.DOWN))
                           && (bestPos == null || MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < distance)) {
                           distance = MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos()));
                           bestPos = pos;
                        }
                     }

                     if (bestPos != null) {
                        BaritoneUtil.cancelEverything();
                        if (shulkerSlot != -1) {
                           InventoryUtil.inventorySwap(shulkerSlot, mc.player.getInventory().selectedSlot);
                           BlockUtil.clickBlock(bestPos.offset(Direction.DOWN), Direction.UP, this.rotate.getValue());
                           InventoryUtil.inventorySwap(shulkerSlot, mc.player.getInventory().selectedSlot);
                        } else {
                           BlockPos ec = BlockUtil.getBlock(Blocks.ENDER_CHEST, 3.0F);
                           if (ec != null) {
                              BlockUtil.clickBlock(ec, BlockUtil.getClickSide(ec), this.rotate.getValue());
                           } else {
                              int enderChest = InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST);
                              if (enderChest != -1) {
                                 InventoryUtil.inventorySwap(enderChest, mc.player.getInventory().selectedSlot);
                                 BlockUtil.placeBlock(bestPos, true);
                                 InventoryUtil.inventorySwap(enderChest, mc.player.getInventory().selectedSlot);
                              }
                           }
                        }
                     }
                  } else {
                     BlockUtil.clickBlock(shulker, BlockUtil.getClickSide(shulker), this.rotate.getValue());
                  }
               } else if (this.storageSand) {
                  if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulkerBoxScreenHandler) {
                     for (Slot slotx : shulkerBoxScreenHandler.slots) {
                        if (slotx.id < 27 && slotx.getStack().isEmpty()) {
                           for (Slot slot2x : shulkerBoxScreenHandler.slots) {
                              if (slot2x.id >= 27
                                 && slot2x.getStack().getItem() == Items.SAND
                                 && slot2x.getStack().getCount() == slot2x.getStack().getMaxCount()) {
                                 mc.interactionManager.clickSlot(shulkerBoxScreenHandler.syncId, slot2x.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                 return;
                              }
                           }
                        }
                     }

                     mc.currentScreen.close();
                     this.storageSand = false;
                  }
               } else {
                  BlockPos shulkerx = BlockUtil.getBlock(ShulkerBoxBlock.class, 3.0F);
                  if (shulkerx != null) {
                     BaritoneUtil.mine(mc.world.getBlockState(shulkerx).getBlock());
                  } else {
                     int fillShulker = InventoryUtil.findClassInventorySlot(ShulkerBoxBlock.class);
                     if (fillShulker != -1) {
                        BaritoneUtil.cancelEverything();
                        ScreenHandler var110 = mc.player.currentScreenHandler;
                        if (var110 instanceof ScreenHandler) {
                           for (Slot slotxx : var110.slots) {
                              if (slotxx.id >= 27
                                 && slotxx.getStack().getItem() instanceof BlockItem blockItem
                                 && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                                 mc.interactionManager.clickSlot(var110.syncId, slotxx.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                 return;
                              }
                           }
                        }

                        double distance = 100.0;
                        BlockPos bestPos = null;

                        for (BlockPos posx : BlockUtil.getSphere(3.0F)) {
                           if (mc.world.isAir(posx.up())
                              && BlockUtil.clientCanPlace(posx, false)
                              && BlockUtil.isStrictDirection(posx.offset(Direction.DOWN), Direction.UP)
                              && BlockUtil.canClick(posx.offset(Direction.DOWN))
                              && (bestPos == null || MathHelper.sqrt((float)mc.player.squaredDistanceTo(posx.toCenterPos())) < distance)) {
                              distance = MathHelper.sqrt((float)mc.player.squaredDistanceTo(posx.toCenterPos()));
                              bestPos = posx;
                           }
                        }

                        if (bestPos != null) {
                           BlockPos ec = BlockUtil.getBlock(Blocks.ENDER_CHEST, 3.0F);
                           if (ec != null) {
                              BlockUtil.clickBlock(ec, BlockUtil.getClickSide(ec), this.rotate.getValue());
                           } else {
                              int enderChest = InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST);
                              if (enderChest != -1) {
                                 InventoryUtil.inventorySwap(enderChest, mc.player.getInventory().selectedSlot);
                                 BlockUtil.placeBlock(bestPos, true);
                                 InventoryUtil.inventorySwap(enderChest, mc.player.getInventory().selectedSlot);
                              }
                           }
                        }

                        return;
                     }

                     if (hasShulkerItemEntity != null) {
                        BaritoneUtil.gotoPos(hasShulkerItemEntity.getBlockPos());
                     }

                     BlockPos posxx = BlockUtil.getBlock(Blocks.ENDER_CHEST, 5.0F);
                     if (posxx != null) {
                        BaritoneUtil.mine(Blocks.ENDER_CHEST);
                        if (mc.currentScreen != null) {
                           mc.currentScreen.close();
                        }

                        return;
                     }

                     if (this.ai.getValue()) {
                        BaritoneUtil.mine(sandBlock);
                     }

                     if (this.nuker.getValue()) {
                        if (!mc.player.isOnGround()) {
                           return;
                        }

                        int b = 0;

                        for (BlockPos sand : BlockUtil.getSphere(3.0F, mc.player.getEyePos())) {
                           if (sandBlock == mc.world.getBlockState(sand).getBlock()) {
                              Direction side = BlockUtil.getClickSideStrict(sand);
                              if (side != null) {
                                 PlaceRender.INSTANCE.create(sand);
                                 Alien.ROTATION.snapAt(sand.toCenterPos());
                                 sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, sand, side, id));
                                 sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, sand, side, id));
                                 sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, sand, side, id));
                                 Alien.ROTATION.snapBack();
                                 if (++b >= this.breaks.getValue()) {
                                    return;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
               break;
            case ScoreFarmer:
               if (this.getScore.getValue() && this.commandTimer.passedS(4.0)) {
                  mc.player.networkHandler.sendCommand("duel " + this.name.getValue());
                  this.commandTimer.reset();
               }

               if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler && this.duelItemTimer.passedS(1.0)) {
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 0, SlotActionType.PICKUP, mc.player);
                  this.duelItemTimer.reset();
               }
               break;
            case SlabPlacer:
               if (this.inventory.getValue() && !EntityUtil.inInventory()) {
                  return;
               }

               this.placeProgress = 0;
               if (!this.slabPlacerDelay.passed((long)this.placeDelay.getValue())) {
                  return;
               }

               if (mc.player.isUsingItem() && this.usingPause.getValue()) {
                  return;
               }

               for (BlockPos posxxx : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getPos())) {
                  if ((mc.world.getBlockState(posxxx).isFullCube(mc.world, posxxx) || !(mc.world.getBlockState(posxxx).getBlock() instanceof SlabBlock))
                     && !mc.world.isAir(posxxx)
                     && BlockUtil.canReplace(posxxx.up())) {
                     this.tryPlaceBlock(posxxx.up());
                  }
               }
               break;
            case Ominous:
               int ominousSlot = InventoryUtil.findItem(Items.OMINOUS_BOTTLE);
               if (ominousSlot != -1) {
                  if (!mc.player.hasStatusEffect(StatusEffects.BAD_OMEN) && !mc.player.hasStatusEffect(StatusEffects.RAID_OMEN)) {
                     if (this.ominousTimer.passedS(this.maxTime.getValue())) {
                        if (mc.player.getMainHandStack().getItem() != Items.OMINOUS_BOTTLE) {
                           this.lastSlot = mc.player.getInventory().selectedSlot;
                           InventoryUtil.switchToSlot(ominousSlot);
                        } else {
                           mc.options.useKey.setPressed(true);
                           this.ominousTimer.reset();
                        }
                     }
                  } else if (mc.player.getMainHandStack().getItem() == Items.OMINOUS_BOTTLE) {
                     mc.options.useKey.setPressed(false);
                     if (this.lastSlot != -1) {
                        InventoryUtil.switchToSlot(this.lastSlot);
                        this.lastSlot = -1;
                     }
                  }
               }
         }
      } else {
         int food = InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
         if (food == -1) {
            food = InventoryUtil.findItem(Items.GOLDEN_APPLE);
         }

         if (food == -1) {
            food = InventoryUtil.findItem(Items.GOLDEN_CARROT);
         }

         if (food == -1 && this.anyFood.getValue()) {
            food = InventoryUtil.getFood();
         }

         if (food != -1) {
            if (mc.currentScreen != null && this.mode.is(Bot.Mode.XinDupe)) {
               mc.currentScreen.close();
            }

            if (mc.player.getInventory().selectedSlot != food) {
               InventoryUtil.switchToSlot(food);
            }

            mc.options.useKey.setPressed(true);
            this.endEat = true;
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         }

         BaritoneUtil.cancelEverything();
      }
   }

   @EventListener
   private void PacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (this.mode.is(Bot.Mode.ScoreFarmer) && event.getPacket() instanceof GameMessageS2CPacket packet && packet.content() != null) {
            String received = packet.content().getString().replaceAll("§[a-zA-Z0-9]", "");
            if (!this.getScore.getValue() && received.contains("你收到一个决斗申请")) {
               mc.player.networkHandler.sendCommand("duel accept " + this.name.getValue());
            } else if (!received.contains("<")) {
               if (this.getScore.getValue() && received.contains("Starting in 3 seconds")) {
                  mc.player.networkHandler.sendCommand("suicide");
               } else if (!this.getScore.getValue() && received.contains("Starting in 4 seconds")) {
                  mc.player.networkHandler.sendCommand("suicide");
               }
            }
         }
      }
   }

   @EventListener
   public void onRotate(UpdateRotateEvent event) {
      if (this.mode.is(Bot.Mode.XinDupe)) {
         event.setPitch(88.0F);
      }
   }

   private boolean check() {
      int at = ((ILivingEntity)mc.player).getLastAttackedTicks();
      return Math.max(at / Aura.getAttackCooldownProgressPerTick(), 0.0F) >= 1.3;
   }

   private void closeTo(BlockPos pos) {
      double speed = 0.19153333333333333;
      float forward = 1.0F;
      float side = 0.0F;
      float yaw = RotationManager.getRotation(pos.toCenterPos())[0];
      double sin = Math.sin(Math.toRadians(yaw + 90.0F));
      double cos = Math.cos(Math.toRadians(yaw + 90.0F));
      double posX = forward * speed * cos + side * speed * sin;
      double posZ = forward * speed * sin - side * speed * cos;
      MovementUtil.setMotionX(posX);
      MovementUtil.setMotionZ(posZ);
   }

   public static enum Mode {
      AutoTrade,
      ItemFrameDupe,
      XinDupe,
      NPlusOneDupe,
      TridentDupe,
      TurtlePath,
      Turtle,
      SandMiner,
      ScoreFarmer,
      SlabPlacer,
      Ominous,
      None;
   }

   public static enum Stage {
      Open,
      Take,
      Summon,
      Tame,
      Kill;
   }
}
