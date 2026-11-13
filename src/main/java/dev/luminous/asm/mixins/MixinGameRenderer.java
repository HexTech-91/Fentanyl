package dev.luminous.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.utils.render.AlienFrameStamp;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.api.utils.world.InteractUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.Freecam;
import dev.luminous.mod.modules.impl.player.InteractTweaks;
import dev.luminous.mod.modules.impl.render.AspectRatio;
import dev.luminous.mod.modules.impl.render.Chams;
import dev.luminous.mod.modules.impl.render.Fov;
import dev.luminous.mod.modules.impl.render.HighLight;
import dev.luminous.mod.modules.impl.render.NoRender;
import dev.luminous.mod.modules.impl.render.Zoom;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GameRenderer.class})
public class MixinGameRenderer {
   @Shadow
   @Final
   MinecraftClient client;
   @Shadow
   private float fovMultiplier;
   @Shadow
   private float lastFovMultiplier;
   @Shadow
   private boolean renderingPanorama;
   @Shadow
   private float zoom;
   @Shadow
   private float zoomX;
   @Shadow
   private float zoomY;
   @Shadow
   private float viewDistance;
   @Unique
   private Entity cameraEntity;
   @Unique
   private float originalYaw;
   @Unique
   private float originalPitch;

   @Shadow
   private static HitResult ensureTargetInRange(HitResult var0, Vec3d var1, double var2) {
      Vec3d var4 = var0.getPos();
      if (!var4.isInRange(var1, var2)) {
         Vec3d var5 = var0.getPos();
         Direction var6 = Direction.getFacing(var5.x - var1.x, var5.y - var1.y, var5.z - var1.z);
         return BlockHitResult.createMissed(var5, var6, BlockPos.ofFloored(var5));
      } else {
         return var0;
      }
   }

