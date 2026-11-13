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
   public static ManagedShaderEffect SMOKE_OUTLINE;
   public static ManagedShaderEffect GRADIENT_OUTLINE;
   public static ManagedShaderEffect SNOW_OUTLINE;
   public static ManagedShaderEffect FLOW_OUTLINE;
   public static ManagedShaderEffect RAINBOW_OUTLINE;
   public static ManagedShaderEffect DEFAULT;
   public static ManagedShaderEffect SMOKE;
   public static ManagedShaderEffect GRADIENT;
   public static ManagedShaderEffect SNOW;
   public static ManagedShaderEffect FLOW;
   public static ManagedShaderEffect RAINBOW;
   public static ManagedShaderEffect SKYBOX;
   public static ManagedShaderEffect MOTION_BLUR;
   public float time = 0.0F;
   private ShaderManager.MyFramebuffer shaderBuffer;

   public void renderShader(Runnable var1, ShaderManager.Shader var2) {
      tasks.add(new ShaderManager.RenderTask(var1, var2));
   }

   public void renderShaders() {
      tasks.forEach(var1 -> this.applyShader(var1.task(), var1.shader()));
      tasks.clear();
   }

   public void applyShader(Runnable var1, ShaderManager.Shader var2) {
      try {
         if (this.fullNullCheck()) {
            return;
         }

         RenderSystem.assertOnRenderThreadOrInit();
         Framebuffer var3 = MinecraftClient.getInstance().getFramebuffer();
         if (this.shaderBuffer.textureWidth != var3.textureWidth || this.shaderBuffer.textureHeight != var3.textureHeight) {
            this.shaderBuffer.resize(var3.textureWidth, var3.textureHeight, false);
         }

         GlStateManager._glBindFramebuffer(36009, this.shaderBuffer.fbo);
         this.shaderBuffer.beginWrite(true);
         var1.run();
         this.shaderBuffer.endWrite();
         GlStateManager._glBindFramebuffer(36009, var3.fbo);
         var3.beginWrite(false);
         ManagedShaderEffect var4 = this.getShader(var2);
         PostEffectProcessor var5 = var4.getShaderEffect();
         if (var5 == null) {
            return;
         }

         ((IShaderEffectHook)var5).alienClient$addHook("bufIn", this.shaderBuffer);
         Framebuffer var6 = var5.getSecondaryTarget("bufOut");
         if (var6 == null) {
            return;
         }

         this.setupShader(var2, var4);
         this.shaderBuffer.clear(false);
         var3.beginWrite(false);
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ZERO, DstFactor.ONE);
         RenderSystem.backupProjectionMatrix();
         var6.draw(var6.textureWidth, var6.textureHeight, false);
         RenderSystem.restoreProjectionMatrix();
         RenderSystem.disableBlend();
      } catch (Exception var7) {
         var7.printStackTrace();
      }
   }

   public ManagedShaderEffect getShader(@NotNull ShaderManager.Shader var1) {
      return switch (var1) {
         case Smoke -> SMOKE;
         case Gradient -> GRADIENT;
         case Flow -> FLOW;
         case Rainbow -> RAINBOW;
         case Snow -> SNOW;
         default -> DEFAULT;
      };
   }

   public ManagedShaderEffect getShaderOutline(@NotNull ShaderManager.Shader var1) {
      return switch (var1) {
         case Smoke -> SMOKE_OUTLINE;
         case Gradient -> GRADIENT_OUTLINE;
         case Flow -> FLOW_OUTLINE;
         case Rainbow -> RAINBOW_OUTLINE;
         case Snow -> SNOW_OUTLINE;
         default -> DEFAULT_OUTLINE;
      };
   }

   public void setupShader(ShaderManager.Shader var1, ManagedShaderEffect var2) {
      if (var2 != null) {
         try {
            Color var3 = ShaderModule.INSTANCE.fill.getValue();
            this.time = (float)timer.getMs() / 5.0F * ShaderModule.INSTANCE.speed.getValueFloat() * 0.004F;
            if (var1 == ShaderManager.Shader.Rainbow) {
               safeSetUniform(var2, "alpha2", var3.getAlpha() / 255.0F);
               safeSetUniform(var2, "radius", ShaderModule.INSTANCE.radius.getValueFloat());
               safeSetUniform(var2, "quality", ShaderModule.INSTANCE.smoothness.getValueFloat());
               safeSetUniform(var2, "divider", ShaderModule.INSTANCE.divider.getValueFloat());
               safeSetUniform(var2, "maxSample", ShaderModule.INSTANCE.maxSample.getValueFloat());
               safeSetUniform(var2, "resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
               safeSetUniform(var2, "time", this.time);
               var2.render(mc.getRenderTickCounter().getTickDelta(true));
            } else if (var1 == ShaderManager.Shader.Gradient) {
               safeSetUniform(var2, "alpha2", var3.getAlpha() / 255.0F);
               Color var4 = ShaderModule.INSTANCE.smoke1.getValue();
               Color var5 = ShaderModule.INSTANCE.smoke2.getValue();
               Color var6 = ShaderModule.INSTANCE.smoke3.getValue();
               Color var7 = ShaderModule.INSTANCE.smoke4.getValue();
               safeSetUniform(var2, "rgb", var4.getRed() / 255.0F, var4.getGreen() / 255.0F, var4.getBlue() / 255.0F);
               safeSetUniform(var2, "rgb1", var5.getRed() / 255.0F, var5.getGreen() / 255.0F, var5.getBlue() / 255.0F);
               safeSetUniform(var2, "rgb2", var6.getRed() / 255.0F, var6.getGreen() / 255.0F, var6.getBlue() / 255.0F);
               safeSetUniform(var2, "rgb3", var7.getRed() / 255.0F, var7.getGreen() / 255.0F, var7.getBlue() / 255.0F);
               safeSetUniform(var2, "step", ShaderModule.INSTANCE.step.getValueFloat() * 300.0F);
               safeSetUniform(var2, "radius", ShaderModule.INSTANCE.radius.getValueFloat());
               safeSetUniform(var2, "quality", ShaderModule.INSTANCE.smoothness.getValueFloat());
               safeSetUniform(var2, "divider", ShaderModule.INSTANCE.divider.getValueFloat());
               safeSetUniform(var2, "maxSample", ShaderModule.INSTANCE.maxSample.getValueFloat());
               safeSetUniform(var2, "resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
               safeSetUniform(var2, "time", this.time * 300.0F);
               var2.render(mc.getRenderTickCounter().getTickDelta(true));
            } else if (var1 == ShaderManager.Shader.Smoke) {
               safeSetUniform(var2, "alpha1", var3.getAlpha() / 255.0F);
               Color var9 = ShaderModule.INSTANCE.smoke1.getValue();
               Color var10 = ShaderModule.INSTANCE.smoke2.getValue();
               Color var11 = ShaderModule.INSTANCE.smoke3.getValue();
               safeSetUniform(var2, "radius", ShaderModule.INSTANCE.radius.getValueFloat());
               safeSetUniform(var2, "quality", ShaderModule.INSTANCE.smoothness.getValueFloat());
               safeSetUniform(var2, "divider", ShaderModule.INSTANCE.divider.getValueFloat());
               safeSetUniform(var2, "maxSample", ShaderModule.INSTANCE.maxSample.getValueFloat());
               safeSetUniform(var2, "first", var9.getRed() / 255.0F, var9.getGreen() / 255.0F, var9.getBlue() / 255.0F, var9.getAlpha() / 255.0F);
               safeSetUniform(var2, "second", var10.getRed() / 255.0F, var10.getGreen() / 255.0F, var10.getBlue() / 255.0F);
               safeSetUniform(var2, "third", var11.getRed() / 255.0F, var11.getGreen() / 255.0F, var11.getBlue() / 255.0F);
               safeSetUniform(var2, "oct", (int)ShaderModule.INSTANCE.octaves.getValueFloat());
               safeSetUniform(var2, "resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
               safeSetUniform(var2, "time", this.time);
               var2.render(mc.getRenderTickCounter().getTickDelta(true));
            } else if (var1 == ShaderManager.Shader.Solid) {
               safeSetUniform(var2, "mixFactor", var3.getAlpha() / 255.0F);
               safeSetUniform(var2, "minAlpha", ShaderModule.INSTANCE.alpha.getValueFloat() / 255.0F);
               safeSetUniform(var2, "radius", ShaderModule.INSTANCE.radius.getValueFloat());
               safeSetUniform(var2, "quality", ShaderModule.INSTANCE.smoothness.getValueFloat());
               safeSetUniform(var2, "divider", ShaderModule.INSTANCE.divider.getValueFloat());
               safeSetUniform(var2, "maxSample", ShaderModule.INSTANCE.maxSample.getValueFloat());
               safeSetUniform(var2, "color", var3.getRed() / 255.0F, var3.getGreen() / 255.0F, var3.getBlue() / 255.0F);
               safeSetUniform(var2, "resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
               var2.render(mc.getRenderTickCounter().getTickDelta(true));
            } else if (var1 == ShaderManager.Shader.Flow) {
               safeSetUniform(var2, "mixFactor", var3.getAlpha() / 255.0F);
               safeSetUniform(var2, "radius", ShaderModule.INSTANCE.radius.getValueFloat());
               safeSetUniform(var2, "quality", ShaderModule.INSTANCE.smoothness.getValueFloat());
               safeSetUniform(var2, "divider", ShaderModule.INSTANCE.divider.getValueFloat());
               safeSetUniform(var2, "maxSample", ShaderModule.INSTANCE.maxSample.getValueFloat());
               safeSetUniform(var2, "resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
               safeSetUniform(var2, "time", this.time);
               var2.render(mc.getRenderTickCounter().getTickDelta(true));
            }
         } catch (Exception var8) {
            var8.printStackTrace();
         }
      }
   }

   public boolean skyboxNullCheck() {
      if (mc == null || mc.getFramebuffer() == null) {
         return true;
      } else if (SKYBOX == null) {
         this.reloadShaders();
         return true;
      } else {
         return false;
      }
   }

   public void renderSkyboxFullscreen(Color var1, Color var2, float var3, float var4, float var5) {
      if (!this.skyboxNullCheck()) {
         RenderSystem.assertOnRenderThreadOrInit();
         ManagedShaderEffect var6 = SKYBOX;
         if (var6 != null && var6.getShaderEffect() != null) {
            Framebuffer var7 = MinecraftClient.getInstance().getFramebuffer();
            PostEffectProcessor var8 = var6.getShaderEffect();
            if (var8 != null) {
               try {
                  ((IShaderEffectHook)var8).alienClient$addHook("bufIn", var7);
               } catch (Exception var13) {
               }
            }

            Framebuffer var9 = var6.getShaderEffect().getSecondaryTarget("bufOut");
            if (var9 != null) {
               float var10 = (float)timer.getMs() / 5.0F * var4 * 0.004F;
               safeSetUniform(var6, "mixFactor", var5);
               safeSetUniform(var6, "u_Color", var1.getRed() / 255.0F, var1.getGreen() / 255.0F, var1.getBlue() / 255.0F, var1.getAlpha() / 255.0F);
               safeSetUniform(var6, "u_BackgroundColor", var2.getRed() / 255.0F, var2.getGreen() / 255.0F, var2.getBlue() / 255.0F, var2.getAlpha() / 255.0F);
               safeSetUniform(var6, "u_Resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
               safeSetUniform(var6, "u_Mouse", 0.0F, 0.0F);
               safeSetUniform(var6, "u_Scale", var3);
               safeSetUniform(var6, "u_Time", var10);

               try {
                  var6.render(mc.getRenderTickCounter().getTickDelta(true));
               } catch (Exception var12) {
               }

               var7.beginWrite(false);
               RenderSystem.enableBlend();
               RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ZERO, DstFactor.ONE);
               RenderSystem.backupProjectionMatrix();
               var9.draw(var9.textureWidth, var9.textureHeight, false);
               RenderSystem.restoreProjectionMatrix();
               RenderSystem.disableBlend();
            }
         }
      }
   }

   private static void safeSetUniform(ManagedShaderEffect var0, String var1, float var2) {
      if (var0 != null) {
         try {
            var0.setUniformValue(var1, var2);
         } catch (Throwable var4) {
         }
      }
   }

   private static void safeSetUniform(ManagedShaderEffect var0, String var1, float var2, float var3) {
      if (var0 != null) {
         try {
            var0.setUniformValue(var1, var2, var3);
         } catch (Throwable var5) {
         }
      }
   }

   private static void safeSetUniform(ManagedShaderEffect var0, String var1, float var2, float var3, float var4) {
      if (var0 != null) {
         try {
            var0.setUniformValue(var1, var2, var3, var4);
         } catch (Throwable var6) {
         }
      }
   }

   private static void safeSetUniform(ManagedShaderEffect var0, String var1, float var2, float var3, float var4, float var5) {
      if (var0 != null) {
         try {
            var0.setUniformValue(var1, var2, var3, var4, var5);
         } catch (Throwable var7) {
         }
      }
   }

   public void reloadShaders() {
      DEFAULT = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/outline.json"));
      SMOKE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/smoke.json"));
      GRADIENT = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/gradient.json"));
      SNOW = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/snow.json"));
      FLOW = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/flow.json"));
      RAINBOW = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/rainbow.json"));
      SKYBOX = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/skybox.json"));
      MOTION_BLUR = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/motion_blur.json"));
      DEFAULT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/outline.json"), var0 -> {
         PostEffectProcessor var1 = var0.getShaderEffect();
         if (var1 != null) {
            ((IShaderEffectHook)var1).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)var1).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      SMOKE_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/smoke.json"), var0 -> {
         PostEffectProcessor var1 = var0.getShaderEffect();
         if (var1 != null) {
            ((IShaderEffectHook)var1).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)var1).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      GRADIENT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/gradient.json"), var0 -> {
         PostEffectProcessor var1 = var0.getShaderEffect();
         if (var1 != null) {
            ((IShaderEffectHook)var1).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)var1).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      SNOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/snow.json"), var0 -> {
         PostEffectProcessor var1 = var0.getShaderEffect();
         if (var1 != null) {
            ((IShaderEffectHook)var1).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)var1).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      FLOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/flow.json"), var0 -> {
         PostEffectProcessor var1 = var0.getShaderEffect();
         if (var1 != null) {
            ((IShaderEffectHook)var1).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)var1).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
      RAINBOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("shaders/post/rainbow.json"), var0 -> {
         PostEffectProcessor var1 = var0.getShaderEffect();
         if (var1 != null) {
            ((IShaderEffectHook)var1).alienClient$addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffectHook)var1).alienClient$addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
         }
      });
   }

   public boolean fullNullCheck() {
      if (GRADIENT != null
         && SMOKE != null
         && DEFAULT != null
         && SNOW != null
         && FLOW != null
         && RAINBOW != null
         && GRADIENT_OUTLINE != null
         && SMOKE_OUTLINE != null
         && DEFAULT_OUTLINE != null
         && SNOW_OUTLINE != null
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
      public MyFramebuffer(int var1, int var2) {
         super(false);
         RenderSystem.assertOnRenderThreadOrInit();
         this.resize(var1, var2, true);
         this.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      }
   }

   public record RenderTask(Runnable task, ShaderManager.Shader shader) {
   }

   public static enum Shader {
      Solid,
      Smoke,
      Gradient,
      Flow,
      Rainbow,
      Snow;
   }
}
