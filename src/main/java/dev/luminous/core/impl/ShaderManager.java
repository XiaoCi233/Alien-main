package dev.luminous.core.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.interfaces.IShaderEffectHook;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.impl.render.ShaderModule;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;

public class ShaderManager implements Wrapper {
   static final Timer timer = new Timer();
   private static final List<ShaderManager.RenderTask> tasks = new ArrayList<>();
   public static ManagedShaderEffect DEFAULT_OUTLINE;
   public static ManagedShaderEffect PULSE_OUTLINE;
   public static ManagedShaderEffect SMOKE_OUTLINE;
   public static ManagedShaderEffect GRADIENT_OUTLINE;
   public static ManagedShaderEffect SNOW_OUTLINE;
   public static ManagedShaderEffect FLOW_OUTLINE;
   public static ManagedShaderEffect RAINBOW_OUTLINE;
   public static ManagedShaderEffect DEFAULT;
   public static ManagedShaderEffect PULSE;
   public static ManagedShaderEffect SMOKE;
   public static ManagedShaderEffect GRADIENT;
   public static ManagedShaderEffect SNOW;
   public static ManagedShaderEffect FLOW;
   public static ManagedShaderEffect RAINBOW;
   public float time = 0.0F;
   private ShaderManager.MyFramebuffer shaderBuffer;

   public void renderShader(Runnable runnable, ShaderManager.Shader mode) {
      tasks.add(new ShaderManager.RenderTask(runnable, mode));
   }

   public void renderShaders() {
      tasks.forEach(t -> this.applyShader(t.task(), t.shader()));
      tasks.clear();
   }

   public void applyShader(Runnable runnable, ShaderManager.Shader mode) {
      if (!this.fullNullCheck()) {
         RenderSystem.assertOnRenderThreadOrInit();
         Framebuffer MCBuffer = MinecraftClient.getInstance().getFramebuffer();
         if (this.shaderBuffer.textureWidth != MCBuffer.textureWidth || this.shaderBuffer.textureHeight != MCBuffer.textureHeight) {
            this.shaderBuffer.resize(MCBuffer.textureWidth, MCBuffer.textureHeight, false);
         }

         GlStateManager._glBindFramebuffer(36009, this.shaderBuffer.fbo);
         this.shaderBuffer.beginWrite(true);
         runnable.run();
         this.shaderBuffer.endWrite();
         GlStateManager._glBindFramebuffer(36009, MCBuffer.fbo);
         MCBuffer.beginWrite(false);
         ManagedShaderEffect shader = this.getShader(mode);
         PostEffectProcessor effect = shader.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).alienClient$addHook("bufIn", this.shaderBuffer);
         }

