package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.client.TextRadar;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class NameTags extends Module {
   public static NameTags INSTANCE;
   final ColorSetting colorConfig = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
   final ColorSetting friendConfig = this.add(new ColorSetting("Friend", new Color(155, 155, 255)).injectBoolean(true));
   final ColorSetting invisibleConfig = this.add(new ColorSetting("Invisible", new Color(200, 200, 200)).injectBoolean(true));
   final ColorSetting died = this.add(new ColorSetting("Died", new Color(180, 0, 0)).injectBoolean(true));
   final ColorSetting sneakingConfig = this.add(new ColorSetting("Sneaking", new Color(200, 200, 0)).injectBoolean(true));
   final ColorSetting rectConfig = this.add(new ColorSetting("Rectangle", new Color(0, 0, 0, 100)).injectBoolean(true));
   final BooleanSetting armorConfig = this.add(new BooleanSetting("Armor", true).setParent());
   final BooleanSetting drawItemConfig = this.add(new BooleanSetting("DrawItem", true, this.armorConfig::isOpen));
   final SliderSetting offsetConfig = this.add(new SliderSetting("Offset", -20.0, -30.0, 10.0, 0.01, this.armorConfig::isOpen));
   final BooleanSetting enchantmentsConfig = this.add(new BooleanSetting("Enchantments", true));
   final BooleanSetting durabilityConfig = this.add(new BooleanSetting("Durability", true).setParent());
   final BooleanSetting forceBarConfig = this.add(new BooleanSetting("ForceBar", true, this.durabilityConfig::isOpen));
   final BooleanSetting itemNameConfig = this.add(new BooleanSetting("ItemName", false));
   final BooleanSetting entityIdConfig = this.add(new BooleanSetting("EntityId", false));
   final BooleanSetting gamemodeConfig = this.add(new BooleanSetting("Gamemode", false));
   final BooleanSetting pingConfig = this.add(new BooleanSetting("Ping", true));
   final BooleanSetting healthConfig = this.add(new BooleanSetting("Health", true));
   final BooleanSetting totemsConfig = this.add(new BooleanSetting("Totems", false));
   final SliderSetting scaleConfig = this.add(new SliderSetting("Scale", 1.0, 0.0, 3.0, 0.1));
   final BooleanSetting factorConfig = this.add(new BooleanSetting("Factor", true).setParent());
   final SliderSetting scalingConfig = this.add(new SliderSetting("Scaling", 1.0, 0.0, 3.0, 0.1, this.factorConfig::isOpen));
   final SliderSetting distanceConfig = this.add(new SliderSetting("Distance", 6.0, 0.0, 20.0, 0.1, this.factorConfig::isOpen));
   final SliderSetting heightConfig = this.add(new SliderSetting("Height", 0.0, -3.0, 3.0, 0.01));
   final DecimalFormat df = new DecimalFormat("0.0");

   public NameTags() {
      super("NameTags", "Renders info on player NameTags", Module.Category.Render);
      this.setChinese("名字标签");
      INSTANCE = this;
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      if (mc.gameRenderer != null && mc.getCameraEntity() != null) {
         Camera camera = mc.gameRenderer.getCamera();
         RenderSystem.enableBlend();
         GL11.glDepthFunc(519);
         MatrixStack matrixStack = new MatrixStack();

         for (PlayerEntity player : Alien.THREAD.getPlayers()) {
            if ((this.died.booleanValue || player.isAlive())
               && (player != mc.player || !mc.options.getPerspective().isFirstPerson())
               && (this.invisibleConfig.booleanValue || !player.isInvisible())) {
               String info = this.getNametagInfo(player);
               Vec3d renderPosition = MathUtil.getRenderPosition(player, event.tickDelta);
               double x = renderPosition.getX();
               double y = renderPosition.getY();
               double z = renderPosition.getZ();
               int width = mc.textRenderer.getWidth(info);
               float hwidth = width / 2.0F;
               this.renderInfo(info, hwidth, player, x, y, z, camera, matrixStack);
            }
         }

         GL11.glDepthFunc(515);
         RenderSystem.disableBlend();
      }
   }

   private void renderInfo(String info, float width, PlayerEntity entity, double x, double y, double z, Camera camera, MatrixStack matrices) {
      Vec3d pos = camera.getPos();
      double eyeY = y + entity.getHeight() + (entity.isSneaking() ? 0.4F : 0.43F) + this.heightConfig.getValueFloat();
      float scale = (float)(
         -0.025F * this.scaleConfig.getValueFloat()
            + (
               this.factorConfig.getValue() && pos.squaredDistanceTo(x, eyeY, z) > this.distanceConfig.getValueFloat() * this.distanceConfig.getValueFloat()
                  ? (Math.sqrt(pos.squaredDistanceTo(x, eyeY, z)) - this.distanceConfig.getValueFloat()) * -0.0025F * this.scalingConfig.getValueFloat()
                  : 0.0
            )
      );
      matrices.push();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrices.translate(x - pos.getX(), eyeY - pos.getY() + (scale / -0.025F - 1.0F) / 4.0F, z - pos.getZ());
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.scale(scale, scale, -1.0F);
      if (this.rectConfig.booleanValue) {
         Render2DUtil.drawRect(matrices, -width - 2.0F, -1.0F, width * 2.0F + 3.0F, 9.0F + 1.0F, this.rectConfig.getValue());
      }

      this.drawWithShadow(matrices, info, -width, 0.0F, this.getNametagColor(entity));
      if (this.armorConfig.getValue()) {
         this.renderItems(matrices, entity);
      }

      matrices.pop();
   }

   private void drawWithShadow(MatrixStack matrices, String info, float x, float y, int color) {
      Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
      mc.textRenderer.drawLayer(info, x, y, color, true, matrices.peek().getPositionMatrix(), immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
      immediate.draw();
      mc.textRenderer.draw(info, x, y, color, false, matrices.peek().getPositionMatrix(), immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
      immediate.draw();
   }

   private void renderItems(MatrixStack matrixStack, PlayerEntity player) {
      List<ItemStack> displayItems = new CopyOnWriteArrayList();
      if (!player.getOffHandStack().isEmpty()) {
         displayItems.add(player.getOffHandStack());
      }

      player.getInventory().armor.forEach(armorStack -> {
         if (!armorStack.isEmpty()) {
            displayItems.add(armorStack);
         }
      });
      if (!player.getMainHandStack().isEmpty()) {
         displayItems.add(player.getMainHandStack());
      }

      Collections.reverse(displayItems);
      float x = 0.0F;
      int n11 = 0;

      for (ItemStack stack : displayItems) {
         x -= 8.0F;
         if (stack.getEnchantments().getSize() > n11) {
            n11 = stack.getEnchantments().getSize();
         }
      }

      float y = this.offsetConfig.getValueFloat();

      for (ItemStack stackx : displayItems) {
         GL11.glDepthFunc(519);
         if (this.drawItemConfig.getValue()) {
            this.renderItemStack(matrixStack, stackx, x, y + 1.0F);
         }

         this.renderItemOverlay(matrixStack, stackx, x, y + 2.5F);
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         if (this.durabilityConfig.getValue()) {
            this.renderDurability(matrixStack, stackx, x + 2.0F, y + 11.5F);
         }

         if (this.enchantmentsConfig.getValue()) {
            this.renderEnchants(matrixStack, stackx, x + 2.0F, y + 7.0F);
         }

         matrixStack.scale(2.0F, 2.0F, 2.0F);
         x += 16.0F;
         GL11.glDepthFunc(515);
      }

      ItemStack heldItem = player.getMainHandStack();
      if (!heldItem.isEmpty()) {
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         if (this.itemNameConfig.getValue()) {
            this.renderItemName(matrixStack, heldItem, y - 4.5F + this.enchantOffset(n11));
         }

         matrixStack.scale(2.0F, 2.0F, 2.0F);
      }
   }

   private void renderItemStack(MatrixStack matrixStack, ItemStack stack, float x, float y) {
      matrixStack.push();
      matrixStack.translate(x, y, 0.0F);
      matrixStack.translate(8.0F, 8.0F, 0.0F);
      matrixStack.scale(16.0F, 16.0F, 1.0E-8F);
      matrixStack.multiplyPositionMatrix(new Matrix4f().scaling(1.0F, -1.0F, 1.0F));
      DiffuseLighting.disableGuiDepthLighting();
      BakedModel model = mc.getItemRenderer().getModel(stack, mc.world, null, 0);
      Immediate i = mc.getBufferBuilders().getEntityVertexConsumers();
      mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GUI, false, matrixStack, i, 16711680, OverlayTexture.DEFAULT_UV, model);
      i.draw();
      DiffuseLighting.enableGuiDepthLighting();
      matrixStack.pop();
   }

   private void renderItemOverlay(MatrixStack matrixStack, ItemStack stack, float x, float y) {
      matrixStack.push();
      if (stack.getCount() != 1) {
         String string = String.valueOf(stack.getCount());
         this.drawWithShadow(matrixStack, string, x + 17.0F - mc.textRenderer.getWidth(string), y + 9.0F, -1);
      }

      if (stack.isItemBarVisible() || stack.isDamageable() && this.forceBarConfig.getValue()) {
         int i = stack.getItemBarStep();
         int j = stack.getItemBarColor();
         float k = x + 2.0F;
         float l = y + 13.0F;
         Render2DUtil.drawRect(matrixStack, k, l, 13.0F, 2.0F, -16777216);
         Render2DUtil.drawRect(matrixStack, k, l, i, 1.0F, j | 0xFF000000);
      }

      matrixStack.pop();
   }

   private void renderDurability(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
      if (itemStack.isDamageable()) {
         int n = itemStack.getMaxDamage();
         int n2 = itemStack.getDamage();
         int durability = (int)((float)(n - n2) / n * 100.0F);
         this.drawWithShadow(
            matrixStack, durability + "%", x * 2.0F, y * 2.0F, ColorUtil.hslToColor((float)(n - n2) / n * 120.0F, 100.0F, 50.0F, 1.0F).getRGB()
         );
      }
   }

   private void renderEnchants(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
      if (itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
         this.drawWithShadow(matrixStack, "God", x * 2.0F, y * 2.0F, -3977663);
      } else if (itemStack.hasEnchantments()) {
         ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(itemStack);
         float n2 = 0.0F;

         for (RegistryEntry<Enchantment> enchantment : enchants.getEnchantments()) {
            int lvl = enchants.getLevel(enchantment);
            StringBuilder enchantString = new StringBuilder();
            String translatedName = ((Enchantment)enchantment.value()).toString().replace("Enchantment ", "");
            if (translatedName.contains("Vanish")) {
               enchantString.append("§cVan");
            } else if (translatedName.contains("Bind")) {
               enchantString.append("§cBind");
            } else {
               int maxLen = lvl > 1 ? 2 : 3;
               if (translatedName.length() > maxLen) {
                  translatedName = translatedName.substring(0, maxLen);
               }

               enchantString.append(translatedName);
               enchantString.append(lvl);
            }

            this.drawWithShadow(matrixStack, enchantString.toString(), x * 2.0F, (y + n2) * 2.0F, -1);
            n2 -= 4.5F;
         }
      }
   }

   private float enchantOffset(int n) {
      if (this.enchantmentsConfig.getValue() && n > 2) {
         float value = -2.0F;
         return value - (n - 3) * 4.5F;
      } else {
         return 0.0F;
      }
   }

   private void renderItemName(MatrixStack matrixStack, ItemStack itemStack, float y) {
      String itemName = itemStack.getName().getString();
      float width = mc.textRenderer.getWidth(itemName) / 4.0F;
      this.drawWithShadow(matrixStack, itemName, (0.0F - width) * 2.0F, y * 2.0F, -1);
   }

   private String getNametagInfo(PlayerEntity player) {
      StringBuilder info = new StringBuilder();
      if (this.gamemodeConfig.getValue()) {
         if (player.isCreative()) {
            info.append(Formatting.GOLD);
            info.append("[C] ");
         } else if (player.isSpectator()) {
            info.append(Formatting.GRAY);
            info.append("[I] ");
         } else {
            info.append(Formatting.BOLD);
            info.append("[S] ");
         }
      }

      if (this.pingConfig.getValue()) {
         info.append(this.getEntityPing(player));
         info.append("ms ");
         info.append(Formatting.RESET);
      }

      info.append(player.getName().getString());
      info.append(" ");
      if (this.entityIdConfig.getValue()) {
         info.append("ID: ");
         info.append(player.getId());
         info.append(" ");
      }

      if (this.healthConfig.getValue()) {
         double health = player.getHealth() + player.getAbsorptionAmount();
         Formatting hcolor;
         if (health > 18.0) {
            hcolor = Formatting.GREEN;
         } else if (health > 16.0) {
            hcolor = Formatting.DARK_GREEN;
         } else if (health > 12.0) {
            hcolor = Formatting.YELLOW;
         } else if (health > 8.0) {
            hcolor = Formatting.GOLD;
         } else if (health > 4.0) {
            hcolor = Formatting.RED;
         } else {
            hcolor = Formatting.DARK_RED;
         }

         String phealth = this.df.format(health);
         info.append(hcolor);
         info.append(phealth);
         info.append(" ");
      }

      if (this.totemsConfig.getValue()) {
         int totems = Alien.POP.getPop(player);
         if (totems > 0) {
            Formatting c = TextRadar.getPopColor(totems);
            info.append(c);
            info.append(-totems);
            info.append(" ");
         }
      }

      return info.toString().trim();
   }

   private String getEntityPing(PlayerEntity entity) {
      if (mc.getNetworkHandler() == null) {
         return "§7-1";
      } else {
         PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
         if (playerListEntry == null) {
            return "§7-1";
         } else {
            int ping = playerListEntry.getLatency();
            Formatting color;
            if (ping >= 200) {
               color = Formatting.RED;
            } else if (ping >= 100) {
               color = Formatting.YELLOW;
            } else {
               color = Formatting.GREEN;
            }

            return color.toString() + ping;
         }
      }
   }

   private int getNametagColor(PlayerEntity player) {
      if (this.friendConfig.booleanValue && player.getDisplayName() != null && Alien.FRIEND.isFriend(player)) {
         return this.friendConfig.getValue().getRGB();
      } else if (this.invisibleConfig.booleanValue && player.isInvisible()) {
         return this.invisibleConfig.getValue().getRGB();
      } else if (this.sneakingConfig.booleanValue && player.isSneaking()) {
         return this.sneakingConfig.getValue().getRGB();
      } else {
         return !player.isAlive() ? this.died.getValue().getRGB() : this.colorConfig.getValue().getRGB();
      }
   }
}
