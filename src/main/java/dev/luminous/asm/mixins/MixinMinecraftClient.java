package dev.luminous.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.Alien;
import dev.luminous.api.events.Event;
import dev.luminous.api.events.Event;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.DoAttackEvent;
import dev.luminous.api.events.impl.GameLeftEvent;
import dev.luminous.api.events.impl.OpenScreenEvent;
import dev.luminous.api.events.impl.ResizeEvent;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.core.impl.FontManager;
import dev.luminous.core.impl.ShaderManager;
import dev.luminous.core.impl.ShaderManager.Shader;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.player.InteractTweaks;
import java.awt.Color;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient extends ReentrantThreadExecutor<Runnable> {
   @Shadow
   @Final
   public InGameHud inGameHud;
   @Shadow
   public int attackCooldown;
   @Shadow
   public ClientPlayerEntity player;
   @Shadow
   public HitResult crosshairTarget;
   @Shadow
   public ClientPlayerInteractionManager interactionManager;
   @Final
   @Shadow
   public ParticleManager particleManager;
   @Shadow
   public ClientWorld world;
   @Shadow
   private IntegratedServer server;
   @Shadow
   public Screen currentScreen;
   @Shadow
   @Final
   private Window window;
   @Unique
   private static long alienStartTs = 0L;

   public MixinMinecraftClient(String string) {
      super(string);
   }

   @Inject(method = "onResolutionChanged", at = @At("TAIL"))
   private void captureResize(CallbackInfo ci) {
      Alien.EVENT_BUS.post(new ResizeEvent(this.window));
   }

   @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;limitDisplayFPS(I)V"), require = 0)
   public void fpsHook(int fps) {
      if (!ClientSetting.INSTANCE.fuckFPSLimit.getValue()) {
         RenderSystem.limitDisplayFPS(fps);
      }
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   void postWindowInit(RunArgs args, CallbackInfo ci) {
      FontManager.init();
      alienStartTs = System.currentTimeMillis();
   }

   @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
   private void onSetScreen(Screen screen, CallbackInfo info) {
      OpenScreenEvent event = OpenScreenEvent.get(screen);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         info.cancel();
      }
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void vitalityMagentaOverlay(CallbackInfo ci) {
      Screen screen = this.currentScreen;
      if (screen != null) {
         boolean isMainMenu = screen instanceof TitleScreen;
         boolean isLoading = screen instanceof ProgressScreen || screen instanceof DownloadingTerrainScreen || screen instanceof ConnectScreen;
         if (isMainMenu || isLoading) {
            if (!Alien.SHADER.fullNullCheck()) {
               float w = this.window.getScaledWidth();
               float h = this.window.getScaledHeight();
               float t = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;
               ManagedShaderEffect gradient = Alien.SHADER.getShader(ShaderManager.Shader.Gradient);
               gradient.setUniformValue("alpha2", 0.85F);
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
               gradient.setUniformValue("time", t * 300.0F);
               gradient.render(((MinecraftClient)((Object)this)).getRenderTickCounter().getTickDelta(true));
               ManagedShaderEffect pulse = Alien.SHADER.getShader(ShaderManager.Shader.Pulse);
               pulse.setUniformValue("mixFactor", 0.65F);
               pulse.setUniformValue("minAlpha", 0.35F);
               pulse.setUniformValue("radius", 2.0F);
               pulse.setUniformValue("quality", 1.0F);
               pulse.setUniformValue("divider", 150.0F);
               pulse.setUniformValue("maxSample", 10.0F);
               pulse.setUniformValue("color", 0.85F, 0.05F, 0.66F);
               pulse.setUniformValue("color2", 0.56F, 0.06F, 0.68F);
               pulse.setUniformValue("time", t);
               pulse.setUniformValue("size", 12.0F);
               pulse.setUniformValue("resolution", w, h);
               pulse.render(((MinecraftClient)((Object)this)).getRenderTickCounter().getTickDelta(true));
               MatrixStack m = new MatrixStack();
               w = this.window.getScaledWidth();
               h = this.window.getScaledHeight();
               long now = System.currentTimeMillis();
               long elapsed = now - alienStartTs;
               if (elapsed < 2400L) {
                  float p = Math.max(0.0F, Math.min(1.0F, (float)elapsed / 2400.0F));
                  int aTop = (int)(180.0 * Math.sin(p * Math.PI));
                  Color c1 = new Color(28, 60, 110, aTop);
                  Color c2 = new Color(190, 50, 160, aTop);
                  Render2DUtil.verticalGradient(m, 0.0F, 0.0F, w, h, c1, c2);
                  float r = Math.min(w, h) * (0.12F + 0.18F * p);
                  Render2DUtil.drawCircle(m, w / 2.0F, h / 2.0F, r, new Color(255, 255, 255, (int)(70.0F * p)), 80);
                  Render2DUtil.drawCircle(m, w / 2.0F, h / 2.0F, r * 1.2F, new Color(120, 220, 255, (int)(50.0F * p)), 80);
               } else {
                  float phase = (float)(now % 6000L) / 6000.0F;
                  float angle = 0.523599F;
                  float dx = (float)Math.tan(angle) * h;
                  float base = -h;
                  float spacing = 28.0F;
                  float shift = phase * spacing * 5.2F;
                  int cA1 = new Color(230, 60, 170, 64).getRGB();
                  int cA2 = new Color(160, 40, 130, 48).getRGB();
                  int nodeC = new Color(255, 255, 255, 42).getRGB();

                  for (float i = base; i < w; i += spacing) {
                     float x0 = i + shift;
                     Render2DUtil.drawLine(m, x0, 0.0F, x0 + dx, h, cA1);
                     float nx = x0 + dx * 0.25F;
                     float ny = h * 0.25F;
                     Render2DUtil.drawCircle(m, nx, ny, 2.5F, new Color(nodeC, true), 32);
                  }

                  for (float i = base + spacing / 2.0F; i < w; i += spacing) {
                     float x0 = i + shift * 0.85F;
                     Render2DUtil.drawLine(m, x0, 0.0F, x0 + dx, h, cA2);
                     float nx = x0 + dx * 0.65F;
                     float ny = h * 0.6F;
                     Render2DUtil.drawCircle(m, nx, ny, 2.0F, new Color(nodeC, true), 28);
                  }
               }
            }
         }
      }
   }

   @Inject(
      method = "doAttack",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/hit/HitResult;getType()Lnet/minecraft/util/hit/HitResult$Type;", shift = Shift.BEFORE)
   )
   public void onAttack(CallbackInfoReturnable<Boolean> cir) {
      Alien.EVENT_BUS.post(DoAttackEvent.getPre());
   }

   @Inject(
      method = "doAttack",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V", shift = Shift.AFTER)
   )
   public void onAttackPost(CallbackInfoReturnable<Boolean> cir) {
      Alien.EVENT_BUS.post(DoAttackEvent.getPost());
   }

   @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
   private void onDisconnect(Screen screen, CallbackInfo info) {
      if (this.world != null) {
         Alien.EVENT_BUS.post(GameLeftEvent.INSTANCE);
      }
   }

   @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
   private void clearTitleMixin(Screen screen, CallbackInfo info) {
      if (ClientSetting.INSTANCE.titleFix.getValue()) {
         this.inGameHud.clearTitle();
         this.inGameHud.setDefaultTitleFade();
      }
   }

   @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
   private void handleBlockBreaking(boolean breaking, CallbackInfo ci) {
      if (this.attackCooldown <= 0 && this.player.isUsingItem() && InteractTweaks.INSTANCE.multiTask()) {
         if (breaking && this.crosshairTarget != null && this.crosshairTarget.getType() == Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult)this.crosshairTarget;
            BlockPos blockPos = blockHitResult.getBlockPos();
            if (!this.world.getBlockState(blockPos).isAir()) {
               Direction direction = blockHitResult.getSide();
               if (this.interactionManager.updateBlockBreakingProgress(blockPos, direction)) {
                  this.particleManager.addBlockBreakingParticles(blockPos, direction);
                  this.player.swingHand(Hand.MAIN_HAND);
               }
            }
         } else {
            this.interactionManager.cancelBlockBreaking();
         }

         ci.cancel();
      }
   }

   @Inject(at = @At("HEAD"), method = "tick()V")
   public void tickHead(CallbackInfo info) {
      try {
         Alien.EVENT_BUS.post(ClientTickEvent.get(Event.Stage.Pre));
      } catch (Exception var3) {
         var3.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (MinecraftClient.tick() [HEAD]) Message: [" + var3.getMessage() + "]");
         }
      }
   }

   @Inject(at = @At("TAIL"), method = "tick()V")
   public void tickTail(CallbackInfo info) {
      try {
         Alien.EVENT_BUS.post(ClientTickEvent.get(Event.Stage.Post));
      } catch (Exception var3) {
         var3.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (MinecraftClient.tick() [TAIL]) Message: [" + var3.getMessage() + "]");
         }
      }
   }

   /**
    * @author
    * @reason
    */
   @Overwrite
   private String getWindowTitle() {
      if (ClientSetting.INSTANCE == null) {
         return "Alien: Loading..";
      } else if (ClientSetting.INSTANCE.titleOverride.getValue()) {
         return ClientSetting.INSTANCE.windowTitle.getValue();
      } else {
         StringBuilder stringBuilder = new StringBuilder(ClientSetting.INSTANCE.windowTitle.getValue());
         stringBuilder.append(" ");
         stringBuilder.append(SharedConstants.getGameVersion().getName());
         ClientPlayNetworkHandler clientPlayNetworkHandler = this.getNetworkHandler();
         if (clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isOpen()) {
            stringBuilder.append(" - ");
            ServerInfo serverInfo = this.getCurrentServerEntry();
            if (this.server != null && !this.server.isRemote()) {
               stringBuilder.append(I18n.translate("title.singleplayer"));
            } else if (serverInfo != null && serverInfo.isRealm()) {
               stringBuilder.append(I18n.translate("title.multiplayer.realms"));
            } else if (this.server != null || serverInfo != null && serverInfo.isLocal()) {
               stringBuilder.append(I18n.translate("title.multiplayer.lan"));
            } else {
               stringBuilder.append(I18n.translate("title.multiplayer.other"));
            }
         }

         return stringBuilder.toString();
      }
   }

   @Shadow
   public ClientPlayNetworkHandler getNetworkHandler() {
      return null;
   }

   @Shadow
   public ServerInfo getCurrentServerEntry() {
      return null;
   }
}