         Framebuffer outBuffer = shader.getShaderEffect().getSecondaryTarget("bufOut");
         this.setupShader(mode, shader);
         this.shaderBuffer.clear(false);
         MCBuffer.beginWrite(false);
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ZERO, DstFactor.ONE);
         RenderSystem.backupProjectionMatrix();
         outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
         RenderSystem.restoreProjectionMatrix();
         RenderSystem.disableBlend();
      }
   }

   public ManagedShaderEffect getShader(@NotNull ShaderManager.Shader mode) {
      return switch (mode) {
         case Pulse -> PULSE;
         case Smoke -> SMOKE;
         case Gradient -> GRADIENT;
         case Snow -> SNOW;
         case Flow -> FLOW;
         case Rainbow -> RAINBOW;
         default -> DEFAULT;
      };
   }

   public ManagedShaderEffect getShaderOutline(@NotNull ShaderManager.Shader mode) {
      return switch (mode) {
         case Pulse -> PULSE_OUTLINE;
         case Smoke -> SMOKE_OUTLINE;
         case Gradient -> GRADIENT_OUTLINE;
         case Snow -> SNOW_OUTLINE;
         case Flow -> FLOW_OUTLINE;
         case Rainbow -> RAINBOW_OUTLINE;
         default -> DEFAULT_OUTLINE;
      };
   }

   public void setupShader(ShaderManager.Shader shader, ManagedShaderEffect effect) {
      ShaderModule module = ShaderModule.INSTANCE;
      Color color = module.fill.getValue();
      this.time = (float)timer.getMs() / 5.0F * module.speed.getValueFloat() * 0.004F;
      if (shader == ShaderManager.Shader.Rainbow) {
         effect.setUniformValue("alpha2", color.getAlpha() / 255.0F);
         effect.setUniformValue("radius", module.radius.getValueFloat());
         effect.setUniformValue("quality", module.smoothness.getValueFloat());
         effect.setUniformValue("divider", module.divider.getValueFloat());
         effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
         effect.setUniformValue("resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
         effect.setUniformValue("time", this.time);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));
      } else if (shader == ShaderManager.Shader.Gradient) {
         effect.setUniformValue("alpha2", color.getAlpha() / 255.0F);
         effect.setUniformValue(
            "rgb", module.smoke1.getValue().getRed() / 255.0F, module.smoke1.getValue().getGreen() / 255.0F, module.smoke1.getValue().getBlue() / 255.0F
         );
         effect.setUniformValue(
            "rgb1", module.smoke2.getValue().getRed() / 255.0F, module.smoke2.getValue().getGreen() / 255.0F, module.smoke2.getValue().getBlue() / 255.0F
         );
         effect.setUniformValue(
            "rgb2", module.smoke3.getValue().getRed() / 255.0F, module.smoke3.getValue().getGreen() / 255.0F, module.smoke3.getValue().getBlue() / 255.0F
         );
         effect.setUniformValue(
            "rgb3", module.smoke4.getValue().getRed() / 255.0F, module.smoke4.getValue().getGreen() / 255.0F, module.smoke4.getValue().getBlue() / 255.0F
         );
         effect.setUniformValue("step", module.step.getValueFloat() * 300.0F);
         effect.setUniformValue("radius", module.radius.getValueFloat());
         effect.setUniformValue("quality", module.smoothness.getValueFloat());
         effect.setUniformValue("divider", module.divider.getValueFloat());
         effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
         effect.setUniformValue("resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
         effect.setUniformValue("time", this.time * 300.0F);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));
      } else if (shader == ShaderManager.Shader.Smoke) {
         effect.setUniformValue("alpha1", color.getAlpha() / 255.0F);
         effect.setUniformValue("radius", module.radius.getValueFloat());
         effect.setUniformValue("quality", module.smoothness.getValueFloat());
         effect.setUniformValue("divider", module.divider.getValueFloat());
         effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
         effect.setUniformValue(
            "first",
            module.smoke1.getValue().getRed() / 255.0F,
            module.smoke1.getValue().getGreen() / 255.0F,
            module.smoke1.getValue().getBlue() / 255.0F,
            module.smoke1.getValue().getAlpha() / 255.0F
         );
         effect.setUniformValue(
            "second", module.smoke2.getValue().getRed() / 255.0F, module.smoke2.getValue().getGreen() / 255.0F, module.smoke2.getValue().getBlue() / 255.0F
         );
         effect.setUniformValue(
            "third", module.smoke3.getValue().getRed() / 255.0F, module.smoke3.getValue().getGreen() / 255.0F, module.smoke3.getValue().getBlue() / 255.0F
         );
         effect.setUniformValue("oct", (int)module.octaves.getValue());
         effect.setUniformValue("resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
         effect.setUniformValue("time", this.time);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));
      } else if (shader == ShaderManager.Shader.Solid) {
         effect.setUniformValue("mixFactor", color.getAlpha() / 255.0F);
         effect.setUniformValue("minAlpha", module.alpha.getValueFloat() / 255.0F);
         effect.setUniformValue("radius", module.radius.getValueFloat());
         effect.setUniformValue("quality", module.smoothness.getValueFloat());
         effect.setUniformValue("divider", module.divider.getValueFloat());
         effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
         effect.setUniformValue("color", color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
         effect.setUniformValue("resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
         effect.render(mc.getRenderTickCounter().getTickDelta(true));
      } else if (shader == ShaderManager.Shader.Pulse) {
         effect.setUniformValue("mixFactor", color.getAlpha() / 255.0F);
         effect.setUniformValue("minAlpha", module.alpha.getValueFloat() / 255.0F);
         effect.setUniformValue("radius", module.radius.getValueFloat());
         effect.setUniformValue("quality", module.smoothness.getValueFloat());
         effect.setUniformValue("divider", module.divider.getValueFloat());
         effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
         effect.setUniformValue("color", color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
         Color color2 = module.pulse.getValue();
         effect.setUniformValue("color2", color2.getRed() / 255.0F, color2.getGreen() / 255.0F, color2.getBlue() / 255.0F);
         effect.setUniformValue("time", this.time);
         effect.setUniformValue("size", module.pulseSpeed.getValueFloat());
         effect.setUniformValue("resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
         effect.render(mc.getRenderTickCounter().getTickDelta(true));
      } else if (shader == ShaderManager.Shader.Snow) {
         effect.setUniformValue("color", color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
         effect.setUniformValue("radius", module.radius.getValueFloat());
         effect.setUniformValue("quality", module.smoothness.getValueFloat());
         effect.setUniformValue("divider", module.divider.getValueFloat());
         effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
         effect.setUniformValue("resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
         effect.setUniformValue("time", this.time);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));
      } else if (shader == ShaderManager.Shader.Flow) {
         effect.setUniformValue("mixFactor", color.getAlpha() / 255.0F);
         effect.setUniformValue("radius", module.radius.getValueFloat());
         effect.setUniformValue("quality", module.smoothness.getValueFloat());
         effect.setUniformValue("divider", module.divider.getValueFloat());
         effect.setUniformValue("maxSample", module.maxSample.getValueFloat());
         effect.setUniformValue("resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
         effect.setUniformValue("time", this.time);
         effect.render(mc.getRenderTickCounter().getTickDelta(true));
      }
   }

   public void reloadShaders() {
      DEFAULT = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/outline.json"));
      SMOKE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/smoke.json"));
      GRADIENT = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/gradient.json"));
      SNOW = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/snow.json"));
      FLOW = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/flow.json"));
      RAINBOW = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/rainbow.json"));
      PULSE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/pulse.json"));
      DEFAULT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/outline.json"), managedShaderEffect -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      PULSE_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/pulse.json"), managedShaderEffect -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      SMOKE_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/smoke.json"), managedShaderEffect -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      GRADIENT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/gradient.json"), managedShaderEffect -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      SNOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/snow.json"), managedShaderEffect -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      FLOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/flow.json"), managedShaderEffect -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      RAINBOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/rainbow.json"), managedShaderEffect -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)effect).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
   }

   public boolean fullNullCheck() {
      if (GRADIENT != null
         && SMOKE != null
         && DEFAULT != null
         && FLOW != null
         && RAINBOW != null
         && PULSE != null
         && PULSE_OUTLINE != null
         && GRADIENT_OUTLINE != null
         && SMOKE_OUTLINE != null
         && DEFAULT_OUTLINE != null
         && FLOW_OUTLINE != null
         && RAINBOW_OUTLINE != null
         && this.shaderBuffer != null) {
         return false;
      } else if (mc.getFramebuffer() == null) {
         return true;
      } else {
         this.shaderBuffer = new ShaderManager.MyFramebuffer(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
         this.reloadShaders();
         return true;
      }
   }

   public static class MyFramebuffer extends Framebuffer {
      public MyFramebuffer(int width, int height) {
         super(false);
         RenderSystem.assertOnRenderThreadOrInit();
         this.resize(width, height, true);
         this.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      }
   }

   public record RenderTask(Runnable task, ShaderManager.Shader shader) {
   }

   public static enum Shader {
      Solid,
      Pulse,
      Smoke,
      Gradient,
      Snow,
      Flow,
      Rainbow;
   }
}
