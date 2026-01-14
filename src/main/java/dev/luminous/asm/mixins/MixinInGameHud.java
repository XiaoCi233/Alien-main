package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.PreRender2DEvent;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.impl.render.Crosshair;
import dev.luminous.mod.modules.impl.render.NoRender;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {
   @Final
   @Shadow
   private MinecraftClient client;
   @Unique
   final Color minColor = new Color(196, 0, 0);
   @Unique
   final Color maxColor = new Color(0, 227, 0);

   @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
   private void onRenderPortalOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.portal.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
   private void onRenderStatusEffectOverlay(CallbackInfo info) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.potionsIcon.getValue()) {
         info.cancel();
      }
   }

   @Inject(method = "renderMainHud", at = @At("TAIL"))
   private void onRenderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (HUD.INSTANCE.isOn() && HUD.INSTANCE.armor.getValue()) {
         PlayerEntity player = this.getCameraPlayer();
         if (player != null) {
            int x = context.getScaledWindowWidth() / 2 + 91;
            int y = context.getScaledWindowHeight() - 28 - HUD.INSTANCE.armorOffset.getValueInt();
            if (this.client.interactionManager.hasStatusBars()) {
               y -= 16;
            }

            int t = this.getHeartCount(this.getRiddenEntity());
            if (t == 0) {
               y -= 10;
            }

            int maxAir = player.getMaxAir();
            int air = Math.min(player.getAir(), maxAir);
            if (player.isSubmergedIn(FluidTags.WATER) || air < maxAir) {
               int w = this.getHeartRows(t) - 1;
               y += w * 10;
            }

            for (ItemStack armor : this.client.player.getInventory().armor) {
               x -= 20;
               if (!armor.isEmpty()) {
                  context.getMatrices().push();
                  int damage = EntityUtil.getDamagePercent(armor);
                  context.drawItem(armor, x, y);
                  context.drawItemInSlot(this.client.textRenderer, armor, x, y);
                  if (HUD.INSTANCE.durability.getValue()) {
                     if (HUD.INSTANCE.font.getValue()) {
                        FontManager.small
                           .drawString(
                              context.getMatrices(),
                              damage + "%",
                              x + 1,
                              y - FontManager.small.getFontHeight() / 2.0F,
                              ColorUtil.fadeColor(this.minColor, this.maxColor, damage / 100.0F).getRGB(),
                              HUD.INSTANCE.shadow.getValue()
                           );
                     } else {
                        TextUtil.drawStringScale(
                           context,
                           damage + "%",
                           x + 2,
                           y - 9.0F / 4.0F,
                           ColorUtil.fadeColor(this.minColor, this.maxColor, damage / 100.0F).getRGB(),
                           0.5F,
                           HUD.INSTANCE.shadow.getValue()
                        );
                     }
                  }

                  context.getMatrices().pop();
               }
            }
         }
      }
   }

   @Shadow
   private int getHeartRows(int t) {
      return 0;
   }

   @Shadow
   private int getHeartCount(LivingEntity livingEntity) {
      return 0;
   }

   @Shadow
   private PlayerEntity getCameraPlayer() {
      return null;
   }

   @Shadow
   private LivingEntity getRiddenEntity() {
      return null;
   }

   @Shadow
   public abstract void render(DrawContext var1, RenderTickCounter var2);

   @Inject(at = @At("HEAD"), method = "render")
   public void renderStart(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Alien.EVENT_BUS.post(PreRender2DEvent.get(context));
   }

   @Inject(at = @At("TAIL"), method = "render")
   public void renderHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Alien.MODULE.onRender2D(context);
   }

   @Inject(method = "clear", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;clear(Z)V"), cancellable = true)
   private void onClear(CallbackInfo info) {
      if (ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.keepHistory.getValue()) {
         info.cancel();
      }
   }

   @ModifyArg(
      method = "renderHotbar",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V", ordinal = 1),
      index = 1
   )
   private int selectedSlotX(int x) {
      if (ClientSetting.INSTANCE.hotbar()) {
         double hotbarX = ClientSetting.animation.get(x, ClientSetting.INSTANCE.hotbarTime.getValueInt(), (Easing)ClientSetting.INSTANCE.animEase.getValue());
         return (int)hotbarX;
      } else {
         return x;
      }
   }

   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
   private void onRenderCrosshairBegin(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (Crosshair.INSTANCE.isOn()) {
         Crosshair.INSTANCE.draw(context);
         ci.cancel();
      }
   }
}
