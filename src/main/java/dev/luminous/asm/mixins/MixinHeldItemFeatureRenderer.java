package dev.luminous.asm.mixins;

import dev.luminous.mod.features.SpearPort;
import dev.luminous.mod.modules.impl.player.ClientSpear;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemFeatureRenderer.class})
public abstract class MixinHeldItemFeatureRenderer {
   @Inject(
      method = {"renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("HEAD")}
   )
   private void alien$beginThirdPersonSpearRender(
      LivingEntity var1, ItemStack var2, ModelTransformationMode var3, Arm var4, MatrixStack var5, VertexConsumerProvider var6, int var7, CallbackInfo var8
   ) {
      if (ClientSpear.shouldRender(var2) && isThirdPersonHand(var3)) {
         SpearPort.setInHandRender(true);
      }
   }

   @Inject(
      method = {"renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("RETURN")}
   )
   private void alien$endThirdPersonSpearRender(
      LivingEntity var1, ItemStack var2, ModelTransformationMode var3, Arm var4, MatrixStack var5, VertexConsumerProvider var6, int var7, CallbackInfo var8
   ) {
      if (ClientSpear.shouldRender(var2) && isThirdPersonHand(var3)) {
         SpearPort.setInHandRender(false);
      }
   }

   private static boolean isThirdPersonHand(ModelTransformationMode var0) {
      return var0 == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND || var0 == ModelTransformationMode.THIRD_PERSON_LEFT_HAND;
   }
}
