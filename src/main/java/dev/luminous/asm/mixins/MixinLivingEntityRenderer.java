package dev.luminous.asm.mixins;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.render.EveryonePig;
import dev.luminous.mod.modules.impl.render.NoRender;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({LivingEntityRenderer.class})
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> {
   @Unique
   private LivingEntity lastEntity;
   @Unique
   private float originalYaw;
   @Unique
   private float originalHeadYaw;
   @Unique
   private float originalBodyYaw;
   @Unique
   private float originalPitch;
   @Unique
   private float originalPrevYaw;
   @Unique
   private float originalPrevHeadYaw;
   @Unique
   private float originalPrevBodyYaw;
   @Unique
   private float rotationVisualYawOffset;
   @Unique
   private float rotationVisualPitchOffset;
   @Unique
   private boolean scalePushed;

   @Inject(
      method = {"render*(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onRenderPre(T var1, float var2, float var3, MatrixStack var4, VertexConsumerProvider var5, int var6, CallbackInfo var7) {
      this.scalePushed = false;
      PlayerEntity var8;
      if (EveryonePig.INSTANCE != null
         && EveryonePig.INSTANCE.isOn()
         && var1 instanceof PlayerEntity
         && EveryonePig.INSTANCE.shouldReplace(var8 = (PlayerEntity)var1)) {
         var7.cancel();
      } else {
         if (MinecraftClient.getInstance().player != null && var1 == MinecraftClient.getInstance().player && ClientSetting.INSTANCE.rotations.getValue()) {
            this.originalYaw = var1.getYaw();
            this.originalHeadYaw = var1.headYaw;
            this.originalBodyYaw = var1.bodyYaw;
            this.originalPitch = var1.getPitch();
            this.originalPrevYaw = var1.prevYaw;
            this.originalPrevHeadYaw = var1.prevHeadYaw;
            this.originalPrevBodyYaw = var1.prevBodyYaw;
            var1.setYaw(RotationManager.getRenderYawOffset());
            var1.headYaw = RotationManager.getRotationYawHead();
            var1.bodyYaw = RotationManager.getRenderYawOffset();
            var1.setPitch(RotationManager.getRenderPitch());
            var1.prevYaw = RotationManager.getPrevRenderYawOffset();
            var1.prevHeadYaw = RotationManager.getPrevRotationYawHead();
            var1.prevBodyYaw = RotationManager.getPrevRenderYawOffset();
            var1.prevPitch = RotationManager.getPrevRenderPitch();
         }

         this.lastEntity = var1;
      }
   }

   @Inject(
      method = {"render*(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("TAIL")}
   )
   public void onRenderPost(T var1, float var2, float var3, MatrixStack var4, VertexConsumerProvider var5, int var6, CallbackInfo var7) {
      if (MinecraftClient.getInstance().player != null && var1 == MinecraftClient.getInstance().player && ClientSetting.INSTANCE.rotations.getValue()) {
         var1.setYaw(this.originalYaw);
         var1.headYaw = this.originalHeadYaw;
         var1.bodyYaw = this.originalBodyYaw;
         var1.setPitch(this.originalPitch);
         var1.prevYaw = this.originalPrevYaw;
         var1.prevHeadYaw = this.originalPrevHeadYaw;
         var1.prevBodyYaw = this.originalPrevBodyYaw;
         var1.prevPitch = this.originalPitch;
      }

      if (this.scalePushed) {
         var4.pop();
      }
   }

   @ModifyArgs(
      method = {"render*(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
      )
   )
   private void renderHook(Args var1) {
      float var4 = -1.0F;
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.antiPlayerCollision.getValue() && this.lastEntity != Wrapper.mc.player) {
         LivingEntity var3 = this.lastEntity;
         PlayerEntity var2;
         if (this.lastEntity instanceof PlayerEntity && !(var2 = (PlayerEntity)var3).isInvisible()) {
            var4 = MathUtil.clamp((float)(Wrapper.mc.player.squaredDistanceTo(this.lastEntity.getPos()) / 3.0) + 0.2F, 0.0F, 1.0F);
         }
      }

      if (var4 != -1.0F) {
         var1.set(4, this.applyOpacity(654311423, var4));
      }
   }

   @Unique
   int applyOpacity(int var1, float var2) {
      var2 = Math.min(1.0F, Math.max(0.0F, var2));
      Color var3 = new Color(var1);
      return new Color(var3.getRed(), var3.getGreen(), var3.getBlue(), (int)(var3.getAlpha() * var2)).getRGB();
   }
}
