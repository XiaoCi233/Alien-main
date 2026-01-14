package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Punctuation extends Module {
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));
   private final SliderSetting clearTime = this.add(new SliderSetting("ClearTime", 10.0, 0.0, 100.0, 0.1).setSuffix("s"));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 100)));
   private final BindSetting enemySpot = this.add(new BindSetting("EnemySpot", -1));
   private final StringSetting key = this.add(new StringSetting("EncryptKey", "IDKWTFTHIS"));
   private final ConcurrentHashMap<String, Punctuation.Spot> waypoint = new ConcurrentHashMap();
   private boolean pressed = false;

   public Punctuation() {
      super("Punctuation", Module.Category.Misc);
      this.setChinese("标点");
   }

   public static SecretKeySpec getKey(String myKey) {
      try {
         byte[] key = myKey.getBytes(StandardCharsets.UTF_8);
         MessageDigest sha = MessageDigest.getInstance("SHA-256");
         key = sha.digest(key);
         key = Arrays.copyOf(key, 16);
         return new SecretKeySpec(key, "AES");
      } catch (Exception var3) {
         return null;
      }
   }

   @Override
   public void onDisable() {
      this.waypoint.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.waypoint.values().removeIf(t -> t.timer.passedS(this.clearTime.getValue()));
      if (this.enemySpot.isPressed() && mc.currentScreen == null) {
         if (!this.pressed && mc.getCameraEntity().raycast(256.0, 0.0F, false) instanceof BlockHitResult blockHitResult) {
            BlockPos pos = blockHitResult.getBlockPos();
            mc.player
               .networkHandler
               .sendChatMessage(
                  this.Encrypt(
                     "EnemyHere{" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + this.color.getValue().getRGB() + "}"
                  )
               );
         }

         this.pressed = true;
      } else {
         this.pressed = false;
      }
   }

   @Override
   public void onRender2D(DrawContext context, float tickDelta) {
      for (Punctuation.Spot spot : this.waypoint.values()) {
         Vec3d vector = TextUtil.worldSpaceToScreenSpace(spot.pos.toCenterPos().add(0.0, 1.0, 0.0));
         String text = "§a" + spot.name + " §f(" + spot.pos.getX() + ", " + spot.pos.getY() + ", " + spot.pos.getZ() + ")";
         if (vector.z > 0.0 && vector.z < 1.0) {
            double posX = vector.x;
            double posY = vector.y;
            double endPosX = Math.max(vector.x, vector.z);
            float diff = (float)(endPosX - posX) / 2.0F;
            float textWidth = mc.textRenderer.getWidth(text);
            float tagX = (float)((posX + diff - textWidth / 4.0F) * 1.0);
            context.getMatrices().push();
            context.getMatrices().scale(0.5F, 0.5F, 1.0F);
            context.drawText(mc.textRenderer, text, (int)tagX * 2, (int)(posY - 11.0 + 9.0 * 1.2) * 2, -1, true);
            context.getMatrices().pop();
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      for (Punctuation.Spot spot : this.waypoint.values()) {
         Render3DUtil.drawFill(
            matrixStack,
            new Box(
               spot.pos.getX() + 0.25, -60.0, spot.pos.getZ() + 0.25, spot.pos.getX() + 0.75, 320.0, spot.pos.getZ() + 0.75
            ),
            spot.color
         );
      }
   }

   @EventListener
   private void PacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (event.getPacket() instanceof GameMessageS2CPacket packet && packet.content() != null) {
            mc.execute(() -> this.receive(packet.content().getString()));
         }

         if (event.getPacket() instanceof ChatMessageS2CPacket packet) {
            if (packet.unsignedContent() != null) {
               mc.execute(() -> this.receive(packet.unsignedContent().getString()));
            } else {
               mc.execute(() -> this.receive(packet.body().content()));
            }
         }
      }
   }

   private void receive(String s) {
      try {
         if (s == null) {
            return;
         }

         String decrypt = this.Decrypt(s.replaceAll("§[a-zA-Z0-9]", "").replaceAll("<[^>]*> ", ""));
         if (decrypt == null) {
            return;
         }

         if (decrypt.contains("EnemyHere")) {
            Pattern pattern = Pattern.compile("\\{(.*?)}");
            Matcher matcher = pattern.matcher(decrypt);
            if (matcher.find()) {
               String pos = matcher.group(1);
               String[] posSplit = pos.split(",");
               if (posSplit.length == 3) {
                  if (this.sound.getValue()) {
                     mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
                  }

                  String xString = posSplit[0];
                  String yString = posSplit[1];
                  String zString = posSplit[2];
                  pattern = Pattern.compile("<(.*?)>");
                  matcher = pattern.matcher(s);
                  if (!this.isNumeric(xString)) {
                     return;
                  }

                  double x = Double.parseDouble(xString);
                  if (!this.isNumeric(yString)) {
                     return;
                  }

                  double y = Double.parseDouble(yString);
                  if (!this.isNumeric(zString)) {
                     return;
                  }

                  double z = Double.parseDouble(zString);
                  if (matcher.find()) {
                     String sender = matcher.group(1);
                     this.waypoint.put(sender, new Punctuation.Spot(sender, new BlockPosX(x, y, z), this.color.getValue(), new Timer()));
                     CommandManager.sendMessage(sender + " marked at §r(" + xString + ", " + yString + ", " + zString + ")");
                  } else {
                     this.waypoint
                        .put(MathUtil.random(0.0F, 1.0E9F) + "", new Punctuation.Spot("Unknown", new BlockPosX(x, y, z), this.color.getValue(), new Timer()));
                     CommandManager.sendMessage("Unknown marked at §r(" + xString + ", " + yString + ", " + zString + ")");
                  }
               } else if (posSplit.length == 4) {
                  if (this.sound.getValue()) {
                     mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
                  }

                  String xStringx = posSplit[0];
                  String yStringx = posSplit[1];
                  String zStringx = posSplit[2];
                  String colorString = posSplit[3];
                  pattern = Pattern.compile("<(.*?)>");
                  matcher = pattern.matcher(s);
                  if (!this.isNumeric(xStringx)) {
                     return;
                  }

                  double xx = Double.parseDouble(xStringx);
                  if (!this.isNumeric(yStringx)) {
                     return;
                  }

                  double yx = Double.parseDouble(yStringx);
                  if (!this.isNumeric(zStringx)) {
                     return;
                  }

                  double z = Double.parseDouble(zStringx);
                  if (!this.isNumeric(colorString)) {
                     return;
                  }

                  double color = Double.parseDouble(colorString);
                  if (matcher.find()) {
                     String sender = matcher.group(1);
                     this.waypoint.put(sender, new Punctuation.Spot(sender, new BlockPosX(xx, yx, z), new Color((int)color, true), new Timer()));
                     CommandManager.sendMessage(sender + " marked at §r(" + xStringx + ", " + yStringx + ", " + zStringx + ")");
                  } else {
                     this.waypoint
                        .put(
                           MathUtil.random(0.0F, 1.0E9F) + "",
                           new Punctuation.Spot("Unknown", new BlockPosX(xx, yx, z), new Color((int)color, true), new Timer())
                        );
                     CommandManager.sendMessage("Unknown marked at §r(" + xStringx + ", " + yStringx + ", " + zStringx + ")");
                  }
               }
            }
         }
      } catch (Exception var20) {
         var20.printStackTrace();
      }
   }

   private boolean isNumeric(String str) {
      return str.matches("-?\\d+(\\.\\d+)?");
   }

   public String Decrypt(String strToDecrypt) {
      try {
         Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         SecretKeySpec secretKey = getKey(this.key.getValue());
         byte[] iv = new byte[16];
         IvParameterSpec ivParams = new IvParameterSpec(iv);
         cipher.init(2, secretKey, ivParams);
         byte[] original = cipher.doFinal(Base64.getDecoder().decode(strToDecrypt));
         return new String(original, StandardCharsets.UTF_8);
      } catch (Exception var7) {
         return null;
      }
   }

   public String Encrypt(String strToEncrypt) {
      try {
         Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         SecretKeySpec secretKey = getKey(this.key.getValue());
         byte[] iv = new byte[16];
         IvParameterSpec ivParams = new IvParameterSpec(iv);
         cipher.init(1, secretKey, ivParams);
         return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
      } catch (Exception var6) {
         return null;
      }
   }

   private record Spot(String name, BlockPos pos, Color color, Timer timer) {
   }
}
