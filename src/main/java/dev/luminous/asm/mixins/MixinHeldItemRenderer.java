package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.HeldItemRendererEvent;
import dev.luminous.mod.features.SpearPort;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HeldItemRenderer.class})
public abstract class MixinHeldItemRenderer {
   @Shadow
   private void applyEquipOffset(MatrixStack var1, Arm var2, float var3) {
   }

   @Shadow
   public abstract void renderItem(
      LivingEntity var1, ItemStack var2, ModelTransformationMode var3, boolean var4, MatrixStack var5, VertexConsumerProvider var6, int var7
   );

   @Inject(
      method = {"renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
      )}
   )
   private void onRenderItem(
      AbstractClientPlayerEntity var1,
      float var2,
      float var3,
      Hand var4,
      float var5,
      ItemStack var6,
      float var7,
      MatrixStack var8,
      VertexConsumerProvider var9,
      int var10,
      CallbackInfo var11
   ) {
      HeldItemRendererEvent var12 = HeldItemRendererEvent.get(var4, var6, var7, var8);
      Alien.EVENT_BUS.post(var12);
   }

   @Inject(
      method = {"renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderPortedSpear(
      AbstractClientPlayerEntity var1,
      float var2,
      float var3,
      Hand var4,
      float var5,
      ItemStack var6,
      float var7,
      MatrixStack var8,
      VertexConsumerProvider var9,
      int var10,
      CallbackInfo var11
   ) {
      if (SpearPort.isSpear(var6)) {
         boolean var12 = var4 == Hand.MAIN_HAND ? var1.getMainArm() == Arm.RIGHT : var1.getMainArm() != Arm.RIGHT;
         Arm var13 = var12 ? Arm.RIGHT : Arm.LEFT;
         int var14 = var12 ? 1 : -1;
         boolean var15 = var1.isUsingItem() && var1.getActiveHand() == var4 && var1.getItemUseTimeLeft() > 0;
         HeldItemRendererEvent var16 = HeldItemRendererEvent.get(var4, var6, var7, var8);
         Alien.EVENT_BUS.post(var16);
         var8.push();
         if (var15) {
            var8.translate(var14 * 0.56F, -0.52F, -0.72F);
            float var17 = var6.getMaxUseTime(var1) - (var1.getItemUseTimeLeft() - var2 + 1.0F);
            float var18 = MathHelper.clamp(var17 / 10.0F, 0.0F, 1.0F);
            applyVanillaSpearUseTransform(var18, var8, var17, var13);
         } else {
            this.applyEquipOffset(var8, var13, var7);
            applyVanillaSpearIdleTransform(var5, var8, var14, var13);
         }

         this.renderSpearItem(var1, var6, var12, var8, var9, var10);
         var8.pop();
         var11.cancel();
      }
   }

   private void renderSpearItem(AbstractClientPlayerEntity var1, ItemStack var2, boolean var3, MatrixStack var4, VertexConsumerProvider var5, int var6) {
      SpearPort.setInHandRender(true);

      try {
         this.renderItem(
            var1,
            SpearPort.getVisualStack(var2),
            var3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
            !var3,
            var4,
            var5,
            var6
         );
      } finally {
         SpearPort.setInHandRender(false);
      }
   }

   private static void applyVanillaSpearIdleTransform(float var0, MatrixStack var1, int var2, Arm var3) {
      float var4 = -0.4F * MathHelper.sin(MathHelper.sqrt(var0) * (float) Math.PI);
      float var5 = 0.2F * MathHelper.sin(MathHelper.sqrt(var0) * (float) (Math.PI * 2));
      float var6 = -0.2F * MathHelper.sin(var0 * (float) Math.PI);
      var1.translate(var2 * var4, var5, var6);
      applyVanillaSwingOffset(var1, var3, var0);
   }

   private static void applyVanillaSwingOffset(MatrixStack var0, Arm var1, float var2) {
      int var3 = var1 == Arm.RIGHT ? 1 : -1;
      float var4 = MathHelper.sin(var2 * var2 * (float) Math.PI);
      var0.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var3 * (45.0F + var4 * -20.0F)));
      float var5 = MathHelper.sin(MathHelper.sqrt(var2) * (float) Math.PI);
      var0.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var3 * var5 * -20.0F));
      var0.multiply(RotationAxis.POSITIVE_X.rotationDegrees(var5 * -80.0F));
      var0.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var3 * -45.0F));
   }

   private static void applyVanillaSpearUseTransform(float var0, MatrixStack var1, float var2, Arm var3) {
      float var4 = clampCurve(var2, 0.0F, 10.0F);
      float var5 = clampCurve(var4, 0.0F, 0.5F);
      float var6 = clampCurve(var4, 0.5F, 0.8F);
      float var7 = clampCurve(var4, 0.8F, 1.0F);
      float var8 = clampCurve(var2, 4.0F, 10.0F);
      float var9 = smoothOut(smoothHold(clampCurve(var2 - 6.0F, 0.0F, 8.0F)));
      float var10 = clampCurve(var2, 12.0F, 16.0F);
      float var11 = 2.0F * smoothIn(var8) - 2.0F * smoothOut(var10);
      float var12 = MathHelper.sin(var2 * 19.0F * (float) (Math.PI / 180.0)) * var11;
      float var13 = MathHelper.sin(var2 * 30.0F * (float) (Math.PI / 180.0)) * var11;
      int var14 = var3 == Arm.RIGHT ? 1 : -1;
      var1.translate(
         var14 * (var4 * 0.15F + var7 * -0.05F + var8 * -0.1F + var12 * 0.005F),
         var4 * -0.075F + var6 * 0.075F + var13 * 0.01F,
         var5 * 0.05F + var7 * -0.05F + var12 * 0.005F
      );
      var1.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-65.0F * smoothOut(var4) - 35.0F * var9 + 100.0F * var10 - 0.5F * var13));
      var1.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(var14 * (-90.0F * var4 * clampCurve(var4, 0.5F, 0.55F) + 90.0F * var8 + 2.0F * var12)));
      var1.translate(var14 * 0.15F, -spearPullback(var0), 0.0F);
   }

   private static float spearPullback(float var0) {
      return smoothHold(clampCurve(var0, 0.4F, 3.0F)) - smoothIn(clampCurve(var0, 3.0F, 10.0F));
   }

   private static float clampCurve(float var0, float var1, float var2) {
      return MathHelper.clamp((var0 - var1) / (var2 - var1), 0.0F, 1.0F);
   }

   private static float smoothIn(float var0) {
      return var0 * var0;
   }

   private static float smoothOut(float var0) {
      float var1 = 1.0F - var0;
      return 1.0F - var1 * var1;
   }

   private static float smoothHold(float var0) {
      return (float)Math.sin(var0 * Math.PI * 0.5);
   }
}