   @Inject(
      method = {"showFloatingItem(Lnet/minecraft/item/ItemStack;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onShowFloatingItem(ItemStack var1, CallbackInfo var2) {
      if (var1.getItem() == Items.TOTEM_OF_UNDYING && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.totem.getValue()) {
         var2.cancel();
      }
   }

   @Redirect(
      method = {"renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"
      ),
      require = 0
   )
   private float applyCameraTransformationsMathHelperLerpProxy(float var1, float var2, float var3) {
      return NoRender.INSTANCE.isOn() && NoRender.INSTANCE.nausea.getValue() ? 0.0F : MathHelper.lerp(var1, var2, var3);
   }

   @Inject(
      method = {"tiltViewWhenHurt(Lnet/minecraft/client/util/math/MatrixStack;F)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void tiltViewWhenHurtHook(MatrixStack var1, float var2, CallbackInfo var3) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.hurtCam.getValue()) {
         var3.cancel();
      }
   }

   @Inject(
      method = {"shouldRenderBlockOutline()Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void hookOutline(CallbackInfoReturnable<Boolean> var1) {
      if (HighLight.INSTANCE.isOn()) {
         var1.setReturnValue(false);
      }
   }

   @Inject(
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z",
         opcode = 180,
         ordinal = 0
      )},
      method = {"renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V"}
   )
   void render3dHook(RenderTickCounter var1, CallbackInfo var2) {
      if (!Module.nullCheck()) {
         Camera var3 = MinecraftClient.getInstance().gameRenderer.getCamera();
         MatrixStack var4 = new MatrixStack();
         var4.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var3.getPitch()));
         var4.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var3.getYaw() + 180.0F));
         TextUtil.lastProjMat.set(RenderSystem.getProjectionMatrix());
         TextUtil.lastModMat.set(RenderSystem.getModelViewMatrix());
         TextUtil.lastWorldSpaceMatrix.set(var4.peek().getPositionMatrix());
         AlienFrameStamp.next();
         Alien.FPS.record();
         Alien.MODULE.render3D(var4);
      }
   }

   @Inject(
      at = {@At("TAIL")},
      method = {"renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V"}
   )
   void render3dTail(RenderTickCounter var1, CallbackInfo var2) {
      if (Chams.INSTANCE.isOn() && Chams.INSTANCE.hand.booleanValue) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   @Inject(
      method = {"getFov(Lnet/minecraft/client/render/Camera;FZ)D"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getFov(Camera var1, float var2, boolean var3, CallbackInfoReturnable<Double> var4) {
      if (!this.renderingPanorama && (Fov.INSTANCE.isOn() || Zoom.on)) {
         double var5 = 70.0;
         if (var3) {
            if (Fov.INSTANCE.isOn()) {
               double var7 = Fov.INSTANCE.fov.getValue();
               if (Zoom.on) {
                  var4.setReturnValue(Math.min(Math.max(var7 - Zoom.INSTANCE.currentFov, 1.0), 177.0));
               } else {
                  var4.setReturnValue(var7);
               }

               return;
            }

            var5 = ((Integer)this.client.options.getFov().getValue()).intValue();
            var5 *= MathHelper.lerp(var2, this.lastFovMultiplier, this.fovMultiplier);
            if (Zoom.on) {
               var5 = Math.min(Math.max(var5 - Zoom.INSTANCE.currentFov, 1.0), 177.0);
            }
         } else if (Fov.INSTANCE.isOn()) {
            var4.setReturnValue(Fov.INSTANCE.itemFov.getValue());
            return;
         }

         if (var1.getFocusedEntity() instanceof LivingEntity && ((LivingEntity)var1.getFocusedEntity()).isDead()) {
            float var10 = Math.min(((LivingEntity)var1.getFocusedEntity()).deathTime + var2, 20.0F);
            var5 /= (1.0F - 500.0F / (var10 + 500.0F)) * 2.0F + 1.0F;
         }

         CameraSubmersionType var11;
         if ((var11 = var1.getSubmersionType()) == CameraSubmersionType.LAVA || var11 == CameraSubmersionType.WATER) {
            var5 *= MathHelper.lerp((Double)this.client.options.getFovEffectScale().getValue(), 1.0, 0.85714287F);
         }

         var4.setReturnValue(var5);
      }
   }

   @Inject(
      method = {"getNightVisionStrength(Lnet/minecraft/entity/LivingEntity;F)F"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void getNightVisionStrengthHook(LivingEntity var0, float var1, CallbackInfoReturnable<Float> var2) {
      StatusEffectInstance var3 = var0.getStatusEffect(StatusEffects.NIGHT_VISION);
      if (var3 == null) {
         var2.setReturnValue(1.0F);
      }
   }

   @Inject(
      method = {"renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V",
         shift = Shift.AFTER
      )},
      require = 0
   )
   public void postRender3dHook(RenderTickCounter var1, CallbackInfo var2) {
      Alien.SHADER.renderShaders();
   }

   @Inject(
      method = {"renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V"},
      at = {@At("TAIL")},
      require = 0
   )
   public void renderShadersTailHook(RenderTickCounter var1, CallbackInfo var2) {
      Alien.SHADER.renderShaders();
   }

   @Inject(
      method = {"getBasicProjectionMatrix(D)Lorg/joml/Matrix4f;"},
      at = {@At("TAIL")},
      cancellable = true
   )
   public void getBasicProjectionMatrixHook(double var1, CallbackInfoReturnable<Matrix4f> var3) {
      if (AspectRatio.INSTANCE.isOn()) {
         MatrixStack var4 = new MatrixStack();
         var4.peek().getPositionMatrix().identity();
         if (this.zoom != 1.0F) {
            var4.translate(this.zoomX, -this.zoomY, 0.0F);
            var4.scale(this.zoom, this.zoom, 1.0F);
         }

         var4.peek()
            .getPositionMatrix()
            .mul(
               new Matrix4f()
                  .setPerspective((float)(var1 * (float) (Math.PI / 180.0)), AspectRatio.INSTANCE.ratio.getValueFloat(), 0.05F, this.viewDistance * 4.0F)
            );
         var3.setReturnValue(var4.peek().getPositionMatrix());
      }
   }

   @Inject(
      method = {"findCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void findCrosshairTarget(Entity var1, double var2, double var4, float var6, CallbackInfoReturnable<HitResult> var7) {
      if (Freecam.INSTANCE.isOn()) {
         var7.setReturnValue(
            InteractUtil.getRtxTarget(
               Freecam.INSTANCE.getFakeYaw(),
               Freecam.INSTANCE.getFakePitch(),
               Freecam.INSTANCE.getFakeX(),
               Freecam.INSTANCE.getFakeY(),
               Freecam.INSTANCE.getFakeZ()
            )
         );
      } else {
         double var8 = Math.max(var2, var4);
         double var10 = MathHelper.square(var8);
         Vec3d var12 = var1.getCameraPosVec(var6);
         InteractTweaks.INSTANCE.isActive = InteractTweaks.INSTANCE.ghostHand();
         HitResult var13 = var1.raycast(var8, var6, false);
         InteractTweaks.INSTANCE.isActive = false;
         double var14 = var13.getPos().squaredDistanceTo(var12);
         if (var13.getType() != Type.MISS) {
            var10 = var14;
            var8 = Math.sqrt(var14);
         }

         Vec3d var16 = var1.getRotationVec(var6);
         Vec3d var17 = var12.add(var16.x * var8, var16.y * var8, var16.z * var8);
         Box var18 = var1.getBoundingBox().stretch(var16.multiply(var8)).expand(1.0, 1.0, 1.0);
         if (!InteractTweaks.INSTANCE.noEntityTrace()) {
            EntityHitResult var19 = ProjectileUtil.raycast(var1, var12, var17, var18, var0 -> !var0.isSpectator() && var0.canHit(), var10);
            var7.setReturnValue(
               var19 != null && var19.getPos().squaredDistanceTo(var12) < var14
                  ? ensureTargetInRange(var19, var12, var4)
                  : ensureTargetInRange(var13, var12, var2)
            );
         } else {
            var7.setReturnValue(ensureTargetInRange(var13, var12, var2));
         }
      }
   }
}
