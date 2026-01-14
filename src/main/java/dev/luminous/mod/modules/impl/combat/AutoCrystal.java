package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.*;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.entity.PlayerEntityPredict;
import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.DamageUtils;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.math.ExplosionUtil;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.JelloUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.text.DecimalFormat;
import java.util.ArrayList;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AutoCrystal extends Module {
   public static AutoCrystal INSTANCE;
   public BlockPos crystalPos;
   public final Timer lastBreakTimer = new Timer();
   private final EnumSetting<AutoCrystal.Page> page = this.add(new EnumSetting("Page", AutoCrystal.Page.General));
   final Animation animation = new Animation();
   final DecimalFormat df = new DecimalFormat("0.0");
   private final Timer baseTimer = new Timer();
   private final Timer placeTimer = new Timer();
   private final Timer noPosTimer = new Timer();
   private final Timer switchTimer = new Timer();
   private final Timer calcDelay = new Timer();
   private final BindSetting pause = this.add(new BindSetting("Pause", -1, () -> this.page.is(AutoCrystal.Page.Check)));
   private final BooleanSetting preferAnchor = this.add(new BooleanSetting("PreferAnchor", true, () -> this.page.getValue() == AutoCrystal.Page.Check));
   private final BooleanSetting breakOnlyHasCrystal = this.add(new BooleanSetting("OnlyHold", true, () -> this.page.getValue() == AutoCrystal.Page.Check));
   private final BooleanSetting eatingPause = this.add(new BooleanSetting("EatingPause", true, () -> this.page.getValue() == AutoCrystal.Page.Check));
   private final SliderSetting switchCooldown = this.add(
      new SliderSetting("SwitchPause", 100, 0, 1000, () -> this.page.getValue() == AutoCrystal.Page.Check).setSuffix("ms")
   );
   private final SliderSetting targetRange = this.add(
      new SliderSetting("TargetRange", 12.0, 0.0, 20.0, () -> this.page.getValue() == AutoCrystal.Page.Check).setSuffix("m")
   );
   private final SliderSetting updateDelay = this.add(
      new SliderSetting("UpdateDelay", 50, 0, 1000, () -> this.page.getValue() == AutoCrystal.Page.Check).setSuffix("ms")
   );
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == AutoCrystal.Page.Rotation).setParent());
   private final BooleanSetting onPlace = this.add(
      new BooleanSetting("OnPlace", false, () -> this.rotate.isOpen() && this.page.getValue() == AutoCrystal.Page.Rotation)
   );
   private final BooleanSetting onBreak = this.add(
      new BooleanSetting("OnBreak", false, () -> this.rotate.isOpen() && this.page.getValue() == AutoCrystal.Page.Rotation)
   );
   private final BooleanSetting yawStep = this.add(
      new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.page.getValue() == AutoCrystal.Page.Rotation).setParent()
   );
   private final BooleanSetting whenElytra = this.add(
      new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.page.getValue() == AutoCrystal.Page.Rotation && this.yawStep.isOpen())
   );
   private final SliderSetting steps = this.add(
      new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoCrystal.Page.Rotation)
   );
   private final BooleanSetting checkFov = this.add(
      new BooleanSetting("OnlyLooking", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoCrystal.Page.Rotation)
   );
   private final SliderSetting fov = this.add(
      new SliderSetting(
         "Fov",
         20.0,
         0.0,
         360.0,
         0.1,
         () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.checkFov.getValue() && this.page.getValue() == AutoCrystal.Page.Rotation
      )
   );
   private final SliderSetting priority = this.add(
      new SliderSetting("Priority", 10, 0, 100, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == AutoCrystal.Page.Rotation)
   );
   private final SliderSetting minDamage = this.add(
      new SliderSetting("Min", 5.0, 0.0, 36.0, () -> this.page.getValue() == AutoCrystal.Page.General).setSuffix("dmg")
   );
   private final SliderSetting maxSelf = this.add(
      new SliderSetting("Max", 12.0, 0.0, 36.0, () -> this.page.getValue() == AutoCrystal.Page.General).setSuffix("dmg")
   );
   private final SliderSetting reserve = this.add(
      new SliderSetting("Reserve", 2.0, 0.0, 10.0, () -> this.page.getValue() == AutoCrystal.Page.General).setSuffix("hp")
   );
   private final BooleanSetting balance = this.add(new BooleanSetting("Balance", true, () -> this.page.getValue() == AutoCrystal.Page.General).setParent());
   private final SliderSetting balanceOffset = this.add(
      new SliderSetting("BalanceOffset", 0.0, -20.0, 20.0, 0.1, () -> this.page.getValue() == AutoCrystal.Page.General && this.balance.isOpen())
         .setSuffix("hp")
   );
   private final BooleanSetting place = this.add(new BooleanSetting("Place", true, () -> this.page.getValue() == AutoCrystal.Page.General).setParent());
   public final SliderSetting placeRange = this.add(
      new SliderSetting("PlaceRange", 5.0, 0.0, 6.0, 0.01, () -> this.page.getValue() == AutoCrystal.Page.General && this.place.isOpen()).setSuffix("m")
   );
   private final SliderSetting placeDelay = this.add(
      new SliderSetting("PlaceDelay", 300, 0, 1000, () -> this.page.getValue() == AutoCrystal.Page.General && this.place.isOpen()).setSuffix("ms")
   );
   private final EnumSetting<AutoCrystal.SwapMode> autoSwap = this.add(
      new EnumSetting("AutoSwap", AutoCrystal.SwapMode.None, () -> this.page.getValue() == AutoCrystal.Page.General && this.place.isOpen())
   );
   private final BooleanSetting afterBreak = this.add(
      new BooleanSetting("AfterBreak", true, () -> this.page.getValue() == AutoCrystal.Page.General && this.place.isOpen())
   );
   private final BooleanSetting forcePlace = this.add(
      new BooleanSetting("ForcePlace", false, () -> this.page.getValue() == AutoCrystal.Page.General && this.place.isOpen())
   );
   private final BooleanSetting breakSetting = this.add(new BooleanSetting("Break", true, () -> this.page.getValue() == AutoCrystal.Page.General).setParent());
   public final SliderSetting breakRange = this.add(
      new SliderSetting("BreakRange", 4.0, 0.0, 6.0, 0.01, () -> this.page.getValue() == AutoCrystal.Page.General && this.breakSetting.isOpen()).setSuffix("m")
   );
   private final SliderSetting breakDelay = this.add(
      new SliderSetting("BreakDelay", 300, 0, 1000, () -> this.page.getValue() == AutoCrystal.Page.General && this.breakSetting.isOpen()).setSuffix("ms")
   );
   private final SliderSetting minAge = this.add(
      new SliderSetting("MinAge", 0, 0, 20, () -> this.page.getValue() == AutoCrystal.Page.General && this.breakSetting.isOpen()).setSuffix("tick")
   );
   private final BooleanSetting breakRemove = this.add(
      new BooleanSetting("Remove", false, () -> this.page.getValue() == AutoCrystal.Page.General && this.breakSetting.isOpen())
   );
   private final BooleanSetting onAdd = this.add(
      new BooleanSetting("OnAdd", false, () -> this.page.getValue() == AutoCrystal.Page.General && this.breakSetting.isOpen())
   );
   private final BooleanSetting resetCD = this.add(
      new BooleanSetting("ResetAttack", true, () -> this.page.getValue() == AutoCrystal.Page.General && this.breakSetting.isOpen())
   );
   private final EnumSetting<Timing> timing = this.add(new EnumSetting("Timing", Timing.All, () -> this.page.getValue() == AutoCrystal.Page.General));
   private final BooleanSetting interactOnRender = this.add(
      new BooleanSetting("InteractOnRender", false, () -> this.page.getValue() == AutoCrystal.Page.General)
   );
   private final SliderSetting wallRange = this.add(
      new SliderSetting("WallRange", 6.0, 0.0, 6.0, () -> this.page.getValue() == AutoCrystal.Page.General).setSuffix("m")
   );
   private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting("Swing", SwingSide.All, () -> this.page.getValue() == AutoCrystal.Page.General));
   private final ColorSetting text = this.add(
      new ColorSetting("Text", new Color(-1), () -> this.page.getValue() == AutoCrystal.Page.Render).injectBoolean(true)
   );
   private final EnumSetting<AutoCrystal.TargetESP> mode = this.add(
      new EnumSetting("TargetESP", AutoCrystal.TargetESP.Fill, () -> this.page.getValue() == AutoCrystal.Page.Render)
   );
   private final SliderSetting animationTime = this.add(
      new SliderSetting("AnimationTime", 200.0, 0.0, 2000.0, 1.0, () -> this.page.getValue() == AutoCrystal.Page.Render)
   );
   private final EnumSetting<Easing> ease = this.add(new EnumSetting("Ease", Easing.CubicInOut, () -> this.page.getValue() == AutoCrystal.Page.Render));
   private final ColorSetting color = this.add(
      new ColorSetting("TargetColor", new Color(255, 255, 255, 50), () -> this.page.getValue() == AutoCrystal.Page.Render)
   );
   private final ColorSetting outlineColor = this.add(
      new ColorSetting("TargetOutlineColor", new Color(255, 255, 255, 50), () -> this.page.getValue() == AutoCrystal.Page.Render)
   );
   private final ColorSetting hitColor = this.add(
      new ColorSetting("HitColor", new Color(255, 255, 255, 150), () -> this.page.getValue() == AutoCrystal.Page.Render)
   );
   private final ColorSetting hitOutlineColor = this.add(
      new ColorSetting("HitOutlineColor", new Color(255, 255, 255, 150), () -> this.page.getValue() == AutoCrystal.Page.Render)
   );
   private final BooleanSetting render = this.add(new BooleanSetting("Render", true, () -> this.page.getValue() == AutoCrystal.Page.Render));
   private final BooleanSetting sync = this.add(
      new BooleanSetting("Sync", true, () -> this.page.getValue() == AutoCrystal.Page.Render && this.render.getValue())
   );
   private final BooleanSetting shrink = this.add(
      new BooleanSetting("Shrink", true, () -> this.page.getValue() == AutoCrystal.Page.Render && this.render.getValue())
   );
   private final ColorSetting box = this.add(
      new ColorSetting("Box", new Color(255, 255, 255, 255), () -> this.page.getValue() == AutoCrystal.Page.Render && this.render.getValue())
         .injectBoolean(true)
   );
   private final SliderSetting lineWidth = this.add(
      new SliderSetting("LineWidth", 1.5, 0.01, 3.0, 0.01, () -> this.page.getValue() == AutoCrystal.Page.Render && this.render.getValue())
   );
   private final ColorSetting fill = this.add(
      new ColorSetting("Fill", new Color(255, 255, 255, 100), () -> this.page.getValue() == AutoCrystal.Page.Render && this.render.getValue())
         .injectBoolean(true)
   );
   private final SliderSetting sliderSpeed = this.add(
      new SliderSetting("SliderSpeed", 0.2, 0.01, 1.0, 0.01, () -> this.page.getValue() == AutoCrystal.Page.Render && this.render.getValue())
   );
   private final SliderSetting startFadeTime = this.add(
      new SliderSetting("StartFade", 0.3, 0.0, 2.0, 0.01, () -> this.page.getValue() == AutoCrystal.Page.Render && this.render.getValue()).setSuffix("s")
   );
   private final SliderSetting fadeSpeed = this.add(
      new SliderSetting("FadeSpeed", 0.2, 0.01, 1.0, 0.01, () -> this.page.getValue() == AutoCrystal.Page.Render && this.render.getValue())
   );
   private final SliderSetting attackVecStep = this.add(
      new SliderSetting("AttackVecStep", 0.1, 0.01, 1.0, 0.01, () -> this.page.getValue() == AutoCrystal.Page.Calc)
   );
   private final BooleanSetting thread = this.add(new BooleanSetting("Thread", false, () -> this.page.getValue() == AutoCrystal.Page.Calc));
   private final BooleanSetting doCrystal = this.add(new BooleanSetting("InteractInCalc", false, () -> this.page.getValue() == AutoCrystal.Page.Calc));
   private final SliderSetting selfPredict = this.add(
      new SliderSetting("SelfPredict", 0, 0, 20, () -> this.page.getValue() == AutoCrystal.Page.Calc).setSuffix("ticks")
   );
   private final SliderSetting predictTicks = this.add(
      new SliderSetting("Predict", 4, 0, 20, () -> this.page.getValue() == AutoCrystal.Page.Calc).setSuffix("ticks")
   );
   private final SliderSetting simulation = this.add(new SliderSetting("Simulation", 5.0, 0.0, 20.0, 1.0, () -> this.page.getValue() == AutoCrystal.Page.Calc));
   private final SliderSetting maxMotionY = this.add(new SliderSetting("MaxMotionY", 0.34, 0.0, 2.0, 0.01, () -> this.page.getValue() == AutoCrystal.Page.Calc));
   private final BooleanSetting step = this.add(new BooleanSetting("Step", false, () -> this.page.getValue() == AutoCrystal.Page.Calc));
   private final BooleanSetting doubleStep = this.add(new BooleanSetting("DoubleStep", false, () -> this.page.getValue() == AutoCrystal.Page.Calc));
   private final BooleanSetting jump = this.add(new BooleanSetting("Jump", false, () -> this.page.getValue() == AutoCrystal.Page.Calc));
   private final BooleanSetting inBlockPause = this.add(new BooleanSetting("InBlockPause", true, () -> this.page.getValue() == AutoCrystal.Page.Calc));
   private final BooleanSetting terrainIgnore = this.add(new BooleanSetting("TerrainIgnore", true, () -> this.page.getValue() == AutoCrystal.Page.Calc));
   private final BooleanSetting basePlace = this.add(new BooleanSetting("BasePlace", true, () -> this.page.getValue() == AutoCrystal.Page.Base));
   private final SliderSetting baseMin = this.add(
      new SliderSetting("BaseMin", 6.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoCrystal.Page.Base).setSuffix("hp")
   );
   private final SliderSetting baseMax = this.add(
      new SliderSetting("BaseMax", 12.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoCrystal.Page.Base).setSuffix("hp")
   );
   private final SliderSetting overrideMax = this.add(
      new SliderSetting("MaxOverride", 8.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoCrystal.Page.Base).setSuffix("hp")
   );
   private final BooleanSetting baseBalance = this.add(new BooleanSetting("BaseBalance", true, () -> this.page.getValue() == AutoCrystal.Page.Base));
   private final BooleanSetting onlyBelow = this.add(new BooleanSetting("OnlyBelow", true, () -> this.page.getValue() == AutoCrystal.Page.Base));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.getValue() == AutoCrystal.Page.Base));
   private final BooleanSetting detectMining = this.add(new BooleanSetting("DetectMining", true, () -> this.page.getValue() == AutoCrystal.Page.Base));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 3000, 0, 10000, () -> this.page.getValue() == AutoCrystal.Page.Base).setSuffix("ms"));
   private final BooleanSetting ignoreMine = this.add(new BooleanSetting("IgnoreMine", true, () -> this.page.getValue() == AutoCrystal.Page.Misc).setParent());
   private final SliderSetting constantProgress = this.add(
      new SliderSetting("Progress", 90.0, 0.0, 100.0, () -> this.page.getValue() == AutoCrystal.Page.Misc && this.ignoreMine.isOpen()).setSuffix("%")
   );
   private final BooleanSetting antiSurround = this.add(
      new BooleanSetting("AntiSurround", false, () -> this.page.getValue() == AutoCrystal.Page.Misc).setParent()
   );
   private final SliderSetting miningProgress = this.add(
      new SliderSetting("MiningProgress", 90.0, 0.0, 100.0, () -> this.page.getValue() == AutoCrystal.Page.Misc && this.antiSurround.isOpen()).setSuffix("%")
   );
   private final SliderSetting antiSurroundMax = this.add(
      new SliderSetting("WhenLower", 5.0, 0.0, 36.0, () -> this.page.getValue() == AutoCrystal.Page.Misc && this.antiSurround.isOpen()).setSuffix("dmg")
   );
   private final BooleanSetting slowPlace = this.add(new BooleanSetting("Timeout", true, () -> this.page.getValue() == AutoCrystal.Page.Misc).setParent());
   private final SliderSetting slowDelay = this.add(
      new SliderSetting("TimeoutDelay", 600, 0, 2000, () -> this.page.getValue() == AutoCrystal.Page.Misc && this.slowPlace.isOpen()).setSuffix("ms")
   );
   private final SliderSetting slowMinDamage = this.add(
      new SliderSetting("TimeoutMin", 1.5, 0.0, 36.0, () -> this.page.getValue() == AutoCrystal.Page.Misc && this.slowPlace.isOpen()).setSuffix("dmg")
   );
   private final BooleanSetting lethalOverride = this.add(
      new BooleanSetting("LethalOverride", true, () -> this.page.getValue() == AutoCrystal.Page.Misc).setParent()
   );
   private final SliderSetting forceMaxHealth = this.add(
      new SliderSetting("LowerThan", 7.0, 0.0, 36.0, 0.1, () -> this.page.getValue() == AutoCrystal.Page.Misc && this.lethalOverride.isOpen())
         .setSuffix("health")
   );
   private final SliderSetting forceMin = this.add(
      new SliderSetting("ForceMin", 1.5, 0.0, 36.0, () -> this.page.getValue() == AutoCrystal.Page.Misc && this.lethalOverride.isOpen()).setSuffix("dmg")
   );
   private final BooleanSetting armorBreaker = this.add(
      new BooleanSetting("ArmorBreaker", true, () -> this.page.getValue() == AutoCrystal.Page.Misc).setParent()
   );
   private final SliderSetting maxDurable = this.add(
      new SliderSetting("MaxDurable", 8, 0, 100, () -> this.page.getValue() == AutoCrystal.Page.Misc && this.armorBreaker.isOpen()).setSuffix("%")
   );
   private final SliderSetting armorBreakerDamage = this.add(
      new SliderSetting("BreakerMin", 3.0, 0.0, 36.0, () -> this.page.getValue() == AutoCrystal.Page.Misc && this.armorBreaker.isOpen()).setSuffix("dmg")
   );
   private final BooleanSetting forceWeb = this.add(new BooleanSetting("WebReset", true, () -> this.page.getValue() == AutoCrystal.Page.Misc).setParent());
   public final BooleanSetting airPlace = this.add(
      new BooleanSetting("AirPlace", false, () -> this.page.getValue() == AutoCrystal.Page.Misc && this.forceWeb.isOpen())
   );
   public final BooleanSetting replace = this.add(
      new BooleanSetting("Replace", false, () -> this.page.getValue() == AutoCrystal.Page.Misc && this.forceWeb.isOpen())
   );
   private final SliderSetting hurtTime = this.add(new SliderSetting("HurtTime", 10.0, 0.0, 10.0, 1.0, () -> this.page.getValue() == AutoCrystal.Page.Misc));
   private final SliderSetting waitHurt = this.add(new SliderSetting("WaitHurt", 10.0, 0.0, 10.0, 1.0, () -> this.page.getValue() == AutoCrystal.Page.Misc));
   private final SliderSetting syncTimeout = this.add(
      new SliderSetting("WaitTimeOut", 500.0, 0.0, 2000.0, 10.0, () -> this.page.getValue() == AutoCrystal.Page.Misc)
   );
   private final Timer syncTimer = new Timer();
   public PlayerEntity displayTarget;
   public float breakDamage;
   public float tempDamage;
   public float lastDamage;
   public Vec3d directionVec = null;
   double currentFade = 0.0;
   private EndCrystalEntity tempBreakCrystal;
   private EndCrystalEntity breakCrystal;
   private BlockPos tempPos;
   private BlockPos syncPos;
   private Vec3d placeVec3d;
   private Vec3d curVec3d;
   int lastSlot;
   BlockPos tempBasePos;
   BlockPos basePos;

   public AutoCrystal() {
      super("AutoCrystal", Module.Category.Combat);
      this.setChinese("自动水晶");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new AutoCrystal.CrystalRender());
   }

   @Override
   public String getInfo() {
      return this.displayTarget != null && this.lastDamage > 0.0F ? this.df.format(this.lastDamage) : null;
   }

   @Override
   public void onDisable() {
      this.crystalPos = null;
      this.tempPos = null;
   }

   @Override
   public void onEnable() {
      this.crystalPos = null;
      this.tempPos = null;
      this.tempBreakCrystal = null;
      this.displayTarget = null;
      this.syncTimer.reset();
      this.lastBreakTimer.reset();
   }

   public void onThread() {
      if (!this.isOff()) {
         if (this.thread.getValue()) {
            this.updateCrystalPos();
         }
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
            if (!this.thread.getValue()) {
               this.updateCrystalPos();
            }

            if (!this.shouldReturn()) {
               this.doInteract();
               BlockPos basePos = this.basePos;
               if (this.basePlace.getValue() && basePos != null && BlockUtil.canPlace(basePos)) {
                  this.doPlace(basePos);
               }
            }
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (this.interactOnRender.getValue() && !this.shouldReturn()) {
         this.doInteract();
         BlockPos basePos = this.basePos;
         if (this.basePlace.getValue() && basePos != null && BlockUtil.canPlace(basePos)) {
            this.doPlace(basePos);
         }
      }

      if (this.displayTarget != null && !this.noPosTimer.passed(500L)) {
         this.doRender(matrixStack, mc.getRenderTickCounter().getTickDelta(true), this.displayTarget, (AutoCrystal.TargetESP)this.mode.getValue());
      }
   }

   public void doRender(MatrixStack stack, float partialTicks, Entity entity, AutoCrystal.TargetESP mode) {
      switch (mode) {
         case Box:
            Render3DUtil.draw3DBox(
               stack,
               ((IEntity)entity)
                  .getDimensions()
                  .getBoxAt(
                     new Vec3d(
                        MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)partialTicks)
                     )
                  )
                  .expand(0.0, 0.1, 0.0),
               ColorUtil.fadeColor(
                  this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), (Easing)this.ease.getValue())
               ),
               ColorUtil.fadeColor(
                  this.outlineColor.getValue(),
                  this.hitOutlineColor.getValue(),
                  this.animation.get(0.0, this.animationTime.getValueInt(), (Easing)this.ease.getValue())
               ),
               true,
               true
            );
            break;
         case Fill:
            Render3DUtil.draw3DBox(
               stack,
               ((IEntity)entity)
                  .getDimensions()
                  .getBoxAt(
                     new Vec3d(
                        MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)partialTicks)
                     )
                  )
                  .expand(0.0, 0.1, 0.0),
               ColorUtil.fadeColor(
                  this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), (Easing)this.ease.getValue())
               ),
               ColorUtil.fadeColor(
                  this.outlineColor.getValue(),
                  this.hitOutlineColor.getValue(),
                  this.animation.get(0.0, this.animationTime.getValueInt(), (Easing)this.ease.getValue())
               ),
               false,
               true
            );
            break;
         case Jello:
            JelloUtil.drawJello(stack, entity, this.color.getValue());
            break;
         case ThunderHack:
            Render3DUtil.drawTargetEsp(stack, this.displayTarget, this.color.getValue());
      }
   }

   private void doInteract() {
      BlockPos crystalPos = this.crystalPos;
      if (crystalPos != null) {
         this.doCrystal(crystalPos);
      }

      if (this.breakCrystal != null) {
         this.doBreak(this.breakCrystal);
         this.breakCrystal = null;
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.rotate.getValue()
         && this.shouldYawStep()
         && this.directionVec != null
         && this.displayTarget != null
         && !this.noPosTimer.passed(1000L)
         && !this.shouldReturn()) {
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @EventListener(priority = -199)
   public void onPacketSend(PacketEvent.Send event) {
      if (!event.isCancelled()) {
         if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket packet && this.lastSlot != packet.getSelectedSlot()) {
            this.lastSlot = packet.getSelectedSlot();
            this.switchTimer.reset();
         }
      }
   }

   public Vec3d getAttackVec(Vec3d feetPos) {
      return MathUtil.getPointToBoxFromBottom(mc.player.getEyePos(), feetPos, this.breakRange.getValue(), 2.0, this.attackVecStep.getValue());
   }

   private void updateCrystalPos() {
      if (this.calcDelay.passedMs(this.updateDelay.getValue())) {
         this.calcDelay.reset();
         this.calcCrystalPos();
         CombatUtil.modifyPos = null;
         CombatUtil.modifyBlockState = null;
         this.basePos = this.tempBasePos;
         this.lastDamage = this.tempDamage;
         this.breakCrystal = this.tempBreakCrystal;
         this.crystalPos = this.tempPos;
      }
   }

   private boolean shouldReturn() {
      if ((!this.eatingPause.getValue() || !mc.player.isUsingItem()) && (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue())) {
         if (this.preferAnchor.getValue() && AutoAnchor.INSTANCE.currentPos != null) {
            this.lastBreakTimer.reset();
            return true;
         } else if (this.pause.isPressed()) {
            this.lastBreakTimer.reset();
            return true;
         } else {
            return false;
         }
      } else {
         this.lastBreakTimer.reset();
         return true;
      }
   }

   private void calcCrystalPos() {
      if (!nullCheck()) {
         if (this.breakOnlyHasCrystal.getValue()
            && !mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL)
            && !mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL)
            && !this.hasCrystal()) {
            this.tempPos = null;
            this.tempBreakCrystal = null;
            this.lastBreakTimer.reset();
         } else {
            boolean shouldReturn = this.shouldReturn();
            boolean needBasePlace = this.basePlace.getValue() && this.baseTimer.passedMs(this.delay.getValue()) && this.getBlock() != -1;
            this.tempBreakCrystal = null;
            this.breakDamage = 0.0F;
            this.tempPos = null;
            this.tempDamage = 0.0F;
            this.tempBasePos = null;
            float baseDamage = 0.0F;
            ArrayList<PlayerEntityPredict> list = new ArrayList();

            for (PlayerEntity target : CombatUtil.getEnemies(this.targetRange.getValueFloat())) {
               if (target.hurtTime <= this.hurtTime.getValueInt()) {
                  list.add(
                     new PlayerEntityPredict(
                        target,
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
            }

            PlayerEntityPredict self = new PlayerEntityPredict(
               mc.player,
               this.maxMotionY.getValue(),
               this.selfPredict.getValueInt(),
               this.simulation.getValueInt(),
               this.step.getValue(),
               this.doubleStep.getValue(),
               this.jump.getValue(),
               this.inBlockPause.getValue()
            );
            if (list.isEmpty()) {
               this.lastBreakTimer.reset();
            } else {
               for (Entity entity : Alien.THREAD.getEntities()) {
                  if (entity instanceof EndCrystalEntity crystal && entity.age >= this.minAge.getValueInt()) {
                     Vec3d attackVec = this.getAttackVec(crystal.getPos());
                     if (attackVec != null && (mc.player.canSee(crystal) || !(mc.player.getEyePos().distanceTo(attackVec) > this.wallRange.getValue()))
                        )
                      {
                        float selfDamage = this.calculateDamage(crystal.getPos(), self.player, self.predict);

                        for (PlayerEntityPredict pap : list) {
                           float damage = this.calculateDamage(crystal.getPos(), pap.player, pap.predict);
                           if (damage > this.breakDamage
                              && !(selfDamage > this.maxSelf.getValue())
                              && (
                                 !(this.reserve.getValue() > 0.0)
                                    || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                              )
                              && (
                                 !(damage < EntityUtil.getHealth(pap.player))
                                    || !(damage < this.getDamage(pap.player))
                                       && (
                                          !this.balance.getValue()
                                             || (
                                                this.getDamage(pap.player) == this.forceMin.getValue()
                                                   ? !(damage < selfDamage - 2.5)
                                                   : !(damage < selfDamage + this.balanceOffset.getValue())
                                             )
                                       )
                              )) {
                              this.breakDamage = damage;
                              this.tempBreakCrystal = crystal;
                              this.displayTarget = pap.player;
                           }
                        }
                     }
                  }
               }

               if (this.doCrystal.getValue() && this.tempBreakCrystal != null && !shouldReturn) {
                  this.doBreak(this.tempBreakCrystal);
                  this.tempBreakCrystal = null;
               }

               for (BlockPos pos : BlockUtil.getSphere((float)this.breakRange.getValue() + 1.5F)) {
                  boolean base = false;
                  CombatUtil.modifyPos = null;
                  CombatUtil.modifyBlockState = null;
                  if (needBasePlace && BlockUtil.canPlace(pos.down())) {
                     CombatUtil.modifyPos = pos.down();
                     CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                     base = true;
                  }

                  if (!base || !Alien.BREAK.isMining(pos.down()) || !this.detectMining.getValue()) {
                     Vec3d attackVec = this.getAttackVec(pos.toBottomCenterPos());
                     if (attackVec != null && !this.behindWall(pos, attackVec) && this.canTouch(pos.down()) && this.canPlaceCrystal(pos, true, false)) {
                        float selfDamage = base
                           ? this.calculateBaseDamage(pos, self.player, self.predict)
                           : this.calculateDamage(pos, self.player, self.predict);

                        for (PlayerEntityPredict papx : list) {
                           if (!base || !this.onlyBelow.getValue() || !(pos.getY() - 0.5 > papx.player.getY())) {
                              float damage = base
                                 ? this.calculateBaseDamage(pos, papx.player, papx.predict)
                                 : this.calculateDamage(pos, papx.player, papx.predict);
                              if (base) {
                                 if (this.tempDamage <= this.overrideMax.getValue()
                                    && damage > this.tempDamage
                                    && damage > baseDamage
                                    && !(selfDamage > this.baseMax.getValue())
                                    && (
                                       !(this.reserve.getValue() > 0.0)
                                          || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                                    )
                                    && (
                                       !(damage < EntityUtil.getHealth(papx.player))
                                          || !(damage < this.baseMin.getValue()) && (!this.baseBalance.getValue() || !(damage < selfDamage))
                                    )) {
                                    this.displayTarget = papx.player;
                                    baseDamage = damage;
                                    this.tempBasePos = pos.down();
                                    this.tempPos = null;
                                 }
                              } else if (damage > this.tempDamage
                                 && (damage >= baseDamage || this.tempDamage > this.overrideMax.getValue())
                                 && !(selfDamage > this.maxSelf.getValue())
                                 && (
                                    !(this.reserve.getValue() > 0.0)
                                       || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                                 )
                                 && (
                                    !(damage < EntityUtil.getHealth(papx.player))
                                       || !(damage < this.getDamage(papx.player))
                                          && (
                                             !this.balance.getValue()
                                                || (
                                                   this.getDamage(papx.player) == this.forceMin.getValue()
                                                      ? !(damage < selfDamage - 2.5)
                                                      : !(damage < selfDamage + this.balanceOffset.getValue())
                                                )
                                          )
                                 )) {
                                 this.displayTarget = papx.player;
                                 this.tempPos = pos;
                                 this.tempBasePos = null;
                                 this.tempDamage = damage;
                              }
                           }
                        }
                     }
                  }
               }

               CombatUtil.modifyPos = null;
               CombatUtil.modifyBlockState = null;
               if (this.antiSurround.getValue()
                  && PacketMine.getBreakPos() != null
                  && PacketMine.progress >= this.miningProgress.getValueFloat()
                  && !BlockUtil.hasEntity(PacketMine.getBreakPos(), false)
                  && this.tempDamage <= this.antiSurroundMax.getValueFloat()) {
                  for (PlayerEntityPredict papxx : list) {
                     BlockPos pos = new BlockPosX(papxx.player.getPos().add(0.0, 0.5, 0.0));
                     if (!BlockUtil.canCollide(papxx.player, new Box(pos))) {
                        for (Direction i : Direction.values()) {
                           if (i != Direction.DOWN && i != Direction.UP) {
                              BlockPos offsetPos = pos.offset(i);
                              if (offsetPos.equals(PacketMine.getBreakPos())) {
                                 for (Direction direction : Direction.values()) {
                                    if (direction != Direction.DOWN
                                       && direction != Direction.UP
                                       && this.canPlaceCrystal(offsetPos.offset(direction), false, false)) {
                                       float selfDamage = this.calculateDamage(offsetPos.offset(direction), self.player, self.predict);
                                       if (selfDamage < this.maxSelf.getValue()
                                          && (
                                             !(this.reserve.getValue() > 0.0)
                                                || !(selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - this.reserve.getValue())
                                          )) {
                                          this.tempPos = offsetPos.offset(direction);
                                          if (this.doCrystal.getValue() && this.tempPos != null && !shouldReturn) {
                                             this.doCrystal(this.tempPos);
                                          }

                                          return;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            if (this.doCrystal.getValue() && this.tempPos != null && !shouldReturn) {
               this.doCrystal(this.tempPos);
            }
         }
      }
   }

   @EventListener
   private void onEntity(EntitySpawnedEvent event) {
      if (this.onAdd.getValue() && event.getEntity() instanceof EndCrystalEntity crystal && crystal.getBlockPos().equals(this.syncPos)) {
         this.doBreak(crystal);
      }
   }

   public boolean canPlaceCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
      BlockPos obsPos = pos.down();
      BlockPos boost = obsPos.up();
      BlockPos boost2 = boost.up();
      return (BlockUtil.getBlock(obsPos) == Blocks.BEDROCK || BlockUtil.getBlock(obsPos) == Blocks.OBSIDIAN)
         && BlockUtil.getClickSideStrict(obsPos) != null
         && this.noEntityBlockCrystal(boost, ignoreCrystal, ignoreItem)
         && this.noEntityBlockCrystal(boost2, ignoreCrystal, ignoreItem)
         && (mc.world.isAir(boost) || BlockUtil.hasCrystal(boost) && BlockUtil.getBlock(boost) == Blocks.FIRE)
         && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(boost2));
   }

   private boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
      for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
         if (entity.isAlive()
            && (!(entity instanceof ItemEntity) || !ignoreItem)
            && (
               !(entity instanceof EndCrystalEntity)
                  || !ignoreCrystal
                  || this.getAttackVec(entity.getPos()) == null
                  || !mc.player.canSee(entity) && !(mc.player.getEyePos().distanceTo(entity.getPos()) <= this.wallRange.getValue())
            )) {
            return false;
         }
      }

      return true;
   }

   public boolean behindWall(BlockPos pos, Vec3d attackVec) {
      Vec3d crystalEyePos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.7, pos.getZ() + 0.5);
      HitResult result = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), crystalEyePos, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
      return result != null && result.getType() != Type.MISS ? mc.player.getEyePos().distanceTo(attackVec) > this.wallRange.getValue() : false;
   }

   private boolean canTouch(BlockPos pos) {
      Direction side = BlockUtil.getClickSideStrict(pos);
      return side != null
         && pos.toCenterPos()
               .add(new Vec3d(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5))
               .distanceTo(mc.player.getEyePos())
            <= this.placeRange.getValue();
   }

   private void doCrystal(BlockPos pos) {
      if (this.canPlaceCrystal(pos, false, false)) {
         this.doPlace(pos, this.rotate.getValue() && this.onPlace.getValue());
      }

      this.doBreak(pos);
   }

   private void doPlace(BlockPos pos) {
      if (this.baseTimer.passed((long)this.delay.getValue())) {
         if (!this.detectMining.getValue() || !Alien.BREAK.isMining(pos)) {
            int block = this.getBlock();
            if (block != -1) {
               int old = mc.player.getInventory().selectedSlot;
               this.baseSwap(block);
               BlockUtil.placeBlock(pos, this.rotate.getValue());
               if (this.inventory.getValue()) {
                  this.baseSwap(block);
                  EntityUtil.syncInventory();
               } else {
                  this.baseSwap(old);
               }

               this.baseTimer.reset();
            }
         }
      }
   }

   public float calculateDamage(BlockPos pos, PlayerEntity player, PlayerEntity predict) {
      return this.calculateDamage(new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), player, predict);
   }

   public float calculateDamage(Vec3d pos, PlayerEntity player, PlayerEntity predict) {
      if (this.ignoreMine.getValue()
         && PacketMine.getBreakPos() != null
         && mc.player.getEyePos().distanceTo(PacketMine.getBreakPos().toCenterPos()) <= PacketMine.INSTANCE.range.getValue()
         && PacketMine.progress >= this.constantProgress.getValue() / 100.0) {
         CombatUtil.modifyPos = PacketMine.getBreakPos();
         CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
      }

      if (this.terrainIgnore.getValue()) {
         CombatUtil.terrainIgnore = true;
      }

      float damage = ExplosionUtil.calculateDamage(pos, player, predict, 6.0F);
      CombatUtil.modifyPos = null;
      CombatUtil.terrainIgnore = false;
      return damage;
   }

   public float calculateBaseDamage(BlockPos pos, PlayerEntity player, PlayerEntity predict) {
      if (this.terrainIgnore.getValue()) {
         CombatUtil.terrainIgnore = true;
      }

      float damage = DamageUtils.overridingExplosionDamage(
         player,
         predict,
         new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5),
         12.0F,
         pos.down(),
         Blocks.OBSIDIAN.getDefaultState()
      );
      CombatUtil.terrainIgnore = false;
      return damage;
   }

   private double getDamage(PlayerEntity target) {
      if (!PacketMine.INSTANCE.obsidian.isPressed() && this.slowPlace.getValue() && this.lastBreakTimer.passed((long)this.slowDelay.getValue())) {
         return this.slowMinDamage.getValue();
      } else if (this.lethalOverride.getValue() && EntityUtil.getHealth(target) <= this.forceMaxHealth.getValue() && !PacketMine.INSTANCE.obsidian.isPressed()) {
         return this.forceMin.getValue();
      } else {
         if (this.armorBreaker.getValue()) {
            for (ItemStack armor : target.getInventory().armor) {
               if (!armor.isEmpty() && !(EntityUtil.getDamagePercent(armor) > this.maxDurable.getValue())) {
                  return this.armorBreakerDamage.getValue();
               }
            }
         }

         return this.minDamage.getValue();
      }
   }

   private boolean shouldYawStep() {
      return (this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())) && this.yawStep.getValue() && !Velocity.INSTANCE.noRotation();
   }

   public boolean hasCrystal() {
      return this.autoSwap.getValue() != SwapMode.None && this.getCrystal() != -1;
   }

   private void doBreak(EndCrystalEntity entity) {
      this.noPosTimer.reset();
      if (this.breakSetting.getValue()) {
         if (entity.isAlive()) {
            if (this.displayTarget == null
               || this.displayTarget.hurtTime <= this.waitHurt.getValueInt()
               || this.syncTimer.passedMs(this.syncTimeout.getValue())) {
               this.lastBreakTimer.reset();
               if (this.switchTimer.passed((long)this.switchCooldown.getValue())) {
                  this.syncTimer.reset();
                  if (entity.age >= this.minAge.getValueInt()) {
                     if (!this.shouldYawStep() && !CombatUtil.breakTimer.passed((long)this.breakDelay.getValue())) {
                        if (this.forcePlace.getValue() && this.crystalPos != null) {
                           this.doPlace(this.crystalPos, false);
                        }
                     } else {
                        if (this.rotate.getValue() && this.onBreak.getValue()) {
                           Vec3d attackVec = this.getAttackVec(entity.getPos());
                           if (!this.faceVector(attackVec == null ? entity.getPos() : attackVec)) {
                              if (this.forcePlace.getValue() && this.crystalPos != null) {
                                 this.doPlace(this.crystalPos, false);
                              }

                              return;
                           }
                        }

                        if (this.shouldYawStep() && !CombatUtil.breakTimer.passed((long)this.breakDelay.getValue())) {
                           if (this.forcePlace.getValue() && this.crystalPos != null) {
                              this.doPlace(this.crystalPos, false);
                           }
                        } else {
                           this.animation.to = 1.0;
                           this.animation.from = 1.0;
                           CombatUtil.breakTimer.reset();
                           this.syncPos = entity.getBlockPos();
                           mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                           if (this.resetCD.getValue()) {
                              mc.player.resetLastAttackedTicks();
                           }

                           EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)this.swingMode.getValue());
                           if (this.breakRemove.getValue()) {
                              mc.world.removeEntity(entity.getId(), RemovalReason.KILLED);
                           }

                           BlockPos crystalPos = this.crystalPos;
                           if (crystalPos != null
                              && this.displayTarget != null
                              && this.lastDamage >= this.getDamage(this.displayTarget)
                              && this.afterBreak.getValue()
                              && (
                                 !this.rotate.getValue()
                                    || !this.shouldYawStep()
                                    || !this.checkFov.getValue()
                                    || Alien.ROTATION.inFov(entity.getPos(), this.fov.getValueFloat())
                              )) {
                              this.doPlace(crystalPos, false);
                           }

                           if (this.forceWeb.getValue() && AutoWeb.INSTANCE.isOn()) {
                              AutoWeb.force = true;
                           }

                           if (this.rotate.getValue() && !this.shouldYawStep()) {
                              Alien.ROTATION.snapBack();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void doBreak(BlockPos pos) {
      this.noPosTimer.reset();
      if (this.breakSetting.getValue()) {
         if (this.displayTarget == null || this.displayTarget.hurtTime <= this.waitHurt.getValueInt() || this.syncTimer.passedMs(this.syncTimeout.getValue())
            )
          {
            this.lastBreakTimer.reset();
            if (this.switchTimer.passed((long)this.switchCooldown.getValue())) {
               this.syncTimer.reset();

               for (EndCrystalEntity entity : BlockUtil.getEndCrystals(
                  new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1)
               )) {
                  if (entity.age >= this.minAge.getValueInt() && entity.isAlive()) {
                     if (!this.shouldYawStep() && !CombatUtil.breakTimer.passed((long)this.breakDelay.getValue())) {
                        if (this.forcePlace.getValue() && this.crystalPos != null) {
                           this.doPlace(this.crystalPos, false);
                        }

                        return;
                     }

                     if (this.rotate.getValue() && this.onBreak.getValue()) {
                        Vec3d attackVec = this.getAttackVec(entity.getPos());
                        if (!this.faceVector(attackVec == null ? entity.getPos() : attackVec)) {
                           if (this.forcePlace.getValue() && this.crystalPos != null) {
                              this.doPlace(this.crystalPos, false);
                           }

                           return;
                        }
                     }

                     if (this.shouldYawStep() && !CombatUtil.breakTimer.passed((long)this.breakDelay.getValue())) {
                        if (this.forcePlace.getValue() && this.crystalPos != null) {
                           this.doPlace(this.crystalPos, false);
                        }

                        return;
                     }

                     this.animation.to = 1.0;
                     this.animation.from = 1.0;
                     CombatUtil.breakTimer.reset();
                     this.syncPos = pos;
                     mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                     if (this.resetCD.getValue()) {
                        mc.player.resetLastAttackedTicks();
                     }

                     EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)this.swingMode.getValue());
                     if (this.breakRemove.getValue()) {
                        mc.world.removeEntity(entity.getId(), RemovalReason.KILLED);
                     }

                     BlockPos crystalPos = this.crystalPos;
                     if (crystalPos != null
                        && this.displayTarget != null
                        && this.lastDamage >= this.getDamage(this.displayTarget)
                        && this.afterBreak.getValue()
                        && (
                           !this.rotate.getValue()
                              || !this.shouldYawStep()
                              || !this.checkFov.getValue()
                              || Alien.ROTATION.inFov(entity.getPos(), this.fov.getValueFloat())
                        )) {
                        this.doPlace(crystalPos, false);
                     }

                     if (this.forceWeb.getValue() && AutoWeb.INSTANCE.isOn()) {
                        AutoWeb.force = true;
                     }

                     if (this.rotate.getValue() && !this.shouldYawStep()) {
                        Alien.ROTATION.snapBack();
                     }

                     return;
                  }
               }

               if (this.forcePlace.getValue() && this.crystalPos != null) {
                  this.doPlace(this.crystalPos, false);
               }
            }
         }
      }
   }

   private void doPlace(BlockPos pos, boolean rotate) {
      this.noPosTimer.reset();
      if (this.place.getValue()) {
         if (mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL) || mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL) || this.hasCrystal()) {
            if (this.canTouch(pos.down())) {
               BlockPos obsPos = pos.down();
               Direction facing = BlockUtil.getClickSide(obsPos);
               Vec3d vec = obsPos.toCenterPos().add(facing.getVector().getX() * 0.5, facing.getVector().getY() * 0.5, facing.getVector().getZ() * 0.5);
               if (facing != Direction.UP && facing != Direction.DOWN) {
                  vec = vec.add(0.0, 0.45, 0.0);
               }

               if (this.shouldYawStep() || this.placeTimer.passed((long)this.placeDelay.getValue())) {
                  if (!rotate || this.faceVector(vec)) {
                     if (this.placeTimer.passed((long)this.placeDelay.getValue())) {
                        if (!mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL) && !mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL)) {
                           this.placeTimer.reset();
                           this.syncPos = pos;
                           int old = mc.player.getInventory().selectedSlot;
                           int crystal = this.getCrystal();
                           if (crystal == -1) {
                              return;
                           }

                           this.doSwap(crystal);
                           this.placeCrystal(pos);
                           if (this.autoSwap.getValue() == AutoCrystal.SwapMode.Silent) {
                              this.doSwap(old);
                           } else if (this.autoSwap.getValue() == AutoCrystal.SwapMode.Inventory) {
                              this.doSwap(crystal);
                              EntityUtil.syncInventory();
                           }
                        } else {
                           this.placeTimer.reset();
                           this.syncPos = pos;
                           this.placeCrystal(pos);
                        }

                        if (rotate && !this.shouldYawStep()) {
                           Alien.ROTATION.snapBack();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.autoSwap.getValue() == AutoCrystal.SwapMode.Silent || this.autoSwap.getValue() == AutoCrystal.SwapMode.Normal) {
         InventoryUtil.switchToSlot(slot);
      } else if (this.autoSwap.getValue() == AutoCrystal.SwapMode.Inventory) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      }
   }

   private void baseSwap(int slot) {
      if (!this.inventory.getValue()) {
         InventoryUtil.switchToSlot(slot);
      } else {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      }
   }

   private int getBlock() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
   }

   private int getCrystal() {
      if (this.autoSwap.getValue() == AutoCrystal.SwapMode.Silent || this.autoSwap.getValue() == AutoCrystal.SwapMode.Normal) {
         return InventoryUtil.findItem(Items.END_CRYSTAL);
      } else {
         return this.autoSwap.getValue() == AutoCrystal.SwapMode.Inventory ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL) : -1;
      }
   }

   private void placeCrystal(BlockPos pos) {
      boolean offhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
      BlockPos obsPos = pos.down();
      Direction facing = BlockUtil.getClickSide(obsPos);
      BlockUtil.clickBlock(obsPos, facing, false, offhand ? Hand.OFF_HAND : Hand.MAIN_HAND, (SwingSide)this.swingMode.getValue());
   }

   private boolean faceVector(Vec3d directionVec) {
      if (directionVec == null) {
         return false;
      } else if (!this.shouldYawStep()) {
         Alien.ROTATION.lookAt(directionVec);
         return true;
      } else {
         this.directionVec = directionVec;
         return Alien.ROTATION.inFov(directionVec, this.fov.getValueFloat()) ? true : !this.checkFov.getValue();
      }
   }

   private class CrystalRender {
      @EventListener
      public void onRender3D(Render3DEvent event) {
         BlockPos cpos = AutoCrystal.this.sync.getValue() && AutoCrystal.this.crystalPos != null ? AutoCrystal.this.syncPos : AutoCrystal.this.crystalPos;
         if (cpos != null) {
            AutoCrystal.this.placeVec3d = cpos.down().toCenterPos();
         }

         if (AutoCrystal.this.placeVec3d != null) {
            if (AutoCrystal.this.fadeSpeed.getValue() >= 1.0) {
               AutoCrystal.this.currentFade = AutoCrystal.this.noPosTimer.passed((long)(AutoCrystal.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5;
            } else {
               AutoCrystal.this.currentFade = AnimateUtil.animate(
                  AutoCrystal.this.currentFade,
                  AutoCrystal.this.noPosTimer.passed((long)(AutoCrystal.this.startFadeTime.getValue() * 1000.0)) ? 0.0 : 0.5,
                  AutoCrystal.this.fadeSpeed.getValue() / 10.0
               );
            }

            if (AutoCrystal.this.currentFade == 0.0) {
               AutoCrystal.this.curVec3d = null;
            } else {
               if (AutoCrystal.this.curVec3d != null && !(AutoCrystal.this.sliderSpeed.getValue() >= 1.0)) {
                  AutoCrystal.this.curVec3d = new Vec3d(
                     AnimateUtil.animate(AutoCrystal.this.curVec3d.x, AutoCrystal.this.placeVec3d.x, AutoCrystal.this.sliderSpeed.getValue() / 10.0),
                     AnimateUtil.animate(AutoCrystal.this.curVec3d.y, AutoCrystal.this.placeVec3d.y, AutoCrystal.this.sliderSpeed.getValue() / 10.0),
                     AnimateUtil.animate(AutoCrystal.this.curVec3d.z, AutoCrystal.this.placeVec3d.z, AutoCrystal.this.sliderSpeed.getValue() / 10.0)
                  );
               } else {
                  AutoCrystal.this.curVec3d = AutoCrystal.this.placeVec3d;
               }

               if (AutoCrystal.this.render.getValue()) {
                  Box cbox = new Box(AutoCrystal.this.curVec3d, AutoCrystal.this.curVec3d);
                  if (AutoCrystal.this.shrink.getValue()) {
                     cbox = cbox.expand(AutoCrystal.this.currentFade);
                  } else {
                     cbox = cbox.expand(0.5);
                  }

                  MatrixStack matrixStack = event.matrixStack;
                  if (AutoCrystal.this.fill.booleanValue) {
                     Render3DUtil.drawFill(
                        matrixStack,
                        cbox,
                        ColorUtil.injectAlpha(
                           AutoCrystal.this.fill.getValue(), (int)(AutoCrystal.this.fill.getValue().getAlpha() * AutoCrystal.this.currentFade * 2.0)
                        )
                     );
                  }

                  if (AutoCrystal.this.box.booleanValue) {
                     Render3DUtil.drawBox(
                        matrixStack,
                        cbox,
                        ColorUtil.injectAlpha(
                           AutoCrystal.this.box.getValue(), (int)(AutoCrystal.this.box.getValue().getAlpha() * AutoCrystal.this.currentFade * 2.0)
                        ),
                        AutoCrystal.this.lineWidth.getValueFloat()
                     );
                  }
               }

               if (AutoCrystal.this.text.booleanValue
                  && AutoCrystal.this.lastDamage > 0.0F
                  && !AutoCrystal.this.noPosTimer.passed((long)(AutoCrystal.this.startFadeTime.getValue() * 1000.0))) {
                  Render3DUtil.drawText3D(AutoCrystal.this.df.format(AutoCrystal.this.lastDamage), AutoCrystal.this.curVec3d, AutoCrystal.this.text.getValue());
               }
            }
         }
      }
   }

   private static enum Page {
      General,
      Base,
      Misc,
      Rotation,
      Check,
      Calc,
      Render;
   }

   private static enum SwapMode {
      None,
      Normal,
      Silent,
      Inventory;
   }

   public static enum TargetESP {
      Box,
      Fill,
      Jello,
      ThunderHack,
      None;
   }
}
