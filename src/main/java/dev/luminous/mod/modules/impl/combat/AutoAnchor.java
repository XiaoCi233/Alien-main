package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.entity.PlayerEntityPredict;
import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.math.ExplosionUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.impl.player.AirPlace;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AutoAnchor extends Module {
   public static AutoAnchor INSTANCE;
   static Vec3d placeVec3d;
   static Vec3d curVec3d;
   public final EnumSetting<AutoAnchor.Page> page = this.add(new EnumSetting("Page", AutoAnchor.Page.General));
   public final SliderSetting range = this.add(
      new SliderSetting("Range", 5.0, 0.0, 6.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.General).setSuffix("m")
   );
   public final SliderSetting targetRange = this.add(
      new SliderSetting("TargetRange", 8.0, 0.1, 12.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.General).setSuffix("m")
   );
   public final SliderSetting minDamage = this.add(
      new SliderSetting("Min", 4.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact).setSuffix("dmg")
   );
   public final SliderSetting breakMin = this.add(
      new SliderSetting("ExplosionMin", 4.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact).setSuffix("dmg")
   );
   public final SliderSetting headDamage = this.add(
      new SliderSetting("ForceHead", 7.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact).setSuffix("dmg")
   );
   private final SliderSetting selfPredict = this.add(
      new SliderSetting("SelfPredict", 4, 0, 10, () -> this.page.getValue() == AutoAnchor.Page.Predict).setSuffix("ticks")
   );
   private final SliderSetting predictTicks = this.add(
      new SliderSetting("Predict", 4, 0, 10, () -> this.page.getValue() == AutoAnchor.Page.Predict).setSuffix("ticks")
   );
   private final SliderSetting simulation = this.add(
      new SliderSetting("Simulation", 5.0, 0.0, 20.0, 1.0, () -> this.page.getValue() == AutoAnchor.Page.Predict)
   );
   private final SliderSetting maxMotionY = this.add(
      new SliderSetting("MaxMotionY", 0.34, 0.0, 2.0, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Predict)
   );
   private final BooleanSetting step = this.add(new BooleanSetting("Step", false, () -> this.page.getValue() == AutoAnchor.Page.Predict));
   private final BooleanSetting doubleStep = this.add(new BooleanSetting("DoubleStep", false, () -> this.page.getValue() == AutoAnchor.Page.Predict));
   private final BooleanSetting jump = this.add(new BooleanSetting("Jump", false, () -> this.page.getValue() == AutoAnchor.Page.Predict));
   private final BooleanSetting inBlockPause = this.add(new BooleanSetting("InBlockPause", true, () -> this.page.getValue() == AutoAnchor.Page.Predict));
   final ArrayList<BlockPos> chargeList = new ArrayList();
   private final BooleanSetting assist = this.add(new BooleanSetting("Assist", true, () -> this.page.getValue() == AutoAnchor.Page.Assist));
   private final BooleanSetting obsidian = this.add(new BooleanSetting("Obsidian", true, () -> this.page.getValue() == AutoAnchor.Page.Assist));
   private final BooleanSetting checkMine = this.add(new BooleanSetting("DetectMining", false, () -> this.page.getValue() == AutoAnchor.Page.Assist));
   private final SliderSetting assistRange = this.add(
      new SliderSetting("AssistRange", 5.0, 0.0, 6.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Assist).setSuffix("m")
   );
   private final SliderSetting assistDamage = this.add(
      new SliderSetting("AssistDamage", 6.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Assist).setSuffix("h")
   );
   private final SliderSetting delay = this.add(
      new SliderSetting("AssistDelay", 0.1, 0.0, 1.0, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Assist).setSuffix("s")
   );
   private final BooleanSetting preferCrystal = this.add(new BooleanSetting("PreferCrystal", false, () -> this.page.getValue() == AutoAnchor.Page.General));
   private final BooleanSetting thread = this.add(new BooleanSetting("Thread", false, () -> this.page.getValue() == AutoAnchor.Page.General));
   private final BooleanSetting light = this.add(new BooleanSetting("LessCPU", true, () -> this.page.getValue() == AutoAnchor.Page.General));
   private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.getValue() == AutoAnchor.Page.General));
   private final BooleanSetting breakCrystal = this.add(
      new BooleanSetting("BreakCrystal", true, () -> this.page.getValue() == AutoAnchor.Page.General).setParent()
   );
   private final BooleanSetting spam = this.add(new BooleanSetting("Spam", true, () -> this.page.getValue() == AutoAnchor.Page.General).setParent());
   private final BooleanSetting mineSpam = this.add(
      new BooleanSetting("OnlyMining", true, () -> this.page.getValue() == AutoAnchor.Page.General && this.spam.isOpen())
   );
   private final BooleanSetting spamPlace = this.add(new BooleanSetting("Fast", true, () -> this.page.getValue() == AutoAnchor.Page.General).setParent());
   private final BooleanSetting inSpam = this.add(
      new BooleanSetting("WhenSpamming", true, () -> this.page.getValue() == AutoAnchor.Page.General && this.spamPlace.isOpen())
   );
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.page.getValue() == AutoAnchor.Page.General));
   private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting("Swing", SwingSide.All, () -> this.page.getValue() == AutoAnchor.Page.General));
   private final EnumSetting<Timing> timing = this.add(new EnumSetting("Timing", Timing.All, () -> this.page.getValue() == AutoAnchor.Page.General));
   private final SliderSetting placeDelay = this.add(
      new SliderSetting("PlaceDelay", 100.0, 0.0, 500.0, 1.0, () -> this.page.getValue() == AutoAnchor.Page.General).setSuffix("ms")
   );
   private final SliderSetting fillDelay = this.add(
      new SliderSetting("FillDelay", 100.0, 0.0, 500.0, 1.0, () -> this.page.getValue() == AutoAnchor.Page.General).setSuffix("ms")
   );
   private final SliderSetting breakDelay = this.add(
      new SliderSetting("BreakDelay", 100.0, 0.0, 500.0, 1.0, () -> this.page.getValue() == AutoAnchor.Page.General).setSuffix("ms")
   );
   private final SliderSetting spamDelay = this.add(
      new SliderSetting("SpamDelay", 200.0, 0.0, 1000.0, 1.0, () -> this.page.getValue() == AutoAnchor.Page.General).setSuffix("ms")
   );
   private final SliderSetting updateDelay = this.add(
      new SliderSetting("UpdateDelay", 200.0, 0.0, 1000.0, 1.0, () -> this.page.getValue() == AutoAnchor.Page.General).setSuffix("ms")
   );
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == AutoAnchor.Page.Rotate).setParent());
   private final BooleanSetting yawStep = this.add(
      new BooleanSetting("YawStep", true, () -> this.rotate.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate).setParent()
   );
   private final BooleanSetting whenElytra = this.add(
      new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate)
   );
   private final SliderSetting steps = this.add(
      new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate)
   );
   private final BooleanSetting checkFov = this.add(
      new BooleanSetting("OnlyLooking", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate)
   );
   private final SliderSetting fov = this.add(
      new SliderSetting(
         "Fov",
         20.0,
         0.0,
         360.0,
         0.1,
         () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.checkFov.getValue() && this.page.getValue() == AutoAnchor.Page.Rotate
      )
   );
   private final SliderSetting priority = this.add(
      new SliderSetting("Priority", 10, 0, 100, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoAnchor.Page.Rotate)
   );
   private final BooleanSetting noSuicide = this.add(new BooleanSetting("NoSuicide", true, () -> this.page.getValue() == AutoAnchor.Page.Interact));
   private final BooleanSetting smart = this.add(new BooleanSetting("Smart", true, () -> this.page.getValue() == AutoAnchor.Page.Interact));
   private final BooleanSetting terrainIgnore = this.add(new BooleanSetting("TerrainIgnore", true, () -> this.page.getValue() == AutoAnchor.Page.Interact));
   private final SliderSetting minPrefer = this.add(
      new SliderSetting("Prefer", 7.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact).setSuffix("dmg")
   );
   private final SliderSetting maxSelfDamage = this.add(
      new SliderSetting("MaxSelf", 8.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoAnchor.Page.Interact).setSuffix("dmg")
   );
   private final EnumSetting<Aura.TargetESP> mode = this.add(
      new EnumSetting("TargetESP", Aura.TargetESP.Jello, () -> this.page.getValue() == AutoAnchor.Page.Render)
   );
   private final ColorSetting color = this.add(
      new ColorSetting("TargetColor", new Color(255, 255, 255, 250), () -> this.page.getValue() == AutoAnchor.Page.Render)
   );
   private final ColorSetting outlineColor = this.add(
      new ColorSetting("TargetOutlineColor", new Color(255, 255, 255, 250), () -> this.page.getValue() == AutoAnchor.Page.Render)
   );
   private final BooleanSetting render = this.add(new BooleanSetting("Render", true, () -> this.page.getValue() == AutoAnchor.Page.Render));
   private final BooleanSetting shrink = this.add(
      new BooleanSetting("Shrink", true, () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue())
   );
   private final ColorSetting box = this.add(
      new ColorSetting("Box", new Color(255, 255, 255, 255), () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue())
         .injectBoolean(true)
   );
   private final ColorSetting fill = this.add(
      new ColorSetting("Fill", new Color(255, 255, 255, 100), () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue())
         .injectBoolean(true)
   );
   private final SliderSetting sliderSpeed = this.add(
      new SliderSetting("SliderSpeed", 0.2, 0.0, 1.0, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue())
   );
   private final SliderSetting startFadeTime = this.add(
      new SliderSetting("StartFade", 0.3, 0.0, 2.0, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue()).setSuffix("s")
   );
   private final SliderSetting fadeSpeed = this.add(
      new SliderSetting("FadeSpeed", 0.2, 0.01, 1.0, 0.01, () -> this.page.getValue() == AutoAnchor.Page.Render && this.render.getValue())
   );
   private final Timer delayTimer = new Timer();
   private final Timer calcTimer = new Timer();
   private final Timer noPosTimer = new Timer();
   private final Timer assistTimer = new Timer();
   public Vec3d directionVec = null;
   public PlayerEntity displayTarget;
   public BlockPos currentPos;
   public BlockPos tempPos;
   public double lastDamage;
   double fade = 0.0;
   BlockPos assistPos;

   public AutoAnchor() {
      super("AutoAnchor", Module.Category.Combat);
      this.setChinese("重生锚光环");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new AutoAnchor.AnchorRender());
   }

   public static boolean canSee(Vec3d from, Vec3d to) {
      HitResult result = mc.world.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
      return result == null || result.getType() == Type.MISS;
   }

   @Override
   public String getInfo() {
      return this.displayTarget != null && this.currentPos != null ? this.displayTarget.getName().getString() : null;
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (this.displayTarget != null && this.currentPos != null) {
         Aura.doRender(
            matrixStack,
            mc.getRenderTickCounter().getTickDelta(true),
            this.displayTarget,
            this.color.getValue(),
            this.outlineColor.getValue(),
            (Aura.TargetESP)this.mode.getValue()
         );
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.currentPos != null && this.rotate.getValue() && this.shouldYawStep() && this.directionVec != null) {
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @Override
   public void onDisable() {
      this.tempPos = null;
      this.currentPos = null;
   }

   public void onThread() {
      if (!this.isOff() && !nullCheck()) {
         if (this.thread.getValue()) {
            if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
               this.currentPos = null;
               return;
            }

            if (AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.crystalPos != null && this.preferCrystal.getValue()) {
               this.currentPos = null;
               return;
            }

            int anchor = this.inventorySwap.getValue()
               ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR)
               : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
            int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
            int unBlock = this.inventorySwap.getValue() ? anchor : InventoryUtil.findUnBlock();
            if (anchor == -1) {
               this.currentPos = null;
               return;
            }

            if (glowstone == -1) {
               this.currentPos = null;
               return;
            }

            if (unBlock == -1) {
               this.currentPos = null;
               return;
            }

            if (mc.player.isSneaking()) {
               this.currentPos = null;
               return;
            }

            if (this.usingPause.getValue() && mc.player.isUsingItem()) {
               this.currentPos = null;
               return;
            }

            this.calc();
         }
      }
   }

   private boolean shouldYawStep() {
      return this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())
         ? this.yawStep.getValue() && !Velocity.INSTANCE.noRotation()
         : false;
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
            int anchor = this.inventorySwap.getValue()
               ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR)
               : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
            int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
            int unBlock = this.inventorySwap.getValue() ? anchor : InventoryUtil.findUnBlock();
            int old = mc.player.getInventory().selectedSlot;
            if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
               this.currentPos = null;
            } else if (AutoCrystal.INSTANCE.isOn() && AutoCrystal.INSTANCE.crystalPos != null) {
               this.currentPos = null;
            } else if (anchor == -1) {
               this.currentPos = null;
            } else if (glowstone == -1) {
               this.currentPos = null;
            } else if (unBlock == -1) {
               this.currentPos = null;
            } else if (mc.player.isSneaking()) {
               this.currentPos = null;
            } else if (this.usingPause.getValue() && mc.player.isUsingItem()) {
               this.currentPos = null;
            } else if (!this.inventorySwap.getValue() || EntityUtil.inInventory()) {
               if (this.assist.getValue()) {
                  this.onAssist();
               }

               if (!this.thread.getValue()) {
                  this.calc();
               }

               BlockPos pos = this.currentPos;
               if (pos != null) {
                  if (this.breakCrystal.getValue()) {
                     CombatUtil.attackCrystal(new BlockPos(pos), this.rotate.getValue(), false);
                  }

                  boolean shouldSpam = this.spam.getValue() && (!this.mineSpam.getValue() || Alien.BREAK.isMining(pos));
                  if (shouldSpam) {
                     if (!this.delayTimer.passed((long)this.spamDelay.getValueFloat())) {
                        return;
                     }

                     this.delayTimer.reset();
                     if (BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                        this.placeBlock(pos, this.rotate.getValue(), anchor);
                     }

                     if (!this.chargeList.contains(pos)) {
                        this.delayTimer.reset();
                        this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), glowstone);
                        this.chargeList.add(pos);
                     }

                     this.chargeList.remove(pos);
                     this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), unBlock);
                     if (this.spamPlace.getValue() && this.inSpam.getValue()) {
                        if (this.shouldYawStep() && this.checkFov.getValue()) {
                           Direction side = BlockUtil.getClickSide(pos);
                           Vec3d directionVec = new Vec3d(
                              pos.getX() + 0.5 + side.getVector().getX() * 0.5,
                              pos.getY() + 0.5 + side.getVector().getY() * 0.5,
                              pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
                           );
                           if (Alien.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
                              CombatUtil.modifyPos = pos;
                              CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                              this.placeBlock(pos, this.rotate.getValue(), anchor);
                              CombatUtil.modifyPos = null;
                           }
                        } else {
                           CombatUtil.modifyPos = pos;
                           CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                           this.placeBlock(pos, this.rotate.getValue(), anchor);
                           CombatUtil.modifyPos = null;
                        }
                     }
                  } else if (BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                     if (!this.delayTimer.passed((long)this.placeDelay.getValueFloat())) {
                        return;
                     }

                     this.delayTimer.reset();
                     this.placeBlock(pos, this.rotate.getValue(), anchor);
                  } else if (BlockUtil.getBlock(pos) == Blocks.RESPAWN_ANCHOR) {
                     if (!this.chargeList.contains(pos)) {
                        if (!this.delayTimer.passed((long)this.fillDelay.getValueFloat())) {
                           return;
                        }

                        this.delayTimer.reset();
                        this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), glowstone);
                        this.chargeList.add(pos);
                     } else {
                        if (!this.delayTimer.passed((long)this.breakDelay.getValueFloat())) {
                           return;
                        }

                        this.delayTimer.reset();
                        this.chargeList.remove(pos);
                        this.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue(), unBlock);
                        if (this.spamPlace.getValue()) {
                           if (this.shouldYawStep() && this.checkFov.getValue()) {
                              Direction side = BlockUtil.getClickSide(pos);
                              Vec3d directionVec = new Vec3d(
                                 pos.getX() + 0.5 + side.getVector().getX() * 0.5,
                                 pos.getY() + 0.5 + side.getVector().getY() * 0.5,
                                 pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
                              );
                              if (Alien.ROTATION.inFov(directionVec, this.fov.getValueFloat())) {
                                 CombatUtil.modifyPos = pos;
                                 CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                                 this.placeBlock(pos, this.rotate.getValue(), anchor);
                                 CombatUtil.modifyPos = null;
                              }
                           } else {
                              CombatUtil.modifyPos = pos;
                              CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                              this.placeBlock(pos, this.rotate.getValue(), anchor);
                              CombatUtil.modifyPos = null;
                           }
                        }
                     }
                  }

                  if (!this.inventorySwap.getValue()) {
                     this.doSwap(old);
                  }
               }
            }
         }
      }
   }

   private void calc() {
      if (!nullCheck()) {
         if (this.calcTimer.passed((long)this.updateDelay.getValueFloat())) {
            this.calcTimer.reset();
            PlayerEntityPredict selfPredict = new PlayerEntityPredict(
               mc.player,
               this.maxMotionY.getValue(),
               this.selfPredict.getValueInt(),
               this.simulation.getValueInt(),
               this.step.getValue(),
               this.doubleStep.getValue(),
               this.jump.getValue(),
               this.inBlockPause.getValue()
            );
            this.tempPos = null;
            double placeDamage = this.minDamage.getValue();
            double breakDamage = this.breakMin.getValue();
            boolean anchorFound = false;
            List<PlayerEntity> enemies = CombatUtil.getEnemies(this.targetRange.getValue());
            ArrayList<PlayerEntityPredict> list = new ArrayList();

            for (PlayerEntity player : enemies) {
               list.add(
                  new PlayerEntityPredict(
                     player,
                     this.maxMotionY.getValue(),
                     this.predictTicks.getValueInt(),
                     this.simulation.getValueInt(),
                     this.step.getValue(),
                     this.doubleStep.getValue(),
                     this.jump.getValue(),
                     this.inBlockPause.getValue()
                  )
               );
            }

            for (PlayerEntityPredict pap : list) {
               BlockPos pos = EntityUtil.getEntityPos(pap.player, true).up(2);
               double selfDamage;
               double damage;
               if ((
                     BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())
                        || BlockUtil.getBlock(pos) == Blocks.RESPAWN_ANCHOR && BlockUtil.getClickSideStrict(pos) != null
                  )
                  && !((selfDamage = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > this.maxSelfDamage.getValue())
                  && (!this.noSuicide.getValue() || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount()))
                  && (damage = this.getAnchorDamage(pos, pap.player, pap.predict)) > this.headDamage.getValueFloat()
                  && (!this.smart.getValue() || !(selfDamage > damage))) {
                  this.lastDamage = damage;
                  this.displayTarget = pap.player;
                  this.tempPos = pos;
                  break;
               }
            }

            if (this.tempPos == null) {
               for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat() + 1.0F, mc.player.getEyePos())) {
                  for (PlayerEntityPredict papx : list) {
                     if (this.light.getValue()) {
                        CombatUtil.modifyPos = pos;
                        CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                        boolean skip = !canSee(pos.toCenterPos(), papx.predict.getPos());
                        CombatUtil.modifyPos = null;
                        if (skip) {
                           continue;
                        }
                     }

                     if (BlockUtil.getBlock(pos) != Blocks.RESPAWN_ANCHOR) {
                        if (!anchorFound && BlockUtil.canPlace(pos, this.range.getValue(), this.breakCrystal.getValue())) {
                           CombatUtil.modifyPos = pos;
                           CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                           boolean skip = BlockUtil.getClickSideStrict(pos) == null;
                           CombatUtil.modifyPos = null;
                           if (!skip) {
                              double damage = this.getAnchorDamage(pos, papx.player, papx.predict);
                              double selfDamage;
                              if (damage >= placeDamage
                                 && (AutoCrystal.INSTANCE.crystalPos == null || AutoCrystal.INSTANCE.isOff() || AutoCrystal.INSTANCE.lastDamage < damage)
                                 && !((selfDamage = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > this.maxSelfDamage.getValue())
                                 && (!this.noSuicide.getValue() || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount()))
                                 && (!this.smart.getValue() || !(selfDamage > damage))) {
                                 this.lastDamage = damage;
                                 this.displayTarget = papx.player;
                                 placeDamage = damage;
                                 this.tempPos = pos;
                              }
                           }
                        }
                     } else {
                        double damage = this.getAnchorDamage(pos, papx.player, papx.predict);
                        if (BlockUtil.getClickSideStrict(pos) != null && damage >= breakDamage) {
                           if (damage >= this.minPrefer.getValue()) {
                              anchorFound = true;
                           }

                           double selfDamage;
                           if ((anchorFound || !(damage < placeDamage))
                              && (AutoCrystal.INSTANCE.crystalPos == null || AutoCrystal.INSTANCE.isOff() || AutoCrystal.INSTANCE.lastDamage < damage)
                              && !((selfDamage = this.getAnchorDamage(pos, selfPredict.player, selfPredict.predict)) > this.maxSelfDamage.getValue())
                              && (!this.noSuicide.getValue() || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount()))
                              && (!this.smart.getValue() || !(selfDamage > damage))) {
                              this.lastDamage = damage;
                              this.displayTarget = papx.player;
                              breakDamage = damage;
                              this.tempPos = pos;
                           }
                        }
                     }
                  }
               }
            }
         }

         this.currentPos = this.tempPos;
      }
   }

   public double getAnchorDamage(BlockPos anchorPos, PlayerEntity target, PlayerEntity predict) {
      if (this.terrainIgnore.getValue()) {
         CombatUtil.terrainIgnore = true;
      }

      double damage = ExplosionUtil.anchorDamage(anchorPos, target, predict);
      CombatUtil.terrainIgnore = false;
      return damage;
   }

   public void placeBlock(BlockPos pos, boolean rotate, int slot) {
      if (BlockUtil.allowAirPlace()) {
         this.airPlace(pos, rotate, slot);
      } else {
         Direction side = BlockUtil.getPlaceSide(pos);
         if (side != null) {
            this.clickBlock(pos.offset(side), side.getOpposite(), rotate, slot);
         }
      }
   }

   public void clickBlock(BlockPos pos, Direction side, boolean rotate, int slot) {
      if (pos != null) {
         Vec3d directionVec = new Vec3d(
            pos.getX() + 0.5 + side.getVector().getX() * 0.5,
            pos.getY() + 0.5 + side.getVector().getY() * 0.5,
            pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
         );
         if (!rotate || this.faceVector(directionVec)) {
            this.doSwap(slot);
            EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)this.swingMode.getValue());
            BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
            Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
            if (this.inventorySwap.getValue()) {
               this.doSwap(slot);
            }

            if (rotate && !this.shouldYawStep()) {
               Alien.ROTATION.snapBack();
            }
         }
      }
   }

   public void airPlace(BlockPos pos, boolean rotate, int slot) {
      if (pos != null) {
         Direction side = BlockUtil.getClickSide(pos);
         Vec3d directionVec = new Vec3d(
            pos.getX() + 0.5 + side.getVector().getX() * 0.5,
            pos.getY() + 0.5 + side.getVector().getY() * 0.5,
            pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
         );
         if (!rotate || this.faceVector(directionVec)) {
            this.doSwap(slot);
            boolean bypass = AirPlace.INSTANCE.grimBypass.getValue();
            if (bypass) {
               mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
            }

            EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)this.swingMode.getValue());
            BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
            Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(bypass ? Hand.OFF_HAND : Hand.MAIN_HAND, result, id));
            if (bypass) {
               mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
            }

            if (this.inventorySwap.getValue()) {
               this.doSwap(slot);
            }

            if (rotate && !this.shouldYawStep()) {
               Alien.ROTATION.snapBack();
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventorySwap.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   public boolean faceVector(Vec3d directionVec) {
      if (!this.shouldYawStep()) {
         Alien.ROTATION.lookAt(directionVec);
         return true;
      } else {
         this.directionVec = directionVec;
         return Alien.ROTATION.inFov(directionVec, this.fov.getValueFloat()) ? true : !this.checkFov.getValue();
      }
   }

   public void onAssist() {
      this.assistPos = null;
      int anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
      int glowstone = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.GLOWSTONE) : InventoryUtil.findBlock(Blocks.GLOWSTONE);
      int old = mc.player.getInventory().selectedSlot;
      if (anchor != -1) {
         if (this.obsidian.getValue()) {
            anchor = this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
            if (anchor == -1) {
               return;
            }
         }

         if (glowstone != -1) {
            if (!mc.player.isSneaking()) {
               if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
                  if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                     if (this.assistTimer.passed((long)(this.delay.getValueFloat() * 1000.0F))) {
                        this.assistTimer.reset();
                        ArrayList<PlayerEntityPredict> list = new ArrayList();

                        for (PlayerEntity player : CombatUtil.getEnemies(this.assistRange.getValue())) {
                           list.add(
                              new PlayerEntityPredict(
                                 player,
                                 this.maxMotionY.getValue(),
                                 this.predictTicks.getValueInt(),
                                 this.simulation.getValueInt(),
                                 this.step.getValue(),
                                 this.doubleStep.getValue(),
                                 this.jump.getValue(),
                                 this.inBlockPause.getValue()
                              )
                           );
                        }

                        double bestDamage = this.assistDamage.getValue();

                        for (PlayerEntityPredict pap : list) {
                           BlockPos pos = EntityUtil.getEntityPos(pap.player, true).up(2);
                           if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                              return;
                           }

                           if (BlockUtil.clientCanPlace(pos, false)) {
                              double damage = this.getAnchorDamage(pos, pap.player, pap.predict);
                              if (damage >= bestDamage) {
                                 bestDamage = damage;
                                 this.assistPos = pos;
                              }
                           }

                           for (Direction i : Direction.values()) {
                              if (i != Direction.UP && i != Direction.DOWN && BlockUtil.clientCanPlace(pos.offset(i), false)) {
                                 double damage = this.getAnchorDamage(pos.offset(i), pap.player, pap.predict);
                                 if (damage >= bestDamage) {
                                    bestDamage = damage;
                                    this.assistPos = pos.offset(i);
                                 }
                              }
                           }
                        }

                        BlockPos placePos;
                        if (this.assistPos != null
                           && BlockUtil.getPlaceSide(this.assistPos, this.range.getValue()) == null
                           && (placePos = this.getHelper(this.assistPos)) != null) {
                           this.doSwap(anchor);
                           BlockUtil.placeBlock(placePos, this.rotate.getValue());
                           if (this.inventorySwap.getValue()) {
                              this.doSwap(anchor);
                           } else {
                              this.doSwap(old);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public BlockPos getHelper(BlockPos pos) {
      for (Direction i : Direction.values()) {
         if ((!this.checkMine.getValue() || !Alien.BREAK.isMining(pos.offset(i)))
            && BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite())
            && BlockUtil.canPlace(pos.offset(i))) {
            return pos.offset(i);
         }
      }

      return null;
   }

   public class AnchorRender {
      @EventListener
      public void onRender3D(Render3DEvent event) {
         BlockPos currentPos = AutoAnchor.INSTANCE.currentPos;
         if (currentPos != null) {
            AutoAnchor.this.noPosTimer.reset();
            AutoAnchor.placeVec3d = currentPos.toCenterPos();
         }

         if (AutoAnchor.placeVec3d != null) {
            if (AutoAnchor.this.fadeSpeed.getValue() >= 1.0) {
               AutoAnchor.this.fade = AutoAnchor.this.noPosTimer.passed((long)(AutoAnchor.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5;
            } else {
               AutoAnchor.this.fade = AnimateUtil.animate(
                  AutoAnchor.this.fade,
                  AutoAnchor.this.noPosTimer.passed((long)(AutoAnchor.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5,
                  AutoAnchor.this.fadeSpeed.getValue() / 10.0
               );
            }

            if (AutoAnchor.this.fade == 0.0) {
               AutoAnchor.curVec3d = null;
            } else {
               if (AutoAnchor.curVec3d != null && !(AutoAnchor.this.sliderSpeed.getValue() >= 1.0)) {
                  AutoAnchor.curVec3d = new Vec3d(
                     AnimateUtil.animate(AutoAnchor.curVec3d.x, AutoAnchor.placeVec3d.x, AutoAnchor.this.sliderSpeed.getValue() / 10.0),
                     AnimateUtil.animate(AutoAnchor.curVec3d.y, AutoAnchor.placeVec3d.y, AutoAnchor.this.sliderSpeed.getValue() / 10.0),
                     AnimateUtil.animate(AutoAnchor.curVec3d.z, AutoAnchor.placeVec3d.z, AutoAnchor.this.sliderSpeed.getValue() / 10.0)
                  );
               } else {
                  AutoAnchor.curVec3d = AutoAnchor.placeVec3d;
               }

               if (AutoAnchor.this.render.getValue()) {
                  Box cbox = new Box(AutoAnchor.curVec3d, AutoAnchor.curVec3d);
                  if (AutoAnchor.this.shrink.getValue()) {
                     cbox = cbox.expand(AutoAnchor.this.fade);
                  } else {
                     cbox = cbox.expand(0.5);
                  }

                  if (AutoAnchor.this.fill.booleanValue) {
                     event.drawFill(
                        cbox,
                        ColorUtil.injectAlpha(AutoAnchor.this.fill.getValue(), (int)(AutoAnchor.this.fill.getValue().getAlpha() * AutoAnchor.this.fade * 2.0))
                     );
                  }

                  if (AutoAnchor.this.box.booleanValue) {
                     event.drawBox(
                        cbox,
                        ColorUtil.injectAlpha(AutoAnchor.this.box.getValue(), (int)(AutoAnchor.this.box.getValue().getAlpha() * AutoAnchor.this.fade * 2.0))
                     );
                  }
               }
            }
         }
      }
   }

   public static enum Page {
      General,
      Interact,
      Predict,
      Rotate,
      Assist,
      Render;
   }
}
