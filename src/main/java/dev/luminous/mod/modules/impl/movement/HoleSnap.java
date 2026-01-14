package dev.luminous.mod.modules.impl.movement;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.events.impl.TimerEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.player.Freecam;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class HoleSnap extends Module {
   public static HoleSnap INSTANCE;
   public final BooleanSetting any = this.add(new BooleanSetting("AnyHole", true));
   public final SliderSetting timer = this.add(new SliderSetting("Timer", 1.0, 0.1, 8.0, 0.1));
   public final BooleanSetting up = this.add(new BooleanSetting("Up", true));
   public final BooleanSetting grim = this.add(new BooleanSetting("Grim", false));
   public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 100)));
   public final SliderSetting circleSize = this.add(new SliderSetting("CircleSize", 1.0, 0.1F, 2.5));
   public final BooleanSetting fade = this.add(new BooleanSetting("Fade", true));
   public final SliderSetting segments = this.add(new SliderSetting("Segments", 180, 0, 360));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5, 1, 50));
   private final SliderSetting timeoutTicks = this.add(new SliderSetting("TimeOut", 40, 0, 100));
   private final SliderSetting steps = this.add(new SliderSetting("Steps", 0.8, 0.0, 1.0, 0.01, this.grim::getValue));
   private final SliderSetting priority = this.add(new SliderSetting("Priority", 10, 0, 100, this.grim::getValue));
   boolean resetMove = false;
   boolean applyTimer = false;
   Vec3d targetPos;
   private BlockPos holePos;
   private int stuckTicks;
   private int enabledTicks;

   public HoleSnap() {
      super("HoleSnap", "HoleSnap", Module.Category.Movement);
      this.setChinese("拉坑");
      INSTANCE = this;
   }

   public static Vec2f getRotationTo(Vec3d posFrom, Vec3d posTo) {
      Vec3d vec3d = posTo.subtract(posFrom);
      return getRotationFromVec(vec3d);
   }

   public static void drawCircle(MatrixStack matrixStack, Color color, double circleSize, Vec3d pos, int segments) {
      Vec3d camPos = mc.getBlockEntityRenderDispatcher().camera.getPos();
      RenderSystem.disableDepthTest();
      Matrix4f matrix = matrixStack.peek().getPositionMatrix();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      float a = color.getAlpha() / 255.0F;
      float r = color.getRed() / 255.0F;
      float g = color.getGreen() / 255.0F;
      float b = color.getBlue() / 255.0F;
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      double i = 0.0;

      while (i < 360.0) {
         double x = Math.sin(Math.toRadians(i)) * circleSize;
         double z = Math.cos(Math.toRadians(i)) * circleSize;
         Vec3d tempPos = new Vec3d(pos.x + x, pos.y, pos.z + z).add(-camPos.x, -camPos.y, -camPos.z);
         bufferBuilder.vertex(matrix, (float)tempPos.x, (float)tempPos.y, (float)tempPos.z).color(r, g, b, a);
         i += 360.0 / segments;
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.enableDepthTest();
   }

   private static Vec2f getRotationFromVec(Vec3d vec) {
      double d = vec.x;
      double d2 = vec.z;
      double xz = Math.hypot(d, d2);
      d2 = vec.z;
      double d3 = vec.x;
      double yaw = normalizeAngle(Math.toDegrees(Math.atan2(d2, d3)) - 90.0);
      double pitch = normalizeAngle(Math.toDegrees(-Math.atan2(vec.y, xz)));
      return new Vec2f((float)yaw, (float)pitch);
   }

   private static double normalizeAngle(double angleIn) {
      double angle;
      if ((angle = angleIn % 360.0) >= 180.0) {
         angle -= 360.0;
      }

      if (angle < -180.0) {
         angle += 360.0;
      }

      return angle;
   }

   @EventListener(priority = -99)
   public void onTimer(TimerEvent event) {
      if (this.applyTimer) {
         event.set(this.timer.getValueFloat());
      }
   }

   @Override
   public void onEnable() {
      this.applyTimer = false;
      if (nullCheck()) {
         this.disable();
      } else {
         this.resetMove = false;
         this.holePos = Alien.HOLE.getHole((float)this.range.getValue(), true, this.any.getValue(), this.up.getValue());
      }
   }

   @Override
   public void onDisable() {
      this.holePos = null;
      this.stuckTicks = 0;
      this.enabledTicks = 0;
      if (!nullCheck()) {
         if (this.resetMove && !this.grim.getValue()) {
            MovementUtil.setMotionX(0.0);
            MovementUtil.setMotionZ(0.0);
         }
      }
   }

   @EventListener
   public void onReceivePacket(PacketEvent.Receive event) {
      if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.disable();
      }
   }

   @EventListener(priority = -999)
   public void onKeyInput(KeyboardInputEvent e) {
      if (this.grim.getValue()) {
         if (!AntiCheat.INSTANCE.movementSync()) {
            this.sendMessage("§4HoleSnap require MovementSync.");
            this.disable();
         } else if (!mc.player.isRiding() && !Freecam.INSTANCE.isOn()) {
            mc.player.input.movementSideways = 0.0F;
            mc.player.input.movementForward = 1.0F;
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.holePos = Alien.HOLE.getHole((float)this.range.getValue(), true, this.any.getValue(), this.up.getValue());
      if (this.holePos == null) {
         this.disable();
      } else {
         this.enabledTicks++;
         if (this.enabledTicks > this.timeoutTicks.getValue() - 1.0) {
            this.disable();
         } else {
            this.applyTimer = true;
            if (this.grim.getValue()) {
               if (!mc.player.isAlive() || mc.player.isFallFlying()) {
                  this.disable();
               } else if (this.stuckTicks > 8) {
                  this.disable();
               } else if (this.holePos == null) {
                  this.disable();
               } else {
                  Vec3d playerPos = mc.player.getPos();
                  this.targetPos = new Vec3d(this.holePos.getX() + 0.5, mc.player.getY(), this.holePos.getZ() + 0.5);
                  if (Alien.HOLE.isDoubleHole(this.holePos)) {
                     Direction facing = Alien.HOLE.is3Block(this.holePos);
                     if (facing != null) {
                        this.targetPos = this.targetPos
                           .add(new Vec3d(facing.getVector().getX() * 0.5, facing.getVector().getY() * 0.5, facing.getVector().getZ() * 0.5));
                     }
                  }

                  this.applyTimer = true;
                  this.resetMove = true;
                  float rotation = getRotationTo(playerPos, this.targetPos).x;
                  float yawRad = rotation / 180.0F * (float) Math.PI;
                  double dist = playerPos.distanceTo(this.targetPos);
                  double cappedSpeed = Math.min(0.2873, dist);
                  double x = -((float)Math.sin(yawRad)) * cappedSpeed;
                  double z = (float)Math.cos(yawRad) * cappedSpeed;
                  if (Math.abs(x) < 0.25 && Math.abs(z) < 0.25 && playerPos.y <= this.holePos.getY() + 0.8) {
                     this.disable();
                  } else {
                     if (mc.player.horizontalCollision) {
                        this.stuckTicks++;
                     } else {
                        this.stuckTicks = 0;
                     }
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.grim.getValue() && this.holePos != null) {
         this.targetPos = new Vec3d(this.holePos.getX() + 0.5, mc.player.getY(), this.holePos.getZ() + 0.5);
         if (Alien.HOLE.isDoubleHole(this.holePos)) {
            Direction facing = Alien.HOLE.is3Block(this.holePos);
            if (facing != null) {
               this.targetPos = this.targetPos
                  .add(new Vec3d(facing.getVector().getX() * 0.5, facing.getVector().getY() * 0.5, facing.getVector().getZ() * 0.5));
            }
         }

         event.setTarget(this.targetPos, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @EventListener
   public void onMove(MoveEvent event) {
      if (!this.grim.getValue()) {
         if (!mc.player.isAlive() || mc.player.isFallFlying()) {
            this.disable();
         } else if (this.stuckTicks > 8) {
            this.disable();
         } else if (this.holePos == null) {
            this.disable();
         } else {
            Vec3d playerPos = mc.player.getPos();
            this.targetPos = new Vec3d(this.holePos.getX() + 0.5, mc.player.getY(), this.holePos.getZ() + 0.5);
            if (Alien.HOLE.isDoubleHole(this.holePos)) {
               Direction facing = Alien.HOLE.is3Block(this.holePos);
               if (facing != null) {
                  this.targetPos = this.targetPos
                     .add(new Vec3d(facing.getVector().getX() * 0.5, facing.getVector().getY() * 0.5, facing.getVector().getZ() * 0.5));
               }
            }

            this.applyTimer = true;
            this.resetMove = true;
            float rotation = getRotationTo(playerPos, this.targetPos).x;
            float yawRad = rotation / 180.0F * (float) Math.PI;
            double dist = playerPos.distanceTo(this.targetPos);
            double cappedSpeed = Math.min(0.2873, dist);
            double x = -((float)Math.sin(yawRad)) * cappedSpeed;
            double z = (float)Math.cos(yawRad) * cappedSpeed;
            event.setX(x);
            event.setZ(z);
            if (Math.abs(x) < 0.1 && Math.abs(z) < 0.1 && playerPos.y <= this.holePos.getY() + 0.5) {
               this.disable();
            }

            if (mc.player.horizontalCollision) {
               this.stuckTicks++;
            } else {
               this.stuckTicks = 0;
            }
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (this.targetPos != null && this.holePos != null) {
         GL11.glEnable(3042);
         Color color = this.color.getValue();
         Vec3d pos = new Vec3d(this.targetPos.x, this.holePos.getY(), this.targetPos.getZ());
         if (this.fade.getValue()) {
            double temp = 0.01;

            for (double i = 0.0; i < this.circleSize.getValue(); i += temp) {
               drawCircle(
                  matrixStack,
                  ColorUtil.injectAlpha(color, (int)Math.min(color.getAlpha() * 2 / (this.circleSize.getValue() / temp), 255.0)),
                  i,
                  pos,
                  this.segments.getValueInt()
               );
            }
         } else {
            drawCircle(matrixStack, color, this.circleSize.getValue(), pos, this.segments.getValueInt());
         }

         GL11.glDisable(3042);
      }
   }
}
