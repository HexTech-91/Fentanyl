package dev.luminous.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.events.impl.WeatherRenderEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.core.impl.ShaderManager;
import dev.luminous.mod.modules.impl.player.Freecam;
import dev.luminous.mod.modules.impl.render.Chams;
import dev.luminous.mod.modules.impl.render.NoRender;
import dev.luminous.mod.modules.impl.render.ShaderModule;
import dev.luminous.mod.modules.impl.render.Skybox;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldRenderer.class})
public abstract class MixinWorldRenderer {
   @Unique
   boolean renderingChams = false;
   @Unique
   boolean renderingEntity = false;

   @Inject(
      method = {"renderEntity(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"},
      at = {@At("HEAD")}
   )
   private void injectChamsForEntity(
      Entity var1, double var2, double var4, double var6, float var8, MatrixStack var9, VertexConsumerProvider var10, CallbackInfo var11
   ) {
      if (Chams.INSTANCE.isOn() && Chams.INSTANCE.throughWall.getValue()) {
         if (Chams.INSTANCE.chams(var1)) {
            if (this.renderingEntity) {
               Wrapper.mc.getBufferBuilders().getEntityVertexConsumers().draw();
               this.renderingEntity = false;
            }

            GL11.glEnable(32823);
            GL11.glPolygonOffset(1.0F, -1000000.0F);
            RenderSystem.depthMask(false);
            this.renderingChams = true;
         } else {
            this.renderingEntity = true;
         }
      }
   }

   @Inject(
      method = {"renderEntity(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"},
      at = {@At("RETURN")}
   )
   private void injectChamsForEntityPost(
      Entity var1, double var2, double var4, double var6, float var8, MatrixStack var9, VertexConsumerProvider var10, CallbackInfo var11
   ) {
      if (Chams.INSTANCE.isOn() && Chams.INSTANCE.throughWall.getValue() && this.renderingChams) {
         Wrapper.mc.getBufferBuilders().getEntityVertexConsumers().draw();
         GL11.glPolygonOffset(1.0F, 1000000.0F);
         GL11.glDisable(32823);
         RenderSystem.depthMask(true);
         this.renderingChams = false;
      }
   }

   @Redirect(
      method = {"render(Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gl/PostEffectProcessor;render(F)V",
         ordinal = 0
      ),
      require = 0
   )
   void replaceShaderHook(PostEffectProcessor var1, float var2) {
      ShaderManager.Shader var3 = ShaderModule.INSTANCE.mode.getValue();
      if (ShaderModule.INSTANCE.isOn() && Wrapper.mc.world != null) {
         Alien.SHADER.setupShader(var3, Alien.SHADER.getShaderOutline(var3));
      } else {
         var1.render(var2);
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"},
      at = {@At("TAIL")}
   )
   private void shaderFallbackHook(CallbackInfo var1) {
      if (ShaderModule.INSTANCE.isOn() && Wrapper.mc.world != null) {
         ShaderManager.Shader var2 = ShaderModule.INSTANCE.mode.getValue();
         Alien.SHADER.setupShader(var2, Alien.SHADER.getShaderOutline(var2));
      }
   }

   @ModifyArg(
      method = {"render(Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"
      ),
      index = 3
   )
   private boolean renderSetupTerrainModifyArg(boolean var1) {
      return Freecam.INSTANCE.isOn() || var1;
   }

   @Inject(
      method = {"renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderSky(Matrix4f var1, Matrix4f var2, float var3, Camera var4, boolean var5, Runnable var6, CallbackInfo var7) {
      Skybox var8 = Skybox.INSTANCE;
      if (var8 != null && var8.isOn()) {
         var8.renderSkybox();
         var7.cancel();
      }
   }

   @Inject(
      method = {"renderWeather(Lnet/minecraft/client/render/LightmapTextureManager;FDDD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderWeather(LightmapTextureManager var1, float var2, double var3, double var5, double var7, CallbackInfo var9) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.weather.getValue()) {
         var9.cancel();
      } else {
         WeatherRenderEvent var10 = WeatherRenderEvent.get(var1, var2, var3, var5, var7);
         Alien.EVENT_BUS.post(var10);
         if (var10.isCancelled()) {
            var9.cancel();
         }
      }
   }
}
