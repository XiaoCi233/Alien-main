package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.ShaderManager;
import dev.luminous.core.impl.ShaderManager.Shader;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {
   @Shadow
   public int width;
   @Shadow
   public int height;
   @Shadow
   protected MinecraftClient client;

   @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
   public void renderInGameBackgroundHook(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ci.cancel();
      if (this.client.world == null) {
         boolean isMainMenu = (Screen)((Object)this) instanceof TitleScreen;
         boolean isLoading = (Screen)((Object)this) instanceof ProgressScreen || (Screen)((Object)this) instanceof DownloadingTerrainScreen;
         if (isMainMenu || isLoading) {
            float w = this.width;
            float h = this.height;
            MatrixStack m = context.getMatrices();
            Render2DUtil.verticalGradient(m, 0.0F, 0.0F, w, h, new Color(40, 10, 60, 255), new Color(200, 20, 150, 255));
            long now = System.currentTimeMillis();
            float phase = (float)(now % 6000L) / 6000.0F;
            float angle = 0.523599F;
            float dx = (float)Math.tan(angle) * h;
            float base = -h;
            float spacing = 36.0F;
            float shift = phase * spacing * 4.0F;
            Color c1 = new Color(220, 60, 170, 52);
            Color c2 = new Color(160, 40, 130, 36);

            for (float i = base; i < w; i += spacing) {
               float x0 = i + shift;
               Render2DUtil.drawLine(m, x0, 0.0F, x0 + dx, h, c1.getRGB());
            }

            for (float i = base + spacing / 2.0F; i < w; i += spacing) {
               float x0 = i + shift * 0.8F;
               Render2DUtil.drawLine(m, x0, 0.0F, x0 + dx, h, c2.getRGB());
            }

            float hSpacing = 64.0F;
            Color c3 = new Color(255, 255, 255, 18);

            for (float y = 0.0F; y <= h; y += hSpacing) {
               Render2DUtil.drawLine(m, 0.0F, y, w, y, c3.getRGB());
            }

            if (!Alien.SHADER.fullNullCheck()) {
               ManagedShaderEffect gradient = Alien.SHADER.getShader(ShaderManager.Shader.Gradient);
               gradient.setUniformValue("alpha2", 0.2F);
               gradient.setUniformValue("rgb", 0.78F, 0.05F, 0.59F);
               gradient.setUniformValue("rgb1", 0.56F, 0.06F, 0.68F);
               gradient.setUniformValue("rgb2", 0.93F, 0.12F, 0.63F);
               gradient.setUniformValue("rgb3", 0.64F, 0.0F, 0.64F);
               gradient.setUniformValue("step", 180.0F);
               gradient.setUniformValue("radius", 2.0F);
               gradient.setUniformValue("quality", 1.0F);
               gradient.setUniformValue("divider", 150.0F);
               gradient.setUniformValue("maxSample", 10.0F);
               gradient.setUniformValue("resolution", w, h);
               float t = (float)(now % 100000L) / 1000.0F;
               gradient.setUniformValue("time", t * 300.0F);
               gradient.render(this.client.getRenderTickCounter().getTickDelta(true));
            }

            return;
         }

         this.renderPanoramaBackground(context, delta);
      }

      if (ClientSetting.INSTANCE.darkening.getValue() && !((Screen)((Object)this) instanceof TitleScreen)) {
         this.renderDarkening(context);
      }

      if (this.client.world != null && ClickGui.getInstance().tint.booleanValue) {
         context.fillGradient(
            0, 0, this.width, this.height, ClickGui.getInstance().tint.getValue().getRGB(), ClickGui.getInstance().endColor.getValue().getRGB()
         );
      }
   }

   @Shadow
   protected void renderPanoramaBackground(DrawContext context, float delta) {
   }

   @Shadow
   protected void renderDarkening(DrawContext context) {
   }

   @Shadow
   public void close() {
   }

   @Shadow
   public ScreenRect getNavigationFocus() {
      return null;
   }

   @Shadow
   protected <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement) {
      return null;
   }
}
