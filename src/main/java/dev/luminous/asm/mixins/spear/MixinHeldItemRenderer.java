package dev.luminous.asm.mixins.spear;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import dev.luminous.mod.modules.impl.render.SpearFix;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HeldItemRenderer.class})
public abstract class MixinHeldItemRenderer {
   @Inject(
      method = {"renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
      )},
      require = 0
   )
   private void spearFix$onRenderItem(
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
      if (SpearFix.INSTANCE != null && var4 == Hand.MAIN_HAND && SpearFix.INSTANCE.isFallbackUseActive() && SpearFix.INSTANCE.shouldUseAnimate(var6)) {
         SpearFix.INSTANCE.applyFirstPersonUseAnimation(var8, var1, var1.getMainArm(), var6, SpearFix.INSTANCE.getFallbackUseTicks(var2));
      }
   }

   @WrapOperation(
      method = {"renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V",
         ordinal = 12
      )},
      require = 0
   )
   private void spearFix$suppressNormalSwing(MatrixStack var1, float var2, float var3, float var4, Operation<Void> var5, @Local(argsOnly = true) ItemStack var6) {
      if (SpearFix.INSTANCE == null || !SpearFix.INSTANCE.shouldAnimateFirstPerson(var6)) {
         var5.call(new Object[]{var1, var2, var3, var4});
      }
   }

   @WrapOperation(
      method = {"renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applySwingOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"
      )},
      require = 0
   )
   private void spearFix$applySwingOffset(
      HeldItemRenderer var1,
      MatrixStack var2,
      Arm var3,
      float var4,
      Operation<Void> var5,
      @Local(argsOnly = true) AbstractClientPlayerEntity var6,
      @Local(argsOnly = true) ItemStack var7
   ) {
      if (SpearFix.INSTANCE == null || !SpearFix.INSTANCE.shouldAnimateFirstPerson(var7)) {
         var5.call(new Object[]{var1, var2, var3, var4});
      } else if (!var6.isUsingItem()) {
         SpearFix.INSTANCE.applyStabAnimation(var2, var4, var7);
      } else {
         var5.call(new Object[]{var1, var2, var3, 0.0F});
      }
   }

   @WrapOperation(
      method = {"renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
         ordinal = 8
      )},
      require = 0
   )
   private void spearFix$applyEquipOffset(
      HeldItemRenderer var1,
      MatrixStack var2,
      Arm var3,
      float var4,
      Operation<Void> var5,
      @Local(argsOnly = true) AbstractClientPlayerEntity var6,
      @Local(argsOnly = true) ItemStack var7
   ) {
      if (SpearFix.INSTANCE != null && SpearFix.INSTANCE.isFallbackUseActive() && !var6.isUsingItem() && SpearFix.INSTANCE.shouldUseAnimate(var7)) {
         var5.call(new Object[]{var1, var2, var3, 0.0F});
      } else if (SpearFix.INSTANCE != null && SpearFix.INSTANCE.shouldSuppressEquip(var7, var6.handSwingProgress)) {
         var5.call(new Object[]{var1, var2, var3, 0.0F});
      } else {
         var5.call(new Object[]{var1, var2, var3, var4});
      }
   }

   @Inject(
      method = {"renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/util/UseAction;"
      )},
      require = 0
   )
   private void spearFix$useAnimation(
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
      CallbackInfo var11,
      @Share("spearFixSuppress") LocalBooleanRef var12
   ) {
      if (SpearFix.INSTANCE != null && SpearFix.INSTANCE.shouldUseAnimate(var6) && SpearFix.INSTANCE.isFallbackUseActive()) {
         var12.set(true);
      }
   }

   @WrapOperation(
      method = {"renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
         ordinal = 2
      )},
      require = 0
   )
   private void spearFix$suppressEquipOffsetForUseAnimation(
      HeldItemRenderer var1, MatrixStack var2, Arm var3, float var4, Operation<Void> var5, @Share("spearFixSuppress") LocalBooleanRef var6
   ) {
      if (!var6.get()) {
         var5.call(new Object[]{var1, var2, var3, var4});
      }
   }
}
